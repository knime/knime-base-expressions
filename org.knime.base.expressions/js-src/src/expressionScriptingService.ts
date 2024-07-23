import { getScriptingService } from "@knime/scripting-editor";
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

const scriptingService = getScriptingService();

// TODO(AP-22975) initial data should go via the ScriptingNodeSettingsService
const expressionScriptingService = {
  ...scriptingService,
  registerSettingsGetterForApply: (
    settingsGetter: () => ExpressionNodeSettings,
    // @ts-ignore - incompatibility between NodeSettings and ExpressionNodeSettings
  ) => scriptingService.registerSettingsGetterForApply(settingsGetter),
  getInitialSettings: (): Promise<ExpressionNodeSettings> =>
    // @ts-ignore - incompatibility between NodeSettings and ExpressionNodeSettings
    scriptingService.getInitialSettings(),
  getFunctions: () =>
    scriptingService.sendToService(
      "getFunctionCatalog",
    ) as Promise<FunctionCatalogData>,
};

export type ExpressionScriptingServiceType = typeof expressionScriptingService;

export const getExpressionScriptingService =
  (): ExpressionScriptingServiceType => expressionScriptingService;
