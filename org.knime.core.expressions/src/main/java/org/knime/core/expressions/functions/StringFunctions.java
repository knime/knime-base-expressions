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
 *   Apr 8, 2024 (benjamin): created
 */
package org.knime.core.expressions.functions;

import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyMissing;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.anyOptional;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.arg;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.functionBuilder;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isAnything;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isBoolean;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isBooleanOrOpt;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isInteger;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isIntegerOrOpt;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isString;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.isStringOrOpt;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.optarg;
import static org.knime.core.expressions.functions.ExpressionFunctionBuilder.vararg;
import static org.knime.core.expressions.functions.FunctionUtils.RETURN_BOOLEAN_MISSING;
import static org.knime.core.expressions.functions.FunctionUtils.RETURN_FLOAT_MISSING;
import static org.knime.core.expressions.functions.FunctionUtils.RETURN_INTEGER_MISSING;
import static org.knime.core.expressions.functions.FunctionUtils.RETURN_STRING;
import static org.knime.core.expressions.functions.FunctionUtils.RETURN_STRING_MISSING;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.OperatorCategory;
import org.knime.core.expressions.ValueType;

/**
 * Implementation of built-in functions that manipulate strings.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("javadoc")
public final class StringFunctions {

    private StringFunctions() {
    }

    /** The "String – General" category */
    public static final OperatorCategory CATEGORY_GENERAL = new OperatorCategory("String – General", """
            The "String – General" category in KNIME Expression language includes functions for manipulating and
            converting string data. These functions cover changing case, padding, joining, reversing, handling empty
            or missing values, and parsing strings into boolean, float, or integer types.
            """);

    /** The "String – Match & Compare" category */
    public static final OperatorCategory CATEGORY_MATCH_COMPARE = new OperatorCategory("String – Match & Compare", """
            The "String – Match & Compare" category in KNIME Expression language includes functions for evaluating and
            comparing string data. These functions check if strings start with, end with, or contain specific
            substrings, match patterns using regular expressions, and perform general string comparisons.
            """);

    /** The "String – Extract & Replace" category */
    public static final OperatorCategory CATEGORY_EXTRACT_REPLACE =
        new OperatorCategory("String – Extract & Replace", """
                The "String – Extract & Replace" category in KNIME Expression language includes functions for extracting
                and replacing parts of strings. These functions handle operations such as retrieving first or last
                characters, extracting substrings, performing regex-based extraction and replacement, finding
                substrings, and counting characters or occurrences.
                """);

    /** The "String – Clean" category */
    public static final OperatorCategory CATEGORY_CLEAN = new OperatorCategory("String – Clean", """
            The "String – Clean" category in KNIME Expression language includes functions for cleaning and sanitizing
            string data. These functions handle character replacement, diacritics and umlauts removal, eliminating
            duplicate spaces, and stripping characters from the start, end, or both ends of a string.
            """);

    /** The "String – Encode" category */
    public static final OperatorCategory CATEGORY_ENCODE = new OperatorCategory("String – Encode", """
            The "String – Encode" category in KNIME Expression language includes functions for encoding and decoding
            string data. These functions handle XML encoding, URL encoding, and URL decoding.
            """);

    public static final ExpressionFunction COMPARE = functionBuilder() //
        .name("compare") //
        .description("""
                Compares two strings lexicographically, returning the lexicographical
                distance between them. The function returns a negative number, zero,
                or a positive number when string_1 is less than, equal to, or greater
                than string_2 respectively.

                If either string is `MISSING`, the result is also `MISSING`.

                 Examples:
                 * `compare("abc", "abc")` returns 0
                 * `compare("abc", "ABC")` returns 32
                 * `compare("ABC", "abc")` returns -32
                 * `compare("ab", "abc")` returns -1
                 * `compare("", "ABC")` returns -3
                 * `compare("ABC", "")` returns 3
                """) //
        .keywords("match", "equals") //
        .category(CATEGORY_MATCH_COMPARE.name()) //
        .args( //
            arg("string_1", "First string", isStringOrOpt()), //
            arg("string_2", "Second string", isStringOrOpt()) //
        ) //
        .returnType("Lexicographical distance `x - y`; if the strings are equal this is 0", RETURN_INTEGER_MISSING,
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::compareImpl) //
        .build();

    private static Computer compareImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        return IntegerComputer.of( //
            ctx -> c1.compute(ctx).compareTo(c2.compute(ctx)), //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction CONTAINS = functionBuilder() //
        .name("contains") //
        .description("""
                This function returns `true` if the specified string contains the
                search term. It can optionally perform a case-insensitive match
                using the "i" modifier. If the string or search term is `MISSING`,
                the result is also `MISSING`.

                Examples:
                * `contains("Hello World", "world")` returns `false`\\
                  By default, the match is case-sensitive.
                * `contains("Hello World", "world", "i")` returns `true`\\
                  With the "i" modifier, the match becomes case-insensitive.
                * `contains("OpenAI", "AI")` returns `true`
                """) //
        .keywords("match", "pattern", "includes") //
        .category(CATEGORY_MATCH_COMPARE.name()) //
        .args( //
            arg("string", "String to check", isStringOrOpt()), //
            arg("search", "Term to search for", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" case-insensitive match (root locale)", isString()) //
        ) //
        .returnType("`true` if string contains the search term, `false` otherwise", RETURN_BOOLEAN_MISSING,
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::containsImpl) //
        .build();

    private static Computer containsImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        final Predicate<EvaluationContext> ignoreCase;
        if (args.size() == 3) {
            var modifier = (StringComputer)args.get(2);
            ignoreCase = ctx -> modifier.compute(ctx).contains("i");
        } else {
            ignoreCase = ctx -> false;
        }

        return BooleanComputer.of(ctx -> {
            if (ignoreCase.test(ctx)) {
                return c1.compute(ctx).toLowerCase(Locale.ROOT).contains(c2.compute(ctx).toLowerCase(Locale.ROOT));
            } else {
                return c1.compute(ctx).contains(c2.compute(ctx));
            }
        }, anyMissing(args));
    }

    public static final ExpressionFunction STARTS_WITH = functionBuilder() //
        .name("starts_with") //
        .description("""
                Check whether the specified string begins with the provided prefix
                string. Matching is case-sensitive. If any of the arguments are
                `MISSING`, the result is also `MISSING`.

                The optional modifiers argument can be used to specify case-insensitive
                matching.

                Examples:
                * `starts_with("Hello world", "Hello")` returns `true`
                * `starts_with("Hello world", "abc")` returns `false`
                """) //
        .keywords("match", "pattern") //
        .category(CATEGORY_MATCH_COMPARE.name()) //
        .args( //
            arg("string", "String to check", isStringOrOpt()), //
            arg("prefix", "Prefix to check", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (using root locale)", isString())) //
        .returnType("`true` if the string starts with prefix, `false` otherwise", RETURN_BOOLEAN_MISSING, //
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::startsWithImpl) //
        .build();

    private static Computer startsWithImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        return BooleanComputer.of( //
            ctx -> {
                String s1 = c1.compute(ctx);
                String s2 = c2.compute(ctx);
                String modifiers = extractModifiersOrDefault(args, 2, ctx);

                if (modifiers.contains("i")) {
                    s1 = s1.toLowerCase(Locale.ROOT);
                    s2 = s2.toLowerCase(Locale.ROOT);
                }

                return s1.startsWith(s2);
            }, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction ENDS_WITH = functionBuilder() //
        .name("ends_with") //
        .description("""
                Check whether the specified string ends with the provided suffix
                string. Matching is case-sensitive. If any of the arguments are
                `MISSING`, the result is also `MISSING`.

                The optional modifiers argument can be used to specify case-insensitive
                matching.

                Examples:
                * `ends_with("Hello world", "world")` returns `true`
                * `ends_with("Hello world", "abs")` returns `false`
                """) //
        .keywords("match", "pattern") //
        .category(CATEGORY_MATCH_COMPARE.name()) //
        .args( //
            arg("string", "String to check", isStringOrOpt()), //
            arg("suffix", "Suffix to check", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (using root locale)", isString()) //
        ) //
        .returnType("`true` if the string ends with suffix, `false` otherwise", RETURN_BOOLEAN_MISSING, //
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::endsWithImpl) //
        .build();

    private static Computer endsWithImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        return BooleanComputer.of( //
            ctx -> {
                String s1 = c1.compute(ctx);
                String s2 = c2.compute(ctx);
                String modifiers = extractModifiersOrDefault(args, 2, ctx);

                if (modifiers.contains("i")) {
                    s1 = s1.toLowerCase(Locale.ROOT);
                    s2 = s2.toLowerCase(Locale.ROOT);
                }

                return s1.endsWith(s2);
            }, //
            anyMissing(args) //
        );
    }

    // TODO(AP-22345) emit a warning on potentially slow regexes
    public static final ExpressionFunction LIKE = functionBuilder() //
        .name("like") //
        .description("""
                Check if string matches the given pattern using the syntax of SQL's
                LIKE:
                * `_` is a single-character wildcard
                * `%` represents 0, 1, or more characters
                * to match a literal `%` or `_`, use `[%]` or `[_]`

                The optional modifiers argument gives the ability to tune the search
                parameters:
                * "i" to ignore case

                If any of the arguments are `MISSING`, the result is also
                `MISSING`.

                Examples:
                * `like("apple", "a%le")` returns `true`\\
                  matches any string starting with "a" and ending with "le")
                * `like("banana", "_a_a_a")` returns `true`\\
                  matches strings like "banana", "bacada"
                * `like("abc", "A_C", "i")` returns `true`\\
                  case-insensitive match, matches "A_C" with "a_c"
                 """) //
        .keywords("match", "pattern", "wildcard", "SQL") //
        .category(CATEGORY_MATCH_COMPARE.name()) //
        .args( //
            arg("string", "String to check", isStringOrOpt()), //
            arg("pattern", "Matching rule", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (using root locale)", isString())) //
        .returnType("`true` if the string matches the pattern, `false` otherwise", RETURN_BOOLEAN_MISSING, //
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::likeImpl) //
        .build();

    // TODO(AP-22345) emit a warning on potentially slow regexes
    private static final Pattern underscorePattern = Pattern.compile("([^\\[]|^)(?:_)([^\\[]|$)");

    private static final Pattern percentPattern = Pattern.compile("([^\\[]|^)(?:%)([^\\[]|$)");

    private static final Pattern regexCharsExceptSquareBracketsPattern = Pattern.compile("[{}().+*?^$\\\\|]");

    private static Computer likeImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        Predicate<EvaluationContext> value = ctx -> {
            String escapedPattern = c2.compute(ctx);
            String toMatch = c1.compute(ctx);
            var ignoreCase = extractModifiersOrDefault(args, 2, ctx).contains("i");

            if (ignoreCase) {
                escapedPattern = escapedPattern.toLowerCase(Locale.ROOT);
                toMatch = toMatch.toLowerCase(Locale.ROOT);
            }

            // Anything we need to escape in our regex, except for []
            // (we do those later)
            escapedPattern = regexCharsExceptSquareBracketsPattern //
                .matcher(escapedPattern) //
                .replaceAll("\\\\$0");

            // Replace _ and % with their appropriate wildcards, then replace
            // [_] and [%] with literal _ and %
            escapedPattern = underscorePattern.matcher(escapedPattern).replaceAll("$1.$2");
            escapedPattern = percentPattern.matcher(escapedPattern).replaceAll("$1.*$2");
            escapedPattern = escapedPattern //
                .replace("[_]", "_") //
                .replace("[%]", "%") //
                .replace("[", "\\[") //
                .replace("]", "\\]");

            return toMatch.matches(escapedPattern);
        };

        return BooleanComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction REGEX_MATCH = functionBuilder() //
        .name("regex_match") //
        .description("""
                  Checks if a string matches the given regular expression pattern.

                  Raises an error if the regex is invalid. If any of the arguments
                  are `MISSING`, the result is also `MISSING`.

                  The optional `modifiers` argument can be used to specify case-insensitive
                  matching.

                  Examples:
                  * `regex_match("hello123", "[a-z]+\\d+")` returns `true`
                  * `regex_match("abc", "a.c")` returns `true`
                  * `regex_match("123-456-7890", "\\d{3}-\\d{3}-\\d{4}")` returns `true`
                """) //
        .keywords("pattern") //
        .category(CATEGORY_MATCH_COMPARE.name()) //
        .args( //
            arg("string", "String to check against the regex pattern", isStringOrOpt()), //
            arg("pattern", "Regular expression pattern to match", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (using root locale)", isString()) //
        ) //
        .returnType("`true` if the string matches the pattern, `false` otherwise", RETURN_BOOLEAN_MISSING, //
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::regexMatchImpl) //
        .build();

    // TODO(AP-22345) emit a warning on potentially slow regexes
    private static Computer regexMatchImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        return BooleanComputer.of( //
            ctx -> {
                String str = c1.compute(ctx);
                String patternString = c2.compute(ctx);
                String modifiers = extractModifiersOrDefault(args, 2, ctx);

                return Pattern.compile(patternString, modifiers.contains("i") ? Pattern.CASE_INSENSITIVE : 0) //
                    .matcher(str) //
                    .matches();
            }, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction REGEX_EXTRACT = functionBuilder() //
        .name("regex_extract") //
        .description("""
                Given a regex that captures some groups, extract and return the
                group referred to by the index. Groups are one-indexed. Index `0`
                refers to the entire match.

                Raises an error if the regex is invalid. If the group index is out
                of bounds, the regex does not match, or any of the arguments are
                `MISSING`, returns `MISSING`.

                The optional `modifiers` argument can be used to specify case-insensitive
                matching.

                Examples:
                * `regex_extract("5hello123", "[0-9]([a-z]+).*", 1)` returns "hello"
                * `regex_extract("abc123def", "(\\d+)", 1)` returns "123"
                * `regex_extract("foo_bar_baz", "foo_(\\w+)_baz", 1)` returns "bar"
                * `regex_extract("abc", "(a)(b)(c)", 2)` returns "b"
                """) //
        .keywords("pattern", "match", "capture group") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "String to check against the regex pattern", isStringOrOpt()), //
            arg("pattern", "Regular expression pattern to match", isStringOrOpt()), //
            arg("group", "Index of the group to extract", isIntegerOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (using root locale)", isString())) //
        .returnType("Extracted group", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::regexExtractImpl) //
        .build();

    private static String extractGroupOrReturnNull(final String toMatch, final String pattern, final int group,
        final String modifiers) {

        boolean ignoreCase = modifiers.contains("i");

        var matcher = Pattern.compile(pattern, ignoreCase ? Pattern.CASE_INSENSITIVE : 0).matcher(toMatch);

        if (!matcher.matches() || group < 0 || group > matcher.groupCount()) {
            return null;
        } else {
            return matcher.group(group);
        }
    }

    // TODO(AP-22345) emit a warning on potentially slow regexes
    private static Computer regexExtractImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));
        var c3 = toInteger(args.get(2));

        Predicate<EvaluationContext> isMissing = ctx -> anyMissing(args).test(ctx) //
            || extractGroupOrReturnNull(c1.compute(ctx), c2.compute(ctx), (int)c3.compute(ctx),
                extractModifiersOrDefault(args, 3, ctx)) == null;

        Function<EvaluationContext, String> value = ctx -> extractGroupOrReturnNull(c1.compute(ctx), c2.compute(ctx),
            (int)c3.compute(ctx), extractModifiersOrDefault(args, 3, ctx));

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction REGEX_REPLACE = functionBuilder() //
        .name("regex_replace") //
        .description("""
                Replaces the parts of the string that match the given regex pattern
                with the replacement string. The optional `modifiers` argument can
                be used to specify case-insensitive matching.

                The replacement string can specify regex groups using `$1`, `$2`,
                etc., allowing for some very flexible usage. See the examples for
                more detail.

                Raises an error if the regex is invalid, and returns `MISSING` if
                any of the arguments are `MISSING`.

                The optional `modifiers` argument can be used to tune the search:
                * "i" for case-insensitive matching

                Examples:
                * `regex_replace("abc", "[a-zA-Z]{3}", "cba")` returns "cba"\\
                  Replaces the entire string "abc" with "cba".
                * `regex_replace("fooBARFOO", "foo', "baz", "i")` returns "bazBARbaz"\\
                  Case-insensitive replacement of "foo" with "baz". Both "foo" instances are replaced.
                * `regex_replace("abcd", "[a-zA-Z]{3}", "ABC")` returns "ABCd"\\
                * `regex_replace("abc-123-456-xyz", "([0-9]+)-([0-9]+)", "$2-$1")` returns "abc-456-123-xyz"\\
                  Uses regex groups to swap the two numbers in the string.
                """) //
        .keywords("pattern", "match", "substitute") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "Input string to perform replacements on", isStringOrOpt()), //
            arg("pattern", "Regular expression pattern to search for", isStringOrOpt()), //
            arg("replace", "Replacement text", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (using root locale)", isString()) //
        ) //
        .returnType("String with pattern replaced", RETURN_STRING_MISSING, args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::regexReplaceImpl) //
        .build();

    private static Computer regexReplaceImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            String search = toString(args.get(1)).compute(ctx);
            String replacement = toString(args.get(2)).compute(ctx);

            String modifiers = extractModifiersOrDefault(args, 3, ctx);
            boolean ignoreCase = modifiers.contains("i");

            var pattern = Pattern.compile(search, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
            return pattern.matcher(str).replaceAll(replacement);
        };

        Predicate<EvaluationContext> isMissing = anyMissing(args);

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction REPLACE = functionBuilder() //
        .name("replace") //
        .description("""
                Replaces all occurrences of a string pattern within another given
                input string. The optional modifiers argument gives several options
                to control the search:
                * "i" to ignore case
                * "w" to match whole words only (word boundaries are whitespace
                characters).

                Modifiers can be combined, e.g. "iw" for case-insensitive whole
                word matching.

                If any of the arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `replace("abcABC", "ab", "z")` returns "zcABC"
                * `replace("abcABC", "ab", "")` returns "cABC"
                * `replace("abcABC", "ab", "z", "i")` returns "zczC"
                * `replace("ab abAB AB", "ab", "z", "w")` returns "z abAB AB"
                """) //
        .keywords() //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "Input string to perform replacements on", isStringOrOpt()), //
            arg("pattern", "Pattern to search for", isStringOrOpt()), //
            arg("replace", "Replacement text", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (root locale), " //
                + "\"w\" to match whole words only", isString()) //
        ) //
        .returnType("String with pattern replaced", RETURN_STRING_MISSING, args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::replaceImpl) //
        .build();

    private static Computer replaceImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            String search = toString(args.get(1)).compute(ctx);
            String replacement = toString(args.get(2)).compute(ctx);

            String modifiers = extractModifiersOrDefault(args, 3, ctx);
            boolean ignoreCase = modifiers.contains("i");
            boolean wholeWords = modifiers.contains("w");

            String patternString = wholeWords //
                ? ("\\b" + escapeRegexChars(search) + "\\b") //
                : escapeRegexChars(search);

            var pattern = Pattern.compile(patternString, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
            return pattern.matcher(str).replaceAll(Matcher.quoteReplacement(replacement));
        };

        Predicate<EvaluationContext> isMissing = anyMissing(args);

        return StringComputer.of(value, isMissing);
    }

    /** Basically does a translate like in python or SQL */
    public static final ExpressionFunction REPLACE_CHARS = functionBuilder() //
        .name("replace_chars") //
        .description("""
                Replace characters in string with the given substitutions. If the
                string of new characters is shorter, then we delete the
                corresponding old characters. If new_chars is longer than old_chars
                the additional chars in new_chars will be ignored.

                The optional modifiers argument can be used to specify
                case-insensitive matching.

                If any of the arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `replace_chars("abcABC", "ac", "xy")` returns "xbyABC"\\
                  Replaces "a" with "x" and "c" with "y".
                * `replace_chars("abcABC", "ac", "")` returns "bABC"\\
                  Removes all "a" and "c" characters.
                * `replace_chars("abcABC", "ac", "x")` returns "xbABC"\\
                  Replaces "a" with "x" and removes "c".
                * `replace_chars("abcABC", "ac", "xyz")` returns "xbyABC"\\
                  Replaces "a" with "x" and "c" with "y", ignoring the extra "z".
                """) //
        .keywords("translate", "substitute", "match") //
        .category(CATEGORY_CLEAN.name()) //
        .args( //
            arg("string", "Input string to perform replacements on", isStringOrOpt()), //
            arg("old_chars", "Characters to be replaced", isStringOrOpt()), //
            arg("new_chars", "Characters to replace with", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (root locale)", isString()) //
        ) //
        .returnType("String with (old) characters replaced by (new) characters", RETURN_STRING_MISSING,
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::replaceCharsImpl) //
        .build();

    private static Computer replaceCharsImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            char[] oldChars = toString(args.get(1)).compute(ctx).toCharArray();
            char[] newChars = toString(args.get(2)).compute(ctx).toCharArray();

            String modifiers = extractModifiersOrDefault(args, 3, ctx);
            boolean ignoreCase = modifiers.contains("i");

            for (int i = 0; i < oldChars.length; ++i) {
                // if newChars is shorter, replace the extra chars in oldChars
                // with nothing.
                String searchChar = String.valueOf(oldChars[i]);
                String replacementChar = i < newChars.length //
                    ? String.valueOf(newChars[i]) //
                    : "";

                if (ignoreCase) {
                    str = str //
                        .replace(searchChar.toUpperCase(Locale.ROOT), replacementChar) //
                        .replace(searchChar.toLowerCase(Locale.ROOT), replacementChar);
                } else {
                    str = str.replace(searchChar, replacementChar);
                }
            }

            return str;
        };

        Predicate<EvaluationContext> isMissing = anyMissing(args);

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction REPLACE_UMLAUTS = functionBuilder() //
        .name("replace_umlauts") //
        .description("""
                Replaces umlauts (i.e., ä, ö, ü) with their ASCII equivalents.
                If the `no_e` argument is `true`, replaces characters like ö with o.
                If `false`, replaces characters like ö with oe.

                By default, also replaces ß and ẞ with ss and SS respectively, but
                this behaviour can be changed by setting the optional argument
                `replace_eszett` to `false`).

                If any of the arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `replace_umlauts("fröhlich", false)` returns "froehlich"\\
                  Replaces ö with oe.
                * `replace_umlauts("fröhlich", true)` returns "frohlich"\\
                  Replaces ö with o.
                * `replace_umlauts("übermäßig", false)` returns "uebermaessig"\\
                  Replaces ü with ue and ß with ss.
                * `replace_umlauts("übermäßig", true)` returns "ubermassig"\\
                  Replaces ü with u and ß with ss.
                * `replace_umlauts("übermäßig", true, false)` returns "ubermaßig"\\
                  Replaces ü with u and keeps ß unchanged.
                """) //
        .keywords("translate", "special characters", "conversion", "normalize") //
        .category(CATEGORY_CLEAN.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()), //
            arg("no_e", "If `true`, e.g. ö->o. If `false`, o->oe", isBooleanOrOpt()), //
            optarg("replace_eszett", "If `true`, also replace ß with ss (default `true`)", isBoolean()) //
        ) //
        .returnType("String with umlauts replaced", RETURN_STRING_MISSING, args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::replaceUmlautsImpl) //
        .build();

    private static Computer replaceUmlautsImpl(final List<Computer> args) {
        var umlauts = "äüö";
        var umlautReplacements = "auo";

        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            boolean noE = toBoolean(args.get(1)).compute(ctx);

            for (int i = 0; i < umlauts.length(); i++) {
                str = str.replace( //
                    String.valueOf(umlauts.charAt(i)), //
                    umlautReplacements.charAt(i) + (noE ? "" : "e") //
                );

                str = str.replace( //
                    String.valueOf(umlauts.charAt(i)).toUpperCase(Locale.ROOT), //
                    (umlautReplacements.charAt(i) + (noE ? "" : "e")).toUpperCase(Locale.ROOT) //
                );
            }

            if (args.size() < 3 || toBoolean(args.get(2)).compute(ctx)) {
                str = str //
                    .replace("ß", "ss") //
                    .replace("ẞ", "SS");
            }

            return str;
        };

        Predicate<EvaluationContext> isMissing = anyMissing(args);

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction REPLACE_DIACRITICS = functionBuilder() //
        .name("replace_diacritics") //
        .description("""
                Replace diacritics in the given string with their ASCII equivalents.
                This process converts characters like é to e, ü to u, and so on. If
                the string provided is `MISSING`, the result is also `MISSING`.

                (NB: This is implemented by Java's

                `Normalizer.normalize(string, Normalizer.Form.NFKD)`

                under the hood.)

                Examples:
                * `replace_diacritics("café")` returns "cafe"
                * `replace_diacritics("français")` returns "francais"
                """) //
        .keywords("conversion", "special characters", "translate", "normalize") //
        .category(CATEGORY_CLEAN.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String with diacritics replaced", RETURN_STRING_MISSING,
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::replaceDiacriticsImpl) //
        .build();

    private static Computer replaceDiacriticsImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            str = Normalizer.normalize(str, Normalizer.Form.NFKD);
            str = str.replaceAll("\\p{M}", "");
            return str;
        };

        Predicate<EvaluationContext> isMissing = anyMissing(args);

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction LOWER_CASE = functionBuilder() //
        .name("lower_case") //
        .description("""
                Convert the given string to lower case. This method uses the root
                locale for the conversion to ensure consistency across different
                locales. If the string provided is `MISSING`, the result is also
                `MISSING`.

                Examples:
                * `lower_case("Hello World")` returns "hello world"
                * `lower_case("JAVA")` returns "java"
                * `lower_case("123ABC")` returns "123abc"\\
                  Numbers are not affected.
                """) //
        .keywords("conversion") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String in lower case", RETURN_STRING_MISSING, args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::lowerCaseImpl) //
        .build();

    private static Computer lowerCaseImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> toString(args.get(0)).compute(ctx).toLowerCase(Locale.ROOT), //
            anyMissing(args));
    }

    public static final ExpressionFunction UPPER_CASE = functionBuilder() //
        .name("upper_case") //
        .description("""
                Convert the given string to upper case. This method uses the root
                locale for the conversion to ensure consistency across different
                locales. If the string provided is `MISSING`, the result is also
                `MISSING`.

                Examples:
                * `upper_case("Hello World")` returns "HELLO WORLD"
                * `upper_case("java")` returns "JAVA"
                * `upper_case("123abc")` returns "123ABC"\\
                  Numbers are not affected.
                """) //
        .keywords("conversion") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String in upper case", RETURN_STRING_MISSING, args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::upperCaseImpl) //
        .build();

    private static Computer upperCaseImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> toString(args.get(0)).compute(ctx).toUpperCase(Locale.ROOT), //
            anyMissing(args));
    }

    public static final ExpressionFunction CAPITALIZE = functionBuilder() //
        .name("capitalize") //
        .description("""
                Convert the given string to title case, where the first letter of
                each word is capitalized. Words are defined as sequences of
                characters separated by whitespace. If the string provided is
                `MISSING`, the result is also `MISSING`.

                Examples:
                * `capitalize("hello world")` returns "Hello World"
                * `capitalize("hello-world")` returns "Hello-world"\\
                  Hypens are not considered word separators.
                * `capitalize("java is fun")` returns "Java Is Fun"
                * `capitalize("123abc")` returns "123abc"\\
                  Numbers are not affected.
                """) //
        .keywords("capitalise", "title_case") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String in title case", RETURN_STRING_MISSING, args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::titleCaseImpl) //
        .build();

    private static Computer titleCaseImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            var output = new StringBuilder(str.length());

            var capitaliseNextChar = true;
            for (int i = 0; i < str.length(); ++i) {
                var currentChar = str.charAt(i);
                output.append( //
                    capitaliseNextChar //
                        ? Character.toUpperCase(currentChar) //
                        : currentChar //
                );

                capitaliseNextChar = Character.isWhitespace(currentChar);
            }

            return output.toString();
        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction PAD_END = functionBuilder() //
        .name("pad_end") //
        .description("""
                Right-pad the given string to the specified length with the provided
                character. If the string is already that length or longer, no
                padding is applied.

                By default, the string is padded with spaces, but if an optional
                padding character is provided, the string is padded with that
                character instead. Note that this padding character has to be a
                _single_  character. Anything longer will be truncated.

                If any of the arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `pad_end("hello", 10)` returns "hello     "\\
                  Pads the string "hello" with spaces to make it 10 characters long.
                * `pad_end("abc", 3)` returns "abc"\\
                  No padding is applied since the string is already 3 characters long.
                * `pad_end("test", 8, "*")` returns "test****"\\
                  Pads the string "test" with asterisks to make it 8 characters long.
                * `pad_end("abc", 3, "x")` returns "abc"\\
                  No padding is applied since the padding character is longer than the string.
                * `pad_end("abc", 5, "xy")` returns "abcxx"\\
                  Pads the string "abc" with "x". The extra "y" is ignored.
                """) //
        .keywords("right-pad") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to pad", isStringOrOpt()), //
            arg("length", "Desired length", isIntegerOrOpt()), //
            optarg("char", "Char with which to pad (default: space)", isString()) //
        ) //
        .returnType("String padded to specified length", RETURN_STRING_MISSING,
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::padEndImpl) //
        .build();

    private static Computer padEndImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            int targetLength = (int)toInteger(args.get(1)).compute(ctx);

            String charToAppend = args.size() == 3 //
                ? takeFirstChar(toString(args.get(2)).compute(ctx), " ") //
                : " ";

            var output = new StringBuilder(str);
            output.ensureCapacity(targetLength);

            while (output.length() < targetLength) {
                output.append(charToAppend);
            }
            return output.toString();
        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction PAD_START = functionBuilder() //
        .name("pad_start") //
        .description("""
                Left-pad the given string to the specified length with the provided
                character. If the string is already that length or longer, no
                padding is applied.

                By default, the string is padded with spaces, but if an optional
                padding character is provided, the string is padded with that
                character instead. Note that this padding character has to be a
                _single_  character. Anything longer will be truncated.

                If any of the arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `pad_start("hello", 10)` returns "     hello"\\
                  Pads the string "hello" with spaces to make it 10 characters long.
                * `pad_start("abc", 3)` returns "abc"\\
                  No padding is applied since the string is already 3 characters long.
                * `pad_start("test", 8, "*")` returns "****test"\\
                  Pads the string "test" with asterisks to make it 8 characters long.
                * `pad_start("abc", 5, "xy")` returns "xxabc"\\
                  Pads the string "abc" with "x". The extra "y" is ignored.
                """) //
        .keywords("left-pad") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to pad", isStringOrOpt()), //
            arg("length", "Desired length", isIntegerOrOpt()), //
            optarg("char", "Char with which to pad (default: space)", isString()) //
        ) //
        .returnType("String padded to specified length", RETURN_STRING_MISSING,
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::padStartImpl) //
        .build();

    private static Computer padStartImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            int targetLength = (int)toInteger(args.get(1)).compute(ctx);
            String charToPrepend = args.size() == 3 //
                ? takeFirstChar(toString(args.get(2)).compute(ctx), " ") //
                : " ";

            StringBuilder output = new StringBuilder(targetLength);

            for (int i = 0; i < targetLength - str.length(); ++i) {
                output.append(charToPrepend);
            }
            return output.append(str).toString();
        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction JOIN = functionBuilder() //
        .name("join") //
        .description("""
                Join several strings with the specified separator. If any of the
                arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `join(",", "apple", "banana", "orange")` returns "apple,banana,orange"
                * `join(" ", "Hello", "world!")` returns "Hello world!"
                * `join("", "a", "b", "c")` returns "abc"
                * `join("-", "one")` returns "one"
                """) //
        .keywords("concatenation") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("seperator", "Separator with which to join the strings", isStringOrOpt()), //
            arg("string_1", "First string", isStringOrOpt()), //
            vararg("strings...", "More strings", isStringOrOpt()) //
        ) //
        .returnType("Strings joined with the specified separator", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::joinImpl) //
        .build();

    private static Computer joinImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String sep = toString(args.get(0)).compute(ctx);
            String[] toJoin = args.stream() //
                .skip(1) // skip over the first arg, which is the separator
                .map(StringFunctions::toString) //
                .map(sc -> sc.compute(ctx)) //
                .toArray(String[]::new);

            return String.join(sep, toJoin);
        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction SUBSTRING = functionBuilder() //
        .name("substring") //
        .description("""
                Get a substring of the string. If the length is unspecified or
                bigger than the remaining characters in the string after the start
                index, it returns the entire substring after the start index. The
                start index begins at 1.

                If any of the arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `substring("OpenAI", 2, 4)` returns "penA"
                * `substring("abcdef", 2, 10)` returns "bcdef"\\
                  Length is greater than the remaining characters, so returns the rest of the string.
                * `substring("substring", 5)` returns "tring"\\
                  No length provided, so returns everything after the 5th character
                """) //
        .keywords("extract") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "Original string", isStringOrOpt()), //
            arg("start", "Start index (inclusive, 1-indexed)", isIntegerOrOpt()), //
            optarg("length", "Length - if unspecified, or bigger than the string, get entire string after the start",
                isInteger()) //
        ) //
        .returnType("Extracted substring", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::substrImpl) //
        .build();

    private static Computer substrImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            int start = (int)toInteger(args.get(1)).compute(ctx);
            int length = args.size() == 3 //
                ? (int)toInteger(args.get(2)).compute(ctx) //
                : (str.length() - start + 1);

            // We do one-indexing in expressions editor
            // Also: clamp indices rather than erroring
            int clampedStart = Math.min(Math.max(start - 1, 0), str.length());
            return str.substring( //
                clampedStart, //
                Math.max(clampedStart, Math.min(start - 1 + length, str.length())) //
            );
        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction FIRST_CHARS = functionBuilder() //
        .name("first_chars") //
        .description("""
                Extract the first n characters from a string. If n is greater than
                or equal to the length of the string, it returns the entire string.
                If either of the arguments is `MISSING`, the result is also
                `MISSING`.

                Examples:
                * `first_chars("Hello world", 5)` returns "Hello"
                * `first_chars("12345", 10)` returns "12345"\\
                  Retrieves the entire string since the specified length exceeds the string's length.
                * `first_chars("", 3)` returns ""\\
                  Returns an empty string since the input string is empty.
                """) //
        .keywords("substring", "start", "extract") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "Input string", isStringOrOpt()), //
            arg("n", "Number of characters to get from start", isIntegerOrOpt()) //
        ) //
        .returnType("Substring with only the first `n` chars", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::firstCharsImpl) //
        .build();

    private static Computer firstCharsImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            int numChars = (int)toInteger(args.get(1)).compute(ctx);

            // Clamp indices rather than erroring
            return str.substring( //
                0, //
                Math.min(numChars, str.length()) //
            );
        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction LAST_CHARS = functionBuilder() //
        .name("last_chars") //
        .description("""
                Extract the last n characters from a string. If n is greater than
                or equal to the length of the string, it returns the entire string.
                If either of the arguments is `MISSING`, the result is also
                `MISSING`.

                Examples:
                * `last_chars("Hello world", 5)` returns "world"
                * `last_chars("12345", 10)` returns "12345"\\
                  Retrieves the entire string since the specified length exceeds the string's length.
                * `last_chars("", 3)` returns ""\\
                  Returns an empty string since the input string is empty.
                """) //
        .keywords("substring", "end", "extract") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "Input string", isStringOrOpt()), //
            arg("n", "Number of characters to get from end", isIntegerOrOpt()) //
        ) //
        .returnType("Substring with only the last `n` chars\"", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::lastCharsImpl) //
        .build();

    private static Computer lastCharsImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            int numChars = (int)toInteger(args.get(1)).compute(ctx);

            // Clamp indices rather than erroring
            return str.substring( //
                Math.max(str.length() - numChars, 0), //
                str.length() //
            );
        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction REMOVE_CHARS = functionBuilder() //
        .name("remove_chars") //
        .description("""
                Remove the specified characters from the input string. Equivalent to
                `replace_chars(string, chars, "")`. If either of the inputs is
                `MISSING`, the result is also `MISSING`. The optional modifiers
                argument can be used to tune the matching:
                * "i" for case-insensitive matching.

                Examples:
                * `remove_chars("Hello world", "lo")` returns "He wrd"
                * `remove_chars("abcdef", "ace")` returns "bdf"
                * `remove_chars("Mississippi", "s")` returns "Miippi"
                """) //
        .keywords("substring", "delete") //
        .category(CATEGORY_CLEAN.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()), //
            arg("chars", "Characters to delete", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for case-insensitive matching (root locale)", isString()) //)
        ) //
        .returnType("String with characters removed", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::removeCharsImpl) //
        .build();

    private static Computer removeCharsImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            String toRemove = toString(args.get(1)).compute(ctx);
            String modifiers = extractModifiersOrDefault(args, 2, ctx);

            boolean ignoreCase = modifiers.contains("i");

            for (int i = 0; i < toRemove.length(); ++i) {
                var charToRemove = String.valueOf(toRemove.charAt(i));
                if (ignoreCase) {
                    str = str.replace(charToRemove.toUpperCase(Locale.ROOT), "");
                    str = str.replace(charToRemove.toLowerCase(Locale.ROOT), "");
                } else {
                    str = str.replace(charToRemove, "");
                }
            }

            return str;
        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction STRIP = functionBuilder() //
        .name("strip") //
        .description("""
                Remove leading and trailing whitespace from the input string. If the
                input string is `MISSING`, the result is also `MISSING`.

                Examples:
                * `strip("  Hello world  ")` returns "Hello world"\\
                  Removes spaces from both ends.
                * `strip("No extra spaces")` returns "No extra spaces"\\
                  Leaves strings with no leading/trailing spaces unchanged.
                * `strip("   ")` returns ""\\
                  Converts a string of only spaces to an empty string.
                """) //
        .keywords("trim", "whitespace", "clean") //
        .category(CATEGORY_CLEAN.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String with leading/trailing whitespace removed", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::stripImpl) //
        .build();

    private static Computer stripImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> toString(args.get(0)).compute(ctx).strip(), //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction STRIP_START = functionBuilder() //
        .name("strip_start") //
        .description("""
                Remove leading whitespace from the input string. If the input string
                is `MISSING`, the result is also `MISSING`.

                Examples:
                * `strip_start("  Hello world  ")` returns "Hello world  "
                * `strip_start("  No leading space")` returns "No leading space"
                * `strip_start("No trailing space  ")` returns "No trailing space  "\\
                  Does not remove spaces from the end.
                * `strip_start("   ")` returns ""\\
                  Converts a string of only spaces to an empty string.
                """) //
        .keywords("trim", "whitespace", "clean") //
        .category(CATEGORY_CLEAN.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String with leading whitespace removed", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::stripstartImpl) //
        .build();

    private static Computer stripstartImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> toString(args.get(0)).compute(ctx).stripLeading(), //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction STRIP_END = functionBuilder() //
        .name("strip_end") //
        .description("""
                Remove trailing whitespace from the input string. If the input
                string is `MISSING`, the result is also `MISSING`.

                Examples:
                * `strip_end("  Hello world  ")` returns "  Hello world"
                * `strip_end("No leading space  ")` returns "No leading space"
                * `strip_end("  No trailing space")` returns "  No trailing space"\\
                  Does not remove spaces from the start.
                * `strip_end("   ")` returns ""\\
                  Converts a string of only spaces to an empty string.
                """) //
        .keywords("trim", "whitespace", "clean") //
        .category(CATEGORY_CLEAN.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String with trailing whitespace removed", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::stripEndImpl) //
        .build();

    private static Computer stripEndImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> toString(args.get(0)).compute(ctx).stripTrailing(), //
            anyMissing(args) //
        );
    }

    private static final Pattern multiSpacePattern = Pattern.compile("( ){2,}");

    public static final ExpressionFunction REMOVE_DUPLICATE_SPACES = functionBuilder() //
        .name("remove_duplicate_spaces") //
        .description("""
                Replace all duplicated spaces with a single space in the given
                string. If the input string is `MISSING`, the result is also
                `MISSING`.

                Examples:
                * `remove_duplicate_spaces("This  is  a  test")` returns "This is a test"\\
                  Reduces multiple spaces between words to one.
                * `remove_duplicate_spaces("Single space")` returns "Single space"\\
                  Leaves strings with single spaces between words unchanged.
                * `remove_duplicate_spaces("  Leading and trailing  ")` returns " Leading and trailing "\\
                  Also reduces multiple leading/trailing spaces to one.
                """) //
        .keywords("trim", "spaces", "whitespace", "clean") //
        .category(CATEGORY_CLEAN.name()) //
        .args( //
            arg("string", "String to clean up", isStringOrOpt()) //
        ) //
        .returnType("String with all repeated spaces replaced", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::removeDuplicateSpacesImpl) //
        .build();

    private static Computer removeDuplicateSpacesImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> multiSpacePattern.matcher(toString(args.get(0)).compute(ctx)).replaceAll(" "), //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction MISSING_TO_EMPTY = functionBuilder() //
        .name("missing_to_empty") //
        .description("""
                If the input string is `MISSING`, return an empty string. Otherwise,
                return the input string.

                This function is useful for handling cases where a `MISSING` value
                should be converted to an empty string to avoid issues with further
                processing.

                Examples:
                * `missing_to_empty($["Missing Column"])` returns ""
                * `missing_to_empty("Hello")` returns "Hello"\\
                  Any non-missing string is unchanged.
                """) //
        .keywords("default", "null") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to check for `MISSING`", isStringOrOpt()) //
        ) //
        .returnType("Input string or empty when input was `MISSING`", RETURN_STRING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::nullToEmptyImpl) //
        .build();

    private static Computer nullToEmptyImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> args.get(0).isMissing(ctx) ? "" : toString(args.get(0)).compute(ctx), //
            ctx -> false //
        );
    }

    public static final ExpressionFunction EMPTY_TO_MISSING = functionBuilder() //
        .name("empty_to_missing") //
        .description("""
                If the input string is empty or `MISSING`, returns `MISSING`.
                Otherwise, returns the input string unchanged.

                Examples:
                * `empty_to_missing("")` returns `MISSING`
                * `empty_to_missing("Hello")` returns "Hello"
                * `empty_to_missing($["Missing Column"])` returns `MISSING`\\
                  Leaves `MISSING` unchanged.
                """) //
        .keywords("default", "null") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("Input string or `MISSING` when string was empty", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::emptyToNull) //
        .build();

    private static Computer emptyToNull(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> toString(args.get(0)).compute(ctx), //
            ctx -> args.get(0).isMissing(ctx) || toString(args.get(0)).compute(ctx).isEmpty() //
        );
    }

    public static final ExpressionFunction REVERSE = functionBuilder() //
        .name("reverse") //
        .description("""
                Reverses the input string and returns the result. If the input
                string is `MISSING`, the result is also `MISSING`.

                Examples:
                * `reverse("hello")` returns "olleh"
                * `reverse("12345") returns "54321"
                * `reverse("")` returns ""
                """) //
        .keywords("invert", "mirror", "backward", "flip") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to reverse", isStringOrOpt()) //
        ) //
        .returnType("Reversed string", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::reverseImpl) //
        .build();

    private static Computer reverseImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> new StringBuilder(toString(args.get(0)).compute(ctx)).reverse().toString(), //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction LENGTH = functionBuilder() //
        .name("length") //
        .description("""
                Returns the number of characters in the input string. If the input
                string is `MISSING`, the result is also `MISSING`.

                Examples:
                * `length("hello")` returns 5
                * `length("")` returns 0
                """) //
        .keywords("count", "size", "number of characters") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "String to count chars for", isStringOrOpt()) //
        ) //
        .returnType("Length of the string", RETURN_INTEGER_MISSING, //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::lengthImpl) //
        .build();

    private static Computer lengthImpl(final List<Computer> args) {
        return IntegerComputer.of( //
            ctx -> toString(args.get(0)).compute(ctx).length(), //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction COUNT = functionBuilder() //
        .name("count") //
        .description("""
                Returns the number of occurrences of the search term in the input
                string. The optional modifiers argument can be used to tune the
                behaviour:
                * "i" to enable case-insensitive matching
                * "w" to match only whole words

                Modifiers can be combined, so e.g. "iw" ignores case and matches
                only whole words.

                If any of the arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `count("hello", "l")` returns 2\\
                  Returns 2 as "l" occurs twice in "hello".
                * `count("hello", "L", "i")` returns 2\\
                  Returns 2 as "L" occurs twice in "hello" when case-insensitive matching is enabled.
                * `count("hello world hello", "hello")` returns 2\\
                  Returns 2 as "hello" occurs twice in the input string.
                * `count("hello world", "ello", "w")` returns 0\\
                  Returns 0 as "ello" does not occur as a whole word in the input string.
                * `count("", "")` returns 0\\
                  Returns 0 when both input string and search term are empty.
                """) //
        .keywords("occurrences", "matching") //

        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "String to search within", isStringOrOpt()), //
            arg("search", "Search term to count", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" case-insensitive matching, " //
                + "\"w\" to match only whole words", isString()) //
        ) //
        .returnType("Number of occurences", RETURN_INTEGER_MISSING, //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::countImpl) //
        .build();

    private static Computer countImpl(final List<Computer> args) {
        ToLongFunction<EvaluationContext> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            String search = toString(args.get(1)).compute(ctx);
            String modifiers = extractModifiersOrDefault(args, 2, ctx);

            boolean ignoreCase = modifiers.contains("i");
            boolean wholeWords = modifiers.contains("w");

            int patternModifiers = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;

            if (!wholeWords) {
                return Pattern.compile(Pattern.quote(search), patternModifiers) //
                    .matcher(str) //
                    .results() //
                    .count();
            } else {
                return Pattern.compile("\\b" + Pattern.quote(search) + "\\b", patternModifiers) //
                    .matcher(str) //
                    .results() //
                    .count();
            }
        };

        return IntegerComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction COUNT_CHARS = functionBuilder() //
        .name("count_chars") //
        .description("""
                Counts the number of occurrences of the specified characters in the input string.
                If more than one character is provided, the occurrences of each character are summed.
                The optional modifiers argument can be used to tune the behaviour:
                * "i" to ignore case when looking for chars
                * "v" to count all characters except those provided

                Modifiers can be combined, so e.g. "iv" ignores case and counts all
                characters _except_ those provided.

                If any of the arguments are `MISSING`, the result is also `MISSING`.

                Examples:
                * `count_chars("hello", "l")` returns 2\\
                  Returns 2 as "l" occurs twice in "hello".
                * `count_chars("hello", "L", "i")` returns 2\\
                  Returns 2 as "L" occurs twice in "hello" ignoring case.
                * `count_chars("hello world", "ol")` returns 5\\
                  Returns 3 as "o" and "l" occur three times in "hello world".
                * `count_chars("hello", "ol", "v")` returns 2\\
                  Returns 2 as "l" and "o" appear 3 times and there are 2 chars left.
                * `count_chars("", "")` returns 0\\
                  Returns 0 when both the input string and search characters are empty.
                """) //
        .keywords("occurrences") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "String to count chars for", isStringOrOpt()), //
            arg("search", "Characters to search for", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" case-insensitive matching" //
                + "\"v\" to count all chars except those provided", isString()) //
        ) //
        .returnType("Number of occurences", RETURN_INTEGER_MISSING, //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::countCharsImpl) //
        .build();

    private static Computer countCharsImpl(final List<Computer> args) {
        ToLongFunction<EvaluationContext> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            String searchChars = toString(args.get(1)).compute(ctx);
            String modifiers = extractModifiersOrDefault(args, 2, ctx);

            boolean ignoreCase = modifiers.contains("i");
            boolean matchInvert = modifiers.contains("v");

            if (ignoreCase) {
                str = str.toLowerCase(Locale.ROOT);
                searchChars = searchChars.toLowerCase(Locale.ROOT);
            }

            final String finalSearchChars = searchChars;

            return str.chars() //
                .filter(c -> matchInvert ^ finalSearchChars.contains(String.valueOf((char)c))) //
                .count();
        };

        return IntegerComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction FIND = functionBuilder() //
        .name("find") //
        .description("""
                Find the index (starting at 1) of the first or last occurrence of
                the given string within another string. The optional modifiers
                parameter can be used to tune the behaviour:
                * "i" to ignore case when matching strings
                * "w" to match whole words only
                * "b" to search backwards (i.e. find the _last_ matching instance)

                Modifiers can be combined, so e.g. "ib" would ignore case and search
                backwards.

                If any of the arguments are `MISSING`, or the search string is not
                found, the result is `MISSING`.

                Examples:
                * `find("hello world", "world")` returns 7
                * `find("hello world", "World", "i")` returns 7
                * `find("hello hello", "l", "b")` returns 10
                * `find("hello world", "universe")` returns `MISSING`
                """) //
        .keywords("index", "position", "search") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "String to search within", isStringOrOpt()), //
            arg("search", "String to search for", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" to ignore case, " //
                + "\"w\" to match whole words, " //
                + "\"b\" to search backwards", isString()) //
        ) //
        .returnType("Index (1-based) of the first occurence (or `MISSING` if not found)", RETURN_INTEGER_MISSING, //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::findImpl) //
        .build();

    private static Computer findImpl(final List<Computer> args) {
        ToLongFunction<EvaluationContext> indexSupplier = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            String search = toString(args.get(1)).compute(ctx);
            String modifiers = extractModifiersOrDefault(args, 2, ctx);

            boolean ignoreCase = modifiers.contains("i");
            boolean wholeWords = modifiers.contains("w");
            boolean backwards = modifiers.contains("b");

            var patternFlags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;

            var pattern = wholeWords //
                ? Pattern.compile("\\b" + Pattern.quote(search) + "\\b", patternFlags) //
                : Pattern.compile(Pattern.quote(search), patternFlags);

            var match = pattern.matcher(str);

            if (!match.find()) {
                return -1;
            }
            match.reset();

            if (backwards) {
                return match.results().reduce((a, b) -> b).get().start() + 1;
            } else {
                return match.results().findFirst().get().start() + 1;
            }
        };

        Predicate<EvaluationContext> isMissing = ctx -> //
        anyMissing(args).test(ctx) //
            || indexSupplier.applyAsLong(ctx) == -1;

        return IntegerComputer.of( //
            indexSupplier, //
            isMissing //
        );
    }

    public static final ExpressionFunction FIND_CHARS = functionBuilder() //
        .name("find_chars") //
        .description("""
                Find the index (starting at 1) of the first character in the
                string that matches any of the characters provided in the search
                string. The optional modifiers argument can be used to tune the
                behaviour:
                * "i" to ignore case when matching characters
                * "v" to match only characters _not_ provided
                * "b" to search backwards (i.e. get the _last_ matching character)

                Modifiers can be combined, so e.g. "iv" would ignore case and match
                only characters _not_ provided.

                If any of the arguments are `MISSING`, or none of the search
                characters are present in the string, the result is `MISSING`.

                Examples:
                * `find_chars("hello world", "o")` returns 5
                * `find_chars("hello world", "owxl")` returns 3
                * `find_chars("hello world", "O", "i")` returns 5
                * `find_chars("hello world", "ab", "v")` returns 1\\
                  The first character that isn't a or b is at index 1.
                * `find_chars("hello world", "l", "b")` returns 9
                * `find_chars("hello world", "xyz")` returns `MISSING`\\
                  Returns `MISSING` as none of the search characters are found.
                * `find_chars("hello world", "")` returns `MISSING`\\
                  Returns `MISSING` as no search characters are provided.
                """) //
        .keywords("index", "position", "search") //
        .category(CATEGORY_EXTRACT_REPLACE.name()) //
        .args( //
            arg("string", "String to search within", isStringOrOpt()), //
            arg("chars", "Characters to search for", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" to ignore case, " //
                + "\"v\" to match all characters not provided, " //
                + "\"b\" to search backwards", isString()) //
        ) //
        .returnType("Index (1-based!) of the first occurence (or `MISSING` if not found)", RETURN_INTEGER_MISSING, //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::findCharsImpl) //
        .build();

    private static Computer findCharsImpl(final List<Computer> args) {
        ToLongFunction<EvaluationContext> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);
            String search = toString(args.get(1)).compute(ctx);
            String modifiers = extractModifiersOrDefault(args, 2, ctx);

            boolean ignoreCase = modifiers.contains("i");
            boolean backwards = modifiers.contains("b");
            boolean invert = modifiers.contains("v");

            if (ignoreCase) {
                str = str.toLowerCase(Locale.ROOT);
                search = search.toLowerCase(Locale.ROOT);
            }

            final String finalStr = str;
            final String finalSearch = search;

            var matchingdIndicesStream = IntStream.range(0, str.length())
                .filter(i -> invert ^ finalSearch.contains(String.valueOf(finalStr.charAt(i))));

            OptionalInt returnValue = backwards //
                ? matchingdIndicesStream.reduce((a, b) -> b) //
                : matchingdIndicesStream.findFirst();

            return returnValue.isEmpty() ? -1 : (1 + returnValue.getAsInt());
        };

        return IntegerComputer.of( //
            value, //
            ctx -> anyMissing(args).test(ctx) || value.applyAsLong(ctx) < 0//
        );
    }

    public static final ExpressionFunction CHECKSUM_MD5 = functionBuilder() //
        .name("checksum_md5") //
        .description("""
                Get the MD5 checksum of the string.

                *Note that MD5 is prone to hash collisions and is not safe for
                hashing passwords.*

                If the input string is `MISSING`, the result is also `MISSING`.

                Examples:
                * `checksum_md5("hello")` returns "5d41402abc4b2a76b9719d911017c592"
                * `checksum_md5("") returns "d41d8cd98f00b204e9800998ecf8427e"
                """) //
        .keywords("hash") //
        .category(CATEGORY_ENCODE.name()) //
        .args( //
            arg("string", "String to compute the MD5 checksum for", isStringOrOpt()) //
        ) //
        .returnType("MD5 hash of the string", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::checksumMd5Impl) //
        .build();

    private static Computer checksumMd5Impl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            String str = toString(args.get(0)).compute(ctx);

            try {
                var digest = MessageDigest.getInstance("MD5") //
                    .digest(str.getBytes(StandardCharsets.UTF_8));

                var md5String = new StringBuilder(32);

                for (byte b : digest) {
                    md5String.append(String.format("%02x", b));
                }

                return md5String.toString();
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("this should never happen", ex);
            }

        };

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction XML_ENCODE = functionBuilder() //
        .name("xml_encode") //
        .description("""
                Escape XML characters in a string.

                XML encoding replaces certain characters with their
                corresponding XML entities to prevent interpretation of the
                characters as markup.

                The following characters are replaced: &, <, >, \\, ' and ".

                If the input string is `MISSING`, the result is also `MISSING`.

                Examples:
                * `xml_encode("<a>Hello</a>")` returns "&lt;a&gt;Hello&lt;/a&gt;"
                * `xml_encode("&")` returns "&amp;"
                """) //
        .keywords("escape", "special characters", "markup") //
        .category(CATEGORY_ENCODE.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String with XML special characters escaped", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::xmlEncodeImpl) //
        .build();

    private static Computer xmlEncodeImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> toString(args.get(0)).compute(ctx) //
            .replace("&", "&amp;") //
            .replace("<", "&lt;") //
            .replace(">", "&gt;") //
            .replace("\"", "&quot;") //
            .replace("'", "&apos;");

        return StringComputer.of( //
            value, //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction URL_ENCODE = functionBuilder() //
        .name("url_encode") //
        .description("""
                Replaces characters that can break URLs. This includes non-ascii
                characters (e.g. umlauts) and reserved characters (e.g. "?").
                The resulting string is "percent encoded", i.e., non-alphanumeric
                values are replaced. The resulting string is safe to use in a HTTP
                GET request, for instance when sending data via an HTML form. The
                method uses the UTF-8 encoding scheme to obtain the bytes for
                unsafe characters.

                If the input string is `MISSING`, the result is also `MISSING`.

                Examples:
                * `urlEncode("the space between")` returns "the+space+between"
                * `urlEncode("1 + 1 = 2")` returns "1+%2B+1+%3D+2"
                """) //
        .keywords("escape", "special characters") //
        .category(CATEGORY_ENCODE.name()) //
        .args( //
            arg("string", "String to convert", isStringOrOpt()) //
        ) //
        .returnType("String with forbidden characters escaped", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::urlEncodeImpl) //
        .build();

    private static Computer urlEncodeImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> URLEncoder.encode(toString(args.get(0)).compute(ctx), StandardCharsets.UTF_8), //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction URL_DECODE = functionBuilder() //
        .name("url_decode") //
        .description("""
                Given a string with forbidden URL characters escaped, recover
                the original string by decoding the percent-encoded characters.

                If the input string is `MISSING`, the result is also `MISSING`.

                Examples:
                * `url_decode("the+space+between")` returns "the space between"
                * `url_decode("1+%2B+1+%3D+2")` returns "1 + 1 = 2"
                """) //
        .keywords("escape", "special characters") //
        .category(CATEGORY_ENCODE.name()) //
        .args( //
            arg("string", "String with escaped URL-specific chars", isStringOrOpt()) //
        ) //
        .returnType("Original URL, with encoding undone", RETURN_STRING_MISSING, //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::urlDecodeImpl) //
        .build();

    private static Computer urlDecodeImpl(final List<Computer> args) {
        return StringComputer.of( //
            ctx -> URLDecoder.decode(toString(args.get(0)).compute(ctx), StandardCharsets.UTF_8), //
            anyMissing(args) //
        );
    }

    public static final ExpressionFunction TO_STRING = functionBuilder() //
        .name("string") //
        .description("""
                Convert the input to a string.

                This function supports converting various data types to strings:
                * Boolean: converted to either `true` or `false`.
                * Float: Converted to their string representations, e.g. "5.4".
                * Integer: Converted to their string representations, e.g. "1".
                * String: Returns unchanged input string.

                Any values that are `MISSING` are converted to the string "MISSING".

                Examples:
                * `string(42)` returns "42"\\
                  Converts the integer 42 to the string "42".
                * `string(true)` returns "true"\\
                  Converts the boolean `true` to the string "true".
                * `string(MISSING)` returns "MISSING"\\
                  Converts the `MISSING` value to the string "MISSING".
                """) //
        .keywords("cast", "types") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("input", "Input to convert to a string", isAnything()) //
        ) //
        .returnType("Input as string", RETURN_STRING, args -> ValueType.STRING) //
        .impl(StringFunctions::toStringImpl) //
        .build();

    private static Computer toStringImpl(final List<Computer> args) {
        Function<EvaluationContext, String> value = ctx -> {
            var c = args.get(0);

            if (c.isMissing(ctx)) {
                return "MISSING";
            } else if (c instanceof BooleanComputer bc) {
                return String.valueOf(bc.compute(ctx));
            } else if (c instanceof FloatComputer fc) {
                return String.valueOf(fc.compute(ctx));
            } else if (c instanceof IntegerComputer ic) {
                return String.valueOf(ic.compute(ctx));
            } else if (c instanceof StringComputer sc) {
                return sc.compute(ctx);
            } else {
                throw FunctionUtils.calledWithIllegalArgs();
            }
        };

        return StringComputer.of(value, ctx -> false);
    }

    public static final ExpressionFunction PARSE_FLOAT = functionBuilder() //
        .name("parse_float") //
        .description("""
                Convert a string to a float if possible.

                This function attempts to parse the input string as a float value.
                If successful, returns the float representation of the string. If
                the input string cannot be parsed as a float (or the input is
                `MISSING`), returns `MISSING`.

                Examples:
                * `parse_float("3.14")` returns 3.14
                * `parse_float("3")` returns 3.0
                * `parse_float("hello")` returns `MISSING`
                """) //
        .keywords("cast", "types") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to parse as float", isStringOrOpt()) //
        ) //
        .returnType("Float representation of the string, or `MISSING`", RETURN_FLOAT_MISSING, //
            args -> ValueType.FLOAT(anyOptional(args))) //
        .impl(StringFunctions::parseFloatImpl) //
        .build();

    private static Computer parseFloatImpl(final List<Computer> args) {
        return FloatComputer.of( //
            ctx -> Float.parseFloat(toString(args.get(0)).compute(ctx)), //
            ctx -> {
                if (anyMissing(args).test(ctx)) {
                    return true;
                }

                try {
                    Float.parseFloat(toString(args.get(0)).compute(ctx));
                } catch (NumberFormatException ex) {
                    return true;
                }

                return false;
            });
    }

    public static final ExpressionFunction PARSE_INT = functionBuilder() //
        .name("parse_int") //
        .description("""
                Convert a string to an integer if possible.

                This function attempts to parse the input string as an integer. If
                successful, returns the float representation of the string. If the
                input string cannot be parsed as an integer (or the input is
                `MISSING`), returns `MISSING`.

                Examples:
                * `parse_int("123")` returns 123
                * `parse_int("123.0")` returns `MISSING`
                * `parse_int("hello")` returns `MISSING`
                """) //
        .keywords("cast", "types") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to parse as integer", isStringOrOpt()) //
        ) //
        .returnType("Integer representation of the string, or `MISSING`", RETURN_INTEGER_MISSING, //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::parseIntImpl) //
        .build();

    private static Computer parseIntImpl(final List<Computer> args) {
        return IntegerComputer.of( //
            ctx -> Integer.parseInt(toString(args.get(0)).compute(ctx)), //
            ctx -> {
                if (anyMissing(args).test(ctx)) {
                    return true;
                }

                try {
                    Integer.parseInt(toString(args.get(0)).compute(ctx));
                } catch (NumberFormatException ex) {
                    return true;
                }

                return false;
            });
    }

    public static final ExpressionFunction PARSE_BOOL = functionBuilder() //
        .name("parse_bool") //
        .description("""
                Convert a string to a boolean if possible. If the input cannot be
                interpreted as a boolean, or is `MISSING`, returns `MISSING`.

                This function attempts to parse the input string as a boolean value,
                according to the following rules:
                * If the string is "true" (case-insensitive), returns `true`.
                * If the string is "false" (case-insensitive), returns `false`.
                * If the string is neither "true" nor "false", returns `MISSING`.

                Examples:
                * `parse_bool("true")` returns `true`
                * `parse_bool("false")` returns `false`
                * `parse_bool("abc")` returns `MISSING`
                """) //
        .keywords("parse", "types") //
        .category(CATEGORY_GENERAL.name()) //
        .args( //
            arg("string", "String to parse as boolean", isStringOrOpt()) //
        ) //
        .returnType("Boolean representation of the string, or `MISSING`", RETURN_BOOLEAN_MISSING, //
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::parseBoolImpl) //
        .build();

    private static Computer parseBoolImpl(final List<Computer> args) {
        return BooleanComputer.of( //
            ctx -> toString(args.get(0)).compute(ctx).equalsIgnoreCase("true"), //
            ctx -> {
                if (anyMissing(args).test(ctx)) {
                    return true;
                }

                var stringArg = toString(args.get(0)).compute(ctx);

                return (!stringArg.equalsIgnoreCase("true") && !stringArg.equalsIgnoreCase("false"));
            });
    }

    // ======================= UTILITIES ==============================

    private static StringComputer toString(final Computer c) {
        if (c instanceof StringComputer sc) {
            return sc;
        }

        throw FunctionUtils.calledWithIllegalArgs();
    }

    private static BooleanComputer toBoolean(final Computer c) {
        if (c instanceof BooleanComputer bc) {
            return bc;
        }

        throw FunctionUtils.calledWithIllegalArgs();
    }

    private static IntegerComputer toInteger(final Computer c) {
        if (c instanceof IntegerComputer ic) {
            return ic;
        }

        throw FunctionUtils.calledWithIllegalArgs();
    }

    private static final Pattern regexSpecialChars = Pattern.compile("[{}()\\[\\]<>.+*?^$\\\\|]");

    private static String escapeRegexChars(final String s) {
        return regexSpecialChars.matcher(s).replaceAll("\\\\$0");
    }

    private static String takeFirstChar(final String s, final String fallback) {
        if (s.length() == 0) {
            return fallback;
        } else {
            return String.valueOf(s.charAt(0));
        }
    }

    private static String extractModifiersOrDefault(final List<Computer> args, final int index,
        final EvaluationContext ctx) {
        if (args.size() > index) {
            return toString(args.get(index)).compute(ctx);
        } else {
            return "";
        }
    }
}
