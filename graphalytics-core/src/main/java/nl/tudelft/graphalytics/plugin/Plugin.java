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
package nl.tudelft.graphalytics.plugin;

import nl.tudelft.graphalytics.domain.Benchmark;
import nl.tudelft.graphalytics.domain.BenchmarkResult;
import nl.tudelft.graphalytics.domain.BenchmarkSuite;
import nl.tudelft.graphalytics.domain.BenchmarkSuiteResult;
import nl.tudelft.graphalytics.reporting.BenchmarkReportGenerator;

/**
 * Created by tim on 12/11/15.
 */
public interface Plugin {

	String getPluginName();

	String getPluginDescription();

	void preBenchmarkSuite(BenchmarkSuite benchmarkSuite);

	void preBenchmark(Benchmark nextBenchmark);

	void postBenchmark(Benchmark completedBenchmark, BenchmarkResult benchmarkResult);

	void postBenchmarkSuite(BenchmarkSuite benchmarkSuite, BenchmarkSuiteResult benchmarkSuiteResult);

	void preReportGeneration(BenchmarkReportGenerator reportGenerator);

	void shutdown();

}
