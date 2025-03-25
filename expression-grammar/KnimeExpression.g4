grammar KnimeExpression;

// Lexer rules
// This ruleset will be used to tokenize the input string
// recommended: https://tomassetti.me/antlr-mega-tutorial

WHITESPACE: [ \r\n\t]+ -> skip;
LINE_COMMENT: '#' ~[\r\n]* -> skip;

// BOOLEAN literal
BOOLEAN: 'TRUE' | 'FALSE';

// INTEGER literal
INTEGER: '0' | NON_ZERO_DIGIT ('_'? DIGIT)*;
fragment NON_ZERO_DIGIT: [1-9];
fragment DIGIT: [0-9];

// FLOAT literal
FLOAT: POINT_FLOAT | EXPONENT_FLOAT;
fragment POINT_FLOAT: DIGIT_PART? FRACTION | DIGIT_PART '.';
fragment EXPONENT_FLOAT: (DIGIT_PART | POINT_FLOAT) EXPONENT;
fragment DIGIT_PART: DIGIT ('_'? DIGIT)*;
fragment EXPONENT: ('e' | 'E') ('+' | '-')? DIGIT_PART;
fragment FRACTION: '.' DIGIT_PART;

// STRING literal
STRING: '"' (ESC | .)*? '"' | '\'' (ESC | .)*? '\'';
fragment ESC: '\\' . ;

// MISSING literal
MISSING: 'MISSING';

// literals for accessing "special" columns
ROW_INDEX: '$[ROW_INDEX]';
ROW_NUMBER: '$[ROW_NUMBER]';
ROW_ID: '$[ROW_ID]';

// Operators

// Arithmetic
PLUS: '+';
MINUS: '-';
MULTIPLY: '*';
DIVIDE: '/';
FLOOR_DIVIDE: '//';
EXPONENTIATE: '**';
MODULO: '%';

// Comparison
LESS_THAN: '<';
LESS_THAN_EQUAL: '<=';
GREATER_THAN: '>';
GREATER_THAN_EQUAL: '>=';
EQUAL: '=';
DBL_EQUAL: '==';
NOT_EQUAL: '!=' | '<>';

// Logical
AND: 'and';
OR: 'or';
NOT: 'not';

// Missing fallback operator
MISSING_FALLBACK: '??';

// Identifier
IF_KEYWORD: 'if';
ELSE_KEYWORD: 'else';
IDENTIFIER: [a-zA-Z] [a-zA-Z_0-9]*;
COLUMN_IDENTIFIER: '$' [a-zA-Z_0-9]*;
FLOW_VAR_IDENTIFIER: '$$' [a-zA-Z_0-9]*;

FLOW_VARIABLE_ACCESS_START: '$$[';
COLUMN_ACCESS_START: '$[';
ACCESS_END: ']';
COMMA: ',';

BRACKET_OPEN: '(' ;
BRACKET_CLOSE: ')';

CURLY_OPEN: '{' ;
CURLY_CLOSE: '}';

// PARSER RULES
// This ruleset will be used to parse the tokenized input string
// This is a combined grammar, so it contains both lexer and parser rules
// For context-sensitive lexer separation of the lexer and parser rules into separate files is necessary
// Then "lexer modes" can be used to switch between the different lexer rulesets
// https://denisdefreyne.com/articles/2022-modal-lexer/
// interesting for string interpolation for instance

// Eternal rule for the full expression
fullExpr: expr EOF;

// atoms
atom:
    BOOLEAN
    | INTEGER
    | FLOAT
    | STRING
    | MISSING
    | ROW_INDEX
    | ROW_NUMBER
    | ROW_ID;


/*

match (
    match(4 = 5 -> TRUE, 6 = 7 -> FALSE) -> match (
        3 = 4 -> "true",
        TRUE -> "true",
    ),
    TRUE -> "true",
)


switch ($col,
    1 -> "bar",
    2 -> "baz",
    3 -> "foo",
    MISSING
)

when (
    $col == "foo" then 1,
    $col == "bar" -> 2.2,
    $col == "baz" -> MISSING
    else exp(5),
) # -> FLOAT | MISSING

if 1 == 2 then 3 else 4        # 3 if 1 == 2 else 4
if 1 == 2 -> 3 else 4
if (1 == 2) then
    if $condition then
        5
    else
        4
else
    6

if (1 == 2) then
    4
else if $condition then
    5
else
    4

*/

// Any valid expression
expr:
    (shortName = FLOW_VAR_IDENTIFIER |  FLOW_VARIABLE_ACCESS_START + longName = STRING ACCESS_END )                                              # flowVarAccess
    | (shortName = COLUMN_IDENTIFIER | COLUMN_ACCESS_START + longName = STRING (COMMA + (minus = MINUS)? offset = INTEGER )? ACCESS_END)         # colAccess
    | constant = IDENTIFIER                                                                                       # constant
    | IF_KEYWORD BRACKET_OPEN condition = expr BRACKET_CLOSE CURLY_OPEN thenBranch = expr CURLY_CLOSE ELSE_KEYWORD CURLY_OPEN elseBranch = expr CURLY_CLOSE # ifElse
    | name = IF_KEYWORD BRACKET_OPEN arguments? BRACKET_CLOSE                                                            # functionOrAggregationCall
    | name =  IDENTIFIER BRACKET_OPEN arguments? BRACKET_CLOSE                                                    # functionOrAggregationCall
    | expr op = MISSING_FALLBACK expr                                                                             # binaryOp
    | <assoc = right> expr op = EXPONENTIATE expr                                                                 # binaryOp
    | op = MINUS expr                                                                                             # unaryOp
    | expr op = (MULTIPLY | DIVIDE | MODULO | FLOOR_DIVIDE) expr                                                  # binaryOp
    | expr op = (PLUS | MINUS) expr                                                                               # binaryOp
    | expr op = (
        LESS_THAN
        | LESS_THAN_EQUAL
        | GREATER_THAN
        | GREATER_THAN_EQUAL
        | EQUAL
        | DBL_EQUAL
        | NOT_EQUAL
    ) expr                                                  # binaryOp
    | op = NOT expr                                         # unaryOp
    | expr op = AND expr                                    # binaryOp
    | expr op = OR expr                                     # binaryOp
    | BRACKET_OPEN inner = expr BRACKET_CLOSE               # parenthesisedExpr
    | atom                                                  # atomExpr;

arguments
    :   (namedArgument | positionalArgument) (COMMA (namedArgument | positionalArgument))* COMMA?
    ;

namedArgument
    :   argName=IDENTIFIER EQUAL expr
    ;

positionalArgument
    :   expr
    ;