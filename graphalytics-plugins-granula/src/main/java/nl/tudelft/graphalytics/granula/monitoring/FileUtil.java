package nl.tudelft.graphalytics.granula.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.readAllBytes;

/**
 * Created by wlngai on 14-1-16.
 */
public class FileUtil {

    public static boolean fileExists(Path path) {
        return path.toFile().exists();
    }

    public static void writeFile(String content, Path path) {
        try {
            path.getParent().toFile().mkdirs();
            Files.write(path, content.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(Path path) {
        try {
            return new String(readAllBytes(path));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Can't read file at " + path);
        }
    }
}
