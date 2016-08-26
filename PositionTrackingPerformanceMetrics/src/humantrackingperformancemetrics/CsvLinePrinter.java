/*
 * This is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * 
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain. NIST Real-Time Control System software is an experimental
 * system. NIST assumes no responsibility whatsoever for its use by other
 * parties, and makes no guarantees, expressed or implied, about its
 * quality, reliability, or any other characteristic. We would appreciate
 * acknowledgement if the software is used. This software can be
 * redistributed and/or modified freely provided that any derivative works
 * bear some notice that they are derived from it, and any modified
 * versions bear some notice that they have been modified.
 * 
 */

package humantrackingperformancemetrics;

import java.io.PrintStream;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class CsvLinePrinter implements CsvLinePrinterInterface {

    /**
     * Print one line of a csv file using one track point.
     *
     * @param tp point to save
     * @param name ID of person/trackable to associate track point to.
     * @param ps print stream of open csv file.
     * @throws Exception
     */
    @Override
    public void printOneLine(TrackPoint tp, String name, long frameNumber, double timeSinceLastFrame, double remoteTimeStamp, PrintStream ps) throws Exception {
        //<timestamp>, <person ID>, <person centroid X>, <person centroid Y>, <person centroid Z>,<bounding box top center X>, <bounding box top center Y>,  <bounding box top center Z>, <X velocity>, <Y velocity>, <Z velocity>, <ROI width>, <ROI height>,confidence
        // ps.println("timestamp,personID,personcentroidX,personcentroidY,personcentroidZ,boundingboxtopcenterX,boundingboxtopcenterY,boundingboxtopcenterZ,Xvelocity,Yvelocity,Zvelocity,ROIwidth,ROIheight,confidence,radius");
        boolean have_orientation = tp.orientation != null && tp.orientation.length == 4;
        
        // Frame number,timestampFromSensor,timestampFromDataCollection,ObjectID,qx,qy,qz,qw,x,y,z,latency
        ps.println(frameNumber+","
                + remoteTimeStamp+","
                + tp.time + ","
                + name + ","
                + (have_orientation ? tp.orientation[0] : Double.NaN) + ","
                + (have_orientation ? tp.orientation[1] : Double.NaN) + "," 
                + (have_orientation ? tp.orientation[2] : Double.NaN) + ","
                + (have_orientation ? tp.orientation[3] : Double.NaN) + ","
                + tp.x + ","
                + tp.y + ","
                + tp.z + ","
                + tp.getLatency()
         );
//        ps.println(tp.time + ","
//                + name + ","
//                + tp.x + ","
//                + tp.y + ","
//                + tp.z + ","
//                + tp.x + ","
//                + tp.y + ","
//                + tp.z + ","
//                + tp.vel_x + ","
//                + tp.vel_y + ","
//                + tp.vel_z + ","
//                + tp.ROI_width + ","
//                + tp.ROI_height + ","
//                + tp.confidence + ","
//                + tp.radius + ","
//                + tp.source + ","
//                + tp.getLatency() + ","
//                + frameNumber + ","
//                + timeSinceLastFrame + ","
//                + remoteTimeStamp + ","
//                + (have_orientation ? tp.orientation[0] : Double.NaN) + ","
//                + (have_orientation ? tp.orientation[1] : Double.NaN) + ","
//                + (have_orientation ? tp.orientation[2] : Double.NaN) + ","
//                + (have_orientation ? tp.orientation[3] : Double.NaN) + ","
//        );
    }

    @Override
    public void printHeader(PrintStream ps) {
        ps.println("timestamp,personID,personcentroidX,personcentroidY,personcentroidZ,boundingboxtopcenterX,boundingboxtopcenterY,boundingboxtopcenterZ,Xvelocity,Yvelocity,Zvelocity,ROIwidth,ROIheight,confidence,radius,source,latency,frameNumber,timeSinceLastFrame,remoteTimeStamp,qx,qy,qz,qw");
    }

}
