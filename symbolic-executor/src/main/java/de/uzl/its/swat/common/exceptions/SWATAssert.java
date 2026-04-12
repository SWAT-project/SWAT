package de.uzl.its.swat.common.exceptions;

import ch.qos.logback.classic.Logger;
import de.uzl.its.swat.common.ErrorHandler;
import de.uzl.its.swat.common.logging.GlobalLogger;
import de.uzl.its.swat.common.logging.records.ErrorRecord;
import de.uzl.its.swat.config.Config;
import de.uzl.its.swat.symbolic.instruction.Instruction;
import de.uzl.its.swat.thread.ThreadContext;
import de.uzl.its.swat.thread.ThreadHandler;
import lombok.Setter;

import java.util.Arrays;

import static java.lang.Thread.currentThread;

/**
 * SWATAssert is a utility class for checking conditions and throwing an AssertionError if the
 * condition is not met. The behavior of the assertions can be toggled on or off.
 * This class mimics the behaviour of Java's built-in assert keyword.
 * <p>
 * We (need) to use a custom assertion class because Java's built-in assert statement does not work reliably in the
 * classes used during instrumentation even when -esa (enable system assertions) is set.
 */
public class SWATAssert {
    //
    @Setter
    private static boolean enabled = Config.instance().isUseAssertions();

    private static final Logger logger = GlobalLogger.getSymbolicExecutionLogger();

    private SWATAssert() {
        // Prevent instantiation
    }

    /**
     * Checks the given condition and throws an AssertionError with the given message if the condition is false.
     * This method is similar to Java's built-in assert keyword and should be used in cases where execution is expected
     * to be able to continue with some recoverable loss. In cases that are critical to the application, consider using
     * enforce instead.
     *
     * @param condition The condition to check
     * @param message   The message to include in the AssertionError, including placeholders {}
     * @param args     The arguments to replace the placeholders in the message
     */

    public static void check(boolean condition, String message, Object... args) {
        if (condition) {
            return;
        }
        String formattedMessage = "[SWAT Assertion]: " + String.format(message.replace("{}", "%s"), args);
        AssertionError assertionError = new AssertionError(formattedMessage);
        if (enabled) {
            new ErrorHandler().handleException(formattedMessage, assertionError);
        } else {
            new ErrorHandler().logException(formattedMessage, assertionError);
        }
    }

    /**
     * Analogous to check, but always throws an AssertionError. Should be used in cases where execution cannot continue.
     *
     * @param condition The condition to check
     * @param message   The message to include in the AssertionError, including placeholders {}
     * @param args     The arguments to replace the placeholders in the message
     */
    public static void enforce(boolean condition, String message, Object... args) {
        if (condition) {
            return;
        }
        String formattedMessage = "[SWAT Assertion]: " + String.format(message.replace("{}", "%s"), args);
        AssertionError assertionError = new AssertionError(formattedMessage);
        new ErrorHandler().handleException(formattedMessage, assertionError);
    }
}
