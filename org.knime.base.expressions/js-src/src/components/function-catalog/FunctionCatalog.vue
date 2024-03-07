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

const MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION = 450;
const MIN_WIDTH_FOR_CATALOG_PANEL = 300;

const props = defineProps<{
  functionCatalogData: FunctionCatalogData;
  initiallyExpanded: boolean;
}>();

const catalogData = mapFunctionCatalogData(props.functionCatalogData);
const categoryNames = Object.keys(catalogData);
const categories = ref(
  Object.fromEntries(
    categoryNames.map((category) => [
      category,
      { expanded: props.initiallyExpanded },
    ]),
  ),
);

const catalogRoot = ref();
const catalogRootRef = useElementBounding(catalogRoot);
const isSlimMode = computed(
  () => catalogRootRef.width.value <= MIN_WIDTH_FOR_DISPLAYING_DESCRIPTION,
);

const searchQuery = ref("");
const filteredFunctionCatalog = computed(() =>
  filterCatalogData(searchQuery.value, categories.value, catalogData),
);

const selectedEntry = ref<null | SelectableItem>(null);
const focusedEntry = ref<null | SelectableItem>(null);

const focusEntry = (item: SelectableItem | null) => {
  focusedEntry.value = item;
};

const toggleSelectionOfEntry = (item: SelectableItem) => {
  if (item.type === "category") {
    selectedEntry.value = item;
  } else if (item.type === "function") {
    selectedEntry.value =
      selectedEntry.value?.type === "function" &&
      item.functionData.name === selectedEntry.value?.functionData.name
        ? null
        : item;
  }
  focusEntry(item);
};

const toggleCategoryExpansion = (categoryName: string) => {
  categories.value[categoryName].expanded =
    !categories.value[categoryName].expanded;

  focusEntry({
    type: "category",
    name: categoryName,
  });

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

const isFocused = (item: SelectableItem): boolean => {
  if (item.type === "category" && focusedEntry.value?.type === "category") {
    return focusedEntry.value?.name === item.name;
  }

  if (item.type === "function" && focusedEntry.value?.type === "function") {
    return focusedEntry.value?.functionData.name === item.functionData.name;
  }

  return false;
};

const moveSelectionToTop = () => {
  focusEntry(filteredFunctionCatalog.value.orderOfItems[0]);
};

const moveSelectionToEnd = () => {
  focusEntry(
    filteredFunctionCatalog.value.orderOfItems[
      filteredFunctionCatalog.value.orderOfItems.length - 1
    ],
  );
};

const moveSelectionUp = () => {
  const itemToSelect =
    filteredFunctionCatalog.value.orderOfItems.findIndex(isFocused);

  if (itemToSelect === -1) {
    moveSelectionToEnd();
  } else {
    focusEntry(
      filteredFunctionCatalog.value.orderOfItems[
        (itemToSelect - 1 + filteredFunctionCatalog.value.orderOfItems.length) %
          filteredFunctionCatalog.value.orderOfItems.length
      ],
    );
  }
};

const moveSelectionDown = () => {
  const itemToSelect =
    filteredFunctionCatalog.value.orderOfItems.findIndex(isFocused);

  if (itemToSelect === -1) {
    moveSelectionToTop();
  } else {
    focusEntry(
      filteredFunctionCatalog.value.orderOfItems[
        (itemToSelect + 1) % filteredFunctionCatalog.value.orderOfItems.length
      ],
    );
  }
};

const onKeyPress = (e: KeyboardEvent) => {
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
      if (focusedEntry.value?.type === "category") {
        toggleCategoryExpansion(focusedEntry.value.name);
      } else if (focusedEntry.value?.type === "function") {
        toggleSelectionOfEntry(focusedEntry.value);
      }
      break;
  }
};

const grabFocus = () => {
  moveSelectionToTop();
};

const handleBlur = () => {
  focusEntry(null);
};
</script>

<template>
  <div ref="catalogRoot" class="function-catalog-container" @blur="handleBlur">
    <div
      class="function-catalog"
      :class="isSlimMode ? 'slim-mode' : ''"
      :style="{ '--catalog-min-width': MIN_WIDTH_FOR_CATALOG_PANEL }"
    >
      <div class="sticky-search">
        <div class="search-input">
          <SearchInput
            v-model="searchQuery"
            placeholder="Search the catalog"
            tabindex="0"
          />
        </div>
      </div>

      <div class="function-list" @keydown="onKeyPress" @focus="grabFocus">
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
            tabindex="0"
            role="button"
            :class="{
              expanded: categories[categoryName].expanded,
              'focused-item': isFocused({
                type: 'category',
                name: categoryName,
              }),
              empty: categoryData.length === 0,
            }"
            @click="toggleCategoryExpansion(categoryName)"
          >
            <span
              class="category-icon"
              :class="{
                expanded: categories[categoryName].expanded ? 'expanded' : '',
                empty: categoryData.length === 0,
              }"
            >
              <NextIcon />
            </span>

            {{ categoryName }}
          </div>

          <div
            v-if="categories[categoryName].expanded"
            class="category-functions"
          >
            <div v-for="functionData in categoryData" :key="functionData.name">
              <span
                class="function-header"
                :class="{
                  'focused-item':
                    isFocused({ type: 'function', functionData }) &&
                    !isSelected({ type: 'function', functionData }),
                  selected: isSelected({ type: 'function', functionData }),
                }"
                @click="
                  toggleSelectionOfEntry({
                    type: 'function',
                    functionData,
                  })
                "
              >
                {{ functionData.displayName || functionData.name }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="info-panel" :class="isSlimMode ? 'slim-mode' : ''">
      <div v-if="selectedEntry !== null && selectedEntry.type === 'function'">
        <FunctionDescription :function-data="selectedEntry.functionData" />
      </div>
      <div v-else>Select a function to see its description.</div>
    </div>
  </div>
</template>

<style scoped>
.sticky-search {
  position: sticky;
  top: 0;
  z-index: 999;
  background-color: white;
  padding-bottom: 10px;
}

.function-catalog-container {
  display: flex;
  flex-direction: row;
  height: 100%;
}

.function-catalog {
  --catalog-min-width: 300px;

  flex: 0 0 var(--catalog-min-width) px;
  padding: 10px;
  overflow: hidden auto;
  background-color: white;
}

.function-catalog.slim-mode {
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
  transition: transform 0.3s ease;
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
  overflow: hidden;
  word-wrap: anywhere;
  color: var(--knime-dove-gray);
  box-decoration-break: clone;
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
  inset: 0 100vw 0 -100vw;
  width: 300vw;
  background-color: var(--theme-dropdown-background-color-selected);
  z-index: -1;
}

.focused-item {
  position: relative;
  color: var(--theme-dropdown-foreground-color-hover);
  background-color: transparent;
  z-index: 0;
}

.focused-item::before {
  content: "";
  position: absolute;
  inset: 0 100vw 0 -100vw;
  width: 300vw;
  height: 100%;
  background: var(--theme-dropdown-background-color-hover);
  z-index: -1;
}

.category-functions > div:hover,
.category-header:hover {
  background: var(--theme-dropdown-background-color-hover);
  color: var(--theme-dropdown-foreground-color-hover);
  padding-left: 100px;
  margin-left: -100px;
  padding-right: 100px;
  margin-right: -100px;
}
</style>
