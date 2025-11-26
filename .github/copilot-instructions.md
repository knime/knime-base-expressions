# KNIME Base Expressions - AI Agent Instructions

## Project Overview

This repository implements the **KNIME Expressions** extension for KNIME Analytics Platform. It provides expression language support across three KNIME nodes: Row Mapper, Row Filter, and Flow Variable. The project is a hybrid Java/TypeScript codebase combining ANTLR-based parsing, Eclipse plugin architecture, and Vue.js frontend.

## Architecture

### Module Structure

The repository uses **Maven Tycho** (Eclipse plugin build system) with these key modules:

- **`org.knime.core.expressions/`** - Core expression language implementation (Java)
  - Parser, AST, type system, function/aggregation framework
  - Uses ANTLR4 for parsing (`expression-grammar/KnimeExpression.g4`)
  - Generated parsers in `src/generated/java/`
- **`org.knime.base.expressions/`** - KNIME node implementations (Java + TypeScript)
  - Java: Node factories and business logic in `src/main/java/`
  - TypeScript: Monaco-based expression editor in `js-src/`
- **`expression-grammar/`** - ANTLR grammar definition
  - Generates Java parsers for `org.knime.core.expressions`
  - Frontend uses Monaco editor with custom language support
- **Test modules**: `*.tests` directories for unit tests, `*.benchmarks.tests` for JMH benchmarks

## Development Workflows

### Building the Project

**Maven (Java)**: From repository root:
```bash
mvn clean verify                    # Full build with tests
mvn verify -Pbenchmark             # Run JMH benchmarks (requires profile)
```

**Frontend (TypeScript)**: Navigate to `org.knime.base.expressions/js-src`:
```bash
npm ci                             # Install dependencies
npm run build                      # Production build
npm run dev:knime                  # Development mode (KNIME integration)
npm run dev:browser                # Development mode (mocked browser preview)
npm run test:unit                  # Vitest tests
npm run lint                       # ESLint + Stylelint
```

**ANTLR Grammar**: In `expression-grammar/`:
```bash
pixi install                       # Install ANTLR tools
pixi run build                     # Generate parsers
pixi run dev                       # Interactive testing (CTRL+D to submit)
```

### Java Dependencies

External JARs (like ANTLR runtime) must be fetched manually:
```bash
cd org.knime.core.expressions/libs/fetch_jars
mvn clean package                  # Downloads JARs to ../libs
```

**Important**: When running Maven from Eclipse, set working directory to `fetch_jars`.

### KNIME Development Mode

To develop with live KNIME integration, start KNIME Analytics Platform with:
```
-Dchromium.remote_debugging_port=8988
-Dorg.knime.ui.dev.node.dialog.url=http://localhost:5173/row-mapper.html
```

Change `row-mapper.html` to `row-filter.html` or `flow-variable.html` for other nodes.

### Linking Local Dependencies

The frontend uses `@knime/scripting-editor` from `knime-core-ui` repository. For local development:
```bash
# In knime-core-ui/js-src/packages/scripting-editor/
npm run build-watch

# In this repo's js-src/
npm link @knime/scripting-editor
```

## Project-Specific Conventions

### Code Organization

- **Generated code**: Never edit `src/generated/` - regenerate from ANTLR grammar
- **Function implementations**: Group by category in files like `StringFunctions.java`, `MathFunctions.java`
- **Aggregations**: Similar builder pattern in `AggregationBuilder`, located in `org.knime.core.expressions/aggregations/`
- **Node definitions**: Register in `org.knime.base.expressions/plugin.xml`

### Naming Conventions

- Functions/aggregations: `snake_case` (e.g., `make_date`, `regex_match`) - enforced by regex pattern

### Testing

- Function tests: Use `FunctionTestBuilder` from `org.knime.core.expressions.testing` module
- JUnit 5 in `*.tests` modules, Vitest in `js-src/src/**/__tests__/`

### CI/CD

- **Jenkins**: Main CI (see `Jenkinsfile`) - builds, tests, Sonar analysis, benchmarks
- **Bitbucket Pipelines**: Frontend-only checks (`bitbucket-pipelines.yml`) - lint, test, type-check, build
- Both run on PRs and `master`/`releases/*` branches

## Common Pitfalls

1. **ANTLR changes**: After modifying `KnimeExpression.g4`, run `pixi run build` to regenerate Java parsers
2. **Missing JARs**: If compilation fails with ANTLR classes, run `mvn clean package` in `org.knime.core.expressions/libs/fetch_jars/`
4. **Eclipse plugin context**: This is a Tycho-based Eclipse plugin - uses `plugin.xml` for extension points, P2 repositories for dependencies
5. **Type system**: Expression language has nullable types - always check `anyOptional(args)` when defining return types

## Key Files

- `org.knime.core.expressions/src/main/java/org/knime/core/expressions/` - Core AST, parser, type system
- `org.knime.core.expressions/src/main/java/org/knime/core/expressions/functions/` - Function/aggregation implementations
- `org.knime.base.expressions/js-src/src/components/ExpressionEditorPane.vue` - Monaco editor integration
- `expression-grammar/KnimeExpression.g4` - ANTLR4 grammar
- `org.knime.base.expressions/plugin.xml` - Node registration
