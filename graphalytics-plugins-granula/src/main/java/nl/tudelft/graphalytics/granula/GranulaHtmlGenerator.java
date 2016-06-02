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

import nl.tudelft.graphalytics.domain.Benchmark;
import nl.tudelft.graphalytics.domain.BenchmarkResult;
import nl.tudelft.graphalytics.domain.BenchmarkSuiteResult;
import nl.tudelft.graphalytics.reporting.BenchmarkReportFile;
import nl.tudelft.graphalytics.reporting.html.HtmlBenchmarkReportGenerator;
import nl.tudelft.graphalytics.reporting.html.StaticResource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by tim on 12/17/15.
 */
public class GranulaHtmlGenerator implements HtmlBenchmarkReportGenerator.Plugin {

	private static final String STATIC_RESOURCES[] = new String[]{
			"lib/granula-visualizer/css/granula.css",
			"lib/granula-visualizer/js/chart.js",
			"lib/granula-visualizer/js/environmentview.js",
			"lib/granula-visualizer/js/operation-chart.js",
			"lib/granula-visualizer/js/view.js",
			"lib/granula-visualizer/js/util.js",
			"lib/granula-visualizer/js/overview.js",
			"lib/granula-visualizer/js/operationview.js",
			"lib/granula-visualizer/js/data.js",
			"lib/granula-visualizer/lib/underscore-min.js",
			"lib/granula-visualizer/lib/d3.min.js",
			"lib/granula-visualizer/lib/jquery.js",
			"lib/granula-visualizer/lib/nv.d3.css",
			"lib/granula-visualizer/lib/bootstrap.js",
			"lib/granula-visualizer/lib/snap.svg-min.js",
			"lib/granula-visualizer/lib/bootstrap.css",
			"lib/granula-visualizer/lib/nv.d3.js",
			"lib/granula-visualizer/visualizer.htm"
	};

	@Override
	public void preGenerate(HtmlBenchmarkReportGenerator htmlBenchmarkReportGenerator, BenchmarkSuiteResult result) {
		for (BenchmarkResult benchmarkResult : result.getBenchmarkResults()) {
			if (benchmarkResult.isCompletedSuccessfully()) {
				htmlBenchmarkReportGenerator.registerPageLink(benchmarkResult.getBenchmark(),
						"html/lib/granula-visualizer/visualizer.htm");
			}
		}
	}

	@Override
	public Collection<BenchmarkReportFile> generateAdditionalReportFiles(
			HtmlBenchmarkReportGenerator htmlBenchmarkReportGenerator, BenchmarkSuiteResult benchmarkSuiteResult) {
		List<BenchmarkReportFile> additionalFiles = new ArrayList<>(STATIC_RESOURCES.length);
		for (String resource : STATIC_RESOURCES) {
			URL resourceUrl = HtmlBenchmarkReportGenerator.class.getResource("/granula/reporting/html/" + resource);
			additionalFiles.add(new StaticResource(resourceUrl, resource));
		}
		return additionalFiles;
	}

}
