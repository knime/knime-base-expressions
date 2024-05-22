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
 *   May 22, 2024 (benjamin): created
 */
package org.knime.core.expressions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A description of a function or aggregation.
 *
 * @param name
 * @param description a description which can contain Markdown
 * @param arguments the arguments
 * @param returnType the type of the return value
 * @param returnDescription a description of the return value
 * @param keywords
 * @param category
 */
public record OperatorDescription(String name, String description, List<OperatorDescription.Argument> arguments,
    String returnType, String returnDescription, List<String> keywords, String category) {

    /**
     * The description of a function argument.
     *
     * @param name
     * @param type
     * @param description
     */
    public record Argument(String name, String type, String description) {

        /**
         * Match the positional and named arguments to the signature.
         *
         * @param <T> the type of the arguments
         * @param signature the signature of the function
         * @param arguments the arguments
         * @return the matched arguments or {@link Optional#empty()} if the arguments do not match the signature
         */
        public static <T> Optional<Map<String, T>> matchSignature(final List<OperatorDescription.Argument> signature,
            final Arguments<T> arguments) {

            // TODO(AP-22303) return an error instead of Optional.empty
            // TODO(AP-22303) add support for varargs

            var argumentsMap = new HashMap<String, T>();

            // Add positional arguments to the map
            for (var i = 0; i < arguments.positionalArguments().size(); i++) {
                if (i >= signature.size()) {
                    // Too many arguments
                    return Optional.empty();
                }
                argumentsMap.put(signature.get(i).name(), arguments.positionalArguments().get(i));
            }

            // Add named arguments to the map
            var argSigIds = signature.stream().map(OperatorDescription.Argument::name).collect(Collectors.toSet());
            for (final var entry : arguments.namedArguments().entrySet()) {
                var name = entry.getKey();
                if (argumentsMap.containsKey(name)) {
                    // Argument already set
                    return Optional.empty();
                }
                if (!argSigIds.contains(name)) {
                    // No argument with this identifier
                    return Optional.empty();
                }
                argumentsMap.put(name, entry.getValue());
            }
            return Optional.of(argumentsMap);
        }
    }
}
