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
 *   May 7, 2024 (benjamin): created
 */
package org.knime.base.expressions;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.columnar.table.virtual.ColumnarVirtualTable.ColumnarMapperFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.data.v2.schema.ValueSchemaUtils;
import org.knime.core.data.v2.time.LocalDateValueFactory;
import org.knime.core.data.v2.value.BooleanValueFactory;
import org.knime.core.data.v2.value.DoubleValueFactory;
import org.knime.core.data.v2.value.LongValueFactory;
import org.knime.core.data.v2.value.StringValueFactory;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.Expressions;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.schema.BooleanDataSpec;
import org.knime.core.table.schema.ColumnarSchema;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.DoubleDataSpec;
import org.knime.core.table.schema.LocalDateDataSpec;
import org.knime.core.table.schema.LongDataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.virtual.spec.MapTransformSpec.MapperFactory;

/**
 * Applies the given expression to each row of the given data.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // Expressions API is not yet public
public final class ExpressionMapperFactory implements ColumnarMapperFactory {

    /** Context for evaluating expressions. */
    public interface ExpressionMapperContext {

        /**
         * Returns a computer for the given flow variable access.
         *
         * @param flowVarAccess the flow variable access
         * @return a computer that returns the value of the flow variable
         */
        Optional<Computer> flowVariableToComputer(Ast.FlowVarAccess flowVarAccess);

        /**
         * Returns a computer for the given aggregation call.
         *
         * @param aggregationCall the aggregation call
         * @return a computer that returns the result of the aggregation
         */
        Optional<Computer> aggregationToComputer(Ast.AggregationCall aggregationCall);
    }

    private final MapperFactory m_mapperFactory;

    private final int[] m_columnIndices;

    private final String m_outputColumnName;

    /**
     * Creates a new instance.
     *
     * @param ast the expression. Must have {@link Expressions#inferTypes inferred types}.
     * @param inputTableSchema
     * @param outputColumnName
     * @param exprContext
     * @param ctx
     */
    public ExpressionMapperFactory(final Ast ast, final ColumnarSchema inputTableSchema, final String outputColumnName,
        final ExpressionMapperContext exprContext, final EvaluationContext ctx) {
        m_outputColumnName = outputColumnName;

        final var columns = Exec.RequiredColumns.of(ast);
        m_columnIndices = columns.columnIndices();

        final IntFunction<Function<ReadAccess[], ? extends Computer>> columnIndexToComputerFactory = columnIndex -> {
            int inputIndex = columns.getInputIndex(columnIndex);
            Function<ReadAccess, ? extends Computer> createComputer =
                inputTableSchema.getSpec(columnIndex).accept(Exec.DATA_SPEC_TO_READER_FACTORY);
            return readAccesses -> createComputer.apply(readAccesses[inputIndex]);
        };

        m_mapperFactory = Exec.createMapperFactory(ast, columnIndexToComputerFactory,
            exprContext::flowVariableToComputer, exprContext::aggregationToComputer, ctx);
    }

    @Override
    public Runnable createMapper(final ReadAccess[] inputs, final WriteAccess[] outputs) {
        return m_mapperFactory.createMapper(inputs, outputs);
    }

    @Override
    public ValueSchema getOutputSchema() {
        var schema = m_mapperFactory.getOutputSchema();
        CheckUtils.checkArgument(schema.numColumns() == 1,
            "An expression must create exactly one column, but got " + schema.numColumns());
        var valueFactories = new ValueFactory[]{primitiveDataSpecToValueFactory(schema.getSpec(0))};
        var dataColumnSpecs =
            new DataColumnSpec[]{primitiveDataSpecToDataColumnSpec(schema.getSpec(0), m_outputColumnName)};
        var dataTableSpec = new DataTableSpec(dataColumnSpecs);
        return ValueSchemaUtils.create(dataTableSpec, valueFactories);
    }

    int[] getInputColumnIndices() {
        return m_columnIndices;
    }

    private static ValueFactory<?, ?> primitiveDataSpecToValueFactory(final DataSpec spec) {
        // NB: These are all output types supported by expressions
        if (spec instanceof BooleanDataSpec) {
            return BooleanValueFactory.INSTANCE;
        } else if (spec instanceof LongDataSpec) {
            return LongValueFactory.INSTANCE;
        } else if (spec instanceof DoubleDataSpec) {
            return DoubleValueFactory.INSTANCE;
        } else if (spec instanceof StringDataSpec) {
            return StringValueFactory.INSTANCE;
        } else if (spec instanceof LocalDateDataSpec) {
            return LocalDateValueFactory.INSTANCE;
        }
        throw new IllegalArgumentException("Cannot convert " + spec + " to ValueFactory");
    }

    /**
     * Turn a DataSpec (which we got by type inference from the AST) into a full-fledged DataColumnSpec
     *
     * @param spec
     * @param newColumnName
     * @return The corresponding DataColumnSpec
     */
    public static DataColumnSpec primitiveDataSpecToDataColumnSpec(final DataSpec spec, final String newColumnName) {
        // NB: These are all output types supported by expressions
        final DataType type;
        if (spec instanceof BooleanDataSpec) {
            type = BooleanCell.TYPE;
        } else if (spec instanceof LongDataSpec) {
            type = LongCell.TYPE;
        } else if (spec instanceof DoubleDataSpec) {
            type = DoubleCell.TYPE;
        } else if (spec instanceof StringDataSpec) {
            type = StringCell.TYPE;
        } else if (spec instanceof LocalDateDataSpec) {
            type = LocalDateCellFactory.TYPE;
        } else {
            throw new IllegalArgumentException("Cannot convert " + spec + " to DataColumnSpec");
        }
        return new DataColumnSpecCreator(newColumnName, type).createSpec();
    }
}
