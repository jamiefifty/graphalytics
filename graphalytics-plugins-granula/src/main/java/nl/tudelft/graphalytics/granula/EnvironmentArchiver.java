package nl.tudelft.graphalytics.granula;

import nl.tudelft.graphalytics.granula.util.json.JsonUtil;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by wlngai on 6/20/16.
 */
public class EnvironmentArchiver {


    public void creatEnvArchive(Path logDataPath, Path usedArcPath, String jobId) {

        String envLogPath = logDataPath.resolve("UtilizationLog").toAbsolutePath().toString();
        collectEnvData(envLogPath, jobId);
        createMontioringArc(envLogPath + "/" + jobId, usedArcPath.toString());
    }

    public void collectEnvData(String outpath, String benchmarkId) {

        CommandLine commandLine = new CommandLine("/var/scratch/wlngai/graphalytics-runner/debug/app/granula/sh/collect-data.sh");
        commandLine.addArgument(benchmarkId);
        commandLine.addArgument(outpath);

        System.out.println("Collect monitoring data with command line: " + commandLine);
        Executor executor = new DefaultExecutor();
        executor.setExitValues(null);
        try {
            executor.execute(commandLine);
            Thread.sleep(5000);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void createMontioringArc(String inPath, String outPath) {
        System.out.println("inPath = " + inPath);
        System.out.println("outPath = " + outPath);


        (new File(outPath)).mkdirs();
        Path utildatajs =  Paths.get(outPath).resolve("env-arc.js");
        String data = "var jobMetrics = " + JsonUtil.toJson(createMetrics(inPath));
        data = data.replaceAll("\\{\"key", "\n\\{\"key");
        nl.tudelft.graphalytics.granula.util.FileUtil.writeFile(data, utildatajs);
    }

    public List<MetricData> createMetrics(String inputPath) {


        List<MetricData> metricDatas = new ArrayList<>();

        String rootPath = inputPath;
        Collection files = FileUtils.listFiles(new File(rootPath), new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY);


        for (Object f : files) {
            Path filePath = ((File) f).toPath();
            if (Files.isRegularFile(filePath) && !filePath.getFileName().toString().equals("success")
                    && filePath.toString().contains("1000ms")) {

                MetricData metricData = new MetricData();
                metricData.key = filePath.toAbsolutePath().toString().replaceAll(rootPath, "");
                if(metricData.key.startsWith("/")) {
                    metricData.key = metricData.key.substring(1, metricData.key.length());
                }

                try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] dp = line.split("\\s+");
                        metricData.addValue(dp[0], dp[1]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                metricDatas.add(metricData);
            }

        }

        return metricDatas;
    }
}
