import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
import { createInitialDataServiceMock } from "@knime/scripting-editor/initial-data-service-browser-mock";
import { createSettingsServiceMock } from "@knime/scripting-editor/settings-service-browser-mock";
import {
  getInitialDataService,
  getScriptingService,
  getSettingsService,
} from "@knime/scripting-editor";
import { DEFAULT_INITIAL_DATA, FLOW_VARIABLES } from "@/__mocks__/mock-data";
import type { ExpressionFlowVariableNodeSettings } from "@/expressionSettingsService";
import { log } from "@/common/functions";

export const DEFAULT_FLOW_VARIABLE_INITIAL_SETTINGS: ExpressionFlowVariableNodeSettings =
  {
    scripts: ["mocked default script"],
    flowVariableOutputModes: ["APPEND"],
    createdFlowVariables: ["mocked output col"],
    replacedFlowVariables: [FLOW_VARIABLES.subItems![1].name],
    languageVersion: 1,
    builtinFunctionsVersion: 1,
    builtinAggregationsVersion: 1,
  };

if (import.meta.env.MODE === "development.browser") {
  const scriptingService = createScriptingServiceMock({
    sendToServiceMockResponses: {
      runFlowVariableExpression: (options: any[] | undefined) => {
        log("runFlowVariableExpression", options);
        return Promise.resolve();
      },
      getFlowVariableDiagnostics: () => Promise.resolve([]),
    },
  });

  Object.assign(getScriptingService(), scriptingService);

  Object.assign(
    getInitialDataService(),
    createInitialDataServiceMock(DEFAULT_INITIAL_DATA),
  );

  Object.assign(
    getSettingsService(),
    createSettingsServiceMock(DEFAULT_FLOW_VARIABLE_INITIAL_SETTINGS),
  );
}