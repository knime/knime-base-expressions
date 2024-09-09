<script setup lang="ts">
import {
  editor,
  type UseCodeEditorReturn,
  useReadonlyStore,
} from "@knime/scripting-editor";
import { onKeyStroke } from "@vueuse/core";
import {
  computed,
  type FunctionalComponent,
  ref,
  type SVGAttributes,
  watch,
} from "vue";
import {
  resetDraggedFunctionStore,
  useDraggedFunctionStore,
} from "@/draggedFunctionStore";
import TrashIcon from "@knime/styles/img/icons/trash.svg";
import UpArrowIcon from "@knime/styles/img/icons/arrow-up.svg";
import DownArrowIcon from "@knime/styles/img/icons/arrow-down.svg";
import CopyIcon from "@knime/styles/img/icons/copy.svg";
import WarningIcon from "@knime/styles/img/icons/circle-warning.svg";
import { FunctionButton } from "@knime/components";
import type { EditorErrorState } from "@/common/types";

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
    overviewRulerLanes: 0,
    overviewRulerBorder: false,
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
      <WarningIcon v-if="errorState.level !== 'OK'" class="error-icon" />
      <span v-if="errorState.level !== 'OK'" class="error-message">
        {{ errorState.message }}
      </span>
    </div>
  </div>
</template>

<style lang="postcss" scoped>
.editor-and-controls-container {
  margin: var(--space-4) var(--space-8);
  position: relative;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  height: 0;

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

  /* Max height should be parent height */
  max-height: 100%;
  min-height: 200px;

  &:first-child {
    /* Editor gets an extra margin iff it's the first one of its type. */
    margin-top: var(--space-24);
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
    }
  }

  &:focus-within,
  &.active {
    & .everything-except-error::after {
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

  & .error-container {
    display: flex;
    flex-flow: row nowrap;
    align-items: flex-start;
    margin-top: var(--space-4);
    margin-left: var(--space-4);
    min-height: 15px;

    & .error-message {
      color: var(--error-text-colour);
      font-size: 12px;
      line-height: 14px;
      word-wrap: normal;
    }

    & .error-icon {
      stroke: var(--error-text-colour);
      width: 13px;
      min-width: 13px;
      height: 13px;
      margin-right: var(--space-8);
    }
  }
}
</style>
