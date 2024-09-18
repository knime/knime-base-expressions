import type { GenericInitialData } from "@knime/scripting-editor";
import type { FunctionCatalogData } from "@/components/functionCatalogTypes";

export type OutputInsertionMode = "APPEND" | "REPLACE_EXISTING";

export type ExpressionVersion = {
  languageVersion: number;
  builtinFunctionsVersion: number;
  builtinAggregationsVersion: number;
};

export type ExpressionInitialData = GenericInitialData & {
  expressionVersion: ExpressionVersion;
  functionCatalog: FunctionCatalogData;
};
