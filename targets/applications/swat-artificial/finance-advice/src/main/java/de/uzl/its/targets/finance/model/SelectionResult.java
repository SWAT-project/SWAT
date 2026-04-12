package de.uzl.its.targets.finance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectionResult {
    private long totalSelectedCents;
    private int selectedCount;
    private List<String> selectedTransactionIds = new ArrayList<String>();
    private List<String> notes = new ArrayList<String>();
}
