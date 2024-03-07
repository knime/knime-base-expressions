export const getFunctionCatalogData = () => ({
  categories: [
    {
      name: "String Manipulation",
      description: "Functions for manipulating strings",
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
  ],
  functions: [
    {
      name: "concatenateStrings",
      category: "String Manipulation",
      arguments: [
        { name: "string1", type: "string", description: "the first string" },
        { name: "string2", type: "string", description: "the second string" },
      ],
      description:
        "# Markdown\nConcatenates two strings.\n## Example:\n`concatenateStrings('hello', 'world')`",
      keywords: ["concat", "join", "merge"],
      returnType: "string",
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
    },
    {
      name: "Show that html is not rendered",
      category: "Demo",
      arguments: [],
      description:
        "# Markdown \nExtracts a substring from the input string.\n ## Example:\n`substring('abcdef', 1, 4)` <script>alert('hello')</script>",
      keywords: ["demo", "no", "html"],
      returnType: "Awww",
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
    },
  ],
});
