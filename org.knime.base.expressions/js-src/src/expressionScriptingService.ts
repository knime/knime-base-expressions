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
  ColumnSettings;

export type MathConstant = {
  name: string;
  value: number;
  type: string;
  documentation: string;
};

export type MathConstantData = {
  constants: MathConstant[];
  category: { name: string; description: string };
};

const scriptingService = getScriptingService();
const expressionScriptingService = {
  ...scriptingService,
  registerSettingsGetterForApply: (
    settingsGetter: () => ExpressionNodeSettings,
  ) => scriptingService.registerSettingsGetterForApply(settingsGetter),
  getInitialSettings: () =>
    scriptingService.getInitialSettings() as Promise<ExpressionNodeSettings>,
  getFunctions: () =>
    scriptingService.sendToService(
      "getFunctionCatalog",
    ) as Promise<FunctionCatalogData>,
  getMathConstants: () =>
    scriptingService.sendToService(
      "getMathConstants",
    ) as Promise<MathConstantData>,
};

export type ExpressionScriptingServiceType = typeof expressionScriptingService;

export const getExpressionScriptingService =
  (): ExpressionScriptingServiceType => expressionScriptingService;
