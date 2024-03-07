package de.uzl.its.swat.symbolic.trace;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Base class for all elements in the symbolic trace.
 */
@Setter
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
class Element implements Serializable {
    // Unique identifier for the element
    private int iid;

}
