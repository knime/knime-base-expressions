import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import App from "../App.vue";
import registerKnimeExpressionLanguage from "@/registerKnimeExpressionLanguage";
import { nextTick } from "vue";
import {
  DEFAULT_INITIAL_DATA,
  DEFAULT_INITIAL_SETTINGS,
} from "@/__mocks__/mock-data";

vi.mock("@/registerKnimeExpressionLanguage", () => ({
  default: vi.fn(() => vi.fn()),
}));

const mockedScriptingService = vi.hoisted(() => ({
  sendToService: vi.fn(),
  getInitialData: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_DATA)),
  registerSettingsGetterForApply: vi.fn(),
}));

vi.mock("@knime/scripting-editor", async () => ({
  ...(await vi.importActual("@knime/scripting-editor")),
  getScriptingService: vi.fn(() => mockedScriptingService),
}));

vi.mock("@/expressionInitialDataService", () => ({
  getExpressionInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_DATA)),
  })),
}));

vi.mock("@/expressionSettingsService", () => ({
  getExpressionSettingsService: vi.fn(() => ({
    getSettings: vi.fn(() => Promise.resolve(DEFAULT_INITIAL_SETTINGS)),
    registerSettingsGetterForApply: vi.fn(),
  })),
}));

vi.mock("@/expressionDiagnostics", () => ({
  runDiagnostics: vi.fn(() => Promise.resolve([[]])),
  runColumnOutputDiagnostics: vi.fn(() => Promise.resolve([])),
}));

describe("App.vue", () => {
  const doMount = () => {
    const wrapper = mount(App, {
      global: {
        stubs: {
          TabBar: true,
          OutputConsole: true,
        },
      },
    });
    return { wrapper };
  };

  beforeEach(() => {
    vi.resetModules();
  });

  it("renders the ScriptingEditor component with the correct language", async () => {
    const { wrapper } = doMount();

    await flushPromises();
    await nextTick();

    const scriptingComponent = wrapper.findComponent({
      name: "ScriptingEditor",
    });
    expect(scriptingComponent.exists()).toBeTruthy();
    expect(scriptingComponent.props("language")).toBe("knime-expression");
  });

  it("registers the knime expression language", async () => {
    doMount();

    await flushPromises();

    expect(registerKnimeExpressionLanguage).toHaveBeenCalled();
  });

  it("renders function catalog with data from service", async () => {
    const { wrapper } = doMount();

    // Resolve the promise - the function catalog should now be rendered
    await flushPromises();

    const functionCatalog = wrapper.findComponent({ name: "FunctionCatalog" });
    expect(functionCatalog.exists()).toBeTruthy();
    expect(functionCatalog.props("functionCatalogData")).toEqual(
      DEFAULT_INITIAL_DATA.functionCatalog,
    );
  });

  it("renders column selector and check that it is updated when promises are resolved", async () => {
    const { wrapper } = doMount();

    await flushPromises();
    await nextTick();

    const columnSelector = wrapper.findComponent({
      name: "ColumnOutputSelector",
    });

    expect(columnSelector.exists()).toBeTruthy();

    await flushPromises();
    await nextTick();

    expect(columnSelector.props("modelValue")).toEqual({
      createColumn: DEFAULT_INITIAL_SETTINGS.createdColumns[0],
      outputMode: "APPEND",
      replaceColumn: DEFAULT_INITIAL_SETTINGS.replacedColumns[0],
    });
  });
});
