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

import java.util.logging.Level;
import java.util.logging.Logger;
import vicon.client.Client;
import vicon.client.Output_Connect;
import vicon.client.Output_GetSegmentCount;
import vicon.client.Output_GetSubjectCount;
import vicon.client.StringResult;
import vicon.client.ViconClient;
import static vicon.client.ViconDataStreamSDK.CPP.Result.Enum.Success;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class ViconDataStream extends MonitoredConnection {

    static {
        System.loadLibrary("ViconJavaSwigLibrary");
    }
    
    private Client client = null;
    private final String host;
    private Thread thread;
    
    @Override
    public void run() {
        try {
            System.out.println("ViconDataStream thread started.");
            client = new Client();
            Output_Connect outputConnect = client.Connect(host + ":801");
            System.out.println("outputConnect.getResult() = " + outputConnect.getResult());
            client.EnableCentroidData();
            client.EnableMarkerData();
            client.EnableSegmentData();
            client.EnableUnlabeledMarkerData();
            long lastFrameNumber = -1;
            while (!Thread.currentThread().isInterrupted()
                    && client.GetFrame().getResult() == Success) {
                long frameNumber = client.GetFrameNumber().getFrameNumber();
                if (frameNumber == lastFrameNumber) {
                    Thread.sleep(20);
                    continue;
                }
                long diff= frameNumber - lastFrameNumber;
                if(lastFrameNumber > 0 && diff > 1) {
                    System.out.println("frameNumber = " + frameNumber);
                    System.out.println("lastFrameNumber = " + lastFrameNumber);
                    System.out.println("diff = " + diff);
                    setMissedFrames(getMissedFrames()+(diff-1));
                }
                lastFrameNumber = frameNumber;
                Output_GetSubjectCount output_GetSubjectCount = client.GetSubjectCount();
                if(output_GetSubjectCount.getResult() == Success) {
                    long subjectCount = output_GetSubjectCount.getSubjectCount();
                    for(int subjectIndex = 0; subjectIndex < subjectCount; subjectIndex++) {
                        StringResult sr = ViconClient.getSubjectName(client, subjectIndex);
                        String name = sr.getStdString();
                        sr.delete();
                        Output_GetSegmentCount output_GetSegmentCount = client.GetSegmentCount(name);
                        if(output_GetSegmentCount.getResult() == Success) {
                            long segmentCount = output_GetSegmentCount.getSegmentCount();
                            for(int segmentIndex =0; segmentIndex < segmentCount; segmentIndex++) {
                                sr.
                            }
                        }
                    }
                }
                this.incUpdates();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    
    
    public ViconDataStream(String host) {
        this.host = host;
        setSource("Vicon:"+host);
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() {
        super.close(); 
        if(null != thread) {
            thread.interrupt();
            try {
                thread.join(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(ViconDataStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(null != client) {
            client.Disconnect();
            client.delete();
            client = null;
        }
    }

    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize(); 
    }
    
    


}
