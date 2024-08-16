import "./__mocks__/browser-mock-row-filter-services";
import { createApp } from "vue";
import ExpressionRowFilterApp from "@/rowFilterApp/ExpressionRowFilterApp.vue";
import { setupConsola } from "@/common/functions";

setupConsola();

createApp(ExpressionRowFilterApp).mount("#app");
