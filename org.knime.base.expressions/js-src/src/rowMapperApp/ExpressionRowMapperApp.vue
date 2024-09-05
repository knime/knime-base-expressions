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
import { FunctionButton, LoadingIcon } from "@knime/components";

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
  type EditorErrorState,
} from "@/components/ExpressionEditorPane.vue";
import type { FunctionCatalogData } from "@/components/functionCatalogTypes";
import { runRowMapperDiagnostics } from "@/rowMapperApp/expressionRowMapperDiagnostics";
import { DEFAULT_NUMBER_OF_ROWS_TO_RUN, LANGUAGE } from "@/common/constants";
import {
  calculateInitialPaneSizes,
  registerInsertionListener,
} from "@/common/functions";
import RunButton from "@/components/RunButton.vue";
import {
  type ExpressionRowMapperNodeSettings,
  getRowMapperSettingsService,
} from "@/expressionSettingsService";
import OutputSelector, {
  type AllowedDropDownValue,
  type SelectorState,
} from "@/components/OutputSelector.vue";
import { runOutputDiagnostics } from "@/generalDiagnostics";

const activeEditorFileName = ref<string | null>(null);

// Populated by the initial settings
const columnSelectorStates = reactive<{ [key: string]: SelectorState }>({});

const columnSelectorStateErrorMessages = reactive<{
  [key: string]: string | null;
}>({});

/**
 * Generate a new key for a new editor. This is used to index the columnSelectorStates
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

const columnStateWatchers = reactive<{ [key: string]: Function }>({});
const editorStateWatchers = reactive<{ [key: string]: Function }>({});

// The canonical source of ordering truth, used to index the columnSelectorStates
// and the multiEditorComponentRefs. Things are run in the order defined here,
// saved in the order defined here, etc.
const orderedEditorKeys = reactive<string[]>([]);

const createEditorTitle = (index: number) => `Expression (${index + 1})`;
const getEditorTitleFromKey = (key: string) =>
  createEditorTitle(orderedEditorKeys.indexOf(key));

const numberOfEditors = computed(() => orderedEditorKeys.length);

/** Called by components when they're created, stores a ref to the component in our dict */
const createElementReference = (title: string) => {
  return (el: Element | ComponentPublicInstance | null) => {
    multiEditorComponentRefs[title] =
      el as unknown as ExpressionEditorPaneExposes;
  };
};

// Input columns helpers
const columnsInInputTable = ref<AllowedDropDownValue[]>([]);

const getAvailableColumnsForReplacement = (
  key: string,
): AllowedDropDownValue[] => {
  const index = orderedEditorKeys.indexOf(key);

  const columnsFromPreviousEditors = orderedEditorKeys
    .slice(0, index)
    .filter((key) => columnSelectorStates[key].outputMode === "APPEND")
    .map((key) => columnSelectorStates[key].create)
    .map((column) => {
      return { id: column, text: column };
    });

  return [...columnsInInputTable.value, ...columnsFromPreviousEditors];
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
const inputsAvailable = ref(false);
const editorErrorStates = reactive<{ [key: string]: EditorErrorState }>({});

const getFirstEditor = (): ExpressionEditorPaneExposes => {
  return multiEditorComponentRefs[orderedEditorKeys[0]];
};

const runDiagnosticsFunction = async () => {
  const codeErrors = await runRowMapperDiagnostics(
    orderedEditorKeys
      .map((key) => multiEditorComponentRefs[key])
      .map((editor) => editor.getEditorState()),
    orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) => (state.outputMode === "APPEND" ? state.create : null)),
  );

  const columnValidities = runOutputDiagnostics(
    "Column",
    orderedEditorKeys.map((key) => columnSelectorStates[key]),
    columnsInInputTable.value.map((column) => column.id),
  );

  orderedEditorKeys.forEach((key, index) => {
    columnSelectorStateErrorMessages[key] = columnValidities[index];

    if (codeErrors[index] === "ERROR" || columnValidities[index] !== null) {
      editorErrorStates[key] = {
        level: "ERROR",
        message: "An error occurred.",
      };
    } else {
      editorErrorStates[key] = { level: "OK" };
    }
  });
};

const initialData = ref<ExpressionInitialData | null>(null);

onMounted(async () => {
  const [initialDataLocal, settings] = await Promise.all([
    getExpressionInitialDataService().getInitialData(),
    getRowMapperSettingsService().getSettings(),
  ]);
  initialData.value = initialDataLocal;

  const {
    inputsAvailable: inputsAvailableLocal,
    functionCatalog,
    inputObjects,
  } = initialData.value;

  expressionVersion.value = {
    languageVersion: settings.languageVersion,
    builtinFunctionsVersion: settings.builtinFunctionsVersion,
    builtinAggregationsVersion: settings.builtinAggregationsVersion,
  };

  inputsAvailable.value = inputsAvailableLocal;
  if (!inputsAvailable.value) {
    consoleHandler.writeln({
      warning: "No input available. Connect an executed node.",
    });
  }

  functionCatalogData.value = functionCatalog;

  if (inputObjects && inputObjects.length > 0 && inputObjects[0].subItems) {
    columnsInInputTable.value = inputObjects[0]?.subItems?.map(
      (c: { name: string }) => {
        return { id: c.name, text: c.name };
      },
    );
  }

  registerKnimeExpressionLanguage(initialDataLocal);

  for (let i = 0; i < settings.scripts.length; ++i) {
    const key = generateNewKey();

    orderedEditorKeys.push(key);

    columnSelectorStates[key] = {
      outputMode: settings.outputModes[i],
      create: settings.createdColumns[i],
      replace: settings.replacedColumns[i],
    };
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
    columnStateWatchers[key] = watch(
      () => columnSelectorStates[key],
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

const runRowMapperExpressions = (rows: number) => {
  if (!anyEditorHasError()) {
    getScriptingService().sendToService("runExpression", [
      orderedEditorKeys
        .map((key) => multiEditorComponentRefs[key])
        .map((ref) => ref.getEditorState().text.value),
      rows,
      orderedEditorKeys
        .map((key) => columnSelectorStates[key])
        .map((state) => state.outputMode),
      orderedEditorKeys
        .map((key) => columnSelectorStates[key])
        .map((state) =>
          state.outputMode === "APPEND" ? state.create : state.replace,
        ),
    ]);
  }
};

getRowMapperSettingsService().registerSettingsGetterForApply(
  (): ExpressionRowMapperNodeSettings => ({
    ...expressionVersion.value,
    createdColumns: orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) => state.create),
    replacedColumns: orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) => state.replace),
    scripts: orderedEditorKeys
      .map((key) => multiEditorComponentRefs[key])
      .map((editor) => editor.getEditorState().text.value ?? ""),
    outputModes: orderedEditorKeys
      .map((key) => columnSelectorStates[key])
      .map((state) => state.outputMode),
  }),
);

const addNewEditorBelowExisting = async (fileNameAbove: string) => {
  const latestKey = generateNewKey();
  const desiredInsertionIndex = orderedEditorKeys.indexOf(fileNameAbove) + 1;

  columnSelectorStates[latestKey] = {
    outputMode: "APPEND",
    create: "New Column",
    replace: initialData.value?.inputObjects[0].subItems?.[0].name ?? "",
  };

  orderedEditorKeys.splice(desiredInsertionIndex, 0, latestKey);

  // Wait for the editor to render and populate the ref
  await nextTick();

  await runDiagnosticsFunction();

  editorStateWatchers[latestKey] = watch(
    multiEditorComponentRefs[latestKey].getEditorState().text,
    runDiagnosticsFunction,
  );
  columnStateWatchers[latestKey] = watch(
    () => columnSelectorStates[latestKey],
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
  delete columnSelectorStates[filename];
  delete multiEditorComponentRefs[filename];
  delete columnSelectorStateErrorMessages[filename];

  // Clean up watchers
  editorStateWatchers[filename]();
  columnStateWatchers[filename]();
  delete editorStateWatchers[filename];
  delete columnStateWatchers[filename];

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
  columnSelectorStates[newKey] = { ...columnSelectorStates[filename] };
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
    runRowMapperExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN);
  }
});

const runButtonDisabledErrorReason = computed(() => {
  const errors: string[] = [];

  if (!inputsAvailable.value) {
    errors.push("No input available. Connect an executed node.");
  }

  if (anyEditorHasError()) {
    errors.push("An editor is invalid.");
  }

  const columnErrors = Object.entries(columnSelectorStateErrorMessages)
    .map(([key, message]) =>
      message === null ? null : `${getEditorTitleFromKey(key)}: ${message}`,
    )
    .filter((message) => message !== null);

  errors.push(...columnErrors);

  if (errors.length === 0) {
    return null;
  }

  let result = errors[0];

  if (errors.length > 1) {
    result += ` And ${errors.length - 1} more error${errors.length - 1 > 1 ? "s" : ""} not shown.`;
  }

  return result;
});

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
        :show-output-table="true"
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
            :error-state="editorErrorStates[key]"
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
                  v-model="columnSelectorStates[key]"
                  v-model:error-message="columnSelectorStateErrorMessages[key]"
                  entity-name="column"
                  :allowed-replacement-entities="
                    getAvailableColumnsForReplacement(key)
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
          <RunButton
            :run-button-disabled-error-reason="runButtonDisabledErrorReason"
            :show-button-text="showButtonText"
            @run-expressions="runRowMapperExpressions"
          />
        </template>
      </ScriptingEditor>
    </template>
  </main>
</template>

<style lang="postcss">
@import url("@knime/styles/css");

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
</style>
