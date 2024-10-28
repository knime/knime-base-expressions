import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { flushPromises, mount } from "@vue/test-utils";

import { ROW_FILTER_INITIAL_DATA } from "@/__mocks__/mock-data";
import type { EditorErrorState } from "@/generalDiagnostics";
import registerKnimeExpressionLanguage from "@/registerKnimeExpressionLanguage";
import ExpressionRowFilterApp from "../ExpressionRowFilterApp.vue";
import { DEFAULT_ROW_FILTER_INITIAL_SETTINGS } from "../__mocks__/browser-mock-row-filter-services";

vi.mock("@/registerKnimeExpressionLanguage", () => ({
  default: vi.fn(() => vi.fn()),
}));

const mockedScriptingService = vi.hoisted(() => ({
  sendToService: vi.fn(),
  getInitialData: vi.fn(() => Promise.resolve(ROW_FILTER_INITIAL_DATA)),
  registerSettingsGetterForApply: vi.fn(),
  registerEventHandler: vi.fn(),
}));

vi.mock("@knime/scripting-editor", async () => ({
  ...(await vi.importActual("@knime/scripting-editor")),
  getScriptingService: vi.fn(() => mockedScriptingService),
}));

vi.mock("@/expressionInitialDataService", () => ({
  getRowFilterInitialDataService: vi.fn(() => ({
    getInitialData: vi.fn(() => Promise.resolve(ROW_FILTER_INITIAL_DATA)),
  })),
}));

vi.mock("@/expressionSettingsService", () => ({
  getRowFilterSettingsService: vi.fn(() => ({
    getSettings: vi.fn(() =>
      Promise.resolve(DEFAULT_ROW_FILTER_INITIAL_SETTINGS),
    ),
    registerSettingsGetterForApply: vi.fn(),
  })),
}));

vi.mock("@/rowFilterApp/expressionRowFilterDiagnostics", () => ({
  runRowFilterDiagnostics: vi.fn(() =>
    Promise.resolve({ level: "OK" } satisfies EditorErrorState),
  ),
}));

describe("ExpressionsRowFilterApp.vue", () => {
  const doMount = () => {
    const wrapper = mount(ExpressionRowFilterApp, {
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
      ROW_FILTER_INITIAL_DATA.functionCatalog,
    );
  });
});
