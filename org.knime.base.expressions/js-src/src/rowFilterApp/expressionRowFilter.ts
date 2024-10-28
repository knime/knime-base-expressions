import "./__mocks__/browser-mock-row-filter-services";
import { createApp } from "vue";

import { setupConsola } from "@/common/functions";
import ExpressionRowFilterApp from "@/rowFilterApp/ExpressionRowFilterApp.vue";

setupConsola();

createApp(ExpressionRowFilterApp).mount("#app");
