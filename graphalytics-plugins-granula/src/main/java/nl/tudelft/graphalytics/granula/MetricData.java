package nl.tudelft.graphalytics.granula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MetricData {
    String key;
    List values;

    public MetricData() {
        values = new ArrayList<>();
    }

    public void addValue(String timestamp, String value) {
        values.add(Arrays.asList(timestamp, value));
    }
}
