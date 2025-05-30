/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 16, 2024 (david): created
 */
package org.knime.core.expressions.functions;

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.OPT_BOOLEAN;
import static org.knime.core.expressions.ValueType.OPT_FLOAT;
import static org.knime.core.expressions.ValueType.OPT_INTEGER;
import static org.knime.core.expressions.ValueType.OPT_STRING;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misInteger;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misString;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests for {@link StringFunctions}.
 *
 * @author David Hickey
 */
@SuppressWarnings("static-method")
final class StringFunctionTests {

    @TestFactory
    List<DynamicNode> contains() {
        return new FunctionTestBuilder(StringFunctions.CONTAINS) //
            .typing("STRING", List.of(STRING, STRING), BOOLEAN) //
            .typing("2xSTRING + MODIFIERS", List.of(STRING, STRING, STRING), BOOLEAN) //
            .typing("STRING?", List.of(OPT_STRING, STRING), OPT_BOOLEAN) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("CONTAINS", List.of(arg("testhellostring"), arg("hello")), true) //
            .impl("!CONTAINS", List.of(arg("testhellostring"), arg("goodbye")), false) //
            .impl("CONTAINS (+i)", List.of(arg("testhellostring"), arg("HELLO"), arg("i")), true) //
            .impl("!CONTAINS (-i)", List.of(arg("testhellostring"), arg("HELLO"), arg("")), false) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> compare() {
        return new FunctionTestBuilder(StringFunctions.COMPARE) //
            .typing("STRING", List.of(STRING, STRING), INTEGER) //
            .typing("STRING?", List.of(STRING, OPT_STRING), OPT_INTEGER) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("LT", List.of(arg("bar"), arg("foo")), "bar".compareTo("foo")) //
            .impl("GT", List.of(arg("foo"), arg("bar")), "foo".compareTo("bar")) //
            .impl("EQ", List.of(arg("foo"), arg("foo")), 0) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> startsWith() {
        return new FunctionTestBuilder(StringFunctions.STARTS_WITH) //
            .typing("STRING", List.of(STRING, STRING), BOOLEAN) //
            .typing("STRING?", List.of(STRING, OPT_STRING), OPT_BOOLEAN) //
            .typing("2 x String", List.of(STRING, STRING), BOOLEAN) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("starts with", List.of(arg("barfoo"), arg("bar")), true) //
            .impl("doesn't start with", List.of(arg("foo"), arg("bar")), false) //
            .impl("EQ", List.of(arg("foo"), arg("foo")), true) //
            .impl("case insensitive", List.of(arg("HELLO"), arg("hello"), arg("i")), true) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> endsWith() {
        return new FunctionTestBuilder(StringFunctions.ENDS_WITH) //
            .typing("STRING", List.of(STRING, STRING), BOOLEAN) //
            .typing("STRING?", List.of(STRING, OPT_STRING), OPT_BOOLEAN) //
            .typing("2 x String", List.of(STRING, STRING), BOOLEAN) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("ends with", List.of(arg("foobar"), arg("bar")), true) //
            .impl("doesn't end with", List.of(arg("foo"), arg("bar")), false) //
            .impl("EQ", List.of(arg("foo"), arg("foo")), true) //
            .impl("case insensitive", List.of(arg("HELLO"), arg("hello"), arg("i")), true) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> like() {
        return new FunctionTestBuilder(StringFunctions.LIKE) //

            // ──────────────────────────────────────────────
            // 1) Type‐Checking and Illegal Arguments
            // ──────────────────────────────────────────────
            .typing("STRING + STRING", List.of(STRING, STRING), BOOLEAN) //
            .typing("STRING + STRING?", List.of(STRING, OPT_STRING), OPT_BOOLEAN) //
            .typing("STRING x 3", List.of(STRING, STRING, STRING), BOOLEAN) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //

            // ──────────────────────────────────────────────
            // 2) Basic Matching (Expected TRUE)
            // ──────────────────────────────────────────────
            .impl("match %", List.of(arg("hellototheworld"), arg("hello%world")), true) //
            .impl("match _", List.of(arg("hellotoaworld"), arg("helloto_world")), true) //
            .impl("literal", List.of(arg("foo"), arg("foo")), true) //

            // ──────────────────────────────────────────────
            // 3) Escaping Literal Wildcards (Expected TRUE)
            // ──────────────────────────────────────────────
            .impl("escape %", List.of(arg("50%50"), arg("50[%]50")), true) //
            .impl("escape _", List.of(arg("50_50"), arg("50[_]50")), true) //
            .impl("escape % at end", List.of(arg("5050%"), arg("5050[%]")), true) //
            .impl("escape _ at start", List.of(arg("_5050"), arg("[_]5050")), true) //
            .impl("double escape _", List.of(arg("50[_]50"), arg("50[[_]]50")), true) //

            // ──────────────────────────────────────────────
            // 4) Case‐Insensitive Matching (Expected TRUE)
            // ──────────────────────────────────────────────
            .impl("case insensitive", List.of(arg("HELLO"), arg("hello"), arg("i")), true) //

            // ──────────────────────────────────────────────
            // 5) Additional Examples (All TRUE)
            //    (from your question: patterns on "ABCDE", etc.)
            // ──────────────────────────────────────────────
            .impl("A%%E => ABCDE", List.of(arg("ABCDE"), arg("A%%E")), true) //
            .impl("A%E => ABCDE", List.of(arg("ABCDE"), arg("A%E")), true) //
            .impl("%%%%E => ABCDE", List.of(arg("ABCDE"), arg("%%%%E")), true) // same as %E effectively
            .impl("%E => ABCDE", List.of(arg("ABCDE"), arg("%E")), true) //
            .impl("____E => ABCDE", List.of(arg("ABCDE"), arg("____E")), true) //
            .impl("A____ => ABCDE", List.of(arg("ABCDE"), arg("A____")), true) //
            .impl("A% => ABCDE", List.of(arg("ABCDE"), arg("A%")), true) //
            .impl("A%E => ABCDE", List.of(arg("ABCDE"), arg("A%E")), true) //
            .impl("A%_ => ABCDE", List.of(arg("ABCDE"), arg("A%_")), true) //
            .impl("% => ABCDE", List.of(arg("ABCDE"), arg("%")), true) // matches anything
            .impl("%B% => ABCDE", List.of(arg("ABCDE"), arg("%B%")), true) // must contain 'B'
            .impl("%B__% => ABCDE", List.of(arg("ABCDE"), arg("%B__%")), true) //
            .impl("apple => a%le", List.of(arg("apple"), arg("a%le")), true) //
            .impl("banana => _a_a_a", List.of(arg("banana"), arg("_a_a_a")), true) //
            // ──────────────────────────────────────────────
            // 6) Explicitly Failing Scenarios (Expected FALSE)
            // ──────────────────────────────────────────────
            .impl("Too short for underscores", List.of(arg("ABCDE"), arg("___E")), false) // pattern: 4 char, string: 5
            .impl("Must start with B", List.of(arg("ABCDE"), arg("B%")), false) //
            .impl("Doesn't contain Z", List.of(arg("ABCDE"), arg("%Z%")), false) //
            .impl("Case‐sensitive fail", List.of(arg("AbCdE"), arg("abcde")), false) //
            .impl("Single char vs two underscores", List.of(arg("A"), arg("__")), false) //
            .impl("No partial substring match", List.of(arg("ABCDE"), arg("CD")), false) //
            // ──────────────────────────────────────────────
            // 7) Tests with MISSING arguments
            // ──────────────────────────────────────────────
            .impl("missing STRING arg", List.of(misString(), arg("foo"))) //

            // Collect everything into test nodes
            .tests();
    }

    @TestFactory
    List<DynamicNode> regexMatch() {
        return new FunctionTestBuilder(StringFunctions.REGEX_MATCH) //
            .typing("STRING", List.of(STRING, STRING), BOOLEAN) //
            .typing("STRING?", List.of(STRING, OPT_STRING), OPT_BOOLEAN) //
            .typing("STRING x 3", List.of(STRING, STRING, STRING), BOOLEAN) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("regex 1 matches", List.of(arg("helloworld"), arg("^[a-zA-Z]*$")), true) //
            .impl("regex 1 doesn't match", List.of(arg("hellow0rld"), arg("^[a-zA-Z]*$")), false) //
            .impl("regex 2 matches", List.of(arg("12a"), arg("[abc123]+")), true) //
            .impl("regex 2 doesn't match", List.of(arg("14a"), arg("[abc123]+")), false) //
            .impl("literal", List.of(arg("foo"), arg("foo")), true) //
            .impl("case insensitive", List.of(arg("HELLO"), arg("hello"), arg("i")), true) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .errors("invalid pattern", List.of(arg("foo"), arg("(1")),
                "Invalid regex pattern '\\(1'\\. Unclosed group near index 2\\.") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> regexExtract() {
        return new FunctionTestBuilder(StringFunctions.REGEX_EXTRACT) //
            .typing("STRING", List.of(STRING, STRING, INTEGER), STRING) //
            .typing("STRING?", List.of(STRING, OPT_STRING, INTEGER), OPT_STRING) //
            .typing("with modifiers", List.of(STRING, STRING, INTEGER, STRING), STRING) //
            .illegalArgs("too few strings", List.of(STRING, STRING)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("extract 0 group", List.of(arg("1234abcd"), arg(".*"), arg(0)), "1234abcd") //
            .impl("extract 1 group", List.of(arg("hellow0rld"), arg("^([a-z]+).*"), arg(1)), "hellow") //
            .impl("extract 2 group", List.of(arg("12-_a"), arg("(\\d+)(?:[_-]+)(\\w+)"), arg(2)), "a") //
            .impl("find semantics", List.of(arg("abc123"), arg("(\\d+)"), arg(1)), "123") //
            .impl("group too big", List.of(arg("12-_a"), arg("(\\d+)(?:[_-]+)(\\w+)"), arg(3))) //
            .impl("group < 0", List.of(arg("12-_a"), arg("(\\d+)(?:[_-]+)(\\w+)"), arg(-1))) //
            .impl("regex doesn't match", List.of(arg("14a"), arg("q"), arg(0))) //
            .impl("case insensitive", List.of(arg("HELLO"), arg("h(el)lo"), arg(1), arg("i")), "EL") //
            .impl("missing STRING", List.of(misString(), arg("foo"), arg(0))) //
            .errors("invalid pattern", List.of(arg("foo"), arg("1)"), arg(0)),
                "Invalid regex pattern '1\\)'\\. Unmatched closing '\\)'\\.") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> regexReplace() {
        return new FunctionTestBuilder(StringFunctions.REGEX_REPLACE) //
            .typing("STRING", List.of(STRING, STRING, STRING, STRING), STRING) //
            .typing("STRING?", List.of(STRING, OPT_STRING, STRING, STRING), OPT_STRING) //
            .illegalArgs("too few strings", List.of(STRING, STRING)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("replace", List.of(arg("1234abcd"), arg("12345?"), arg("56")), "56abcd") //
            .impl("ignore case", List.of(arg("1234abcd"), arg("[ABC]{3}"), arg("yz"), arg("i")), "1234yzd") //
            .impl("multiple replacements", List.of(arg("quick quicker"), arg("quick(er)?"), arg("slow")), "slow slow") //
            .impl("groups", List.of(arg("abc-123-456-xyz"), arg("([0-9]+)-([0-9]+)"), arg("$2-$1")), "abc-456-123-xyz") //
            .impl("noop", List.of(arg("a"), arg("A"), arg("b")), "a") //
            .errors("invalid pattern", List.of(arg("foo"), arg("(?R)"), arg("bar")),
                "Invalid regex pattern '\\(\\?R\\)'\\. Unknown inline modifier near index 2\\.") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> replace() {
        return new FunctionTestBuilder(StringFunctions.REPLACE) //
            .typing("STRING", List.of(STRING, STRING, STRING, STRING), STRING) //
            .typing("STRING?", List.of(STRING, OPT_STRING, STRING, STRING), OPT_STRING) //
            .illegalArgs("too few strings", List.of(STRING, STRING)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("replace", List.of(arg("1234abcd"), arg("1234"), arg("56")), "56abcd") //
            .impl("ignore case", List.of(arg("1234abcd"), arg("BC"), arg("yz"), arg("i")), "1234ayzd") //
            .impl("whole words", List.of(arg("quick quicker"), arg("quick"), arg("slow"), arg("w")), "slow quicker") //
            .impl("multiple replacements", List.of(arg("quick quicker"), arg("quick"), arg("slow")), "slow slower") //
            .impl("noop", List.of(arg("a"), arg("A"), arg("b")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> replaceChars() {
        return new FunctionTestBuilder(StringFunctions.REPLACE_CHARS) //
            .typing("STRING", List.of(STRING, STRING, STRING, STRING), STRING) //
            .typing("STRING?", List.of(STRING, OPT_STRING, STRING, STRING), OPT_STRING) //
            .illegalArgs("too few strings", List.of(STRING, STRING)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("replace", List.of(arg("1234abcd"), arg("12dQ"), arg("€$£P")), "€$34abc£") //
            .impl("ignore case", List.of(arg("1234abcd"), arg("Bd"), arg("yz"), arg("i")), "1234aycz") //
            .impl("deletion", List.of(arg("quick quicker"), arg("qk"), arg("u")), "uuic uuicer") //
            .impl("noop", List.of(arg("a"), arg("A"), arg("b")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> replaceUmlauts() {
        return new FunctionTestBuilder(StringFunctions.REPLACE_UMLAUTS) //
            .typing("STRING", List.of(STRING, BOOLEAN), STRING) //
            .typing("STRING?", List.of(OPT_STRING, BOOLEAN), OPT_STRING) //
            .illegalArgs("2 strings", List.of(STRING, STRING)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .impl("replace no e", List.of(arg("äÄöÖüÜßẞ"), arg(true)), "aAoOuUssSS") //
            .impl("replace with e", List.of(arg("äÄöÖüÜßẞ"), arg(false)), "aeAEoeOEueUEssSS") //
            .impl("skip esszet", List.of(arg("äÄßẞ"), arg(false), arg(false)), "aeAEßẞ") //
            .impl("don't skip esszet", List.of(arg("äÄßẞ"), arg(false), arg(true)), "aeAEssSS") //
            .impl("noop", List.of(arg("a"), arg(true)), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> replaceDiacritics() {
        return new FunctionTestBuilder(StringFunctions.REPLACE_DIACRITICS) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("2 strings", List.of(STRING, STRING)) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .impl("replace", List.of(arg("äÄñçóîǫȯủ")), "aAncoioou") //
            .impl("noop", List.of(arg("a")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> lowerCase() {
        return new FunctionTestBuilder(StringFunctions.LOWER_CASE) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("2 strings", List.of(STRING, STRING)) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .impl("lower", List.of(arg("ABC123abc")), "abc123abc") //
            .impl("noop", List.of(arg("a")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> upperCase() {
        return new FunctionTestBuilder(StringFunctions.UPPER_CASE) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("2 strings", List.of(STRING, STRING)) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .impl("upper", List.of(arg("ABC123abc")), "ABC123ABC") //
            .impl("noop", List.of(arg("A")), "A") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> capitalize() {
        return new FunctionTestBuilder(StringFunctions.CAPITALIZE) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("2 strings", List.of(STRING, STRING)) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .impl("empty string", List.of(arg("")), "") //
            .impl("single lowercase letter", List.of(arg("a")), "A") //
            .impl("single uppercase letter", List.of(arg("A")), "A") //
            .impl("single word lowercase", List.of(arg("hello")), "Hello") //
            .impl("single word uppercase", List.of(arg("HELLO")), "Hello") //
            .impl("multiple words mixed case", List.of(arg("hElLo WoRLd")), "Hello World") //
            .impl("multiple words uppercase", List.of(arg("HELLO WORLD")), "Hello World") //
            .impl("string with leading/trailing spaces", List.of(arg("   hello world   ")), "   Hello World   ") //
            .impl("string with punctuation", List.of(arg("hello, world!")), "Hello, World!") //
            .impl("string with numbers and special characters", List.of(arg("123 hello @world")), "123 Hello @world") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> padEnd() {
        return new FunctionTestBuilder(StringFunctions.PAD_END) //
            .typing("STRING", List.of(STRING, INTEGER), STRING) //
            .typing("STRING?", List.of(OPT_STRING, INTEGER), OPT_STRING) //
            .typing("STRING + pad char", List.of(STRING, INTEGER, STRING), STRING) //
            .illegalArgs("2 strings", List.of(STRING, STRING)) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .impl("pad right", List.of(arg("ab"), arg(4)), "ab  ") //
            .impl("noop", List.of(arg("abcde"), arg(3)), "abcde") //
            .impl("negative target length", List.of(arg("ab"), arg(-1)), "ab") //
            .impl("with pad char", List.of(arg("ab"), arg(4), arg("0")), "ab00") //
            .errors("target length bigger than int MAX_VALUE", List.of(arg("ab"), arg(100L + Integer.MAX_VALUE)),
                ".*too large.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> padStart() {
        return new FunctionTestBuilder(StringFunctions.PAD_START) //
            .typing("STRING", List.of(STRING, INTEGER), STRING) //
            .typing("STRING?", List.of(OPT_STRING, INTEGER), OPT_STRING) //
            .typing("STRING + pad char", List.of(STRING, INTEGER, STRING), STRING) //
            .illegalArgs("2 strings", List.of(STRING, STRING)) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .impl("pad right", List.of(arg("ab"), arg(4)), "  ab") //
            .impl("noop", List.of(arg("abcde"), arg(3)), "abcde") //
            .impl("negative target length", List.of(arg("ab"), arg(-1)), "ab") //
            .impl("with pad char", List.of(arg("ab"), arg(4), arg("0")), "00ab") //
            .errors("target length bigger than int MAX_VALUE", List.of(arg("ab"), arg(100L + Integer.MAX_VALUE)),
                ".*too large.*") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> join() {
        return new FunctionTestBuilder(StringFunctions.JOIN) //
            .typing("STRING", List.of(STRING, STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING, STRING), OPT_STRING) //
            .typing("STRINGS", List.of(STRING, STRING, STRING, STRING), STRING) //
            .illegalArgs("1 string", List.of(STRING)) //
            .illegalArgs("INTEGER", List.of(INTEGER, STRING, STRING)) //
            .impl("join", List.of(arg("._"), arg("foo"), arg("bar"), arg("baz")), "foo._bar._baz") //
            .impl("noop", List.of(arg(","), arg("foo")), "foo") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> substr() {
        return new FunctionTestBuilder(StringFunctions.SUBSTRING) //
            .typing("STRING + start", List.of(STRING, INTEGER), STRING) //
            .typing("STRING?", List.of(OPT_STRING, INTEGER), OPT_STRING) //
            .typing("STRING + start + len", List.of(STRING, INTEGER, INTEGER), STRING) //
            .typing("optional length", List.of(STRING, INTEGER, OPT_INTEGER), STRING) //
            .typing("Optional start and length", List.of(STRING, OPT_INTEGER, OPT_INTEGER), STRING) //
            .typing("only STRING", List.of(STRING), STRING) //
            .illegalArgs("Integer instead of string", List.of(INTEGER)) //
            .illegalArgs("only optional length", List.of(), Map.of("length", INTEGER)) //)
            .warns("negative length", List.of(arg("abcdefg"), arg(1), arg(-1)),
                "The length of a substring cannot be negative. The length will be set to 0.") //
            .warns("non-positive start", List.of(arg("abcdefg"), arg(0), arg(3)),
                "The start index is set to 1 because the index must be 1 or higher.") //
            .impl("substr", List.of(arg("abcdefg"), arg(2), arg(3)), "bcd") //
            .impl("length is MISSING", List.of(arg("abcdefg"), arg(0), misInteger()), "abcdefg") //))
            .impl("length is negative", List.of(arg("abcdefg"), arg(1), arg(-1)), "") //
            .impl("start is negative", List.of(arg("abcdefg"), arg(-1), arg(1)), "a") //
            .impl("noop", List.of(arg("abcdefg"), arg(1), arg(100)), "abcdefg") //
            .impl("last char", List.of(arg("abc"), arg(3), arg(1)), "c") //
            .impl("start after last char", List.of(arg("abc"), arg(4), arg(1)), "") //
            .impl("empty string1", List.of(arg(""), arg(0), arg(0)), "") //
            .impl("empty string2", List.of(arg(""), arg(1), arg(3)), "") //
            .impl("string is missing", List.of(misString(), arg(1), arg(3))) //
            .impl("start and length is missing", List.of(arg("abc"), misInteger(), misInteger()), "abc") //
            .impl("start is missing", List.of(arg("abc"), misInteger(), arg(3)), "abc") //
            .impl("length is negative", List.of(arg("abc"), misInteger(), arg(-1)), "") //
            .impl("length is very large", List.of(arg("abc"), misInteger(), arg(Integer.MAX_VALUE)), "abc") //
            .impl("length is larger than int MAX_VALUE", List.of(arg("abc"), misInteger(), arg(Long.MAX_VALUE)), "abc") //
            .impl("length is less than int MIN_VALUE", List.of(arg("abc"), misInteger(), arg(Long.MIN_VALUE)), "") //
            .impl("start is larger than int MAX_VALUE", List.of(arg("abc"), arg(Long.MAX_VALUE - 100L), arg(1)), "") //
            .impl("start is less than int MIN_VALUE", List.of(arg("abc"), arg(Long.MIN_VALUE + 100L), arg(1)), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> firstChars() {
        return new FunctionTestBuilder(StringFunctions.FIRST_CHARS) //
            .typing("STRING + start", List.of(STRING, INTEGER), STRING) //
            .typing("STRING?", List.of(OPT_STRING, INTEGER), OPT_STRING) //
            .illegalArgs("1 string", List.of(STRING)) //
            .impl("first n", List.of(arg("abcdefg"), arg(2)), "ab") //
            .impl("noop", List.of(arg("abcdefg"), arg(100)), "abcdefg") //
            .impl("n bigger than integer MAX_VALUE", List.of(arg("abcdefg"), arg(100L + Integer.MAX_VALUE)), "abcdefg") //
            .impl("n negative", List.of(arg("abcdefg"), arg(-100)), "") //
            .impl("n less than integer MIN_VALUE", List.of(arg("abcdefg"), arg(-100L + Integer.MIN_VALUE)), "") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> lastChars() {
        return new FunctionTestBuilder(StringFunctions.LAST_CHARS) //
            .typing("STRING + start", List.of(STRING, INTEGER), STRING) //
            .typing("STRING?", List.of(OPT_STRING, INTEGER), OPT_STRING) //
            .illegalArgs("1 string", List.of(STRING)) //
            .impl("last n", List.of(arg("abcdefg"), arg(2)), "fg") //
            .impl("noop", List.of(arg("abcdefg"), arg(100)), "abcdefg") //
            .impl("n bigger than integer MAX_VALUE", List.of(arg("abcdefg"), arg(100L + Integer.MAX_VALUE)), "abcdefg") //
            .impl("n negative", List.of(arg("abcdefg"), arg(-100)), "") //
            .impl("n less than integer MIN_VALUE", List.of(arg("abcdefg"), arg(-100L + Integer.MIN_VALUE)), "") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> removeChars() {
        return new FunctionTestBuilder(StringFunctions.REMOVE_CHARS) //
            .typing("STRING", List.of(STRING, STRING), STRING) //
            .typing("STRING?", List.of(STRING, OPT_STRING), OPT_STRING) //
            .illegalArgs("too few strings", List.of(STRING)) //
            .illegalArgs("integers", List.of(INTEGER, INTEGER)) //
            .impl("replace", List.of(arg("1234abcd1"), arg("12dQ")), "34abc") //
            .impl("ignore case", List.of(arg("1234abcdB"), arg("Bd"), arg("i")), "1234ac") //
            .impl("noop", List.of(arg("a"), arg("A")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> strip() {
        return new FunctionTestBuilder(StringFunctions.STRIP) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("too many strings", List.of(STRING, STRING)) //
            .illegalArgs("integers", List.of(INTEGER)) //
            .impl("strip", List.of(arg("  b ")), "b") //
            .impl("strip to nothing", List.of(arg("   ")), "") //
            .impl("noop", List.of(arg("a")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> stripStart() {
        return new FunctionTestBuilder(StringFunctions.STRIP_START) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("too many strings", List.of(STRING, STRING)) //
            .illegalArgs("integers", List.of(INTEGER)) //
            .impl("strip", List.of(arg("  b ")), "b ") //
            .impl("strip to nothing", List.of(arg("   ")), "") //
            .impl("noop", List.of(arg("a")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> stripEnd() {
        return new FunctionTestBuilder(StringFunctions.STRIP_END) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("too many strings", List.of(STRING, STRING)) //
            .illegalArgs("integers", List.of(INTEGER)) //
            .impl("strip", List.of(arg("  b ")), "  b") //
            .impl("strip to nothing", List.of(arg("   ")), "") //
            .impl("noop", List.of(arg("a")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> removeDuplicateSpaces() {
        return new FunctionTestBuilder(StringFunctions.REMOVE_DUPLICATE_SPACES) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("too many strings", List.of(STRING, STRING)) //
            .illegalArgs("integers", List.of(INTEGER)) //
            .impl("strip", List.of(arg(" c  b        d ")), " c b d ") //
            .impl("strip to one space", List.of(arg("   ")), " ") //
            .impl("noop", List.of(arg("a b ")), "a b ") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> reverse() {
        return new FunctionTestBuilder(StringFunctions.REVERSE) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("too many strings", List.of(STRING, STRING)) //
            .illegalArgs("integers", List.of(INTEGER)) //
            .impl("strip", List.of(arg("abc 123")), "321 cba") //
            .impl("noop", List.of(arg("a bb a")), "a bb a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> emptyToNull() {
        return new FunctionTestBuilder(StringFunctions.EMPTY_TO_MISSING) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("too many strings", List.of(STRING, STRING)) //
            .illegalArgs("integers", List.of(INTEGER)) //
            .impl("to null", List.of(arg(""))) //
            .impl("null to null", List.of(misString())) //
            .impl("noop", List.of(arg("a")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> nullToEmpty() {
        return new FunctionTestBuilder(StringFunctions.MISSING_TO_EMPTY) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("too many strings", List.of(STRING, STRING)) //
            .illegalArgs("integers", List.of(INTEGER)) //
            .impl("to empty", List.of(misString()), "") //
            .impl("empty to empty", List.of(arg("")), "") //
            .impl("noop", List.of(arg("a")), "a") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> length() {
        return new FunctionTestBuilder(StringFunctions.LENGTH) //
            .typing("STRING", List.of(STRING), INTEGER) //
            .typing("STRING?", List.of(OPT_STRING), OPT_INTEGER) //
            .illegalArgs("too many strings", List.of(STRING, STRING)) //
            .illegalArgs("integers", List.of(INTEGER)) //
            .impl("length", List.of(arg("abc 123")), 7) //
            .impl("empty string", List.of(arg("")), 0) //
            .impl("missing string", List.of(misString())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> count() {
        return new FunctionTestBuilder(StringFunctions.COUNT) //
            .typing("STRING", List.of(STRING, STRING), INTEGER) //
            .typing("2xSTRING + MODIFIERS", List.of(STRING, STRING, STRING), INTEGER) //
            .typing("STRING?", List.of(OPT_STRING, STRING), OPT_INTEGER) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("count", List.of(arg("testhellostring"), arg("hello")), 1) //
            .impl("count non-match", List.of(arg("testhellostring"), arg("goodbye")), 0) //
            .impl("don't count overlapping", List.of(arg("zzzz"), arg("zz")), 2) //
            .impl("count ignorecase", List.of(arg("ABCBA"), arg("b"), arg("i")), 2) //
            .impl("count whole words", List.of(arg("A BC BA B"), arg("B"), arg("w")), 1) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> countChars() {
        return new FunctionTestBuilder(StringFunctions.COUNT_CHARS) //
            .typing("STRING", List.of(STRING, STRING), INTEGER) //
            .typing("2xSTRING + MODIFIERS", List.of(STRING, STRING, STRING), INTEGER) //
            .typing("STRING?", List.of(OPT_STRING, STRING), OPT_INTEGER) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("count", List.of(arg("teststring"), arg("t")), 3) //
            .impl("count ignorecase", List.of(arg("testsTring"), arg("t"), arg("i")), 3) //
            .impl("multiple chars", List.of(arg("teststring"), arg("ts")), 5) //
            .impl("count invert", List.of(arg("abcdbd"), arg("ba"), arg("v")), 3) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> find() {
        return new FunctionTestBuilder(StringFunctions.FIND) //
            .typing("STRING", List.of(STRING, STRING), INTEGER) //
            .typing("2xSTRING + MODIFIERS", List.of(STRING, STRING, STRING), INTEGER) //
            .typing("STRING?", List.of(OPT_STRING, STRING), OPT_INTEGER) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("find forward", List.of(arg("teststring"), arg("st")), 3) //
            .impl("find backward", List.of(arg("teststring"), arg("st"), arg("b")), 5) //
            .impl("find ignorecase", List.of(arg("TestsTring"), arg("t"), arg("i")), 1) //
            .impl("find whole words", List.of(arg("tests test"), arg("test"), arg("w")), 7) //
            .impl("not found", List.of(arg("tests test"), arg("qq"))) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> findChars() {
        return new FunctionTestBuilder(StringFunctions.FIND_CHARS) //
            .typing("STRING", List.of(STRING, STRING), INTEGER) //
            .typing("2xSTRING + MODIFIERS", List.of(STRING, STRING, STRING), INTEGER) //
            .typing("STRING?", List.of(OPT_STRING, STRING), OPT_INTEGER) //
            .illegalArgs("INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("1 STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN, BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING, MISSING)) //
            .impl("find forward", List.of(arg("teststring"), arg("st")), 1) //
            .impl("find backward", List.of(arg("teststring"), arg("st"), arg("b")), 6) //
            .impl("find ignorecase", List.of(arg("TestsTring"), arg("t"), arg("i")), 1) //
            .impl("find inverted", List.of(arg("tests test"), arg("test"), arg("v")), 6) //
            .impl("not found", List.of(arg("tests test"), arg("q"))) //
            .impl("missing STRING", List.of(misString(), arg("foo"))) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> xmlEncode() {
        return new FunctionTestBuilder(StringFunctions.XML_ENCODE) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .illegalArgs("2 STRING", List.of(STRING, STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("noop", List.of(arg("hello")), "hello") //
            .impl("a\"b'c&><d", List.of(arg("a\"b'c&><d")), "a&quot;b&apos;c&amp;&gt;&lt;d") //
            .impl("missing STRING", List.of(misString())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> urlEncode() {
        return new FunctionTestBuilder(StringFunctions.URL_ENCODE) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .illegalArgs("2 STRING", List.of(STRING, STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("noop", List.of(arg("hello")), "hello") //
            .impl("som< cöol str!ng", List.of(arg("som< cöol str!ng")), "som%3C+c%C3%B6ol+str%21ng") //
            .impl("missing STRING", List.of(misString())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> urlDecode() {
        return new FunctionTestBuilder(StringFunctions.URL_DECODE) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), OPT_STRING) //
            .illegalArgs("INTEGER", List.of(INTEGER)) //
            .illegalArgs("2 STRING", List.of(STRING, STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("noop", List.of(arg("hello")), "hello") //
            .impl("som< cöol str!ng", List.of(arg("som%3C%20c%C3%B6ol%20str%21ng")), "som< cöol str!ng") //
            .impl("missing STRING", List.of(misString())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> toStringTest() {
        return new FunctionTestBuilder(StringFunctions.TO_STRING) //
            .typing("STRING", List.of(STRING), STRING) //
            .typing("STRING?", List.of(OPT_STRING), STRING) //
            .typing("FLOAT", List.of(FLOAT), STRING) //
            .typing("INTEGER", List.of(INTEGER), STRING) //
            .typing("BOOLEAN", List.of(BOOLEAN), STRING) //
            .typing("MISSING", List.of(MISSING), STRING) //
            .illegalArgs("2 STRINGs", List.of(STRING, STRING)) //
            .impl("STRING", List.of(arg("foo")), "foo") //
            .impl("FLOAT", List.of(arg(25.0)), "25.0") //
            .impl("BOOLEAN", List.of(arg(true)), "true") //
            .impl("INTEGER", List.of(arg(1)), "1") //
            .impl("MISSING", List.of(misString()), "MISSING") //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseFloat() {
        return new FunctionTestBuilder(StringFunctions.PARSE_FLOAT) //
            .typing("STRING", List.of(STRING), OPT_FLOAT) //
            .typing("STRING?", List.of(OPT_STRING), OPT_FLOAT) //
            .illegalArgs("FLOAT", List.of(FLOAT)) //
            .illegalArgs("INTEGER", List.of(FLOAT)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("2 STRINGs", List.of(STRING, STRING)) //
            .impl("valid", List.of(arg("-1.24")), -1.24) //
            .impl("valid int", List.of(arg("10")), 10.0) //
            .impl("invalid", List.of(arg("1.3a"))) //
            .impl("MISSING", List.of(misString())) //
            .impl("very large float", List.of(arg("3e39")), 3e39) //
            .impl("with underscores", List.of(arg("314_159.265_358")), 314_159.265_358) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseInt() {
        return new FunctionTestBuilder(StringFunctions.PARSE_INT) //
            .typing("STRING", List.of(STRING), OPT_INTEGER) //
            .typing("STRING?", List.of(OPT_STRING), OPT_INTEGER) //
            .illegalArgs("FLOAT", List.of(FLOAT)) //
            .illegalArgs("INTEGER", List.of(FLOAT)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("2 STRINGs", List.of(STRING, STRING)) //
            .impl("valid", List.of(arg("-10")), -10) //
            .impl("invalid", List.of(arg("1.3"))) //
            .impl("MISSING", List.of(misString())) //
            .impl("very large int", List.of(arg(String.valueOf(Integer.MAX_VALUE + 11L))), 11 + (long)Integer.MAX_VALUE) //)
            .impl("with underscores", List.of(arg("1_000_000")), 1000000) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> parseBool() {
        return new FunctionTestBuilder(StringFunctions.PARSE_BOOL) //
            .typing("STRING", List.of(STRING), OPT_BOOLEAN) //
            .typing("STRING?", List.of(OPT_STRING), OPT_BOOLEAN) //
            .illegalArgs("FLOAT", List.of(FLOAT)) //
            .illegalArgs("INTEGER", List.of(FLOAT)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .illegalArgs("2 STRINGs", List.of(STRING, STRING)) //
            .impl("true", List.of(arg("tRuE")), true) //
            .impl("false", List.of(arg("falsE")), false) //
            .impl("invalid 1", List.of(arg("falseE"))) //
            .impl("invalid 2", List.of(arg("tru3"))) //
            .impl("MISSING", List.of(misString())) //
            .tests();
    }
}
