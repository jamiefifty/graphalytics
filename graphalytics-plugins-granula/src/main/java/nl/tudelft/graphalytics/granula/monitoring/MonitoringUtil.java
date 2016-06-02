package nl.tudelft.graphalytics.granula.monitoring;

import nl.tudelft.graphalytics.granula.monitoring.json.JsonUtil;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MonitoringUtil {


    public static void createJobOp(String name, long startTime, long endTime, String outPath, String driverLog) {

        Path opdatajs =  Paths.get(outPath).resolve("job.js");

        JobData jobData = new JobData();
        jobData.name = "PGX.D Job visualizer [Proof of concepts v0.1]";
        jobData.description = "PGX.D is a graph processing engine by Oracle. " +
                "While conventional graph processing systems only allow vertices to ‘push’ (write) data to its neighbors, " +
                "PGX.D enables vertices to also ‘pull’ (read) data. Additionally, " +
                "PGX.D uses a fast cooperative context-switching mechanism and focuses on low-overhead, " +
                "bandwidth-efficient network communication.";
        jobData.startTime = String.valueOf(startTime);
        jobData.endTime = String.valueOf(endTime);
        jobData.addBreakDown("makespan", String.valueOf((endTime - startTime) / 1000));


        for (String line : driverLog.split("\n")) {
            if(line.contains("HostNames")) {
                String[] parts = line.split("\\s+");
                String nodes = parts[parts.length - 1];
                jobData.nodeSize = String.valueOf(nodes.split(",").length);
            }

            if(line.contains("GOLDEN_VALUES ")) {
                String[] parts = line.split("\\s+");
                String algText = parts[1];
                jobData.algorithm = algText.replace(",", "").split("=")[1];
                String datText = parts[2];
                jobData.dataset  = datText.replace(",", "").split("=")[1];
            }

            if(line.contains("Label propagation algorithm")) {
                jobData.algorithm = "label_propagation";
            }
        }


        String data = "var job = " + JsonUtil.toJson(jobData);
        data = data.substring(0, data.length() - 1) + "\n" +
                "    ,tree: jobOperations,\n" +
                "    usage: jobMetrics\n" +
                "}";
        FileUtil.writeFile(data, opdatajs);
    }


    public static void createMontioringArc(String inPath, String outPath) {
        System.out.println("inPath = " + inPath);
        System.out.println("outPath = " + outPath);


        (new File(outPath)).mkdirs();
        Path utildatajs =  Paths.get(outPath).resolve("jobarchive-ut.js");
        String data = "var jobMetrics = " + JsonUtil.toJson(createMetrics(inPath));
        data = data.replaceAll("\\{\"key", "\n\\{\"key");
        FileUtil.writeFile(data, utildatajs);

//        copyStaticFile("/granula/lib/bootstrap.css", outPath + "/lib");
//        copyStaticFile("/granula/lib/bootstrap.js", outPath + "/lib");
//        copyStaticFile("/granula/lib/d3.min.js", outPath + "/lib");
//        copyStaticFile("/granula/data.js", outPath);
//        copyStaticFile("/granula/granula.css", outPath);
//        copyStaticFile("/granula/index.html", outPath);
//        copyStaticFile("/granula/lib/jquery.js", outPath + "/lib");
//        copyStaticFile("/granula/lib/nv.d3.css", outPath + "/lib");
//        copyStaticFile("/granula/lib/nv.d3.js", outPath + "/lib");
//        copyStaticFile("/granula/view.js", outPath);
    }

    private static void copyStaticFile(String filePath, String outPath) {

        URL resourceUrl = MonitoringUtil.class.getResource(filePath);
        (new File(outPath)).mkdirs();
        try {
            FileUtils.copyInputStreamToFile(resourceUrl.openStream(), new File(outPath+ "/"+(new File(filePath).getName())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static List<MetricOutput> createMetrics(String inputPath) {


        List<MetricOutput> metricOutputs = new ArrayList<>();

        String rootPath = inputPath;
        Collection files = FileUtils.listFiles(new File(rootPath), new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY);


        for (Object f : files) {
            Path filePath = ((File) f).toPath();
            if (Files.isRegularFile(filePath) && !filePath.getFileName().toString().equals("success")
                    && filePath.toString().contains("1000ms")) {

                MetricOutput metricOutput = new MetricOutput();
                metricOutput.key = filePath.toAbsolutePath().toString().replaceAll(rootPath, "");
                if(metricOutput.key.startsWith("/")) {
                    metricOutput.key = metricOutput.key.substring(1, metricOutput.key.length());
                }

                try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] dp = line.split("\\s+");
                        metricOutput.addValue(dp[0], dp[1]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                metricOutputs.add(metricOutput);
            }

        }

        return metricOutputs;
    }

    public static void collectMonitoringData(String benchmarkId, String outpath) {

        CommandLine commandLine = new CommandLine("/var/scratch/wlngai/graphalytics-runner/debug/app/granula/sh/collect-data.sh");
        commandLine.addArgument(benchmarkId);
        commandLine.addArgument(outpath);

//        (new File(outpath)).mkdir();
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
}
