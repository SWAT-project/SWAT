package de.uzl.its.swat.common.logging.records;

import java.util.ArrayList;

public record InvocationEntry(String owner, String name, String desc, boolean isInstance, long invokeId, boolean isSymbolic) {}
