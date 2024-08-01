import type {
  InputOutputModel,
  KAIConfig,
  PortConfigs,
} from "@knime/scripting-editor";
import type { FunctionCatalogData } from "./components/functionCatalogTypes";

export type OutputInsertionMode = "APPEND" | "REPLACE_EXISTING";

export type ExpressionVersion = {
  languageVersion: number;
  builtinFunctionsVersion: number;
  builtinAggregationsVersion: number;
};

export type ColumnSettings = {
  outputModes: OutputInsertionMode[];
  createdColumns: string[];
  replacedColumns: string[];
};

export type ExpressionNodeSettings = ExpressionVersion &
  ColumnSettings & { scripts: string[] } & {
    scriptUsedFlowVariable?: string | null;
  };

export type ExpressionInitialData = {
  settings: ExpressionNodeSettings;
  functionCatalog: FunctionCatalogData;
  inputObjects: InputOutputModel[];
  flowVariables: InputOutputModel;
  inputsAvailable: boolean;
  kAiConfig: KAIConfig;
  inputPortConfigs: PortConfigs;
};
