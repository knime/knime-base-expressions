<script setup lang="ts">
import {
  getScriptingService,
  type InputOutputModel,
  InputOutputPane,
  ScriptingEditor,
  type SubItem,
  useReadonlyStore,
} from "@knime/scripting-editor";
import MultiEditorContainer, {
  type EditorState,
  type EditorStates,
} from "@/components/MultiEditorContainer.vue";
import type { ExpressionInitialData, ExpressionVersion } from "@/common/types";
import { LoadingIcon } from "@knime/components";
import { onMounted, ref, shallowRef } from "vue";
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
import { runOutputDiagnostics } from "@/generalDiagnostics";
import OutputPreviewFlowVariable from "@/flowVariableApp/OutputPreviewFlowVariable.vue";
import SimpleRunButton from "@/components/SimpleRunButton.vue";
import { type TypeRendererProps } from "@/components/TypeRenderer.vue";
import {
  buildAppendedOutput,
  replaceSubItems,
} from "@/common/inputOutputUtils";
import type {
  AllowedDropDownValue,
  SelectorState,
} from "@/components/OutputSelector.vue";

// Input flowVariables helpers
const runButtonDisabledErrorReason = ref<string | null>(null);
const initialData = shallowRef<ExpressionInitialData | null>(null);
const initialSettings = ref<ExpressionFlowVariableNodeSettings | null>(null);
const multiEditorContainerRef =
  ref<InstanceType<typeof MultiEditorContainer>>();

const currentInputOutputItems = ref<InputOutputModel[]>();

const getInitialItems = (): InputOutputModel[] => {
  return initialData.value
    ? [structuredClone(initialData.value?.flowVariables)]
    : [];
};

const refreshInputOutputItems = (
  { states, activeEditorKey }: EditorStates,
  returnTypes: string[],
) => {
  if (!activeEditorKey) {
    return;
  }

  const lastIndexToConsider =
    states.findIndex((state) => state.key === activeEditorKey) + 1;
  const statesUntilActiveWithReturnTypes = states
    .slice(0, lastIndexToConsider)
    .map((state, index) => ({
      selectorState: state.selectorState,
      key: state.key,
      returnType: returnTypes[index],
    }));

  const focusEditorActionBuilder = (editorKey: string) => () => {
    multiEditorContainerRef.value?.setActiveEditor(editorKey);
  };

  currentInputOutputItems.value = getInitialItems();

  currentInputOutputItems.value[0].subItems =
    currentInputOutputItems.value[0].subItems?.map((subItem) =>
      replaceSubItems(
        subItem as SubItem<TypeRendererProps>,
        statesUntilActiveWithReturnTypes,
        focusEditorActionBuilder,
      ),
    );

  if (
    statesUntilActiveWithReturnTypes.some(
      (state) => state.selectorState.outputMode === "APPEND",
    )
  ) {
    currentInputOutputItems.value.push(
      buildAppendedOutput(
        statesUntilActiveWithReturnTypes,
        focusEditorActionBuilder,
        {
          name: "f(X) appended flow variables",
          portType: "flowVariable",
          subItemCodeAliasTemplate:
            currentInputOutputItems.value[0].subItemCodeAliasTemplate,
        },
      ),
    );
  }
};

const runDiagnosticsFunction = async ({
  states,
}: EditorStates): Promise<string[]> => {
  const diagnostics = await runFlowVariableDiagnostics(
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
      diagnostics[index].errorState,
    );
  }

  const haveFlowVariableErrors = flowVariableErrorMessages.some(
    (error) => error !== null,
  );
  const haveSyntaxErrors = diagnostics.some(
    ({ errorState }) => errorState.level === "ERROR",
  );
  if (haveSyntaxErrors) {
    runButtonDisabledErrorReason.value =
      "To evaluate your expression, first resolve syntax errors.";
  } else if (haveFlowVariableErrors) {
    runButtonDisabledErrorReason.value =
      "To evaluate your expression, first resolve flow variable output errors.";
  } else {
    runButtonDisabledErrorReason.value = null;
  }

  return diagnostics.map(({ returnType }) => returnType);
};

const onChange = async (editorStates: EditorStates) => {
  const returnTypes = await runDiagnosticsFunction(editorStates);
  refreshInputOutputItems(editorStates, returnTypes);
};

onMounted(async () => {
  [initialData.value, initialSettings.value] = await Promise.all([
    getExpressionInitialDataService().getInitialData(),
    getFlowVariableSettingsService().getSettings(),
  ]);

  registerKnimeExpressionLanguage(initialData.value, {
    specialColumnAccess: false,
  });

  useReadonlyStore().value =
    initialSettings.value.settingsAreOverriddenByFlowVariable ?? false;
});

const runFlowVariableExpressions = (states: EditorState[]) => {
  getScriptingService().sendToService("runFlowVariableExpression", [
    states.map((state) => state.monacoState.text.value),
    states.map((state) =>
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
            slotName: 'bottomPaneTabSlot:outputPreview',
          },
        ]"
      >
        <!-- Extra content in the bottom tab pane -->
        <template #bottomPaneTabSlot:outputPreview="{ grabFocus }">
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
            @on-change="onChange"
            @run-expressions="runFlowVariableExpressions"
          />
        </template>

        <template #left-pane>
          <InputOutputPane :input-output-items="currentInputOutputItems" />
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
          <SimpleRunButton
            :run-button-disabled-error-reason="runButtonDisabledErrorReason"
            :show-button-text="showButtonText"
            @run-expressions="
              () =>
                runFlowVariableExpressions(
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
</style>
