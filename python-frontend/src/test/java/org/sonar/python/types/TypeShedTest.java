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
package org.sonar.python.types;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.plugins.python.api.symbols.ClassSymbol;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.python.semantic.FunctionSymbolImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeShedTest {

  @Test
  public void classes() {
    ClassSymbol intClass = TypeShed.typeShedClass("int");
    assertThat(intClass.superClasses()).isEmpty();
    assertThat(intClass.hasUnresolvedTypeHierarchy()).isFalse();
    assertThat(intClass.usages()).isEmpty();
    assertThat(intClass.declaredMembers()).allMatch(member -> member.usages().isEmpty());
    assertThat(TypeShed.typeShedClass("bool").superClasses()).containsExactly(intClass);
  }

  @Test(expected = IllegalArgumentException.class)
  public void not_a_class() {
    TypeShed.typeShedClass("repr");
  }

  @Test(expected = IllegalArgumentException.class)
  public void unknown_name() {
    TypeShed.typeShedClass("xxx");
  }

  @Test
  public void none_type() {
    ClassSymbol noneType = TypeShed.typeShedClass("NoneType");
    assertThat(noneType.superClasses()).isEmpty();
  }

  @Test
  public void typing_module() {
    Map<String, Symbol> symbols = TypeShed.typingModuleSymbols().stream().collect(Collectors.toMap(Symbol::name, Function.identity()));
    assertThat(symbols.values()).allMatch(symbol -> symbol.usages().isEmpty());
    // python3 specific
    assertThat(symbols.get("Awaitable").kind()).isEqualTo(Symbol.Kind.CLASS);
    // overlap btw python2 and python3
    assertThat(symbols.get("Iterator").kind()).isEqualTo(Symbol.Kind.OTHER);
  }

  @Test
  public void stdlib_symbols() {
    Map<String, Symbol> mathSymbols = TypeShed.standardLibrarySymbols("math").stream().collect(Collectors.toMap(Symbol::name, Function.identity()));
    Symbol acosSymbol = mathSymbols.get("acos");
    assertThat(acosSymbol.kind()).isEqualTo(Symbol.Kind.FUNCTION);
    assertThat(((FunctionSymbolImpl) acosSymbol).declaredReturnType().canOnlyBe("float")).isTrue();
    assertThat(TypeShed.standardLibrarySymbol("math", "math.acos")).isSameAs(acosSymbol);
    assertThat(mathSymbols.values()).allMatch(symbol -> symbol.usages().isEmpty());

    Map<String, Symbol> threadingSymbols = TypeShed.standardLibrarySymbols("threading").stream().collect(Collectors.toMap(Symbol::name, Function.identity()));
    assertThat(threadingSymbols.get("Thread").kind()).isEqualTo(Symbol.Kind.CLASS);
    assertThat(threadingSymbols.values()).allMatch(symbol -> symbol.usages().isEmpty());

    Map<String, Symbol> imaplibSymbols = TypeShed.standardLibrarySymbols("imaplib").stream().collect(Collectors.toMap(Symbol::name, Function.identity()));
    assertThat(imaplibSymbols).isNotEmpty();
    assertThat(imaplibSymbols.values()).allMatch(symbol -> symbol.usages().isEmpty());
  }
}
