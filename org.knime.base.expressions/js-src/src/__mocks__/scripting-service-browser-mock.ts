import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";
import { getScriptingService } from "@knime/scripting-editor";

if (import.meta.env.MODE === "development.browser") {
  const scriptingService = createScriptingServiceMock({
    sendToServiceMockResponses: {
      runExpression: (options) => {
        consola.log("runExpression", options);
        return Promise.resolve();
      },
    },
  });

  Object.assign(getScriptingService(), scriptingService);
}
