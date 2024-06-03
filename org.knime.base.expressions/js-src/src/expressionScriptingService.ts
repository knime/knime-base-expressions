import {
  getScriptingService,
  type NodeSettings,
} from "@knime/scripting-editor";
import type { FunctionCatalogData } from "./components/functionCatalogTypes";

export type OutputInsertionMode = "APPEND" | "REPLACE_EXISTING";

export type ExpressionNodeSettings = NodeSettings & {
  columnOutputMode: OutputInsertionMode;
  createdColumn: string;
  replacedColumn: string;
};

export type MathsConstant = {
  name: string;
  value: number;
  type: string;
  documentation: string;
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
  getMathsConstants: () =>
    scriptingService.sendToService("getMathsConstants") as Promise<
      MathsConstant[]
    >,
};

export type ExpressionScriptingServiceType = typeof expressionScriptingService;

export const getExpressionScriptingService =
  (): ExpressionScriptingServiceType => expressionScriptingService;
