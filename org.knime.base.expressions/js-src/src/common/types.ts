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

/** Identifies the error level of an ExpressionEditorPane */
export type ErrorLevel = "ERROR" | "WARNING" | "OK";

/** Error level with attached message */
export type EditorErrorState =
  | { level: "OK" }
  | { level: ErrorLevel; message: string };

export type ExpressionDiagnostic = {
  message: string;
  shortMessage: string;
  severity: "ERROR" | "WARNING" | "INFO" | "HINT";
  location: {
    start: number;
    stop: number;
  } | null;
};
