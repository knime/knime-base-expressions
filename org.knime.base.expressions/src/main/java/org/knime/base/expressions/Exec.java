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
 */
package org.knime.base.expressions;

import static org.knime.core.expressions.Expressions.getResolvedColumnIdx;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionCompileException;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.ValueType;
import org.knime.core.table.access.BooleanAccess;
import org.knime.core.table.access.BooleanAccess.BooleanReadAccess;
import org.knime.core.table.access.ByteAccess.ByteReadAccess;
import org.knime.core.table.access.DoubleAccess;
import org.knime.core.table.access.DoubleAccess.DoubleReadAccess;
import org.knime.core.table.access.FloatAccess.FloatReadAccess;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.LongAccess;
import org.knime.core.table.access.LongAccess.LongReadAccess;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.StringAccess;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.schema.BooleanDataSpec;
import org.knime.core.table.schema.ByteDataSpec;
import org.knime.core.table.schema.ColumnarSchema;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.DataSpecs;
import org.knime.core.table.schema.DataSpecs.DataSpecWithTraits;
import org.knime.core.table.schema.DoubleDataSpec;
import org.knime.core.table.schema.FloatDataSpec;
import org.knime.core.table.schema.IntDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.LongDataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec;
import org.knime.core.table.schema.VoidDataSpec;
import org.knime.core.table.virtual.VirtualTable;
import org.knime.core.table.virtual.spec.MapTransformSpec;
import org.knime.core.table.virtual.spec.MapTransformSpec.MapperFactory;
import org.knime.core.table.virtual.spec.RowFilterTransformSpec.RowFilterFactory;

/**
 * Utilities to evaluate {@link Expressions} on {@link VirtualTable virtual tables}.
 *
 * @author Tobias Pietzsch
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class Exec {

    private Exec() {
    }

    /**
     * A visitor that maps {@link DataSpec} to a function that creates a {@link Computer} for a {@link ReadAccess}. The
     * visitor throws an {@link IllegalArgumentException} if the {@link DataSpec} cannot be used as expression inputs.
     */
    public static final DataSpec.Mapper<Function<ReadAccess, ? extends Computer>> DATA_SPEC_TO_READER_FACTORY =
        new ReaderFactoryMapper();

    /**
     * A visitor that maps {@link DataSpec} to a function that creates a {@link Runnable} for writing results from a
     * {@link Computer} to a {@link WriteAccess}. The visitor throws an {@link IllegalArgumentException} if the
     * {@link DataSpec} cannot be used as an expression output.
     */
    private static final DataSpec.Mapper<TriFunction<WriteAccess, Computer, EvaluationContext, Runnable>> //
    /**/ DATA_SPEC_TO_WRITER_FACTORY = new WriterFactoryMapper();

    /**
     * Create a {@link MapperFactory}, that is, the final realization of the expression.
     * <p>
     * Note that the mapper of the returned {@link MapperFactory} might throw a
     * {@link ExpressionEvaluationRuntimeException}. Make sure to catch this exception while executing the mapper and
     * unwrap it to get the original {@link ExpressionEvaluationException}.
     *
     * @param ast the expression
     * @param columnIndexToComputerFactory function that maps column index to a factory that produces {@code Computer}
     *            (of matching type) from the {@code ReadAccess[]} array given to the {@code MapperFactory}
     * @param flowVariableToComputer a function that maps flow variable access to a Computer which evaluates to the
     *            value of the variable
     * @param aggregationToComputer a function that maps aggregation calls to a Computer which evaluates to the value of
     *            the aggregation
     * @param ctx the {@link EvaluationContext} that functions can use to raise a warning
     * @return a {@link MapperFactory} that implements the given expression
     */
    public static MapperFactory createMapperFactory(final Ast ast,
        final IntFunction<Function<ReadAccess[], ? extends Computer>> columnIndexToComputerFactory,
        final Function<Ast.FlowVarAccess, Optional<Computer>> flowVariableToComputer,
        final Function<Ast.AggregationCall, Optional<Computer>> aggregationToComputer, final EvaluationContext ctx) {

        var outputSpec = valueTypeToDataSpec(Expressions.getInferredType(ast));
        final TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> writerFactory =
            outputSpec.spec().accept(DATA_SPEC_TO_WRITER_FACTORY);
        final Function<ReadAccess[], Computer> computerFactory =
            createComputerFactory(ast, columnIndexToComputerFactory, flowVariableToComputer, aggregationToComputer);
        final BiFunction<ReadAccess[], WriteAccess[], Runnable> factory = (readAccesses, writeAccesses) -> writerFactory
            .apply(writeAccesses[0], computerFactory.apply(readAccesses), ctx);

        return MapperFactory.of(ColumnarSchema.of(outputSpec), factory);
    }

    /**
     * Create a {@code RowFilterFactory}, that includes only rows for which the expression evaluates to
     * <code>true</code>.
     * <p>
     * Note that the filter of the returned {@link RowFilterFactory} might throw a
     * {@link ExpressionEvaluationRuntimeException}. Make sure to catch this exception while executing the filter and
     * unwrap it to get the original {@link ExpressionEvaluationException}.
     *
     * @param ast the expression
     * @param columnIndexToComputerFactory function that maps column index to a factory that produces {@code Computer}
     *            (of matching type) from the {@code ReadAccess[]} array given to the {@code MapperFactory}
     * @param flowVariableToComputer a function that maps flow variable access to a Computer which evaluates to the
     *            value of the variable
     * @param aggregationToComputer a function that maps aggregation calls to a Computer which evaluates to the value of
     *            the aggregation
     * @param ctx the {@link EvaluationContext} that functions can use to raise a warning
     * @return a {@link RowFilterFactory}
     * @throws IllegalArgumentException if the output type of the expression is not {@link ValueType#BOOLEAN}
     */
    public static RowFilterFactory createRowFilterFactory(final Ast ast,
        final IntFunction<Function<ReadAccess[], ? extends Computer>> columnIndexToComputerFactory,
        final Function<Ast.FlowVarAccess, Optional<Computer>> flowVariableToComputer,
        final Function<Ast.AggregationCall, Optional<Computer>> aggregationToComputer, final EvaluationContext ctx) {
        var outputType = Expressions.getInferredType(ast);
        if (!ValueType.BOOLEAN.equals(outputType)) {
            throw new IllegalArgumentException(
                "The expression must evaluate to BOOLEAN. Got " + outputType.name() + ".");
        }
        final Function<ReadAccess[], Computer> computerFactory =
            createComputerFactory(ast, columnIndexToComputerFactory, flowVariableToComputer, aggregationToComputer);
        return inputs -> () -> {
            try {
                return ((BooleanComputer)computerFactory.apply(inputs)).compute(ctx);
            } catch (ExpressionEvaluationException ex) {
                throw new ExpressionEvaluationRuntimeException(ex);
            }
        };
    }

    /**
     * @return a function that creates a {@link Computer} that evaluates the expression based on the input of the given
     *         {@link ReadAccess read accesses}
     */
    private static Function<ReadAccess[], Computer> createComputerFactory(final Ast ast,
        final IntFunction<Function<ReadAccess[], ? extends Computer>> columnIndexToComputerFactory,
        final Function<Ast.FlowVarAccess, Optional<Computer>> flowVariableToComputer,
        final Function<Ast.AggregationCall, Optional<Computer>> aggregationToComputer) {
        return readAccesses -> {
            Function<ColumnAccess, Optional<Computer>> columnToComputer = columnAccess -> Optional
                .of(columnIndexToComputerFactory.apply(getResolvedColumnIdx(columnAccess)).apply(readAccesses));

            try {
                return Expressions.evaluate(ast, columnToComputer, flowVariableToComputer, aggregationToComputer);
            } catch (ExpressionCompileException ex) {
                // NB: We never use Optional.empty() for the column computer
                throw new IllegalStateException(ex);
            }
        };
    }

    /**
     * @param valueType a {@link ValueType} of an expression
     * @return the {@link DataSpec} of data that represents the same set of values (note: results of expression
     *         evaluation can be written to data of this type)
     */
    public static DataSpecWithTraits valueTypeToDataSpec(final ValueType valueType) {
        if (ValueType.BOOLEAN.equals(valueType.baseType())) {
            return DataSpecs.BOOLEAN;
        } else if (ValueType.INTEGER.equals(valueType.baseType())) {
            return DataSpecs.LONG;
        } else if (ValueType.FLOAT.equals(valueType.baseType())) {
            return DataSpecs.DOUBLE;
        } else if (ValueType.STRING.equals(valueType.baseType())) {
            return DataSpecs.STRING;
        } else {
            throw new IllegalArgumentException("The value type " + valueType.name() + " cannot be mapped to DataSpecs");
        }
    }

    /**
     * Exception throw by the {@link MapperFactory} and {@link RowFilterFactory} when the expression cannot be evaluated
     * on the current input data.
     */
    public static final class ExpressionEvaluationRuntimeException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private ExpressionEvaluationRuntimeException(final ExpressionEvaluationException cause) {
            super(cause);
        }

        /**
         * The {@link ExpressionEvaluationException} that caused this runtime exception.
         * <p>
         * {@inheritDoc}
         */
        @Override
        public synchronized ExpressionEvaluationException getCause() {
            return (ExpressionEvaluationException)super.getCause();
        }
    }

    /**
     * A map from the input of the {@link MapTransformSpec.MapperFactory#createMapper mapper} function to the full table
     * column index (that is {@link Expressions#getResolvedColumnIdx(Ast.ColumnAccess)}). For example, if an expression
     * uses (only) <code>$["second column"]</code> and <code>$["fifth column"]</code> this would result in
     * <code>columnIndices = [1, 4]</code>
     *
     * @param columnIndices
     */
    public record RequiredColumns(int[] columnIndices) {

        /**
         * @param expression
         * @return the {@link RequiredColumns} of all {@link Ast.ColumnAccess} nodes
         */
        public static RequiredColumns of(final Ast expression) {
            var nodes = Ast.postorder(expression);
            int[] columnIndices = nodes.stream().mapToInt(node -> {
                if (node instanceof Ast.ColumnAccess n) {
                    return getResolvedColumnIdx(n);
                } else {
                    return -1;
                }
            }).filter(i -> i != -1).distinct().toArray();
            return new RequiredColumns(columnIndices);
        }

        /**
         * @param columnIndex the index of the table column
         * @return the input index of the mapper function that corresponds to this column
         */
        public int getInputIndex(final int columnIndex) {
            for (int i = 0; i < columnIndices.length; i++) {
                if (columnIndices[i] == columnIndex) {
                    return i;
                }
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString() {
            return "RequiredColumns" + Arrays.toString(columnIndices);
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof RequiredColumns o) {
                return Arrays.equals(columnIndices, o.columnIndices);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(columnIndices);
        }
    }

    private static class ReaderFactoryMapper implements DataSpec.Mapper<Function<ReadAccess, ? extends Computer>> {

        @Override
        public Function<ReadAccess, BooleanComputer> visit(final BooleanDataSpec spec) {
            return ra -> BooleanComputer.of(ctx -> ((BooleanReadAccess)ra).getBooleanValue(), ctx -> ra.isMissing());
        }

        @Override
        public Function<ReadAccess, IntegerComputer> visit(final ByteDataSpec spec) {
            return ra -> IntegerComputer.of(ctx -> ((ByteReadAccess)ra).getByteValue(), ctx -> ra.isMissing());
        }

        @Override
        public Function<ReadAccess, FloatComputer> visit(final DoubleDataSpec spec) {
            return ra -> FloatComputer.of(ctx -> ((DoubleReadAccess)ra).getDoubleValue(), ctx -> ra.isMissing());
        }

        @Override
        public Function<ReadAccess, FloatComputer> visit(final FloatDataSpec spec) {
            return ra -> FloatComputer.of(ctx -> ((FloatReadAccess)ra).getFloatValue(), ctx -> ra.isMissing());
        }

        @Override
        public Function<ReadAccess, IntegerComputer> visit(final IntDataSpec spec) {
            return ra -> IntegerComputer.of(ctx -> ((IntReadAccess)ra).getIntValue(), ctx -> ra.isMissing());
        }

        @Override
        public Function<ReadAccess, IntegerComputer> visit(final LongDataSpec spec) {
            return ra -> IntegerComputer.of(ctx -> ((LongReadAccess)ra).getLongValue(), ctx -> ra.isMissing());
        }

        @Override
        public Function<ReadAccess, StringComputer> visit(final StringDataSpec spec) {
            return ra -> StringComputer.of(ctx -> ((StringReadAccess)ra).getStringValue(), ctx -> ra.isMissing());
        }

        @Override
        public Function<ReadAccess, ? extends Computer> visit(final VarBinaryDataSpec spec) {
            throw new IllegalArgumentException("Var binary data is not suppored by expressions");
        }

        @Override
        public Function<ReadAccess, ? extends Computer> visit(final VoidDataSpec spec) {
            throw new IllegalArgumentException("Void data is not suppored by expressions");
        }

        @Override
        public Function<ReadAccess, ? extends Computer> visit(final StructDataSpec spec) {
            throw new IllegalArgumentException("Struct data is not suppored by expressions");
        }

        @Override
        public Function<ReadAccess, ? extends Computer> visit(final ListDataSpec listDataSpec) {
            throw new IllegalArgumentException("List data is not suppored by expressions");
        }
    }

    private static class WriterFactoryMapper
        implements DataSpec.Mapper<TriFunction<WriteAccess, Computer, EvaluationContext, Runnable>> {

        interface ThrowingRunnable {
            void run() throws ExpressionEvaluationException;
        }

        private static Runnable setMissingOrSetValue(final Computer c, final WriteAccess a,
            final ThrowingRunnable setValue, final EvaluationContext ctx) {
            return () -> {
                try {
                    if (c.isMissing(ctx)) {
                        a.setMissing();
                    } else {
                        setValue.run();
                    }
                } catch (ExpressionEvaluationException e) {
                    // NB: We wrap the exception as a RuntimeException so we can throw it
                    throw new ExpressionEvaluationRuntimeException(e);
                }
            };
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final BooleanDataSpec spec) {
            return (access, computer, ctx) -> {
                var a = (BooleanAccess.BooleanWriteAccess)access;
                var c = (BooleanComputer)computer;
                return setMissingOrSetValue(c, a, () -> a.setBooleanValue(c.compute(ctx)), ctx);
            };
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final DoubleDataSpec spec) {
            return (access, computer, ctx) -> {
                var a = (DoubleAccess.DoubleWriteAccess)access;
                var c = (FloatComputer)computer;
                return setMissingOrSetValue(c, a, () -> a.setDoubleValue(c.compute(ctx)), ctx);
            };
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final LongDataSpec spec) {
            return (access, computer, ctx) -> {
                var a = (LongAccess.LongWriteAccess)access;
                var c = (IntegerComputer)computer;
                return setMissingOrSetValue(c, a, () -> a.setLongValue(c.compute(ctx)), ctx);
            };
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final StringDataSpec spec) {
            return (access, computer, ctx) -> {
                var a = (StringAccess.StringWriteAccess)access;
                var c = (StringComputer)computer;
                return setMissingOrSetValue(c, a, () -> a.setStringValue(c.compute(ctx)), ctx);
            };
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final IntDataSpec spec) {
            throw new IllegalArgumentException("Expressions cannot produce int data");
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final FloatDataSpec spec) {
            throw new IllegalArgumentException("Expressions cannot produce float data");
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final ByteDataSpec spec) {
            throw new IllegalArgumentException("Expressions cannot produce byte data");
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final VarBinaryDataSpec spec) {
            throw new IllegalArgumentException("Expressions cannot produce var binary data");
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final VoidDataSpec spec) {
            throw new IllegalArgumentException("Expressions cannot produce void data");
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final StructDataSpec spec) {
            throw new IllegalArgumentException("Expressions cannot produce struct data");
        }

        @Override
        public TriFunction<WriteAccess, Computer, EvaluationContext, Runnable> visit(final ListDataSpec listDataSpec) {
            throw new IllegalArgumentException("Expressions cannot produce list data");
        }
    }

    /**
     * Three-argument specialisation of a Function, like a BiFunction with one extra argument.
     *
     * @param <A> First argument type
     * @param <B> Second argument type
     * @param <C> Third argument type
     * @param <R> Return type
     */
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {

        /**
         * Run the function with the specified arguments.
         *
         * @param a
         * @param b
         * @param c
         * @return
         */
        R apply(A a, B b, C c);

    }
}
