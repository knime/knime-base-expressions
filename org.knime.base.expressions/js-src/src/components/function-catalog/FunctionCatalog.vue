<script setup lang="ts">
import { computed, ref, reactive, type ComponentPublicInstance } from "vue";
import SearchInput from "../../../webapps-common/ui/components/forms/SearchInput.vue";
import NextIcon from "../../../webapps-common/ui/assets/img/icons/arrow-next.svg";
import { useElementBounding } from "@vueuse/core";
import { mapFunctionCatalogData } from "@/components/function-catalog/mapFunctionCatalogData";
import type {
  FunctionCatalogData,
  FunctionCatalogEntryData,
} from "@/components/functionCatalogTypes";
import { filterCatalogData } from "@/components/function-catalog/filterCatalogData";
import type {
  SelectableItem,
  SelectableFunction,
} from "@/components/function-catalog/catalogTypes";
import CategoryDescription from "@/components/function-catalog/CategoryDescription.vue";
import { createDragGhosts } from "webapps-common/ui/components/FileExplorer/dragGhostHelpers";
import { EMPTY_DRAG_IMAGE } from "webapps-common/ui/components/FileExplorer/useItemDragging";
import { useDraggedFunctionStore } from "@/draggedFunctionStore";
import type { MathConstantData } from "@/expressionScriptingService";
import FunctionDescription from "@/components/function-catalog/FunctionDescription.vue";

const MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION = 600;
const FUNCTION_CATALOG_WIDTH = "300px";

const props = defineProps<{
  functionCatalogData: FunctionCatalogData;
  constantData: MathConstantData;
  initiallyExpanded: boolean;
}>();

const catalogData = mapFunctionCatalogData(
  props.functionCatalogData,
  props.constantData,
);
const categoryNames = Object.keys(catalogData);
const categories = ref(
  Object.fromEntries(
    categoryNames.map((categoryName) => [
      categoryName,
      {
        expanded: props.initiallyExpanded,
        name: categoryName,
        description:
          props.functionCatalogData.categories.find(
            (category) => category.name === categoryName,
          )?.description ?? "No description for category available",
      },
    ]),
  ),
);

const removeGhostsRef = ref<() => void>(() => {});

const draggedFunctionStore = useDraggedFunctionStore();

const onDragStart = (e: DragEvent, f: FunctionCatalogEntryData) => {
  const { removeGhosts } = createDragGhosts({
    badgeCount: null, // don't show a badge at all
    dragStartEvent: e,
    selectedTargets: [
      {
        textContent: f.name,
        targetEl: e.target as HTMLElement,
      },
    ],
  });

  removeGhostsRef.value = removeGhosts;

  // We only insert the function name, not the arguments, as the arguments are
  // inserted as a snippet when the function is dropped. The snippet will be
  // handled by the editor.
  e.dataTransfer?.setData("text", f.name);
  e.dataTransfer?.setData("eventSource", "function-catalog");

  // Chrome doesn't let you use e.dataTransfer, so we have to use a global store
  // instead to pass the extra data.
  draggedFunctionStore.draggedFunctionData = {
    name: f.name,
    arguments:
      f.entryType === "mathConstant"
        ? null
        : f.arguments?.map((arg) => arg.name)!,
  };

  e.dataTransfer?.setDragImage(EMPTY_DRAG_IMAGE, 0, 0);
};

const onDragEnd = () => {
  removeGhostsRef.value();
};

const emit = defineEmits(["functionInsertionEvent"]);
const triggerFunctionInsertionEvent = (
  evt: Event,
  f: FunctionCatalogEntryData,
) => {
  emit("functionInsertionEvent", {
    functionName: f.name,
    functionArgs:
      f.entryType === "mathConstant"
        ? null
        : f.arguments?.map((arg) => arg.name),
    eventSource: "function-catalog",
  });
};

const catalogRoot = ref();
const catalogRootRef = useElementBounding(catalogRoot);
const isSlimMode = computed(
  () => catalogRootRef.width.value <= MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION,
);

const selectedEntry = ref<null | SelectableItem>(null);

const functionAndCategoryElementRefs = reactive<{ [key: string]: HTMLElement }>(
  {},
);

const createElementName = (type: "function" | "category", name: string) =>
  `ref-${type}-${name}`;

// You can pass a function to v-bind:ref and let it store the element in a
// custom way. We'll use this to keep track of all of our function and category
// headers by storing them by their type+name in
// `functionAndCategoryElementRefs`
const createElementReference = (
  type: "function" | "category",
  name: string,
) => {
  return (el: Element | ComponentPublicInstance | null) => {
    functionAndCategoryElementRefs[createElementName(type, name)] =
      el as HTMLElement;
  };
};

const selectEntry = (item: SelectableItem | null) => {
  selectedEntry.value = item;

  if (item?.type) {
    const refName = createElementName(
      item.type,
      item.type === "category" ? item.name : item.functionData.name,
    );

    functionAndCategoryElementRefs[refName].scrollIntoView({
      behavior: "smooth",
      block: "nearest",
    });
  }
};

const toggleCategoryExpansion = (categoryName: string) => {
  categories.value[categoryName].expanded =
    !categories.value[categoryName].expanded;
  if (
    selectedEntry.value?.type === "function" &&
    selectedEntry.value.functionData.category === categoryName &&
    !categories.value[categoryName].expanded
  ) {
    selectedEntry.value = null;
  }
};
const isSelected = (item: SelectableItem): boolean => {
  if (item.type === "category" && selectedEntry.value?.type === "category") {
    return selectedEntry.value?.name === item.name;
  }
  if (item.type === "function" && selectedEntry.value?.type === "function") {
    return selectedEntry.value?.functionData.name === item.functionData.name;
  }
  return false;
};

const searchQuery = ref("");
const filteredFunctionCatalog = computed(() => {
  return filterCatalogData(searchQuery.value, categories.value, catalogData);
});

const isFilteredCatalogDataEmpty = computed(() => {
  return filteredFunctionCatalog.value.orderOfItems.length === 0;
});

const moveSelectionToTop = () => {
  selectEntry(filteredFunctionCatalog.value.orderOfItems[0]);
};
const moveSelectionToEnd = () => {
  selectEntry(
    filteredFunctionCatalog.value.orderOfItems[
      filteredFunctionCatalog.value.orderOfItems.length - 1
    ],
  );
};
const moveSelectionUp = () => {
  const itemToSelect =
    filteredFunctionCatalog.value.orderOfItems.findIndex(isSelected);
  if (itemToSelect === -1) {
    moveSelectionToEnd();
  } else {
    selectEntry(
      filteredFunctionCatalog.value.orderOfItems[
        (itemToSelect - 1 + filteredFunctionCatalog.value.orderOfItems.length) %
          filteredFunctionCatalog.value.orderOfItems.length
      ],
    );
  }
};
const moveSelectionDown = () => {
  const itemToSelect =
    filteredFunctionCatalog.value.orderOfItems.findIndex(isSelected);
  if (itemToSelect === -1) {
    moveSelectionToTop();
  } else {
    selectEntry(
      filteredFunctionCatalog.value.orderOfItems[
        (itemToSelect + 1) % filteredFunctionCatalog.value.orderOfItems.length
      ],
    );
  }
};
const onKeyDown = (e: KeyboardEvent) => {
  switch (e.key) {
    case "End":
      moveSelectionToEnd();
      break;
    case "Home":
      moveSelectionToTop();
      break;
    case "ArrowDown":
      moveSelectionDown();
      e.preventDefault();
      break;
    case "ArrowUp":
      moveSelectionUp();
      e.preventDefault();
      break;
    case " ":
    case "Enter":
      if (selectedEntry.value?.type === "category") {
        toggleCategoryExpansion(selectedEntry.value.name);
      } else {
        triggerFunctionInsertionEvent(
          e,
          (selectedEntry.value as SelectableFunction)?.functionData,
        );
      }

      e.preventDefault(); // Stop accidental scrolling
      break;
    case "ArrowRight":
      if (
        selectedEntry.value?.type === "category" &&
        !categories.value[selectedEntry.value.name].expanded
      ) {
        toggleCategoryExpansion(selectedEntry.value.name);
      }

      e.preventDefault(); // Stop accidental scrolling
      break;
    case "ArrowLeft":
      if (
        selectedEntry.value?.type === "category" &&
        categories.value[selectedEntry.value.name].expanded
      ) {
        toggleCategoryExpansion(selectedEntry.value.name);
      }

      e.preventDefault(); // Stop accidental scrolling
      break;
  }
};

const functionListRef = ref<HTMLDivElement | null>(null);
const grabFocus = () => {
  if (selectedEntry.value === null) {
    moveSelectionToTop();
  }

  // This means that the focus is specifically on the function list,
  // not on any of its children, which gives us the <tab> navigation
  // behaviour we want.
  functionListRef.value?.focus();
};

const expandAll = () => {
  if (searchQuery.value.trim().length !== 0) {
    for (const categoryName in categories.value) {
      categories.value[categoryName].expanded = true;
    }
  }
};
</script>

<template>
  <div
    ref="catalogRoot"
    class="function-catalog-container"
    :class="isSlimMode ? 'slim-mode' : ''"
  >
    <div
      class="function-catalog"
      :class="isSlimMode ? 'slim-mode' : ''"
      :style="{ '--function-catalog-width': FUNCTION_CATALOG_WIDTH }"
      tabindex="-1"
    >
      <div class="sticky-search">
        <div class="search-input">
          <SearchInput
            v-model="searchQuery"
            placeholder="Search the catalog"
            @update:model-value="
              () => {
                selectEntry(null);
                expandAll();
              }
            "
          />
        </div>
      </div>

      <!-- Make this panel not be a tab stop if filtered catalogue is empty -->
      <div
        ref="functionListRef"
        class="function-list"
        :tabindex="isFilteredCatalogDataEmpty ? -1 : 0"
        role="button"
        @keydown="onKeyDown"
        @focus="grabFocus"
        @click="grabFocus"
      >
        <div
          v-for="(
            categoryData, categoryName
          ) in filteredFunctionCatalog.filteredCatalogData"
          :key="categoryName"
          class="category-container"
        >
          <div
            v-if="categoryData.length > 0"
            :ref="createElementReference('category', categoryName)"
            class="category-header"
            role="button"
            :class="{
              expanded: categories[categoryName].expanded,
              selected: isSelected({
                type: 'category',
                name: categoryName,
              }),
              empty: categoryData.length === 0,
            }"
            tabindex="-1"
            @click="toggleCategoryExpansion(categoryName)"
            @focus="selectEntry({ type: 'category', name: categoryName })"
          >
            <span
              class="category-icon"
              :class="{
                expanded: categories[categoryName].expanded ? 'expanded' : '',
                'selected-icon': isSelected({
                  type: 'category',
                  name: categoryName,
                }),
                empty: categoryData.length === 0,
              }"
            >
              <NextIcon />
            </span>

            {{ categoryName }}
          </div>

          <div
            v-if="categories[categoryName].expanded"
            role="button"
            class="category-functions"
          >
            <div v-for="functionData in categoryData" :key="functionData.name">
              <div
                :ref="createElementReference('function', functionData.name)"
                class="function-header"
                :class="{
                  selected: isSelected({ type: 'function', functionData }),
                }"
                tabindex="-1"
                :draggable="true"
                @click="
                  selectEntry({
                    type: 'function',
                    functionData,
                  })
                "
                @dragstart="(event) => onDragStart(event, functionData)"
                @dragend="onDragEnd"
                @dblclick="
                  (event) => triggerFunctionInsertionEvent(event, functionData)
                "
              >
                {{
                  functionData.entryType === "function"
                    ? functionData.displayName || functionData.name
                    : functionData.name
                }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="info-panel" :class="isSlimMode ? 'slim-mode' : ''">
      <div v-if="selectedEntry !== null">
        <div v-if="selectedEntry.type === 'function'">
          <FunctionDescription :function-data="selectedEntry.functionData" />
        </div>
        <div v-else>
          <CategoryDescription :category="categories[selectedEntry.name]" />
        </div>
      </div>
      <div v-else>Select an entry to see its description.</div>
    </div>
  </div>
</template>

<style scoped>
@import url("webapps-common/ui/css/variables/spacings.css");

.function-catalog-container {
  display: flex;
  flex-direction: row;
  height: 100%;
}

.function-catalog-container.slim-mode {
  flex-direction: column;
}

.sticky-search {
  background-color: white;
  padding: var(--space-4);
  padding-bottom: var(--space-8);
}

.function-catalog {
  --function-catalog-width: 300px;

  width: var(--function-catalog-width);
  padding: var(--space-4);
  background-color: white;
  display: flex;
  flex-direction: column;
}

.function-catalog.slim-mode {
  width: 100%;
  height: 60%;
}

.function-list {
  overflow: hidden auto;
  padding-top: var(--space-4);
  flex: 1;
}

.function-list:focus {
  outline: none;
}

.info-panel {
  flex: 1;
  padding: var(--space-8);
  font-size: x-small;
  overflow-y: auto;
  border-left: 1px solid var(--knime-silver-sand);
}

.info-panel.slim-mode {
  border-left: none;
  border-top: 1px solid var(--knime-silver-sand);
  width: 100%;
  height: 40%;
}

.category-header {
  cursor: pointer;
  display: flex;
  align-items: center;
  font-weight: bold;
  font-size: 13px;
}

.category-header:focus {
  outline: none;
}

.category-icon {
  width: 16px;
  margin-right: var(--space-8);
  stroke: var(--knime-masala);
  transition: transform 0.3s ease;
  translate: 0 1px;
}

.category-icon svg {
  stroke-width: 2px;
}

.category-icon.selected-icon svg {
  stroke: white;
}

.function-list:not(:focus-within) .category-icon.selected-icon svg {
  stroke: var(--knime-masala);
}

.category-icon.expanded {
  transform: rotate(90deg);
}

.category-icon.empty {
  cursor: default;
  visibility: hidden;
}

.category-header.empty {
  color: var(--knime-silver-sand-semi);
  cursor: default;
}

.category-functions {
  margin-bottom: var(--space-8);
}

.function-header {
  font-size: 13px;
  font-weight: normal;
  margin-top: 2px;
  margin-left: var(--space-24);
  cursor: pointer;
  color: var(--knime-dove-gray);
  word-wrap: normal;
  box-decoration-break: clone;
}

.function-header:focus {
  outline: none;
}

.selected {
  position: relative;
  color: var(--theme-dropdown-foreground-color-selected);
  background-color: transparent;
  z-index: 0;
}

.selected::before {
  content: "";
  position: absolute;
  inset: -1px 100vw 0 -100vw;
  width: 300vw;
  height: 105%;
  background-color: var(--theme-dropdown-background-color-selected);
  z-index: -1;
}

.function-list:not(:focus-within) .selected::before {
  background: var(--theme-dropdown-background-color-hover);
}

.function-list:not(:focus-within) .selected {
  color: var(--theme-dropdown-foreground-color-hover);
}

.category-functions:not(.selected) > div:hover,
.category-header:not(.selected):hover {
  background: var(--theme-dropdown-background-color-hover);
  color: var(--theme-dropdown-foreground-color-hover);
  padding-left: 100px;
  margin-left: -100px;
  padding-right: 100px;
  margin-right: -100px;
}
</style>
