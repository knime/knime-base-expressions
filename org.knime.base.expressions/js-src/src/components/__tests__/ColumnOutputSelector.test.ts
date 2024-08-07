import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import ColumnOutputSelector, {
  type ColumnSelectorState,
} from "../ColumnOutputSelector.vue";
import { Dropdown, InputField, ValueSwitch } from "@knime/components";

describe("ColumnOutputSelector", () => {
  enableAutoUnmount(afterEach);

  const columnsToReplace = [
    { id: "a", text: "A" },
    { id: "b", text: "B" },
    { id: "c", text: "C" },
    { id: "d", text: "D" },
  ];

  const doMount = (
    modelValue: ColumnSelectorState = {
      outputMode: "APPEND",
      createColumn: "test column",
      replaceColumn: "a",
    },
  ) => {
    const wrapper = mount(ColumnOutputSelector, {
      props: {
        allowedReplacementColumns: columnsToReplace,
        modelValue,
      },
      attachTo: "body", // needed for label clicking to work
    });

    return wrapper;
  };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("shows input field on APPEND mode", () => {
    const wrapper = doMount({
      outputMode: "APPEND",
      createColumn: "a1",
      replaceColumn: "c",
    });
    expect(wrapper.findComponent(ValueSwitch).props().modelValue).equals(
      "APPEND",
    );
    const inputField = wrapper.findComponent(InputField);
    expect(inputField.isVisible()).toBe(true);
    expect(inputField.props("modelValue")).toBe("a1");
    const dropdown = wrapper.findComponent(Dropdown);
    expect(dropdown.exists()).toBe(false);
    wrapper.unmount();
  });

  it("shows dropdown field on REPLACE mode", () => {
    const wrapper = doMount({
      outputMode: "REPLACE_EXISTING",
      createColumn: "a1",
      replaceColumn: "c",
    });
    expect(wrapper.findComponent(ValueSwitch).props().modelValue).equals(
      "REPLACE_EXISTING",
    );
    const inputField = wrapper.findComponent(InputField);
    expect(inputField.exists()).toBe(false);
    const dropdown = wrapper.findComponent(Dropdown);
    expect(dropdown.isVisible()).toBe(true);
    expect(dropdown.props("modelValue")).toBe("c");
    wrapper.unmount();
  });

  it("switches mode on ValueSwitch click", async () => {
    const wrapper = doMount();

    const leftButton = wrapper.find(".value-switch label:first-child");
    await leftButton.trigger("click");
    expect(wrapper.props("modelValue")?.outputMode).equals("APPEND");

    const rightButton = wrapper.find(".value-switch label:last-child");
    await rightButton.trigger("click");
    expect(wrapper.props("modelValue")?.outputMode).equals("REPLACE_EXISTING");
  });
});
