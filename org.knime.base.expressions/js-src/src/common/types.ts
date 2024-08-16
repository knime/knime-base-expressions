import type {
  InputOutputModel,
  KAIConfig,
  PortConfigs,
} from "@knime/scripting-editor";
import type { FunctionCatalogData } from "@/components/functionCatalogTypes";

export type OutputInsertionMode = "APPEND" | "REPLACE_EXISTING";

export type ExpressionVersion = {
  languageVersion: number;
  builtinFunctionsVersion: number;
  builtinAggregationsVersion: number;
};

export type ExpressionInitialData = {
  functionCatalog: FunctionCatalogData;
  inputObjects: InputOutputModel[];
  flowVariables: InputOutputModel;
  inputsAvailable: boolean;
  kAiConfig: KAIConfig;
  inputPortConfigs: PortConfigs;
};

/** Identifies the error state of an ExpressionEditorPane */
export type ErrorLevel = "ERROR" | "WARNING" | "OK";

export type Diagnostic = {
  message: string;
  severity: "ERROR" | "WARNING" | "INFO" | "HINT";
  location: {
    start: number;
    stop: number;
  };
};
