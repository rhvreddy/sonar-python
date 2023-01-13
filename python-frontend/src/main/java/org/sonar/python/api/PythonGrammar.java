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
package org.sonar.python.api;

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Grammar;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.sonar.sslr.grammar.LexerfulGrammarBuilder;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static com.sonar.sslr.api.GenericTokenType.IDENTIFIER;
import static org.sonar.python.api.PythonTokenType.DEDENT;
import static org.sonar.python.api.PythonTokenType.INDENT;
import static org.sonar.python.api.PythonTokenType.NEWLINE;
import static org.sonar.python.api.PythonTokenType.NUMBER;

public enum PythonGrammar implements GrammarRuleKey {
  FACTOR,
  TRAILER,
  SUBSCRIPTLIST,
  SUBSCRIPT,
  SLICEOP,
  TESTLIST_COMP,
  DICTORSETMAKER,

  ARGLIST,
  ARGUMENT,

  NAME,

  VARARGSLIST,
  FPDEF,
  FPLIST,

  TYPEDARGSLIST,
  TFPDEF,
  TFPLIST,

  TEST,
  TESTLIST,

  COMP_FOR,
  COMP_ITER,
  COMP_IF,
  TEST_NOCOND,
  EXPRLIST,
  EXPR,
  STAR_EXPR,

  TESTLIST_STAR_EXPR,

  YIELD_EXPR,

  // Expressions

  ATOM,

  POWER,

  A_EXPR,
  M_EXPR,

  SHIFT_EXPR,

  XOR_EXPR,
  AND_EXPR,
  OR_EXPR,

  NAMED_EXPR_TEST,
  STAR_NAMED_EXPRESSIONS,
  STAR_NAMED_EXPRESSION,
  FORMATTED_EXPR,
  F_STRING_CONTENT,
  FORMAT_SPECIFIER,

  COMPARISON,
  COMP_OPERATOR,

  OR_TEST,
  AND_TEST,
  NOT_TEST,

  LAMBDEF,
  LAMBDEF_NOCOND,

  ELLIPSIS,

  // Simple statements

  SIMPLE_STMT,
  EXPRESSION_STMT,
  PRINT_STMT,
  EXEC_STMT,
  ASSERT_STMT,

  ANNASSIGN,
  AUGASSIGN,

  PASS_STMT,
  DEL_STMT,
  RETURN_STMT,
  YIELD_STMT,
  RAISE_STMT,
  BREAK_STMT,
  CONTINUE_STMT,

  IMPORT_STMT,
  IMPORT_NAME,
  IMPORT_FROM,
  IMPORT_AS_NAME,
  DOTTED_AS_NAME,
  IMPORT_AS_NAMES,
  DOTTED_AS_NAMES,

  GLOBAL_STMT,
  NONLOCAL_STMT,

  // Compound statements

  COMPOUND_STMT,
  SUITE,
  STATEMENT,
  STMT_LIST,

  IF_STMT,
  WHILE_STMT,
  FOR_STMT,

  TRY_STMT,
  EXCEPT_CLAUSE,

  WITH_STMT,
  WITH_ITEM,

  MATCH_STMT,
  SUBJECT_EXPR,
  CASE_BLOCK,
  GUARD,

  PATTERNS,
  PATTERN,
  AS_PATTERN,
  OR_PATTERN,
  CLOSED_PATTERN,
  LITERAL_PATTERN,
  CAPTURE_PATTERN,
  SEQUENCE_PATTERN,
  STAR_PATTERN,
  MAYBE_STAR_PATTERN,
  MAYBE_SEQUENCE_PATTERN,
  OPEN_SEQUENCE_PATTERN,
  WILDCARD_PATTERN,
  GROUP_PATTERN,
  CLASS_PATTERN,
  PATTERN_ARGS,
  PATTERN_ARG,
  KEYWORD_PATTERN,
  NAME_OR_ATTR,
  VALUE_PATTERN,
  MAPPING_PATTERN,
  ITEMS_PATTERN,
  KEY_VALUE_PATTERN,
  DOUBLE_STAR_PATTERN,

  SIGNED_NUMBER,
  COMPLEX_NUMBER,

  FUNCDEF,
  DECORATORS,
  DECORATOR,
  DOTTED_NAME,
  ATTR,
  FUNCNAME,
  FUN_RETURN_ANNOTATION,

  CLASSDEF,
  CLASSNAME,

  ASYNC_STMT,

  // Top-level components

  FILE_INPUT;

  public static Grammar create() {
    LexerfulGrammarBuilder b = LexerfulGrammarBuilder.create();

    b.rule(FILE_INPUT).is(b.zeroOrMore(b.firstOf(NEWLINE, STATEMENT)), EOF);

    grammar(b);
    compoundStatements(b);
    simpleStatements(b);
    expressions(b);

    b.setRootRule(FILE_INPUT);
    return b.buildWithMemoizationOfMatchesForAllRules();
  }

  public static void grammar(LexerfulGrammarBuilder b) {

    // https://github.com/python/cpython/blob/v3.6.4/Grammar/Grammar#L41
    b.rule(EXPRESSION_STMT).is(
      TESTLIST_STAR_EXPR,
      b.firstOf(
        ANNASSIGN,
        b.sequence(AUGASSIGN, b.firstOf(YIELD_EXPR, TESTLIST)),
        b.zeroOrMore("=", b.firstOf(YIELD_EXPR, TESTLIST_STAR_EXPR))));

    // https://github.com/python/cpython/blob/v3.8.0/Grammar/Grammar#L87
    b.rule(ANNASSIGN).is(":", TEST, b.optional( "=", b.firstOf(YIELD_EXPR, TESTLIST_STAR_EXPR)));

    b.rule(TESTLIST_STAR_EXPR).is(b.firstOf(TEST, STAR_EXPR), b.zeroOrMore(",", b.firstOf(TEST, STAR_EXPR)), b.optional(","));
    b.rule(AUGASSIGN).is(b.firstOf("+=", "-=", "*=", "/=", "//=", "%=", "**=", ">>=", "<<=", "&=", "^=", "|=", "@="));

    b.rule(NAMED_EXPR_TEST).is(TEST, b.optional(PythonPunctuator.WALRUS_OPERATOR, TEST));
    b.rule(STAR_NAMED_EXPRESSIONS).is(STAR_NAMED_EXPRESSION, b.zeroOrMore(",", STAR_NAMED_EXPRESSION), b.optional(","));
    b.rule(STAR_NAMED_EXPRESSION).is(b.firstOf(
      STAR_EXPR,
      NAMED_EXPR_TEST
    ));
    b.rule(TEST).is(b.firstOf(
      b.sequence(OR_TEST, b.optional("if", OR_TEST, "else", TEST)),
      LAMBDEF));
    b.rule(TEST_NOCOND).is(b.firstOf(OR_TEST, LAMBDEF_NOCOND));
    b.rule(LAMBDEF).is("lambda", b.optional(VARARGSLIST), ":", TEST);
    b.rule(LAMBDEF_NOCOND).is("lambda", b.optional(VARARGSLIST), ":", TEST_NOCOND);

    b.rule(STAR_EXPR).is("*", EXPR);
    b.rule(EXPR).is(XOR_EXPR, b.zeroOrMore("|", XOR_EXPR));

    // https://docs.python.org/3/reference/lexical_analysis.html#formatted-string-literals
    b.rule(F_STRING_CONTENT).is(b.zeroOrMore(b.firstOf(GenericTokenType.UNKNOWN_CHAR, FORMATTED_EXPR)));
    b.rule(FORMATTED_EXPR).is(
      PythonPunctuator.LCURLYBRACE,
      TEST,
      b.optional(PythonPunctuator.ASSIGN),
      b.optional("!", b.firstOf("s", "r", "a")),
      b.optional(FORMAT_SPECIFIER),
      PythonPunctuator.RCURLYBRACE);
    b.rule(FORMAT_SPECIFIER).is(
      ":",
      b.oneOrMore(b.firstOf(FORMATTED_EXPR, b.anyTokenButNot(PythonPunctuator.RCURLYBRACE)))
    );

    b.rule(FACTOR).is(b.firstOf(
      b.sequence(b.firstOf("+", "-", "~"), FACTOR),
      POWER)).skipIfOneChild();
    b.rule(POWER).is(b.firstOf(
      b.sequence(b.optional("await"), ATOM, b.zeroOrMore(TRAILER), b.optional("**", FACTOR)),
      // matches "await" identifier
      "await")).skipIfOneChild();

    b.rule(ATOM).is(b.firstOf(
        b.sequence("(", b.optional(b.firstOf(YIELD_EXPR, TESTLIST_COMP)), ")"),
        b.sequence("[", b.optional(TESTLIST_COMP), "]"),
        b.sequence("{", b.optional(DICTORSETMAKER), "}"),
        b.sequence("`", TEST, b.zeroOrMore(",", TEST), "`"),
        NAME,
        PythonTokenType.NUMBER,
        b.oneOrMore(PythonTokenType.STRING),
        ELLIPSIS,
        PythonKeyword.NONE));
    b.rule(ELLIPSIS).is(b.sequence(".", ".", "."));
    b.rule(TESTLIST_COMP).is(b.firstOf(NAMED_EXPR_TEST, STAR_EXPR), b.firstOf(COMP_FOR, b.sequence(b.zeroOrMore(",", b.firstOf(NAMED_EXPR_TEST, STAR_EXPR)), b.optional(","))));
    b.rule(TRAILER).is(b.firstOf(
        b.sequence("(", b.optional(ARGLIST), ")"),
        b.sequence("[", SUBSCRIPTLIST, "]"),
        b.sequence(".", NAME)));
    b.rule(SUBSCRIPTLIST).is(SUBSCRIPT, b.zeroOrMore(",", SUBSCRIPT), b.optional(","));
    b.rule(SUBSCRIPT).is(b.firstOf(
        b.sequence(b.optional(TEST), ":", b.optional(TEST), b.optional(SLICEOP)),
        NAMED_EXPR_TEST));
    b.rule(SLICEOP).is(":", b.optional(TEST));
    b.rule(EXPRLIST).is(b.firstOf(EXPR, STAR_EXPR), b.zeroOrMore(",", b.firstOf(EXPR, STAR_EXPR)), b.optional(","));
    b.rule(TESTLIST).is(TEST, b.zeroOrMore(",", TEST), b.optional(","));
    b.rule(DICTORSETMAKER).is(b.firstOf(
      b.sequence(
        b.firstOf(b.sequence(TEST, ":", TEST), b.sequence("**", EXPR)),
        b.firstOf(COMP_FOR, b.sequence(b.zeroOrMore(",", b.firstOf(b.sequence(TEST, ":", TEST), b.sequence("**", EXPR))), b.optional(",")))),
      b.sequence(b.firstOf(TEST, STAR_EXPR), b.firstOf(COMP_FOR, b.sequence(b.zeroOrMore(",", b.firstOf(TEST, STAR_EXPR)), b.optional(","))))));

    b.rule(ARGLIST).is(ARGUMENT, b.zeroOrMore(",", ARGUMENT), b.optional(","));
    b.rule(ARGUMENT).is(b.firstOf(
      b.sequence("*", TEST),
      b.sequence("**", TEST),
      b.sequence(TEST, PythonPunctuator.WALRUS_OPERATOR, TEST),
      b.sequence(TEST, "=", TEST),
      b.sequence(TEST, b.optional(COMP_FOR))));
    b.rule(COMP_ITER).is(b.firstOf(COMP_FOR, COMP_IF));
    b.rule(COMP_FOR).is(b.optional("async"), "for", EXPRLIST, "in", TESTLIST, b.optional(COMP_ITER));
    b.rule(COMP_IF).is("if", TEST_NOCOND, b.optional(COMP_ITER));

    b.rule(YIELD_EXPR).is(b.firstOf(
      b.sequence("yield", "from", TEST),
      b.sequence("yield", b.optional(TESTLIST_STAR_EXPR))));

    b.rule(NAME).is(IDENTIFIER);

    b.rule(VARARGSLIST).is(b.firstOf(
      b.sequence("**", NAME),
      b.sequence("*", b.optional(NAME), b.zeroOrMore(",", FPDEF, b.optional("=", TEST)), b.optional(",", "**", NAME)),
      b.sequence(FPDEF, b.optional("=", TEST),
        b.zeroOrMore(",", FPDEF, b.optional("=", TEST)),
        b.optional(",", "/", b.zeroOrMore(",", FPDEF, b.optional("=", TEST))),
        b.optional(",", b.firstOf(
          b.sequence("**", NAME),
          b.sequence("*", b.optional(NAME), b.zeroOrMore(",", FPDEF, b.optional("=", TEST)), b.optional(",", "**", NAME))))
        )), b.optional(","));
    b.rule(FPDEF).is(b.firstOf(
        NAME,
        b.sequence("(", FPLIST, ")")));
    b.rule(FPLIST).is(FPDEF, b.zeroOrMore(",", FPDEF), b.optional(","));

    b.rule(TYPEDARGSLIST).is(b.firstOf(
      b.sequence("**", TFPDEF, b.optional(",")),
      b.sequence("*", b.optional(TFPDEF), b.zeroOrMore(",", TFPDEF, b.optional("=", TEST)), b.optional(",", "**", TFPDEF), b.optional(",")),
      b.sequence(TFPDEF, b.optional("=", TEST),
        b.zeroOrMore(",", TFPDEF, b.optional("=", TEST)),
        b.optional(",", "/", b.zeroOrMore(",", TFPDEF, b.optional("=", TEST))),
        b.optional(",", b.optional(b.firstOf(
          b.sequence("**", TFPDEF),
          b.sequence("*", b.optional(TFPDEF), b.zeroOrMore(",", TFPDEF, b.optional("=", TEST)), b.optional(",", "**", TFPDEF))
      ), b.optional(","))))
    ));
    b.rule(TFPDEF).is(b.firstOf(
      b.sequence(NAME, b.optional(":", TEST)),
      b.sequence("(", TFPLIST, ")")));
    b.rule(TFPLIST).is(TFPDEF, b.zeroOrMore(",", TFPDEF), b.optional(","));
  }

  /**
   * Expressions
   * http://docs.python.org/reference/expressions.html
   */
  public static void expressions(LexerfulGrammarBuilder b) {
    b.rule(M_EXPR).is(FACTOR, b.zeroOrMore(b.firstOf("*", "//", "/", "%", "@"), FACTOR)).skipIfOneChild();
    b.rule(A_EXPR).is(M_EXPR, b.zeroOrMore(b.firstOf("+", "-"), M_EXPR)).skipIfOneChild();

    b.rule(SHIFT_EXPR).is(A_EXPR, b.zeroOrMore(b.firstOf("<<", ">>"), A_EXPR)).skipIfOneChild();

    b.rule(AND_EXPR).is(SHIFT_EXPR, b.zeroOrMore("&", SHIFT_EXPR)).skipIfOneChild();
    b.rule(XOR_EXPR).is(AND_EXPR, b.zeroOrMore("^", AND_EXPR)).skipIfOneChild();
    b.rule(OR_EXPR).is(XOR_EXPR, b.zeroOrMore("|", XOR_EXPR)).skipIfOneChild();

    b.rule(COMPARISON).is(OR_EXPR, b.zeroOrMore(COMP_OPERATOR, OR_EXPR)).skipIfOneChild();
    b.rule(COMP_OPERATOR).is(b.firstOf(
      "<",
      ">",
      "==",
      ">=",
      "<=",
      "!=",
      "<>",
      b.sequence("is", b.optional("not")),
      b.sequence(b.optional("not"), "in")));

    b.rule(OR_TEST).is(AND_TEST, b.zeroOrMore("or", AND_TEST)).skipIfOneChild();
    b.rule(AND_TEST).is(NOT_TEST, b.zeroOrMore("and", NOT_TEST)).skipIfOneChild();
    b.rule(NOT_TEST).is(b.firstOf(COMPARISON, b.sequence("not", NOT_TEST))).skipIfOneChild();
  }

  /**
   * Simple statements
   * http://docs.python.org/reference/simple_stmts.html
   */
  public static void simpleStatements(LexerfulGrammarBuilder b) {
    b.rule(SIMPLE_STMT).is(b.firstOf(
      PRINT_STMT,
      EXEC_STMT,
      EXPRESSION_STMT,
      ASSERT_STMT,
      PASS_STMT,
      DEL_STMT,
      RETURN_STMT,
      YIELD_STMT,
      RAISE_STMT,
      BREAK_STMT,
      CONTINUE_STMT,
      IMPORT_STMT,
      GLOBAL_STMT,
      NONLOCAL_STMT));

    b.rule(PRINT_STMT).is("print", b.nextNot("="), b.nextNot("("), b.firstOf(
      b.sequence(">>", TEST, b.optional(b.oneOrMore(",", TEST), b.optional(","))),
      b.optional(TEST, b.zeroOrMore(",", TEST), b.optional(","))));

    b.rule(EXEC_STMT).is("exec", b.nextNot("("), EXPR, b.optional("in", TEST, b.optional(",", TEST)));

    b.rule(ASSERT_STMT).is("assert", TEST, b.optional(",", TEST));

    b.rule(PASS_STMT).is("pass");
    b.rule(DEL_STMT).is("del", EXPRLIST);
    b.rule(RETURN_STMT).is("return", b.optional(TESTLIST_STAR_EXPR));
    b.rule(YIELD_STMT).is(YIELD_EXPR);
    b.rule(RAISE_STMT).is("raise", b.optional(TEST, b.optional(b.firstOf(b.sequence("from", TEST), b.sequence(",", TEST, b.optional(",", TEST))))));
    b.rule(BREAK_STMT).is("break");
    b.rule(CONTINUE_STMT).is("continue");

    b.rule(IMPORT_STMT).is(b.firstOf(IMPORT_NAME, IMPORT_FROM));
    b.rule(IMPORT_NAME).is("import", DOTTED_AS_NAMES);
    b.rule(IMPORT_FROM).is(
      "from",
      b.firstOf(
        b.sequence(b.zeroOrMore("."), DOTTED_NAME),
        b.oneOrMore(".")),
      "import",
      b.firstOf("*", b.sequence("(", IMPORT_AS_NAMES, ")"), IMPORT_AS_NAMES));
    b.rule(IMPORT_AS_NAME).is(NAME, b.optional("as", NAME));
    b.rule(DOTTED_AS_NAME).is(DOTTED_NAME, b.optional("as", NAME));
    b.rule(IMPORT_AS_NAMES).is(IMPORT_AS_NAME, b.zeroOrMore(",", IMPORT_AS_NAME),
      b.optional(","));
    b.rule(DOTTED_AS_NAMES).is(DOTTED_AS_NAME, b.zeroOrMore(",", DOTTED_AS_NAME));

    b.rule(GLOBAL_STMT).is("global", NAME, b.zeroOrMore(",", NAME));
    b.rule(NONLOCAL_STMT).is("nonlocal", NAME, b.zeroOrMore(",", NAME));
  }

  /**
   * Compound statements
   * http://docs.python.org/reference/compound_stmts.html
   */
  public static void compoundStatements(LexerfulGrammarBuilder b) {
    b.rule(COMPOUND_STMT).is(b.firstOf(
      IF_STMT,
      WHILE_STMT,
      FOR_STMT,
      TRY_STMT,
      WITH_STMT,
      MATCH_STMT,
      FUNCDEF,
      CLASSDEF,
      ASYNC_STMT));
    b.rule(SUITE).is(b.firstOf(
      b.sequence(STMT_LIST, b.firstOf(NEWLINE, b.next(EOF), /* (Godin): no newline at the end of file */ b.next(DEDENT))),
      b.sequence(NEWLINE, INDENT, b.oneOrMore(STATEMENT), DEDENT)));
    b.rule(STATEMENT).is(b.firstOf(
      b.sequence(STMT_LIST, b.firstOf(NEWLINE, b.next(EOF), /* (Godin): no newline at the end of file */ b.next(DEDENT))),
      COMPOUND_STMT));
    b.rule(STMT_LIST).is(SIMPLE_STMT, b.zeroOrMore(";", SIMPLE_STMT), b.optional(";"));

    b.rule(IF_STMT).is("if", NAMED_EXPR_TEST, ":", SUITE, b.zeroOrMore("elif", NAMED_EXPR_TEST, ":", SUITE), b.optional("else", ":", SUITE));
    b.rule(WHILE_STMT).is("while", NAMED_EXPR_TEST, ":", SUITE, b.optional("else", ":", SUITE));
    b.rule(FOR_STMT).is("for", EXPRLIST, "in", STAR_NAMED_EXPRESSIONS, ":", SUITE, b.optional("else", ":", SUITE));

    b.rule(TRY_STMT).is("try", ":", SUITE, b.firstOf(
      b.sequence(b.oneOrMore(EXCEPT_CLAUSE, ":", SUITE),
        b.optional("else", ":", SUITE),
        b.optional("finally", ":", SUITE)),
      b.sequence("finally", ":", SUITE)));

    b.rule(EXCEPT_CLAUSE).is("except", b.optional("*"), b.optional(TEST, b.optional(b.firstOf("as", ","), TEST)));

    b.rule(WITH_STMT).is(b.firstOf(
      b.sequence("with", "(", WITH_ITEM, b.zeroOrMore(",", WITH_ITEM), b.optional(","), ")", ":", SUITE),
      b.sequence("with", WITH_ITEM, b.zeroOrMore(",", WITH_ITEM), ":", SUITE)
    ));
    b.rule(WITH_ITEM).is(TEST, b.optional("as", EXPR));

    b.rule(MATCH_STMT).is("match", SUBJECT_EXPR, ":", NEWLINE, INDENT, b.oneOrMore(CASE_BLOCK), DEDENT);
    b.rule(SUBJECT_EXPR).is(STAR_NAMED_EXPRESSIONS);
    b.rule(CASE_BLOCK).is("case", PATTERNS, b.optional(GUARD), ":", SUITE);
    b.rule(GUARD).is("if", NAMED_EXPR_TEST);

    b.rule(PATTERNS).is(b.firstOf(OPEN_SEQUENCE_PATTERN, PATTERN));
    b.rule(PATTERN).is(b.firstOf(AS_PATTERN, OR_PATTERN));
    b.rule(CLOSED_PATTERN).is(b.firstOf(CLASS_PATTERN, LITERAL_PATTERN, GROUP_PATTERN, WILDCARD_PATTERN, VALUE_PATTERN, CAPTURE_PATTERN, SEQUENCE_PATTERN, MAPPING_PATTERN));
    b.rule(AS_PATTERN).is(OR_PATTERN, "as", CAPTURE_PATTERN);
    b.rule(OR_PATTERN).is(CLOSED_PATTERN, b.zeroOrMore("|", CLOSED_PATTERN));
    b.rule(CAPTURE_PATTERN).is(NAME);
    b.rule(VALUE_PATTERN).is(ATTR);
    b.rule(MAPPING_PATTERN).is(b.firstOf(
      b.sequence("{", "}"),
      b.sequence("{", DOUBLE_STAR_PATTERN, b.optional(","), "}"),
      b.sequence("{", ITEMS_PATTERN, ",", DOUBLE_STAR_PATTERN, b.optional(","), "}"),
      b.sequence("{", ITEMS_PATTERN, b.optional(","), "}")
    ));
    b.rule(ITEMS_PATTERN).is(KEY_VALUE_PATTERN, b.zeroOrMore(",", KEY_VALUE_PATTERN));
    b.rule(KEY_VALUE_PATTERN).is(b.firstOf(LITERAL_PATTERN, VALUE_PATTERN), ":", PATTERN);
    b.rule(DOUBLE_STAR_PATTERN).is("**", CAPTURE_PATTERN);

    b.rule(SEQUENCE_PATTERN).is(b.firstOf(
      b.sequence("[", b.optional(MAYBE_SEQUENCE_PATTERN) , "]"),
      b.sequence("(", b.optional(OPEN_SEQUENCE_PATTERN), ")")
    ));
    b.rule(OPEN_SEQUENCE_PATTERN).is(MAYBE_STAR_PATTERN, ",", b.optional(MAYBE_SEQUENCE_PATTERN));
    b.rule(MAYBE_SEQUENCE_PATTERN).is(MAYBE_STAR_PATTERN, b.zeroOrMore(",", MAYBE_STAR_PATTERN), b.optional(","));
    b.rule(MAYBE_STAR_PATTERN).is(b.firstOf(STAR_PATTERN, PATTERN));
    b.rule(STAR_PATTERN).is("*", b.firstOf(WILDCARD_PATTERN, CAPTURE_PATTERN));

    b.rule(CLASS_PATTERN).is(NAME_OR_ATTR, "(", b.optional(PATTERN_ARGS), ")");
    b.rule(NAME_OR_ATTR).is(NAME, b.zeroOrMore(".", NAME));
    b.rule(ATTR).is(NAME, b.oneOrMore(".", NAME));
    b.rule(PATTERN_ARGS).is(PATTERN_ARG, b.zeroOrMore(b.sequence(",", PATTERN_ARG)), b.optional(","));
    b.rule(PATTERN_ARG).is(b.firstOf(KEYWORD_PATTERN, PATTERN));
    b.rule(KEYWORD_PATTERN).is(NAME, "=", PATTERN);

    b.rule(LITERAL_PATTERN).is(b.firstOf(
      COMPLEX_NUMBER,
      SIGNED_NUMBER,
      b.oneOrMore(PythonTokenType.STRING),
      PythonKeyword.NONE,
      "True",
      "False"
    ));

    b.rule(COMPLEX_NUMBER).is(b.firstOf(
      b.sequence(SIGNED_NUMBER, "+" , NUMBER),
      b.sequence(SIGNED_NUMBER, "-", NUMBER))
    );

    b.rule(SIGNED_NUMBER).is(b.firstOf(
      NUMBER,
      b.sequence("-", NUMBER)
    ));

    b.rule(WILDCARD_PATTERN).is("_");
    b.rule(GROUP_PATTERN).is("(", PATTERN, ")");

    b.rule(FUNCDEF).is(b.optional(DECORATORS), b.optional("async"), "def", FUNCNAME, "(", b.optional(TYPEDARGSLIST), ")", b.optional(FUN_RETURN_ANNOTATION), ":", SUITE);
    b.rule(FUNCNAME).is(NAME);
    b.rule(FUN_RETURN_ANNOTATION).is("-", ">", TEST);

    b.rule(DECORATORS).is(b.oneOrMore(DECORATOR));
    b.rule(DECORATOR).is("@", NAMED_EXPR_TEST, NEWLINE);
    b.rule(DOTTED_NAME).is(NAME, b.zeroOrMore(".", NAME));

    b.rule(CLASSDEF).is(b.optional(DECORATORS), "class", CLASSNAME, b.optional("(", b.optional(ARGLIST), ")"), ":", SUITE);
    b.rule(CLASSNAME).is(NAME);

    b.rule(ASYNC_STMT).is("async", b.firstOf(WITH_STMT, FOR_STMT));
  }

}
