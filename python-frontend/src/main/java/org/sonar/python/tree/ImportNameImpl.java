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
package org.sonar.python.tree;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.plugins.python.api.tree.AliasedName;
import org.sonar.plugins.python.api.tree.ImportName;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TreeVisitor;

public class ImportNameImpl extends SimpleStatement implements ImportName {

  private final Token importKeyword;
  private final List<AliasedName> aliasedNames;
  private final Separators separators;

  public ImportNameImpl(Token importKeyword, List<AliasedName> aliasedNames, Separators separators) {
    this.importKeyword = importKeyword;
    this.aliasedNames = aliasedNames;
    this.separators = separators;
  }

  @Override
  public Token importKeyword() {
    return importKeyword;
  }

  @Override
  public List<AliasedName> modules() {
    return aliasedNames;
  }

  @CheckForNull
  @Override
  public Token separator() {
    return separators.last();
  }

  @Override
  public Kind getKind() {
    return Kind.IMPORT_NAME;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitImportName(this);
  }

  @Override
  public List<Tree> computeChildren() {
    return Stream.of(Collections.singletonList(importKeyword), aliasedNames, separators.elements())
      .flatMap(List::stream).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
