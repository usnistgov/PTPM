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

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class DeviceSetupOptions {

    private final String host;
    private final String versionString;
    private final boolean multicast;
    private final boolean groundtruth;
    private final boolean startRecording;
    private final DeviceTypeEnum deviceType;

    public DeviceTypeEnum getDeviceType() {
        return deviceType;
    }

    public String getHost() {
        return host;
    }

    public String getVersionString() {
        return versionString;
    }

    public boolean isMulticast() {
        return multicast;
    }

    public boolean isGroundtruth() {
        return groundtruth;
    }

    public boolean isStartRecording() {
        return startRecording;
    }

    
    public DeviceSetupOptions(String host, 
            String versionString, 
            boolean multicast, 
            boolean groundtruth, 
            boolean startRecording,
            DeviceTypeEnum deviceType) {
        this.host = host;
        this.versionString = versionString;
        this.multicast = multicast;
        this.groundtruth = groundtruth;
        this.startRecording = startRecording;
        this.deviceType = deviceType;
    }

    
}
