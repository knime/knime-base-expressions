import {
  getScriptingService,
  type NodeSettings,
} from "@knime/scripting-editor";
import type { FunctionCatalogData } from "./components/functionCatalogTypes";

export type OutputInsertionMode = "APPEND" | "REPLACE_EXISTING";

export type ExpressionVersion = {
  languageVersion: number;
  builtinFunctionsVersion: number;
  builtinAggregationsVersion: number;
};

export type ColumnSettings = {
  columnOutputMode: OutputInsertionMode;
  createdColumn: string;
  replacedColumn: string;
};

export type ExpressionNodeSettings = NodeSettings &
  ExpressionVersion &
  ColumnSettings & {
    additionalScripts: string[];
  };

const scriptingService = getScriptingService();
const expressionScriptingService = {
  ...scriptingService,
  registerSettingsGetterForApply: (
    settingsGetter: () => ExpressionNodeSettings,
  ) => scriptingService.registerSettingsGetterForApply(settingsGetter),
  getInitialSettings: (): Promise<ExpressionNodeSettings> =>
    scriptingService.getInitialSettings() as Promise<ExpressionNodeSettings>,
  getFunctions: () =>
    scriptingService.sendToService(
      "getFunctionCatalog",
    ) as Promise<FunctionCatalogData>,
};

export type ExpressionScriptingServiceType = typeof expressionScriptingService;

export const getExpressionScriptingService =
  (): ExpressionScriptingServiceType => expressionScriptingService;
