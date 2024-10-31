import { afterEach, describe, expect, it, vi } from "vitest";
import * as monaco from "monaco-editor";

import type { FunctionCatalogEntryData } from "@/components/functionCatalogTypes";
import { registerCompletionItemProvider } from "../autoComplete";

const FUNCTION_ITEMS = [
  {
    entryType: "function",
    name: "foo",
    arguments: [{ name: "arg1", type: "string", description: "" }],
    // eslint-disable-next-line no-template-curly-in-string
    expectedInsertionText: "foo($0)",
  },
  {
    entryType: "function",
    name: "abcd",
    arguments: [
      { name: "a", type: "string", description: "" },
      { name: "b", type: "number", description: "" },
    ],
    // eslint-disable-next-line no-template-curly-in-string
    expectedInsertionText: "abcd($0)",
  },
  {
    entryType: "constant",
    name: "PI",
    returnType: "number",
    expectedInsertionText: "PI",
  },
  {
    entryType: "constant",
    name: "MAX_INT",
    returnType: "number",
    expectedInsertionText: "MAX_INT",
  },
];
const SPECIAL_COLUMNS_ITEMS = [
  {
    name: "ROW_ID",
    type: "string",
    supported: true,
    insertionText: "$[ROW_ID]",
  },
  {
    name: "ROW_NUMBER",
    type: "number",
    supported: true,
    insertionText: "$[ROW_NUMBER]",
  },
];
const COLUMNS_ITEMS = [
  {
    name: "input1",
    type: "string",
    supported: true,
    expectedCompletion: "$input1",
    expectedSingleQuoteCompletion: "$['input1']",
    expectedDoubleQuoteCompletion: '$["input1"]',
  },
  {
    name: "input2",
    type: "not supported",
    supported: false,
  },
  {
    name: "_input3",
    type: "string",
    supported: true,
    expectedCompletion: "$_input3",
    expectedSingleQuoteCompletion: "$['_input3']",
    expectedDoubleQuoteCompletion: '$["_input3"]',
  },
  {
    name: "input 4",
    type: "string",
    supported: true,
    expectedSingleQuoteCompletion: "$['input 4']",
    expectedDoubleQuoteCompletion: '$["input 4"]',
  },
  {
    name: "input'5'",
    type: "string",
    supported: true,
    expectedSingleQuoteCompletion: "$['input\\'5\\'']",
    expectedDoubleQuoteCompletion: "$[\"input'5'\"]",
  },
  {
    name: 'input"6"',
    type: "string",
    supported: true,
    expectedSingleQuoteCompletion: "$['input\"6\"']",
    expectedDoubleQuoteCompletion: '$["input\\"6\\""]',
  },
];

const FLOW_VARIABLES_ITEMS = [
  {
    name: "flowVar1",
    type: "string",
    supported: true,
    expectedCompletion: "$$flowVar1",
    expectedSingleQuoteCompletion: "$$['flowVar1']",
    expectedDoubleQuoteCompletion: '$$["flowVar1"]',
  },
  {
    name: "flowVar2",
    type: "not supported",
    supported: false,
  },
  {
    name: "_flowVar3",
    type: "string",
    supported: true,
    expectedCompletion: "$$_flowVar3",
    expectedSingleQuoteCompletion: "$$['_flowVar3']",
    expectedDoubleQuoteCompletion: '$$["_flowVar3"]',
  },
  {
    name: "flowVar 4",
    type: "string",
    supported: true,
    expectedSingleQuoteCompletion: "$$['flowVar 4']",
    expectedDoubleQuoteCompletion: '$$["flowVar 4"]',
  },
  {
    name: "flowVar'5'",
    type: "string",
    supported: true,
    expectedSingleQuoteCompletion: "$$['flowVar\\'5\\'']",
    expectedDoubleQuoteCompletion: "$$[\"flowVar'5'\"]",
  },
  {
    name: 'flowVar"6"',
    type: "string",
    supported: true,
    expectedSingleQuoteCompletion: "$$['flowVar\"6\"']",
    expectedDoubleQuoteCompletion: '$$["flowVar\\"6\\""]',
  },
];

// Helper constants to make constructing the expected items easier

const FUNCTIONS = FUNCTION_ITEMS.map((i) => i.expectedInsertionText);

const SPECIAL_COLUMNS = SPECIAL_COLUMNS_ITEMS.map((i) => i.insertionText);
const SHORTHAND_COLUMNS = COLUMNS_ITEMS.filter(
  (i) => typeof i.expectedCompletion !== "undefined",
).map((i) => i.expectedCompletion);
const DOUBLE_QUOTE_COLUMNS = COLUMNS_ITEMS.filter(
  (i) => typeof i.expectedDoubleQuoteCompletion !== "undefined",
).flatMap((i) => i.expectedDoubleQuoteCompletion);
const SINGLE_QUOTE_COLUMNS = COLUMNS_ITEMS.filter(
  (i) => typeof i.expectedSingleQuoteCompletion !== "undefined",
).flatMap((i) => i.expectedSingleQuoteCompletion);

const SHORTHAND_FLOW_VARIABLES = FLOW_VARIABLES_ITEMS.filter(
  (i) => typeof i.expectedCompletion !== "undefined",
).map((i) => i.expectedCompletion);
const DOUBLE_QUOTE_FLOW_VARIABLES = FLOW_VARIABLES_ITEMS.filter(
  (i) => typeof i.expectedDoubleQuoteCompletion !== "undefined",
).flatMap((i) => i.expectedDoubleQuoteCompletion);
const SINGLE_QUOTE_FLOW_VARIABLES = FLOW_VARIABLES_ITEMS.filter(
  (i) => typeof i.expectedSingleQuoteCompletion !== "undefined",
).flatMap((i) => i.expectedSingleQuoteCompletion);

/** Unsupported items. They should never appear in the suggestions. */
const UNSUPPORTED_ITEM_NAMES = [
  ...COLUMNS_ITEMS.filter((i) => !i.supported).map((i) => i.name),
  ...FLOW_VARIABLES_ITEMS.filter((i) => !i.supported).map((i) => i.name),
];

afterEach(() => {
  vi.restoreAllMocks();
});

describe("autoComplete", () => {
  const getProvideCompletionItems = () =>
    vi.mocked(monaco.languages.registerCompletionItemProvider).mock.calls[0][1]
      .provideCompletionItems;

  const editorModelMock = (value: string) =>
    ({
      getWordUntilPosition: (position: monaco.IPosition) => {
        let i = position.column - 2;
        let word = "";
        while (i >= 0 && /[a-zA-Z0-9]/.test(value.at(i) ?? "")) {
          word = value.at(i) + word;
          i--;
        }
        return { word, startColumn: i + 2, endColumn: position.column };
      },
      getValueInRange: (range: monaco.IRange) =>
        value.substring(range.startColumn - 1, range.endColumn - 1),
    }) as monaco.editor.ITextModel;

  describe("does not trigger suggestions", () => {
    it.each([
      // string start
      { val: '10 + "', column: 7 },
      { val: "10 + '", column: 7 },
      // inside string
      { val: '10 + "Hello"', column: 12 },
      { val: "10 + 'foo'", column: 10 },
      // with unrelated string before
      { val: '"foo" + "', column: 10 },
      { val: "'bar' + '", column: 10 },
      { val: '"foo" + "bar" + "', column: 18 },
      { val: "'bar' + 'foo' + '", column: 18 },
      // with unrelated string with escaped quotes before
      { val: '"a\\"" + "a', column: 11 },
      { val: "'a\\'s' + 'b", column: 12 },
      // with unrelated string with escaped \ before
      { val: '"a\\\\" + "a', column: 11 },
      { val: "'a\\\\' + 'b", column: 11 },
      // column/flowvar access trigger inside string
      { val: '1 + "$[', column: 8 },
      { val: '1 + "$["', column: 9 },
      { val: '1 + "$$["', column: 10 },
      { val: "2 + '$[", column: 8 },
      { val: "2 + '$['", column: 9 },
      { val: "2 + '$$['", column: 10 },
    ])(
      "$val at position $column",
      ({ val, column }: { val: string; column: number }) => {
        registerCompletionItemProvider({
          functionData: FUNCTIONS.map((name) => ({ name }) as any),
          columnGetter: () => [...SPECIAL_COLUMNS_ITEMS, ...COLUMNS_ITEMS],
          flowVariableGetter: () => FLOW_VARIABLES_ITEMS,
          languageName: "knime-expressions",
        });
        const provideCompletionItems = getProvideCompletionItems();
        const result = provideCompletionItems(
          editorModelMock(val),
          { lineNumber: 1, column } as any,
          {} as any,
          {} as any,
        );
        expect(result).toBeNull();
      },
    );
  });

  describe("provides correct completions", () => {
    it.each([
      {
        // No input yet
        val: "",
        column: 1,
        expectedRangeText: "",
        expectedItems: [
          ...FUNCTIONS,
          ...SPECIAL_COLUMNS,
          ...SHORTHAND_COLUMNS,
          ...DOUBLE_QUOTE_COLUMNS,
          ...SHORTHAND_FLOW_VARIABLES,
          ...DOUBLE_QUOTE_FLOW_VARIABLES,
        ],
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a word
        val: "a",
        column: 2,
        expectedRangeText: "a",
        expectedItems: FUNCTIONS,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Strings somewhere before (should have no effect)
        val: "\"\\\"a\\\"\\\\\" + '\\'b\\'\\\\' + ", // raw: "\"a\"\\" + '\'b\'\\' +
        column: 25,
        expectedRangeText: "",
        expectedItems: [
          ...FUNCTIONS,
          ...SPECIAL_COLUMNS,
          ...SHORTHAND_COLUMNS,
          ...DOUBLE_QUOTE_COLUMNS,
          ...SHORTHAND_FLOW_VARIABLES,
          ...DOUBLE_QUOTE_FLOW_VARIABLES,
        ],
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a column/flowvar access
        val: "$",
        column: 2,
        expectedRangeText: "$",
        expectedItems: [
          ...DOUBLE_QUOTE_COLUMNS,
          ...DOUBLE_QUOTE_FLOW_VARIABLES,
        ],
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },

      // --------- FLOW VARIABLES

      {
        // Started with a flowvar access with double quote
        val: 'foo$$["',
        column: 8,
        expectedRangeText: '$$["',
        expectedItems: DOUBLE_QUOTE_FLOW_VARIABLES,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a flowvar access with double quote + ending quote
        val: 'foo$$[""]',
        column: 8,
        expectedRangeText: '$$[""]',
        expectedItems: DOUBLE_QUOTE_FLOW_VARIABLES,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a flowvar access with single quote
        val: "foo$$['",
        column: 8,
        expectedRangeText: "$$['",
        expectedItems: SINGLE_QUOTE_FLOW_VARIABLES,
        forbiddenItems: [
          ...DOUBLE_QUOTE_COLUMNS,
          ...DOUBLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a flowvar access with single quote + ending quote
        val: "foo$$['']",
        column: 8,
        expectedRangeText: "$$['']",
        expectedItems: SINGLE_QUOTE_FLOW_VARIABLES,
        forbiddenItems: [
          ...DOUBLE_QUOTE_COLUMNS,
          ...DOUBLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a flowvar access without quote
        val: "$$[",
        column: 4,
        expectedRangeText: "$$[",
        expectedItems: DOUBLE_QUOTE_FLOW_VARIABLES,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a flowvar access without quote + ending bracket
        val: "o$$[]",
        column: 5,
        expectedRangeText: "$$[]",
        expectedItems: DOUBLE_QUOTE_FLOW_VARIABLES,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },

      // --------- COLUMNS

      {
        // Started with a column access with double quote
        val: '10 + $["',
        column: 9,
        expectedRangeText: '$["',
        expectedItems: DOUBLE_QUOTE_COLUMNS,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a column access with double quote + ending quote
        val: '  $[""]',
        column: 6,
        expectedRangeText: '$[""]',
        expectedItems: DOUBLE_QUOTE_COLUMNS,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a column access with single quote
        val: "$['",
        column: 4,
        expectedRangeText: "$['",
        expectedItems: SINGLE_QUOTE_COLUMNS,
        forbiddenItems: [
          ...DOUBLE_QUOTE_COLUMNS,
          ...DOUBLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a column access with single quote + ending quote
        val: "20 + $['']",
        column: 9,
        expectedRangeText: "$['']",
        expectedItems: SINGLE_QUOTE_COLUMNS,
        forbiddenItems: [
          ...DOUBLE_QUOTE_COLUMNS,
          ...DOUBLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a column access without quote
        val: "$[",
        column: 3,
        expectedRangeText: "$[",
        expectedItems: DOUBLE_QUOTE_COLUMNS,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
      {
        // Started with a flowvar access without quote + ending bracket
        val: "o$[]",
        column: 4,
        expectedRangeText: "$[]",
        expectedItems: DOUBLE_QUOTE_COLUMNS,
        forbiddenItems: [
          ...SINGLE_QUOTE_COLUMNS,
          ...SINGLE_QUOTE_FLOW_VARIABLES,
        ],
      },
    ])(
      "$val at position $column",
      ({
        val,
        column,
        expectedRangeText,
        expectedItems,
        forbiddenItems,
      }: {
        val: string;
        column: number;
        expectedRangeText: string;
        expectedItems: string[];
        forbiddenItems: string[];
      }) => {
        registerCompletionItemProvider({
          functionData: FUNCTION_ITEMS as unknown as FunctionCatalogEntryData[],
          columnGetter: () => [...SPECIAL_COLUMNS_ITEMS, ...COLUMNS_ITEMS],
          flowVariableGetter: () => FLOW_VARIABLES_ITEMS,
          languageName: "knime-expressions",
        });
        const provideCompletionItems = getProvideCompletionItems();
        const model = editorModelMock(val);
        const result = provideCompletionItems(
          model,
          { lineNumber: 1, column } as any,
          {} as any,
          {} as any,
        ) as monaco.languages.CompletionList;

        // Check that the completion item range contains the correct word
        result.suggestions.forEach((suggestion) => {
          expect(suggestion.range).toBeDefined();
          expect(
            model.getValueInRange(suggestion.range as monaco.IRange),
            `expected range text for '${suggestion.label as string}'`,
          ).toBe(expectedRangeText);
        });

        // Check that the completions contain the expected items
        expectedItems.forEach((expectedItem) => {
          expect(result.suggestions, "contains suggestion").toContainEqual(
            expect.objectContaining({ insertText: expectedItem }),
          );
        });

        // Check that the completions do not contain the forbidden items
        forbiddenItems.forEach((forbiddenItem) => {
          expect(
            result.suggestions,
            "does not contain suggestion",
          ).not.toContainEqual(
            expect.objectContaining({ insertText: forbiddenItem }),
          );
        });

        // Check that the unsupported items are not present
        UNSUPPORTED_ITEM_NAMES.forEach((unsupportedItem) => {
          result.suggestions.forEach((suggestion) => {
            expect(
              suggestion.insertText,
              "should not contain unsupported",
            ).not.toContain(unsupportedItem);
          });
        });
      },
    );
  });
});
