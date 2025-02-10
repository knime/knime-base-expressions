package org.knime.core.expressions;

import static org.knime.core.expressions.Ast.BinaryOperator.DIVIDE;
import static org.knime.core.expressions.Ast.UnaryOperator.MINUS;
import static org.knime.core.expressions.AstTestUtils.COL;
import static org.knime.core.expressions.AstTestUtils.FLOAT;
import static org.knime.core.expressions.AstTestUtils.OP;
import static org.knime.core.expressions.AstTestUtils.ROW_INDEX;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExpressionsTest {

    @Test
    void testRequiresRowIndexColumn() {
        Assertions.assertTrue(Expressions.requiresRowIndexColumn(ROW_INDEX()),
            "The expression should use the row index column");

        Assertions.assertTrue(Expressions.requiresRowIndexColumn(OP(COL("c"), DIVIDE, OP(MINUS, ROW_INDEX()))),
            "The expression should use the row index column");

        Assertions.assertFalse(Expressions.requiresRowIndexColumn(COL("c")),
            "The expression should not use the row index column");

        Assertions.assertFalse(Expressions.requiresRowIndexColumn(OP(COL("cT"), DIVIDE, FLOAT(2.0))),
            "The expression should not use the row index column");
    }
}
