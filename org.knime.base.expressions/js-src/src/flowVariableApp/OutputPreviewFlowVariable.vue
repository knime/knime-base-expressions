<script setup lang="ts">
import { onMounted, ref } from "vue";

import { getScriptingService } from "@knime/scripting-editor";
import {
  type ExtensionConfig,
  UIExtension,
  type UIExtensionAPILayer,
} from "@knime/ui-extension-renderer/vue";
import { AlertingService, JsonDataService } from "@knime/ui-extension-service";

const extensionConfig = ref<ExtensionConfig | null>(null);
const resourceLocation = ref<string>("");
const dataAvailable = ref<boolean>(false);
const noDataMessage = ref<string | null>(null);

const emit = defineEmits(["output-preview-updated"]);

const makeExtensionConfig = async (
  nodeId: string,
  projectId: string,
  workflowId: string,
  baseUrl: string,
): Promise<ExtensionConfig> => {
  const initialData = JSON.parse(
    await (
      await JsonDataService.getInstance()
    ).data({
      method: "FlowVariablePreviewInitialDataSupplier.getInitialData",
    }),
  );

  return {
    nodeId,
    extensionType: "dialog",
    projectId,
    workflowId,
    hasNodeView: false,
    resourceInfo: {
      id: "someId",
      type: "SHADOW_APP",
      path: `${baseUrl}uiext/tableview/TableView.js`,
    },
    initialData,
  };
};

const noop = () => {
  /* mock unused api fields */
};

const apiLayer: UIExtensionAPILayer = {
  registerPushEventService: () => noop,
  callNodeDataService: async () => {},
  updateDataPointSelection: () => Promise.resolve({ result: null }),
  getResourceLocation: () => Promise.resolve(resourceLocation.value),
  imageGenerated: noop,
  onApplied: noop,
  onDirtyStateChange: noop,
  publishData: noop,
  sendAlert: (alert) => {
    AlertingService.getInstance().then((service) =>
      // @ts-expect-error baseService is not API but it exists
      service.baseService.sendAlert(alert),
    );
  },
  setControlsVisibility: noop,
  setReportingContent: noop,
  showDataValueView: noop,
  closeDataValueView: noop,
};

const updateExtensionConfig = async (config: ExtensionConfig) => {
  // @ts-expect-error baseUrl is not part of the type but it exists
  resourceLocation.value = `${config.resourceInfo.baseUrl}${config.resourceInfo.path.split("/").slice(0, -1).join("/")}/core-ui/TableView.js`;

  extensionConfig.value = await makeExtensionConfig(
    config.nodeId,
    config.projectId,
    config.workflowId,
    // @ts-expect-error baseUrl is not part of the type but it exists
    config.resourceInfo.baseUrl,
  );
};

onMounted(async () => {
  const jsonService = await JsonDataService.getInstance();
  const baseService = (jsonService as any).baseService;
  const extensionConfigLoaded = await baseService.getConfig();

  await updateExtensionConfig(extensionConfigLoaded);

  getScriptingService().registerEventHandler(
    "updatePreview",
    async (errorMessage: null | string) => {
      if (errorMessage) {
        dataAvailable.value = false;
        noDataMessage.value = errorMessage;
        emit("output-preview-updated");
        return;
      }

      if (!extensionConfig.value) {
        return;
      }

      dataAvailable.value = true;
      noDataMessage.value = null;
      emit("output-preview-updated");
      await updateExtensionConfig(extensionConfigLoaded);
    },
  );
});
</script>

<template>
  <div
    v-if="dataAvailable === true && extensionConfig !== null"
    class="output-table"
  >
    <div class="output-table-preview">
      <div class="preview-background">
        <div class="preview-warning-text">Preview</div>
      </div>
      <UIExtension
        :api-layer="apiLayer"
        :extension-config="extensionConfig"
        :resource-location="resourceLocation"
        :shadow-app-style="{ height: '100%', width: '100%' }"
      />
    </div>
  </div>
  <div
    v-else-if="noDataMessage !== null"
    class="output-table-preview"
    data-testid="no-data-placeholder"
  >
    {{ noDataMessage }}
  </div>
  <div v-else class="output-table-preview pre-evaluation-sign">
    To see the preview, evaluate the expression using the button above.
  </div>
</template>

<style lang="postcss" scoped>
.output-table-preview {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.pre-evaluation-sign {
  display: flex;
  height: 35px;
  justify-content: center;
  border-radius: 10px;
}

.output-table {
  height: 100%;
  width: 100%;
}

.preview-background {
  width: 100%;
  background-color: var(--knime-cornflower-semi);
  padding: var(--space-4) 0;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}

.preview-warning-text {
  background-color: white;
  color: black;
  padding: var(--space-4) var(--space-8);
  border-radius: 999vw;
  box-shadow: 0 4px 8px rgb(0 0 0 / 10%);
  text-align: center;
  font-size: small;
  vertical-align: middle;
}
</style>
