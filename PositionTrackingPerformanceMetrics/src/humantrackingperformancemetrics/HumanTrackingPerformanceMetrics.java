/*
 * Class with main for jar.
 */
package humantrackingperformancemetrics;

import java.io.File;

/**
 * Class with main for jar.
 * @author Will Shackleford<shackle@nist.gov>
 */
public class HumanTrackingPerformanceMetrics {

    /**
     * main function
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if(args.length < 2) {
            HTPM_JFrame.main(args);
            return;
        }
        for(int i = 0; i <  args.length; i++) {
            if(args[i].compareTo("--gui")== 0) {
                HTPM_JFrame.main(args);
                break;
            } else if (args[i].compareTo("--gt")== 0) {
                HTPM_JFrame.LoadGroundTruthFile(args[i+1],CsvParseOptions.DEFAULT);
                i++;
            } else if (args[i].compareTo("--settings")== 0) {
                HTPM_JFrame.settings_file = new File(args[i+1]);
                HTPM_JFrame.s = HTPM_JFrame.readSettings(HTPM_JFrame.settings_file);
                i++;
            } else if (args[i].compareTo("--sut")== 0) {
                HTPM_JFrame.LoadSystemUnderTestFile(args[i+1],CsvParseOptions.DEFAULT);
                i++;
            } else if (args[i].compareTo("--confidence")== 0) {
                HTPM_JFrame.s.confidence_threshold = Double.valueOf(args[i+1]);
                i++;
            } else if (args[i].compareTo("--process")== 0) {
                FrameStats fs = HTPM_JFrame.processAll();
                System.out.println("TRUE_Positive="+fs.true_occupied_area/fs.total_gt_occupied_area);
                System.out.println("False_Positive="+fs.false_occupied_area/fs.total_gt_occupied_area);
                System.out.println("fs.avg_sut_to_gt_dist = " + fs.avg_sut_to_gt_dist);
                System.out.println("fs.avg_gt_to_sut_dist = " + fs.avg_gt_to_sut_dist);
                System.out.println("fs.max_sut_to_gt_dist = " + fs.max_sut_to_gt_dist);
                System.out.println("fs.max_gt_to_sut_dist = " + fs.max_gt_to_sut_dist);
            } else if (args[i].compareTo("--roc")== 0) {
                System.out.println("True_Positives,False_Positives");
                for(HTPM_JFrame.s.confidence_threshold = 0.0 ;
                        HTPM_JFrame.s.confidence_threshold <= 1.0; 
                        HTPM_JFrame.s.confidence_threshold += 0.1) { 
                    FrameStats fs = HTPM_JFrame.processAll();
                    System.out.println(""+fs.true_occupied_area/fs.total_gt_occupied_area
                            +","+fs.false_occupied_area/fs.total_gt_occupied_area);
                }
            }
        }
    }
}
