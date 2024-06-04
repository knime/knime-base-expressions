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
package org.knime.core.expressions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

/**
 * TODO (TP) Revise Ast.accept(AstVisitor) to recursively descent into children, collecting results etc. Probably the
 * AstVisitor interface has to be revised to allow for flexible accumulation of results or similar. Learn about how to
 * do this right. For example: https://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html
 * https://www.baeldung.com/java-asm#working-with-the-event-based-asm-api
 * https://asm.ow2.io/javadoc/org/objectweb/asm/ClassVisitor.html
 */
class AstVisitors {

    /**
     * TODO (TP) Ast.accept should do the recursion!
     */
    private interface RecursiveAstVisitor<O, E extends Exception> extends Ast.AstVisitor<O, E> {

        O applyLeaf(Ast node) throws E;

        O applyBinaryOp(Ast.BinaryOp node, O arg1Result, O arg2Result) throws E;

        O applyUnaryOp(Ast.UnaryOp node, O argResult) throws E;

        O applyFunctionCall(Ast.FunctionCall node, List<O> argResults) throws E;

        O applyAggregationCall(Ast.AggregationCall node, List<O> positionalArgResults, Map<String, O> namedArgResults)
            throws E;

        @Override
        default O visit(final Ast.MissingConstant node) throws E {
            return applyLeaf(node);
        }

        @Override
        default O visit(final Ast.BooleanConstant node) throws E {
            return applyLeaf(node);
        }

        @Override
        default O visit(final Ast.IntegerConstant node) throws E {
            return applyLeaf(node);
        }

        @Override
        default O visit(final Ast.FloatConstant node) throws E {
            return applyLeaf(node);
        }

        @Override
        default O visit(final Ast.StringConstant node) throws E {
            return applyLeaf(node);
        }

        @Override
        default O visit(final Ast.ColumnAccess node) throws E {
            return applyLeaf(node);
        }

        @Override
        default O visit(final Ast.FlowVarAccess node) throws E {
            return applyLeaf(node);
        }

        @Override
        default O visit(final Ast.BinaryOp node) throws E {
            final O arg1Result = node.arg1().accept(this);
            final O arg2Result = node.arg2().accept(this);
            return applyBinaryOp(node, arg1Result, arg2Result);
        }

        @Override
        default O visit(final Ast.UnaryOp node) throws E {
            final O argResult = node.arg().accept(this);
            return applyUnaryOp(node, argResult);
        }

        @Override
        default O visit(final Ast.FunctionCall node) throws E {
            final List<O> argResults = new ArrayList<>();
            for (var child : node.children()) {
                argResults.add(child.accept(this));
            }
            return applyFunctionCall(node, argResults);
        }

        @Override
        default O visit(final Ast.AggregationCall node) throws E {
            final List<O> positionalArgResults = new ArrayList<>();
            for (Ast arg : node.args().positionalArguments()) {
                positionalArgResults.add(arg.accept(this));
            }
            final Map<String, O> namedArgResults = new LinkedHashMap<>();
            for (var entry : node.args().namedArguments().entrySet()) {
                namedArgResults.put(entry.getKey(), entry.getValue().accept(this));
            }
            return applyAggregationCall(node, positionalArgResults, namedArgResults);
        }
    }

    /**
     * Recursively visits Ast nodes and combines results using a binary operator.
     */
    static class ReducingAstVisitor<O, E extends Exception> implements RecursiveAstVisitor<O, E> {

        private final O m_identity;

        private final BinaryOperator<O> m_accumulator;

        public ReducingAstVisitor(final O identity, final BinaryOperator<O> accumulator) {
            this.m_identity = identity;
            this.m_accumulator = accumulator;
        }

        @Override
        public O applyLeaf(final Ast node) {
            return m_identity;
        }

        @Override
        public O applyBinaryOp(final Ast.BinaryOp node, final O arg1Result, final O arg2Result) {
            return m_accumulator.apply(arg1Result, arg2Result);
        }

        @Override
        public O applyUnaryOp(final Ast.UnaryOp node, final O argResult) {
            return argResult;
        }

        @Override
        public O applyFunctionCall(final Ast.FunctionCall node, final List<O> argResults) {
            return reduce(argResults);
        }

        @Override
        public O applyAggregationCall(final Ast.AggregationCall node, final List<O> positionalArgResults,
            final Map<String, O> namedArgResults) {

            List<O> argResults = new ArrayList<>(positionalArgResults);
            argResults.addAll(namedArgResults.values());
            return reduce(argResults);
        }

        private O reduce(final List<O> argResults) {
            O result = m_identity;
            for (O r : argResults) {
                result = m_accumulator.apply(result, r);
            }
            return result;
        }
    }
}
