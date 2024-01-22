package de.uzl.its.swat.symbolicRegression;

import de.uzl.its.symbolic.value.Value;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class Node {


    @Getter
    @Setter
    String value;

    @Getter
    String type;

    @Getter
    List<Node> children = new ArrayList<>();

    @Getter
    @Setter
    Value output = null;

    Node(String value) {
        this.type = value;
    }

    void addChild(Node child) {
        children.add(child);
    }

}
