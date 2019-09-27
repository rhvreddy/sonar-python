/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2019 SonarSource SA
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
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.python.PythonSubscriptionCheck;
import org.sonar.python.SubscriptionContext;
import org.sonar.python.api.tree.ElseStatement;
import org.sonar.python.api.tree.Expression;
import org.sonar.python.api.tree.IfStatement;
import org.sonar.python.api.tree.StatementList;
import org.sonar.python.api.tree.Statement;
import org.sonar.python.api.tree.Tree;

@Rule(key = SameConditionCheck.CHECK_KEY)
public class SameConditionCheck extends PythonSubscriptionCheck {
  public static final String CHECK_KEY = "S1862";
  private static final String MESSAGE = "This branch duplicates the one on line %s.";

  private List<IfStatement> ignoreList;

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FILE_INPUT, ctx -> this.ignoreList = new ArrayList<>());

    context.registerSyntaxNodeConsumer(Tree.Kind.IF_STMT, ctx -> {
      IfStatement ifStatement = (IfStatement) ctx.syntaxNode();
      if (ignoreList.contains(ifStatement)) {
        return;
      }
      List<Expression> conditions = getConditionsToCompare(ifStatement);
      findSameConditions(conditions, ctx);
    });
  }

  private List<Expression> getConditionsToCompare(IfStatement ifStatement) {
    List<Expression> conditions = new ArrayList<>();
    conditions.add(ifStatement.condition());
    conditions.addAll(ifStatement.elifBranches().stream().map(IfStatement::condition).collect(Collectors.toList()));
    ElseStatement elseStatement = ifStatement.elseBranch();
    if (elseStatement != null) {
      lookForElseIfs(conditions, elseStatement);
    }
    return conditions;
  }

  private void lookForElseIfs(List<Expression> conditions, ElseStatement elseBranch) {
    IfStatement singleIfChild = singleIfChild(elseBranch.body());
    if (singleIfChild != null) {
      ignoreList.add(singleIfChild);
      conditions.addAll(getConditionsToCompare(singleIfChild));
    }
  }

  private void findSameConditions(List<Expression> conditions, SubscriptionContext ctx) {
    for (int i = 1; i < conditions.size(); i++) {
      compareConditions(conditions, i, ctx);
    }
  }

  private void compareConditions(List<Expression> conditions, int index, SubscriptionContext ctx) {
    for (int j = 0; j < index; j++) {
      if (CheckUtils.areEquivalent(conditions.get(j), conditions.get(index))) {
        String message = String.format(MESSAGE, conditions.get(j).firstToken().line());
        ctx.addIssue(conditions.get(index), message).secondary(conditions.get(j), "Original");
        return;
      }
    }
  }

  private static IfStatement singleIfChild(StatementList statementList) {
    List<Statement> statements = statementList.statements();
    if (statements.size() == 1 && statements.get(0).is(Tree.Kind.IF_STMT)) {
      return (IfStatement) statements.get(0);
    }
    return null;
  }
}
