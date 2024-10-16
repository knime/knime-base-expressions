import * as monaco from "monaco-editor";
import { functionDataToMarkdown } from "@/components/function-catalog/functionDescriptionToMarkdown";
import type { FunctionCatalogEntryData } from "@/components/functionCatalogTypes";
import { convertFunctionsToInsertionItems } from "@/components/convertFunctionsToInsertionItems";
import { LANGUAGE } from "@/common/constants";
import type { GenericExpressionInitialData } from "@/common/types";
import type { SubItem } from "@knime/scripting-editor";
import type { TypeRendererProps } from "@/components/TypeRenderer.vue";

export type CompletionItemWithType = {
  text: string;
  kind: monaco.languages.CompletionItemKind;
  extraDetailForMonaco: { [key: string]: any };
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
const register = ({
  columnNamesForCompletion = [],
  flowVariableNamesForCompletion = [],
  extraCompletionItems = [],
  functionData = [],
  languageName = "knime-expression",
}: {
  columnNamesForCompletion?: Array<CompletionItemWithType>;
  flowVariableNamesForCompletion?: Array<CompletionItemWithType>;
  extraCompletionItems?: Array<CompletionItemWithType>;
  functionData?: FunctionCatalogEntryData[];
  languageName?: string;
} = {}) => {
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
        [
          /\$\[(?!\[['"])/,
          "string.rowinfo.escape",
          "@rowInformationAccessBody",
        ],
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

  monaco.editor.defineTheme(languageName, {
    base: "vs",
    inherit: true,
    rules: [
      { token: "string.colname.escape", foreground: "3289ac" },
      { token: "string.colname", foreground: "af01db" },
      { token: "number.coloffset", foreground: "af01db" },
      { token: "string.rowinfo.escape", foreground: "3289ac" },
      { token: "string.rowinfo", foreground: "af01db" },
    ],
    colors: {},
  });
  monaco.editor.setTheme("knime-expression");

  const completionItemProvider =
    monaco.languages.registerCompletionItemProvider(languageName, {
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

        const staticSuggestions = extraCompletionItems.map(
          (item: CompletionItemWithType) => ({
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
          }),
        );

        const escapeRegex = (text: string) =>
          text.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");

        const escapeColumnName = (columnName: string, quoteType: string) =>
          columnName
            .replace(/\\/g, "\\\\")
            .replace(new RegExp(`${quoteType}`, "g"), '\\"');

        const mapInputNameReducerFactory =
          (prefix: string) =>
          (
            aggregation: OptionalSuggestion[],
            column: CompletionItemWithType,
          ) => {
            // Add column shorthand as an option if we can
            if (/^[_a-zA-Z]\w*$/.test(column.text)) {
              aggregation.push({
                ...column,
                text: `${prefix}${column.text}`,
                requirePrefixMatches: RegExp(
                  `^.*${escapeRegex(prefix)}([A-Za-z_]\\w*)?$`,
                ),
              });
            }
            // Add the longhand methods (single and double quotes both)
            aggregation.push({
              ...column,
              text: `${prefix}['${escapeColumnName(column.text, "'")}']`,
              requirePrefixMatches: RegExp(`^.*${escapeRegex(prefix)}\\['.*$`),
            });
            aggregation.push({
              ...column,
              text: `${prefix}["${escapeColumnName(column.text, '"')}"]`,
              requirePrefixMatches: RegExp(
                `^.*${escapeRegex(prefix)}($|(\\[("?).*$))`,
              ),
            });

            return aggregation;
          };

        const columnAndFlowVariableNames = [
          ...columnNamesForCompletion.reduce(
            mapInputNameReducerFactory("$"),
            [] as OptionalSuggestion[],
          ),
          ...flowVariableNamesForCompletion.reduce(
            mapInputNameReducerFactory("$$"),
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
              /(?:[^$]*(\${1,2}(?:\[["']?)?)?)/.exec(fourCharsBefore)![1]
                .length;
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

        return {
          suggestions: [
            ...columnNameSuggestions,
            ...staticSuggestions,
          ] as monaco.languages.CompletionItem[],
        };
      },
    });

  const hoverProvider = monaco.languages.registerHoverProvider(languageName, {
    provideHover: (model, position) => {
      const isIdentifierOrKeyword = () => {
        const lineContent = model.getLineContent(position.lineNumber);
        const tokens = monaco.editor.tokenize(lineContent, languageName);
        if (!tokens || tokens.length === 0) {
          return false;
        }

        let token: monaco.Token | null = null;
        for (const currentToken of tokens[0]) {
          if (currentToken.offset >= position.column) {
            break;
          }
          token = currentToken;
        }

        return (
          token !== null &&
          (token.type.includes("identifier") || token.type.includes("keyword"))
        );
      };

      if (!isIdentifierOrKeyword()) {
        return null;
      }

      const word = model.getWordAtPosition(position);
      if (!word) {
        return null;
      }

      const func = functionData.find((f) => f.name === word.word);
      if (func) {
        const markdownContent = functionDataToMarkdown(func);
        return {
          contents: [{ value: markdownContent }],
        };
      }

      return null;
    },
  });

  return () => {
    completionItemProvider.dispose();
    hoverProvider.dispose();
  };
};

/**
 * Mapper to map a SubItem to a CompletionItemWithType.
 * @param subItem
 */
const mapSubItemToCompletionItemWithType = (
  subItem: SubItem<TypeRendererProps>,
): CompletionItemWithType => {
  if (typeof subItem.type === "string") {
    return {
      text: subItem.insertionText ?? subItem.name,
      kind: monaco.languages.CompletionItemKind.Variable,
      extraDetailForMonaco: {
        detail: `Type: ${subItem.type}`,
      },
    };
  } else {
    return {
      text: subItem.insertionText ?? subItem.name,
      kind: monaco.languages.CompletionItemKind.Variable,
      extraDetailForMonaco: {
        detail: `Type: ${subItem.type.props?.type ?? "UNKNOWN"}`,
      },
    };
  }
};

/**
 * Set up the knime-expression language and register it with Monaco directly from initial data.
 *
 * @param initialData the initial data from the initialData service to use for
 * the autocompletion.
 * @param options options for the autocompletion.
 *
 * @returns a dispose function for the autocompletion. If you register the
 *          language multiple times, e.g. to add/remove autocompletion
 *          suggestions, call this.
 */
const registerKnimeExpressionLanguage = (
  initialData: GenericExpressionInitialData,
) => {
  const allSupportedColumns = (
    (initialData.inputObjects
      ?.map((inputItem) => inputItem.subItems ?? [])
      .flat(1) ?? []) as SubItem<TypeRendererProps>[]
  ).filter((subItem) => subItem.supported);

  const allSupportedInputFlowVariables = (
    (initialData.flowVariables?.subItems ?? []) as SubItem<TypeRendererProps>[]
  ).filter((subItem) => subItem.supported);

  // Any input columns or flow variables that have insertionText specified can be passed
  // as extraCompletionItems directly (but we also have to remove them from the other lists)
  const inputsWithInsertionText: CompletionItemWithType[] = [
    ...allSupportedInputFlowVariables,
    ...allSupportedColumns,
  ]
    .filter((input) => input.insertionText)
    .map((input) => ({
      text: input.insertionText!,
      kind: monaco.languages.CompletionItemKind.Variable,
      extraDetailForMonaco: {
        detail: `Type: ${input.type}`, // NOSONAR
      },
    }));
  // TODO(AP-23422): remove the NOSONAR above when the type becomes a string

  const inputsWithoutInsertionText = allSupportedColumns.filter(
    (column) => !column.insertionText,
  );

  const flowVariablesWithoutInsertionText =
    allSupportedInputFlowVariables.filter(
      (flowVariable) => !flowVariable.insertionText,
    );

  return register({
    columnNamesForCompletion: inputsWithoutInsertionText.map(
      mapSubItemToCompletionItemWithType,
    ),
    flowVariableNamesForCompletion: flowVariablesWithoutInsertionText.map(
      mapSubItemToCompletionItemWithType,
    ),
    extraCompletionItems: [
      ...convertFunctionsToInsertionItems(
        initialData.functionCatalog.functions,
      ),
      ...inputsWithInsertionText,
    ],
    functionData: initialData.functionCatalog.functions,
    languageName: LANGUAGE,
  });
};

export default registerKnimeExpressionLanguage;
