# Benchmarks

This folder contains JMH benchmarks for the KNIME Core Expressions project, aimed at evaluating the performance and efficiency of the KNIME Expression Backend.

## Overview

The benchmarks are designed to provide insights into the performance of TODO

## Structure

- `org.knime.core.expressions.BenchmarkRunner`: The main entry point for running the benchmarks.
- `org.knime.core.expressions.*`: Contains micro-benchmarks for individual components of the expressions backend.
- `org.knime.core.expressions.workflow`: Contains benchmarks based on KNIME workflows.
- `workflows/`: Directory containing the benchmark workflows and associated data files.

## Running Benchmarks Locally

To execute the benchmarks, run the following command in the repository’s root directory:

```sh
mvn clean verify -Pbenchmark
```

### Running in Eclipse

To run the benchmarks in Eclipse:
- Execute the `BenchmarkRunner` class as a "JUnit Plug-in Test".
- Use the `-Dbenchmark.include.regex` system property with a regex that matches the benchmark name to include only specific tests for faster feedback.
- Use "Project -> Clean..." to rebuild the test classes if you encounter `ClassNotFoundException: org.knime.my.package.jmh_generated.<...>_jmhTest cannot be found by org.knime.core.expressions.benchmarks.tests_5.4.0.qualifier`.

### Viewing Results

- After running the benchmarks locally, find the results at `org.knime.core.expressions.benchmarks.tests/target/surefire-reports/benchmark-results.json`.
- To visualize the results, upload the JSON file to [https://jmh.morethan.io/](https://jmh.morethan.io/).
- If using Jenkins, the Jenkins job will include a **JMH Report** tab for viewing the results directly.

## Tips for Writing Benchmarks

- Use the system property `-Dbenchmark.include.regex` with a regex that matches the benchmark name to run a subset of benchmarks for faster feedback during testing.
- Ensure that all benchmarks within a class use consistent parameters. The JMH Visualizer requires this for accurate results ([reference](https://github.com/jzillmann/jmh-visualizer/issues/38#issuecomment-1072569073)).
