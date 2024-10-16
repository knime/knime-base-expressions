import * as monaco from "monaco-editor";
import { beforeAll, describe, expect, it, vi } from "vitest";
import registerKnimeExpressionLanguage from "@/registerKnimeExpressionLanguage";
import { BASE_INITIAL_DATA } from "../__mocks__/mock-data";

vi.mock("monaco-editor");

const disposeOfCompletionProvider = vi.fn();
const disposeOfHoverProvider = vi.fn();

beforeAll(() => {
  vi.restoreAllMocks();

  vi.spyOn(monaco.languages, "registerCompletionItemProvider").mockReturnValue({
    dispose: disposeOfCompletionProvider,
  });

  vi.spyOn(monaco.languages, "registerHoverProvider").mockReturnValue({
    dispose: disposeOfHoverProvider,
  });
});

describe("registerKnimeExpressionLanguage", () => {
  it("registers the language", () => {
    registerKnimeExpressionLanguage(BASE_INITIAL_DATA);
    expect(monaco.languages.register).toHaveBeenCalledWith({
      id: "knime-expression",
    });
  });

  it("registers a configuration", () => {
    registerKnimeExpressionLanguage(BASE_INITIAL_DATA);

    expect(monaco.languages.setLanguageConfiguration).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers monarch tokeniser", () => {
    registerKnimeExpressionLanguage(BASE_INITIAL_DATA);

    expect(monaco.languages.setMonarchTokensProvider).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers a theme", () => {
    registerKnimeExpressionLanguage(BASE_INITIAL_DATA);

    expect(monaco.editor.defineTheme).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers autocompletion", () => {
    registerKnimeExpressionLanguage(BASE_INITIAL_DATA);

    expect(
      monaco.languages.registerCompletionItemProvider,
    ).toHaveBeenCalledWith("knime-expression", expect.anything());
  });

  it("registers hover", () => {
    registerKnimeExpressionLanguage(BASE_INITIAL_DATA);

    expect(monaco.languages.registerHoverProvider).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("returns a disposal function that disposes registered providers", () => {
    const dispose = registerKnimeExpressionLanguage(BASE_INITIAL_DATA);
    dispose();
    expect(disposeOfCompletionProvider).toHaveBeenCalled();
    expect(disposeOfHoverProvider).toHaveBeenCalled();
  });
});
