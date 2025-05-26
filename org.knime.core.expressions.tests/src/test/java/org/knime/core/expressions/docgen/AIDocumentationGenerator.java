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
 *   May 26, 2025 (benjaminwilhelm): created
 */
package org.knime.core.expressions.docgen;

import java.util.Collection;
import java.util.stream.Collectors;

import org.knime.core.expressions.ExpressionConstants;
import org.knime.core.expressions.NamedExpressionOperator;
import org.knime.core.expressions.OperatorDescription.Argument;
import org.knime.core.expressions.aggregations.BuiltInAggregations;
import org.knime.core.expressions.functions.BuiltInFunctions;

/**
 * Generates the documentation for the AI service (K-AI), including constants, functions, and aggregations.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public class AIDocumentationGenerator {

    /**
     * Main method to generate the documentation for the AI service. Run this file from Eclipse as a "Java Application".
     *
     * @param args command line arguments (not used)
     */
    public static void main(final String[] args) {
        // Constants
        printConstantsDocumentation();

        // Functions
        printFileSeparator("knime_expression_functions.inc");
        printFunctionsDocumentation();

        // Aggregations
        printFileSeparator("knime_expression_aggregations.inc");
        printAggregationsDocumentation();

        // Date/Time functions
        printFileSeparator("knime_expression_datetime.inc");
        printDateTimeDocumentation();
    }

    private static void printConstantsDocumentation() {
        System.out.println("# Available constants:\n");
        for (var constant : ExpressionConstants.values()) {
            var description = llmDescriptionOfConstant(constant);
            description = description.isEmpty() ? "" : " (" + description + ")";
            System.out.println("* " + constant.name() + description);
        }
    }

    private static String llmDescriptionOfConstant(final ExpressionConstants constant) {
        return switch (constant) {
            case PI -> "";
            case E -> "";
            case INFINITY -> "";
            case NaN -> "Not a number, e.g. as a result of division by zero";
            case MIN_INTEGER -> "Smallest integer representable by this computer";
            case MAX_INTEGER -> "Largest integer";
            case MIN_FLOAT -> "Smallest float";
            case MAX_FLOAT -> "Largest float";
            case TINY_FLOAT -> "Smallest positive float";
            case TRUE -> "";
            case FALSE -> "";
            case MISSING -> "";
        };
    }

    private static void printFunctionsDocumentation() {
        // Do not include temporal functions in the documentation (they are listed separately)
        var functions = BuiltInFunctions.BUILT_IN_FUNCTIONS.stream() //
            .filter(op -> !op.description().category().startsWith("Temporal")) //
            .toList();

        System.out.println("# Available functions\n");
        System.out.println("You can ONLY use these functions listed here:\n");
        System.out.println("# Functions");
        System.out.println(generateSubdocumentation(functions, "Function"));
    }

    private static void printAggregationsDocumentation() {
        System.out.println("# Column aggregations\n");
        System.out
            .println("The column argument is always required and must be a column name formatted as a string literal.");
        System.out
            .println("All other arguments are optional and have default values, which can be passed positionally");
        System.out.println("or by their name in the format `argname=value`. Optional arguments must be literals.\n");
        System.out.println("You can ONLY use these aggregations listed here:\n");
        System.out.println(generateSubdocumentation(BuiltInAggregations.BUILT_IN_AGGREGATIONS, "Aggregation"));
    }

    private static void printDateTimeDocumentation() {
        var dateTimeFunctions = BuiltInFunctions.BUILT_IN_FUNCTIONS.stream() //
            .filter(op -> op.description().category().startsWith("Temporal")) //
            .toList();

        System.out.println(
            """
                    # Date&Time and Durations

                    *Types* `LOCAL_DATE`, `LOCAL_TIME`, `LOCAL_DATE_TIME`, `ZONED_DATE_TIME`; durations `DATE_DURATION`, `TIME_DURATION`.
                    *Operators* `+ / -` date-time ± duration, duration ± duration; `-` date-time – date-time → duration; `*` duration × integer; comparisons `< > <= >= = !=` (no ordering for `DATE_DURATION`, no equality for `ZONED_DATE_TIME`).

                    You can ONLY use these functions listed here:
                    """);
        System.out.println(generateSubdocumentation(dateTimeFunctions, "Date/Time Function"));
    }

    private static String generateSubdocumentation(final Collection<? extends NamedExpressionOperator> operators,
        final String typeName) {
        StringBuilder sb = new StringBuilder();

        for (var operator : operators) {
            sb.append("").append("\n");
            sb.append("## " + typeName + " `" + operator.name() + "("
                + operator.description().arguments().stream().map(Argument::name).collect(Collectors.joining(", "))
                + ")`\n");
            sb.append("### Arguments\n");
            for (var arg : operator.description().arguments()) {
                sb.append(
                    "- " + arg.name() + " [" + arg.type() + "]: " + arg.description().replaceAll("\n", " ") + "\n");
            }
            sb.append("### Returns\n");
            sb.append(
                "[" + operator.description().returnType() + "] " + operator.description().returnDescription() + "\n");
            sb.append("### Description\n");
            sb.append(getShortDescription(operator.description().description()) + "\n");
        }

        return sb.toString();
    }

    private static String getShortDescription(final String description) {
        // We basically want to extract the first paragraph of the description
        return description.split("\n\n")[0].replaceAll("\n", " ").trim();
    }

    private static void printFileSeparator(final String fileName) {
        System.out.println("------------------------------ " + fileName + " --------------------------------\n");
    }
}
