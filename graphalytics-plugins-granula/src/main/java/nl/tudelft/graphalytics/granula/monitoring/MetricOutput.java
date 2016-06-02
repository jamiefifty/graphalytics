package nl.tudelft.graphalytics.granula.monitoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wlngai on 5/30/16.
 */
public class MetricOutput {
    String key;
    List values;

    public MetricOutput() {
        values = new ArrayList<>();
    }

    public void addValue(String timestamp, String value) {
        values.add(Arrays.asList(timestamp, value));
    }
}
