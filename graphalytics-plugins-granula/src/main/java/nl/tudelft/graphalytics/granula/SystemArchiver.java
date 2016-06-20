package nl.tudelft.graphalytics.granula;

import nl.tudelft.granula.archiver.GranulaArchiver;
import nl.tudelft.granula.modeller.entity.BasicType;
import nl.tudelft.granula.modeller.job.JobModel;
import nl.tudelft.granula.modeller.job.Overview;
import nl.tudelft.granula.modeller.source.JobDirectorySource;

import java.nio.file.Path;

/**
 * Created by wlngai on 6/20/16.
 */
public class SystemArchiver {

    public void createSysArchive(Path benchmarkLogPath, Path usedArcPath, long startTime, long endTime, JobModel jobModel) {
        // archive

        Overview overview = new Overview();
        overview.setStartTime(startTime);
        overview.setEndTime(endTime);
        overview.setName("PGX.D Job");

        overview.setDescription("PGX.D is a graph processing engine by Oracle. " +
                "While conventional graph processing systems only allow vertices to ‘push’ (write) data to its neighbors, " +
                "PGX.D enables vertices to also ‘pull’ (read) data. Additionally, " +
                "PGX.D uses a fast cooperative context-switching mechanism and focuses on low-overhead, " +
                "bandwidth-efficient network communication.");
        archive(overview, benchmarkLogPath.toString(), usedArcPath.toString(), jobModel);
    }


    public void archive(Overview overview, String inputPath, String outputPath, JobModel jobModel) {
        JobDirectorySource jobDirSource = new JobDirectorySource(inputPath);
        jobDirSource.load();

        GranulaArchiver granulaArchiver = new GranulaArchiver(jobDirSource, jobModel, outputPath, BasicType.ArchiveFormat.JS);
        granulaArchiver.setOverview(overview);
        granulaArchiver.archive();

    }



}
