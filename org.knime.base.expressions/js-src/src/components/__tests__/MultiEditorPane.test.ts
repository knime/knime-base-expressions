import { mount } from "@vue/test-utils";
import { onKeyStroke } from "@vueuse/core";
import { afterEach, describe, expect, it, vi } from "vitest";
import { editor } from "@knime/scripting-editor";
import MultiEditorPane from "../MultiEditorPane.vue";

vi.mock("@vueuse/core", async (importOriginal) => {
  const original = await importOriginal<typeof import("@vueuse/core")>();
  return {
    ...original,
    onKeyStroke: vi.fn(),
  };
});

describe("MultiEditorPane", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  const doMount = (
    args: {
      props: Partial<InstanceType<typeof MultiEditorPane>["$props"]>;
      slots?: any;
    } = {
      props: {
        title: "myTitle",
        language: "someLanguage",
        fileName: "myFile.ts",
      },
    },
  ) => {
    const wrapper = mount(MultiEditorPane, {
      props: { title: "", language: "", fileName: "", ...args.props },
      slots: args.slots,
    });

    return {
      wrapper,
    };
  };

  describe("renders", () => {
    it("displays code editor and passes props", () => {
      const useCodeEditorSpy = vi.spyOn(editor, "useCodeEditor");
      doMount();
      expect(useCodeEditorSpy).toHaveBeenCalledWith({
        container: expect.anything(),
        language: "someLanguage",
        fileName: "myFile.ts",
      });
      expect(useCodeEditorSpy.mock.calls[0][0].container.value).toBeDefined();
    });

    it("uses the provided title", () => {
      const { wrapper } = doMount();
      const heading = wrapper.find(".editor-title-bar");
      expect(heading.element.textContent).toBe("myTitle");
    });
  });

  it("should register onKeyStroke handler", () => {
    doMount();
    expect(onKeyStroke).toHaveBeenCalledWith("z", expect.anything());
    expect(onKeyStroke).toHaveBeenCalledWith("Escape", expect.anything());
  });

  it("doesn't display the title bar if we tell it not to", () => {
    const { wrapper } = doMount({
      props: { showTitleBar: false },
    });
    const heading = wrapper.find(".editor-title-bar");
    expect(heading.exists()).toBeFalsy();
  });

  it("checks that you can get the editor state", () => {
    const { wrapper } = doMount();

    expect(wrapper.vm.getEditorState).toBeDefined();
  });
});
