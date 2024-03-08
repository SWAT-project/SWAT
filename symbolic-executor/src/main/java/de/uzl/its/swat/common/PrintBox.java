package de.uzl.its.swat.common;

import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents a utility for printing messages inside a box structure. The box has a
 * dynamic size based on the content and can include a heading. Using the .toString() method will
 * return the box as a string.
 */
public class PrintBox {

    // Used to indicate whether a box contains content or just boilerplate. Used to determine if a
    // box should be printed.
    @Getter @Setter private boolean contentPresent = false;
    // The size of the box
    private int boxSize;
    // The builder for the box
    private final StringBuilder boxBuilder = new StringBuilder();

    // The heading of the box
    @Getter @Setter private String heading;
    // The content of the box
    @Getter @Setter private ArrayList<String> msgs;

    /**
     * Constructs a PrintBox with a specified box size.
     *
     * @param boxSize The size of the box.
     */
    public PrintBox(int boxSize) {

        this.boxSize = boxSize;
        this.heading = "";
        this.msgs = new ArrayList<>();
    }

    /**
     * Constructs a PrintBox with a specified box size and heading.
     *
     * @param boxSize The size of the box.
     * @param heading The heading of the box.
     */
    public PrintBox(int boxSize, String heading) {

        this.boxSize = boxSize;
        this.heading = heading;
        this.msgs = new ArrayList<>();
    }

    /**
     * Constructs a PrintBox with a specified box size, heading and some or all content.
     *
     * @param boxSize The size of the box.
     * @param heading The heading of the box.
     * @param msgs The content of the box.
     */
    public PrintBox(int boxSize, String heading, ArrayList<String> msgs) {

        this.boxSize = boxSize;
        this.heading = heading;
        this.msgs = msgs;
    }

    /**
     * Adds a message to the box.
     *
     * @param msg The message to add.
     */
    public void addMsg(String msg) {
        this.msgs.add(msg);
    }

    /**
     * Assembles the box based on the content and returns it as a string.
     *
     * @return The box as a string.
     */
    private String assemble() {
        int minLen =
                Math.max(
                                (heading.length() + 2),
                                msgs.stream().mapToInt(String::length).max().orElse(0))
                        + 6;
        this.boxSize = Math.max(minLen, boxSize);

        // Header
        boxBuilder.append("\n+");
        int paddingBeforeHeading = (this.boxSize - (heading.length() + 2)) / 2;
        boxBuilder.append("-".repeat(Math.max(0, paddingBeforeHeading)));
        boxBuilder.append(" ").append(heading).append(" ");
        int paddingAfterHeading = this.boxSize - paddingBeforeHeading - (heading.length() + 2);
        boxBuilder.append("-".repeat(Math.max(0, paddingAfterHeading)));
        boxBuilder.append("+\n");

        // Adding an empty line after the first line
        boxBuilder.append("|");
        boxBuilder.append(" ".repeat(Math.max(0, this.boxSize)));
        boxBuilder.append("|\n");

        // Messages
        for (String msg : msgs) {
            boxBuilder.append("| ");
            int padding = this.boxSize - msg.length() - 2; // 2 for the side borders
            boxBuilder.append(msg);
            boxBuilder.append(" ".repeat(Math.max(0, padding)));
            boxBuilder.append(" |\n");
        }
        // Footer
        boxBuilder.append("|");
        boxBuilder.append(" ".repeat(Math.max(0, this.boxSize)));
        boxBuilder.append("|\n");
        boxBuilder.append("+");
        boxBuilder.append("-".repeat(Math.max(0, this.boxSize)));
        boxBuilder.append("+\n");

        return boxBuilder.toString();
    }

    /**
     * Returns the box as a string.
     *
     * @return The box as a string.
     */
    @Override
    public String toString() {
        return assemble();
    }
}
