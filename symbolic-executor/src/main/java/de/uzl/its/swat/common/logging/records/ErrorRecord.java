package de.uzl.its.swat.common.logging.records;

public record ErrorRecord(String message, String type, String stackTrace, String exceptionMessage, String executionStage) {
}
