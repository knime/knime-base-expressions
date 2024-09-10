<script setup lang="ts">
import {
  getScriptingService,
  ScriptingEditor,
  setActiveEditorStoreForAi,
  useReadonlyStore,
} from "@knime/scripting-editor";
import { getExpressionInitialDataService } from "@/expressionInitialDataService";
import type {
  ExpressionInitialData,
  ExpressionVersion,
  EditorErrorState,
} from "@/common/types";
import { Button, FunctionButton, LoadingIcon } from "@knime/components";
import PlusIcon from "@knime/styles/img/icons/circle-plus.svg";
import { onKeyStroke } from "@vueuse/core";
import {
  type ComponentPublicInstance,
  computed,
  nextTick,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import FunctionCatalog from "@/components/function-catalog/FunctionCatalog.vue";
import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";
import { MIN_WIDTH_FUNCTION_CATALOG } from "@/components/function-catalog/contraints";
import { v4 as uuidv4 } from "uuid";
import ExpressionEditorPane, {
  type ExpressionEditorPaneExposes,
} from "@/components/ExpressionEditorPane.vue";
import type { FunctionCatalogData } from "@/components/functionCatalogTypes";
import { LANGUAGE } from "@/common/constants";
import {
  calculateInitialPaneSizes,
  registerInsertionListener,
} from "@/common/functions";
import {
  type ExpressionFlowVariableNodeSettings,
  getFlowVariableSettingsService,
} from "@/expressionSettingsService";
import { runFlowVariableDiagnostics } from "@/flowVariableApp/expressionFlowVariableDiagnostics";
import OutputSelector, {
  type AllowedDropDownValue,
  type SelectorState,
} from "@/components/OutputSelector.vue";
import { runOutputDiagnostics } from "@/generalDiagnostics";
import PlayIcon from "@knime/styles/img/icons/play.svg";
import OutputPreviewFlowVariable from "@/flowVariableApp/OutputPreviewFlowVariable.vue";

const activeEditorFileName = ref<string | null>(null);

// Populated by the initial settings
const flowVariableSelectorStates = reactive<{
  [key: string]: SelectorState;
}>({});

const editorErrorStates = reactive<{ [key: string]: EditorErrorState }>({});
const flowVariableSelectorErrorStates = reactive<{
  [key: string]: EditorErrorState;
}>({});

/**
 * Generate a new key for a new editor. This is used to index the flowVariableSelectorStates
 * and the multiEditorComponentRefs, plus it's used to keep track of the order of the editors.
 */
const generateNewKey = () => {
  return uuidv4();
};

// Overwritten by the initial settings
const expressionVersion = ref<ExpressionVersion>({
  languageVersion: 0,
  builtinFunctionsVersion: 0,
  builtinAggregationsVersion: 0,
});

const multiEditorComponentRefs = reactive<{
  [key: string]: ExpressionEditorPaneExposes;
}>({});

const flowVariableStateWatchers = reactive<{ [key: string]: Function }>({});
const editorStateWatchers = reactive<{ [key: string]: Function }>({});

// The canonical source of ordering truth, used to index the flowVariableSelectorStates
// and the multiEditorComponentRefs. Things are run in the order defined here,
// saved in the order defined here, etc.
const orderedEditorKeys = reactive<string[]>([]);

const createEditorTitle = (index: number) => `Expression (${index + 1})`;

const numberOfEditors = computed(() => orderedEditorKeys.length);

/** Called by components when they're created, stores a ref to the component in our dict */
const createElementReference = (title: string) => {
  return (el: Element | ComponentPublicInstance | null) => {
    multiEditorComponentRefs[title] =
      el as unknown as ExpressionEditorPaneExposes;
  };
};

// Input flowVariables helpers
const inputFlowVariables = ref<AllowedDropDownValue[]>([]);

const getAvailableFlowVariableForReplacement = (
  key: string,
): AllowedDropDownValue[] => {
  const index = orderedEditorKeys.indexOf(key);

  const flowVariableFromPreviousEditors = orderedEditorKeys
    .slice(0, index)
    .filter((key) => flowVariableSelectorStates[key].outputMode === "APPEND")
    .map((key) => flowVariableSelectorStates[key].create)
    .map((flowVariable) => {
      return { id: flowVariable, text: flowVariable };
    });

  return [...inputFlowVariables.value, ...flowVariableFromPreviousEditors];
};

const getActiveEditor = (): ExpressionEditorPaneExposes | null => {
  if (activeEditorFileName.value === null) {
    return null;
  }

  return multiEditorComponentRefs[activeEditorFileName.value];
};

const onEditorFocused = (filename: string) => {
  activeEditorFileName.value = filename;
  setActiveEditorStoreForAi(
    multiEditorComponentRefs[filename]?.getEditorState(),
  );

  // Scroll to the editor. The focusing somehow interferes with scrolling,
  // so wait for a tick first.
  nextTick().then(() => {
    multiEditorComponentRefs[filename]
      ?.getEditorState()
      .editor.value?.getDomNode()
      ?.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
      });
  });
};

const functionCatalogData = ref<FunctionCatalogData>();

const getFirstEditor = (): ExpressionEditorPaneExposes => {
  return multiEditorComponentRefs[orderedEditorKeys[0]];
};

const runDiagnosticsFunction = async () => {
  const codeErrors = await runFlowVariableDiagnostics(
    orderedEditorKeys
      .map((key) => multiEditorComponentRefs[key])
      .map((editor) => editor.getEditorState()),
    orderedEditorKeys
      .map((key) => flowVariableSelectorStates[key])
      .map((state) => (state.outputMode === "APPEND" ? state.create : null)),
  );

  const flowVariableErrorMessages = runOutputDiagnostics(
    "flow variable",
    orderedEditorKeys.map((key) => flowVariableSelectorStates[key]),
    inputFlowVariables.value.map((flowVariable) => flowVariable.id),
  );

  orderedEditorKeys.forEach((key, index) => {
    if (flowVariableErrorMessages[index] === null) {
      flowVariableSelectorErrorStates[key] = {
        level: "OK",
      };
    } else {
      flowVariableSelectorErrorStates[key] = {
        level: "ERROR",
        message: flowVariableErrorMessages[index],
      };
    }

    editorErrorStates[key] = codeErrors[index];
  });
};

const initialData = ref<ExpressionInitialData | null>(null);

onMounted(async () => {
  const [initialDataLocal, settings] = await Promise.all([
    getExpressionInitialDataService().getInitialData(),
    getFlowVariableSettingsService().getSettings(),
  ]);
  initialData.value = initialDataLocal;

  const { functionCatalog, flowVariables } = initialData.value;

  expressionVersion.value = {
    languageVersion: settings.languageVersion,
    builtinFunctionsVersion: settings.builtinFunctionsVersion,
    builtinAggregationsVersion: settings.builtinAggregationsVersion,
  };

  functionCatalogData.value = functionCatalog;

  if (flowVariables && flowVariables.subItems) {
    inputFlowVariables.value = flowVariables.subItems.map(
      (c: { name: string }) => {
        return { id: c.name, text: c.name };
      },
    );
  }

  registerKnimeExpressionLanguage(initialDataLocal, {
    specialColumnAccess: false,
  });

  for (let i = 0; i < settings.scripts.length; ++i) {
    const key = generateNewKey();

    orderedEditorKeys.push(key);

    flowVariableSelectorStates[key] = {
      outputMode: settings.flowVariableOutputModes[i],
      create: settings.createdFlowVariables[i],
      replace: settings.replacedFlowVariables[i],
    };

    editorErrorStates[key] = { level: "OK" };
    flowVariableSelectorErrorStates[key] = { level: "OK" };
  }

  await nextTick(); // Wait for the editors to be rendered

  useReadonlyStore().value =
    settings.settingsAreOverriddenByFlowVariable || false;

  for (let i = 0; i < settings.scripts.length; i++) {
    const key = orderedEditorKeys[i];

    multiEditorComponentRefs[key]
      .getEditorState()
      .setInitialText(settings.scripts[i]);

    multiEditorComponentRefs[key].getEditorState().editor.value?.updateOptions({
      readOnly: useReadonlyStore().value,
      readOnlyMessage: {
        value: "Read-Only-Mode: configuration is set by flow variables.",
      },
      renderValidationDecorations: "on",
    });

    // Watch all editor text and when changes occur, rerun diagnostics
    editorStateWatchers[key] = watch(
      multiEditorComponentRefs[key].getEditorState().text,
      runDiagnosticsFunction,
    );
    flowVariableStateWatchers[key] = watch(
      () => flowVariableSelectorStates[key],
      runDiagnosticsFunction,
      {
        deep: true,
      },
    );
  }

  setActiveEditorStoreForAi(getFirstEditor()?.getEditorState());
  activeEditorFileName.value = orderedEditorKeys[0];

  // Run initial diagnostics now that we've set the initial text
  await runDiagnosticsFunction();
});

const anyEditorHasError = () =>
  Object.values(editorErrorStates).some(
    (errorState) => errorState.level === "ERROR",
  );

const runFlowVariableExpressions = () => {
  if (!anyEditorHasError()) {
    const expressions: string[] = orderedEditorKeys.map(
      (key) => multiEditorComponentRefs[key].getEditorState().text.value,
    );

    const names: string[] = orderedEditorKeys.map((key) => {
      const state = flowVariableSelectorStates[key];
      return state.outputMode === "APPEND" ? state.create : state.replace;
    });

    getScriptingService().sendToService("runFlowVariableExpression", [
      expressions,
      names,
    ]);
  }
};

getFlowVariableSettingsService().registerSettingsGetterForApply(
  (): ExpressionFlowVariableNodeSettings => ({
    ...expressionVersion.value,
    createdFlowVariables: orderedEditorKeys.map(
      (key) => flowVariableSelectorStates[key].create,
    ),
    replacedFlowVariables: orderedEditorKeys.map(
      (key) => flowVariableSelectorStates[key].replace,
    ),
    scripts: orderedEditorKeys.map(
      (key) => multiEditorComponentRefs[key].getEditorState().text.value ?? "",
    ),
    flowVariableOutputModes: orderedEditorKeys.map(
      (key) => flowVariableSelectorStates[key].outputMode,
    ),
  }),
);

const addNewEditorBelowExisting = async (fileNameAbove: string) => {
  const latestKey = generateNewKey();
  const desiredInsertionIndex = orderedEditorKeys.indexOf(fileNameAbove) + 1;

  flowVariableSelectorStates[latestKey] = {
    outputMode: "APPEND",
    create: "New flow variable",
    replace: initialData.value?.flowVariables.subItems?.[0].name ?? "",
  };

  editorErrorStates[latestKey] = { level: "OK" };
  flowVariableSelectorErrorStates[latestKey] = { level: "OK" };

  orderedEditorKeys.splice(desiredInsertionIndex, 0, latestKey);

  // Wait for the editor to render and populate the ref
  await nextTick();

  await runDiagnosticsFunction();

  editorStateWatchers[latestKey] = watch(
    multiEditorComponentRefs[latestKey].getEditorState().text,
    runDiagnosticsFunction,
  );
  flowVariableStateWatchers[latestKey] = watch(
    () => flowVariableSelectorStates[latestKey],
    runDiagnosticsFunction,
    {
      deep: true,
    },
  );

  multiEditorComponentRefs[latestKey].getEditorState().editor.value?.focus();

  return latestKey;
};

const addEditorAtBottom = () => {
  addNewEditorBelowExisting(orderedEditorKeys[orderedEditorKeys.length - 1]);
};

const onEditorRequestedDelete = (filename: string) => {
  if (numberOfEditors.value === 1) {
    return;
  }

  orderedEditorKeys.splice(orderedEditorKeys.indexOf(filename), 1);

  delete flowVariableSelectorStates[filename];
  delete multiEditorComponentRefs[filename];
  delete flowVariableSelectorErrorStates[filename];
  delete editorErrorStates[filename];

  // Clean up watchers
  editorStateWatchers[filename]();
  flowVariableStateWatchers[filename]();
  delete editorStateWatchers[filename];
  delete flowVariableStateWatchers[filename];

  runDiagnosticsFunction();
};

const onEditorRequestedMoveUp = (filename: string) => {
  const index = orderedEditorKeys.indexOf(filename);
  if (index <= 0) {
    return;
  }

  // Swap the entries at index and index - 1
  const temp = orderedEditorKeys[index];
  orderedEditorKeys[index] = orderedEditorKeys[index - 1];
  orderedEditorKeys[index - 1] = temp;

  runDiagnosticsFunction();

  // Focus the moved editor after rerendering
  nextTick().then(() => {
    multiEditorComponentRefs[filename].getEditorState().editor.value?.focus();
  });
};

const onEditorRequestedMoveDown = (filename: string) => {
  const index = orderedEditorKeys.indexOf(filename);
  if (index >= orderedEditorKeys.length - 1) {
    return;
  }

  // Swap the entries at index and index + 1
  const temp = orderedEditorKeys[index];
  orderedEditorKeys[index] = orderedEditorKeys[index + 1];
  orderedEditorKeys[index + 1] = temp;

  runDiagnosticsFunction();

  // Focus the moved editor after rerendering
  nextTick().then(() => {
    multiEditorComponentRefs[filename].getEditorState().editor.value?.focus();
  });
};

const onEditorRequestedCopyBelow = async (filename: string) => {
  const newKey = await addNewEditorBelowExisting(filename);

  // Copy state from the editor above to the new editor
  flowVariableSelectorStates[newKey] = {
    ...flowVariableSelectorStates[filename],
  };
  multiEditorComponentRefs[newKey]
    .getEditorState()
    .setInitialText(
      multiEditorComponentRefs[filename].getEditorState().text.value,
    );
};

registerInsertionListener(getActiveEditor);

// Shift+Enter while editor has focus runs expressions
onKeyStroke("Enter", (evt: KeyboardEvent) => {
  if (
    evt.shiftKey &&
    Object.values(multiEditorComponentRefs).some((editor) =>
      editor.getEditorState().editor.value?.hasTextFocus(),
    )
  ) {
    evt.preventDefault();
    runFlowVariableExpressions();
  }
});

const runButtonDisabledErrorReason = computed(() => {
  const haveSyntaxErrors = Object.values(editorErrorStates).some(
    (errorState) => errorState.level === "ERROR",
  );
  const haveColumnErrors = Object.values(flowVariableSelectorErrorStates).some(
    (e) => e.level === "ERROR",
  );

  if (haveSyntaxErrors) {
    return "To evaluate your expression, first resolve syntax errors.";
  } else if (haveColumnErrors) {
    return "To evaluate your expression, first resolve column output errors.";
  } else {
    return null;
  }
});

const getMostConcerningErrorStateForEditor = (
  key: string,
): EditorErrorState => {
  const severitiesInDescendingOrder = ["ERROR", "WARNING", "OK"];

  for (const severity of severitiesInDescendingOrder) {
    if (editorErrorStates[key].level === severity) {
      return editorErrorStates[key];
    } else if (flowVariableSelectorErrorStates[key].level === severity) {
      return flowVariableSelectorErrorStates[key];
    }
  }

  // This shouldn't happen
  return { level: "OK" };
};

const initialPaneSizes = calculateInitialPaneSizes();
</script>

<template>
  <main>
    <template v-if="numberOfEditors === 0">
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
          <ExpressionEditorPane
            v-for="(key, index) in orderedEditorKeys"
            :key="key"
            :ref="createElementReference(key)"
            :title="createEditorTitle(index)"
            :file-name="key"
            :language="LANGUAGE"
            :ordering-options="{
              isFirst: index === 0,
              isLast: index === numberOfEditors - 1,
              isOnly: numberOfEditors === 1,
              isActive: activeEditorFileName === key,
            }"
            :error-state="getMostConcerningErrorStateForEditor(key)"
            @focus="onEditorFocused(key)"
            @delete="onEditorRequestedDelete"
            @move-down="onEditorRequestedMoveDown"
            @move-up="onEditorRequestedMoveUp"
            @copy-below="onEditorRequestedCopyBelow"
          >
            <!-- Controls displayed once per editor -->
            <template #multi-editor-controls>
              <div class="editor-controls">
                <OutputSelector
                  v-model="flowVariableSelectorStates[key]"
                  entity-name="flow variable"
                  :is-valid="
                    flowVariableSelectorErrorStates[key].level === 'OK'
                  "
                  :allowed-replacement-entities="
                    getAvailableFlowVariableForReplacement(key)
                  "
                />
              </div>
            </template>
          </ExpressionEditorPane>

          <FunctionButton
            class="add-new-editor-button"
            :disabled="useReadonlyStore().value"
            @click="addEditorAtBottom"
          >
            <PlusIcon /><span>Add expression</span>
          </FunctionButton>
        </template>

        <template #right-pane>
          <template v-if="functionCatalogData">
            <FunctionCatalog
              :function-catalog-data="functionCatalogData"
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
            @click="runFlowVariableExpressions()"
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
.editor-controls {
  width: 100%;
  display: flex;
  justify-content: space-between;
  padding: var(--space-4) var(--space-8);
  height: fit-content;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.no-editors {
  position: absolute;
  inset: calc(50% - 25px);
  width: 50px;
  height: 50px;
}

.add-new-editor-button {
  width: fit-content;
  margin: var(--space-16) auto;
  outline: 1px solid var(--knime-silver-sand);
}

.hide-button-text {
  margin-right: calc(-1 * var(--space-16));
}

.run-button {
  display: inline;
}
</style>
