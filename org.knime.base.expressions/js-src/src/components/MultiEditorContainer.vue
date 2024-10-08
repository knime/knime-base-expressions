<script setup lang="ts">
import {
  consoleHandler,
  getScriptingService,
  setActiveEditorStoreForAi,
  type UseCodeEditorReturn,
  useReadonlyStore,
} from "@knime/scripting-editor";
import { FunctionButton } from "@knime/components";
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
import { v4 as uuidv4 } from "uuid";
import ExpressionEditorPane, {
  type ExpressionEditorPaneExposes,
} from "@/components/ExpressionEditorPane.vue";
import { LANGUAGE } from "@/common/constants";
import { registerInsertionListener } from "@/common/functions";
import OutputSelector, {
  type AllowedDropDownValue,
  type ItemType,
  type SelectorState,
} from "@/components/OutputSelector.vue";
import type {
  EditorErrorState,
  ExpressionDiagnostic,
} from "@/generalDiagnostics";
import ReturnTypeSelector from "@/components/ReturnTypeSelector.vue";
import {
  type ExpressionReturnType,
  type FlowVariableType,
  getDropDownValuesForCurrentType,
} from "@/flowVariableApp/flowVariableTypes";
import NextIcon from "@knime/styles/img/icons/arrow-next.svg";

type EditorStateWithoutMonaco = {
  selectorState: SelectorState;
  editorErrorState: EditorErrorState;
  selectorErrorState: EditorErrorState;
  expressionReturnType: ExpressionReturnType;
  selectedFlowVariableOutputType?: FlowVariableType;
  expanded: boolean;
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
  defaultReplacementItem: string;
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

const emit = defineEmits<{
  "on-change": [EditorStates];
  "run-expressions": [editorStates: EditorState[]];
}>();

const emitOnChange = () => emit("on-change", getEditorStates());

const setActiveEditor = (key: string) => {
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
        block: "center",
      });
  });
  emitOnChange();
};

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

const pushNewEditorState = (key: string) => {
  const newState: EditorStateWithoutMonaco = {
    selectorState: {
      outputMode: "APPEND",
      create: props.defaultAppendItem,
      replace: props.defaultReplacementItem,
    },
    editorErrorState: { level: "OK" },
    selectorErrorState: { level: "OK" },
    expressionReturnType: "UNKNOWN",
    expanded: false,
  };
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

  return [...props.replaceableItemsInInputTable, ...columnsFromPreviousEditors];
};

const addNewEditorBelowExisting = async (keyAbove: string) => {
  const latestKey = generateNewKey();
  const desiredInsertionIndex = orderedEditorKeys.indexOf(keyAbove) + 1;
  orderedEditorKeys.splice(desiredInsertionIndex, 0, latestKey);

  pushNewEditorState(latestKey);

  // wait for the editor to be added to the DOM
  await nextTick();

  return latestKey;
};

const onRequestedAddEditorAtBottom = async () => {
  const latestKey = await addNewEditorBelowExisting(
    orderedEditorKeys[orderedEditorKeys.length - 1],
  );

  nextTick().then(() => {
    editorReferences[latestKey].getEditorState().editor.value?.focus();
  });

  emitOnChange();
};

const onEditorRequestedDelete = (key: string) => {
  if (numberOfEditors.value === 1) {
    return;
  }

  orderedEditorKeys.splice(orderedEditorKeys.indexOf(key), 1);

  // Clean up state
  editorStateWatchers[key].columnStateUnwatchHandle();
  editorStateWatchers[key].editorStateUnwatchHandle();
  delete editorStates[key];
  delete editorStateWatchers[key];
  delete editorReferences[key];

  emitOnChange();
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
  nextTick().then(() => {
    editorReferences[key].getEditorState().editor.value?.focus();
  });

  emitOnChange();
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
  nextTick().then(() => {
    editorReferences[key].getEditorState().editor.value?.focus();
  });

  emitOnChange();
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

  editorReferences[newKey]
    .getEditorState()
    .setInitialText(editorReferences[key].getEditorState().text.value);

  nextTick().then(() => {
    editorReferences[newKey].getEditorState().editor.value?.focus();
  });

  // Don't need to explicitly emit because changing the text will trigger the
  // watcher which will emit the change event.
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
    if (warnings[i]) {
      editorStates[orderedEditorKeys[i]].editorErrorState = {
        level: "WARNING",
        message: warnings[i].message,
      };
    }
  }
  for (const warning of warnings) {
    // TODO(AP-23173) remove warnings from console
    if (warning?.severity === "WARNING") {
      consoleHandler.writeln({
        warning: warning.message,
      });
    }
  }
};

onMounted(async () => {
  for (let i = 0; i < props.settings.length; ++i) {
    const key = generateNewKey();
    orderedEditorKeys.push(key);
    pushNewEditorState(key);
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

  emitOnChange();
});

const handleToggleExpand = (key: string) =>
  (editorStates[key].expanded = !editorStates[key].expanded);

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
      @focus="setActiveEditor(key)"
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
              expanded: editorStates[key].expanded,
            }"
          >
            <NextIcon />
          </span>
          {{ getOutputLabel(key) }}
        </div>
        <div
          class="editor-controls"
          :class="{
            hidden: !editorStates[key].expanded && key !== activeEditorFileName,
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
            <span v-if="itemType === 'flow variable'" class="input">
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
.input {
  width: 100%;
  max-width: 400px;
}

.multi-editor-container {
  display: flex;
  flex-direction: column;
  overflow: hidden scroll;
  min-height: 100%;
  container-type: inline-size;
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

  &.hidden {
    max-height: 0;
    opacity: 0;
    overflow: hidden;
  }
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
