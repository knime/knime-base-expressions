import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
import { createInitialDataServiceMock } from "@knime/scripting-editor/initial-data-service-browser-mock";
import { createSettingsServiceMock } from "@knime/scripting-editor/settings-service-browser-mock";
import {
  getInitialDataService,
  getScriptingService,
  getSettingsService,
} from "@knime/scripting-editor";
import { DEFAULT_INITIAL_DATA, INPUT_OBJECTS } from "@/__mocks__/mock-data";
import type { ExpressionRowMapperNodeSettings } from "@/expressionSettingsService";
import { log } from "@/common/functions";

export const DEFAULT_ROW_MAPPER_INITIAL_SETTINGS: ExpressionRowMapperNodeSettings =
  {
    scripts: ["mocked default script"],
    outputModes: ["APPEND"],
    createdColumns: ["mocked output col"],
    replacedColumns: [INPUT_OBJECTS[0].subItems![1].name],
    languageVersion: 1,
    builtinFunctionsVersion: 1,
    builtinAggregationsVersion: 1,
  };

if (import.meta.env.MODE === "development.browser") {
  const scriptingService = createScriptingServiceMock({
    sendToServiceMockResponses: {
      runRowMapperExpression: (options: any[] | undefined) => {
        log("runRowFilterExpression", options);
        return Promise.resolve();
      },
      getRowMapperDiagnostics: () => Promise.resolve([]),
    },
  });

  Object.assign(getScriptingService(), scriptingService);

  Object.assign(
    getInitialDataService(),
    createInitialDataServiceMock(DEFAULT_INITIAL_DATA),
  );

  Object.assign(
    getSettingsService(),
    createSettingsServiceMock(DEFAULT_ROW_MAPPER_INITIAL_SETTINGS),
  );
}
