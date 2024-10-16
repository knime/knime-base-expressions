import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
import { createInitialDataServiceMock } from "@knime/scripting-editor/initial-data-service-browser-mock";
import { createSettingsServiceMock } from "@knime/scripting-editor/settings-service-browser-mock";
import {
  getInitialDataService,
  getScriptingService,
  getSettingsService,
} from "@knime/scripting-editor";
import { ROW_FILTER_INITIAL_DATA } from "@/__mocks__/mock-data";
import type { ExpressionRowFilterNodeSettings } from "@/expressionSettingsService";
import { log } from "@/common/functions";

export const DEFAULT_ROW_FILTER_INITIAL_SETTINGS: ExpressionRowFilterNodeSettings =
  {
    languageVersion: 1,
    builtinFunctionsVersion: 1,
    builtinAggregationsVersion: 1,
    script: "mocked default script",
  };

if (import.meta.env.MODE === "development.browser") {
  const scriptingService = createScriptingServiceMock({
    sendToServiceMockResponses: {
      runRowMapperExpression: (options: any[] | undefined) => {
        log("runRowMapperExpression", options);
        return Promise.resolve();
      },
      getRowFilterDiagnostics: () => Promise.resolve([]),
    },
  });

  Object.assign(getScriptingService(), scriptingService);

  Object.assign(
    getInitialDataService(),
    createInitialDataServiceMock(ROW_FILTER_INITIAL_DATA),
  );

  Object.assign(
    getSettingsService(),
    createSettingsServiceMock(DEFAULT_ROW_FILTER_INITIAL_SETTINGS),
  );
}
