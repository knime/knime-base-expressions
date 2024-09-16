import type {
  FunctionCatalogData,
  FunctionCatalogEntryData,
} from "@/components/functionCatalogTypes";
import type { ExpressionInitialData } from "@/common/types";
import type {
  GenericNodeSettings,
  InputOutputModel,
} from "@knime/scripting-editor";
import { DEFAULT_FLOW_VARIABLE_INPUTS } from "@knime/scripting-editor/initial-data-service-browser-mock";

export const INPUT_OBJECTS: InputOutputModel[] = [
  {
    name: "Input table 1",
    subItems: [
      { name: "Column 1", type: "int", supported: true },
      { name: "Column 2", type: "int", supported: true },
      { name: "Column 3", type: "int", supported: true },
      { name: "Column 4", type: "int", supported: true },
      { name: "Column 5", type: "something weird", supported: false },
      { name: "Column 6", type: "string", supported: true },
      { name: "Column 7", type: "boolean", supported: true },
      { name: "%<a&b>", type: "problem", supported: true },
      { name: 'b\\lah"blah', type: "problem", supported: true },
    ],
    multiSelection: false,
    subItemCodeAliasTemplate: '$["{{{escapeDblQuotes subItems.[0]}}}"]',
  },
];

export const FLOW_VARIABLES: InputOutputModel = {
  ...DEFAULT_FLOW_VARIABLE_INPUTS,
  multiSelection: false,
  subItemCodeAliasTemplate: '$$["{{{escapeDblQuotes subItems.[0]}}}"]',
};

export const FUNCTION_CATALOG: FunctionCatalogData = {
  categories: [
    {
      name: "String Manipulation",
      description: "Functions for manipulating strings",
    },
    {
      name: "Demo",
      description: "Demo functions",
    },
    {
      name: "Math Operations",
      description: "Functions for performing mathematical operations",
    },
    {
      name: "Array Operations",
      description: "Functions for manipulating arrays",
    },
    {
      name: "Date and Time",
      description: "Functions for working with dates and times",
    },
    {
      name: "Very big category",
      description: "You have to scroll down to see everything",
    },
    { name: "Mathematical Constants", description: "Constants" },
  ],
  functions: [
    {
      name: "concatenateStringsIsAVeryLongFunctionName",
      category: "String Manipulation",
      arguments: [
        { name: "string1", type: "string", description: "the first string" },
        { name: "string2", type: "string", description: "the second string" },
      ],
      description:
        "# Markdown\nConcatenates two strings.\n## Example:\n`concatenateStrings('hello', 'world')`",
      examples: "`concatenateStrings('hello', 'world')` returns 'helloworld'",
      keywords: ["concat", "join", "merge"],
      returnType: "string",
      entryType: "function",
    },
    {
      name: "substring",
      category: "String Manipulation",
      arguments: [
        {
          name: "inputString",
          type: "string",
          description: "the input string",
        },
        {
          name: "start",
          type: "int",
          description: "starting index of the substring",
        },
        {
          name: "end",
          type: "int",
          description: "ending index of the substring",
        },
      ],
      description: "# Markdown \nExtracts a substring from the input string.",
      examples: "`substring('abcdef', 1, 4)`",
      keywords: ["slice", "extract", "subset"],
      returnType: "string",
      entryType: "function",
    },
    {
      name: "Show that html is not rendered",
      category: "Demo",
      arguments: [],
      description:
        "# Markdown \nExtracts a substring from the input string.\n<script>alert('hello')</script>",
      examples: "`blah`",
      keywords: ["demo", "no", "html"],
      returnType: "Awww",
      entryType: "function",
    },
    {
      name: "sum",
      category: "Math Operations",
      arguments: [
        {
          name: "numbers",
          type: "int",
          description: "list of numbers to sum",
          vararg: true,
        },
      ],
      description: "# Markdown\nCalculates the sum of a list of numbers.",
      examples: "`sum(1, 2, 3, 4)`",
      keywords: ["add", "total", "accumulate"],
      returnType: "int",
      entryType: "function",
    },
    {
      name: "multiply",
      category: "Math Operations",
      arguments: [
        { name: "factor1", type: "int", description: "the first factor" },
        { name: "factor2", type: "int", description: "the second factor" },
      ],
      description: "# Markdown\nMultiplies two numbers.",
      examples: "`multiply(2, 3)`",
      keywords: ["product", "times", "multiply"],
      returnType: "int",
      entryType: "function",
    },
    {
      name: "filterArray",
      category: "Array Operations",
      arguments: [
        { name: "array", type: "array", description: "input array" },
        {
          name: "condition",
          type: "function",
          description: "filtering condition function",
        },
      ],
      description:
        "# Markdown\nFilters elements of an array based on a condition.",
      examples: "`filterArray([1, 2, 3, 4, 5], x => x % 2 === 0)`",
      keywords: ["select", "subset", "extract"],
      returnType: "array",
      entryType: "function",
    },
    {
      name: "getDayOfWeek",
      category: "Date and Time",
      arguments: [{ name: "date", type: "date", description: "input date" }],
      description: "# Markdown\nReturns the day of the week for a given date.",
      examples: "`getDayOfWeek('2024-03-07')`",
      keywords: [
        "day",
        "weekday",
        "calendar",
        "date",
        "time",
        "day of week",
        "day of the week",
        "dayOfWeek",
      ],
      returnType: "string",
      entryType: "function",
    },
    // eslint-disable-next-line no-magic-numbers
    ...[...Array(20).keys()].map(
      (i) =>
        ({
          name: `function${i}`,
          category: "Very big category",
          arguments: [
            { name: "arg", type: "int", description: "Doesn't do anything" },
          ],
          description: "Doesn't do anything",
          examples: `\`functions${i}(i+1)\``,
          returnType: "int",
          entryType: "function",
          keywords: ["something"],
        }) satisfies FunctionCatalogEntryData,
    ),
    {
      name: "pi",
      category: "Mathematical Constants",
      description: "The value of Pi",
      returnType: "float",
      keywords: ["pi", "π"],
      entryType: "constant",
    },
    {
      name: "e",
      category: "Mathematical Constants",
      description: "The value of the Euler's number",
      returnType: "float",
      keywords: ["e", "Euler"],
      entryType: "constant",
    },
  ],
};

export const DEFAULT_INITIAL_DATA: ExpressionInitialData = {
  functionCatalog: FUNCTION_CATALOG,
  inputObjects: INPUT_OBJECTS,
  flowVariables: FLOW_VARIABLES,
  inputConnectionInfo: [
    { status: "OK", isOptional: true },
    { status: "OK", isOptional: false },
  ],
  expressionVersion: {
    languageVersion: 1,
    builtinFunctionsVersion: 1,
    builtinAggregationsVersion: 1,
  },
  inputPortConfigs: {
    inputPorts: [
      {
        nodeId: "id0",
        portIdx: 0,
        portName: "Input 1",
        portViewConfigs: [
          {
            label: "Input 1",
            portViewIdx: 0,
          },
        ],
      },
    ],
  },
  kAiConfig: {
    codeAssistantEnabled: true,
    codeAssistantInstalled: true,
    hubId: "mocked hub id",
  },
};

export const DEFAULT_INITIAL_SETTINGS: GenericNodeSettings = {
  settingsAreOverriddenByFlowVariable: false,
  someKey: "someValue",
};
