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
 *   June 17, 2024 (david): created
 */
package org.knime.core.expressions;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Return result that contains either the return value or an error message.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @param <T>
 */
public sealed interface ReturnResult<T> permits ReturnResult.Success, ReturnResult.Failure {

    /**
     * Check if the return result is successful. Opposite of {@link #isError()}.
     *
     * @return <code>true</code> if the return result is successful, <code>false</code> otherwise
     */
    boolean isOk();

    /**
     * Check if the return result is an error. Opposite of {@link #isOk()}.
     *
     * @return <code>true</code> if the return result is an error, <code>false</code> otherwise
     */
    default boolean isError() {
        return !isOk();
    }

    /**
     * Get the return value. Only call this method if {@link #isOk()} returns <code>true</code>.
     *
     * @return the return value
     * @throws IllegalStateException if the return value is not available
     */
    T getValue();

    /**
     * Get the error message. Only call this method if {@link #isError()} returns <code>true</code>.
     *
     * @return the error message
     * @throws IllegalStateException if the error message is not available
     */
    String getErrorMessage();

    /**
     * Map the return value to a new return value, if it is present. Otherwise, do nothing.
     *
     * @param <R> the type of the new return value
     * @param mapper the function to map the return value to a new return value
     * @return the new return result
     */
    <R> ReturnResult<R> map(final Function<T, R> mapper);

    /**
     * Map the return value to a new return value, if it is present. Otherwise, do nothing.
     * <P>
     * This function is similar to {@link #map(Function)} but allows the mapping function to convert the value into an
     * error.
     *
     * @param <R> the type of the new return value
     * @param mapper the function to map the return value to a new return value or error
     * @return the new return result or error
     */
    <R> ReturnResult<R> flatMap(final Function<T, ReturnResult<R>> mapper);

    /**
     * Filter the return value with a predicate. If the predicate is not satisfied, return an error return result.
     *
     * @param filter the predicate to filter the return value
     * @param errorMessage the error message if the predicate is not satisfied
     * @return the new return result
     */
    ReturnResult<T> filter(final Predicate<T> filter, final String errorMessage);

    /**
     * If this return result is empty, replace it with the return value from the given supplier. Otherwise return the
     * current result.
     *
     * @param otherSupplier the supplier to provide a return value if this return result is empty
     * @return the new return result, or this one if it is not empty
     */
    ReturnResult<T> or(Supplier<T> otherSupplier);

    /**
     * Get the value or if it is an error, map the error message to an alternative value.
     *
     * @param other a function that provides a return value if this return result is empty
     * @return the value
     */
    T orElseGet(Function<String, ? extends T> other);

    /**
     * Get the value or if it is an error, throw an exception with the error message.
     *
     * @param <E> the type of the exception
     * @param exceptionSupplier a function that provides an exception if this return result
     * @return the value if it is present
     * @throws E if the return result is an error
     *
     */
    <E extends Exception> T orElseThrow(Function<String, E> exceptionSupplier) throws E;

    /**
     * Return result that contains a return value. Use {@link ReturnResult#success} to create a new {@link Success}.
     *
     * @param <T>
     */
    final class Success<T> implements ReturnResult<T> {

        private final T m_returnValue;

        private Success(final T returnValue) {
            m_returnValue = Objects.requireNonNull(returnValue);
        }

        private Success() {
            m_returnValue = null;
        }

        @Override
        public T getValue() {
            return m_returnValue;
        }

        @Override
        public String getErrorMessage() {
            throw new IllegalStateException("No error message available for successful return value");
        }

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public <R> ReturnResult<R> map(final Function<T, R> mapper) {
            return new Success<>(mapper.apply(m_returnValue));
        }

        @Override
        public <R> ReturnResult<R> flatMap(final Function<T, ReturnResult<R>> mapper) {
            return mapper.apply(m_returnValue);
        }

        @Override
        public ReturnResult<T> filter(final Predicate<T> filter, final String errorMessage) {
            return filter.test(m_returnValue) ? this : new Failure<>(errorMessage);
        }

        @Override
        public ReturnResult<T> or(final Supplier<T> otherSupplier) {
            return this;
        }

        @Override
        public T orElseGet(final Function<String, ? extends T> other) {
            return getValue();
        }

        @Override
        public <E extends Exception> T orElseThrow(final Function<String, E> exceptionSupplier) throws E {
            return getValue();
        }
    }

    /**
     * Return result that contains an error message. Use {@link ReturnResult#failure} to create a new {@link Failure}.
     *
     * @param <T>
     */
    final class Failure<T> implements ReturnResult<T> {

        private final String m_errorMessage;

        private Failure(final String errorMessage) {
            m_errorMessage = errorMessage;
        }

        @Override
        public String getErrorMessage() {
            return m_errorMessage;
        }

        @Override
        public T getValue() {
            throw new IllegalStateException(
                "No value available for error return value. Error message is: " + m_errorMessage);
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public <R> ReturnResult<R> map(final Function<T, R> mapper) {
            return new Failure<>(m_errorMessage);
        }

        @Override
        public <R> ReturnResult<R> flatMap(final Function<T, ReturnResult<R>> mapper) {
            return new Failure<>(m_errorMessage);
        }

        @Override
        public ReturnResult<T> filter(final Predicate<T> filter, final String errorMessage) {
            return this;
        }

        @Override
        public ReturnResult<T> or(final Supplier<T> otherSupplier) {
            return new Success<>(otherSupplier.get());
        }

        @Override
        public T orElseGet(final Function<String, ? extends T> other) {
            return other.apply(m_errorMessage);
        }

        @Override
        public <E extends Exception> T orElseThrow(final Function<String, E> exceptionSupplier) throws E {
            throw exceptionSupplier.apply(m_errorMessage);
        }
    }

    /**
     * Create a successful return result with the given value.
     *
     * @param value the return value
     * @return the new successful return result
     */
    static <T> ReturnResult<T> success(final T value) {
        return new Success<>(value);
    }

    /**
     * Create a successful return result without a value just indicating success.
     *
     * @return the new successful return result
     */
    static ReturnResult<Void> success() {
        return new Success<>();
    }

    /**
     * Create a failure return result with the given error message.
     *
     * @param errorMessage the error message
     * @return the new failure return result
     */
    static <T> ReturnResult<T> failure(final String errorMessage) {
        return new Failure<>(errorMessage);
    }

    /**
     * Create a return result from an optional value. If the optional value is present, return a successful return
     * result. Otherwise, return a failure return result with the given error message.
     *
     * @param optional the optional value
     * @param errorMessage the error message if the optional is not present
     *
     * @return the new return result. Successful if the optional is present, otherwise a failure
     */
    static <T> ReturnResult<T> fromOptional(final Optional<T> optional, final String errorMessage) { // NOSONAR optional here is ok
        return optional.isPresent() ? success(optional.get()) : failure(errorMessage);
    }

    /**
     * Create a return result from a nullable value. If the value is not null, return a successful return result.
     * Otherwise, return a failure return result with the given error message.
     *
     * @param value the nullable value
     * @param errorMessage the error message if the value is null
     *
     * @return the new return result. Successful if the value is not null, otherwise a failure
     */
    static <T> ReturnResult<T> fromNullable(final T value, final String errorMessage) {
        return value != null ? success(value) : failure(errorMessage);
    }
}
