<script setup lang="ts">
import {
  type FunctionalComponent,
  type SVGAttributes,
  computed,
  nextTick,
  onMounted,
  ref,
  watch,
} from "vue";
import { onKeyStroke } from "@vueuse/core";
import { editor as monaco } from "monaco-editor";

import { FunctionButton } from "@knime/components";
import {
  type UseCodeEditorReturn,
  editor,
  useReadonlyStore,
} from "@knime/scripting-editor";
import DownArrowIcon from "@knime/styles/img/icons/arrow-down.svg";
import UpArrowIcon from "@knime/styles/img/icons/arrow-up.svg";
import WarningIcon from "@knime/styles/img/icons/circle-warning.svg";
import CopyIcon from "@knime/styles/img/icons/copy.svg";
import TrashIcon from "@knime/styles/img/icons/trash.svg";

import {
  resetDraggedFunctionStore,
  useDraggedFunctionStore,
} from "@/draggedFunctionStore";
import type { EditorErrorState } from "@/generalDiagnostics";

import EditorOption = monaco.EditorOption;

export type ExpressionEditorPaneExposes = {
  getEditorState: () => UseCodeEditorReturn;
};

const emit = defineEmits<{
  focus: [filname: string];
  "move-up": [filename: string];
  "move-down": [filename: string];
  delete: [filename: string];
  "copy-below": [filename: string];
}>();

type EditorOrderingOptions = {
  isFirst?: boolean;
  isLast?: boolean;
  isOnly?: boolean;
  isActive?: boolean;
  disableMultiEditorControls?: boolean;
};

interface Props {
  language: string;
  fileName: string;
  title: string;
  orderingOptions?: EditorOrderingOptions;
  errorState?: EditorErrorState;
}

const props = withDefaults(defineProps<Props>(), {
  orderingOptions: () => ({
    isFirst: false,
    isLast: false,
    isOnly: false,
    isActive: false,
    disableMultiEditorControls: false,
  }),
  errorState: () => ({ level: "OK" }),
});

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
    disabled: props.orderingOptions.isFirst || readOnly.value,
  },
  {
    text: "Move downwards",
    icon: DownArrowIcon,
    eventName: "move-down",
    disabled: props.orderingOptions.isLast || readOnly.value,
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
    disabled: props.orderingOptions.isOnly || readOnly.value,
  },
]);

// Main editor
const monacoEditorContainerRef = ref<HTMLDivElement>();
const editorState = editor.useCodeEditor({
  language: props.language,
  fileName: props.fileName,
  container: monacoEditorContainerRef,
  extraEditorOptions: {
    scrollBeyondLastLine: false,
    automaticLayout: true,
    minimap: { enabled: false },
    overviewRulerLanes: 0,
    overviewRulerBorder: false,
    readOnly: useReadonlyStore().value,
    readOnlyMessage: {
      value: "Read-Only-Mode: configuration is set by flow variables.",
    },
    renderValidationDecorations: "on",
  },
});

const getEditorState = () => editorState;
defineExpose<ExpressionEditorPaneExposes>({
  getEditorState,
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

const updateEditorHeight = async () => {
  // If the query for the line height fails, we use this as a fallback
  const DEFAULT_LINE_HEIGHT_IN_PIXEL = 13;
  const lineHeight =
    editorState.editor.value?.getOptions().get(EditorOption.lineHeight) ??
    DEFAULT_LINE_HEIGHT_IN_PIXEL;

  const MINIMUM_LINES_IN_EDITOR = 5;
  const contentHeight = Math.max(
    MINIMUM_LINES_IN_EDITOR * lineHeight,
    editorState.editor.value?.getContentHeight() ?? 0,
  );
  if (monacoEditorContainerRef.value && editor) {
    monacoEditorContainerRef.value.style.height = `${contentHeight}px`;
  }
  await nextTick();
  editorState.editor.value?.layout();
};

onMounted(() => {
  updateEditorHeight();
  editorState.editor.value?.onDidChangeModelContent(updateEditorHeight);
});
</script>

<template>
  <div
    class="editor-and-controls-container"
    :class="{
      'has-error': errorState.level === 'ERROR',
      'has-warning': errorState.level === 'WARNING',
      active: props.orderingOptions.isActive,
    }"
    @focusin="onFocus"
  >
    <div class="everything-except-error">
      <span class="editor-title-bar">
        <span class="title-text">{{ props.title }}</span>
        <span
          v-if="!props.orderingOptions.disableMultiEditorControls"
          class="title-menu"
        >
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

      <div
        ref="monacoEditorContainerRef"
        class="code-editor"
        @drop="onDropEvent"
      />

      <span class="editor-control-bar">
        <slot name="multi-editor-controls" />
      </span>
    </div>
    <div class="error-container">
      <span v-if="errorState.level !== 'OK'">
        <WarningIcon class="icon error-icon" />
        <span class="error-message"> {{ errorState.message }} </span>
      </span>
      <span v-else>&nbsp;</span>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.editor-and-controls-container {
  margin: var(--space-4) var(--space-12);
  position: relative;
  display: flex;
  flex-shrink: 1;
  flex-direction: column;
  height: fit-content;

  --border-colour: var(--knime-cornflower);

  &.has-error {
    --border-colour: var(--knime-coral-dark);
    --error-text-colour: var(--knime-coral-dark);
  }

  &.has-warning {
    --border-colour: var(--knime-carrot);
    --error-text-colour: var(--knime-carrot);
  }

  &.has-warning,
  &.has-error {
    & .everything-except-error::after {
      position: absolute;
      content: "";
      border: 1px solid var(--border-colour);
      pointer-events: none;
      z-index: 1;
      inset: -1px;
    }
  }

  &:first-child {
    /* Editor gets an extra margin iff it's the first one of its type. */
    margin-top: var(--space-12);
  }

  & .everything-except-error {
    box-shadow: var(--shadow-elevation-1);
    flex-grow: 1;
    flex-shrink: 1;
    display: flex;
    flex-direction: column;
    min-height: 70px;
    position: relative;

    &:hover {
      box-shadow: var(--shadow-elevation-2);
    }

    & .editor-title-bar {
      height: var(--space-32);
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

      & .title-text {
        font-size: 13px;
      }
    }

    & .code-editor {
      width: 100%;
      display: flex;
      position: relative;
    }

    & .editor-control-bar {
      background-color: var(--knime-gray-light-semi);
      border-top: 1px solid var(--knime-silver-sand);
      height: fit-content;
    }
  }

  &:focus-within,
  &.active {
    & .everything-except-error::after {
      position: absolute;
      content: "";
      border: 1px solid var(--border-colour);
      pointer-events: none;
      z-index: -1;
      inset: -1px;
    }

    & .code-editor::after {
      position: absolute;
      content: "";
      background-color: var(--knime-cornflower);
      opacity: 0.075;
      pointer-events: none;
      z-index: 0;
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

  & .error-container {
    display: flex;
    flex-flow: row nowrap;
    align-items: flex-start;
    margin-top: 2px;
    margin-bottom: -1px;
    margin-left: var(--space-4);
    min-height: 15px;

    & .error-message {
      color: var(--error-text-colour);
      font-size: 10px;
      line-height: 12px;
      word-break: break-word;
    }

    & .error-icon {
      stroke: var(--error-text-colour);
      width: 12px;
      min-width: 12px;
      height: 12px;
      margin-right: var(--space-4);
      translate: 0 2px;
    }
  }
}
</style>
