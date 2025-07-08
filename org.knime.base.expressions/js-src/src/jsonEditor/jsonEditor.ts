import { createApp } from "vue";

import { setupConsola } from "@/common/functions";
import JsonEditorApp from "@/jsonEditor/JsonEditor.vue";

setupConsola();

createApp(JsonEditorApp).mount("#app");
