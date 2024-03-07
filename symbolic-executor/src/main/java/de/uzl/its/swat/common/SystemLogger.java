package de.uzl.its.swat.common;

import de.uzl.its.swat.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class SystemLogger {
    private static SystemLogger instance;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SystemLogger.class);

    private static final Config config = Config.instance();

    public SystemLogger() {
        // Make this class a singleton
        if (instance != null) {
            return;
        }
        instance = this;
    }

    public Logger getLogger() {
        return SystemLogger.logger;
    }

    private static boolean isBoxStarted = false;
    private static int boxSize = 0;
    private static final StringBuilder boxBuilder = new StringBuilder();
    private static boolean discard = true;

    public void fullBox(int size, String heading, ArrayList<String> msgs) {
        if (!config.isVerbose()) return;

        startBox(size, heading);
        if (msgs != null) {
            for (String msg : msgs) {
                addToBox(msg);
            }
        }
        endBox();
    }

    public void startBox(int size, String heading) {
        if (!config.isVerbose()) return;
        if (isBoxStarted) {
            logger.warn("Box is already started, cannot start box with message: " + heading);
        }
        int len = heading.length() + 2;
        // Adjusting the size if the heading is longer than the specified size
        if (len > size) {
            size = len;
        }

        // Creating the top border with the heading
        boxBuilder.append("\n+");
        int paddingBeforeHeading = (size - len) / 2;
        boxBuilder.append("-".repeat(Math.max(0, paddingBeforeHeading)));
        boxBuilder.append(" ").append(heading).append(" ");
        int paddingAfterHeading = size - paddingBeforeHeading - len;
        boxBuilder.append("-".repeat(Math.max(0, paddingAfterHeading)));
        boxBuilder.append("+\n");

        // Adding an empty line after the first line
        boxBuilder.append("|");
        boxBuilder.append(" ".repeat(Math.max(0, size)));
        boxBuilder.append("|\n");

        isBoxStarted = true;
        boxSize = size;
    }

    public void addToBox(String message) {
        addToBox(message, true);
    }

    public void addToBox(String message, boolean isContent) {
        if (!config.isVerbose()) return;
        if (!isBoxStarted) {
            logger.warn("Box not started, cannot add line: " + message);
        }
        boxBuilder.append("| ");
        int padding = boxSize - message.length() - 2; // 2 for the side borders
        boxBuilder.append(message);
        boxBuilder.append(" ".repeat(Math.max(0, padding)));
        boxBuilder.append(" |\n");
        discard = discard && !isContent;
    }

    public String endBox() {
        if (!config.isVerbose()) return "";
        if (!isBoxStarted) {
            logger.warn("Box not started, cannot end box!");
        }
        if (!discard) {
            // Adding an empty line after the first line
            boxBuilder.append("|");
            boxBuilder.append(" ".repeat(Math.max(0, boxSize)));
            boxBuilder.append("|\n");
            boxBuilder.append("+");
            boxBuilder.append("-".repeat(Math.max(0, boxSize)));
            boxBuilder.append("+\n");
            return boxBuilder.toString();
        }
        // Resetting for the next box
        isBoxStarted = false;
        boxSize = 0;
        boxBuilder.setLength(0);
        discard = true;
        return "";
    }
}
