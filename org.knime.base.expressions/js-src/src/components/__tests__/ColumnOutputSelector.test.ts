import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import ColumnOutputSelector, {
  type ColumnSelectorState,
} from "../ColumnOutputSelector.vue";
import type { OutputInsertionMode } from "@/expressionScriptingService";

describe("ColumnOutputSelector", () => {
  enableAutoUnmount(afterEach);

  const columnsToReplace = [
    { id: "a", text: "A" },
    { id: "b", text: "B" },
    { id: "c", text: "C" },
    { id: "d", text: "D" },
  ];

  const doMount = (outputMode: OutputInsertionMode = "APPEND") => {
    const wrapper = mount(ColumnOutputSelector, {
      props: {
        allowedReplacementColumns: columnsToReplace,
        modelValue: {
          outputMode,
          createColumn: "test column",
          replaceColumn: "a",
        },
      },
      attachTo: "body", // needed for label clicking to work
    });

    return wrapper;
  };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("checks that the v-model input value is respected", () => {
    const wrapperCreate = doMount("APPEND");
    expect(
      wrapperCreate.findComponent({ name: "ValueSwitch" }).props().modelValue,
    ).equals("APPEND");
    wrapperCreate.unmount();

    const wrapperReplace = doMount("REPLACE_EXISTING");
    expect(
      wrapperReplace.findComponent({ name: "ValueSwitch" }).props().modelValue,
    ).equals("REPLACE_EXISTING");
    wrapperReplace.unmount();
  });

  it("checks that clicking the ValueSwitch fires the right event", async () => {
    const wrapper = doMount();

    const leftButton = wrapper.find(".value-switch label:first-child");
    const rightButton = wrapper.find(".value-switch label:last-child");

    await leftButton.trigger("click");
    expect(wrapper.props().modelValue?.outputMode).equals("APPEND");

    await rightButton.trigger("click");
    expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
    expect(
      (wrapper.emitted("update:modelValue")?.[0][0] as ColumnSelectorState)
        ?.outputMode,
    ).equals("REPLACE_EXISTING");
    expect(
      (wrapper.emitted("update:modelValue")?.[0][0] as ColumnSelectorState)
        ?.replaceColumn,
    ).equals("a");

    await leftButton.trigger("click");
    expect(wrapper.emitted("update:modelValue")).toHaveLength(2);
    expect(
      (wrapper.emitted("update:modelValue")?.[1][0] as ColumnSelectorState)
        ?.outputMode,
    ).equals("APPEND");
    expect(
      (wrapper.emitted("update:modelValue")?.[1][0] as ColumnSelectorState)
        ?.createColumn,
    ).equals("test column");

    // Check that nothing fires when clicking the same button twice
    await leftButton.trigger("click");
    expect(wrapper.emitted("update:modelValue")).toHaveLength(2);
  });
});
