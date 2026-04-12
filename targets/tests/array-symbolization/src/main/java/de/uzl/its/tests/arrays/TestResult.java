package de.uzl.its.tests.arrays;

public class TestResult {
    final String testName;
    int totalTests;
    int symbolicVars;

    TestResult(String testName, int totalTests, int symbolicVars) {
        this.testName = testName;
        this.totalTests = totalTests;
        this.symbolicVars = symbolicVars;
    }

    TestResult(String testName) {
        this.testName = testName;
        this.totalTests = 0;
        this.symbolicVars = 0;
        System.out.println("[TEST-START] Test '" + testName + "' started");
    }

    TestResult(TestResult... testResults) {
        this.testName = "All tests";
        for (TestResult tr : testResults) {
            this.totalTests += tr.totalTests;
            this.symbolicVars += tr.symbolicVars;
        }
    }

    @Override
    public String toString() {
        return "[TEST-RESULT] Test '" + testName + "':\n" +
                "  Executed tests: " + totalTests + "\n" +
                "  Expected symbolic variables: " + symbolicVars + "\n";
    }

    public String toParsableString() {
        return "[TEST-VALIDATION] (" + totalTests + "," + symbolicVars + ")";
    }
}
