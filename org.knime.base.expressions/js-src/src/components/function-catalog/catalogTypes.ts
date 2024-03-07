import type { FunctionData } from "@/components/functionCatalogTypes";

export type SelectableCategory = {
  type: "category";
  name: string;
};

export type SelectableFunction = {
  type: "function";
  functionData: FunctionData;
};

export type SelectableItem = SelectableCategory | SelectableFunction;
