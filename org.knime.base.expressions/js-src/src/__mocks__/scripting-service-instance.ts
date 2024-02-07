import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";

const scriptingService = createScriptingServiceMock({
  sendToServiceMockResponses: {
    runExpression: (options) => {
      consola.log("runExpression", options);
      return Promise.resolve();
    },
  },
});

export const getScriptingServiceInstance = () => scriptingService;
export const setScriptingServiceInstance = () => {};
