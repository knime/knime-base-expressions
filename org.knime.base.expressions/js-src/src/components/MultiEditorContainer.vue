<script setup lang="ts">
import {
  useReadonlyStore,
  type UseCodeEditorReturn,
  getScriptingService,
  consoleHandler,
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
  type SelectorState,
} from "@/components/OutputSelector.vue";
import type {
  EditorErrorState,
  ExpressionDiagnostic,
} from "@/generalDiagnostics";

type EditorStateWithoutMonaco = {
  selectorState: SelectorState;
  editorErrorState: EditorErrorState;
  selectorErrorState: EditorErrorState;
};
export type EditorState = EditorStateWithoutMonaco & {
  monacoState: UseCodeEditorReturn;
  key: string;
};
type EditorStateWatchers = {
  columnStateUnwatchHandle: Function;
  editorStateUnwatchHandle: Function;
};
export type MultiEditorContainerExposes = {
  getOrderedEditorStates: () => EditorState[];
  setEditorErrorState: (key: string, errorState: EditorErrorState) => void;
  setSelectorErrorState: (key: string, errorState: EditorErrorState) => void;
};

const activeEditorFileName = ref<string | null>(null);

const props = defineProps<{
  settings: {
    initialScript: string;
    initialSelectorState: SelectorState;
  }[];
  replaceableItemsInInputTable: AllowedDropDownValue[];
  defaultReplacementItem: string;
  defaultAppendItem: string;
  itemType: string;
}>();

const emit = defineEmits<{
  "editor-states-changed": [editorStates: EditorState[]];
  "run-expressions": [editorStates: EditorState[]];
  "active-editor-changed": [editorState: EditorState];
}>();

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

defineExpose<MultiEditorContainerExposes>({
  getOrderedEditorStates: getEditorStatesWithMonacoState,
  setEditorErrorState(key: string, errorState: EditorErrorState) {
    editorStates[key].editorErrorState = errorState;
  },
  setSelectorErrorState(key: string, errorState: EditorErrorState) {
    editorStates[key].selectorErrorState = errorState;
  },
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
        () => emit("editor-states-changed", getEditorStatesWithMonacoState()),
        { deep: true },
      ),
      editorStateUnwatchHandle: watch(editorRef.getEditorState().text, () =>
        emit("editor-states-changed", getEditorStatesWithMonacoState()),
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

const getActiveEditorKey = (): string | null => {
  if (activeEditorFileName.value === null) {
    return null;
  }

  return activeEditorFileName.value;
};

const onEditorFocused = (key: string) => {
  activeEditorFileName.value = key;
  emit("active-editor-changed", {
    ...editorStates[key],
    monacoState: editorReferences[key].getEditorState(),
    key,
  });

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

  emit("editor-states-changed", getEditorStatesWithMonacoState());
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

  emit("editor-states-changed", getEditorStatesWithMonacoState());
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

  emit("editor-states-changed", getEditorStatesWithMonacoState());
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

  emit("editor-states-changed", getEditorStatesWithMonacoState());
};

const onEditorRequestedCopyBelow = async (key: string) => {
  const newKey = await addNewEditorBelowExisting(key);

  // Copy state from the editor above to the new editor
  editorStates[newKey].selectorState = {
    ...editorStates[key].selectorState,
  };
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
  if (
    evt.shiftKey &&
    Object.values(editorReferences).some((state) =>
      state.getEditorState().editor.value?.hasTextFocus(),
    )
  ) {
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
  }

  emit("editor-states-changed", getEditorStatesWithMonacoState());
});
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
            v-model="editorStates[key].selectorState"
            :entity-name="itemType"
            :is-valid="editorStates[key].selectorErrorState.level === 'OK'"
            :allowed-replacement-entities="
              getAvailableColumnsForReplacement(key)
            "
          />
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
</style>
