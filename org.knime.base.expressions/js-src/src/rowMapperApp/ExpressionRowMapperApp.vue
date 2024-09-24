<script setup lang="ts">
import {
  consoleHandler,
  getScriptingService,
  OutputTablePreview,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  useReadonlyStore,
} from "@knime/scripting-editor";
import { getExpressionInitialDataService } from "@/expressionInitialDataService";
import type { ExpressionInitialData, ExpressionVersion } from "@/common/types";
import { LoadingIcon } from "@knime/components";
import {
  type AllowedDropDownValue,
  type SelectorState,
} from "@/components/OutputSelector.vue";
import { onMounted, ref } from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import { MIN_WIDTH_FUNCTION_CATALOG } from "@/components/function-catalog/contraints";
import MultiEditorContainer, {
  type EditorState,
} from "@/components/MultiEditorContainer.vue";
import { runRowMapperDiagnostics } from "@/rowMapperApp/expressionRowMapperDiagnostics";
import { DEFAULT_NUMBER_OF_ROWS_TO_RUN, LANGUAGE } from "@/common/constants";
import {
  calculateInitialPaneSizes,
  mapConnectionInfoToErrorMessage,
} from "@/common/functions";
import RunButton from "@/components/RunButton.vue";
import {
  type ExpressionRowMapperNodeSettings,
  getRowMapperSettingsService,
} from "@/expressionSettingsService";
import { runOutputDiagnostics } from "@/generalDiagnostics";

const initialData = ref<ExpressionInitialData | null>(null);
const initialSettings = ref<ExpressionRowMapperNodeSettings | null>(null);
const runButtonDisabledErrorReason = ref<string | null>(null);
const multiEditorContainerRef =
  ref<InstanceType<typeof MultiEditorContainer>>();

const runDiagnosticsFunction = async (states: EditorState[]) => {
  const codeErrors = await runRowMapperDiagnostics(
    states.map((state) => state.monacoState),
    states.map((state) =>
      state.selectorState.outputMode === "APPEND"
        ? state.selectorState.create
        : state.selectorState.replace,
    ),
  );

  const columnErrorMessages = runOutputDiagnostics(
    "column",
    states.map((state) => state.selectorState),
    initialData.value?.inputObjects[0].subItems?.map((c) => c.name) ?? [],
  );

  for (const [index, state] of states.entries()) {
    if (columnErrorMessages[index] === null) {
      multiEditorContainerRef.value?.setSelectorErrorState(state.key, {
        level: "OK",
      });
    } else {
      multiEditorContainerRef.value?.setSelectorErrorState(state.key, {
        level: "ERROR",
        message: columnErrorMessages[index],
      });
    }

    multiEditorContainerRef.value?.setEditorErrorState(
      state.key,
      codeErrors[index],
    );
  }

  const connectionErrors = mapConnectionInfoToErrorMessage(
    initialData.value?.inputConnectionInfo,
  );
  const haveColumnErrors = columnErrorMessages.some((error) => error !== null);
  const haveSyntaxErrors = codeErrors.some((error) => error.level === "ERROR");

  if (connectionErrors) {
    runButtonDisabledErrorReason.value = connectionErrors;
  } else if (haveSyntaxErrors) {
    runButtonDisabledErrorReason.value =
      "To evaluate your expression, resolve existing errors first.";
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
    getRowMapperSettingsService().getSettings(),
  ]);

  if (initialData.value?.inputConnectionInfo[1].status !== "OK") {
    consoleHandler.writeln({
      warning: "No input available. Connect an executed node.",
    });
  }

  registerKnimeExpressionLanguage(initialData.value);

  useReadonlyStore().value =
    initialSettings.value.settingsAreOverriddenByFlowVariable ?? false;
});

const runRowMapperExpressions = (rows: number, editorStates: EditorState[]) => {
  getScriptingService().sendToService("runExpression", [
    editorStates.map((state) => state.monacoState.text.value),
    rows,
    editorStates.map((state) => state.selectorState.outputMode),
    editorStates.map((state) =>
      state.selectorState.outputMode === "APPEND"
        ? state.selectorState.create
        : state.selectorState.replace,
    ),
  ]);
};

getRowMapperSettingsService().registerSettingsGetterForApply(
  (): ExpressionRowMapperNodeSettings => {
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
      outputModes: orderedEditorStates.map(
        (state) => state.selectorState.outputMode,
      ),
      createdColumns: orderedEditorStates.map(
        (state) => state.selectorState.create,
      ),
      replacedColumns: orderedEditorStates.map(
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
    <template v-if="initialData === null || initialSettings === null">
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
            label: 'Output preview',
            value: 'outputPreview',
          },
        ]"
      >
        <!-- Extra content in the bottom tab pane -->
        <template #outputPreview="{ grabFocus }">
          <OutputTablePreview @output-table-updated="grabFocus()" />
        </template>

        <template #editor>
          <MultiEditorContainer
            ref="multiEditorContainerRef"
            item-type="column"
            :default-replacement-item="
              initialData?.inputObjects[0].subItems?.filter(
                (c) => c.supported,
              )[0].name ?? ''
            "
            :default-append-item="'New Column'"
            :settings="
              initialSettings.scripts.map((script, index) => ({
                initialScript: script,
                initialSelectorState: {
                  outputMode: initialSettings!.outputModes[index],
                  create: initialSettings!.createdColumns[index],
                  replace: initialSettings!.replacedColumns[index],
                } satisfies SelectorState,
              }))
            "
            :replaceable-items-in-input-table="
              initialData!.inputObjects[0].subItems
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
            @run-expressions="
              (states) =>
                runRowMapperExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN, states)
            "
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
          <RunButton
            :run-button-disabled-error-reason="runButtonDisabledErrorReason"
            :show-button-text="showButtonText"
            @run-expressions="
              (numRows) =>
                runRowMapperExpressions(
                  numRows,
                  multiEditorContainerRef!.getOrderedEditorStates(),
                )
            "
          />
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
