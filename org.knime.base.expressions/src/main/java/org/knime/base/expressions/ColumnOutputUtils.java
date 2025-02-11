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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.BooleanValueFactory;
import org.knime.core.data.v2.value.DoubleValueFactory;
import org.knime.core.data.v2.value.LongValueFactory;
import org.knime.core.data.v2.value.StringValueFactory;
import org.knime.core.data.v2.value.ValueInterfaces.BooleanWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.DoubleWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.LongWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringWriteValue;
import org.knime.core.expressions.Computer;
import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.knime.core.expressions.EvaluationContext;
import org.knime.core.expressions.ExpressionEvaluationException;
import org.knime.core.expressions.ValueType;

/**
 * Utilities to write expression output results into columns.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
public final class ColumnOutputUtils {

    private ColumnOutputUtils() {
    }

    /**
     * Turn a ValueType (which we got by type inference from the AST) into a full-fledged DataColumnSpec
     *
     * @param valueType the output ValueType
     * @param newColumnName the name of the new column
     * @return The corresponding DataColumnSpec
     * @throws IllegalArgumentException if the ValueType is missing
     */
    public static DataColumnSpec valueTypeToDataColumnSpec(final ValueType valueType, final String newColumnName) {
        final DataType columnType;
        if (ValueType.BOOLEAN.equals(valueType.baseType())) {
            columnType = BooleanCell.TYPE;
        } else if (ValueType.INTEGER.equals(valueType.baseType())) {
            columnType = LongCell.TYPE;
        } else if (ValueType.FLOAT.equals(valueType.baseType())) {
            columnType = DoubleCell.TYPE;
        } else if (ValueType.STRING.equals(valueType.baseType())) {
            columnType = StringCell.TYPE;
        } else if (ValueType.MISSING.equals(valueType)) {
            throw new IllegalArgumentException("Cannot create DataColumnSpec for missing value type");
        } else {
            throw new IllegalArgumentException("Cannot convert " + valueType + " to DataColumnSpec");
        }
        return new DataColumnSpecCreator(newColumnName, columnType).createSpec();
    }

    /**
     * Turn a ValueType (which we got by type inference from the AST) into a ValueFactory.
     *
     * @param valueType the output ValueType
     * @return The corresponding ValueFactory
     * @throws IllegalArgumentException if the ValueType is missing
     */
    public static ValueFactory<?, ?> valueTypeToValueFactory(final ValueType valueType) {
        // NB: These are all output types supported by expressions
        if (ValueType.BOOLEAN.equals(valueType.baseType())) {
            return BooleanValueFactory.INSTANCE;
        } else if (ValueType.INTEGER.equals(valueType.baseType())) {
            return LongValueFactory.INSTANCE;
        } else if (ValueType.FLOAT.equals(valueType.baseType())) {
            return DoubleValueFactory.INSTANCE;
        } else if (ValueType.STRING.equals(valueType.baseType())) {
            return StringValueFactory.INSTANCE;
        } else if (ValueType.MISSING.equals(valueType)) {
            throw new IllegalArgumentException("Cannot create ValueFactory for missing value type");
        } else {
            throw new IllegalArgumentException("Cannot convert " + valueType + " to ValueFactory");
        }
    }

    /**
     * Create a {@link ComputerResultWriter} that writes the result of a computer to the given {@link WriteValue}.
     *
     * @param outputComputer the computer that computes the output value
     * @param writeValue the write value to set
     * @return a ValueFromComputerSetter that applies the result of the computer to the write value
     */
    public static ComputerResultWriter createComputerResultWriter(final Computer outputComputer,
        final WriteValue<?> writeValue) {
        if (outputComputer instanceof BooleanComputer booleanComputer) {
            return ctx -> ((BooleanWriteValue)writeValue).setBooleanValue(booleanComputer.compute(ctx));
        } else if (outputComputer instanceof IntegerComputer integerComputer) {
            return ctx -> ((LongWriteValue)writeValue).setLongValue(integerComputer.compute(ctx));
        } else if (outputComputer instanceof FloatComputer floatComputer) {
            return ctx -> ((DoubleWriteValue)writeValue).setDoubleValue(floatComputer.compute(ctx));
        } else if (outputComputer instanceof StringComputer stringComputer) {
            return ctx -> ((StringWriteValue)writeValue).setStringValue(stringComputer.compute(ctx));
        } else {
            throw new IllegalArgumentException("Unsupported Computer: " + outputComputer);
        }
    }

    /** {@link #write(EvaluationContext)} applies a result value from a computer to a {@link WriteValue} */
    @FunctionalInterface
    public interface ComputerResultWriter {

        /**
         * Applies the result value from a computer to a {@link WriteValue}.
         *
         * @param ctx the evaluation context
         * @throws ExpressionEvaluationException if the evaluation of the expression fails
         */
        void write(EvaluationContext ctx) throws ExpressionEvaluationException;
    }
}
