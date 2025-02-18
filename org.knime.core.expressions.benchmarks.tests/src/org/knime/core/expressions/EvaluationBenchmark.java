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
 *   Feb 18, 2025 (benjamin): created
 */
package org.knime.core.expressions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.expressions.Computer.BooleanComputer;
import org.knime.core.expressions.Computer.FloatComputer;
import org.knime.core.expressions.Computer.IntegerComputer;
import org.knime.core.expressions.Computer.StringComputer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark {@link Expressions#evaluate}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@SuppressWarnings("javadoc")
public class EvaluationBenchmark {

    @Param
    BenchmarkExpression m_expression;

    private Computer m_resultComputer;

    private AtomicInteger m_rowIndex = new AtomicInteger(0);

    @Setup(Level.Iteration)
    public void setup() throws ExpressionCompileException {
        var ast = Expressions.parse(m_expression.getExpression());
        Expressions.inferTypes(ast, BenchmarkTable::columnToType, BenchmarkFlowVariables::flowVarToType);
        m_resultComputer = Expressions.evaluate(ast, //
            BenchmarkTable.columnToComputer(m_rowIndex::get), //
            BenchmarkFlowVariables::flowVarToComputer, //
            BenchmarkTable::aggregationToComputer //
        );
    }

    @Benchmark
    @OperationsPerInvocation(BenchmarkTable.NUM_ROWS)
    public void evaluate(final Blackhole bh) throws ExpressionEvaluationException {

        EvaluationContext ctx = warning -> {
        };

        if (m_resultComputer instanceof BooleanComputer c) {
            for (int i = 0; i < BenchmarkTable.NUM_ROWS; i++) {
                m_rowIndex.set(i);
                if (c.isMissing(ctx)) {
                    bh.consume(c.compute(ctx));
                }
            }
        } else if (m_resultComputer instanceof IntegerComputer c) {
            for (int i = 0; i < BenchmarkTable.NUM_ROWS; i++) {
                m_rowIndex.set(i);
                if (c.isMissing(ctx)) {
                    bh.consume(c.compute(ctx));
                }
            }
        } else if (m_resultComputer instanceof FloatComputer c) {
            for (int i = 0; i < BenchmarkTable.NUM_ROWS; i++) {
                m_rowIndex.set(i);
                if (c.isMissing(ctx)) {
                    bh.consume(c.compute(ctx));
                }
            }
        } else if (m_resultComputer instanceof StringComputer c) {
            for (int i = 0; i < BenchmarkTable.NUM_ROWS; i++) {
                m_rowIndex.set(i);
                if (c.isMissing(ctx)) {
                    bh.consume(c.compute(ctx));
                }
            }
        }
    }

    /** Evaluate the expression as a compiled Java function. */
    // @Benchmark // Just for comparison during development
    @OperationsPerInvocation(BenchmarkTable.NUM_ROWS)
    public void evaluateInJava(final Blackhole bh) {
        for (int i = 0; i < BenchmarkTable.NUM_ROWS; i++) {
            bh.consume(m_expression.runJavaImpl(i));
        }
    }
}
