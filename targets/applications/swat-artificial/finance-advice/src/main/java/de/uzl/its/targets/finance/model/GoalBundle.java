package de.uzl.its.targets.finance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class GoalBundle {
    private List<String> types = new ArrayList<String>();
    private List<Long> amountCents = new ArrayList<Long>();
    private List<Long> dateMs = new ArrayList<Long>();
}
