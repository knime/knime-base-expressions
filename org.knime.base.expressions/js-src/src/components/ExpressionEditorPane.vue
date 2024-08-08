<script setup lang="ts">
import {
  editor,
  type UseCodeEditorReturn,
  useReadonlyStore,
} from "@knime/scripting-editor";
import { onKeyStroke } from "@vueuse/core";
import {
  ref,
  watch,
  computed,
  type FunctionalComponent,
  type SVGAttributes,
} from "vue";
import {
  useDraggedFunctionStore,
  resetDraggedFunctionStore,
} from "@/draggedFunctionStore";
import TrashIcon from "@knime/styles/img/icons/trash.svg";
import UpArrowIcon from "@knime/styles/img/icons/arrow-up.svg";
import DownArrowIcon from "@knime/styles/img/icons/arrow-down.svg";
import CopyIcon from "@knime/styles/img/icons/copy.svg";
import { FunctionButton } from "@knime/components";
import { type ErrorLevel } from "@/expressionDiagnostics";

export type ExpressionEditorPaneExposes = {
  getEditorState: () => UseCodeEditorReturn;
  setErrorLevel: (errorLevel: ErrorLevel) => void;
};

const errorLevel = ref<ErrorLevel>("OK");

const emit = defineEmits<{
  focus: [filname: string];
  "move-up": [filename: string];
  "move-down": [filename: string];
  delete: [filename: string];
  "copy-below": [filename: string];
}>();

interface Props {
  language: string;
  fileName: string;
  title: string;
  isFirst?: boolean;
  isLast?: boolean;
  isOnly?: boolean;
  isActive?: boolean;
}
const props = defineProps<Props>();
const readOnly = useReadonlyStore();

type ButtonItem = {
  text: string;
  icon: FunctionalComponent<SVGAttributes>;
  eventName: string;
  disabled?: boolean;
};

const buttonActions = computed<ButtonItem[]>(() => [
  {
    text: "Move upwards",
    icon: UpArrowIcon,
    eventName: "move-up",
    disabled: props.isFirst || readOnly.value,
  },
  {
    text: "Move downwards",
    icon: DownArrowIcon,
    eventName: "move-down",
    disabled: props.isLast || readOnly.value,
  },
  {
    text: "Duplicate below",
    icon: CopyIcon,
    eventName: "copy-below",
    disabled: readOnly.value,
  },
  {
    text: "Delete",
    icon: TrashIcon,
    eventName: "delete",
    disabled: props.isOnly || readOnly.value,
  },
]);

// Main editor
const editorContainer = ref<HTMLDivElement>();
const editorState = editor.useCodeEditor({
  language: props.language,
  fileName: props.fileName,
  container: editorContainer,
  hideOverviewRulerLanes: true,
});

const getEditorState = () => editorState;
const setErrorLevel = (level: ErrorLevel) => {
  errorLevel.value = level;
};
defineExpose<ExpressionEditorPaneExposes>({
  getEditorState,
  setErrorLevel,
});

onKeyStroke("Escape", () => {
  if (editorState.editor.value?.hasTextFocus()) {
    (document.activeElement as HTMLElement)?.blur();
  }
});

const onFocus = () => {
  emit("focus", props.fileName);
};

const draggedFunctionStore = useDraggedFunctionStore();

const onDropEvent = (e: DragEvent) => {
  if (e.dataTransfer?.getData("eventSource") === "function-catalog") {
    // The text changes once the browser has processed the drop event
    // (which includes only the function name), so we watch the text
    // here and then put the arguments in.
    const unwatch = watch(editorState.text, () => {
      if (draggedFunctionStore.draggedFunctionData) {
        const functionArguments =
          draggedFunctionStore.draggedFunctionData?.arguments;

        // We pass an empty function name here, as the function name is
        // inserted by the browser on drop. Hence we're only inserting
        // the arguments, with monaco snippet behavior.
        if (functionArguments !== null) {
          editorState.insertFunctionReference("", functionArguments);
        }
        resetDraggedFunctionStore();
      }

      unwatch();
    });
  }
};

// register undo changes from outside the editor
onKeyStroke("z", (e) => {
  const key = navigator.userAgent.toLowerCase().includes("mac")
    ? e.metaKey
    : e.ctrlKey;

  // If we have multiple editors, only trigger undo if the editor has focus
  if (key && editorState.editor.value?.hasTextFocus()) {
    editorState.editor.value?.trigger("window", "undo", {});
  }
});

const onMenuItemClicked = (item: ButtonItem) => {
  // @ts-ignore TS doesn't like dyanmic event names
  emit(item.eventName, props.fileName);
};
</script>

<template>
  <div
    class="editor-container"
    :class="{
      error: errorLevel === 'ERROR',
      active: isActive,
    }"
    @focusin="onFocus"
  >
    <span class="editor-title-bar">
      <span class="title-text">{{ props.title }}</span>
      <span class="title-menu">
        <FunctionButton
          v-for="item in buttonActions"
          :key="item.text"
          :disabled="item.disabled"
          class="menu-button"
          compact
          @click="onMenuItemClicked(item)"
        >
          <component :is="item.icon" />
        </FunctionButton>
      </span>
    </span>
    <div ref="editorContainer" class="code-editor" @drop="onDropEvent" />
    <span class="editor-control-bar">
      <slot name="multi-editor-controls" />
    </span>
  </div>
</template>

<style lang="postcss" scoped>
.editor-container {
  margin: var(--space-12) var(--space-8);
  box-shadow: var(--shadow-elevation-1);
  position: relative;
  display: flex;
  flex-direction: column;
  flex-grow: 1;

  --border-colour: var(--knime-cornflower);

  &.error {
    --border-colour: var(--knime-coral-dark);
  }

  &.warning {
    --border-colour: var(--knime-carrot);
  }

  &.warning,
  &.error {
    &::after {
      position: absolute;
      content: "";
      border: 1px solid var(--border-colour);
      pointer-events: none;
      z-index: 1;
      inset: -1px;
    }
  }

  &:hover {
    box-shadow: var(--shadow-elevation-2);
  }

  /* Max height should be parent height */
  max-height: 100%;
  min-height: 200px;

  &:first-child {
    /* Editor gets an extra margin iff it's the first one of its type. */
    margin-top: var(--space-24);
  }

  & .editor-title-bar {
    height: 30px;
    padding: 0 var(--space-16);
    background-color: var(--knime-porcelain);
    flex-shrink: 0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 400;
    font-family: Roboto, sans-serif;

    & .title-menu {
      background-color: transparent;
      display: flex;
      gap: var(--space-8);
    }
  }

  & .code-editor {
    flex-grow: 1;
    flex-shrink: 1;
    display: flex;
    min-height: 70px;
    position: relative;
  }

  & .editor-control-bar {
    display: flex;
    align-items: center;
    flex-direction: row;
    justify-content: flex-end;
    margin: 0;
    background-color: var(--knime-gray-light-semi);
    border-top: 1px solid var(--knime-silver-sand);
    height: fit-content;
    flex-grow: 1;
  }

  &:focus-within,
  &.active {
    &::after {
      position: absolute;
      content: "";
      border: 1px solid var(--border-colour);
      pointer-events: none;
      z-index: 1;
      inset: -1px;
    }

    & .code-editor::after {
      position: absolute;
      content: "";
      background-color: var(--knime-cornflower);
      opacity: 0.075;
      pointer-events: none;
      z-index: 1;
      inset: 0;
    }

    & .editor-title-bar {
      background-color: var(--knime-cornflower);
      color: var(--knime-porcelain);

      & .title-menu .menu-button :deep(svg) {
        stroke: var(--knime-porcelain);
      }
    }
  }
}
</style>
