package de.uzl.its.symbolic;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class Element implements Serializable {

    public int getIid() {
        return -1;
    }
}
