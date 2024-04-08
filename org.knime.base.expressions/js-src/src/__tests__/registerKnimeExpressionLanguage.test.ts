import * as monaco from "monaco-editor";
import { beforeAll, describe, expect, it, vi } from "vitest";
import registerKnimeExpressionLanguage from "@/registerKnimeExpressionLanguage";

vi.mock("monaco-editor");

const dispose = vi.fn();

beforeAll(() => {
  vi.restoreAllMocks();

  vi.spyOn(monaco.languages, "registerCompletionItemProvider").mockReturnValue({
    dispose,
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
});
