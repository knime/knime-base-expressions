<script setup lang="ts">
import {
  consoleHandler,
  getScriptingService,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  useReadonlyStore,
} from "@knime/scripting-editor";
import MultiEditorContainer, {
  type EditorState,
} from "@/components/MultiEditorContainer.vue";
import type { ExpressionInitialData, ExpressionVersion } from "@/common/types";
import { Button, LoadingIcon } from "@knime/components";
import { onMounted, ref } from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import { MIN_WIDTH_FUNCTION_CATALOG } from "@/components/function-catalog/contraints";
import { LANGUAGE } from "@/common/constants";
import { calculateInitialPaneSizes } from "@/common/functions";
import {
  type ExpressionFlowVariableNodeSettings,
  getFlowVariableSettingsService,
} from "@/expressionSettingsService";
import { getExpressionInitialDataService } from "@/expressionInitialDataService";
import { runFlowVariableDiagnostics } from "@/flowVariableApp/expressionFlowVariableDiagnostics";
import {
  type AllowedDropDownValue,
  type SelectorState,
} from "@/components/OutputSelector.vue";
import { runOutputDiagnostics } from "@/generalDiagnostics";
import PlayIcon from "@knime/styles/img/icons/play.svg";
import OutputPreviewFlowVariable from "@/flowVariableApp/OutputPreviewFlowVariable.vue";

// Input flowVariables helpers
const runButtonDisabledErrorReason = ref<string | null>(null);
const initialData = ref<ExpressionInitialData | null>(null);
const initialSettings = ref<ExpressionFlowVariableNodeSettings | null>(null);
const multiEditorContainerRef =
  ref<InstanceType<typeof MultiEditorContainer>>();

const runDiagnosticsFunction = async (states: EditorState[]) => {
  const codeErrors = await runFlowVariableDiagnostics(
    states.map((state) => state.monacoState),
    states.map((state) =>
      state.selectorState.outputMode === "APPEND"
        ? state.selectorState.create
        : state.selectorState.replace,
    ),
  );

  const flowVariableErrorMessages = runOutputDiagnostics(
    "flow variable",
    states.map((state) => state.selectorState),
    initialData.value?.flowVariables.subItems?.map((c) => c.name) ?? [],
  );

  for (const [index, state] of states.entries()) {
    if (flowVariableErrorMessages[index] === null) {
      multiEditorContainerRef.value?.setSelectorErrorState(state.key, {
        level: "OK",
      });
    } else {
      multiEditorContainerRef.value?.setSelectorErrorState(state.key, {
        level: "ERROR",
        message: flowVariableErrorMessages[index],
      });
    }

    multiEditorContainerRef.value?.setEditorErrorState(
      state.key,
      codeErrors[index],
    );
  }

  const haveColumnErrors = flowVariableErrorMessages.some(
    (error) => error !== null,
  );
  const haveSyntaxErrors = codeErrors.some((error) => error.level === "ERROR");

  if (!initialData.value?.inputsAvailable) {
    runButtonDisabledErrorReason.value =
      "To evaluate your expression, first connect an executed node.";
  } else if (haveSyntaxErrors) {
    runButtonDisabledErrorReason.value =
      "To evaluate your expression, first resolve syntax errors.";
  } else if (haveColumnErrors) {
    runButtonDisabledErrorReason.value =
      "To evaluate your expression, first resolve column output errors.";
  } else {
    runButtonDisabledErrorReason.value = null;
  }
};

onMounted(async () => {
  [initialData.value, initialSettings.value] = await Promise.all([
    getExpressionInitialDataService().getInitialData(),
    getFlowVariableSettingsService().getSettings(),
  ]);

  if (!initialData.value.inputsAvailable) {
    consoleHandler.writeln({
      warning: "No input available. Connect an executed node.",
    });
  }

  registerKnimeExpressionLanguage(initialData.value, {
    specialColumnAccess: false,
  });

  useReadonlyStore().value =
    initialSettings.value.settingsAreOverriddenByFlowVariable ?? false;
});

const runFlowVariableExpressions = (editorStates: EditorState[]) => {
  getScriptingService().sendToService("runFlowVariableExpression", [
    editorStates.map((state) => state.monacoState.text.value),
    editorStates.map((state) =>
      state.selectorState.outputMode === "APPEND"
        ? state.selectorState.create
        : state.selectorState.replace,
    ),
  ]);
};

getFlowVariableSettingsService().registerSettingsGetterForApply(
  (): ExpressionFlowVariableNodeSettings => {
    const orderedEditorStates =
      multiEditorContainerRef.value!.getOrderedEditorStates();

    const expressionVersion: ExpressionVersion = {
      languageVersion: initialSettings.value!.languageVersion,
      builtinFunctionsVersion: initialSettings.value!.builtinFunctionsVersion,
      builtinAggregationsVersion:
        initialSettings.value!.builtinAggregationsVersion,
    };

    return {
      ...expressionVersion,
      flowVariableOutputModes: orderedEditorStates.map(
        (state) => state.selectorState.outputMode,
      ),
      createdFlowVariables: orderedEditorStates.map(
        (state) => state.selectorState.create,
      ),
      replacedFlowVariables: orderedEditorStates.map(
        (state) => state.selectorState.replace,
      ),
      scripts: orderedEditorStates.map((state) => state.monacoState.text.value),
    };
  },
);

const initialPaneSizes = calculateInitialPaneSizes();
</script>

<template>
  <main>
    <template v-if="initialSettings === null || initialData === null">
      <div class="no-editors">
        <LoadingIcon />
      </div>
    </template>
    <template v-else>
      <ScriptingEditor
        :right-pane-minimum-width-in-pixel="MIN_WIDTH_FUNCTION_CATALOG"
        :show-control-bar="true"
        :language="LANGUAGE"
        :initial-pane-sizes="{
          right: initialPaneSizes.right,
          left: initialPaneSizes.left,
          bottom: 30,
        }"
        :additional-bottom-pane-tab-content="[
          {
            label: 'Preview',
            value: 'outputPreview',
          },
        ]"
      >
        <!-- Extra content in the bottom tab pane -->
        <template #outputPreview="{ grabFocus }">
          <OutputPreviewFlowVariable @output-preview-updated="grabFocus()" />
        </template>

        <template #editor>
          <MultiEditorContainer
            ref="multiEditorContainerRef"
            item-type="flow variable"
            :default-replacement-item="
              initialData?.flowVariables.subItems?.filter((c) => c.supported)[0]
                .name ?? ''
            "
            :default-append-item="'New Flow Variable'"
            :settings="
              initialSettings.scripts.map((script, index) => ({
                initialScript: script,
                initialSelectorState: {
                  outputMode: initialSettings!.flowVariableOutputModes[index],
                  create: initialSettings!.createdFlowVariables[index],
                  replace: initialSettings!.replacedFlowVariables[index],
                } satisfies SelectorState,
              }))
            "
            :replaceable-items-in-input-table="
              initialData!.flowVariables.subItems
                ?.filter((c) => c.supported)
                .map(
                  (c): AllowedDropDownValue => ({
                    id: c.name,
                    text: c.name,
                  }),
                ) ?? []
            "
            @active-editor-changed="
              (state) => setActiveEditorStoreForAi(state.monacoState)
            "
            @editor-states-changed="runDiagnosticsFunction"
            @run-expressions="(states) => runFlowVariableExpressions(states)"
          />
        </template>

        <template #right-pane>
          <template v-if="initialData?.functionCatalog">
            <FunctionCatalog
              :function-catalog-data="initialData.functionCatalog"
              :initially-expanded="false"
            />
          </template>
        </template>

        <!-- Controls displayed once only -->
        <template #code-editor-controls="{ showButtonText }">
          <Button
            primary
            compact
            :disabled="runButtonDisabledErrorReason !== null"
            @click="
              () =>
                runFlowVariableExpressions(
                  multiEditorContainerRef!.getOrderedEditorStates(),
                )
            "
          >
            <div
              class="run-button"
              :class="{
                'hide-button-text': !showButtonText,
              }"
            >
              <PlayIcon />
            </div>
            {{ showButtonText ? "Evaluate" : "" }}
          </Button>
        </template>
      </ScriptingEditor>
    </template>
  </main>
</template>

<style lang="postcss">
@import url("@knime/styles/css");
</style>

<style lang="postcss" scoped>
.no-editors {
  position: absolute;
  inset: calc(50% - 25px);
  width: 50px;
  height: 50px;
}

.run-button {
  display: inline;
}
</style>
