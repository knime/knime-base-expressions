import { vi } from "vitest";
export const MarkerTag = {};
export const MarkerSeverity = {};
export const languages = {
  CompletionItemKind: {
    Snippet: 0,
  },
  CompletionItemInsertTextRule: {
    InsertAsSnippet: 0,
  },
  registerCompletionItemProvider: vi.fn(),
  registerHoverProvider: vi.fn(),
  register: vi.fn(),
  setMonarchTokensProvider: vi.fn(),
  setLanguageConfiguration: vi.fn(),
};
export const editor = {
  getModel: vi.fn(() => ({
    getValue: () => "foo",
    setValue: vi.fn(),
    isAttachedToEditor: () => false,
    onDidChangeContent: vi.fn(),
    updateOptions: vi.fn(),
    dispose: vi.fn(),
  })),
  createModel: vi.fn(),
  create: vi.fn(() => ({
    onDidChangeCursorSelection: vi.fn(),
    onDidPaste: vi.fn(),
    dispose: vi.fn(),
  })),
  defineTheme: vi.fn(),
  setTheme: vi.fn(),
};
export const Uri = {
  parse: vi.fn(),
};
