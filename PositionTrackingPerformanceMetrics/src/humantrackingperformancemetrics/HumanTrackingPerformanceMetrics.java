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
                HTPM_JFrame.LoadGroundTruthFile(args[i+1]);
                i++;
            } else if (args[i].compareTo("--copyandprocess")== 0) {
                HTPM_JFrame.CopyAndProcessTrackCsv(args[i+1], args[i+2]);
                i+=2;
            } else if (args[i].compareTo("--settings")== 0) {
                HTPM_JFrame.settings_file = new File(args[i+1]);
                HTPM_JFrame.s = HTPM_JFrame.readSettings(HTPM_JFrame.settings_file);
                i++;
            } else if (args[i].compareTo("--sut")== 0) {
                HTPM_JFrame.LoadSystemUnderTestFile(args[i+1]);
                i++;
            } else if (args[i].compareTo("--sut_time_offset")== 0) {
                HTPM_JFrame.sut_time_offset = Double.valueOf(args[i+1]);
                i++;
            } else if (args[i].compareTo("--gt_time_offset")== 0) {
                HTPM_JFrame.gt_time_offset = Double.valueOf(args[i+1]);
                i++;
            } else if (args[i].compareTo("--confidence")== 0) {
                HTPM_JFrame.s.confidence_threshold = Double.valueOf(args[i+1]);
                i++;
            } else if (args[i].compareTo("--interpolate")== 0) {
                HTPM_JFrame.interpolate(new File(args[i+1]));
                i++;
            } else if (args[i].compareTo("--transform")== 0) {
                HTPM_JFrame.loadDefaultTransform(new File(args[i+1]));
                i++;
            } else if (args[i].compareTo("--time_inc")== 0) {
                HTPM_JFrame.s.time_inc = Double.valueOf(args[i+1]);
                i++;
            }else if (args[i].compareTo("--sutInterpolateMethod")== 0) {
                HTPM_JFrame.s.sutInterpMethod = InterpolationMethodEnum.valueOf(args[i+1]);
                i++;
            }else if (args[i].compareTo("--gtInterpolateMethod")== 0) {
                HTPM_JFrame.s.gtInterpMethod = InterpolationMethodEnum.valueOf(args[i+1]);
                i++;
            } else if (args[i].compareTo("--time_scale")== 0) {
                CsvParseOptions.DEFAULT.TIME_SCALE = Double.valueOf(args[i+1]);
                i++;
            } else if (args[i].compareTo("--dist_scale")== 0) {
                CsvParseOptions.DEFAULT.DISTANCE_SCALE = Double.valueOf(args[i+1]);
                i++;
            } else if (args[i].compareTo("--writeCombinedGT")== 0) {
                HTPM_JFrame.saveCombinedFile(HTPM_JFrame.gtlist, args[i+1]);
                i++;
            }else if (args[i].compareTo("--writeCombinedSUT")== 0) {
                HTPM_JFrame.saveCombinedFile(HTPM_JFrame.sutlist, args[i+1]);
                i++;
            } else if (args[i].compareTo("--findMatches")== 0) {
                HTPM_JFrame.findMatches(new File(args[i+1]), new File(args[i+2]), new File(args[i+3]));
                i +=3;
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
            } else {
                System.err.println("Unrecognized argument :"+args[i]);
                System.exit(1);
            }
        }
    }
}
