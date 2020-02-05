/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.python.semantic;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.sonar.plugins.python.api.symbols.FunctionSymbol;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.FileInput;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.python.PythonTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.python.PythonTestUtils.parse;
import static org.sonar.python.PythonTestUtils.pythonFile;

public class FunctionSymbolTest {

  @Test
  public void arity() {
    FunctionSymbol functionSymbol = functionSymbol("def fn(): pass");
    assertThat(functionSymbol.parameters()).isEmpty();

    functionSymbol = functionSymbol("def fn(p1, p2, p3): pass");
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::name).containsExactly("p1", "p2", "p3");
    assertThat(functionSymbol.hasVariadicParameter()).isFalse();
    assertThat(functionSymbol.isInstanceMethod()).isFalse();
    assertThat(functionSymbol.hasDecorators()).isFalse();
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::hasDefaultValue).containsExactly(false, false, false);
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::isKeywordOnly).containsExactly(false, false, false);

    functionSymbol = functionSymbol("def fn(p1, *, p2): pass");
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::name).containsExactly("p1", "p2");
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::hasDefaultValue).containsExactly(false, false);
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::isKeywordOnly).containsExactly(false, true);

    functionSymbol = functionSymbol("def fn(p1, p2=42): pass");
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::name).containsExactly("p1", "p2");
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::hasDefaultValue).containsExactly(false, true);
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::isKeywordOnly).containsExactly(false, false);

    functionSymbol = functionSymbol("def fn(p1, *, p2=42): pass");
    assertThat(functionSymbol.hasVariadicParameter()).isFalse();
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::name).containsExactly("p1", "p2");
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::hasDefaultValue).containsExactly(false, true);
    assertThat(functionSymbol.parameters()).extracting(FunctionSymbol.Parameter::isKeywordOnly).containsExactly(false, true);

    functionSymbol = functionSymbol("def fn((p1,p2,p3)): pass");
    assertThat(functionSymbol.parameters()).hasSize(1);
    assertThat(functionSymbol.parameters().get(0).name()).isNull();
    assertThat(functionSymbol.parameters().get(0).hasDefaultValue()).isFalse();
    assertThat(functionSymbol.parameters().get(0).isKeywordOnly()).isFalse();

    functionSymbol = functionSymbol("def fn(**kwargs): pass");
    assertThat(functionSymbol.parameters()).hasSize(1);
    assertThat(functionSymbol.hasVariadicParameter()).isTrue();
    assertThat(functionSymbol.parameters().get(0).name()).isEqualTo("kwargs");
    assertThat(functionSymbol.parameters().get(0).hasDefaultValue()).isFalse();
    assertThat(functionSymbol.parameters().get(0).isKeywordOnly()).isFalse();

    functionSymbol = functionSymbol("def fn(p1, *args): pass");
    assertThat(functionSymbol.hasVariadicParameter()).isTrue();

    functionSymbol = functionSymbol("class A:\n  def method(self, p1): pass");
    assertThat(functionSymbol.isInstanceMethod()).isTrue();

    functionSymbol = functionSymbol("class A:\n  @staticmethod\n  def method(self, p1): pass");
    assertThat(functionSymbol.isInstanceMethod()).isFalse();

    functionSymbol = functionSymbol("class A:\n  @classmethod\n  def method(self, p1): pass");
    assertThat(functionSymbol.isInstanceMethod()).isFalse();
    assertThat(functionSymbol.hasDecorators()).isTrue();

    functionSymbol = functionSymbol("class A:\n  @dec\n  def method(self, p1): pass");
    assertThat(functionSymbol.isInstanceMethod()).isTrue();
    assertThat(functionSymbol.hasDecorators()).isTrue();
  }

  @Test
  public void reassigned_symbol() {
    FileInput tree = parse(
      "def fn(): pass",
      "fn = 42"
    );
    FunctionDef functionDef = (FunctionDef) tree.statements().statements().get(0);
    Symbol symbol = functionDef.name().symbol();
    assertThat(symbol.kind()).isEqualTo(Symbol.Kind.OTHER);

    tree = parse(
      "fn = 42",
      "def fn(): pass"
    );
    functionDef = (FunctionDef) tree.statements().statements().get(1);
    symbol = functionDef.name().symbol();
    assertThat(symbol.kind()).isEqualTo(Symbol.Kind.OTHER);

    tree = parse(
      "def fn(p1, p2): pass",
      "def fn(): pass"
    );
    functionDef = (FunctionDef) tree.statements().statements().get(0);
    symbol = functionDef.name().symbol();
    assertThat(symbol.kind()).isEqualTo(Symbol.Kind.OTHER);
  }

  @Test
  public void return_type() {
    Symbol strTypeSymbol = new SymbolImpl("str", "str");
    FunctionSymbolImpl fnSymbol = new FunctionSymbolImpl("fn", "mod.fn", false, false, false, Collections.emptyList(), new Type(strTypeSymbol));
    List<Symbol> modSymbols = Collections.singletonList(fnSymbol);
    Map<String, Set<Symbol>> globalSymbols = Collections.singletonMap("mod", new HashSet<>(modSymbols));
    FileInput tree = parse(
      new SymbolTableBuilder("my_package", pythonFile("my_module.py"), globalSymbols),
      "from mod import fn",
      "x = fn()"
    );
    AssignmentStatement assignmentStatement = (AssignmentStatement) tree.statements().statements().get(1);
    Symbol x = ((Name) assignmentStatement.lhsExpressions().get(0).expressions().get(0)).symbol();
    assertThat(((SymbolImpl) x).type().symbol()).isEqualTo(strTypeSymbol);
  }

  private FunctionSymbol functionSymbol(String code) {
    FileInput tree = parse(code);
    FunctionDef functionDef = (FunctionDef) PythonTestUtils.getAllDescendant(tree, t -> t.is(Tree.Kind.FUNCDEF)).get(0);
    Symbol functionSymbol = functionDef.name().symbol();
    assertThat(functionSymbol.kind()).isEqualTo(Symbol.Kind.FUNCTION);
    List<FunctionSymbol.Parameter> parameters = ((FunctionSymbol) functionSymbol).parameters();
    return ((FunctionSymbol) functionSymbol);
  }
}
