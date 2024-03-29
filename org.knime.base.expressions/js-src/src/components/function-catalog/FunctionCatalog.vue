<script setup lang="ts">
import { computed, ref } from "vue";
import SearchInput from "../../../webapps-common/ui/components/forms/SearchInput.vue";
import NextIcon from "../../../webapps-common/ui/assets/img/icons/arrow-next.svg";

import { useElementBounding } from "@vueuse/core";
import { mapFunctionCatalogData } from "@/components/function-catalog/mapFunctionCatalogData";
import FunctionDescription from "@/components/function-catalog/FunctionDescription.vue";
import type { FunctionCatalogData } from "@/components/functionCatalogTypes";
import { filterCatalogData } from "@/components/function-catalog/filterCatalogData";
import type { SelectableItem } from "@/components/function-catalog/catalogTypes";
import CategoryDescription from "@/components/function-catalog/CategoryDescription.vue";

const MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION = 450;
const FUNCTION_CATALOG_WIDTH = "250px";

const props = defineProps<{
  functionCatalogData: FunctionCatalogData;
  initiallyExpanded: boolean;
}>();

const catalogData = mapFunctionCatalogData(props.functionCatalogData);
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

const catalogRoot = ref();
const catalogRootRef = useElementBounding(catalogRoot);
const isSlimMode = computed(
  () => catalogRootRef.width.value <= MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION,
);

const selectedEntry = ref<null | SelectableItem>(null);

const selectEntry = (item: SelectableItem | null) => {
  selectedEntry.value = item;
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
      break;
    case "ArrowUp":
      moveSelectionUp();
      break;
    case " ":
    case "Enter":
      if (selectedEntry.value?.type === "category") {
        toggleCategoryExpansion(selectedEntry.value.name);
      }
      break;
    case "ArrowRight":
      if (
        selectedEntry.value?.type === "category" &&
        !categories.value[selectedEntry.value.name].expanded
      ) {
        toggleCategoryExpansion(selectedEntry.value.name);
      }
      break;
    case "ArrowLeft":
      if (
        selectedEntry.value?.type === "category" &&
        categories.value[selectedEntry.value.name].expanded
      ) {
        toggleCategoryExpansion(selectedEntry.value.name);
      }
      break;
  }
};
const grabFocus = () => {
  if (selectedEntry.value === null) {
    moveSelectionToTop();
  }
};
</script>

<template>
  <div ref="catalogRoot" class="function-catalog-container">
    <div
      class="function-catalog"
      :class="isSlimMode ? 'slim-mode' : ''"
      :style="{ '--function-catalog-width': FUNCTION_CATALOG_WIDTH }"
    >
      <div class="sticky-search">
        <div class="search-input">
          <SearchInput
            v-model="searchQuery"
            placeholder="Search the catalog"
            @update:model-value="selectEntry(null)"
          />
        </div>
      </div>

      <div
        class="function-list"
        tabindex="0"
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
                class="function-header"
                :class="{
                  selected: isSelected({ type: 'function', functionData }),
                }"
                tabindex="-1"
                @click="
                  selectEntry({
                    type: 'function',
                    functionData,
                  })
                "
              >
                {{ functionData.displayName || functionData.name }}
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
.function-catalog-container {
  display: flex;
  flex-direction: row;
  height: 100%;
}

.sticky-search {
  position: sticky;
  top: 0;
  z-index: 999;
  background-color: white;
  padding-bottom: 10px;
}

.function-catalog {
  --function-catalog-width: 250px;

  width: var(--function-catalog-width);
  padding: 10px;
  overflow: hidden auto;
  background-color: white;
  display: flex;
  flex-direction: column;
}

.function-catalog.slim-mode {
  flex: 1;
}

.function-list {
  flex: 1;
}

.function-list:focus {
  outline: none;
}

.info-panel {
  flex: 1;
  padding: 10px;
  font-size: x-small;
  overflow-y: auto;
  border-left: 1px solid var(--knime-masala);
}

.info-panel.slim-mode {
  display: none;
}

.category-header {
  cursor: pointer;
  display: flex;
  align-items: center;
  font-size: small;
  font-weight: bold;
  margin-top: 5px;
}

.category-header:focus {
  outline: none;
}

.category-icon {
  width: 13px;
  margin-right: 7px;
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
  margin-bottom: 5px;
}

.function-header {
  font-size: small;
  font-weight: normal;
  margin-left: 20px;
  cursor: pointer;
  color: var(--knime-dove-gray);
  word-wrap: anywhere;
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
