This folder contains the grammar definiton for the KNIME Expression Language using the [ANTLR v4 parser generator](https://github.com/antlr/antlr4/blob/master/doc/getting-started.md).

## Trying the Grammar

Install the `antlr4-tools`:
```bash
$ conda create -n antlr4 -c conda-forge antlr4-tools
```

Trying the parser
```bash
$ antlr4-parse KnimeExpression.g4 fullExpr -tree
```

## Generating the Parser

```bash
$ antlr4 -o ../org.knime.core.expressions/src/generated/java/org/knime/core/expressions/antlr -package org.knime.core.expressions.antlr -visitor KnimeExpression.g4
```