import * as monaco from "monaco-editor";

const languageName = "knime-expression";

const columns = ["Universe_0", "Universe_1", "Universe_2", "col name"];

const functions = ["my_func1", "my_func2"];

// TODO ROW_ID, etc
// TODO flow vars
// TODO constants

// Highlighting
monaco.languages.register({ id: languageName });
monaco.languages.setMonarchTokensProvider(languageName, {
  defaultToken: "",

  keywords: ["MISSING", "TRUE", "FALSE"],

  brackets: [
    { open: "[", close: "]", token: "delimiter.bracket" },
    { open: "(", close: ")", token: "delimiter.parenthesis" },
  ],

  tokenizer: {
    root: [
      { include: "@whitespace" },
      { include: "@comments" },
      { include: "@numbers" },
      { include: "@columnAccess" },
      { include: "@rowInformationAccess" },
      { include: "@strings" },

      [/MISSING/, "constant"],
      [/TRUE/, "keyword.true"],
      [/FALSE/, "keyword.false"],
      [/[[\]()]/, "@brackets"],

      [
        /[a-zA-Z]\w*/,
        {
          cases: {
            "@keywords": "keyword",
            "@default": "identifier",
          },
        },
      ],
    ],

    whitespace: [[/\s+/, "white"]],

    comments: [[/(^#.*$)/, "comment"]],

    // Recognize decimals, exponents and ints
    numbers: [
      [/\.[0-9_]+(e-?\d+)?/, "number.float"], // no pre-decimal
      [/[0-9_]+\.(e-?\d+)?/, "number.float"], // no post-decimal
      [/[0-9_]+\.[0-9_]+(e-?\d+)?/, "number.float"], // pre+post decimal
      [/[0-9_]+(e-?\d+)?/, "number.float"], // scientific notation w/o deceimal point
      [/[0-9_]+/, "number.int"], // int
    ],

    strings: [
      [/'/, "string.escape", "@stringBody"],
      [/"/, "string.escape", "@dblStringBody"],
    ],
    stringBody: [
      [/[^\\']+/, "string"],
      [/\\([bfnrt"']u[A-Za-z0-9]{4,8})/, "string.escape"],
      [/'/, "string.escape", "@popall"],
    ],
    dblStringBody: [
      [/[^\\"]+/, "string"],
      [/\\([bfnrt"']|u[A-Za-z0-9]{4,8})/, "string.escape"],
      [/"/, "string.escape", "@popall"],
    ],
    columnAccess: [
      [
        /\${1,2}\[\s*"/,
        "string.colname.escape",
        "@columnAccessBodyDoubleQuotes",
      ],
      [
        /\${1,2}\[\s*'/,
        "string.colname.escape",
        "@columnAccessBodySingleQuotes",
      ],
      [
        /\${1,2}(?=[A-Za-z_])/,
        "string.colname.escape",
        "@columnAccessBodyNoQuotes",
      ],
    ],
    rowInformationAccess: [
      [/\$\[(?!\[['"])/, "string.rowinfo.escape", "@rowInformationAccessBody"],
    ],
    columnAccessBodySingleQuotes: [
      // seems like we have to make this a type of string in order to override bracket colouration
      [/[^\\']+/, "string.colname"],
      [/\\([bfnrt"']|u[A-Za-z0-9]{4,8})/, "string.colname"],
      // Match an end quote iff it is followed by a comma, and jump to the column offset rule
      [/'(?=\s*,)/, "string.colname.escape", "@columnOffsetSeparator"],
      // Otherwise, match an end quote followed by a bracket
      [/'\s*]/, "string.colname.escape", "@popall"],
    ],
    columnAccessBodyDoubleQuotes: [
      [/[^\\"]+/, "string.colname"],
      [/\\([bfnrt"']|u[A-Za-z0-9]{4,8})/, "string.colname"],
      // Match an end quote iff it is followed by a comma, and jump to the column offset rule
      [/"(?=\s*,)/, "string.colname.escape", "@columnOffsetSeparator"],
      // Otherwise, match an end quote followed by a bracket
      [/"\s*]/, "string.colname.escape", "@popall"],
    ],
    columnAccessBodyNoQuotes: [
      [/[A-Za-z_]\w*/, "string.colname", "@popall"],
      [/./, "", "@popall"],
    ],
    columnOffsetSeparator: [[/\s*,\s*/, "", "@columnOffset"]],
    columnOffset: [[/[\d_]+/, "number.coloffset", "@columnAccessTerminator"]],
    columnAccessTerminator: [[/\s*\]/, "string.colname.escape", "@popall"]],
    rowInformationAccessBody: [
      [/ROW_ID/, "string.rowinfo"],
      [/ROW_INDEX/, "string.rowinfo"],
      [/ROW_NUMBER/, "string.rowinfo"],
      [/\]/, "string.rowinfo.escape", "@popall"],
    ],
  },
});

monaco.languages.setLanguageConfiguration(languageName, {
  comments: {
    lineComment: "#",
  },
  brackets: [
    ["[", "]"],
    ["(", ")"],
  ],
  autoClosingPairs: [
    { open: "[", close: "]" },
    { open: "(", close: ")" },
    { open: '"', close: '"' },
    { open: "'", close: "'" },
  ],
  folding: {
    offSide: true,
  },
});

// monaco.editor.defineTheme(languageName, {
//   base: "vs",
//   inherit: true,
//   rules: [
//     { token: "string.colname.escape", foreground: "3289ac" },
//     { token: "string.colname", foreground: "af01db" },
//     { token: "number.coloffset", foreground: "af01db" },
//     { token: "string.rowinfo.escape", foreground: "3289ac" },
//     { token: "string.rowinfo", foreground: "af01db" },
//   ],
//   colors: {},
// });
monaco.editor.setTheme(languageName);

const possibleColumnCompletions = columns.flatMap((col) => {
  // TODO do a proper check if the shorthand is valid
  if (col.indexOf(" ") === -1) {
    return [`$["${col}"]`, `$['${col}']`, `$${col}`];
  } else {
    return [`$["${col}"]`, `$['${col}']`];
  }
});

const columnCompletions = (
  prefix: string,
  range: monaco.Range,
): monaco.languages.CompletionItem[] =>
  possibleColumnCompletions
    .filter((col) => col.startsWith(prefix))
    .map((col) => ({
      label: col,
      kind: monaco.languages.CompletionItemKind.Variable,
      insertText: col,
      range,
    }));

monaco.languages.registerCompletionItemProvider(languageName, {
  triggerCharacters: [
    "$", //
    "[", //
    '"', //
  ],
  provideCompletionItems(model, position, { triggerCharacter }) {
    console.log("completion triggered");

    const relativeRange = (charsBefore: number, charsAfter: number = 0) =>
      new monaco.Range(
        position.lineNumber,
        position.column - charsBefore, // TODO min of 0?
        position.lineNumber,
        position.column + charsAfter, // TODO max of line length?
      );

    const textBefore = (numChars: number) =>
      model.getValueInRange(relativeRange(numChars));

    const textAfter = (numChars: number) =>
      model.getValueInRange(relativeRange(0, numChars));

    // TODO check if we are in a string

    // We check the previous characters to determine the context
    // starting with the most restrictive context options and
    // moving to the least restrictive

    // TODO add flow vars and ROW_ID, etc at the right time

    // Option 1: We are in the middle of a column name or flow var name
    // TODO

    // TODO if the trigger char is '"' but we are not in this case, we should not return any completions
    // Option 2: '$["'
    const textBefore3 = textBefore(3);
    if (textBefore3 === '$["') {
      const range =
        textAfter(2) === '"]' ? relativeRange(3, 2) : relativeRange(3);
      // Only show column completions
      return {
        suggestions: columnCompletions(textBefore3, range),
      };
    } else if (triggerCharacter === '"') {
      // If the completion was triggered by a double quote, we should not return any completions
      // except if the text before the double quote is '$['
      return null;
    }

    // Option 3: '$['
    const textBefore2 = textBefore(2);
    if (textBefore2 === "$[") {
      const range =
        textAfter(1) === "]" ? relativeRange(2, 1) : relativeRange(2);
      return {
        suggestions: columnCompletions(textBefore2, range),
      };
    }

    // Option 4: '$'
    const textBefore1 = textBefore(1);
    if (textBefore1 === "$") {
      // Show column completions
      return {
        suggestions: columnCompletions(textBefore1, relativeRange(1)),
      };
    }

    const word = model.getWordUntilPosition(position);

    return {
      suggestions: [
        {
          label: `DUMMY (${word.word})`,
          insertText: "DUMMY",
          range: relativeRange(0),
        },
      ],
    };
  },
});

// monaco.editor.create(document.getElementById("container"), {
//   value: "",
//   language: languageName,
// });
