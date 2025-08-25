import { createApp } from "vue";

import { init, initMocked } from "@knime/scripting-editor";

import { setupConsola } from "@/common/functions";
import ExpressionRowFilterApp from "@/rowFilterApp/ExpressionRowFilterApp.vue";

setupConsola();

// Initialize application (e.g., load initial data, set up services)
if (import.meta.env.MODE === "development.browser") {
  // Mock the initial data and services
  initMocked(
    (await import("./__mocks__/browser-mock-row-filter-services")).default,
  );
} else {
  await init();
}

createApp(ExpressionRowFilterApp).mount("#app");
