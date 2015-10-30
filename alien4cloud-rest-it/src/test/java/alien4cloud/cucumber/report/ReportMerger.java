package alien4cloud.cucumber.report;

import org.apache.commons.io.FileUtils;
import java.io.*;
import java.util.Collection;
import java.util.UUID;

public class ReportMerger {
    private static String reportFileName = "report.js";
    private static String reportImageExtension = "png";

    public static void main(String[] args) throws Throwable {
        File reportDirectory = new File(args[0]);
        if (reportDirectory.exists()) {
            ReportMerger munger = new ReportMerger();
            munger.mergeReports(reportDirectory);
        }
    }

    /**
     * Merge all reports together into master report in given reportDirectory
     * 
     * @param reportDirectory
     * @throws Exception
     */
    public void mergeReports(File reportDirectory) throws Throwable {
        Collection<File> existingReports = FileUtils.listFiles(reportDirectory, new String[] { "js" }, true);

        File mergedReport = null;

        for (File report : existingReports) {
            // only address report files
            if (report.getName().equals(reportFileName)) {
                // rename all the image files (to give unique names) in report directory and update report
                renameEmbededImages(report);

                // if we are on the first pass, copy the directory of the file to use as basis for merge
                if (mergedReport == null) {
                    FileUtils.copyDirectory(report.getParentFile(), reportDirectory);
                    mergedReport = new File(reportDirectory, reportFileName);
                    // otherwise merge this report into existing master report
                } else {
                    mergeFiles(mergedReport, report);
                }
            }
        }
    }

    /**
     * merge source file into target
     *
     * @param target
     * @param source
     */
    public void mergeFiles(File target, File source) throws Throwable {
        // copy embeded images
        Collection<File> embeddedImages = FileUtils.listFiles(source.getParentFile(), new String[] { reportImageExtension }, true);
        for (File image : embeddedImages) {
            FileUtils.copyFileToDirectory(image, target.getParentFile());
        }

        // merge report files
        String targetReport = FileUtils.readFileToString(target);
        String sourceReport = FileUtils.readFileToString(source);

        FileUtils.writeStringToFile(target, targetReport + sourceReport);
    }

    /**
     * Give unique names to embedded images to ensure they aren't lost during merge
     * Update report file to reflect new image names
     *
     * @param reportFile
     */
    public void renameEmbededImages(File reportFile) throws Throwable {
        File reportDirectory = reportFile.getParentFile();
        Collection<File> embeddedImages = FileUtils.listFiles(reportDirectory, new String[] { reportImageExtension }, true);

        String fileAsString = FileUtils.readFileToString(reportFile);

        for (File image : embeddedImages) {
            String curImageName = image.getName();
            String uniqueImageName = UUID.randomUUID().toString() + "." + reportImageExtension;

            image.renameTo(new File(reportDirectory, uniqueImageName));
            fileAsString = fileAsString.replace(curImageName, uniqueImageName);
        }

        FileUtils.writeStringToFile(reportFile, fileAsString);
    }
}
