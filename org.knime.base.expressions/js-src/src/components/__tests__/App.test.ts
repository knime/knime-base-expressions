import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "../App.vue";
import { ScriptingEditor, getScriptingService } from "@knime/scripting-editor";

describe("App.vue", () => {
  enableAutoUnmount(afterEach);

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

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the ScriptingEditor component with the correct language", () => {
    const { wrapper } = doMount();
    const scriptingComponent = wrapper.findComponent({
      name: "ScriptingEditor",
    });
    expect(scriptingComponent.exists()).toBeTruthy();
    expect(scriptingComponent.props("language")).toBe("knime-expression");
    expect(scriptingComponent.props("fileName")).toBe("main.knexp");
  });

  it("saves settings", () => {
    const { wrapper } = doMount();
    const scriptingEditor = wrapper.findComponent(ScriptingEditor);
    scriptingEditor.vm.$emit("save-settings", { script: "myScript" });
    expect(getScriptingService().saveSettings).toHaveBeenCalledWith({
      script: "myScript",
    });
  });
});
