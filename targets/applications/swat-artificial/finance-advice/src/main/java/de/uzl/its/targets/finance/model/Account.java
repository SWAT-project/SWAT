package de.uzl.its.targets.finance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class Account {
    private String id;
    private String userId;
    private String accountType;
    private String currency;
    private List<String> linkedInstitutionNames = new ArrayList<String>();
    private String metadata;
}
