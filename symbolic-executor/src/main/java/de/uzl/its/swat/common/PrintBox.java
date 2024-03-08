package de.uzl.its.swat.common;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class PrintBox {
    private int boxSize;
    private final StringBuilder boxBuilder = new StringBuilder();
    @Setter
    @Getter
    private boolean isContent = false;

    private boolean isStarted = false;


    public PrintBox(int boxSize) {
        this.boxSize = boxSize;
    }

    public String fullBox(String heading, ArrayList<String> msgs) {
        assert !isStarted;
        startBox(heading);
        if (msgs != null) {
            for (String msg : msgs) {
                addToBox(msg, true);
            }
        }
        return endBox();
    }

    public void startBox(String heading) {
        assert !this.isStarted;
        this.isStarted = true;
        int minLen = heading.length() + 2;
        // Adjusting the size if the heading is longer than the specified size
        this.boxSize = Math.max(minLen, this.boxSize);

        // Creating the top border with the heading
        boxBuilder.append("\n+");
        int paddingBeforeHeading = (this.boxSize - minLen) / 2;
        boxBuilder.append("-".repeat(Math.max(0, paddingBeforeHeading)));
        boxBuilder.append(" ").append(heading).append(" ");
        int paddingAfterHeading = this.boxSize - paddingBeforeHeading - minLen;
        boxBuilder.append("-".repeat(Math.max(0, paddingAfterHeading)));
        boxBuilder.append("+\n");

        // Adding an empty line after the first line
        boxBuilder.append("|");
        boxBuilder.append(" ".repeat(Math.max(0, this.boxSize)));
        boxBuilder.append("|\n");
    }


    public void addToBox(String message) {
        this.addToBox(message, true);
    }
    public void addToBox(String message, boolean isContent) {
        assert this.isStarted;
        boxBuilder.append("| ");
        int padding = boxSize - message.length() - 2; // 2 for the side borders
        boxBuilder.append(message);
        boxBuilder.append(" ".repeat(Math.max(0, padding)));
        boxBuilder.append(" |\n");
        this.isContent = this.isContent && isContent;
    }

    public String endBox() {
        assert this.isStarted;
        // Adding an empty line after the first line
        boxBuilder.append("|");
        boxBuilder.append(" ".repeat(Math.max(0, boxSize)));
        boxBuilder.append("|\n");
        boxBuilder.append("+");
        boxBuilder.append("-".repeat(Math.max(0, boxSize)));
        boxBuilder.append("+\n");
        String box = boxBuilder.toString();
        boxBuilder.setLength(0);
        this.boxSize = 0;
        this.isContent = false;
        this.isStarted = false;
        return box;
    }
}
