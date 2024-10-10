import type { GenericInitialData, SubItem } from "@knime/scripting-editor";
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

export type ExpressionRowMapperInitialData = ExpressionInitialData & {
  rowInformation: SubItem<{}>[];
};

export type ExpressionRowFilterInitialData = ExpressionInitialData & {
  rowInformation: SubItem<{}>[];
};

export type ExpressionFlowVariableInitialData = ExpressionInitialData; // NOSONAR
