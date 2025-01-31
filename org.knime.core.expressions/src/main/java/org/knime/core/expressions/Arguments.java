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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A container for the arguments of a function or aggregation. Consists of a map of named arguments and a list of
 * variable arguments. Operators decide how they want to handle named and variable arguments.
 *
 * @param <T> the type of the arguments
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Tobias Kampmann, TNG, Schwerte, Germany
 */
public class Arguments<T> {

    private final Map<String, T> m_namedArguments;

    private final List<T> m_varArgument;

    /**
     * @param namedArguments a map of named arguments. Can be empty but not null.
     * @param varArgument a list of arguments. Can be empty but not null.
     */
    public Arguments(final Map<String, T> namedArguments, final List<T> varArgument) {
        Objects.requireNonNull(namedArguments, "Named arguments must not be null.");
        Objects.requireNonNull(varArgument, "Variable argument must not be null.");
        m_namedArguments = namedArguments;
        m_varArgument = varArgument;
    }

    /**
     * Get an argument by name or the default if it is missing.
     *
     * @param name
     * @param defaultIfMissing
     * @return the argument
     */
    public T get(final String name, final T defaultIfMissing) {
        return m_namedArguments.getOrDefault(name, defaultIfMissing);
    }

    /**
     * Get an argument by name. Throws if the argument is missing.
     *
     * @param name
     * @return the argument
     * @throws NoSuchElementException if the argument is missing
     */
    public T get(final String name) {
        if (m_namedArguments.containsKey(name)) {
            return m_namedArguments.get(name);
        }
        throw new NoSuchElementException("Argument '" + name + "' does not exist.");
    }

    /**
     * @param name
     * @return <code>true</code> if the argument is present, <code>false</code> otherwise
     */
    public boolean has(final String name) {
        return m_namedArguments.containsKey(name);
    }

    /**
     * Get the variable argument. Can be empty.
     *
     * @return the variable argument
     */
    public List<T> getVariableArgument() {
        return Collections.unmodifiableList(m_varArgument);
    }

    /**
     * @return all named arguments
     */
    public Map<String, T> getNamedArguments() {
        return Collections.unmodifiableMap(m_namedArguments);
    }

    /**
     * Create a list of all arguments. Named arguments are followed my variable arguments.
     *
     * @return a list of arguments
     */
    public List<T> toList() {
        var list = new ArrayList<>(m_namedArguments.values());
        list.addAll(m_varArgument);
        return list;
    }

    /**
     * Allow to conveniently stream over all arguments.
     *
     * @return a stream of all arguments
     */
    public Stream<T> stream() {
        return this.toList().stream();
    }

    /**
     * @return the number of arguments stored in this object
     */
    public int getNumberOfArguments() {
        return m_namedArguments.size() + m_varArgument.size();
    }

    /**
     * Check if any argument matches the predicate.
     *
     * @param <E> the type of the exception if the predicate throws an exception
     * @param predicate the predicate to test
     * @return true if any argument matches, false otherwise
     * @throws E if the predicate throws an exception while testing the arguments
     */
    public <E extends Exception> boolean anyMatch(final PredicateWithException<? super T, E> predicate) throws E {
        for (T t : toList()) {
            if (predicate.test(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if all arguments match the predicate
     *
     * @param <E> the type of the exception if the predicate throws an exception
     * @param predicate the predicate to test
     * @return true if all arguments match the predicate, false otherwise
     * @throws E if the predicate throws an exception while testing the arguments
     */
    public <E extends Exception> boolean allMatch(final PredicateWithException<? super T, E> predicate) throws E {
        for (T t : toList()) {
            if (!predicate.test(t)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Map all arguments to new objects using the given mapper.
     *
     * @param <O> the type of the output arguments
     * @param <E> the type of the exception if the mapper throws
     * @param mapper the mapper to apply to all arguments
     * @return a new Arguments object with the mapped arguments
     * @throws E if the mapper throws an exception
     */
    public <O, E extends Exception> Arguments<O> map(final FunctionWithException<T, O, E> mapper) throws E {
        var mappedNamedArguments = new HashMap<String, O>();
        for (Entry<String, T> arg : m_namedArguments.entrySet()) {
            mappedNamedArguments.put(arg.getKey(), mapper.apply(arg.getValue()));
        }

        var mappedVarArgument = new ArrayList<O>();
        for (T arg : m_varArgument) {
            mappedVarArgument.add(mapper.apply(arg));
        }
        return new Arguments<>(mappedNamedArguments, mappedVarArgument);
    }

    @Override
    public String toString() {
        return "Arguments[named=" + m_namedArguments + ", var=" + m_varArgument + "]";
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Arguments<?> other) {
            return m_namedArguments.equals(other.m_namedArguments) && m_varArgument.equals(other.m_varArgument);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_namedArguments, m_varArgument);
    }

    /**
     * A function that can throw an exception.
     *
     * @param <T> the type of the parameter
     * @param <O> the type of the mapped parameter
     * @param <E> the type of the exception
     */
    @FunctionalInterface
    public interface FunctionWithException<T, O, E extends Exception> {
        /**
         * @param t the input argument
         * @return the result of the function
         * @throws E
         */
        O apply(T t) throws E;
    }

    /**
     * A predicate that can throw an exception.
     *
     * @param <T> the type of the parameter
     * @param <E> the type of the exception
     */
    @FunctionalInterface
    public interface PredicateWithException<T, E extends Exception> {
        /**
         * @param t the input argument
         * @return {@code true} if the input argument matches the predicate,
         * @throws E
         */
        boolean test(T t) throws E;
    }
}
