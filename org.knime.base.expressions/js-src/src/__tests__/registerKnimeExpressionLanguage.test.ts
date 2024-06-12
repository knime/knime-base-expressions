import * as monaco from "monaco-editor";
import { beforeAll, describe, expect, it, vi } from "vitest";
import registerKnimeExpressionLanguage from "@/registerKnimeExpressionLanguage";

vi.mock("monaco-editor");

const disposeOfCompletionProvider = vi.fn();
const disposeOfHoverprovider = vi.fn();

beforeAll(() => {
  vi.restoreAllMocks();

  vi.spyOn(monaco.languages, "registerCompletionItemProvider").mockReturnValue({
    dispose: disposeOfCompletionProvider,
  });

  vi.spyOn(monaco.languages, "registerHoverProvider").mockReturnValue({
    dispose: disposeOfHoverprovider,
  });
});

describe("registerKnimeExpressionLanguage", () => {
  it("registers the language", () => {
    registerKnimeExpressionLanguage();
    expect(monaco.languages.register).toHaveBeenCalledWith({
      id: "knime-expression",
    });
  });

  it("registers a configuration", () => {
    registerKnimeExpressionLanguage();

    expect(monaco.languages.setLanguageConfiguration).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers monarch tokeniser", () => {
    registerKnimeExpressionLanguage();

    expect(monaco.languages.setMonarchTokensProvider).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers a theme", () => {
    registerKnimeExpressionLanguage();

    expect(monaco.editor.defineTheme).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("registers autocompletion", () => {
    registerKnimeExpressionLanguage();

    expect(
      monaco.languages.registerCompletionItemProvider,
    ).toHaveBeenCalledWith("knime-expression", expect.anything());
  });

  it("registers hover", () => {
    registerKnimeExpressionLanguage();

    expect(monaco.languages.registerHoverProvider).toHaveBeenCalledWith(
      "knime-expression",
      expect.anything(),
    );
  });

  it("returns a disposal function that disposes registered providers", () => {
    const dispose = registerKnimeExpressionLanguage();
    dispose();
    expect(disposeOfCompletionProvider).toHaveBeenCalled();
    expect(disposeOfHoverprovider).toHaveBeenCalled();
  });
});
