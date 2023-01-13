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
package org.sonar.python.checks.tests;

import org.junit.Test;
import org.sonar.plugins.python.api.PythonCheck;
import org.sonar.python.checks.utils.PythonCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class SkippedTestNoReasonCheckTest {

  @Test
  public void test() {
    PythonCheckVerifier.verify("src/test/resources/checks/tests/skippedTestNoReason.py", new SkippedTestNoReasonCheck());
  }

  @Test
  public void testWithImport() {
    PythonCheckVerifier.verify("src/test/resources/checks/tests/skippedTestNoReasonWithImport.py", new SkippedTestNoReasonCheck());
  }

  @Test
  public void test_scope() {
    assertThat(new SkippedTestNoReasonCheck().scope()).isEqualTo(PythonCheck.CheckScope.ALL);
  }

}
