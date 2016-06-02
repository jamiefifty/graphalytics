package nl.tudelft.graphalytics.granula.monitoring;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wlngai on 6/2/16.
 */
public class JobData {
    String name;
    String description;
    String startTime;
    String endTime;
    String nodeSize;
    String threadSize;
    String algorithm;
    String dataset;
    List breakDown;

    public JobData() {
        this.breakDown = new ArrayList();
    }

    public void addBreakDown(String lable, String value) {
        breakDown.add(new KeyPair(lable, value));
    }

    private class KeyPair {

        public KeyPair(String label, String value) {
            this.label = label;
            this.value = value;
        }

        String label;
        String value;
    }
}
