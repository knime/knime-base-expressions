import { enableAutoUnmount, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import ColumnOutputSelector from "../ColumnOutputSelector.vue";

describe("ColumnOutputSelector", () => {
  enableAutoUnmount(afterEach);

  const textFieldId = "input-field-to-add-new-column";
  const dropdownMenuId = "dropdown-box-to-select-column";

  const columnsToReplace = [
    { id: "a", text: "A" },
    { id: "b", text: "B" },
    { id: "c", text: "C" },
    { id: "d", text: "D" },
  ];

  const doMount = (defaultOutputMode: "create" | "replace" = "create") => {
    const wrapper = mount(ColumnOutputSelector, {
      props: {
        allowedReplacementColumns: columnsToReplace,
        defaultOutputMode,
      },
      attachTo: "body", // needed for label clicking to work
    });

    return wrapper;
  };

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("checks that the default selected behaviour type is respected", () => {
    const wrapperCreate = doMount("create");
    expect(
      wrapperCreate.findComponent({ name: "ValueSwitch" }).props().modelValue,
    ).equals("create");
    wrapperCreate.unmount();

    const wrapperReplace = doMount("replace");
    expect(
      wrapperReplace.findComponent({ name: "ValueSwitch" }).props().modelValue,
    ).equals("replace");
    wrapperReplace.unmount();
  });

  it("checks that clicking the ValueSwitch displays the right input field", async () => {
    const wrapper = doMount();

    const leftButton = wrapper.find(
      ".value-switch .radio-group-label:first-child",
    );
    const rightButton = wrapper.find(
      ".value-switch .radio-group-label:last-child",
    );

    await leftButton.trigger("click");
    expect(wrapper.find(`#${textFieldId}`).exists()).toBeTruthy();
    expect(wrapper.find(`#${dropdownMenuId}`).exists()).toBeFalsy();

    await rightButton.trigger("click");
    expect(wrapper.find(`#${textFieldId}`).exists()).toBeFalsy();
    expect(wrapper.find(`#${dropdownMenuId}`).exists()).toBeTruthy();

    await leftButton.trigger("click");
    expect(wrapper.find(`#${textFieldId}`).exists()).toBeTruthy();
    expect(wrapper.find(`#${dropdownMenuId}`).exists()).toBeFalsy();

    // Might as well check that nothing goes wrong when clicking the same button twice
    await leftButton.trigger("click");
    expect(wrapper.find(`#${textFieldId}`).exists()).toBeTruthy();
    expect(wrapper.find(`#${dropdownMenuId}`).exists()).toBeFalsy();
  });

  it("checks that the dropdown menu drops down on click (then collapses on a second click)", async () => {
    const wrapper = doMount("replace");

    expect(wrapper.find(`#${dropdownMenuId} > ul`).isVisible()).toBeFalsy();
    await wrapper.find(`#button-${dropdownMenuId}`).trigger("click");
    expect(wrapper.find(`#${dropdownMenuId} > ul`).isVisible()).toBeTruthy();
    await wrapper.find(`#button-${dropdownMenuId}`).trigger("click");
    expect(wrapper.find(`#${dropdownMenuId} > ul`).isVisible()).toBeFalsy();
  });

  it("makes sure all the options we passed in end up in the dropdown", () => {
    const wrapper = doMount("replace");

    wrapper.findAll(`#${dropdownMenuId} > ul > li`).forEach((e, i) => {
      expect(e.text()).equals(columnsToReplace[i].text);
    });
  });
});
