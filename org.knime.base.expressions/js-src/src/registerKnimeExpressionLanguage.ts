import * as monaco from "monaco-editor";

/**
 * Set up the knime-expression language and register it with Monaco.
 *
 * @param shouldSetTheme whether to set the editor theme as well. Might
 *                       cause conflict if you have multiple editors with
 *                       different languages.
 */
const registerKnimeExpressionLanguage = () => {
  monaco.languages.register({ id: "knime-expression" });

  monaco.languages.setMonarchTokensProvider("knime-expression", {
    defaultToken: "",

    keywords: ["MISSING", "true", "false"],

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
        { include: "@strings" },

        [/MISSING/, "constant"],
        [/true/, "keyword.true"],
        [/false/, "keyword.false"],
        [/\$\[?["']?/, "@columnAccess"],

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

      // Recognize negatives, positives, decimals, ints
      numbers: [
        [/-?\.[0-9_]+(e-?\d+)?/, "number.float"], // no pre-decimal
        [/-?[0-9_]+\.(e-?\d+)?/, "number.float"], // no post-decimal
        [/-?[0-9_]+\.[0-9_]+(e-?\d+)?/, "number.float"], // pre+post decimal
        [/-?[0-9_]+(e-?\d+)?/, "number.float"], // scientific notation w/o deceimal point
        [/-?[0-9_]+/, "number.int"], // int
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
        [/\${1,2}\["/, "columnaccess.escape", "@columnAccessBodyDoubleQuotes"],
        [/\${1,2}\['/, "columnaccess.escape", "@columnAccessBodySingleQuotes"],
        [/\${1,2}/, "columnaccess.escape", "@columnAccessBodyNoQuotes"],
      ],
      columnAccessBodySingleQuotes: [
        [/[^\\']+/, "string.columnaccess.colname"], // seems like we have to make this a type of string
        [/\\([bfnrt"']|u[A-Za-z0-9]{4,8})/, "string.columnaccess.colname"],
        [/']/, "columnaccess.escape", "@popall"],
      ],
      columnAccessBodyDoubleQuotes: [
        [/[^\\"]+/, "string.columnaccess.colname"],
        [/\\([bfnrt"']|u[A-Za-z0-9]{4,8})/, "string.columnaccess.colname"],
        [/"]/, "columnaccess.escape", "@popall"],
      ],
      columnAccessBodyNoQuotes: [
        [/[A-Za-z_][0-9A-Za-z_]*/, "string.columnaccess.colname", "@popall"],
        [/./, "", "@popall"],
      ],
    },
  });

  monaco.languages.setLanguageConfiguration("knime-expression", {
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

  monaco.editor.defineTheme("knime-expression", {
    base: "vs",
    inherit: true,
    rules: [
      { token: "columnaccess.escape", foreground: "3289ac" },
      { token: "string.columnaccess.colname", foreground: "af01db" },
    ],
    colors: {},
  });
  monaco.editor.setTheme("knime-expression");
};

export default registerKnimeExpressionLanguage;
