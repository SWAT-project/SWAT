# `basic-1`: Basic Examples with SWAT

## Introduction

This document provides a guide to running the basic examples with SWAT. The examples in this directory serve as the first introduction to using SWAT's capabilities.

## Directory Structure

The `basic-1` directory contains:
- `Example1.java`: A simple Java class demonstrating symbolic execution with integers
- `Example2.java`: A Java class demonstrating symbolic execution with strings
- `build.sh`: Script to compile the examples
- `run.sh`: Script to execute the examples using SWAT
- `README.md`: Contains an example command

## Descriptions

### `Example1.java`

This example demonstrates basic symbolic execution with integers:
- It uses a `@Symbolic` annotation on an integer variable
- Contains a simple conditional statement that creates two distinct execution paths
- Returns different strings based on the condition (path 1 or path 2)

### `Example2.java`

This example demonstrates symbolic execution with strings:
- It uses a `@Symbolic` annotation on a string variable
- Checks if the string equals "Hello world"
- Returns different strings based on the condition (path 1 or path 2)

## Building the Examples

The `build.sh` script compiles the Java examples with the SWAT annotations:
```bash
    ./build.sh [target_file]
```
Parameters:
- `target_file`: The Java file to compile (default: `Example1.java`)
As this example uses `@Symbolic` annotations provided by SWAT, the `annotations` JAR file is required on the classpath to compile the examples.

Actions:
- Compiles the specified Java file with the SWAT annotations JAR in the classpath
- The annotations JAR is located at `../../../annotations/build/libs/annotations-1.0.jar`
In this example the class is compiled into the directory, i.e. `Example1.class`. For more complex examples such as gradle built jars see the next examples.

# Running the Examples

The `run.sh` script executes the compiled examples using SWAT:
```bash
    ./run.sh [target] [mode]
```
Parameters:
- target: The Java class to run (default: Example1)
- mode: Execution mode (options: local or explorer, default: local)

### Execution Modes:
1. Local Mode (default):
- Directly executes the program with the symbolic executor as a Java agent
```bash
     java \
     -Djava.library.path=../../../libs/java-library-path \
    -javaagent:../../../symbolic-executor/lib/symbolic-executor.jar \
    [target]
```
2. Explorer Mode:
- Runs the program using the symbolic explorer Python script
- Command used:

## Expected Outcomes

### `Example1`

When running `Example1`:
- SWAT will symbolically execute the code, exploring both paths
- It will identify that the condition `i == 42` creates two execution paths
- Path 1: When `i == 42`, returns `"Path 1"`
- Path 2: When `i != 42`, returns `"Path 2"`
- The concolic engine will systematically explore both paths and generate test cases for each

### `Example2`

When running `Example2`:
- SWAT will symbolically execute the code using a symbolic string
- It will identify that the condition` s.equals("Hello world")` creates two execution paths
- Path 1: When `s` equals `"Hello world"`, returns `"Path 1"`
- Path 2: When `s` does not equal `"Hello world"`, returns `"Path 2"`
- The engine will generate appropriate string inputs to cover both paths

## Controlling Symbolic Values

Similarly to the explorer, you can also directly control assignments of symbolic values using system properties:
```bash
    java \
    -Djava.library.path=../../../libs/java-library-path \
    -Dswat.assignment.java/lang/String_0="Hello world" \
    -javaagent:../../../symbolic-executor/lib/symbolic-executor.jar \
    Example1
```
The `-Dswat.assignment.java/lang/String_0="Hello world"` property assigns the value `"Hello world"` to the first symbolic String in the program.

## Troubleshooting

- Ensure all paths to JARs and libraries are correct
- The Z3 solver must be in the java.library.path
- If you encounter errors with the symbolic executor, check that the annotations JAR is correctly built

## Advanced Usage

- Different symbolic annotations can be applied to various data types
- Multiple symbolic variables can be used in the same program
- The explorer mode offers more advanced analysis capabilities for complex programs

This documentation provides the essential information to get started with SWAT using these basic examples. As you become more familiar with the system, you can explore more complex examples and applications in the other directories.