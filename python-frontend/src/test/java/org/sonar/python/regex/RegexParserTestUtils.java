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
package org.sonar.python.regex;

import junit.framework.AssertionFailedError;
import org.sonar.plugins.python.api.tree.FileInput;
import org.sonar.plugins.python.api.tree.StringElement;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.RegexSource;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.python.PythonTestUtils.getFirstDescendant;
import static org.sonar.python.PythonTestUtils.parse;

public class RegexParserTestUtils {

  private static final String PYTHON_CODE = "import re\nre.match(%s, 'input')";

  private RegexParserTestUtils() {

  }

  public static RegexTree assertSuccessfulParse(String regex) {
    RegexParseResult result = parseRegex(regex);
    if (!result.getSyntaxErrors().isEmpty()) {
      throw new AssertionFailedError("Parsing should complete with no errors.");
    }
    return result.getResult();
  }

  public static RegexParseResult parseRegex(String regex) {
    RegexSource source = makeSource(regex);
    return new RegexParser(source, new FlagSet()).parse();
  }

  public static RegexSource makeSource(String content) {
    FileInput inputFile = parse(String.format(PYTHON_CODE, content));
    StringElement pattern = getFirstDescendant(inputFile, tree -> tree.is(Tree.Kind.STRING_ELEMENT));
    return new PythonAnalyzerRegexSource(pattern);
  }

  public static void assertKind(RegexTree.Kind expected, RegexTree actual) {
    assertThat(actual.kind()).withFailMessage("Regex should have kind " + expected).isEqualTo(expected);
    assertThat(actual.is(expected)).withFailMessage("`is` should return true when the kinds match.").isTrue();
    assertThat(actual.is(RegexTree.Kind.CHARACTER, RegexTree.Kind.DISJUNCTION, expected)).withFailMessage("`is` should return true when one of the kinds match.").isTrue();
  }
}
