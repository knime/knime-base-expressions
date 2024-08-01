import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
import { createInitialDataServiceMock } from "@knime/scripting-editor/initial-data-service-browser-mock";
import { createSettingsServiceMock } from "@knime/scripting-editor/settings-service-browser-mock";
import {
  getScriptingService,
  getInitialDataService,
  getSettingsService,
} from "@knime/scripting-editor";
import { DEFAULT_INITIAL_DATA, DEFAULT_INITIAL_SETTINGS } from "./mock-data";

const log = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.log(message, ...args);
  } else {
    consola.log(message, ...args);
  }
};

if (import.meta.env.MODE === "development.browser") {
  const scriptingService = createScriptingServiceMock({
    sendToServiceMockResponses: {
      runExpression: (options: any[] | undefined) => {
        log("runExpression", options);
        return Promise.resolve();
      },
      getDiagnostics: () => Promise.resolve([]),
    },
  });

  Object.assign(getScriptingService(), scriptingService);

  Object.assign(
    getInitialDataService(),
    createInitialDataServiceMock(DEFAULT_INITIAL_DATA),
  );

  Object.assign(
    getSettingsService(),
    createSettingsServiceMock(DEFAULT_INITIAL_SETTINGS),
  );
}
