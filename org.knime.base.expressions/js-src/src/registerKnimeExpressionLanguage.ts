import * as monaco from "monaco-editor";

export type CompletionItemWithType = {
  text: string;
  kind: monaco.languages.CompletionItemKind;
  extraDetailForMonaco: { [key: string]: any };
};

export type ColumnWithDType = {
  name: string;
  type: string;
};

/**
 * Set up the knime-expression language and register it with Monaco.
 *
 * @param columnNamesForCompletion the column names that the autocompletion
 *                                 needs to know about, with types included.
 * @param flowVariableNamesForCompletion the flow variable names
 * @param extraCompletionItems anything else that we might want to add to the
 *                             autocompletion.
 * @param shouldSetTheme whether to set the editor theme as well. Might
 *                       cause conflict if you have multiple editors with
 *                       different languages.
 * @param languageName the name of the language. Useful if we ever need
 *                     different autocompletion suggestions per editor, because
 *                     autocompletion in Monaco is on a per-language basis.
 *
 * @returns a dispose function for the autocompletion. If you register the
 *          language multiple times, e.g. to add/remove autocompletion
 *          suggestions, call this.
 */
const registerKnimeExpressionLanguage = ({
  columnNamesForCompletion = [],
  flowVariableNamesForCompletion = [],
  extraCompletionItems = [],
  languageName = "knime-expression",
}: {
  columnNamesForCompletion?: Array<ColumnWithDType>;
  flowVariableNamesForCompletion?: Array<ColumnWithDType>;
  extraCompletionItems?: Array<CompletionItemWithType>;
  languageName?: string;
} = {}) => {
  monaco.languages.register({ id: languageName });

  monaco.languages.setMonarchTokensProvider(languageName, {
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
        [
          /\${1,2}\["/,
          "string.colname.escape",
          "@columnAccessBodyDoubleQuotes",
        ],
        [
          /\${1,2}\['/,
          "string.colname.escape",
          "@columnAccessBodySingleQuotes",
        ],
        [/\${1,2}/, "string.colname.escape", "@columnAccessBodyNoQuotes"],
      ],
      columnAccessBodySingleQuotes: [
        // seems like we have to make this a type of string in order to override bracket colouration
        [/[^\\']+/, "string.colname"],
        [/\\([bfnrt"']|u[A-Za-z0-9]{4,8})/, "string.colname"],
        [/']/, "string.colname.escape", "@popall"],
      ],
      columnAccessBodyDoubleQuotes: [
        [/[^\\"]+/, "string.colname"],
        [/\\([bfnrt"']|u[A-Za-z0-9]{4,8})/, "string.colname"],
        [/"]/, "string.colname.escape", "@popall"],
      ],
      columnAccessBodyNoQuotes: [
        [/[A-Za-z_]\w*/, "string.colname", "@popall"],
        [/./, "", "@popall"],
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

  monaco.editor.defineTheme(languageName, {
    base: "vs",
    inherit: true,
    rules: [
      { token: "string.colname.escape", foreground: "3289ac" },
      { token: "string.colname", foreground: "af01db" },
    ],
    colors: {},
  });
  monaco.editor.setTheme("knime-expression");

  const { dispose } = monaco.languages.registerCompletionItemProvider(
    languageName,
    {
      triggerCharacters: ["$"],
      provideCompletionItems: (model, position) => {
        // Suggestion that is provided iff the characters before the start of
        // the current word match the regex.
        type OptionalSuggestion = {
          text: string;

          // RegEx to be tested on the word + the 4 chars before it (since our
          // longest possible delimiter, `$$["`, is 4 chars long)
          requirePrefixMatches: RegExp | null;

          extraDetailForMonaco: { [key: string]: any };
        };

        const completionWords = ["true", "false", "MISSING"].map((w) => ({
          text: w,
          kind: monaco.languages.CompletionItemKind.Keyword,
          extraDetailForMonaco: {},
        }));

        const staticSuggestions = [
          ...completionWords,
          ...extraCompletionItems,
        ].map((item: CompletionItemWithType) => ({
          // Replace from beginning of current word
          label: item.text,
          kind: item.kind,
          insertText: item.text,
          range: new monaco.Range(
            position.lineNumber,
            position.column,
            position.lineNumber,
            model.getWordUntilPosition(position).startColumn,
          ),
          ...item.extraDetailForMonaco,
        }));

        const escapeRegex = (text: string) =>
          text.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");

        const mapColumnNameReducerFactory = (prefix: string) => {
          const ret = (ar: OptionalSuggestion[], e: ColumnWithDType) => {
            // Add column shorthand as an option if we can
            if (/^[_a-zA-Z]\w*$/.test(e.name)) {
              ar.push({
                text: `${prefix}${e.name}`,
                requirePrefixMatches: RegExp(
                  `^.*${escapeRegex(prefix)}([A-Za-z_]\\w*)?$`,
                ),
                extraDetailForMonaco: { detail: `Type: ${e.type}` },
              });
            }
            // Add the longhand methods (single and double quotes both)
            ar.push({
              text: `${prefix}['${e.name}']`,
              requirePrefixMatches: RegExp(`^.*${escapeRegex(prefix)}\\['.*$`),
              extraDetailForMonaco: { detail: `Type: ${e.type}` },
            });
            ar.push({
              text: `${prefix}["${e.name}"]`,
              requirePrefixMatches: RegExp(
                `^.*${escapeRegex(prefix)}($|(\\[("?).*$))`,
              ),
              extraDetailForMonaco: { detail: `Type: ${e.type}` },
            });

            return ar;
          };

          return ret;
        };

        const columnAndFlowVariableNames = [
          ...columnNamesForCompletion.reduce(
            mapColumnNameReducerFactory("$"),
            [] as OptionalSuggestion[],
          ),
          ...flowVariableNamesForCompletion.reduce(
            mapColumnNameReducerFactory("$$"),
            [] as OptionalSuggestion[],
          ),
        ];

        // So first let's adjust the start to include the left delimiters.
        const partialWordBeforeCursor = model.getWordUntilPosition(position);
        const fourCharsBefore = model.getValueInRange({
          // eslint-disable-next-line no-magic-numbers
          startColumn: partialWordBeforeCursor.startColumn - 4,
          endColumn: partialWordBeforeCursor.startColumn,
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
        });
        const twoCharsAfter = model.getValueInRange({
          startColumn: partialWordBeforeCursor.endColumn,
          // eslint-disable-next-line no-magic-numbers
          endColumn: partialWordBeforeCursor.endColumn + 2,
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
        });

        const columnNameSuggestions = columnAndFlowVariableNames
          .filter((columnSuggestion) => {
            return (
              columnSuggestion.requirePrefixMatches === null ||
              columnSuggestion.requirePrefixMatches.test(
                `${fourCharsBefore}${partialWordBeforeCursor.word}`,
              )
            );
          })
          .map((columSuggestion) => {
            // Column name suggestions need a bit of special handling because
            // monaco doesn't consider [\$\['"] to be parts of a word so we need
            // custom logic to replace them, and because matching of brackets
            // means we'll often have a bracket before and after the cursor (but
            // not always!)
            const startDelimiterPosition =
              partialWordBeforeCursor.startColumn -
              /(?:\${1,2}(?:\[["']?)?)?/.exec(fourCharsBefore)![0].length;
            const endDelimiterPosition =
              partialWordBeforeCursor.endColumn +
              /(?:['"]?\])?/.exec(twoCharsAfter)![0].length;

            return {
              label: columSuggestion.text,
              kind: monaco.languages.CompletionItemKind.Variable,
              insertText: columSuggestion.text,
              range: new monaco.Range(
                position.lineNumber,
                startDelimiterPosition,
                position.lineNumber,
                endDelimiterPosition,
              ),
              ...columSuggestion.extraDetailForMonaco,
            };
          });

        // If column suggestions are available, return those only.
        return {
          suggestions:
            columnNameSuggestions.length > 0
              ? columnNameSuggestions
              : staticSuggestions,
        };
      },
    },
  );

  return dispose;
};

export default registerKnimeExpressionLanguage;
