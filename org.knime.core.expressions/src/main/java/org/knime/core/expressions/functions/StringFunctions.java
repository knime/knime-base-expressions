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

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.STRING;
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

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.ValueType;

/**
 * Implementation of built-in functions that manipulate strings.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("javadoc")
public final class StringFunctions {

    private StringFunctions() {
    }

    /** The "String Manipulation" category */
    public static final FunctionCategory CATEGORY =
        new FunctionCategory("String Manipulation", "Functions that operate on strings");

    public static final ExpressionFunction COMPARE = functionBuilder() //
        .name("compare") //
        .description("Compares two strings lexicographically") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("x", "the first string", isStringOrOpt()), //
            arg("y", "the second string", isStringOrOpt()) //
        ) //
        .returnType("-1 if x < y, 0 if x == y, and 1 if x > y", "INTEGER?",
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::compareImpl) //
        .build();

    private static Computer compareImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        return IntegerComputer.of( //
            () -> c1.compute().compareTo(c2.compute()), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction CONTAINS = functionBuilder() //
        .name("contains") //
        .description("Checks if a string contains another string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string to check", isStringOrOpt()), //
            arg("search", "the search term", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for ignore case (root locale)", isString()) //
        ) //
        .returnType("true if str contains the search term, false otherwise", "BOOLEAN?",
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::containsImpl) //
        .build();

    private static Computer containsImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        // Modifiers
        final BooleanSupplier ignoreCase;
        if (args.size() == 3) {
            // modifier present
            var modifier = (StringComputer)args.get(2);
            ignoreCase = () -> modifier.compute().contains("i");
        } else {
            ignoreCase = () -> false;
        }

        return BooleanComputer.of(() -> {
            if (ignoreCase.getAsBoolean()) {
                return c1.compute().toLowerCase(Locale.ROOT).contains(c2.compute().toLowerCase(Locale.ROOT));
            } else {
                return c1.compute().contains(c2.compute());
            }
        }, () -> args.stream().anyMatch(Computer::isMissing));
    }

    public static final ExpressionFunction STARTS_WITH = functionBuilder() //
        .name("starts_with") //
        .description("Check if the string starts with the given string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string to check", isStringOrOpt()), //
            arg("prefix", "the prefix to check", isStringOrOpt()) //
        ) //
        .returnType("true if the string starts with prefix, false otherwise", "BOOLEAN?", //
            args -> BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::startsWithImpl) //
        .build();

    private static Computer startsWithImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        return BooleanComputer.of( //
            () -> c1.compute().startsWith(c2.compute()), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction ENDS_WITH = functionBuilder() //
        .name("ends_with") //
        .description("Check if the string ends with the given string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string to check", isStringOrOpt()), //
            arg("suffix", "the suffix to check", isStringOrOpt()) //
        ) //
        .returnType("true if the string ends with suffix, false otherwise", "BOOLEAN?", //
            args -> BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::endsWithImpl) //
        .build();

    private static Computer endsWithImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        return BooleanComputer.of( //
            () -> c1.compute().endsWith(c2.compute()), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    // TODO(AP-22345) emit a warning on potentially slow regexes
    public static final ExpressionFunction LIKE = functionBuilder() //
        .name("like") //
        .description("""
                Check if string matches the given pattern using SQL LIKE syntax.
                 * `_` is a single-character wildcard
                 * `%` represents 0, 1, or more characters
                 * to match a literal `%` or `_`, use `[%]` or `[_]`
                 """) //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string to check", isStringOrOpt()), //
            arg("pattern", "the matching rule", isStringOrOpt()), //
            optarg("ignore_case", "if true, do case insensitive matching", isBoolean())) //
        .returnType("true if the string matches the pattern, false otherwise", "BOOLEAN?", //
            args -> BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::likeImpl) //
        .build();

    // TODO(AP-22345) emit a warning on potentially slow regexes
    private static final Pattern underscorePattern = Pattern.compile("([^\\[]|^)(?:_)([^\\[]|$)");

    private static final Pattern percentPattern = Pattern.compile("([^\\[]|^)(?:%)([^\\[]|$)");

    private static final Pattern regexCharsExceptSquareBracketsPattern = Pattern.compile("[{}().+*?^$\\\\|]");

    private static Computer likeImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        BooleanSupplier value = () -> {
            String escapedPattern = c2.compute();
            String toMatch = c1.compute();

            if (args.size() == 3 && toBoolean(args.get(2)).compute()) {
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
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction REGEX_MATCH = functionBuilder() //
        .name("regex_match") //
        .description( //
            "Check if string matches the given regular expression" //
        ) //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string to check", isStringOrOpt()), //
            arg("pattern", "the regex", isStringOrOpt()) //
        ) //
        .returnType("true if the string matches the pattern, false otherwise", "BOOLEAN?", //
            args -> BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::regexMatchImpl) //
        .build();

    // TODO(AP-22345) emit a warning on potentially slow regexes
    private static Computer regexMatchImpl(final List<Computer> args) {
        var c1 = toString(args.get(0));
        var c2 = toString(args.get(1));

        return BooleanComputer.of( //
            () -> c1.compute().matches(c2.compute()), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction REGEX_EXTRACT = functionBuilder() //
        .name("regex_extract") //
        .description( //
            "Given a regex that captures some groups, extract and return the group "
                + "referred to by the index. Groups are one-indexed, so e.g. for the regex"
                + "`[0-9]([a-z]+).*` applied to the string `5hello123`, group 1 would be" + "`hello`." //
        ) //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string to check", isStringOrOpt()), //
            arg("pattern", "the regex", isStringOrOpt()), //
            arg("group", "the index of the group to extract", isIntegerOrOpt())) //
        .returnType("the extracted group", "STRING?", //
            args -> STRING(anyOptional(args))) //
        .impl(StringFunctions::regexExtractImpl) //
        .build();

    private static String extractGroupOrReturnNull(final String toMatch, final String pattern, final int group) {
        var matcher = Pattern.compile(pattern).matcher(toMatch);

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

        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing) //
            || extractGroupOrReturnNull(c1.compute(), c2.compute(), (int)c3.compute()) == null;
        Supplier<String> value = () -> extractGroupOrReturnNull(c1.compute(), c2.compute(), (int)c3.compute());

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction REGEX_REPLACE = functionBuilder() //
        .name("regex_replace") //
        .description("Replace text matching the given regex") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("pattern", "the search term", isStringOrOpt()), //
            arg("replace", "the replacement", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for ignore case (root locale)", isString()) //
        ) //
        .returnType("str with pattern replaced", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::regexReplaceImpl) //
        .build();

    private static Computer regexReplaceImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            String search = toString(args.get(1)).compute();
            String replacement = toString(args.get(2)).compute();

            String modifiers = extractModifiersOrDefault(args, 3);
            boolean ignoreCase = modifiers.contains("i");

            var pattern = Pattern.compile(search, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
            return pattern.matcher(str).replaceAll(replacement);
        };

        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction REPLACE = functionBuilder() //
        .name("replace") //
        .description("Replace text matching the given literal pattern") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("pattern", "the search term", isStringOrOpt()), //
            arg("replace", "the replacement", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for ignore case (root locale), " //
                + "\"w\" to match whole words only", isString()) //
        ) //
        .returnType("str with pattern replaced", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::replaceImpl) //
        .build();

    private static Computer replaceImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            String search = toString(args.get(1)).compute();
            String replacement = toString(args.get(2)).compute();

            String modifiers = extractModifiersOrDefault(args, 3);
            boolean ignoreCase = modifiers.contains("i");
            boolean wholeWords = modifiers.contains("w");

            String patternString = wholeWords //
                ? ("\\b" + escapeRegexChars(search) + "\\b") //
                : escapeRegexChars(search);

            var pattern = Pattern.compile(patternString, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
            return pattern.matcher(str).replaceAll(Matcher.quoteReplacement(replacement));
        };

        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        return StringComputer.of(value, isMissing);
    }

    /** Basically does a translate like in python or SQL */
    public static final ExpressionFunction REPLACE_CHARS = functionBuilder() //
        .name("replace_chars") //
        .description("Replace characters in str with the given substitions. For example,"
            + "if str is \"hello\" and old_chars is \"el\" and new_chars is \"12\","
            + "the result will be \"h122o\". If the string of new characters is shorter,"
            + "then we delete the corresponding old characters, so for example if str is"
            + "\"hello\" and old_chars is\"el\" and new_chars is \"1\", the result would" + "be \"h1o\".") // "
        .keywords("translate") //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("old_chars", "", isStringOrOpt()), //
            arg("new_chars", "", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" for ignore case (root locale)", isString()) //
        ) //
        .returnType("str with characters replaced", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::replaceCharsImpl) //
        .build();

    private static Computer replaceCharsImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            char[] oldChars = toString(args.get(1)).compute().toCharArray();
            char[] newChars = toString(args.get(2)).compute().toCharArray();

            String modifiers = extractModifiersOrDefault(args, 3);
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

        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction REPLACE_UMLAUTS = functionBuilder() //
        .name("replace_umlauts") //
        .description("replace umlauts with their ascii equivalents") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("no_e", "if true, e.g. ö->o. If false, o->oe", isBooleanOrOpt()), //
            optarg("replace_esszet", "if true, also replace ß and ẞ (default true)", isBoolean()) //
        ) //
        .returnType("str with umlauts replaced", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::replaceUmlautsImpl) //
        .build();

    private static Computer replaceUmlautsImpl(final List<Computer> args) {
        var umlauts = "äüö";
        var umlautReplacements = "auo";

        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            boolean noE = toBoolean(args.get(1)).compute();

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

            if (args.size() < 3 || toBoolean(args.get(2)).compute()) {
                str = str //
                    .replace("ß", "ss") //
                    .replace("ẞ", "SS");
            }

            return str;
        };

        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction REPLACE_DIACRITICS = functionBuilder() //
        .name("replace_diacritics") //
        .description("replace diacritics with their ascii equivalents") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("str with diacritics replaced", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::replaceDiacriticsImpl) //
        .build();

    private static Computer replaceDiacriticsImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            str = Normalizer.normalize(str, Normalizer.Form.NFKD);
            str = str.replaceAll("\\p{M}", "");
            return str;
        };

        BooleanSupplier isMissing = () -> args.stream().anyMatch(Computer::isMissing);

        return StringComputer.of(value, isMissing);
    }

    public static final ExpressionFunction LOWER_CASE = functionBuilder() //
        .name("lower_case") //
        .description("string to lower case") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("str in lower case", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::lowerCaseImpl) //
        .build();

    private static Computer lowerCaseImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> toString(args.get(0)).compute().toLowerCase(Locale.ROOT), //
            () -> args.stream().anyMatch(Computer::isMissing));
    }

    public static final ExpressionFunction UPPER_CASE = functionBuilder() //
        .name("upper_case") //
        .description("string to upper case") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("str in upper case", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::upperCaseImpl) //
        .build();

    private static Computer upperCaseImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> toString(args.get(0)).compute().toUpperCase(Locale.ROOT), //
            () -> args.stream().anyMatch(Computer::isMissing));
    }

    public static final ExpressionFunction CAPITALIZE = functionBuilder() //
        .name("capitalize") //
        .description("string to title case") //
        .keywords("capitalise", "title_case") //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("str in title case", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::titleCaseImpl) //
        .build();

    private static Computer titleCaseImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
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
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction PAD_END = functionBuilder() //
        .name("pad_end") //
        .description("Right-pad a string to the specified length. " //
            + "\nIf the string is already that length or longer, do nothing") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("length", "the desired length", isIntegerOrOpt()), //
            optarg("char", "the char with which to pad (default: space)", isString()) //
        ) //
        .returnType("str padded to specified length", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::padEndImpl) //
        .build();

    private static Computer padEndImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            int targetLength = (int)toInteger(args.get(1)).compute();

            // If they've given us more than one char to pad with,
            // just use the first one.
            String charToAppend = args.size() == 3 //
                ? takeFirstChar(toString(args.get(2)).compute(), " ") //
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
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction PAD_START = functionBuilder() //
        .name("pad_start") //
        .description("""
                Left-pad a string to the specified length.
                If the string is already that length or longer, do nothing
                """) //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("length", "the desired length", isIntegerOrOpt()), //
            optarg("char", "the char with which to pad (default: space)", isString()) //
        ) //
        .returnType("str padded to specified length", "STRING?", args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::padStartImpl) //
        .build();

    private static Computer padStartImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            int targetLength = (int)toInteger(args.get(1)).compute();
            String charToPrepend = args.size() == 3 //
                ? takeFirstChar(toString(args.get(2)).compute(), " ") //
                : " ";

            StringBuilder output = new StringBuilder(targetLength);

            for (int i = 0; i < targetLength - str.length(); ++i) {
                output.append(charToPrepend);
            }
            return output.append(str).toString();
        };

        return StringComputer.of( //
            value, //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction JOIN = functionBuilder() //
        .name("join") //
        .description("Join several strings") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("sep", "separator with which to join the strings", isStringOrOpt()), //
            arg("string1", "first string", isStringOrOpt()), //
            vararg("strings...", "more strings", isStringOrOpt()) //
        ) //
        .returnType("strings joined with the specified separator", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::joinImpl) //
        .build();

    private static Computer joinImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String sep = toString(args.get(0)).compute();
            String[] toJoin = args.stream() //
                .skip(1) // skip over the first arg, which is the separator
                .map(StringFunctions::toString).map(StringComputer::compute) //
                .toArray(String[]::new);

            return String.join(sep, toJoin);
        };

        return StringComputer.of( //
            value, //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction SUBSTR = functionBuilder() //
        .name("substr") //
        .description("Get a substring of the string.") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("start", "start index (inclusive, 1-indexed)", isIntegerOrOpt()), //
            optarg("length", "length - if unspecified, or bigger than the string, get entire string after the start",
                isInteger()) //
        ) //
        .returnType("substring", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::substrImpl) //
        .build();

    private static Computer substrImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            int start = (int)toInteger(args.get(1)).compute();
            int length = args.size() == 3 //
                ? (int)toInteger(args.get(2)).compute() //
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
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction FIRST_CHARS = functionBuilder() //
        .name("first_chars") //
        .description("Get the first n characters from a string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("n", "number of characters to get from start", isIntegerOrOpt()) //
        ) //
        .returnType("substring", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::firstCharsImpl) //
        .build();

    private static Computer firstCharsImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            int numChars = (int)toInteger(args.get(1)).compute();

            // Clamp indices rather than erroring
            return str.substring( //
                0, //
                Math.min(numChars, str.length()) //
            );
        };

        return StringComputer.of( //
            value, //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction LAST_CHARS = functionBuilder() //
        .name("last_chars") //
        .description("Get the last n characters from a string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("n", "number of characters to get from end", isIntegerOrOpt()) //
        ) //
        .returnType("substring", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::lastCharsImpl) //
        .build();

    private static Computer lastCharsImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            int numChars = (int)toInteger(args.get(1)).compute();

            // Clamp indices rather than erroring
            return str.substring( //
                Math.max(str.length() - numChars, 0), //
                str.length() //
            );
        };

        return StringComputer.of( //
            value, //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction REMOVE_CHARS = functionBuilder() //
        .name("remove_chars") //
        .description("Remove the given chars from the string." //
            + "\nEquivalent to `replace_chars(str, chars, \"\")`") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("chars", "characters to delete", isStringOrOpt()) //
        ) //
        .returnType("string with characters removed", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::removeCharsImpl) //
        .build();

    private static Computer removeCharsImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();
            String toRemove = toString(args.get(1)).compute();
            String modifiers = extractModifiersOrDefault(args, 2);

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
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction STRIP = functionBuilder() //
        .name("strip") //
        .description("Remove leading and trailing whitespace from the string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the string with leading/trailing whitespace removed", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::stripImpl) //
        .build();

    private static Computer stripImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> toString(args.get(0)).compute().strip(), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction STRIP_START = functionBuilder() //
        .name("strip_start") //
        .description("Remove leading whitespace from the string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the string with leading whitespace removed", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::stripstartImpl) //
        .build();

    private static Computer stripstartImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> toString(args.get(0)).compute().stripLeading(), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction STRIP_END = functionBuilder() //
        .name("strip_end") //
        .description("Remove trailing whitespace from the string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the string with trailing whitespace removed", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::stripEndImpl) //
        .build();

    private static Computer stripEndImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> toString(args.get(0)).compute().stripTrailing(), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    private static final Pattern multiSpacePattern = Pattern.compile("( ){2,}");

    public static final ExpressionFunction REMOVE_DUPLICATE_SPACES = functionBuilder() //
        .name("remove_duplicate_spaces") //
        .description("Replace all duplicated spaces with a single space") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the string with all repeated spaces replaced", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::removeDuplicateSpacesImpl) //
        .build();

    private static Computer removeDuplicateSpacesImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> multiSpacePattern.matcher(toString(args.get(0)).compute()).replaceAll(" "), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction MISSING_TO_EMPTY = functionBuilder() //
        .name("missing_to_empty") //
        .description("If string is `MISSING`, return empty string. Otherwise just return the string.") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the string or empty", "STRING", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::nullToEmptyImpl) //
        .build();

    private static Computer nullToEmptyImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> args.get(0).isMissing() ? "" : toString(args.get(0)).compute(), //
            () -> false //
        );
    }

    public static final ExpressionFunction EMPTY_TO_MISSING = functionBuilder() //
        .name("empty_to_missing") //
        .description("If string is empty, return `MISSING`. Otherwise just return the string.") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the string or `MISSING`", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::emptyToNull) //
        .build();

    private static Computer emptyToNull(final List<Computer> args) {
        return StringComputer.of( //
            () -> toString(args.get(0)).compute(), //
            () -> args.get(0).isMissing() || toString(args.get(0)).compute().isEmpty() //
        );
    }

    public static final ExpressionFunction REVERSE = functionBuilder() //
        .name("reverse") //
        .description("Return the string, reversed") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the reversed string", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::reverseImpl) //
        .build();

    private static Computer reverseImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> new StringBuilder(toString(args.get(0)).compute()).reverse().toString(), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction LENGTH = functionBuilder() //
        .name("length") //
        .description("Get the number of character in a string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the length", "INTEGER?", //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::lengthImpl) //
        .build();

    private static Computer lengthImpl(final List<Computer> args) {
        return IntegerComputer.of( //
            () -> toString(args.get(0)).compute().length(), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction COUNT = functionBuilder() //
        .name("count") //
        .description("Get the number of occurences of the search term in a string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("search", "the search term", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" to ignore case, " //
                + "\"w\" to match only whole words", isString()) //
        ) //
        .returnType("the number of occurences", "INTEGER?", //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::countImpl) //
        .build();

    private static Computer countImpl(final List<Computer> args) {
        LongSupplier value = () -> {
            String str = toString(args.get(0)).compute();
            String search = toString(args.get(1)).compute();
            String modifiers = extractModifiersOrDefault(args, 2);

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
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction COUNT_CHARS = functionBuilder() //
        .name("count_chars") //
        .description("Count how many times the given characters appear in the string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("search", "the characters to search for", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" to ignore case" //
                + "\"v\" to count all chars except those provided", isString()) //
        ) //
        .returnType("the number of occurences", "INTEGER?", //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::countCharsImpl) //
        .build();

    private static Computer countCharsImpl(final List<Computer> args) {
        LongSupplier value = () -> {
            String str = toString(args.get(0)).compute();
            String searchChars = toString(args.get(1)).compute();
            String modifiers = extractModifiersOrDefault(args, 2);

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
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction FIND = functionBuilder() //
        .name("find") //
        .description("Find first or last occurence of given string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("search", "the string to search for", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" to ignore case, " //
                + "\"w\" to match whole words, " //
                + "\"b\" to search backwards", isString()) //
        ) //
        .returnType("the index of the first occurence (or `MISSING` if not found)", "INTEGER?", //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::findImpl) //
        .build();

    private static Computer findImpl(final List<Computer> args) {
        LongSupplier indexSupplier = () -> {
            String str = toString(args.get(0)).compute();
            String search = toString(args.get(1)).compute();
            String modifiers = extractModifiersOrDefault(args, 2);

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

            // One-indexing, so we add 1.
            if (backwards) {
                return match.results().reduce((a, b) -> b).get().start() + 1;
            } else {
                return match.results().findFirst().get().start() + 1;
            }
        };

        // If not found, return missing
        BooleanSupplier isMissing = () -> //
        args.stream().anyMatch(Computer::isMissing) //
            || indexSupplier.getAsLong() == -1;

        return IntegerComputer.of( //
            indexSupplier, //
            isMissing //
        );
    }

    public static final ExpressionFunction FIND_CHARS = functionBuilder() //
        .name("find_chars") //
        .description("Find the index of the first character that is also in chars") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()), //
            arg("chars", "the characters to search for", isStringOrOpt()), //
            optarg("modifiers", "(optional), \"i\" to ignore case, " //
                + "\"v\" to match all characters not provided, " //
                + "\"b\" to search backwards", isString()) //
        ) //
        .returnType("the index of the first occurence (or -1 if not found)", "INTEGER?", //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::findCharsImpl) //
        .build();

    private static Computer findCharsImpl(final List<Computer> args) {
        LongSupplier value = () -> {
            String str = toString(args.get(0)).compute();
            String search = toString(args.get(1)).compute();
            String modifiers = extractModifiersOrDefault(args, 2);

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

            OptionalInt ret = backwards //
                ? matchingdIndicesStream.reduce((a, b) -> b) //
                : matchingdIndicesStream.findFirst();

            // As always, one indexing
            return 1 + ret.orElse(-2);
        };

        return IntegerComputer.of( //
            value, //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction CHECKSUM_MD5 = functionBuilder() //
        .name("checksum_md5") //
        .description("""
                Get the MD5 checksum of the string.
                Note that MD5 is prone to hash collisions, and not safe for hashing passwords.
                """).keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the MD5 hash of the string", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::checksumMd5Impl) //
        .build();

    private static Computer checksumMd5Impl(final List<Computer> args) {
        Supplier<String> value = () -> {
            String str = toString(args.get(0)).compute();

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
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction XML_ENCODE = functionBuilder() //
        .name("xml_encode") //
        .description("Escape XML characters in a string") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the string with XML special characters escaped", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::xmlEncodeImpl) //
        .build();

    private static Computer xmlEncodeImpl(final List<Computer> args) {
        Supplier<String> value = () -> toString(args.get(0)).compute() //
            .replace("&", "&amp;") //
            .replace("<", "&lt;") //
            .replace(">", "&gt;") //
            .replace("\"", "&quot;") //
            .replace("'", "&apos;");

        return StringComputer.of( //
            value, //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction URL_ENCODE = functionBuilder() //
        .name("url_encode") //
        .description("Replace forbidden characters in a URL") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the string with forbidden characters escaped", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::urlEncodeImpl) //
        .build();

    private static Computer urlEncodeImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> URLEncoder.encode(toString(args.get(0)).compute(), StandardCharsets.UTF_8), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction URL_DECODE = functionBuilder() //
        .name("url_decode") //
        .description("Given a URL with forbidden characters escaped, recover the original URL") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("str", "the string", isStringOrOpt()) //
        ) //
        .returnType("the original URL, with encoding undone", "STRING?", //
            args -> ValueType.STRING(anyOptional(args))) //
        .impl(StringFunctions::urlDecodeImpl) //
        .build();

    private static Computer urlDecodeImpl(final List<Computer> args) {
        return StringComputer.of( //
            () -> URLDecoder.decode(toString(args.get(0)).compute(), StandardCharsets.UTF_8), //
            () -> args.stream().anyMatch(Computer::isMissing) //
        );
    }

    public static final ExpressionFunction TO_STRING = functionBuilder() //
        .name("string") //
        .description("Convert input to a string. A value that is `MISSING` will be converted to the string \"MISSING\"") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("i", "the input", isAnything()) //
        ) //
        .returnType("input as string", "STRING", args -> ValueType.STRING) //
        .impl(StringFunctions::toStringImpl) //
        .build();

    private static Computer toStringImpl(final List<Computer> args) {
        Supplier<String> value = () -> {
            var c = args.get(0);

            if (c.isMissing()) {
                return "MISSING";
            } else if (c instanceof BooleanComputer bc) {
                return String.valueOf(bc.compute());
            } else if (c instanceof FloatComputer fc) {
                return String.valueOf(fc.compute());
            } else if (c instanceof IntegerComputer ic) {
                return String.valueOf(ic.compute());
            } else if (c instanceof StringComputer sc) {
                return sc.compute();
            } else {
                throw FunctionUtils.calledWithIllegalArgs();
            }
        };

        return StringComputer.of(value, () -> false);
    }

        public static final ExpressionFunction PARSE_FLOAT = functionBuilder() //
        .name("parse_float") //
        .description("Convert a string to a float if possible, otherwise return `MISSING`") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("s", "the string", isStringOrOpt()) //
        ) //
        .returnType("float representation of the string, or MISSING", "FLOAT?", //
            args -> ValueType.FLOAT(anyOptional(args))) //
        .impl(StringFunctions::parseFloatImpl) //
        .build();

    private static Computer parseFloatImpl(final List<Computer> args) {
        return FloatComputer.of( //
            () -> Float.parseFloat(toString(args.get(0)).compute()), //
            () -> {
                if (args.stream().anyMatch(Computer::isMissing)) {
                    return true;
                }

                try {
                    Float.parseFloat(toString(args.get(0)).compute());
                } catch (NumberFormatException ex) {
                    return true;
                }

                return false;
            });
    }

    public static final ExpressionFunction PARSE_INT = functionBuilder() //
        .name("parse_int") //
        .description("Convert a string to an integer if possible, otherwise return `MISSING`") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("s", "the string", isStringOrOpt()) //
        ) //
        .returnType("integer representation of the string, or MISSING", "INTEGER?", //
            args -> ValueType.INTEGER(anyOptional(args))) //
        .impl(StringFunctions::parseIntImpl) //
        .build();

    private static Computer parseIntImpl(final List<Computer> args) {
        return IntegerComputer.of( //
            () -> Integer.parseInt(toString(args.get(0)).compute()), //
            () -> {
                if (args.stream().anyMatch(Computer::isMissing)) {
                    return true;
                }

                try {
                    Integer.parseInt(toString(args.get(0)).compute());
                } catch (NumberFormatException ex) {
                    return true;
                }

                return false;
            });
    }

    public static final ExpressionFunction PARSE_BOOL = functionBuilder() //
        .name("parse_bool") //
        .description("Convert a string to a boolean if possible, otherwise return `MISSING`") //
        .keywords() //
        .category(CATEGORY.name()) //
        .args( //
            arg("s", "the string", isStringOrOpt()) //
        ) //
        .returnType("boolean representation of the string, or MISSING", "BOOLEAN?", //
            args -> ValueType.BOOLEAN(anyOptional(args))) //
        .impl(StringFunctions::parseBoolImpl) //
        .build();

    private static Computer parseBoolImpl(final List<Computer> args) {
        return BooleanComputer.of( //
            () -> toString(args.get(0)).compute().equalsIgnoreCase("true"), //
            () -> {
                if (args.stream().anyMatch(Computer::isMissing)) {
                    return true;
                }

                var stringArg = toString(args.get(0)).compute();

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

    private static String extractModifiersOrDefault(final List<Computer> args, final int index) {
        if (args.size() > index) {
            return toString(args.get(index)).compute();
        } else {
            return "";
        }
    }
}
