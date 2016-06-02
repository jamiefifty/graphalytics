package nl.tudelft.graphalytics.granula.monitoring.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by wing on 27-11-14.
 */
public class JsonUtil {

    public static String toJson(Object object) {
        // http://stackoverflow.com/questions/4802887/gson-how-to-exclude-specific-fields-from-serialization-without-annotations
        Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusionStrategy()).create();
        return gson.toJson(object);
    }

    public static Object fromJson(String jsonString, Class clazz) {
        return (new Gson()).fromJson(jsonString, clazz);
    }

}
