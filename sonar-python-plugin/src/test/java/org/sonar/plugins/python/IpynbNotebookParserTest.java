/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2024 SonarSource SA
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
package org.sonar.plugins.python;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.internal.apachecommons.lang.StringUtils;
import org.sonar.python.IPythonLocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.plugins.python.TestUtils.createInputFile;

class IpynbNotebookParserTest {
  private final File baseDir = new File("src/test/resources/org/sonar/plugins/python").getAbsoluteFile();

  @Test
  void testParseNotebook() throws IOException {
    var inputFile = createInputFile(baseDir, "notebook.ipynb", InputFile.Status.CHANGED, InputFile.Type.MAIN);

    var resultOptional = IpynbNotebookParser.parseNotebook(inputFile);

    assertThat(resultOptional).isPresent();

    var result = resultOptional.get();

    assertThat(result.locationMap().keySet()).hasSize(27);
    assertThat(result.contents()).hasLineCount(27);
    assertThat(StringUtils.countMatches(result.contents(), IpynbNotebookParser.SONAR_PYTHON_NOTEBOOK_CELL_DELIMITER))
      .isEqualTo(7);
    assertThat(result.locationMap()).extracting(map -> map.get(1)).isEqualTo(new IPythonLocation(17, 5, Map.of(-1, 0)));
    //"    print \"not none\"\n"
    assertThat(result.locationMap()).extracting(map -> map.get(3)).isEqualTo(new IPythonLocation(19, 5, Map.of(10, 16, 19, 26, -1, 2)));
    //"source": "#Some code\nprint(\"hello world\\n\")",
    assertThat(result.locationMap()).extracting(map -> map.get(16)).isEqualTo(new IPythonLocation(64, 14, Map.of(-1, 0), true));
    assertThat(result.locationMap()).extracting(map -> map.get(17)).isEqualTo(new IPythonLocation(64, 27, Map.of(6, 34, 18, 47, 20, 50, -1, 3), true));
    //"source": "print(\"My\\ntext\")\nprint(\"Something else\\n\")"
    assertThat(result.locationMap()).extracting(map -> map.get(22)).isEqualTo(new IPythonLocation(83, 14, Map.of(6, 21, 9, 25, 15, 32, -1, 3), true));
    assertThat(result.locationMap()).extracting(map -> map.get(23)).isEqualTo(new IPythonLocation(83, 37, Map.of(6, 44, 21, 60, 23, 63, -1, 3), true));

    //"source": "a = \"A bunch of characters \\n \\f \\r \\  \"\nb = None"
    assertThat(result.locationMap()).extracting(map -> map.get(25))
      .isEqualTo(new IPythonLocation(90, 14, Map.of(4,19, 27, 43, 30, 47, 33, 51, 36, 55, 39, 59, -1, 6), true));
    assertThat(result.locationMap()).extracting(map -> map.get(26)).isEqualTo(new IPythonLocation(90, 63, Map.of(-1, 0), true));
    // last line with the cell delimiter which contains the EOF token 
    assertThat(result.locationMap()).extracting(map -> map.get(27)).isEqualTo(new IPythonLocation(90, 14, Map.of(-1, 0)));
  }

  @Test
  void testParseNotebookWithEmptyLines() throws IOException {
    var inputFile = createInputFile(baseDir, "notebook_with_empty_lines.ipynb", InputFile.Status.CHANGED, InputFile.Type.MAIN);

    var resultOptional = IpynbNotebookParser.parseNotebook(inputFile);

    assertThat(resultOptional).isPresent();

    var result = resultOptional.get();

    assertThat(result.locationMap().keySet()).hasSize(4);
    assertThat(result.contents()).hasLineCount(4);
    assertThat(StringUtils.countMatches(result.contents(), IpynbNotebookParser.SONAR_PYTHON_NOTEBOOK_CELL_DELIMITER))
      .isEqualTo(1);
    assertThat(result.locationMap()).extracting(map -> map.get(3)).isEqualTo(new IPythonLocation(11, 5, Map.of(-1, 0)));

    // last line with the cell delimiter which contains the EOF token
    assertThat(result.locationMap()).extracting(map -> map.get(4)).isEqualTo(new IPythonLocation(11, 5, Map.of(-1, 0)));
  }

  @Test
  void testParseInvalidNotebook() {
    var inputFile = createInputFile(baseDir, "invalid_notebook.ipynb", InputFile.Status.CHANGED, InputFile.Type.MAIN);

    assertThatThrownBy(() -> IpynbNotebookParser.parseNotebook(inputFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Unexpected token");
  }

  @Test
  void testParseMojoNotebook() {
    var inputFile = createInputFile(baseDir, "notebook_mojo.ipynb", InputFile.Status.CHANGED, InputFile.Type.MAIN);

    var resultOptional = IpynbNotebookParser.parseNotebook(inputFile);

    assertThat(resultOptional).isEmpty();
  }

  @Test
  void testParseNotebookWithNoLanguage() {
    var inputFile = createInputFile(baseDir, "notebook_no_language.ipynb", InputFile.Status.CHANGED, InputFile.Type.MAIN);

    var resultOptional = IpynbNotebookParser.parseNotebook(inputFile);

    assertThat(resultOptional).isPresent();
  }

  @Test
  void testParseNotebookWithExtraLineEndInArray() throws IOException {
    var inputFile = createInputFile(baseDir, "notebook_extra_line.ipynb", InputFile.Status.CHANGED, InputFile.Type.MAIN);

    var resultOptional = IpynbNotebookParser.parseNotebook(inputFile);

    assertThat(resultOptional).isPresent();

    var result = resultOptional.get();
    assertThat(result.locationMap()).hasSize(3);
    assertThat(result.contents()).hasLineCount(3);
  }

  @Test
  void testParseNotebookSingleLine() throws IOException {
    var inputFile = createInputFile(baseDir, "notebook_single_line.ipynb", InputFile.Status.CHANGED, InputFile.Type.MAIN);

    var resultOptional = IpynbNotebookParser.parseNotebook(inputFile);

    assertThat(resultOptional).isPresent();

    var result = resultOptional.get();
    assertThat(result.locationMap()).hasSize(9);
    assertThat(result.contents()).hasLineCount(9);
    // position of variable t
    assertThat(result.locationMap().get(4).column()).isEqualTo(452);

    // First and second line
    assertThat(result.locationMap()).containsEntry(1, new IPythonLocation(1, 382, Map.of(-1, 0), true));
    assertThat(result.locationMap()).containsEntry(2, new IPythonLocation(1, 429, Map.of(-1, 0), true));

    assertThat(result.locationMap()).containsEntry(6, new IPythonLocation(1, 559, Map.of(-1, 3, 0, 560, 1, 562, 2, 564), true));
    assertThat(result.locationMap()).containsEntry(7, new IPythonLocation(1, 610, Map.of(-1, 0), true));
    assertThat(result.locationMap()).containsEntry(8, new IPythonLocation(1, 637, Map.of(-1, 3, 1, 640, 2, 642, 0, 638), true));
  }

  @Test
  void testParseNotebook1() throws IOException {
    var inputFile = createInputFile(baseDir, "notebook_no_code.ipynb", InputFile.Status.CHANGED, InputFile.Type.MAIN);

    var resultOptional = IpynbNotebookParser.parseNotebook(inputFile);

    assertThat(resultOptional).isPresent();

    var result = resultOptional.get();
    assertThat(result.locationMap()).isEmpty();
    assertThat(result.contents()).isEmpty();
  }
}
