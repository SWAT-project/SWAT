package de.uzl.its.targets.artificial.target1.controller;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ModeIdentifier {

    public ModeIdentifier() {}

    @JsonCreator
    public ModeIdentifier(@JsonProperty("val") String val) {
        this.val = val;
    }

    private String val;

    public boolean checkAlpha() {
        return val.equals(ModeIdentifier.ALPHA);
    }

    public boolean checkBeta() {
        return val.equals(ModeIdentifier.BETA);
    }

    public boolean checkGamma() {
        return val.equals(ModeIdentifier.GAMMA);
    }

    private static final String ALPHA = "ALPHA";
    private static final String BETA = "BETA";
    private static final String GAMMA = "GAMMA";
}
