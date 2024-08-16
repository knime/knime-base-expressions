<script setup lang="ts">
import { DEFAULT_NUMBER_OF_ROWS_TO_RUN } from "@/common/constants";
import { Button, SplitButton, SubMenu, Tooltip } from "@knime/components";
import PlayIcon from "@knime/styles/img/icons/play.svg";
import DropdownIcon from "@knime/styles/img/icons/arrow-dropdown.svg";

interface PropType {
  runButtonDisabledErrorReason: string | null;
  showButtonText: boolean;
}

const emit = defineEmits<(event: "run-expressions", rows: number) => void>();

const handleRunExpressions = (rows: number) => {
  emit("run-expressions", rows);
};

const props = defineProps<PropType>();

const buttonText = props.showButtonText
  ? `Evaluate first ${DEFAULT_NUMBER_OF_ROWS_TO_RUN} rows`
  : "";
</script>

<template>
  <Tooltip class="tooltip-word-wrap" :text="runButtonDisabledErrorReason ?? ''">
    <SplitButton>
      <Button
        primary
        compact
        :disabled="runButtonDisabledErrorReason !== null"
        @click="handleRunExpressions(DEFAULT_NUMBER_OF_ROWS_TO_RUN)"
      >
        <div
          class="run-button"
          :class="{
            'hide-button-text': !showButtonText,
          }"
        >
          <PlayIcon />
        </div>
        {{ buttonText }}
      </Button>

      <SubMenu
        :items="[
          { text: 'Evaluate first 100 rows', metadata: 100 },
          { text: 'Evaluate first 1000 rows', metadata: 1000 },
        ]"
        button-title="Run more rows"
        orientation="top"
        :disabled="runButtonDisabledErrorReason !== null"
        @item-click="
          (_evt: any, item: any) => handleRunExpressions(item.metadata)
        "
      >
        <DropdownIcon />
      </SubMenu>
    </SplitButton>
  </Tooltip>
</template>

<style lang="postcss" scoped>
.hide-button-text {
  margin-right: calc(-1 * var(--space-16));
}

.run-button {
  display: inline;
}

.tooltip-word-wrap .text {
  white-space: normal;
  overflow: visible;
  width: auto;
  height: auto;
  z-index: 9999;
}

.submenu {
  background-color: var(--knime-yellow);

  &:focus-within,
  &:hover {
    background-color: var(--knime-masala);

    & .submenu-toggle svg {
      stroke: var(--knime-white);
    }
  }
}
</style>
