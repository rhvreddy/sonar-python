/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2023 SonarSource SA
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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.FileInput;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Tree.Kind;

import static org.sonar.python.PythonTestUtils.getFirstDescendant;
import static org.sonar.python.PythonTestUtils.getLastDescendant;
import static org.sonar.python.PythonTestUtils.parse;

public class MemberResolutionTest {

  @Test
  public void basic_method_resolution() {
    FileInput fileInput = parse(
      "class A():",
      "  def foo(self): pass",
      "def f():",
      "  a = A()",
      "  a.foo()"
    );
    FunctionDef funcDef = getFirstDescendant(fileInput, p -> p.is(Kind.FUNCDEF));
    CallExpression call = getLastDescendant(fileInput, p -> p.is(Kind.CALL_EXPR));
    Assertions.assertThat(call.calleeSymbol()).isEqualTo(funcDef.name().symbol());
  }

  @Test
  public void super_method_resolution() {
    FileInput fileInput = parse(
      "class Base:",
      "  def __reduce__(self, p1, p2): pass",
      "class A(Base):",
      "  def foo(self):",
      "    s = super()",
      "    s.__reduce__(1, 2)"
    );
    CallExpression call = getLastDescendant(fileInput, p -> p.is(Kind.CALL_EXPR));
    // TODO: call.calleeSymbol().fullyQualifiedName() should be equal to "Base.__reduce__"
    Assertions.assertThat(call.calleeSymbol().fullyQualifiedName()).isNotEqualTo("object.__reduce__");
  }
}
