package de.uzl.its.targets.finance_engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Request body for /accounts/import.
 * Replaces the raw string body + Map with a structured DTO.
 */
public class ImportPayload {
    public String csvPayload;
    public List<ColMapping> mappings = new ArrayList<ColMapping>();

    public ImportPayload() {}
}
