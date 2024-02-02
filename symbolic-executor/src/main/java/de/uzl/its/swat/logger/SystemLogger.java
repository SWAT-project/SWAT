package de.uzl.its.swat.logger;

import de.uzl.its.swat.config.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.*;

public class SystemLogger {
    private static SystemLogger instance;
    private static java.util.logging.Logger logger;
    private static FileHandler fileHandler;
    private static ConsoleHandler consoleHandler;
    private static final Config config = Config.instance();

    public SystemLogger() {
        // Make this class a singleton
        if (instance != null) {
            return;
        }
        initLogging();
        // Create the log file
        try {
            fileHandler = new FileHandler(config.getLoggingPath() + "system.log");
            consoleHandler = new ConsoleHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }

        consoleHandler.setFormatter(new ConsoleLogFormatter());
        fileHandler.setFormatter(new LogFormatter());

        // Part of making this class a singleton
        logger = java.util.logging.Logger.getLogger(config.getLoggingPath() + "/System");
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);
        logger.addHandler(consoleHandler);

        instance = this;
    }

    public java.util.logging.Logger getLogger() {
        return SystemLogger.logger;
    }

    private static boolean isBoxStarted = false;
    private static int boxSize = 0;
    private static final StringBuilder boxBuilder = new StringBuilder();
    private static boolean discard = true;

    public void logError(String msg, Throwable t) {
        logger.log(Level.SEVERE, msg, t);
    }

    public void info(String msg) {
        logger.log(Level.INFO, msg);
    }

    public void warn(String msg) {
        logger.log(Level.WARNING, msg);
    }

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
            logger.warning("Box is already started, cannot start box with message: " + heading);
        }
        int len = heading.length() + 2;
        // Adjusting the size if the heading is longer than the specified size
        if (len > size) {
            size = len;
        }

        // Creating the top border with the heading
        boxBuilder.append("+");
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
            logger.warning("Box not started, cannot add line: " + message);
        }
        boxBuilder.append("| ");
        int padding = boxSize - message.length() - 2; // 2 for the side borders
        boxBuilder.append(message);
        boxBuilder.append(" ".repeat(Math.max(0, padding)));
        boxBuilder.append(" |\n");
        discard = discard && !isContent;
    }

    public void endBox() {
        if (!config.isVerbose()) return;
        if (!isBoxStarted) {
            logger.warning("Box not started, cannot end box!");
        }
        if (!discard) {
            // Adding an empty line after the first line
            boxBuilder.append("|");
            boxBuilder.append(" ".repeat(Math.max(0, boxSize)));
            boxBuilder.append("|\n");
            boxBuilder.append("+");
            boxBuilder.append("-".repeat(Math.max(0, boxSize)));
            boxBuilder.append("+\n");
            logger.info(boxBuilder.toString());
        }
        // Resetting for the next box
        isBoxStarted = false;
        boxSize = 0;
        boxBuilder.setLength(0);
        discard = true;
    }

    /** Initializes the logging directory. */
    private static void initLogging() {
        Path path = Paths.get(config.getLoggingPath());
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class LogFormatter extends SimpleFormatter {
    @Override
    public String format(LogRecord logRecord) {
        return logRecord.getLevel() + ": " + formatMessage(logRecord) + "\n";
    }
}

class ConsoleLogFormatter extends SimpleFormatter {
    @Override
    public String format(LogRecord logRecord) {
        StringBuilder sb = new StringBuilder();
        // Start with color
        String color = getLevelColor(logRecord.getLevel());
        String prefix = color + "\u001B[49m" + "[" + logRecord.getLevel() + "]: ";

        String[] lines = formatMessage(logRecord).split("\n");

        for (String line : lines) {
            sb.append(prefix).append(line).append("\n");
        }

        return sb.toString();
    }

    private String getLevelColor(Level level) {
        // Foreground color codes
        String colorCode;
        if (level == Level.INFO) {
            colorCode = "\u001B[32;49m"; // Green text
        } else if (level == Level.WARNING) {
            colorCode = "\u001B[33;49m"; // Yellow text
        } else if (level == Level.SEVERE) {
            colorCode = "\u001B[31;49m"; // Red text
        } else {
            colorCode = "\u001B[0m"; // Reset
        }
        // Optionally set background color here if needed
        return colorCode;
    }
}
