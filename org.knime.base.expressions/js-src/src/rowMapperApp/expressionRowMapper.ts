import "./__mocks__/browser-mock-row-mapper-services";
import { createApp } from "vue";
import ExpressionRowMapperApp from "@/rowMapperApp/ExpressionRowMapperApp.vue";
import { setupConsola } from "@/common/functions";

setupConsola();

createApp(ExpressionRowMapperApp).mount("#app");
