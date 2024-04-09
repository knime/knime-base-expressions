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
 *   Apr 8, 2024 (benjamin): created
 */
package org.knime.core.expressions.functions;

import static org.knime.core.expressions.ValueType.BOOLEAN;
import static org.knime.core.expressions.ValueType.FLOAT;
import static org.knime.core.expressions.ValueType.INTEGER;
import static org.knime.core.expressions.ValueType.MISSING;
import static org.knime.core.expressions.ValueType.OPT_FLOAT;
import static org.knime.core.expressions.ValueType.OPT_INTEGER;
import static org.knime.core.expressions.ValueType.STRING;
import static org.knime.core.expressions.functions.FunctionTestBuilder.arg;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misFloat;
import static org.knime.core.expressions.functions.FunctionTestBuilder.misInteger;

import java.util.List;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests for {@link MathFunctions}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("static-method")
final class MathFunctionTests {

    @TestFactory
    List<DynamicNode> sin() {
        return new FunctionTestBuilder(MathFunctions.SIN) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(1)), Math.sin(1.0)) //
            .impl("FLOAT", List.of(arg(1.2)), Math.sin(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }

    @TestFactory
    List<DynamicNode> cos() {
        return new FunctionTestBuilder(MathFunctions.COS) //
            .typing("INTEGER", List.of(INTEGER), FLOAT) //
            .typing("FLOAT", List.of(FLOAT), FLOAT) //
            .typing("INTEGER?", List.of(OPT_INTEGER), OPT_FLOAT) //
            .typing("FLOAT?", List.of(OPT_FLOAT), OPT_FLOAT) //
            .illegalArgs("INTEGER, INTEGER", List.of(INTEGER, INTEGER)) //
            .illegalArgs("STRING", List.of(STRING)) //
            .illegalArgs("BOOLEAN", List.of(BOOLEAN)) //
            .illegalArgs("MISSING", List.of(MISSING)) //
            .impl("INTEGER", List.of(arg(1)), Math.cos(1.0)) //
            .impl("FLOAT", List.of(arg(1.2)), Math.cos(1.2)) //
            .impl("missing INTEGER", List.of(misInteger())) //
            .impl("missing FLOAT", List.of(misFloat())) //
            .tests();
    }
}
