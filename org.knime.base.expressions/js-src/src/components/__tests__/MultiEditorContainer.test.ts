import { describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { flushPromises, mount } from "@vue/test-utils";
import { onKeyStroke } from "@vueuse/core";

import { getSettingsService } from "@knime/scripting-editor";

import type { FlowVariableType } from "@/flowVariableApp/flowVariableTypes";
import type { ExpressionEditorPaneExposes } from "../ExpressionEditorPane.vue";
import MultiEditorContainer, {
  type EditorStates,
  type MultiEditorContainerExposes,
} from "../MultiEditorContainer.vue";

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

  await flushPromises();

  const keys = extractKeysFromWrapper(
    wrapper.vm as MultiEditorContainerExposes,
  );

  return { wrapper, keys };
};

const getActiveEditorIdxFromWrapper = (
  wrapper: Awaited<ReturnType<typeof doMount>>["wrapper"],
) => {
  const onChangeEvents = wrapper.emitted("on-change") as [EditorStates][];
  expect(onChangeEvents?.length).toBeGreaterThan(0);

  const lastChangeEvent = onChangeEvents?.[onChangeEvents.length - 1];
  const activeEditorKey = lastChangeEvent[0].activeEditorKey;
  const editorKeys = extractKeysFromWrapper(
    wrapper.vm as MultiEditorContainerExposes,
  );
  return activeEditorKey === null ? null : editorKeys.indexOf(activeEditorKey);
};

const addEditorAndFocus = async (
  wrapper: Awaited<ReturnType<typeof doMount>>["wrapper"],
) => {
  const addButton = wrapper.find("[data-testid='add-new-editor-button']");
  await addButton.trigger("click");
  await flushPromises();
  const addedEditorComponent = wrapper
    .findAllComponents({
      name: "ExpressionEditorPane",
    })
    .slice(-1)[0];

  // Trigger the focus event if "focus" was called on the editor.
  // In practice the editor would focus itself, but it does not do this in tests
  const addedEditorState = (
    wrapper.vm as MultiEditorContainerExposes
  ).getOrderedEditorStates()[2].monacoState;
  expect(addedEditorState.editor.value?.focus).toHaveBeenCalled();
  addedEditorComponent.vm.$emit("focus", extractKeysFromWrapper(wrapper.vm)[2]);

  await nextTick();
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

  it("should set the first editor as active on mounted", async () => {
    const { wrapper } = await doMount();
    expect(getActiveEditorIdxFromWrapper(wrapper)).toBe(0);
  });

  it("should not focus the first editor by default", async () => {
    const { wrapper } = await doMount();

    const editors = wrapper.findAllComponents({ name: "ExpressionEditorPane" });

    // We should not call "focus" on load to not steal focus from the user
    expect(
      (editors[0].vm as ExpressionEditorPaneExposes).getEditorState().editor
        .value?.focus,
    ).not.toHaveBeenCalled();
    expect(
      (editors[1].vm as ExpressionEditorPaneExposes).getEditorState().editor
        .value?.focus,
    ).not.toHaveBeenCalled();
  });

  it("should focus the copied editor", async () => {
    const { wrapper } = await doMount();

    wrapper
      .findAllComponents({ name: "ExpressionEditorPane" })[1]
      .vm.$emit("copy-below", extractKeysFromWrapper(wrapper.vm)[1]);

    await flushPromises();
    await nextTick();

    // Get the copied editor model from the wrapper
    const copiedEditor = (
      wrapper.vm as MultiEditorContainerExposes
    ).getOrderedEditorStates()[2].monacoState;

    expect(copiedEditor.editor.value?.focus).toHaveBeenCalled();
  });

  it("should focus editor after it was added", async () => {
    const { wrapper } = await doMount();

    await addEditorAndFocus(wrapper);
    expect(getActiveEditorIdxFromWrapper(wrapper)).toBe(2);
  });

  it("should not change active editor when unactive editor is deleted", async () => {
    const { wrapper } = await doMount();

    // Add two editors - the last one added should be active now
    await addEditorAndFocus(wrapper);
    await addEditorAndFocus(wrapper);
    expect(getActiveEditorIdxFromWrapper(wrapper)).toBe(3);

    const changeEventsBeforeDelete = wrapper.emitted("on-change")!.length;

    // Delete editor at index 1 - should not change active editor or call focus events
    const editorToDelete = 1;
    wrapper
      .findAllComponents({ name: "ExpressionEditorPane" })
      [
        editorToDelete
      ].vm.$emit("delete", extractKeysFromWrapper(wrapper.vm)[editorToDelete]);

    await flushPromises();
    await nextTick();

    // The first editor should not get focused
    const firstEditor = (
      wrapper.vm as MultiEditorContainerExposes
    ).getOrderedEditorStates()[0];
    expect(firstEditor.monacoState.editor.value?.focus).not.toHaveBeenCalled();

    // The last editor should still be active
    // (it was at index 3 but now is at index 2 because of the deletion)
    expect(getActiveEditorIdxFromWrapper(wrapper)).toBe(2);

    // Check that the change event was emitted
    const changeEventsAfterDelete = wrapper.emitted("on-change")!.length;
    expect(changeEventsAfterDelete - changeEventsBeforeDelete).toBe(1);
  });

  it("should set next editor active if active editor is deleted", async () => {
    const editorToDelete = 1;
    const { wrapper } = await doMount();

    // Add two editors - the last one added should be active now
    await addEditorAndFocus(wrapper);
    await addEditorAndFocus(wrapper);

    // Set the editor to delete as active
    const editorToDeleteComponent = wrapper.findAllComponents({
      name: "ExpressionEditorPane",
    })[editorToDelete];
    editorToDeleteComponent.vm.$emit(
      "focus",
      extractKeysFromWrapper(wrapper.vm)[2],
    );
    expect(getActiveEditorIdxFromWrapper(wrapper)).toBe(editorToDelete);

    // Delete the editor
    editorToDeleteComponent.vm.$emit(
      "delete",
      extractKeysFromWrapper(wrapper.vm)[editorToDelete],
    );

    await flushPromises();
    await nextTick();

    // The 1 was deleted but there is a new editor at index 1
    // this one should be active now
    expect(getActiveEditorIdxFromWrapper(wrapper)).toBe(1);
  });

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

  describe("dirty state handling", () => {
    const setupSettingsStateMock = () => {
      const settingsStateMock = {
        setValue: vi.fn(),
        addControllingFlowVariable: vi.fn(),
        addExposedFlowVariable: vi.fn(),
        initialSettings: "" as any,
      };
      const registerFnMock = vi.fn(({ initialValue }: any) => {
        settingsStateMock.initialSettings = initialValue;
        return settingsStateMock;
      });

      vi.mocked(getSettingsService().registerSettings).mockReturnValue(
        registerFnMock,
      );

      return settingsStateMock;
    };

    type SettingsStateMock = ReturnType<typeof setupSettingsStateMock>;

    const getLastSettings = (settingsStateMock: SettingsStateMock) => {
      const lastCallIndex = settingsStateMock.setValue.mock.calls.length - 1;
      return settingsStateMock.setValue.mock.calls[lastCallIndex][0];
    };

    it("should not change initial settings if nothing changes", async () => {
      const settingsStateMock = setupSettingsStateMock();
      await doMount();

      const initialSettings = settingsStateMock.initialSettings;
      const lastSettings = getLastSettings(settingsStateMock);
      expect(lastSettings).toBe(initialSettings);
    });

    it("should mark the editor as dirty when the flow variable type changes", async () => {
      const settingsStateMock = setupSettingsStateMock();

      const wrapper = mount(MultiEditorContainer, {
        props: {
          defaultAppendItem: "New Flow Variable",
          itemType: "flow variable",
          replaceableItemsInInputTable: [],
          settings: [
            {
              initialScript: "test script",
              initialSelectorState: {
                create: "test_var",
                replace: "",
                outputMode: "APPEND",
              },
              initialOutputReturnType: "String" as FlowVariableType,
            },
          ],
        },
      });
      await flushPromises();

      const initialSettings = settingsStateMock.initialSettings;

      // Change the flow variable type
      const returnTypeSelector = wrapper.findComponent({
        name: "ReturnTypeSelector",
      });
      await returnTypeSelector.vm.$emit("update:modelValue", "Integer");
      await nextTick();

      // Settings should have changed
      const newSettings = getLastSettings(settingsStateMock);
      expect(newSettings).not.toBe(initialSettings);
      expect(newSettings).toContain("Integer");
      expect(newSettings).not.toContain("String");

      // Change back to original value
      await returnTypeSelector.vm.$emit("update:modelValue", "String");
      await nextTick();

      // Settings should equal initial settings again
      const restoredSettings = getLastSettings(settingsStateMock);
      expect(restoredSettings).toBe(initialSettings);
    });

    it("should not mark as dirty when flow variable type changes to same value", async () => {
      const settingsStateMock = setupSettingsStateMock();

      const wrapper = mount(MultiEditorContainer, {
        props: {
          defaultAppendItem: "New Flow Variable",
          itemType: "flow variable",
          replaceableItemsInInputTable: [],
          settings: [
            {
              initialScript: "test script",
              initialSelectorState: {
                create: "test_var",
                replace: "",
                outputMode: "APPEND",
              },
              initialOutputReturnType: "Boolean" as FlowVariableType,
            },
          ],
        },
      });
      await flushPromises();

      const initialSettings = settingsStateMock.initialSettings;

      // Change to same value
      const returnTypeSelector = wrapper.findComponent({
        name: "ReturnTypeSelector",
      });
      await returnTypeSelector.vm.$emit("update:modelValue", "Boolean");
      await nextTick();

      // Settings should be the same (no false dirty state)
      const unchangedSettings = getLastSettings(settingsStateMock);
      expect(unchangedSettings).toBe(initialSettings);
    });

    it.each([
      { itemType: "flow variable" as const },
      { itemType: "column" as const },
    ])(
      "should mark as dirty when output name changes in $itemType mode",
      async ({ itemType }) => {
        const settingsStateMock = setupSettingsStateMock();

        const wrapper = mount(MultiEditorContainer, {
          props: {
            defaultAppendItem:
              itemType === "flow variable" ? "New Flow Variable" : "New Column",
            itemType,
            replaceableItemsInInputTable: [],
            settings: [
              {
                initialScript: "test script",
                initialSelectorState: {
                  create: "initialValue",
                  replace: "",
                  outputMode: "APPEND",
                },
                ...(itemType === "flow variable" && {
                  initialOutputReturnType: "String" as FlowVariableType,
                }),
              },
            ],
          },
        });
        await flushPromises();

        const initialSettings = settingsStateMock.initialSettings;

        // Change the output name
        const outputSelector = wrapper.findComponent({
          name: "OutputSelector",
        });
        await outputSelector.vm.$emit("update:modelValue", {
          create: "newValue",
          replace: "",
          outputMode: "APPEND",
        });
        await nextTick();

        // Settings should have changed
        const newSettings = getLastSettings(settingsStateMock);
        expect(newSettings).not.toBe(initialSettings);
        expect(newSettings).toContain("newValue");
        expect(newSettings).not.toContain("initialValue");

        // Change back to original value
        await outputSelector.vm.$emit("update:modelValue", {
          create: "initialValue",
          replace: "",
          outputMode: "APPEND",
        });
        await nextTick();

        // Settings should equal initial settings again
        const restoredSettings = getLastSettings(settingsStateMock);
        expect(restoredSettings).toBe(initialSettings);
      },
    );

    it.each([
      { itemType: "flow variable" as const },
      { itemType: "column" as const },
    ])(
      "should mark as dirty when output mode changes in $itemType mode",
      async ({ itemType }) => {
        const settingsStateMock = setupSettingsStateMock();

        const wrapper = mount(MultiEditorContainer, {
          props: {
            defaultAppendItem:
              itemType === "flow variable" ? "New Flow Variable" : "New Column",
            itemType,
            replaceableItemsInInputTable: [
              {
                id: "existing",
                text: "existing",
                type: { id: "string", text: "String" },
              },
            ],
            settings: [
              {
                initialScript: "test script",
                initialSelectorState: {
                  create: "new_output",
                  replace: "",
                  outputMode: "APPEND",
                },
                ...(itemType === "flow variable" && {
                  initialOutputReturnType: "String" as FlowVariableType,
                }),
              },
            ],
          },
        });
        await flushPromises();

        const initialSettings = settingsStateMock.initialSettings;

        // Change from APPEND to REPLACE_EXISTING
        const outputSelector = wrapper.findComponent({
          name: "OutputSelector",
        });
        await outputSelector.vm.$emit("update:modelValue", {
          create: "new_output",
          replace: "existing",
          outputMode: "REPLACE_EXISTING",
        });
        await nextTick();

        // Settings should have changed
        const newSettings = getLastSettings(settingsStateMock);
        expect(newSettings).not.toBe(initialSettings);
        expect(newSettings).toContain("REPLACE_EXISTING");
        expect(newSettings).toContain("existing");

        // Change back to original value
        await outputSelector.vm.$emit("update:modelValue", {
          create: "new_output",
          replace: "",
          outputMode: "APPEND",
        });
        await nextTick();

        // Settings should equal initial settings again
        const restoredSettings = getLastSettings(settingsStateMock);
        expect(restoredSettings).toBe(initialSettings);
      },
    );
  });
});
