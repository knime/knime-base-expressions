import { afterEach, describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";

import { getScriptingService } from "@knime/scripting-editor";

import OutputPreviewFlowVariable from "../OutputPreviewFlowVariable.vue";

const mocks = vi.hoisted(() => {
  const dummyPath = "something/something/";
  return {
    dummyPath,
    config: {
      nodeId: "nodeId",
      projectId: "projectId",
      workflowId: "workflowId",
      resourceInfo: {
        baseUrl: "http://localhost/",
        path: `${dummyPath}someFile.html`,
      },
    },
    initialData: {
      initialData: "initialDataString",
    },
    getScriptingService: {
      registerEventHandler: vi.fn(),
    },
  };
});

vi.mock("@knime/scripting-editor", () => {
  return {
    getScriptingService: vi.fn(() => mocks.getScriptingService),
  };
});

vi.mock("@knime/ui-extension-service", async (importOriginal) => {
  const original =
    await importOriginal<typeof import("@knime/ui-extension-service")>();
  return {
    ...original,
    JsonDataService: {
      getInstance: vi.fn().mockResolvedValue({
        baseService: {
          getConfig: vi.fn().mockResolvedValue(mocks.config),
          callNodeDataService: vi.fn().mockResolvedValue({}),
          getResourceLocation: vi.fn(() => mocks.dummyPath),
        },
        data: vi.fn().mockResolvedValue(JSON.stringify(mocks.initialData)),
      }),
    },
    AlertingService: {
      getInstance: vi.fn().mockResolvedValue({
        sendAlert: vi.fn(),
      }),
    },
  };
});

describe("OutputPreviewFlowVariable", () => {
  const propsForOutputPreviewFlowVariable = [
    "apiLayer",
    "extensionConfig",
    "resourceLocation",
    "shadowAppStyle",
  ];

  const doMount = async () => {
    const wrapper = mount(OutputPreviewFlowVariable, {
      global: {
        stubs: {
          UIExtension: {
            template: `<div class="ui-extension-stub" >
              {{ $props }}
            </div>`,
            props: propsForOutputPreviewFlowVariable,
          },
        },
      },
    });
    await flushPromises();
    return { wrapper };
  };

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("create output preview", async () => {
    const { wrapper } = await doMount();
    const preEvaluationSign = wrapper.find(".pre-evaluation-sign");

    expect(preEvaluationSign.exists()).toBe(true);
    expect(preEvaluationSign.text()).toContain("To see the preview");
  });

  it("updates state when updatePreview event is triggered", async () => {
    const { wrapper } = await doMount();

    expect(getScriptingService().registerEventHandler).toHaveBeenCalledOnce();

    const callbackUpdatePreview = vi.mocked(
      getScriptingService().registerEventHandler,
    ).mock.calls[0][1];

    callbackUpdatePreview(null);

    await flushPromises();

    const previewWarningText = wrapper.find(".preview-warning-text");
    expect(previewWarningText.exists()).toBe(true);
    expect(previewWarningText.text()).toContain("Preview");
  });

  it("calls UIExtension with correct arguments", async () => {
    const { wrapper } = await doMount();

    expect(getScriptingService().registerEventHandler).toHaveBeenCalledOnce();

    const callbackUpdatePreview = vi.mocked(
      getScriptingService().registerEventHandler,
    ).mock.calls[0][1];

    callbackUpdatePreview(null);

    await flushPromises();

    const uiExtensionStub = wrapper.find(".ui-extension-stub");
    expect(uiExtensionStub.exists()).toBe(true);
    const renderedUiExtensionStubContent = uiExtensionStub.text();

    propsForOutputPreviewFlowVariable.forEach((prop) => {
      expect(renderedUiExtensionStubContent).toContain(prop);
    });

    expect(renderedUiExtensionStubContent).toContain(mocks.dummyPath);
    expect(renderedUiExtensionStubContent).toContain(mocks.config.projectId);
    expect(renderedUiExtensionStubContent).toContain(
      mocks.config.resourceInfo.baseUrl,
    );
  });

  it("shows no data placeholder if update called with text", async () => {
    const { wrapper } = await doMount();

    expect(getScriptingService().registerEventHandler).toHaveBeenCalledOnce();

    const callbackUpdatePreview = vi.mocked(
      getScriptingService().registerEventHandler,
    ).mock.calls[0][1];

    callbackUpdatePreview("No data to display");

    await flushPromises();

    const noDataPlaceholder = wrapper.find(
      "[data-testid='no-data-placeholder']",
    );
    expect(noDataPlaceholder.exists()).toBe(true);
    expect(noDataPlaceholder.text()).toContain("No data to display");
  });
});
