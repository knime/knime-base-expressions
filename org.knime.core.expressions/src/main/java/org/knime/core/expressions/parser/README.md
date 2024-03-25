To generate the `ExpressionParser` class:

1. Navigate to the folder `org.knime.core.expressions/target/classes/`

2. Run

```sh
java -cp "../../libs/rekex-parser-1.2.0.jar:../../libs/rekex-common_util-1.2.0.jar:../../libs/rekex-grammar-1.2.0.jar:../../libs/rekex-regexp-1.2.0.jar:../../libs/javassist-3.30.2-GA.jar:." org.knime.core.expressions.parser.ExpressionGrammar
```

3. Move the class from the temporary folder to here and adapt the package name and class name
