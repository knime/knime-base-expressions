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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A container for the arguments of a function or aggregation. Supports both positional and named arguments.
 *
 * @param <T> the type of the arguments
 * @param positionalArguments a list of positional arguments
 * @param namedArguments a map of named arguments
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public record Arguments<T>(List<T> positionalArguments, Map<String, T> namedArguments) {

    /**
     * @return all arguments as a list. The named arguments are positioned after the positional arguments but the order
     *         of the named arguments is not guaranteed.
     */
    public List<T> asList() {
        var l = new ArrayList<>(positionalArguments);
        l.addAll(namedArguments.values());
        return l;
    }

    /**
     * Applies a mapping function to all arguments.
     *
     * @param <O> the type of the mapped arguments
     * @param mapper the mapping function
     * @return a new {@link Arguments} object with the mapped arguments
     */
    public <O> Arguments<O> map(final Function<T, O> mapper) {
        return new Arguments<>( //
            positionalArguments.stream().map(mapper).toList(), //
            namedArguments.entrySet().stream().collect( //
                Collectors.toMap(Entry::getKey, e -> mapper.apply(e.getValue())) //
            ) //
        );
    }

    /**
     * Renders the arguments as a string.
     *
     * @param argRenderer a function that renders an individual argument
     * @return a string representation of the arguments
     */
    public String renderArgumentList(final Function<T, String> argRenderer) {
        return "(" //
            + Stream.concat( //
                positionalArguments.stream().map(argRenderer), //
                namedArguments.entrySet().stream().map(e -> e.getKey() + "=" + argRenderer.apply(e.getValue())) //
            ).collect(Collectors.joining(", ")) //
            + ")";
    }

    @Override
    public String toString() {
        return renderArgumentList(Object::toString);
    }
}
