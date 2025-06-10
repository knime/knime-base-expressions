This folder contains the grammar definiton for the KNIME Expression Language using the [ANTLR v4 parser generator](https://github.com/antlr/antlr4/blob/master/doc/getting-started.md).

## Trying the Grammar

Install the `antlr4-tools`:
```bash
$ pixi install
```

Trying the parser
* ```bash
    $ pixi run dev
    ```
* Enter an expression
* Press CTRL+D


## Generating the Parser

```bash
$ pixi run build
```