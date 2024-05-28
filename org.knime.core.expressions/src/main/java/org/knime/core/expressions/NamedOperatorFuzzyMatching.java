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
 *   May 31, 2024 (david): created
 */
package org.knime.core.expressions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 * Helper class for fuzzy matching of operator names.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class NamedOperatorFuzzyMatching {

    // Based on testing, it seems like 4 is a good maximum Levenshtein distance
    public static final int MAX_LEVENSHTEIN_DISTANCE = 4;

    private NamedOperatorFuzzyMatching() {
        // prevent instantiation
    }

    /**
     * Case-insensitive Levenshtein distance between two strings.
     *
     * @param s1 first string
     * @param s2 second string
     * @return Levenshtein distance between the two strings
     */
    private static int stringDistance(String s1, String s2) {
        // Let's do this case insensitively
        s1 = s1.toLowerCase(Locale.ROOT);
        s2 = s2.toLowerCase(Locale.ROOT);

        return LevenshteinDistance.getDefaultInstance().apply(s1, s2);
    }

    /**
     * Find the shortest Levenshtein distance between a target string and a collection of strings
     *
     * @param aliases the collection of strings to compare to the target
     * @param s the target string
     * @return the shortest Levenshtein distance
     */
    private static int shortestStringDistance(final Collection<String> aliases, final String s) {
        return aliases.stream().mapToInt(alias -> stringDistance(alias, s)).min()
            .orElseThrow(() -> new IllegalArgumentException("No strings to compare"));
    }

    /**
     * Find the operators with the smallest Levenshtein distance to the given operator name.
     *
     * @param invalidOperatorName the invalid operator name
     * @param operators the operators to compare to
     * @return the operators with the smallest Levenshtein distance to the given operator name
     */
    public static List<String> findMostSimilarlyNamedOperators(final String invalidOperatorName,
        final Map<String, ? extends NamedExpressionOperator> operators) {

        // Find functions with the smallest Levenshtein distance to the given function name
        Map<String, Integer> distances = new HashMap<>();

        // Tune max distance for short invalid function names. Otherwise we get
        // things like 'abc' matching every single 2-4 letter function name.
        var maxDistance = Math.min(MAX_LEVENSHTEIN_DISTANCE, invalidOperatorName.length() - 1);

        // Calculate Levenshtein distance from all function names and keywords to
        // the provided invalid function name
        for (var entry : operators.entrySet()) {
            var validOperatorName = entry.getKey();

            List<String> operatorNameAndAliases = new ArrayList<>();
            operatorNameAndAliases.addAll(entry.getValue().description().keywords());
            operatorNameAndAliases.add(validOperatorName);

            int distance = shortestStringDistance(operatorNameAndAliases, invalidOperatorName);

            distances.put(validOperatorName, distance);
        }

        // Find the keys with the smallest distance (ties are allowed)
        var minDistance = distances.values().stream().min(Integer::compare);

        // If the nearest function is too far away (or we have no matched functions), return nothing
        if (minDistance.isEmpty() || minDistance.get() > maxDistance) {
            return List.of();
        } else {
            return distances.entrySet().stream() //
                .filter(entry -> entry.getValue().equals(minDistance.get())) //
                .map(Map.Entry::getKey) //
                .toList();
        }
    }

}
