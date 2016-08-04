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

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import vicontojava.ViconClient;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class ViconDataStream extends MonitoredConnection {

    private ViconClient client = null;
    private final String host;
    private Thread thread;
    private List<TrackPoint> lastFrameList = null;
    private List<TrackPoint> lastUnlabeledFrameList = null;
    private boolean logUnlabeled = true;
    private long frameNumber;
    private double latency;

    @Override
    public void run() {
        try {
            System.out.println("ViconDataStream thread started.");
            client = new ViconClient();
            client.connect(host + ":801");
            client.enableCentroidData();
            client.enableMarkerData();
            client.enableSegmentData();
            client.enableUnlabeledMarkerData();
            long lastFrameNumber = -1;
            while (!Thread.currentThread().isInterrupted()) {
                List<TrackPoint> frameList = new ArrayList<>();
                List<TrackPoint> unlabledFrameList = null;
                if(this.logUnlabeled) {
                    unlabledFrameList = new ArrayList<>();
                }
                
                client.getFrame();
                long frameNumber = client.getFrameNumber();
                if (frameNumber == lastFrameNumber) {
                    Thread.sleep(20);
                    continue;
                }
                long diff = frameNumber - lastFrameNumber;
                if (lastFrameNumber > 0 && diff > 1) {
                    System.out.println("frameNumber = " + frameNumber);
                    System.out.println("lastFrameNumber = " + lastFrameNumber);
                    System.out.println("diff = " + diff);
                    setMissedFrames(getMissedFrames() + (diff - 1));
                }
                lastFrameNumber = frameNumber;
                double latency = client.getLatencyTotal();
                long subjectCount = client.getSubjectCount();
                for (int subjectIndex = 0; subjectIndex < subjectCount; subjectIndex++) {
                    String subjectName = client.getSubjectName(subjectIndex);
                    long segmentCount = client.getSegmentCount(subjectName);
                    for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
                        String segmentName = client.getSegmentName(subjectName, subjectIndex);
                        if (client.isOccluded(subjectName, segmentName)) {
                            continue;
                        }
                        double trans3[] = client.getSegmentGlobalTranslation(subjectName, segmentName);
                        double quat4[] = client.getSegmentGlobalRotationQuaternion(subjectName, segmentName);
                        TrackPoint tp = new TrackPoint(trans3[0], trans3[1], trans3[2], 0, 0, 0);
                        tp.setLatency(latency);
                        tp.source = "Vicon_" + host;
                        if (!subjectName.equals(segmentName)) {
                            tp.name = subjectName + "_" + segmentName;
                        } else {
                            tp.name = subjectName;
                        }
                        tp.orientation = new float[4];
                        for (int quatIndex = 0; quatIndex < 4; quatIndex++) {
                            tp.orientation[quatIndex] = (float) quat4[quatIndex];
                        }
                        frameList.add(tp);
                    }
                }
                if (this.logUnlabeled) {
                    try {
                        int unlabeledMarkerCount = client.getUnlabeledMarkerCount();
//                    System.out.println("unlabeledMarkerCount = " + unlabeledMarkerCount);
                        for (int unlabeledMarkerIndex = 0; unlabeledMarkerIndex < unlabeledMarkerCount; unlabeledMarkerIndex++) {
//                        System.out.println("unlabeledMarkerIndex = " + unlabeledMarkerIndex);
                            double markerTranslation[] = client.getUnlabeledMarkerGlobalTranslation(unlabeledMarkerIndex);
//                        System.out.println("markerTranslation = " + Arrays.toString(markerTranslation));
                            TrackPoint tp = new TrackPoint(markerTranslation[0], markerTranslation[1], markerTranslation[2], 0, 0, 0);
                            tp.setLatency(latency);
                            tp.source = "Vicon_" + host;
                            tp.name = "unlabeledMarker" + unlabeledMarkerIndex;
                            unlabledFrameList.add(tp);
                        }
                    } catch (ViconClient.ViconSdkException viconSdkException) {

                        viconSdkException.printStackTrace();
//                        System.out.println("Exception occured reading unlabeled marker measurments:" + viconSdkException.getMessage());

                    }
                }
//                TrackPoint tp = new TrackPoint(1.0, 2.0, 3.0, 0, 0, 0);
//                tp.setLatency(latency);
//                tp.source = "Vicon_" + host;
//                tp.name = "testPoint";
//                frameList.add(tp);
                synchronized (this) {
                    this.lastFrameList = frameList;
                    this.lastUnlabeledFrameList = unlabledFrameList;
                    this.frameNumber = frameNumber;
                    this.latency = latency;
                }
                this.incUpdates();
                if (null != listeners) {
                    for (Runnable r : listeners) {
                        r.run();
                    }
                }
            }
        } catch (Exception exception) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "Failed to connect to Vicon host=" + host + " : " + exception.getMessage());
                }
            });
            exception.printStackTrace();
        }
    }

    public ViconDataStream(String host) {
        this.host = host;
        setSource("Vicon:" + host);
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    private double timeSinceLastRecvTime = -0.001;
    private double localRecvTime = -0.001;
    private double lastLocalRecvTime = -0.001;
    private double firstUpdateTime = -0.001;
    private int updates = 0;

    private final TrackPoint nanTrackPoint = new TrackPoint(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

    @Override
    public void updateData(ConnectionUpdate update) throws Exception {
        double time = System.currentTimeMillis() * 1e-3;
        timeSinceLastRecvTime = time - lastLocalRecvTime;
        localRecvTime = time;
        lastLocalRecvTime = time;
        List<TrackPoint> frameList = null;
        List<TrackPoint> unlabeledFrameList = null;
        long frameNumber = -1;
        double latency = Double.NaN;
        synchronized (this) {
            frameList = this.lastFrameList;
            unlabeledFrameList = this.lastUnlabeledFrameList;
            frameNumber = this.frameNumber;
            latency = this.latency;
            this.logUnlabeled = update.isAddUnaffiliatedMarkers();
        }
        if (update.isAddNewFrameLines()) {
            try {
                nanTrackPoint.time = time;
                update.getCsvLinePrinter().printOneLine(nanTrackPoint, "new_frame", frameNumber, localRecvTime, 0.0, update.getPrintStream());
            } catch (Exception ex) {
                Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (update.isAddUnaffiliatedMarkers()) {
            try {
                if (null != unlabeledFrameList
                        && unlabeledFrameList.size() > 0) {
                    Track unaffiliated_track = update.getUnaffiliatedTrack();
                    if (null == unaffiliated_track) {
                        unaffiliated_track = new Track();
                        unaffiliated_track.name = "vicon_unaffiliated_track";
                        unaffiliated_track.source = "Vicon_" + host;
                        unaffiliated_track.disconnected = true;
                        List<Track> curDeviceTracks = update.getCurrentDeviceTracks();
                        if (null == curDeviceTracks) {
                            curDeviceTracks = new LinkedList<Track>();
                        }
                        curDeviceTracks.add(unaffiliated_track);
                        if (isGroundtruth()) {
                            List<Track> gtlist = update.getGtlist();
                            if (null == gtlist) {
                                gtlist = new LinkedList<Track>();
                                update.setGtlist(gtlist);
                            }
                            gtlist.add(unaffiliated_track);
                        } else {
                            List<Track> sutlist = update.getGtlist();
                            if (null == sutlist) {
                                sutlist = new LinkedList<Track>();
                                update.setSutlist(sutlist);
                            }
                            sutlist.add(unaffiliated_track);
                        }
                        update.setCurrentDeviceTracks(curDeviceTracks);
                    }
                    for (TrackPoint tp : unlabeledFrameList) {
                        tp.time = time;
                        tp.setLatency(latency);
                        if (isApplyTransform()) {
                            tp.applyTransform(getTransform());
                            unaffiliated_track.setTransform(getTransform());
                        }
                        if (null == unaffiliated_track.data) {
                            unaffiliated_track.data = new ArrayList<TrackPoint>();
                            unaffiliated_track.disconnected = true;
                        }
                        unaffiliated_track.data.add(tp);
                        if (null != update.getPrintStream()) {
                            update.getCsvLinePrinter()
                                    .printOneLine(tp,
                                            unaffiliated_track.name,
                                            frameNumber,
                                            timeSinceLastRecvTime,
                                            Double.NaN,
                                            update.getPrintStream());
                        }
                        if (unaffiliated_track.data.size() > 200) {
                            unaffiliated_track.data.remove(0);
                        }
                        unaffiliated_track.cur_time_index = unaffiliated_track.data.size() - 1;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        if (updates < 10) {
            firstUpdateTime = time;
        }
        updates++;
        boolean point_updated = false;
        double timeCollecting = 1e-12 + time - firstUpdateTime;
        double fps = (updates - 9) / timeCollecting;
        update.setLabel(String.format("latency = %.3f ms,\n timeSinceLastRecvTime=%.3f,\n timeCollecting=%.3f,framesPerSecond= %.3f, updates=%d,numRigidBodies=%d",
                latency,
                timeSinceLastRecvTime,
                timeCollecting,
                fps,
                updates,
                frameList.size()));
//        try {
        for (TrackPoint tp : frameList) {
            boolean new_update
                    = updatePoint(update, tp);
            point_updated = point_updated || new_update;

        }
//        } catch (Exception ex) {
//            close();
//            ods = null;
//            jCheckBoxMenuItemOptitrackVicon.setSelected(false);
//            stopRecording();
//            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
//            myShowMessageDialog(this,
//                    "Failure encountered updating or recording optitrack data.");
//        }
    }

    private boolean updatePoint(ConnectionUpdate update, TrackPoint pt) throws Exception {
        boolean point_updated = false;

        List<Track> allTracks = update.getAllTracks();
        if (null == allTracks) {
            allTracks = new ArrayList<Track>();
            update.setAllTracks(allTracks);
        }
        List<Track> myTracks = update.getCurrentDeviceTracks();
        if (null == myTracks) {
            myTracks = new LinkedList<Track>();
            update.setCurrentDeviceTracks(myTracks);
        }
        Track curTrack = null;
        String rb_name = pt.name;
        for (Track t : myTracks) {
            if (t.name.compareTo(rb_name) == 0) {
                curTrack = t;
                break;
            }
        }
        if (null == curTrack) {
            curTrack = new Track();
            curTrack.source = "Vicon";
            curTrack.name = pt.name;
            curTrack.is_groundtruth = isGroundtruth();
            HTPM_JFrame.Settings settings = update.getSettings();
            if (null != settings) {
                if (isGroundtruth()) {
                    curTrack.setInterpolatonMethod(settings.gtInterpMethod);
                } else {
                    curTrack.setInterpolatonMethod(settings.sutInterpMethod);
                }
            }
            myTracks.add(curTrack);

            if (isGroundtruth()) {
                List<Track> gtlist = update.getGtlist();
                if (null == gtlist) {
                    gtlist = new ArrayList<Track>();
                    update.setGtlist(gtlist);
                }
                gtlist.add(curTrack);
            } else {
                List<Track> sutlist = update.getGtlist();
                if (null == sutlist) {
                    sutlist = new ArrayList<Track>();
                    update.setSutlist(sutlist);
                }
                sutlist.add(curTrack);
            }
            allTracks.add(curTrack);
            Runnable newTrackRunnable = update.getNewTrackRunnable();
            if (null != newTrackRunnable) {
                newTrackRunnable.run();
            }
//            //this.drawPanel1.tracks.add(optitrack_track);
//            EventQueue.invokeLater(new Runnable() {
//                @Override
//                public void run() {
//                    updateEverything();
//                    //System.out.println("new optitrack track");
//                }
//            });
        }
//        if (optitrack_track.currentPoint != null
//                && optitrack_track.currentPoint.distance(pt) < 0.001) {
//            return false;
//        }
        TrackPoint tp = pt;
//        System.out.println("pre transfrom tp = " + tp);
        if (isApplyTransform()) {
            tp.applyTransform(getTransform());
            curTrack.setTransform(getTransform());
        }
//        System.out.println("post transform tp = " + tp);
        tp.time = System.currentTimeMillis() * 1e-3;
        tp.confidence = 1.0;
        curTrack.selected = true;

        curTrack.currentPoint = tp;
        curTrack.pointColor = Color.RED;
        curTrack.lineColor = Color.RED;
        if (null == curTrack.data) {
            curTrack.data = new ArrayList<>();
        }
        curTrack.data.add(tp);
        if (null != update.getPrintStream()) {
            update.getCsvLinePrinter()
                    .printOneLine(tp, curTrack.name, this.frameNumber, this.timeSinceLastRecvTime, 0.0, update.getPrintStream());
        }
        while (curTrack.data.size() > 200) {
            curTrack.data.remove(0);
        }
        curTrack.cur_time_index = curTrack.data.size() - 1;
        //setCurrentTime(tp.time + 0.00001);
        return true;
    }

    @Override
    public void close() {
        super.close();
        if (null != thread) {
            thread.interrupt();
            try {
                thread.join(100);

            } catch (InterruptedException ex) {
                Logger.getLogger(ViconDataStream.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != client) {
            client.close();
            client = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
