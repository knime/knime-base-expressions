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
 *   Feb 11, 2025 (benjamin): created
 */
package org.knime.base.expressions;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.schema.ValueSchema;
import org.knime.core.expressions.Ast;
import org.knime.core.expressions.Ast.ColumnAccess;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.BooleanComputerResultSupplier;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.Expressions;
import org.knime.core.expressions.ReturnResult;
import org.knime.core.expressions.ValueType;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.virtual.spec.MapTransformSpec;

/**
 * Utilites to manage input columns of Expressions.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class ColumnInputUtils {

    private ColumnInputUtils() {
    }

    /**
     * Utility function to get a mapper from column names to the value type
     *
     * @param spec the {@link DataTableSpec} to get the column types from
     * @return a function that maps column names to the value type
     */
    public static Function<String, ReturnResult<ValueType>> columnToTypesForTypeInference(final DataTableSpec spec) {
        return name -> ReturnResult.fromNullable(spec, "No input data is available.") //
            .flatMap(s -> ReturnResult.fromNullable(s.getColumnSpec(name),
                "No column with the name '" + name + "' is available.")) //
            .map(DataColumnSpec::getType) //
            .flatMap(type -> ReturnResult.fromNullable(mapDataTypeToValueType(type),
                "Columns of the type '" + type + "' are not supported in expressions."));
    }

    /**
     * Map a {@link DataType} to the {@link ValueType} which is used for it in expressions.
     *
     * @param type the {@link DataType} to map
     * @return the {@link ValueType} or {@code null} if the type is not supported
     */
    public static ValueType mapDataTypeToValueType(final DataType type) {
        if (type.isCompatible(BooleanValue.class)) {
            return ValueType.OPT_BOOLEAN;
        } else if (type.isCompatible(LongValue.class)) {
            // Note that IntCell is compatible with LongValue
            return ValueType.OPT_INTEGER;
        } else if (type.isCompatible(DoubleValue.class)) {
            return ValueType.OPT_FLOAT;
        } else if (type.getPreferredValueClass().equals(StringValue.class)) {
            // Note that we do not use isCompatible because many types are compatible with StringValue
            // but we do not want to represent them as Strings (e.g. JSON, XML, Date and Time)
            return ValueType.OPT_STRING;
        } else {
            return null;
        }
    }

    /**
     * Create a function that maps a {@link ColumnAccess} to a {@link Computer} that reads the value from the
     * appropriate input read access.
     *
     * @param inputTableSchema the schema of the input table (needed to create a correct {@link ReadValue} from a
     *            primitive {@link ReadAccess}.
     * @param requiredColumns the columns that are used in the expression
     * @param inputs the read accesses of the input table
     * @return a function that maps a {@link ColumnAccess} to a {@link Computer} and can be used as an argument to
     *         {@link Expressions#evaluate}
     */
    public static Function<ColumnAccess, Optional<Computer>> createColumnToComputerFn(
        final ValueSchema inputTableSchema, final RequiredColumns requiredColumns, final ReadAccess[] inputs) {
        return columnAccess -> {
            var resolvedColumIdx = Expressions.getResolvedColumnIdx(columnAccess);
            var inputAccessIndex = requiredColumns.getInputIndex(resolvedColumIdx);
            var inputAccess = inputs[inputAccessIndex];
            var valueFactory = inputTableSchema.getValueFactory(resolvedColumIdx);
            return Optional.of(readAccessToComputer(valueFactory, inputAccess));
        };
    }

    /** Create a computer that reads the value from the given read access */
    private static Computer readAccessToComputer(final ValueFactory<ReadAccess, WriteAccess> valueFactory,
        final ReadAccess readAccess) {
        BooleanComputerResultSupplier isMissing = ctx -> readAccess.isMissing();

        var readValue = valueFactory.createReadValue(readAccess);
        if (readValue instanceof BooleanValue booleanValue) {
            return BooleanComputer.of(ctx -> booleanValue.getBooleanValue(), isMissing);
        } else if (readValue instanceof LongValue longValue) {
            return IntegerComputer.of(ctx -> longValue.getLongValue(), isMissing);
        } else if (readValue instanceof DoubleValue doubleValue) {
            return FloatComputer.of(ctx -> doubleValue.getDoubleValue(), isMissing);
        } else if (readValue instanceof StringValue stringValue) {
            return StringComputer.of(ctx -> stringValue.getStringValue(), isMissing);
        } else {
            throw new IllegalArgumentException("Unsupported ValueFactory: " + valueFactory);
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
         * @return the {@link RequiredColumns} of all {@link org.knime.core.expressions.Ast.ColumnAccess} nodes
         */
        public static RequiredColumns of(final Ast expression) {
            var nodes = Ast.postorder(expression);
            int[] columnIndices = nodes.stream().mapToInt(node -> {
                if (node instanceof Ast.ColumnAccess n) {
                    return Expressions.getResolvedColumnIdx(n);
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
}
