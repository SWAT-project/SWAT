package witness;

import java.util.HashMap;
import java.util.Map;

public class WitnessNode {

    private final int id;

    private final Map<String, String> data;

    public WitnessNode(int id) {
        this.id = id;
        this.data = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void addData(String key, String value) {
        this.data.put(key, value);
    }

}