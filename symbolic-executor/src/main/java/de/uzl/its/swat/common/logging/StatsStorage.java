package de.uzl.its.swat.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uzl.its.swat.common.logging.records.ErrorRecord;
import de.uzl.its.swat.common.logging.records.InvocationEntry;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class StatsStorage {
    private String entrypoint;
    private final HashMap<InvocationEntry, Integer> invocations;
    private final ArrayList<ErrorRecord> errors;

    public StatsStorage() {
        this.invocations = new HashMap<>();
        this.errors = new ArrayList<>();
    }

    public String convertToJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> serializedList = new ArrayList<>();

        invocations.forEach((entry, count) -> {
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("owner", entry.owner());
            entryMap.put("name", entry.name());
            entryMap.put("desc", entry.desc());
            entryMap.put("isInstance", entry.isInstance());
            entryMap.put("invokeId", entry.invokeId());
            entryMap.put("isSymbolic", entry.isSymbolic());
            entryMap.put("count", count);  // Store the count as well

            serializedList.add(entryMap);
        });
        errors.forEach(error -> {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("message", error.message());
            errorMap.put("type", error.type());
            errorMap.put("stackTrace", error.stackTrace());
            errorMap.put("exceptionMessage", error.exceptionMessage());
            errorMap.put("executionStage", error.executionStage());

            serializedList.add(errorMap);
        });

        return mapper.writeValueAsString(serializedList);  // Convert the list of maps to JSON
    }
}

