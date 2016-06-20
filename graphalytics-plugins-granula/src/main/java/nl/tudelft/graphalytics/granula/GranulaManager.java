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
import nl.tudelft.granula.modeller.system.System;
import nl.tudelft.graphalytics.Graphalytics;
import nl.tudelft.graphalytics.Platform;
import nl.tudelft.graphalytics.domain.Benchmark;
import nl.tudelft.graphalytics.domain.BenchmarkResult;
import nl.tudelft.graphalytics.domain.BenchmarkSuiteResult;
import nl.tudelft.graphalytics.granula.logging.GangliaLogger;
import nl.tudelft.graphalytics.granula.logging.UtilizationLogger;
import nl.tudelft.graphalytics.granula.util.json.JsonUtil;
import nl.tudelft.graphalytics.reporting.html.HtmlBenchmarkReportGenerator;
import nl.tudelft.graphalytics.reporting.html.StaticResource;
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
import java.net.URL;
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


	public static boolean isGranulaEnabled;
	public static boolean isLoggingEnabled;
	public static boolean isLogDataPreserved;
	public static boolean isArchivingEnabled;


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
		} catch (ConfigurationException e) {
			LOG.info("Could not find or load granula.properties.");
		}
		setModel(platform.getPerformanceModel());
	}

	public void generateArchive(BenchmarkSuiteResult benchmarkSuiteResult) throws IOException {


		// Ensure the log and archive directories exist
		Path logPath = reportDataPath.resolve("log");
		Path stdArcPath = reportDataPath.resolve("archive"); //not used atm
		Path usedArcPath = logPath.getParent().getParent().resolve("html").resolve("lib").resolve("granula-visualizer").resolve("data");
		Files.createDirectories(logPath);
		Files.createDirectories(stdArcPath);  //not used atm

		for (BenchmarkResult benchmarkResult : benchmarkSuiteResult.getBenchmarkResults()) {

			// make sure the log path(s) exists.
			Path logDataPath = logPath.resolve(benchmarkResult.getBenchmark().getBenchmarkIdentificationString());
			Files.createDirectories(logDataPath.resolve("OperationLog"));
			Files.createDirectories(logDataPath.resolve("UtilizationLog"));

			Benchmark benchmark = benchmarkResult.getBenchmark();
			long startTime = benchmarkResult.getStartOfBenchmark().getTime();
			long endTime = benchmarkResult.getEndOfBenchmark().getTime();
			String jobId = benchmark.getId();

			SystemArchiver systemArchiver = new SystemArchiver();
			systemArchiver.createSysArchive(logDataPath, usedArcPath, startTime, endTime, model);

			EnvironmentArchiver environmentArchiver = new EnvironmentArchiver();
			environmentArchiver.creatEnvArchive(logDataPath, usedArcPath, jobId);
		}
	}


	public static void generateFailedJobArchive(String rawlogPath, String arcPath, String benchmarkIdString, String benchmarkId, long startTime, long endTime, JobModel model) throws IOException {
		// Ensure the log and archive directories exist
		Path logPath = Paths.get(rawlogPath);
		Path usedArcPath = Paths.get(arcPath).resolve("data");
		Files.createDirectories(logPath);
		// make sure the log path(s) exists.

		Path logDataPath = logPath;
		Files.createDirectories(logDataPath.resolve("OperationLog"));
		Files.createDirectories(logDataPath.resolve("UtilizationLog"));

		String jobId = benchmarkId;

		SystemArchiver systemArchiver = new SystemArchiver();
		systemArchiver.createSysArchive(logDataPath, usedArcPath, startTime, endTime, model);

		EnvironmentArchiver environmentArchiver = new EnvironmentArchiver();
		environmentArchiver.creatEnvArchive(logDataPath, usedArcPath, jobId);


		for (String resource : GranulaHtmlGenerator.STATIC_RESOURCES) {
			URL resourceUrl = HtmlBenchmarkReportGenerator.class.getResource("/granula/reporting/html/" + resource);


			Path outputPath = Paths.get(arcPath).resolve(resource.replace("lib/granula-visualizer/", ""));
			if (!outputPath.getParent().toFile().exists()) {
				Files.createDirectories(outputPath.getParent());
			} else if (!outputPath.getParent().toFile().isDirectory()) {
				throw new IOException("Could not write static resource to \"" + outputPath + "\": parent is not a directory.");
			}
			// Copy the resource to the output file
			FileUtils.copyInputStreamToFile(resourceUrl.openStream(), outputPath.toFile());
		}

	}


	public void setModel(JobModel model) {
		this.model = model;
	}

	public void setReportDirPath(Path reportDataPath) {
		this.reportDataPath = reportDataPath;
	}

}
