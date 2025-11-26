---
applyTo: "org.knime.core.expressions/**"
---
# Core expression language implementation (Java)

## Key Architectural Patterns

**Expression Function Builder Pattern**: All functions use a fluent builder requiring name, description, examples, keywords, category, args, returnType, and impl in sequence:

```java
public static final ExpressionFunction MAKE_DATE = functionBuilder()
    .name("make_date")
    .description("Creates a LOCAL_DATE from year, month, day...")
    .examples("* `make_date(1970, 1, 1)` returns `1970-01-01`")
    .keywords("make", "date", "local_date", "create")
    .category(CATEGORY_CREATE_EXTRACT)
    .args(
        arg("year", "The year to use for the date.", isIntegerOrOpt()),
        arg("month", "The month to use (1-12).", isIntegerOrOpt()),
        arg("day", "The day to use (1-31).", isIntegerOrOpt())
    )
    .returnType("A LOCAL_DATE...", RETURN_LOCAL_DATE_MISSING, args -> OPT_LOCAL_DATE)
    .impl(TemporalCreateExtractFunctions::makeDateImpl)
    .build();
```

Function names **must** be `snake_case` (enforced by regex). Categories use two-level hierarchy (e.g., "String – General").

**AST Design**: All expression nodes implement sealed `Ast` interface. Use the Visitor pattern (`Ast.AstVisitor`) for traversal. Each AST node has a mutable `data()` map for attaching compilation/type information.

**Parser Integration**: `Parser.java` wraps ANTLR-generated `KnimeExpressionParser`/`KnimeExpressionLexer`. Parse errors are collected via custom `ErrorListener` and converted to `ExpressionCompileException`.