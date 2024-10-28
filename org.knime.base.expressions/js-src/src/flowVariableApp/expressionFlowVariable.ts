import "./__mocks__/browser-mock-flow-variable-services";
import { createApp } from "vue";

import { setupConsola } from "@/common/functions";
import ExpressionFlowVariableApp from "@/flowVariableApp/ExpressionFlowVariableApp.vue";

setupConsola();

createApp(ExpressionFlowVariableApp).mount("#app");
