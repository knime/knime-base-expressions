<script setup lang="ts">
import {
  type ComponentPublicInstance,
  computed,
  nextTick,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { onKeyStroke } from "@vueuse/core";
import { v4 as uuidv4 } from "uuid";

import { FunctionButton } from "@knime/components";
import {
  type UseCodeEditorReturn,
  getScriptingService,
  getSettingsService,
  setActiveEditorStoreForAi,
  useReadonlyStore,
} from "@knime/scripting-editor";
import NextIcon from "@knime/styles/img/icons/arrow-next.svg";
import PlusIcon from "@knime/styles/img/icons/circle-plus.svg";
import type { SettingState } from "@knime/ui-extension-service";

import { LANGUAGE } from "@/common/constants";
import { registerInsertionListener } from "@/common/functions";
import ExpressionEditorPane, {
  type ExpressionEditorPaneExposes,
} from "@/components/ExpressionEditorPane.vue";
import OutputSelector, {
  type AllowedDropDownValue,
  type ItemType,
  type SelectorState,
} from "@/components/OutputSelector.vue";
import ReturnTypeSelector from "@/components/ReturnTypeSelector.vue";
import {
  type ExpressionReturnType,
  type FlowVariableType,
  getDropDownValuesForCurrentType,
} from "@/flowVariableApp/flowVariableTypes";
import type {
  EditorErrorState,
  ExpressionDiagnostic,
} from "@/generalDiagnostics";

type EditorStateWithoutMonaco = {
  selectorState: SelectorState;
  editorErrorState: EditorErrorState;
  selectorErrorState: EditorErrorState;
  expressionReturnType: ExpressionReturnType;
  selectedFlowVariableOutputType?: FlowVariableType;
};
export type EditorState = EditorStateWithoutMonaco & {
  monacoState: UseCodeEditorReturn;
  key: string;
};
export type EditorStates = {
  states: EditorState[];
  activeEditorKey: string | null;
};
type EditorStateWatchers = {
  columnStateUnwatchHandle: Function;
  editorStateUnwatchHandle: Function;
};
export type MultiEditorContainerExposes = {
  getOrderedEditorStates: () => EditorState[];
  setEditorErrorState: (key: string, errorState: EditorErrorState) => void;
  setSelectorErrorState: (key: string, errorState: EditorErrorState) => void;
  setCurrentExpressionReturnType: (
    key: string,
    returnType: ExpressionReturnType,
  ) => void;
  setActiveEditor: (key: string) => void;
};

const activeEditorFileName = ref<string | null>(null);

const props = defineProps<{
  settings: {
    initialScript: string;
    initialSelectorState: SelectorState;
    initialOutputReturnType?: FlowVariableType;
  }[];
  replaceableItemsInInputTable: AllowedDropDownValue[];
  defaultAppendItem: string;
  itemType: ItemType;
}>();

const getActiveEditorKey = (): string | null => activeEditorFileName.value;

/**
 * Generate a new key for a new editor. This is used to index the columnSelectorStates
 * and the multiEditorComponentRefs, plus it's used to keep track of the order of the editors.
 */
const generateNewKey = (): string => uuidv4();
const createEditorTitle = (index: number) => `Expression ${index + 1}`;

// The canonical source of ordering truth, used to index the editorStates.
// Things are run in the order defined here, saved in the order defined here, etc.
const orderedEditorKeys = reactive<string[]>([]);
const numberOfEditors = computed(() => orderedEditorKeys.length);

const editorReferences = reactive<{
  [key: string]: ExpressionEditorPaneExposes;
}>({});
const editorStates = reactive<{ [key: string]: EditorStateWithoutMonaco }>({});
const editorStateWatchers = reactive<{ [key: string]: EditorStateWatchers }>(
  {},
);
const editorControlsExpanded = ref<{ [key: string]: boolean }>({});

const getEditorStatesWithMonacoState = (): EditorState[] =>
  orderedEditorKeys.map((key) => ({
    ...editorStates[key],
    monacoState: editorReferences[key].getEditorState(),
    key,
  }));

const getEditorStates = (): EditorStates => ({
  states: getEditorStatesWithMonacoState(),
  activeEditorKey: getActiveEditorKey(),
});

/*
 * Get a minimal state representation of all changes that matter
 * for the settings service to evaluate if the current state is
 * different from the last saved settings state.
 */
const getState = () => {
  return orderedEditorKeys
    .map(
      (key) =>
        `${key} ${editorReferences[key].getEditorState().text.value}
    ${editorStates[key].selectorState.outputMode}
    ${editorStates[key].selectorState.create}
    ${editorStates[key].selectorState.replace}
    ${editorStates[key].selectedFlowVariableOutputType}`,
    )
    .join("\n");
};

const onChangedState = ref<SettingState<string> | null>(null);

const emit = defineEmits<{
  "on-change": [EditorStates];
  "run-expressions": [editorStates: EditorState[]];
}>();

const emitOnChange = () => {
  onChangedState.value?.setValue(getState());
  emit("on-change", getEditorStates());
};

/**
 * Called by the ExpressionEditorPane when it gains focus. This function updates
 * the active editor and emits the change event.
 */
const onFocusChanged = (key: string) => {
  activeEditorFileName.value = key;
  setActiveEditorStoreForAi(editorReferences[key].getEditorState());

  // Scroll to the editor. The focusing somehow interferes with scrolling,
  // so wait for a tick first.
  nextTick().then(() => {
    editorReferences[key]
      ?.getEditorState()
      .editor.value?.getDomNode()
      ?.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
      });
  });
  emitOnChange();
};

/**
 * Focus the editor with the given key. Note that the focus will trigger onFocusChanged,
 * which updates the active editor and emits the change event.
 *
 * @param key The key of the editor to focus.
 */
const setActiveEditor = (key: string) =>
  editorReferences[key].getEditorState().editor.value?.focus();

defineExpose<MultiEditorContainerExposes>({
  getOrderedEditorStates: getEditorStatesWithMonacoState,
  setEditorErrorState(key: string, errorState: EditorErrorState) {
    editorStates[key].editorErrorState = errorState;
  },
  setSelectorErrorState(key: string, errorState: EditorErrorState) {
    editorStates[key].selectorErrorState = errorState;
  },
  setCurrentExpressionReturnType(
    key: string,
    returnType: ExpressionReturnType,
  ) {
    editorStates[key].expressionReturnType = returnType;
  },
  setActiveEditor,
});

const pushNewEditorState = ({
  key,
  expandControls,
}: {
  key: string;
  expandControls: boolean;
}) => {
  const newState: EditorStateWithoutMonaco = {
    selectorState: {
      outputMode: "APPEND",
      create: props.defaultAppendItem,
      replace: "",
    },
    editorErrorState: { level: "OK" },
    selectorErrorState: { level: "OK" },
    expressionReturnType: "UNKNOWN",
  };
  editorControlsExpanded.value[key] = expandControls;
  editorStates[key] = newState;
};

/**
 * Called by components when they're created, creates a function that
 * stores a ref to the component in our dict.
 */
const createElementReferencePusher = (key: string) => {
  return (el: Element | ComponentPublicInstance | null) => {
    if (key in editorReferences || el === null) {
      // Otherwise we'll keep creating more and more watchers
      return;
    }

    const editorRef = el as unknown as ExpressionEditorPaneExposes;

    editorReferences[key] = editorRef;

    const newStateWatchers = {
      columnStateUnwatchHandle: watch(
        () => editorStates[key].selectorState,
        emitOnChange,
        { deep: true },
      ),
      editorStateUnwatchHandle: watch(
        editorRef.getEditorState().text,
        emitOnChange,
      ),
    };
    editorStateWatchers[key] = newStateWatchers;
  };
};

// Input columns helpers
const getAvailableColumnsForReplacement = (
  key: string,
): AllowedDropDownValue[] => {
  const index = orderedEditorKeys.indexOf(key);

  const columnsFromPreviousEditors = orderedEditorKeys
    .slice(0, index)
    .filter((key) => editorStates[key].selectorState.outputMode === "APPEND")
    .map((key) => editorStates[key].selectorState.create)
    .map((column) => {
      return { id: column, text: column };
    });

  return [...props.replaceableItemsInInputTable, ...columnsFromPreviousEditors]
    .filter((flowVariableName) => !flowVariableName.text.startsWith("knime"))
    .filter((flowVariableName) => flowVariableName.text.trim() !== "");
};

/**
 * This function doesn't directly trigger an emit, because it is expected that
 * the caller will do so after the new editor is added.
 *
 * @param keyAbove
 */
const addNewEditorBelowExisting = async (keyAbove: string) => {
  const latestKey = generateNewKey();
  const desiredInsertionIndex = orderedEditorKeys.indexOf(keyAbove) + 1;
  orderedEditorKeys.splice(desiredInsertionIndex, 0, latestKey);

  pushNewEditorState({ key: latestKey, expandControls: true });

  // wait for the editor to be added to the DOM
  await nextTick();

  return latestKey;
};

const onRequestedAddEditorAtBottom = async () => {
  const latestKey = await addNewEditorBelowExisting(
    orderedEditorKeys[orderedEditorKeys.length - 1],
  );

  // setActiveEditor will emit the change event
  nextTick().then(() => setActiveEditor(latestKey));
};

const onEditorRequestedDelete = (key: string) => {
  if (numberOfEditors.value === 1) {
    return;
  }

  const indexOfDeletedEditor = orderedEditorKeys.indexOf(key);
  orderedEditorKeys.splice(indexOfDeletedEditor, 1);

  // Clean up state
  editorStateWatchers[key].columnStateUnwatchHandle();
  editorStateWatchers[key].editorStateUnwatchHandle();
  delete editorStates[key];
  delete editorStateWatchers[key];
  delete editorReferences[key];
  delete editorControlsExpanded.value[key];

  const keyToFocus = orderedEditorKeys[Math.max(0, indexOfDeletedEditor - 1)];

  // Now that the editor is gone, focus the one above it
  // setActiveEditor will emit the change event
  nextTick().then(() => setActiveEditor(keyToFocus));
};

const onEditorRequestedMoveUp = (key: string) => {
  const index = orderedEditorKeys.indexOf(key);
  if (index <= 0) {
    return;
  }

  // Swap the entries at index and index - 1
  [orderedEditorKeys[index], orderedEditorKeys[index - 1]] = [
    orderedEditorKeys[index - 1],
    orderedEditorKeys[index],
  ];

  // Focus the moved editor after rerendering
  // setActiveEditor will emit the change event
  nextTick().then(() => setActiveEditor(key));
};

const onEditorRequestedMoveDown = (key: string) => {
  const index = orderedEditorKeys.indexOf(key);
  if (index >= orderedEditorKeys.length - 1) {
    return;
  }

  // Swap the entries at index and index + 1
  [orderedEditorKeys[index], orderedEditorKeys[index + 1]] = [
    orderedEditorKeys[index + 1],
    orderedEditorKeys[index],
  ];

  // Focus the moved editor after rerendering
  // setActiveEditor will emit the change event
  nextTick().then(() => setActiveEditor(key));
};

const onEditorRequestedCopyBelow = async (key: string) => {
  const newKey = await addNewEditorBelowExisting(key);

  // Copy state from the editor above to the new editor
  editorStates[newKey].selectorState = {
    ...editorStates[key].selectorState,
  };
  editorStates[newKey].expressionReturnType =
    editorStates[key].expressionReturnType;
  editorStates[newKey].selectedFlowVariableOutputType =
    editorStates[key].selectedFlowVariableOutputType;

  editorControlsExpanded.value[newKey] = true;

  editorReferences[newKey]
    .getEditorState()
    .setInitialText(editorReferences[key].getEditorState().text.value);

  // focusing the editor will cause the change event to be emitted
  // setActiveEditor will emit the change event
  nextTick().then(() => setActiveEditor(newKey));
};

registerInsertionListener(() =>
  getActiveEditorKey() === null
    ? null
    : editorReferences[getActiveEditorKey()!],
);

// Shift+Enter while editor has focus runs expressions
onKeyStroke("Enter", (evt: KeyboardEvent) => {
  const atLeastOneEditorHasFocus = Object.values(editorReferences).some(
    (state) => state.getEditorState().editor.value?.hasTextFocus(),
  );

  const allEditorsAreOk = Object.values(editorStates).every(
    (state) => state.editorErrorState.level === "OK",
  );

  if (evt.shiftKey && atLeastOneEditorHasFocus && allEditorsAreOk) {
    evt.preventDefault();
    emit("run-expressions", getEditorStatesWithMonacoState());
  }
});

const getMostConcerningErrorStateForEditor = (
  key: string,
): EditorErrorState => {
  const editorState = editorStates[key];
  if (editorState?.editorErrorState.level === "ERROR") {
    return editorState.editorErrorState;
  }
  if (editorState?.selectorErrorState.level === "ERROR") {
    return editorState.selectorErrorState;
  }
  if (editorState?.editorErrorState.level === "WARNING") {
    return editorState.editorErrorState;
  }
  if (editorState?.selectorErrorState.level === "WARNING") {
    return editorState.selectorErrorState;
  }
  return editorState?.editorErrorState;
};

const setWarningsHandler = (warnings: ExpressionDiagnostic[]) => {
  for (let i = 0; i < warnings.length; i++) {
    const warning = warnings[i];
    if (warning) {
      editorStates[orderedEditorKeys[i]].editorErrorState = {
        level: warning.severity,
        message: warnings[i].message,
      };
    }
  }
};

onMounted(async () => {
  for (let i = 0; i < props.settings.length; ++i) {
    const key = generateNewKey();
    orderedEditorKeys.push(key);

    // Expand controls if there's only one editor, collapse otherwise
    pushNewEditorState({ key, expandControls: props.settings.length === 1 });
  }

  await nextTick();

  getScriptingService().registerEventHandler(
    "updateWarnings",
    setWarningsHandler,
  );

  for (let i = 0; i < orderedEditorKeys.length; ++i) {
    const key = orderedEditorKeys[i];
    editorReferences[key]
      .getEditorState()
      .setInitialText(props.settings[i].initialScript);
    editorStates[key].selectorState = props.settings[i].initialSelectorState;
    editorStates[key].expressionReturnType = "UNKNOWN";
    editorStates[key].selectedFlowVariableOutputType =
      props.settings[i].initialOutputReturnType;
  }

  const register = await getSettingsService().registerSettings("model");
  onChangedState.value = register(getState());

  // setActiveEditor emits the change event, so we don't need to do it here.
  setActiveEditor(orderedEditorKeys[0]);
});

const handleToggleExpand = (key: string) =>
  (editorControlsExpanded.value[key] = !editorControlsExpanded.value[key]);

const getOutputLabel = (key: string) => {
  const selectorState = editorStates[key].selectorState;
  const action = selectorState.outputMode === "APPEND" ? "creates" : "replaces";
  const target =
    selectorState.outputMode === "APPEND"
      ? selectorState.create
      : selectorState.replace;
  return `Output ${action} "${target}"`;
};
</script>

<template>
  <div class="multi-editor-container">
    <ExpressionEditorPane
      v-for="(key, index) in orderedEditorKeys"
      :key="key"
      :ref="createElementReferencePusher(key)"
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
      @focus="onFocusChanged(key)"
      @delete="onEditorRequestedDelete"
      @move-down="onEditorRequestedMoveDown"
      @move-up="onEditorRequestedMoveUp"
      @copy-below="onEditorRequestedCopyBelow"
    >
      <!-- Controls displayed once per editor -->
      <template #multi-editor-controls>
        <div class="output-label" @click="() => handleToggleExpand(key)">
          <span
            class="output-expansion-icon"
            :class="{
              expanded: editorControlsExpanded[key],
            }"
          >
            <NextIcon />
          </span>
          {{ getOutputLabel(key) }}
        </div>
        <div
          class="editor-controls"
          :class="{
            hidden: !editorControlsExpanded[key],
          }"
        >
          <span class="output-settings-container">
            <OutputSelector
              v-model="editorStates[key].selectorState"
              :item-type="itemType"
              :is-valid="editorStates[key].selectorErrorState.level === 'OK'"
              :allowed-replacement-entities="
                getAvailableColumnsForReplacement(key)
              "
            />
            <span
              v-if="itemType === 'flow variable'"
              class="return-type-selector"
            >
              <ReturnTypeSelector
                v-model="editorStates[key].selectedFlowVariableOutputType"
                :allowed-return-types="
                  getDropDownValuesForCurrentType(
                    editorStates[key].expressionReturnType,
                  )
                "
              />
            </span>
          </span>
        </div>
      </template>
    </ExpressionEditorPane>

    <FunctionButton
      class="add-new-editor-button"
      data-testid="add-new-editor-button"
      :disabled="useReadonlyStore().value"
      @click="onRequestedAddEditorAtBottom"
    >
      <PlusIcon /><span>Add expression</span>
    </FunctionButton>
  </div>
</template>

<style lang="postcss">
@import url("@knime/styles/css");
</style>

<style lang="postcss" scoped>
.multi-editor-container {
  display: flex;
  flex-direction: column;
  overflow: hidden scroll;
  min-height: 100%;
}

.add-new-editor-button {
  width: fit-content;
  margin: var(--space-16) auto;
  outline: 1px solid var(--knime-silver-sand);
}

.output-settings-container {
  display: flex;
  flex-direction: row;
  align-items: flex-end;
  gap: var(--space-8);
  width: 100%;
  margin-bottom: var(--space-8);
}

.editor-controls {
  width: 100%;
  padding: 0 var(--space-8);
  transition:
    max-height 0.4s ease-in-out,
    opacity 0.4s ease-in-out;
  max-height: 150px;
  opacity: 1;
  container-type: inline-size;

  &.hidden {
    max-height: 0;
    opacity: 0;
    overflow: hidden;
  }
}

.return-type-selector {
  width: 100%;
  max-width: 400px;
}

.output-expansion-icon {
  width: 12px;
  height: 12px;
  stroke: var(--knime-masala);
  transition: transform 0.3s ease;

  & svg {
    stroke-width: 2px;
  }

  &.expanded {
    transform: rotate(90deg);
  }

  & :hover {
    stroke: var(--knime-cornflower);
    background-color: var(--knime-stone-light);
    border-radius: 50%;
  }
}

.output-label {
  width: 100%;
  display: flex;
  flex-direction: row;
  cursor: pointer;
  gap: var(--space-4);
  padding: var(--space-8);
  font-size: 13px;
  font-weight: 500;
  line-height: 14px;
  text-align: left;
  user-select: none;
}

@container (width < 390px) {
  .output-settings-container {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: var(--space-8);
    width: 100%;
  }
}
</style>
