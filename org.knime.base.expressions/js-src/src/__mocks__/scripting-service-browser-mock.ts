import {
  createScriptingServiceMock,
  DEFAULT_FLOW_VARIABLE_INPUTS,
} from "@knime/scripting-editor/scripting-service-browser-mock";
import { getScriptingService } from "@knime/scripting-editor";
import {
  getExpressionScriptingService,
  type ExpressionNodeSettings,
} from "@/expressionScriptingService";
import type {
  FunctionCatalogData,
  FunctionCatalogEntryData,
} from "@/components/functionCatalogTypes";

const log = (message: any, ...args: any[]) => {
  if (typeof consola === "undefined") {
    // eslint-disable-next-line no-console
    console.log(message, ...args);
  } else {
    consola.log(message, ...args);
  }
};

if (import.meta.env.MODE === "development.browser") {
  const INPUT_OBJECTS = [
    {
      name: "Input table 1",
      subItems: [
        { name: "Column 1", type: "int" },
        { name: "Column 2", type: "int" },
        { name: "Column 3", type: "int" },
        { name: "Column 4", type: "int" },
        { name: "Column 5", type: "int" },
        { name: "Column 6", type: "string" },
        { name: "Column 7", type: "boolean" },
        { name: "%<a&b>", type: "problem" },
        { name: 'b\\lah"blah', type: "problem" },
      ],
      multiSelection: false,
      subItemCodeAliasTemplate: '$["{{{escapeDblQuotes subItems.[0]}}}"]',
    },
  ];
  const FLOW_VARIABLES = {
    ...DEFAULT_FLOW_VARIABLE_INPUTS,
    multiSelection: false,
    subItemCodeAliasTemplate: '$$["{{{escapeDblQuotes subItems.[0]}}}"]',
  };

  const FUNCTION_CATALOG: FunctionCatalogData = {
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
        description:
          "# Markdown \nExtracts a substring from the input string.\n ## Example:\n`substring('abcdef', 1, 4)`",
        keywords: ["slice", "extract", "subset"],
        returnType: "string",
        entryType: "function",
      },
      {
        name: "Show that html is not rendered",
        category: "Demo",
        arguments: [],
        description:
          "# Markdown \nExtracts a substring from the input string.\n ## Example:\n`substring('abcdef', 1, 4)` <script>alert('hello')</script>",
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
        description:
          "# Markdown\nCalculates the sum of a list of numbers.\n## Example:\n`sum(1, 2, 3, 4)`",
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
        description:
          "# Markdown\nMultiplies two numbers.\n## Example:\n`multiply(5, 3)`",
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
          "# Markdown\nFilters elements of an array based on a condition.\n## Example:\n`filterArray([1, 2, 3, 4], x => x > 2)`",
        keywords: ["select", "subset", "extract"],
        returnType: "array",
        entryType: "function",
      },
      {
        name: "getDayOfWeek",
        category: "Date and Time",
        arguments: [{ name: "date", type: "date", description: "input date" }],
        description:
          "# Markdown\nReturns the day of the week for a given date.\n## Example:\n`getDayOfWeek('2024-03-07')`",
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
            returnType: "int",
            entryType: "function",
            keywords: ["something"],
          }) as FunctionCatalogEntryData,
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

  const scriptingService = createScriptingServiceMock({
    sendToServiceMockResponses: {
      runExpression: (options: any[] | undefined) => {
        consola.log("runExpression", options);
        return Promise.resolve();
      },
      getFunctionCatalog: () => Promise.resolve(FUNCTION_CATALOG),
      getDiagnostics: () => Promise.resolve([]),
    },
    inputObjects: INPUT_OBJECTS,
    flowVariableInputs: FLOW_VARIABLES,
  });

  Object.assign(getScriptingService(), scriptingService);
  Object.assign(getExpressionScriptingService(), scriptingService, {
    getInitialSettings: () => {
      const ret = Promise.resolve({
        scripts: ["mocked default script"],
        outputModes: ["APPEND"],
        createdColumns: ["mocked output col"],
        replacedColumns: [INPUT_OBJECTS[0].subItems[1].name],
        languageVersion: 1,
        builtinFunctionsVersion: 1,
        builtinAggregationsVersion: 1,
      } satisfies ExpressionNodeSettings);

      log("Called expressionScriptingService.getInitialSettings ->", ret);

      return ret;
    },
    registerSettingsGetterForApply: () => {
      log("Called expressionScriptingService.registerSettingsGetterForApply");
    },
  });
}
