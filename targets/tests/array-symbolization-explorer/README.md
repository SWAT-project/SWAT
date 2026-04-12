# Array Symbolization with Explorer - Integration Test

This test demonstrates **symbolic array support with explorer integration** and serves as a reusable pattern for building more integration tests.

## Test Overview

The test (`SimpleArrayTest.java`) has a method that takes a symbolic `int[]` array and explores 3 distinct paths:

1. **Path 1**: `arr[0] == 42 && arr[1] == 10` → returns 1
2. **Path 2**: `arr[0] > 100` → returns 2
3. **Path 3**: Default case → returns 0

## Expected Behavior

The explorer should:
1. Start with initial array values `[0, 0]` (hits Path 3)
2. Generate new array values to explore Path 1: `[42, 10]`
3. Generate new array values to explore Path 2: e.g., `[101, 0]`
4. Confirm all 3 paths have been explored

## Prerequisites

The symbolic explorer requires a Python virtual environment:

```bash
cd symbolic-explorer
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Running the Test

### **Option 1: Gradle Task (Recommended)**

```bash
./gradlew :targets:tests:array-symbolization-explorer:arrayExplorerIntegrationTest
```

**Benefits:**
- ✅ Automatic dependency management (builds everything needed)
- ✅ Integrated validation (fails if not all paths explored)
- ✅ Clean output with pass/fail status
- ✅ Can run from IDE
- ✅ Part of build system

**Alternative Gradle task** (pure Gradle, no bash script):
```bash
./gradlew :targets:tests:array-symbolization-explorer:arrayExplorerTest
```

### **Option 2: Bash Script**

```bash
cd targets/tests/array-symbolization-explorer
./run.sh
```

**Benefits:**
- ✅ Quick manual testing
- ✅ Colorized output
- ✅ Easy to modify for debugging
- ✅ CI/CD friendly

## Manual Testing

### Step 1: Build the project
```bash
cd /home/nils/dev/SWAT-Research
./gradlew :targets:tests:array-symbolization-explorer:build
./gradlew :symbolic-executor:assemble
```

### Step 2: Start the symbolic explorer
```bash
cd symbolic-explorer
python3 SymbolicExplorer.py --mode args --symbolicvars "[I" --port 8078
```

### Step 3: Run the Java target (in another terminal)
```bash
cd /home/nils/dev/SWAT-Research

java -Xmx4g \
  -Dconfig.path=targets/tests/array-symbolization-explorer/swat.cfg \
  -javaagent:symbolic-executor/lib/symbolic-executor.jar \
  -Djava.library.path=libs/java-library-path \
  -cp targets/tests/array-symbolization-explorer/build/libs/array-symbolization-explorer.jar \
  de.uzl.its.tests.arrays.SimpleArrayTest
```

## What Should Happen

1. **First execution**: Java runs with `[0, 0]`, sends constraints to explorer
2. **Explorer**: Solves constraints, finds Path 1 needs `arr[0]=42, arr[1]=10`
3. **Second execution**: Java should get new values and hit Path 1
4. **Explorer**: Finds Path 2 needs `arr[0]>100`
5. **Third execution**: Java hits Path 2
6. **Complete**: All paths explored

## Checking Results

Look for these indicators of success:

1. **In Java output**: You should see all three path messages:
   ```
   [TEST] Found path 1: arr[0]==42 && arr[1]==10
   [TEST] Found path 2: arr[0]>100
   [TEST] Found path 3: default
   ```

2. **In Explorer output**: You should see:
   - Constraints being received
   - SMT models being generated with array values
   - Solutions containing array data like `"[I_0": "[42, 10]"`

3. **In logs**: Check `targets/tests/array-symbolization-explorer/logs/` for:
   - Array injection messages: `[DSE] Injecting assignment for [I ...`
   - Array initialization messages: `[DSE] Initializing symbolic tracking for int[]`

## Reusable Test Pattern

This test serves as a **template for building more integration tests**. To create a new integration test:

### 1. **Create Test Structure**
```
targets/tests/your-test-name/
├── src/main/java/...          # Your test code with @Symbolic annotations
├── build.gradle                # Copy from this test
├── swat.cfg                    # Configure for your test
├── run.sh                      # Copy and adapt
└── README.md                   # Document your test
```

### 2. **Adapt build.gradle**
- Change `jar.manifest.attributes 'Main-Class'` to your main class
- Update task names (e.g., `yourTestIntegrationTest`)
- Modify path validation patterns in the validation block

### 3. **Define Success Criteria**
In your Gradle task, check for expected outcomes:
```groovy
def path1Count = (allOutput =~ /Your expected message/).count
if (path1Count == 0) {
    throw new GradleException("Expected condition not met")
}
```

### 4. **Run and Iterate**
```bash
./gradlew :targets:tests:your-test-name:yourTestIntegrationTest
```

### Benefits of this Pattern
- ✅ **Automated validation** - Tests fail if expectations not met
- ✅ **Reproducible** - Same results every run
- ✅ **CI/CD ready** - Can run in automated pipelines
- ✅ **Self-documenting** - Clear pass/fail criteria

## Troubleshooting

- **Arrays not being injected**: Check that `injectAssignment` methods are being called
- **Explorer not receiving constraints**: Verify `solver.mode=HTTP` in swat.cfg
- **No array values in solutions**: Check Z3Handler's `encode_array` method
- **Compilation errors**: Ensure annotation support is properly added to AnnotationMethodNode
- **Test hangs**: Check explorer log for errors, may need to increase timeout
- **Paths not explored**: Verify constraints are being sent to explorer (check logs)
