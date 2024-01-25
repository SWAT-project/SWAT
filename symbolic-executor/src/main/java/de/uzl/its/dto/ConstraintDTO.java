package de.uzl.its.dto;

import java.util.ArrayList;

public class ConstraintDTO {
    @SuppressWarnings("unused")
    private ArrayList<BranchDTO> trace;

    @SuppressWarnings("unused")
    private ArrayList<InputDTO> inputs;

    public ConstraintDTO(ArrayList<InputDTO> inputs, ArrayList<BranchDTO> trace) {
        this.trace = trace;
        this.inputs = inputs;
    }

    @SuppressWarnings("unused")
    public ConstraintDTO() {}
}
