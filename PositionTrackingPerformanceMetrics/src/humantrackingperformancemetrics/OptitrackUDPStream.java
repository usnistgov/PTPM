
package humantrackingperformancemetrics;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class wrapping communication with optitrack.
 * The optitrack is a set of infrared strobe cameras used with special targets.
 * TrackingTools software from NaturalPoint will stream positions of the detected
 * targets using the NetNat protocol if so configured.
 * The class will start two threads. One for reading from the command port(1510) 
 * and the other for reading from the data port(1511). The only command than can
 * be sent is a ping which will prompt the TrackingTools software to reply with
 * the version number and name etc. The data port gets all the data that really
 * matters.  Client classes are expected to register a listener which will be 
 * called after a data frame is received and parsed. The stream always joins a
 * multicast group even though multicast only works on a directly connected 
 * computer. 
 * @author Will Shackleford<shackle@nist.gov>
 */
public class OptitrackUDPStream extends MonitoredConnection {

    public static boolean debug =false;
    
    private java.net.DatagramSocket dataSocket = null;
//    private DatagramPacket dataPacket = null;
    private java.net.DatagramSocket cmdSocket = null;
    private DatagramPacket cmdOutPacket = null;
    private DatagramPacket cmdResponsePacket = null;
    private String hostname;
    private InetAddress svrAddess;
    private InetAddress dataPacketAddress;
    private InetAddress groupAddess;
    private byte data_ba[] = new byte[20000];
    private byte cmd_out_ba[] = new byte[20000];
    private byte cmd_in_ba[] = new byte[20000];
    static final short NAT_PING = 0;
    static final short NAT_PINGRESPONSE = 1;
    static final short NAT_REQUEST = 2;
    static final short NAT_RESPONSE = 3;
    static final short NAT_REQUEST_MODELDEF = 4;
    static final short NAT_MODELDEF = 5;
    static final short NAT_REQUEST_FRAMEOFDATA = 6;
    static final short NAT_FRAMEOFDATA = 7;
    static final short NAT_MESSAGESTRING = 8;
    static final short NAT_UNRECOGNIZED_REQUEST = 100;
    static final String MULTICAST_ADDRESS = "239.255.42.99";
    
    public LinkedList<ActionListener> listeners = null;
    

    /**
     * Write int value to byte array ba at offset using little-endian.
     * @param ba byte array
     * @param offset offset in bytes
     * @param value value to store
     */
    public static void writeIntToByteArray(byte ba[], int offset, int value) {
        ba[offset + 0] = (byte) ((value) & 0xff);
        ba[offset + 1] = (byte) ((value >> 8) & 0xff);
        ba[offset + 2] = (byte) ((value >> 16) & 0xff);
        ba[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    /**
     * Read int value from byte array at offset assuming little-endian format.
     * @param ba byte array
     * @param offset offset in bytes
     * @return value from array
     */
    public static int readIntFromByteArray(byte ba[], int offset) {
        return (((int) ba[offset + 0]) & 0xff)
                | ((((int) ba[offset + 1]) << 8) & 0xff00)
                | ((((int) ba[offset + 2]) << 16) & 0xff0000)
                | ((((int) ba[offset + 3]) << 24) & 0xff000000);
    }

    /**
     * Write short value to byte array ba at offset using little-endian.
     * @param ba byte array
     * @param offset offset in bytes
     * @param value value to store
     */
    public static void writeShortToByteArray(byte ba[], int offset, short value) {
        ba[offset + 0] = (byte) ((value) & 0xff);
        ba[offset + 1] = (byte) ((value >> 8) & 0xff);
    }

    /**
     * Read short value from byte array at offset assuming little-endian format.
     * @param ba byte array
     * @param offset offset in bytes
     * @return value from array
     */
    public static short readShortFromByteArray(byte ba[], int offset) {
        return (short) (((short) ba[offset + 0])
                | (((short) ba[offset + 1]) << 8));
    }

    /**
     * Write float value to byte array ba at offset using little-endian.
     * @param ba byte array
     * @param offset offset in bytes
     * @param value value to store
     */
    public static void writeFloatToByteArray(byte ba[], int offset, float value) {
        int ivalue = Float.floatToRawIntBits(value);
        writeIntToByteArray(ba, offset, ivalue);
    }

    /**
     * Read float value from byte array at offset assuming little-endian format.
     * @param ba byte array
     * @param offset offset in bytes
     * @return value from array
     */
    public static float readFloatFromByteArray(byte ba[], int offset) {
        int ivalue = readIntFromByteArray(ba, offset);
        //if(debug) System.out.println(Integer.toHexString(ivalue));
        return Float.intBitsToFloat(ivalue);
    }

    private void readPacketFromCmdSocket() {
        try {
            cmdSocket.receive(cmdResponsePacket);
            if(debug) System.out.println("cmdResponsePacket = " + cmdResponsePacket);
            unpack(cmdResponsePacket.getData());
        } catch (Exception exception) {
            if (!ignore_errors) {
                exception.printStackTrace();
            }
        }
    }
    private volatile boolean ignore_errors = false;

    private void readPacketFromDataSocket() {
        try {
            DatagramPacket dataPacket = new DatagramPacket(data_ba, data_ba.length,
                    this.svrAddess, 1511);
            dataSocket.receive(dataPacket);
            if(debug) System.out.println("dataPacket = " + dataPacket);
            unpack(dataPacket.getData());
        } catch (Exception exception) {
            if (!ignore_errors) {
                exception.printStackTrace();
            }
        }
    }
    private Thread cmdSocketReaderThread = null;
    private Thread dataSocketReaderThread = null;
    
    
    private final boolean multicast;
    
    /**
     * Create a new stream object from which to get data from the optitrack.
     * 
     * @param _hostname Hostname or IP address of server running TrackingTools.
    */
    public OptitrackUDPStream(String _hostname, boolean _multicast) {
        this.multicast = _multicast;
        try {
            this.source = "optitack";
            this.hostname = _hostname;
            this.svrAddess = InetAddress.getByName(hostname);
            cmdSocket = new DatagramSocket();
            cmdOutPacket = new DatagramPacket(cmd_out_ba, cmd_out_ba.length,
                    this.svrAddess, 1510);
            cmdResponsePacket = new DatagramPacket(cmd_in_ba, cmd_in_ba.length,
                    this.svrAddess, 1510);
            if(!multicast) {
                dataSocket = cmdSocket;// new DatagramSocket();
                this.dataPacketAddress = this.svrAddess;
            } else {
            dataSocket = new MulticastSocket(1511);
            this.groupAddess = InetAddress.getByName(MULTICAST_ADDRESS);
//            NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
//            ((MulticastSocket)dataSocket).joinGroup(new InetSocketAddress(this.groupAddess,1511), networkInterface);
            ((MulticastSocket)dataSocket).joinGroup(this.groupAddess);
////            dataSocket = new DatagramSocket();
//            dataPacket = new DatagramPacket(data_ba, data_ba.length,
//                    this.groupAddess, 1511);
            
            this.dataPacketAddress = this.groupAddess;
            }
            cmdSocketReaderThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    
                    while (!Thread.currentThread().isInterrupted()) {
                        readPacketFromCmdSocket();
                    }
                }
            },"cmdSocketReader");
            dataSocketReaderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        readPacketFromDataSocket();
                    }
                }
            },"dataSocketReader");
            cmdSocketReaderThread.start();
            dataSocketReaderThread.start();
        } catch (Exception ex) {
            Logger.getLogger(OptitrackUDPStream.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Number of ping replies that have been received.
     */
    public static volatile int ping_count = 0;
    
    
    /**
     * Copy of the last data frame received.
     */
    public DataFrame last_frame_recieved =null;
    
    /**
     * Add a listener that will be called whenever a data frame is received.
     * @param al listener to add 
     */
    public void addListener(ActionListener al) {
        if(null == this.listeners) {
            this.listeners = new LinkedList<ActionListener>();
        }
        this.listeners.add(al);
    }
    
    /**
     * Remove a previously added listener.
     * @param al listener to remove
     */
    public void removeListener(ActionListener al) {
        if(null == this.listeners) {
            this.listeners = new LinkedList<ActionListener>();
        }
        this.listeners.remove(al);
    }
    

    /**
     * Class containing all data obtained in one packet from optitrack.
     */
    public class DataFrame {

        /**
         * frame number, seems to not to be implemented properly
         */
        public int frameNumber;
        
        /**
         * Number of marker sets in this frame.
         */
        public int nMarkerSets;
        
        /**
         * Array of marker set data from this frame.
         */
        public MarkerSet marker_set_array[];
        
        
        /**
         * Number of "other" markers in this frame.
         */
        public int nOtherMarkers;
        
        /**
         * Array of points of the locations of the "other" markers.
         */
        public Point3D other_markers_array[];
        
        
        /**
         * Number of rigid bodies with info in this frame.
         */
        public int nRigidBodies;
        
        
        /**
         * Array of rigid body data for this frame.
         */
        public RigidBody rigid_body_array[];
    }

    /**
     * Class representing the information associated with a marker set.
     */
    public class MarkerSet {

        /**
         * Name of the trackable as set within TrackingTools
         */
        public String name;
        
        /**
         * Number of markers in this set.
         */
        public int nMarkers;
        
        /**
         * Array of marker data for this marker set.
         */
        public Point3D points_array[];
    }

    /**
     * Class with information associated with a RigidBody.
     */
    public class RigidBody {

        /**
         * Identifier
         */
        public int ID;
        
        /**
         * Position of rigid body, this may be the centroid or just a point chosen
         * relative to the markers within the TrackingTools program.
         */
        public Point3D pos;
        
        /**
         * Orientation as  a quaternion
         */
        public float ori[];
        
        /**
         * Number of markers in this rigid body
         */
        public int nRigidMarkers=0;
        
        /**
         * Array of info for each marker.
         */
        public MarkerWithId rigid_markers_array[];
        
        /**
         * Mean error as computed by TrackingTools.
         */
        public float meanMarkerError;
    }
    
    /**
     * Class to add ID and size to Marker location.
     */
    public class MarkerWithId extends Point3D {
        /**
         * Identifier
         */
        public int ID;
        
        /**
         * Marker size.
         */
        public float sz;
        
        @Override
        public String toString() {
            return "[ID="+ID+",sz="+String.format("%.3f",sz)+"]"+super.toString();
        }
    }

    private DataFrame unpackFrameData(byte data[], 
            short iMessage, 
            int nDataBytes) {
        DataFrame df = new DataFrame();
        df.frameNumber = readIntFromByteArray(data, 4);
        if(debug) System.out.println("df.frameNumber = " + df.frameNumber);
        df.nMarkerSets = readIntFromByteArray(data, 8);
        if(debug) System.out.println("df.nMarkerSets = " + df.nMarkerSets);
        df.marker_set_array = new MarkerSet[df.nMarkerSets];
        int offset = 12;
        for (int i = 0; i < df.nMarkerSets; i++) {
            int name_offset = offset;
            while (data[offset] != 0 && offset < data.length) {
                offset++;
            }
            MarkerSet ms = new MarkerSet();
            df.marker_set_array[i] = ms;
            ms.name = new String(data, name_offset, (offset - name_offset+1));
            if(debug) System.out.println("name_offset = " + name_offset);
            if(debug) System.out.println("offset = " + offset);
            if(debug) System.out.println("df.marker_set_array[i].name = " + df.marker_set_array[i].name);
            offset++;
            df.marker_set_array[i].nMarkers = readIntFromByteArray(data, offset);
            if(debug) System.out.println("df.marker_set_array[i].nMarkers = " + df.marker_set_array[i].nMarkers);
            offset += 4;
            if (df.marker_set_array[i].nMarkers > 0) {
                df.marker_set_array[i].points_array = new Point3D[df.marker_set_array[i].nMarkers];
                for (int j = 0; j < df.marker_set_array[i].nMarkers; j++) {
                    df.marker_set_array[i].points_array[j] = new Point3D();
                    if(debug) System.out.println("offset = " + offset);
                    df.marker_set_array[i].points_array[j].x = readFloatFromByteArray(data, offset);
                    offset += 4;
                    df.marker_set_array[i].points_array[j].y = readFloatFromByteArray(data, offset);
                    offset += 4;
                    df.marker_set_array[i].points_array[j].z = readFloatFromByteArray(data, offset);
                    offset += 4;
                    if(debug) System.out.println("df.marker_set_array[i].points_array[j] = " + df.marker_set_array[i].points_array[j]);
                }
            }
        }
        df.nOtherMarkers = readIntFromByteArray(data, offset);
        if(debug) System.out.println("df.nOtherMarkers = " + df.nOtherMarkers);
        offset += 4;
        if (df.nOtherMarkers > 0) {
            LinkedList<Point3D> markersList = new LinkedList<Point3D>();
            for (int j = 0; j < df.nOtherMarkers; j++) {
                if(debug) System.out.println("offset = " + offset);
                float x = readFloatFromByteArray(data, offset);
                offset += 4;
                float y = readFloatFromByteArray(data, offset);
                offset += 4;
                float z = readFloatFromByteArray(data, offset);
                offset += 4;
                if(Math.abs(x) > 1e-4 && Math.abs(y) > 1e-4 && Math.abs(z) > 1e-4) {
                    markersList.add(new Point3D(x,y,z));
                    if(debug) System.out.println("df.other_markers_array[j] = " + df.other_markers_array[j]);
                }
            }
            df.other_markers_array = markersList.toArray(new Point3D[markersList.size()]);
        }
        if(debug) System.out.println("offset = " + offset);
        df.nRigidBodies = readIntFromByteArray(data, offset);
        if(debug) System.out.println("df.nRigidBodies = " + df.nRigidBodies);
        offset += 4;
        if (df.nRigidBodies > 0) {
            df.rigid_body_array = new RigidBody[df.nRigidBodies];
            for (int j = 0; j < df.nRigidBodies; j++) {
                RigidBody rb = new RigidBody();
                df.rigid_body_array[j] = rb;
                if(debug) System.out.println("offset = " + offset);
                rb.ID = readIntFromByteArray(data, offset);
                offset+=4;
                if(debug) System.out.println("rb.Id = " + rb.ID);
                float x = readFloatFromByteArray(data,offset);
                offset +=4;
                float y = readFloatFromByteArray(data,offset);
                offset +=4;
                float z = readFloatFromByteArray(data,offset);
                rb.pos = new Point3D(x,y,z);
                offset +=4;
                if(debug) System.out.println("rb.pos = " + rb.pos);
                rb.ori = new float[4];
                rb.ori[0] = readFloatFromByteArray(data,offset);
                offset +=4;
                rb.ori[1] = readFloatFromByteArray(data,offset);
                offset +=4;
                rb.ori[2] = readFloatFromByteArray(data,offset);
                offset +=4;
                rb.ori[3] = readFloatFromByteArray(data,offset);
                offset +=4;
                rb.nRigidMarkers = readIntFromByteArray(data,offset);
                if(debug) System.out.println("rb.nRigidMarkers = " + rb.nRigidMarkers);
                offset +=4;
                if(rb.nRigidMarkers > 0) {
                    rb.rigid_markers_array = new MarkerWithId[rb.nRigidMarkers];
                    for(int k = 0; k < rb.nRigidMarkers; k++) {
                        MarkerWithId marker = new MarkerWithId();
                        marker.x = readFloatFromByteArray(data,offset);
                        offset+=4;
                        marker.y = readFloatFromByteArray(data,offset);
                        offset+=4;
                        marker.z = readFloatFromByteArray(data,offset);
                        offset+=4;
                        rb.rigid_markers_array[k] = marker;
                    }
                    for(int k = 0; k < rb.nRigidMarkers; k++) {
                        MarkerWithId marker = rb.rigid_markers_array[k];
                        marker.ID = readIntFromByteArray(data,offset);
                        offset+=4;
                    }
                    for(int k = 0; k < rb.nRigidMarkers; k++) {
                        MarkerWithId marker = rb.rigid_markers_array[k];
                        marker.sz = readFloatFromByteArray(data,offset);
                        offset+=4;
                        if(debug) System.out.println("marker = " + marker);
                    }
                }
                rb.meanMarkerError = readFloatFromByteArray(data,offset);
                offset += 4;
                if(debug) System.out.println("rb.meanMarkerError = " + rb.meanMarkerError);
            }
        }
        return df;
    }

    /**
     * Unpack the data in the byte array to identify the message type and
     * interpret accordingly.
     * @param data data received from UDP socket
     */
    public void unpack(byte data[]) {
        short iMessage = readShortFromByteArray(data, 0);
        if(debug) System.out.println("iMessage = " + iMessage);
        int nDataBytes = readShortFromByteArray(data, 2);
        if(nDataBytes < 0) {
            nDataBytes = (1<<16)-nDataBytes;
        }
        if(debug) System.out.println("nDataBytes = " + nDataBytes);
        switch (iMessage) {
            case NAT_PINGRESPONSE:
                ping_count++;
                String szName = new String(data, 4, 256);
                if(debug) System.out.println("szName = " + szName);
                String version = new String(data, 260, 4);
                if(debug) System.out.println("version = " + version);
                String natnet_version = new String(data, 264, 4);
                if(debug) System.out.println("natnet_version = " + natnet_version);
                break;

            case NAT_FRAMEOFDATA:
                this.incUpdates();
                this.last_frame_recieved 
                        = unpackFrameData(data, iMessage, nDataBytes);
                break;
        }
        if(null != listeners) {
            ActionEvent ae = new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"New data");
            for(ActionListener al : this.listeners) {
                al.actionPerformed(ae);
            }
        }
    }

    /**
     * Send a request to TrackingTools software to return version number
     * and verify that it is working on the correct host.
     */
    public void sendPingRequest() {
        try {
            writeShortToByteArray(cmd_out_ba, 0, NAT_PING);
            writeShortToByteArray(cmd_out_ba, 2, (short) 0);
            cmdOutPacket.setLength(4);
            cmdSocket.send(cmdOutPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFrameRequest() {
        try {
            writeShortToByteArray(cmd_out_ba, 0, NAT_REQUEST_FRAMEOFDATA);
            writeShortToByteArray(cmd_out_ba, 2, (short) 0);
            cmdOutPacket.setLength(4);
            cmdSocket.send(cmdOutPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ping() {
        try {
            final int old_ping_count = ping_count;
            this.sendPingRequest();
            while (old_ping_count == ping_count) {
                if(debug) System.out.println("ping_count = " + ping_count);
                if(debug) System.out.println("old_ping_count = " + old_ping_count);
                Thread.sleep(1000);
                this.sendPingRequest();
            }
            if(debug) System.out.println("ping_count = " + ping_count);
            if(debug) System.out.println("old_ping_count = " + old_ping_count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean try_ping(int max_tries, long sleep_millis) {
        try {
            final int old_ping_count = ping_count;
            this.sendPingRequest();
            int tries = 0;
            while (old_ping_count == ping_count) {
                if(tries> max_tries) {
                    return false;
                }
                tries++;
                if(debug) System.out.println("ping_count = " + ping_count);
                if(debug) System.out.println("old_ping_count = " + old_ping_count);
                Thread.sleep(sleep_millis);
                this.sendPingRequest();
            }
            if(debug) System.out.println("ping_count = " + ping_count);
            if(debug) System.out.println("old_ping_count = " + old_ping_count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void getFrame() {
        try {
            final long old_updates = updates;
            this.sendFrameRequest();
            while (old_updates == updates) {
                if(debug) System.out.println("updates = " + updates);
                if(debug) System.out.println("old_updates = " + old_updates);
                Thread.sleep(1000);
                this.sendFrameRequest();
            }
            if(debug) System.out.println("updates = " + updates);
            if(debug) System.out.println("old_updates = " + old_updates);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        this.ignore_errors = true;
        if (null != dataSocket) {
            dataSocket.close();
            dataSocket = null;
        }
        if (null != cmdSocket) {
            cmdSocket.close();
            cmdSocket = null;
        }
        try {
            if (null != this.cmdSocketReaderThread) {
                this.cmdSocketReaderThread.interrupt();
                this.cmdSocketReaderThread.join();
                this.cmdSocketReaderThread = null;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            if (null != this.dataSocketReaderThread) {
                this.dataSocketReaderThread.interrupt();
                this.dataSocketReaderThread.join();
                this.dataSocketReaderThread = null;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    /**
     * This is not the main for the jar. The main for the jar is in 
     * HumanTrackingPerformanceMetrics. This main can be used to test optitrack
     * interface and this class only.
     * @param args Command line arguments are not used.
     */
    public static void main(String args[]) {
        debug=true;
        OptitrackUDPStream ots = new OptitrackUDPStream("129.6.39.54",true);
        ots.ping();
        while (ots.updates < 3) {
            if(debug) System.out.println("ots.updates = " + ots.updates);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ots.close();
    }
}
