<script setup lang="ts">
import { editor, type UseCodeEditorReturn } from "@knime/scripting-editor";
import { onKeyStroke } from "@vueuse/core";
import { ref, watch, computed } from "vue";
import {
  useDraggedFunctionStore,
  resetDraggedFunctionStore,
} from "@/draggedFunctionStore";
import SubMenu from "webapps-common/ui/components/SubMenu.vue";
import type { MenuItem } from "webapps-common/ui/components/MenuItems.vue";
import TrashIcon from "webapps-common/ui/assets/img/icons/trash.svg";
import UpArrowIcon from "webapps-common/ui/assets/img/icons/arrow-up.svg";
import DownArrowIcon from "webapps-common/ui/assets/img/icons/arrow-down.svg";
import MenuIcon from "webapps-common/ui/assets/img/icons/menu-options.svg";
import PlusIcon from "webapps-common/ui/assets/img/icons/circle-plus.svg";

export type ExpressionEditorPaneExposes = {
  getEditorState: () => UseCodeEditorReturn;
};

const emit = defineEmits<{
  focus: [filname: string];
  "move-up": [filename: string];
  "move-down": [filename: string];
  delete: [filename: string];
  "add-below": [filename: string];
}>();

interface Props {
  language: string;
  fileName: string;
  title: string;
  isFirst?: boolean;
  isLast?: boolean;
  isOnly?: boolean;
}
const props = defineProps<Props>();

const submenuItems = computed<MenuItem[]>(() => [
  {
    text: "Move upwards",
    icon: UpArrowIcon,
    metadata: "move-up",
    disabled: props.isFirst,
  },
  {
    text: "Move downwards",
    icon: DownArrowIcon,
    metadata: "move-down",
    disabled: props.isLast,
  },
  {
    text: "Add editor below",
    icon: PlusIcon,
    metadata: "add-below",
  },
  {
    text: "Delete",
    icon: TrashIcon,
    metadata: "delete",
    disabled: props.isOnly,
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

const onMenuItemClicked = (evt: Event, item: MenuItem) => {
  emit(item.metadata, props.fileName);
};
</script>

<template>
  <div class="editor-container">
    <span class="editor-title-bar">
      <span class="title-text">{{ props.title }}</span>
      <SubMenu
        :items="submenuItems"
        class="title-menu"
        @item-click="onMenuItemClicked"
      >
        <MenuIcon class="open-icon" />
      </SubMenu>
    </span>
    <div
      ref="editorContainer"
      class="code-editor"
      @drop="onDropEvent"
      @focusin="onFocus"
    />
    <span class="editor-control-bar">
      <slot name="multi-editor-controls" />
    </span>
  </div>
</template>

<style scoped>
.code-editor {
  flex-grow: 1;
  flex-shrink: 1;
  display: flex;
  min-height: 70px;
}

.editor-container {
  --title-bar-height: 30px;
  --min-editor-height: 70px;

  margin-bottom: var(--space-8);
  margin-left: var(--space-8);
  margin-right: var(--space-8);
  box-shadow: var(--shadow-elevation-1);
  position: relative;
  display: flex;
  flex-direction: column;
  flex-grow: 1;

  /* Max height should be parent height */
  max-height: 100%;
  min-height: 200px;
}

.editor-container:focus-within::after {
  position: absolute;
  content: "";
  border: 2px solid var(--knime-cornflower);
  pointer-events: none;
  z-index: 1;
  inset: -3px;
}

/* Editor gets an extra margin iff it's the first one of its type.
Basically gives us some nice margin collapsing. */
.editor-container:first-child {
  margin-top: var(--space-8);
}

.editor-title-bar {
  height: var(--title-bar-height);
  padding-left: var(--space-16);
  background-color: var(--knime-porcelain);
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;

  & .title-menu {
    background-color: transparent;
  }
}

.editor-control-bar {
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
</style>
