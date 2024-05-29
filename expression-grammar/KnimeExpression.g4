grammar KnimeExpression;

// PARSER RULES

// Eternal rule for the full expression
fullExpr: expr EOF;

// atoms
atom:
    BOOLEAN
    | INTEGER
    | FLOAT
    | STRING
    | MISSING
    | MATHS_CONSTANT;

// Any valid expression
expr:
    (shortName = FLOW_VAR_IDENTIFIER | '$$['+ longName = STRING ']')  # flowVarAccess
    | (shortName = COLUMN_IDENTIFIER | '$['+ longName = STRING ']')   # colAccess
    | name = FUNCTION_IDENTIFIER '(' functionArgs? ')'                # functionCall
    | name = AGGREGATION_IDENTIFIER '(' aggregationArgs ')'           # aggregationCall
    | expr op = MISSING_FALLBACK expr                                 # binaryOp
    | <assoc = right> expr op = EXPONENTIATE expr                     # binaryOp
    | op = MINUS expr                                                 # unaryOp
    | expr op = (MULTIPLY | DIVIDE | MODULO | FLOOR_DIVIDE) expr      # binaryOp
    | expr op = (PLUS | MINUS) expr                                   # binaryOp
    | expr op = (
        LESS_THAN
        | LESS_THAN_EQUAL
        | GREATER_THAN
        | GREATER_THAN_EQUAL
        | EQUAL
        | NOT_EQUAL
    ) expr                              # binaryOp
    | op = NOT expr                     # unaryOp
    | expr op = AND expr                # binaryOp
    | expr op = OR expr                 # binaryOp
    | '(' inner = expr ')'              # parenthesisedExpr
    | atom                              # atomExpr;

functionArgs: expr (',' expr)* ','?;

aggregationArgs:
    (positionalAggregationArgs (',' namedAggregationArgs)? ','?)
    | (namedAggregationArgs ','?);
positionalAggregationArgs: atom (',' atom)*;
namedAggregationArgs: namedAggregationArg (',' namedAggregationArg)*;
namedAggregationArg: argName=NAMED_ARGUMENT_IDENTIFIER atom;


// Single-line comment
LINE_COMMENT: '#' ~[\r\n]* -> skip;

// LEXER RULES
WHITESPACE: [ \r\n\t]+ -> skip;

// BOOLEAN literal
BOOLEAN: 'true' | 'false';

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
STRING: '"' (ESC | ~["\\])* '"' | '\'' (ESC | ~['\\])* '\'';
fragment ESC: '\\' (UNICODE_16 | [\n\\'"abfnrtv]);
fragment UNICODE_16:
    'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
fragment HEX_DIGIT: [0-9a-fA-F];

// MISSING literal
MISSING: 'MISSING';

// MATHS_CONSTANT literal
MATHS_CONSTANT: 'E' | 'PI' | 'INF' | 'NaN' | 'MIN_INT' | 'MAX_INT' | 'MIN_FLOAT' | 'MAX_FLOAT' | 'MIN_POSITIVE_FLOAT' | 'MIN_NORMAL_FLOAT';

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
EQUAL: '==' | '=';
NOT_EQUAL: '!=' | '<>';

// Logical
AND: 'and';
OR: 'or';
NOT: 'not';

// Missing fallback operator
MISSING_FALLBACK: '??';

// Identifier
AGGREGATION_IDENTIFIER: [A-Z] [A-Z_0-9]*;
FUNCTION_IDENTIFIER: [a-z] [a-z_0-9]*;
COLUMN_IDENTIFIER: '$' [a-zA-Z_0-9]*;
FLOW_VAR_IDENTIFIER: '$$' [a-zA-Z_0-9]*;
NAMED_ARGUMENT_IDENTIFIER: [a-z] [a-z_0-9]* '=';
