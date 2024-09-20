import type { SelectorState } from "@/components/OutputSelector.vue";
import type { InputOutputModel, SubItem } from "@knime/scripting-editor";
import type { TypeRendererProps } from "@/components/TypeRenderer.vue";
import { shallowRef } from "vue";
import TypeRenderer from "@/components/TypeRenderer.vue";

export type SubItemState = {
  selectorState: SelectorState;
  key: string;
  returnType: string;
};

export const replaceSubItems = (
  subItem: SubItem<TypeRendererProps>,
  states: SubItemState[],
  actionBuilder: (editorKey: string) => () => void,
): SubItem<TypeRendererProps> => {
  const lastReplacement = states.findLast(
    (state) =>
      state.selectorState.outputMode === "REPLACE_EXISTING" &&
      state.selectorState.replace === subItem.name,
  );

  if (lastReplacement) {
    return {
      name: subItem.name,
      type: {
        component: shallowRef(TypeRenderer),
        props: {
          type: lastReplacement.returnType,
          action: actionBuilder(lastReplacement.key),
          isReplacement: lastReplacement.returnType !== subItem.type,
        },
      },
      supported: true,
    };
  }

  return subItem;
};

export const buildAppendedOutput = (
  states: SubItemState[],
  actionBuilder: (editorKey: string) => () => void,
  metaData: Pick<
    InputOutputModel,
    "name" | "portType" | "subItemCodeAliasTemplate"
  >,
): InputOutputModel => ({
  name: metaData.name,
  portType: metaData.portType,
  subItemCodeAliasTemplate: metaData.subItemCodeAliasTemplate,
  subItems: states
    .map((state): SubItem<TypeRendererProps> | null =>
      state.selectorState.outputMode === "APPEND"
        ? {
            name: state.selectorState.create,
            type: state.returnType,
            supported: true,
          }
        : null,
    )
    .filter((item) => item !== null)
    .map((subItem) => replaceSubItems(subItem, states, actionBuilder)),
});
