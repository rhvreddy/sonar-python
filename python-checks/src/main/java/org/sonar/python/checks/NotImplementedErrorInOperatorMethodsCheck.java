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
package org.sonar.python.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.tree.BaseTreeVisitor;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.HasSymbol;
import org.sonar.plugins.python.api.tree.RaiseStatement;
import org.sonar.plugins.python.api.tree.Tree;

@Rule(key="S5712")
public class NotImplementedErrorInOperatorMethodsCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE = "Return \"NotImplemented\" instead of raising \"NotImplementedError\"";
  private static final String NOT_IMPLEMENTED_ERROR = "NotImplementedError";

  private static final List<String> OPERATOR_METHODS = Arrays.asList(
    "__lt__",
    "__le__",
    "__eq__",
    "__ne__",
    "__gt__",
    "__ge__",
    "__add__",
    "__sub__",
    "__mul__",
    "__matmul__",
    "__truediv__",
    "__floordiv__",
    "__mod__",
    "__divmod__",
    "__pow__",
    "__lshift__",
    "__rshift__",
    "__and__",
    "__xor__",
    "__or__",
    "__radd__",
    "__rsub__",
    "__rmul__",
    "__rmatmul__",
    "__rtruediv__",
    "__rfloordiv__",
    "__rmod__",
    "__rdivmod__",
    "__rpow__",
    "__rlshift__",
    "__rrshift__",
    "__rand__",
    "__rxor__",
    "__ror__",
    "__iadd__",
    "__isub__",
    "__imul__",
    "__imatmul__",
    "__itruediv__",
    "__ifloordiv__",
    "__imod__",
    "__ipow__",
    "__ilshift__",
    "__irshift__",
    "__iand__",
    "__ixor__",
    "__ior__",
    "__length_hint__"
  );

  private static class RaiseNotImplementedErrorVisitor extends BaseTreeVisitor {
    private List<RaiseStatement> nonCompliantRaises = new ArrayList<>();

    @Override
    public void visitRaiseStatement(RaiseStatement pyRaiseStatementTree) {
      if (pyRaiseStatementTree.expressions().isEmpty()) {
        // Do not bother with bare raises.
        return;
      }

      Expression raisedException = pyRaiseStatementTree.expressions().get(0);
      if (raisedException.type().canOnlyBe(NOT_IMPLEMENTED_ERROR)) {
        nonCompliantRaises.add(pyRaiseStatementTree);
      } else if (raisedException instanceof HasSymbol) {
        Symbol symbol = ((HasSymbol) raisedException).symbol();
        if (symbol != null && NOT_IMPLEMENTED_ERROR.equals(symbol.fullyQualifiedName())) {
          nonCompliantRaises.add(pyRaiseStatementTree);
        }
      }
    }
  }

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FUNCDEF, ctx -> {
      FunctionDef functionDef = (FunctionDef) ctx.syntaxNode();
      if (!functionDef.isMethodDefinition() || !OPERATOR_METHODS.contains(functionDef.name().name())) {
        return;
      }

      RaiseNotImplementedErrorVisitor visitor = new RaiseNotImplementedErrorVisitor();
      functionDef.accept(visitor);

      for (RaiseStatement notImplementedErrorRaise : visitor.nonCompliantRaises) {
        ctx.addIssue(notImplementedErrorRaise, MESSAGE);
      }
    });
  }
}
