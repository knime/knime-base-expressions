import { createScriptingServiceMock } from "@knime/scripting-editor/scripting-service-browser-mock";

const scriptingService = createScriptingServiceMock({});

export const getScriptingServiceInstance = () => scriptingService;
export const setScriptingServiceInstance = () => {};
