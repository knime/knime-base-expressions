import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import MultiEditorContainer, {
  type MultiEditorContainerExposes,
} from "../MultiEditorContainer.vue";
import { onKeyStroke } from "@vueuse/core";
import { nextTick } from "vue";
import type { ExpressionEditorPaneExposes } from "../ExpressionEditorPane.vue";

vi.mock("@vueuse/core", async (importOriginal) => {
  const original = await importOriginal<typeof import("@vueuse/core")>();
  return {
    ...original,
    onKeyStroke: vi.fn(),
  };
});

const extractKeysFromWrapper = (vm: MultiEditorContainerExposes) =>
  vm.getOrderedEditorStates().map((state) => state.key);

const doMount = async () => {
  const wrapper = mount(MultiEditorContainer, {
    props: {
      defaultAppendItem: "defaultAppendItem",
      defaultReplacementItem: "defaultReplacementItem",
      itemType: "column",
      replaceableItemsInInputTable: [
        {
          id: "a",
          text: "a",
        },
        {
          id: "b",
          text: "b",
        },
      ],
      settings: [
        {
          initialScript: "initialScript1",
          initialSelectorState: {
            create: "create1",
            replace: "replace1",
            outputMode: "APPEND",
          },
        },
        {
          initialScript: "initialScript2",
          initialSelectorState: {
            create: "create2",
            replace: "replace2",
            outputMode: "REPLACE_EXISTING",
          },
        },
      ],
    },
  });

  await nextTick();

  const keys = extractKeysFromWrapper(
    wrapper.vm as MultiEditorContainerExposes,
  );

  return { wrapper, keys };
};

describe("MultiEditorContainer", () => {
  it("should register onKeyStroke handler", async () => {
    await doMount();

    expect(onKeyStroke).toHaveBeenCalled();
  });

  it("should render the correct number of editors", async () => {
    const { wrapper } = await doMount();

    const editors = wrapper.findAllComponents({ name: "ExpressionEditorPane" });
    expect(editors).toHaveLength(2);

    const selectors = wrapper.findAllComponents({ name: "OutputSelector" });
    expect(selectors).toHaveLength(2);
  });

  it("should be possible to add a new editor at the bottom", async () => {
    const { wrapper, keys } = await doMount();

    const addButton = wrapper.find("[data-testid='add-new-editor-button']");
    await addButton.trigger("click");

    await nextTick();

    const editors = wrapper.findAllComponents({ name: "ExpressionEditorPane" });
    expect(editors).toHaveLength(3);

    const selectors = wrapper.findAllComponents({ name: "OutputSelector" });
    expect(selectors).toHaveLength(3);

    expect(
      extractKeysFromWrapper(wrapper.vm as MultiEditorContainerExposes),
    ).toEqual([...keys, expect.anything()]);
  });

  it("should be possible to duplicate an editor", async () => {
    const { wrapper, keys } = await doMount();

    // copy 0th editor below
    const keyToDuplicate = keys[0];
    wrapper
      .findComponent({ name: "ExpressionEditorPane" })
      .vm.$emit("copy-below", keyToDuplicate);

    await nextTick();
    await flushPromises();

    const editors = wrapper.findAllComponents({ name: "ExpressionEditorPane" });
    expect(editors).toHaveLength(3);

    const selectors = wrapper.findAllComponents({ name: "OutputSelector" });
    expect(selectors).toHaveLength(3);

    expect(
      extractKeysFromWrapper(wrapper.vm as MultiEditorContainerExposes),
    ).toEqual([keys[0], expect.anything(), keys[1]]);

    // check the text and selector states of the 0th and 1st editors are the same
    const states = (
      wrapper.vm as MultiEditorContainerExposes
    ).getOrderedEditorStates();

    expect(states[0].monacoState.text.value).toEqual(
      states[1].monacoState.text.value,
    );
    expect(states[0].selectorState).toStrictEqual(states[1].selectorState);
  });

  it("should be possible to remove an editor", async () => {
    const { wrapper, keys } = await doMount();

    const keyToRemove = keys[0];

    wrapper
      .findComponent({ name: "ExpressionEditorPane" })
      .vm.$emit("delete", keyToRemove);

    await nextTick();

    const editors = wrapper.findAllComponents({ name: "ExpressionEditorPane" });
    expect(editors).toHaveLength(1);

    const selectors = wrapper.findAllComponents({ name: "OutputSelector" });
    expect(selectors).toHaveLength(1);

    expect(
      extractKeysFromWrapper(wrapper.vm as MultiEditorContainerExposes),
    ).not.toContain(keyToRemove);
  });

  it("should not be possible to remove the last editor", async () => {
    const { wrapper, keys } = await doMount();

    keys.forEach((key) => {
      wrapper
        .findComponent({ name: "ExpressionEditorPane" })
        .vm.$emit("delete", key);
    });

    await nextTick();

    const editors = wrapper.findAllComponents({ name: "ExpressionEditorPane" });
    expect(editors).toHaveLength(1);

    expect(
      extractKeysFromWrapper(wrapper.vm as MultiEditorContainerExposes),
    ).toHaveLength(1);
  });

  it("should be possible to reorder editors", async () => {
    const { wrapper, keys: originalKeys } = await doMount();

    // First editor requests move down
    wrapper
      .findComponent({ name: "ExpressionEditorPane" })
      .vm.$emit("move-down", originalKeys[0]);

    await nextTick();

    const newKeys = extractKeysFromWrapper(
      wrapper.vm as MultiEditorContainerExposes,
    );
    expect(newKeys).toEqual([originalKeys[1], originalKeys[0]]);

    // Second editor requests move up
    wrapper
      .findComponent({ name: "ExpressionEditorPane" })
      .vm.$emit("move-up", newKeys[1]);

    const newNewKeys = extractKeysFromWrapper(
      wrapper.vm as MultiEditorContainerExposes,
    );

    expect(newNewKeys).toEqual([originalKeys[0], originalKeys[1]]);
  });

  it("should not be possible to move the first editor up or bottom editor down", async () => {
    const { wrapper, keys } = await doMount();

    // First editor requests move up
    wrapper
      .findComponent({ name: "ExpressionEditorPane" })
      .vm.$emit("move-up", keys[0]);

    await flushPromises();
    await nextTick();

    expect(
      extractKeysFromWrapper(wrapper.vm as MultiEditorContainerExposes),
    ).toEqual(keys);

    // Second editor requests move down
    wrapper
      .findComponent({ name: "ExpressionEditorPane" })
      .vm.$emit("move-down", keys[1]);

    await flushPromises();
    await nextTick();

    expect(
      extractKeysFromWrapper(wrapper.vm as MultiEditorContainerExposes),
    ).toEqual(keys);
  });

  it("should focus the first editor by default", async () => {
    const { wrapper } = await doMount();

    const editors = wrapper.findAllComponents({ name: "ExpressionEditorPane" });

    // we expect that the first editor is focused by default, but let's make sure
    expect(
      (editors[0].vm as ExpressionEditorPaneExposes).getEditorState().editor
        .value?.focus,
    ).toHaveBeenCalled();
    expect(
      (editors[1].vm as ExpressionEditorPaneExposes).getEditorState().editor
        .value?.focus,
    ).not.toHaveBeenCalled();
  });

  it.for([
    { eventToEmit: "move-down", keyIndexToPass: 0, expectedFocusedKeyIndex: 1 },
    { eventToEmit: "move-up", keyIndexToPass: 1, expectedFocusedKeyIndex: 0 },
    { eventToEmit: "delete", keyIndexToPass: 1, expectedFocusedKeyIndex: 0 },
    {
      eventToEmit: "copy-below",
      keyIndexToPass: 1,
      expectedFocusedKeyIndex: 2,
    },
  ])(
    "should focus the right editor when an editor fires $eventToEmit",
    async ({ eventToEmit, keyIndexToPass, expectedFocusedKeyIndex }) => {
      const { wrapper } = await doMount();

      wrapper
        .findAllComponents({ name: "ExpressionEditorPane" })
        [
          keyIndexToPass
        ].vm.$emit(eventToEmit, extractKeysFromWrapper(wrapper.vm)[keyIndexToPass]);

      await flushPromises();
      await nextTick();

      // Get the editor model from the wrapper
      const hopefullyFocusedEditor = (
        wrapper.vm as MultiEditorContainerExposes
      ).getOrderedEditorStates()[expectedFocusedKeyIndex].monacoState;

      const failureMessage =
        `editor ${expectedFocusedKeyIndex} wasn't focused after ` +
        `emitting ${eventToEmit} by editor ${keyIndexToPass}`;

      expect(
        hopefullyFocusedEditor.editor.value?.focus,
        failureMessage,
      ).toHaveBeenCalled();
    },
  );

  it("should add a new editor when pressing the 'add new editor' button", async () => {
    const { wrapper } = await doMount();

    const addButton = wrapper.find("[data-testid='add-new-editor-button']");
    await addButton.trigger("click");

    await nextTick();

    const editors = wrapper.findAllComponents({ name: "ExpressionEditorPane" });
    expect(editors).toHaveLength(3);

    const selectors = wrapper.findAllComponents({ name: "OutputSelector" });
    expect(selectors).toHaveLength(3);
  });
});
