/*
 * Copyright 2015 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.tudelft.graphalytics.granula;

import nl.tudelft.granula.archiver.GranulaArchiver;
import nl.tudelft.granula.modeller.entity.BasicType.ArchiveFormat;
import nl.tudelft.granula.modeller.job.JobModel;
import nl.tudelft.granula.modeller.job.Overview;
import nl.tudelft.granula.modeller.source.JobDirectorySource;
import nl.tudelft.graphalytics.domain.Benchmark;
import nl.tudelft.graphalytics.domain.BenchmarkResult;
import nl.tudelft.graphalytics.domain.BenchmarkSuiteResult;
import nl.tudelft.graphalytics.granula.logging.GangliaLogger;
import nl.tudelft.graphalytics.granula.logging.UtilizationLogger;
import nl.tudelft.graphalytics.granula.util.json.JsonUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * Created by wlngai on 10-9-15.
 */
public class GranulaManager {

	private static final Logger LOG = LogManager.getLogger();

	/**
	 * Property key for enabling or disabling Granula.
	 */
	private static final String GRANULA_ENABLED = "benchmark.run.granula.enabled";
	private static final String LOGGING_ENABLED = "benchmark.run.granula.logging-enabled";
	private static final String LOGGING_PRESERVED = "benchmark.run.granula.logging-preserved";
	private static final String ARCHIVING_ENABLED = "benchmark.run.granula.archiving-enabled";

	private static final String UTILIZATION_LOGGING_ENABLED = "benchmark.run.granula.utilization-logging-enabled";
	private static final String UTILIZATION_LOGGING_TOOL = "benchmark.run.granula.utilization-logging-tool";

	public static boolean isGranulaEnabled;
	public static boolean isLoggingEnabled;
	public static boolean isLogDataPreserved;
	public static boolean isArchivingEnabled;
	public static boolean isUtilLoggingEnabled;

	public static UtilizationLogger utilizationLogger;

	JobModel model;
	Path reportDataPath;

	public GranulaManager(GranulaAwarePlatform platform) {
		// Load Granula configuration
		PropertiesConfiguration granulaConfig;
		try {
			granulaConfig = new PropertiesConfiguration("granula.properties");
			isGranulaEnabled = granulaConfig.getBoolean(GRANULA_ENABLED, false);
			isLoggingEnabled = granulaConfig.getBoolean(LOGGING_ENABLED, false);
			isLogDataPreserved = granulaConfig.getBoolean(LOGGING_PRESERVED, false);
			isArchivingEnabled = granulaConfig.getBoolean(ARCHIVING_ENABLED, false);
			isUtilLoggingEnabled = granulaConfig.getBoolean(UTILIZATION_LOGGING_ENABLED, false);

			if(isGranulaEnabled) {
				LOG.info("Granula plugin is found, and is enabled.");
				LOG.info(String.format(" - Logging is %s for Granula.", (isLoggingEnabled) ? "enabled" : "disabled"));
				LOG.info(String.format(" - Archiving is %s for Granula.", (isArchivingEnabled) ? "enabled" : "disabled"));
				LOG.info(String.format(" - Logging data is %s after being used by Granula.", (isLogDataPreserved) ? "preserved" : "not preserved"));
			} else {
				LOG.info("Granula plugin is found, but is disabled.");
			}

			if (isArchivingEnabled && !isLoggingEnabled) {
				LOG.error(String.format("The archiving feature (%s) is not usable while logging feature (%s) is not enabled. " +
						"Turning off the archiving feature of Granula. ", ARCHIVING_ENABLED, LOGGING_ENABLED));
				isGranulaEnabled = false;
			}

			String utilToolName = granulaConfig.getString(UTILIZATION_LOGGING_TOOL);

			switch (utilToolName) {
				case "ganglia":
					utilizationLogger = new GangliaLogger();
					break;
				default:
					throw new IllegalArgumentException(String.format("%s is a valid utilization logging tool", utilToolName));
			}

		} catch (ConfigurationException e) {
			LOG.info("Could not find or load granula.properties.");
		}
		setModel(platform.getPerformanceModel());
	}

	public void archive(Overview overview, String inputPath, String outputPath) {
		JobDirectorySource jobDirSource = new JobDirectorySource(inputPath);
		jobDirSource.load();

		GranulaArchiver granulaArchiver = new GranulaArchiver(jobDirSource, model, outputPath, ArchiveFormat.JS);
		granulaArchiver.setOverview(overview);
		granulaArchiver.archive();
	}

	public void generateArchive(BenchmarkSuiteResult benchmarkSuiteResult) throws IOException {


		// Ensure the log and archive directories exist
		Path logPath = reportDataPath.resolve("log");
		Path archivePath = reportDataPath.resolve("archive");
		Files.createDirectories(logPath);
		Files.createDirectories(archivePath);

		for (BenchmarkResult benchmarkResult : benchmarkSuiteResult.getBenchmarkResults()) {

			// make sure the log path(s) exists.
			Path benchmarkLogPath = logPath.resolve(benchmarkResult.getBenchmark().getBenchmarkIdentificationString());
			Files.createDirectories(benchmarkLogPath.resolve("OperationLog"));
			Files.createDirectories(benchmarkLogPath.resolve("UtilizationLog"));

			long startTime = benchmarkResult.getStartOfBenchmark().getTime();
			long endTime = benchmarkResult.getEndOfBenchmark().getTime();
			Benchmark benchmark = benchmarkResult.getBenchmark();
//			String inputPath = "/local/wlngai/graphalytics/exec/graphalytics/utilization-log";
			String inputPath = benchmarkLogPath.resolve("UtilizationLog").toAbsolutePath().toString();
			collectMonitoringData(benchmark.getId(), inputPath);
			String jobName = String.format("[%s-%s]", benchmark.getAlgorithm().getName(),
					benchmark.getGraph().getName());

			Path arcPath = logPath.getParent().getParent().resolve("html")
					.resolve("lib").resolve("granula-visualizer").resolve("data");

			createMontioringArc(inputPath + "/" + benchmark.getId(), arcPath.toString());

			// archive
			Path newArchiveFile = logPath.getParent().getParent().resolve("html")
					.resolve("lib").resolve("granula-visualizer").resolve("data");

			Overview overview = new Overview();
			overview.setStartTime(startTime);
			overview.setEndTime(endTime);
			overview.setName("PGX.D Job");

			overview.setDescription("PGX.D is a graph processing engine by Oracle. " +
					"While conventional graph processing systems only allow vertices to ‘push’ (write) data to its neighbors, " +
					"PGX.D enables vertices to also ‘pull’ (read) data. Additionally, " +
					"PGX.D uses a fast cooperative context-switching mechanism and focuses on low-overhead, " +
					"bandwidth-efficient network communication.");
			archive(overview, benchmarkLogPath.toString(), newArchiveFile.toString());
		}
	}

	public void collectMonitoringData(String benchmarkId, String outpath) {

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

	public void setModel(JobModel model) {
		this.model = model;
	}

	public void setReportDirPath(Path reportDataPath) {
		this.reportDataPath = reportDataPath;
	}

}
