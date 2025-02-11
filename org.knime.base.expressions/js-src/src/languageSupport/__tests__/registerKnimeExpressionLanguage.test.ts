import { beforeEach, describe, expect, it, vi } from "vitest";
import * as monaco from "monaco-editor";

import { BASE_INITIAL_DATA } from "@/__mocks__/mock-data";
import registerKnimeExpressionLanguage from "../registerKnimeExpressionLanguage";

vi.mock("monaco-editor");

const disposeOfCompletionProvider = vi.fn();
const disposeOfHoverProvider = vi.fn();

beforeEach(() => {
  vi.restoreAllMocks();

  vi.spyOn(monaco.languages, "registerCompletionItemProvider").mockReturnValue({
    dispose: disposeOfCompletionProvider,
  });

  vi.spyOn(monaco.languages, "registerHoverProvider").mockReturnValue({
    dispose: disposeOfHoverProvider,
  });
});

const registerKnimeExpressionLanguageArgs = {
  functionData: BASE_INITIAL_DATA.functionCatalog.functions,
  columnGetter: () => BASE_INITIAL_DATA.inputObjects[0].subItems ?? [],
  flowVariableGetter: () => BASE_INITIAL_DATA.flowVariables.subItems ?? [],
};

describe("registerKnimeExpressionLanguage", () => {
  it("registers the language", () => {
    registerKnimeExpressionLanguage(registerKnimeExpressionLanguageArgs);
    expect(monaco.languages.register).toHaveBeenCalledWith({
      id: "knime-expression",
    });
  });

  it("registers a configuration", () => {
    registerKnimeExpressionLanguage(registerKnimeExpressionLanguageArgs);

    expect(monaco.languages.setLanguageConfiguration).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers monarch tokeniser", () => {
    registerKnimeExpressionLanguage(registerKnimeExpressionLanguageArgs);

    expect(monaco.languages.setMonarchTokensProvider).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers a theme", () => {
    registerKnimeExpressionLanguage(registerKnimeExpressionLanguageArgs);

    expect(monaco.editor.defineTheme).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers autocompletion", () => {
    registerKnimeExpressionLanguage(registerKnimeExpressionLanguageArgs);

    expect(
      monaco.languages.registerCompletionItemProvider,
    ).toHaveBeenCalledWith(
      "knime-expression",
      expect.objectContaining({
        triggerCharacters: ["$", "[", '"', "'", "&", "|", "!"],
        provideCompletionItems: expect.any(Function),
      }),
    );
  });

  it("registers hover", () => {
    registerKnimeExpressionLanguage(registerKnimeExpressionLanguageArgs);

    expect(monaco.languages.registerHoverProvider).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("returns a disposal function that disposes registered providers", () => {
    const dispose = registerKnimeExpressionLanguage(
      registerKnimeExpressionLanguageArgs,
    );
    dispose();
    expect(disposeOfCompletionProvider).toHaveBeenCalled();
    expect(disposeOfHoverProvider).toHaveBeenCalled();
  });
});
