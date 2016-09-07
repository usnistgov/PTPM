package humantrackingperformancemetrics;

import humantrackingperformancemetrics.HTPM_JFrame.Settings;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class wrapping communication with optitrack. The optitrack is a set of
 * infrared strobe cameras used with special targets. TrackingTools software
 * from NaturalPoint will stream positions of the detected targets using the
 * NetNat protocol if so configured. The class will start two threads. One for
 * reading from the command port(1510) and the other for reading from the data
 * port(1511). The only command than can be sent is a ping which will prompt the
 * TrackingTools software to reply with the version number and name etc. The
 * data port gets all the data that really matters. Client classes are expected
 * to register a listener which will be called after a data frame is received
 * and parsed. The stream always joins a multicast group even though multicast
 * only works on a directly connected computer.
 *
 *
 * For Documentation on format check the PacketClient.cpp sample from the NetNat
 * SDK.
 *
 * https://www.optitrack.com/downloads/developer-tools.html#natnet-sdk
 *
 *
 * @author Will Shackleford<shackle@nist.gov>
 */
public class OptitrackUDPStream extends MonitoredConnection {

    public static boolean debug = false;

    private final int major;

    private final int minor;

    /**
     * Get the value of netNatVersionMinor
     *
     * @return the value of netNatVersionMinor
     */
    public int getNetNatVersionMinor() {
        return minor;
    }

    /**
     * Get the value of netNatVersionMajor
     *
     * @return the value of netNatVersionMajor
     */
    public int getNetNatVersionMajor() {
        return major;
    }

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

    /**
     * Write int value to byte array ba at offset using little-endian.
     *
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
     *
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
     * Read int value from byte array at offset assuming little-endian format.
     *
     * @param ba byte array
     * @param offset offset in bytes
     * @return value from array
     */
    public static long readLongFromByteArray(byte ba[], int offset) {
        return (((long) ba[offset + 0]) & 0xff)
                | ((((long) ba[offset + 1]) << 8) & 0xff00)
                | ((((long) ba[offset + 2]) << 16) & 0xff0000)
                | ((((long) ba[offset + 3]) << 24) & 0xff000000)
                | ((((long) ba[offset + 4]) << 32) & 0xff00000000L)
                | ((((long) ba[offset + 5]) << 40) & 0xff0000000000L)
                | ((((long) ba[offset + 6]) << 48) & 0xff000000000000L)
                | ((((long) ba[offset + 7]) << 56) & 0xff00000000000000L);
    }

    /**
     * Write short value to byte array ba at offset using little-endian.
     *
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
     *
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
     *
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
     *
     * @param ba byte array
     * @param offset offset in bytes
     * @return value from array
     */
    public static float readFloatFromByteArray(byte ba[], int offset) {
        int ivalue = readIntFromByteArray(ba, offset);
        //if(debug) System.out.println(Integer.toHexString(ivalue));
        return Float.intBitsToFloat(ivalue);
    }

    /**
     * Read float value from byte array at offset assuming little-endian format.
     *
     * @param ba byte array
     * @param offset offset in bytes
     * @return value from array
     */
    public static double readDoubleFromByteArray(byte ba[], int offset) {
        long lvalue = readLongFromByteArray(ba, offset);
        //if(debug) System.out.println(Integer.toHexString(ivalue));
        return Double.longBitsToDouble(lvalue);
    }

    private void readPacketFromCmdSocket() {
        try {
            cmdSocket.receive(cmdResponsePacket);
            if (debug) {
                System.out.println("cmdResponsePacket = " + cmdResponsePacket);
            }
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
                    svrAddess, 1511);
            dataSocket.receive(dataPacket);
            if (debug) {
                System.out.println("dataPacket = " + dataPacket);
            }
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
    public OptitrackUDPStream(String _hostname, boolean _multicast, final int netNatMajor, final int netNatMinor) {
        multicast = _multicast;
        major = netNatMajor;
        minor = netNatMinor;
        try {
            source = "optitack";
            hostname = _hostname;
            svrAddess = InetAddress.getByName(hostname);
            cmdSocket = new DatagramSocket();
            cmdOutPacket = new DatagramPacket(cmd_out_ba, cmd_out_ba.length,
                    svrAddess, 1510);
            cmdResponsePacket = new DatagramPacket(cmd_in_ba, cmd_in_ba.length,
                    svrAddess, 1510);
            if (!multicast) {
                dataSocket = cmdSocket;// new DatagramSocket();
                dataPacketAddress = svrAddess;
            } else {
                dataSocket = new MulticastSocket(1511);
                groupAddess = InetAddress.getByName(MULTICAST_ADDRESS);
//            NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
//            ((MulticastSocket)dataSocket).joinGroup(new InetSocketAddress(groupAddess,1511), networkInterface);
                ((MulticastSocket) dataSocket).joinGroup(groupAddess);
////            dataSocket = new DatagramSocket();
//            dataPacket = new DatagramPacket(data_ba, data_ba.length,
//                    groupAddess, 1511);

                dataPacketAddress = groupAddess;
            }
            cmdSocketReaderThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (!Thread.currentThread().isInterrupted()) {
                        readPacketFromCmdSocket();
                    }
                }
            }, "cmdSocketReader");
            dataSocketReaderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted()) {
                        readPacketFromDataSocket();
                    }
                }
            }, "dataSocketReader");
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
    public DataFrame last_frame_recieved = null;

    /**
     * Class containing all data obtained in one packet from optitrack.
     */
    public class DataFrame {

        public double localRecvTime;
        public double timeSinceLastRecvTime;

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

        public int nSkeletons;

        public int nLabeledMarkers;

        public LabeledMarker labeled_marker_array[];

        public int nForcePlates;

        /**
         * From NatNet User's Guide
         *
         * Latency is the capture computer's hardware timestamp for the given
         * frame, which is also displayed in Motive in the Camera Preview
         * viewport when camera info is enabled. This is the same whether live
         * or playback from file.
         */
        public float latency;

        int timecode;
        int timecodeSub;

        public double timestamp;

        short params;
        boolean bIsRecording;                  // 0x01 Motive is recording
        boolean bTrackedModelsChanged;
        int eod; // end of data marker
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
    public class LabeledMarker {

        /**
         * Identifier
         */
        public int ID;

        /**
         * Position of labeled marker, this may be the centroid or just a point
         * chosen relative to the markers within the TrackingTools program.
         */
        public Point3D pos;

        public float size;

        public short params;

        boolean bOccluded;// = params & 0x01;     // marker was not visible (occluded) in this frame
        boolean bPCSolved;//  = params & 0x02;     // position provided by point cloud solve
        boolean bModelSolved;// = params & 0x04;  // position provided by model solve

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
         * Position of rigid body, this may be the centroid or just a point
         * chosen relative to the markers within the TrackingTools program.
         */
        public Point3D pos;

        /**
         * Orientation as a quaternion
         */
        public float ori[];

        /**
         * Number of markers in this rigid body
         */
        public int nRigidMarkers = 0;

        /**
         * Array of info for each marker.
         */
        public MarkerWithId rigid_markers_array[];

        /**
         * Mean error as computed by TrackingTools.
         */
        public float meanMarkerError;

        /**
         * rigid body was successfully tracked in this frame
         */
        public boolean trackingValid;
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
            return "[ID=" + ID + ",sz=" + String.format("%.3f", sz) + "]" + super.toString();
        }
    }

    private DataFrame unpackFrameData(byte data[],
            short iMessage,
            int nDataBytes) {
        DataFrame df = new DataFrame();
        df.frameNumber = readIntFromByteArray(data, 4);
        if (debug) {
            System.out.println("df.frameNumber = " + df.frameNumber);
        }
        df.nMarkerSets = readIntFromByteArray(data, 8);
        if (debug) {
            System.out.println("df.nMarkerSets = " + df.nMarkerSets);
        }
        df.marker_set_array = new MarkerSet[df.nMarkerSets];
        int offset = 12;
        for (int i = 0; i < df.nMarkerSets; i++) {
            int name_offset = offset;
            while (data[offset] != 0 && offset < data.length) {
                offset++;
            }
            MarkerSet ms = new MarkerSet();
            df.marker_set_array[i] = ms;
            ms.name = new String(data, name_offset, (offset - name_offset + 1));
            if (debug) {
                System.out.println("name_offset = " + name_offset);
            }
            if (debug) {
                System.out.println("offset = " + offset);
            }
            if (debug) {
                System.out.println("df.marker_set_array[i].name = " + df.marker_set_array[i].name);
            }
            offset++;
            df.marker_set_array[i].nMarkers = readIntFromByteArray(data, offset);
            if (debug) {
                System.out.println("df.marker_set_array[i].nMarkers = " + df.marker_set_array[i].nMarkers);
            }
            offset += 4;
            if (df.marker_set_array[i].nMarkers > 0) {
                df.marker_set_array[i].points_array = new Point3D[df.marker_set_array[i].nMarkers];
                for (int j = 0; j < df.marker_set_array[i].nMarkers; j++) {
                    df.marker_set_array[i].points_array[j] = new Point3D();
                    if (debug) {
                        System.out.println("offset = " + offset);
                    }
                    df.marker_set_array[i].points_array[j].x = readFloatFromByteArray(data, offset);
                    offset += 4;
                    df.marker_set_array[i].points_array[j].y = readFloatFromByteArray(data, offset);
                    offset += 4;
                    df.marker_set_array[i].points_array[j].z = readFloatFromByteArray(data, offset);
                    offset += 4;
                    if (debug) {
                        System.out.println("df.marker_set_array[i].points_array[j] = " + df.marker_set_array[i].points_array[j]);
                    }
                }
            }
        }
        df.nOtherMarkers = readIntFromByteArray(data, offset);
        if (debug) {
            System.out.println("df.nOtherMarkers = " + df.nOtherMarkers);
        }
        offset += 4;
        if (df.nOtherMarkers > 0) {
            LinkedList<Point3D> markersList = new LinkedList<Point3D>();
            for (int j = 0; j < df.nOtherMarkers; j++) {
                if (debug) {
                    System.out.println("offset = " + offset);
                }
                float x = readFloatFromByteArray(data, offset);
                offset += 4;
                float y = readFloatFromByteArray(data, offset);
                offset += 4;
                float z = readFloatFromByteArray(data, offset);
                offset += 4;
                if (Math.abs(x) > 1e-4 && Math.abs(y) > 1e-4 && Math.abs(z) > 1e-4) {
                    markersList.add(new Point3D(x, y, z));
//                    if(debug) System.out.println("df.other_markers_array[j] = " + df.other_markers_array[j]);
                }
            }
            df.other_markers_array = markersList.toArray(new Point3D[markersList.size()]);
        }
        if (debug) {
            System.out.println("offset = " + offset);
        }
        df.nRigidBodies = readIntFromByteArray(data, offset);
        if (debug) {
            System.out.println("df.nRigidBodies = " + df.nRigidBodies);
        }
        offset += 4;
        if (df.nRigidBodies > 0) {
            df.rigid_body_array = new RigidBody[df.nRigidBodies];
            for (int j = 0; j < df.nRigidBodies; j++) {
                RigidBody rb = new RigidBody();
                df.rigid_body_array[j] = rb;
                if (debug) {
                    System.out.println("offset = " + offset);
                }
                rb.ID = readIntFromByteArray(data, offset);
                offset += 4;
                if (debug) {
                    System.out.println("rb.Id = " + rb.ID);
                }
                float x = readFloatFromByteArray(data, offset);
                offset += 4;
                float y = readFloatFromByteArray(data, offset);
                offset += 4;
                float z = readFloatFromByteArray(data, offset);
                rb.pos = new Point3D(x, y, z);
                offset += 4;
                if (debug) {
                    System.out.println("rb.pos = " + rb.pos);
                }
                rb.ori = new float[4];
                rb.ori[0] = readFloatFromByteArray(data, offset);
                offset += 4;
                rb.ori[1] = readFloatFromByteArray(data, offset);
                offset += 4;
                rb.ori[2] = readFloatFromByteArray(data, offset);
                offset += 4;
                rb.ori[3] = readFloatFromByteArray(data, offset);
                offset += 4;
                rb.nRigidMarkers = readIntFromByteArray(data, offset);
                if (debug) {
                    System.out.println("rb.nRigidMarkers = " + rb.nRigidMarkers);
                }
                offset += 4;
                if (rb.nRigidMarkers > 0) {
                    rb.rigid_markers_array = new MarkerWithId[rb.nRigidMarkers];
                    for (int k = 0; k < rb.nRigidMarkers; k++) {
                        MarkerWithId marker = new MarkerWithId();
                        marker.x = readFloatFromByteArray(data, offset);
                        offset += 4;
                        marker.y = readFloatFromByteArray(data, offset);
                        offset += 4;
                        marker.z = readFloatFromByteArray(data, offset);
                        offset += 4;
                        rb.rigid_markers_array[k] = marker;
                    }
                    for (int k = 0; k < rb.nRigidMarkers; k++) {
                        MarkerWithId marker = rb.rigid_markers_array[k];
                        marker.ID = readIntFromByteArray(data, offset);
                        offset += 4;
                    }
                    for (int k = 0; k < rb.nRigidMarkers; k++) {
                        MarkerWithId marker = rb.rigid_markers_array[k];
                        marker.sz = readFloatFromByteArray(data, offset);
                        offset += 4;
                        if (debug) {
                            System.out.println("marker = " + marker);
                        }
                    }
                }

                if (major >= 2) {
                    rb.meanMarkerError = readFloatFromByteArray(data, offset);
                    offset += 4;
                    if (debug) {
                        System.out.println("rb.meanMarkerError = " + rb.meanMarkerError);
                    }
                }
                // 2.6 and later
                if (((major == 2) && (minor >= 6)) || (major > 2) || (major == 0)) {
                    // params
                    short params = readShortFromByteArray(data, offset);
                    offset += 2;
                    rb.trackingValid = (params & 0x01) != 0; // 0x01 : rigid body was successfully tracked in this frame
                }
            }
        }
        // skeletons (version 2.1 and later)
        if (((major == 2) && (minor > 0)) || (major > 2)) {
            df.nSkeletons = readIntFromByteArray(data, offset);
            offset += 4;
            if (debug) {
                System.out.println("Number of skeletons:" + df.nSkeletons);
            }
            if (df.nSkeletons > 0) {
                System.err.println("Skeleton data parsing not implementded.");
                return df;
            }
        }
        // labeled markers (version 2.3 and later)
        if (((major == 2) && (minor >= 3)) || (major > 2)) {
            df.nLabeledMarkers = readIntFromByteArray(data, offset);
            offset += 4;
            if (debug) {
                System.out.println("Number of Labeled Markers:" + df.nLabeledMarkers);
            }
            if (df.nLabeledMarkers > 0
                    && (df.labeled_marker_array == null || df.labeled_marker_array.length != df.nLabeledMarkers)) {
                df.labeled_marker_array = new LabeledMarker[df.nLabeledMarkers];
            }
            for (int j = 0; j < df.nLabeledMarkers; j++) {

                if (null == df.labeled_marker_array[j]) {
                    df.labeled_marker_array[j] = new LabeledMarker();
                }
                LabeledMarker lm = df.labeled_marker_array[j];

                // id
                lm.ID = readIntFromByteArray(data, offset);
                offset += 4;

                if (null == lm.pos) {
                    lm.pos = new Point3D();
                }
                lm.pos.x = readFloatFromByteArray(data, offset);
                offset += 4;

                lm.pos.y = readFloatFromByteArray(data, offset);
                offset += 4;

                lm.pos.z = readFloatFromByteArray(data, offset);
                offset += 4;

                lm.size = readFloatFromByteArray(data, offset);
                offset += 4;

                // 2.6 and later
                if (((major == 2) && (minor >= 6)) || (major > 2) || (major == 0)) {
                    // marker params
                    lm.params = readShortFromByteArray(data, offset);
                    offset += 2;

                    lm.bOccluded = 0 != (lm.params & 0x01);     // marker was not visible (occluded) in this frame
                    lm.bPCSolved = 0 != (lm.params & 0x02);     // position provided by point cloud solve
                    lm.bModelSolved = 0 != (lm.params & 0x04);  // position provided by model solve
                }
                if (debug) {
                    System.out.println("j = " + j);
                    System.out.printf("ID  : %d\n", lm.ID);
                    System.out.printf("pos : [%3.2f,%3.2f,%3.2f]\n", lm.pos.x, lm.pos.y, lm.pos.z);
                    System.out.printf("size: [%3.2f]\n", lm.size);
                }
            }
        }
        // Force Plate data (version 2.9 and later)
        if (((major == 2) && (minor >= 9)) || (major > 2)) {
            df.nForcePlates = readIntFromByteArray(data, offset);
            offset += 4;
            if (debug) {
                System.out.println("Number of Force Plates:" + df.nForcePlates);
            }
            if (df.nForcePlates > 0) {
                System.err.println("Force Plate data parsing not implementded.");
                return df;
            }
        }

        // latency
        df.latency = readFloatFromByteArray(data, offset);
        df.latency *= 1e-3f; // convert from milliseconds to seconds
        offset += 4;
        if (debug) {
            System.out.println("df.latency = " + df.latency);
        }

        // timecode
        df.timecode = readIntFromByteArray(data, offset);
        offset += 4;
        df.timecodeSub = readIntFromByteArray(data, offset);
        offset += 4;
        if (debug) {
            System.out.println("df.timecode = " + df.timecode);
            System.out.println("df.timecodeSub = " + df.timecodeSub);
        }

        // 2.7 and later - increased from single to double precision
        if (((major == 2) && (minor >= 7)) || (major > 2)) {
            df.timestamp = readDoubleFromByteArray(data, offset);
            offset += 8;
        } else {
            float fTemp = readFloatFromByteArray(data, offset);
            offset += 4;
            df.timestamp = (double) fTemp;
        }
        if (debug) {
            System.out.println("df.timestamp = " + df.timestamp);
        }
        // frame params
        df.params = readShortFromByteArray(data, offset);
        if (debug) {
            System.out.println("df.params = " + df.params);
        }
        offset += 2;
        df.bIsRecording = (df.params & 0x01) != 0; // 0x01 Motive is recording
        df.bTrackedModelsChanged = (df.params & 0x02) != 0;// 0x02 Actively tracked model list has changed

        if (debug) {
            System.out.println("df.bIsRecording = " + df.bIsRecording);
            System.out.println("df.bTrackedModelsChanged = " + df.bTrackedModelsChanged);
        }

        // end of data tag
        df.eod = readIntFromByteArray(data, offset);
        if (debug) {
            System.out.println("df.eod = " + df.eod);
        }
        offset += 4;
        if (debug) {
            System.out.println("offset at end of data unpackFrameData = " + offset + ", nDataBytes=" + nDataBytes + ",data.length=" + data.length);
            System.out.println("");
            System.out.println("");
            System.out.flush();
            System.err.println("");
            System.err.println("");
            System.err.flush();
        }

        return df;
    }

    /**
     * Unpack the data in the byte array to identify the message type and
     * interpret accordingly.
     *
     * @param data data received from UDP socket
     */
    public void unpack(byte data[]) {
        short iMessage = readShortFromByteArray(data, 0);
        if (debug) {
            System.out.println("iMessage = " + iMessage);
        }
        int nDataBytes = readShortFromByteArray(data, 2);
        if (nDataBytes < 0) {
            nDataBytes = (1 << 16) - nDataBytes;
        }
        if (debug) {
            System.out.println("nDataBytes = " + nDataBytes);
        }
        switch (iMessage) {
            case NAT_PINGRESPONSE:
                ping_count++;
                String szName = new String(data, 4, 256);
                if (debug) {
                    System.out.println("szName = " + szName);
                }
                String version = new String(data, 260, 4);
                if (debug) {
                    System.out.println("version = " + version);
                }
                String natnet_version = new String(data, 264, 4);
                if (debug) {
                    System.out.println("natnet_version = " + natnet_version);
                }
                break;

            case NAT_FRAMEOFDATA:
                incUpdates();
                last_frame_recieved
                        = unpackFrameData(data, iMessage, nDataBytes);
                break;
        }
        if (null != listeners) {
            for (Runnable r : listeners) {
                r.run();
            }
        }
    }

    /**
     * Send a request to TrackingTools software to return version number and
     * verify that it is working on the correct host.
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
            sendPingRequest();
            while (old_ping_count == ping_count) {
                if (debug) {
                    System.out.println("ping_count = " + ping_count);
                }
                if (debug) {
                    System.out.println("old_ping_count = " + old_ping_count);
                }
                Thread.sleep(1000);
                sendPingRequest();
            }
            if (debug) {
                System.out.println("ping_count = " + ping_count);
            }
            if (debug) {
                System.out.println("old_ping_count = " + old_ping_count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean try_ping(int max_tries, long sleep_millis) {
        try {
            final int old_ping_count = ping_count;
            sendPingRequest();
            int tries = 0;
            while (old_ping_count == ping_count) {
                if (tries > max_tries) {
                    return false;
                }
                tries++;
                if (debug) {
                    System.out.println("ping_count = " + ping_count);
                }
                if (debug) {
                    System.out.println("old_ping_count = " + old_ping_count);
                }
                Thread.sleep(sleep_millis);
                sendPingRequest();
            }
            if (debug) {
                System.out.println("ping_count = " + ping_count);
            }
            if (debug) {
                System.out.println("old_ping_count = " + old_ping_count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void getFrame() {
        try {
            final long old_updates = getUpdates();
            sendFrameRequest();
            while (old_updates == getUpdates()) {
                if (debug) {
                    System.out.println("updates = " + getUpdates());
                }
                if (debug) {
                    System.out.println("old_updates = " + old_updates);
                }
                Thread.sleep(1000);
                sendFrameRequest();
            }
            if (debug) {
                System.out.println("updates = " + getUpdates());
            }
            if (debug) {
                System.out.println("old_updates = " + old_updates);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        ignore_errors = true;
        if (null != dataSocket) {
            dataSocket.close();
            dataSocket = null;
        }
        if (null != cmdSocket) {
            cmdSocket.close();
            cmdSocket = null;
        }
        try {
            if (null != cmdSocketReaderThread) {
                cmdSocketReaderThread.interrupt();
                cmdSocketReaderThread.join();
                cmdSocketReaderThread = null;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            if (null != dataSocketReaderThread) {
                dataSocketReaderThread.interrupt();
                dataSocketReaderThread.join();
                dataSocketReaderThread = null;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    /**
     * This is not the main for the jar. The main for the jar is in
     * HumanTrackingPerformanceMetrics. This main can be used to test optitrack
     * interface and this class only.
     *
     * @param args Command line arguments are not used.
     */
    public static void main(String args[]) {
        debug = true;
        OptitrackUDPStream ots = new OptitrackUDPStream("129.6.39.54", true, 2, 6);
        ots.ping();
        while (ots.getUpdates() < 3) {
            if (debug) {
                System.out.println("ots.updates = " + ots.getUpdates());
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ots.close();
    }

    private int updates = 0;
    private double firstUpdateTime = 0.0;
    private double lastLocalRecvTime = 0.0;
    private int lastFrameNumber = -1;
    private static final Point2D zero2d = new Point2D.Float(0f, 0f);

    /**
     * Update tracks and displays using the current position of one rigid body
     * as reported by the optitrack.
     *
     * @param rb Optitrack rigid body data
     * @return whether displays and/or logs need to be updated.
     */
    public boolean UpdateOptitrackRigidBody(OptitrackUDPStream.RigidBody rb,
            DataFrame df,
            PrintStream ps,
            ConnectionUpdate update) throws Exception {
        boolean point_updated = false;
        Point3D pt = rb.pos;

        if (zero2d.distance(pt) < 0.001) {
            return false;
        }
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
        String rb_name = Integer.toString(rb.ID);
        for (Track t : myTracks) {
            if (t.name.compareTo(rb_name) == 0) {
                curTrack = t;
                break;
            }
        }
        if (null == curTrack) {
            curTrack = new Track();
            curTrack.source = "optitrack";
            curTrack.name = Integer.toString(rb.ID);
            curTrack.is_groundtruth = isGroundtruth();
            Settings settings = update.getSettings();
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
                List<Track> sutlist = update.getSutlist();
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
        TrackPoint tp = new TrackPoint(pt);
        if (null != rb.ori && rb.ori.length == 4) {
            tp.orientation = Arrays.copyOf(rb.ori, 4);
        }
        if (null != df) {
            tp.setLatency(df.latency);
        }
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
        if (isGroundtruth()) {
            curTrack.pointColor = Color.RED;
            curTrack.lineColor = Color.RED;
        } else {
            curTrack.pointColor = Color.BLUE;
            curTrack.lineColor = Color.BLUE;
        }
        if (null == curTrack.data) {
            curTrack.data = new ArrayList<TrackPoint>();
        }
        curTrack.data.add(tp);
        if (null != ps) {
            update.getCsvLinePrinter()
                    .printOneLine(tp, curTrack.name, df.frameNumber, df.timeSinceLastRecvTime, df.timestamp, ps);
        }
        while(curTrack.data.size() > 200) {
            curTrack.data.remove(0);
        }
        curTrack.cur_time_index = curTrack.data.size() - 1;
        //setCurrentTime(tp.time + 0.00001);
        return true;
    }

    private static final TrackPoint nanTrackPoint = new TrackPoint(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

    @Override
    public void updateData(ConnectionUpdate update) throws Exception {
        double time = System.currentTimeMillis() * 1e-3;
        last_frame_recieved.timeSinceLastRecvTime = time - lastLocalRecvTime;
        last_frame_recieved.localRecvTime = time;
        lastLocalRecvTime = time;
        int diff = last_frame_recieved.frameNumber - lastFrameNumber;
        if(lastFrameNumber > 0 && diff > 2) {
            setMissedFrames(getMissedFrames()+(diff-1));
        }
        lastFrameNumber = last_frame_recieved.frameNumber;
        PrintStream optitrack_print_stream = update.getPrintStream();
        
        List<Track> allTracks = update.getAllTracks();
        if (null == allTracks) {
            allTracks = new ArrayList<Track>();
            update.setAllTracks(allTracks);
        }
        if (update.isAddNewFrameLines()) {
            try {
                nanTrackPoint.time = time;
                update.getCsvLinePrinter().printOneLine(nanTrackPoint, "new_frame", last_frame_recieved.frameNumber, last_frame_recieved.localRecvTime, last_frame_recieved.timestamp, optitrack_print_stream);
            } catch (Exception ex) {
                Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (update.isAddUnaffiliatedMarkers()) {
            try {
                if (null != last_frame_recieved.other_markers_array
                        && last_frame_recieved.other_markers_array.length > 0) {
                    Track optitrack_unaffiliated_track = update.getUnaffiliatedTrack();
                    if (null == optitrack_unaffiliated_track) {
                        optitrack_unaffiliated_track = new Track();
                        optitrack_unaffiliated_track.name = "optitrack_unaffiliated_track";
                        optitrack_unaffiliated_track.source = "optitrack";
                        optitrack_unaffiliated_track.disconnected = true;
                        optitrack_unaffiliated_track.is_groundtruth = isGroundtruth();
                        List<Track> optitrack_tracks = update.getCurrentDeviceTracks();
                        if (null == optitrack_tracks) {
                            optitrack_tracks = new LinkedList<Track>();
                        }
                        optitrack_tracks.add(optitrack_unaffiliated_track);
                        if (isGroundtruth()) {
                            List<Track> gtlist = update.getGtlist();
                            if (null == gtlist) {
                                gtlist = new LinkedList<Track>();
                                update.setGtlist(gtlist);
                            }
                            gtlist.add(optitrack_unaffiliated_track);
                        } else {
                            List<Track> sutlist = update.getSutlist();
                            if (null == sutlist) {
                                sutlist = new LinkedList<Track>();
                                update.setSutlist(sutlist);
                            }
                            sutlist.add(optitrack_unaffiliated_track);
                        }
                        allTracks.add(optitrack_unaffiliated_track);
                        update.setCurrentDeviceTracks(optitrack_tracks);
                    }
                    for (Point3D p3d : last_frame_recieved.other_markers_array) {
                        TrackPoint tp = new TrackPoint(p3d);
                        tp.time = time;
                        tp.setLatency(last_frame_recieved.latency);
                        if (isApplyTransform()) {
                            tp.applyTransform(getTransform());
                            optitrack_unaffiliated_track.setTransform(getTransform());
                        }
                        if (null == optitrack_unaffiliated_track.data) {
                            optitrack_unaffiliated_track.data = new ArrayList<TrackPoint>();
                            optitrack_unaffiliated_track.disconnected = true;
                        }
                        optitrack_unaffiliated_track.data.add(tp);
                        if (null != optitrack_print_stream) {
                            update.getCsvLinePrinter()
                                    .printOneLine(tp,
                                            optitrack_unaffiliated_track.name,
                                            last_frame_recieved.frameNumber,
                                            last_frame_recieved.timeSinceLastRecvTime,
                                            last_frame_recieved.timestamp,
                                            optitrack_print_stream);
                        }
                        if (optitrack_unaffiliated_track.data.size() > 5000) {
                            optitrack_unaffiliated_track.data.remove(0);
                        }
                        optitrack_unaffiliated_track.cur_time_index = optitrack_unaffiliated_track.data.size() - 1;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        if (null == last_frame_recieved
                || null == last_frame_recieved.rigid_body_array) {
            return;
        }
        if (updates < 10) {
            firstUpdateTime = time;
        }
        updates++;
        boolean point_updated = false;
        double timeCollecting = 1e-12 + time - firstUpdateTime;
        double fps = (updates - 9) / timeCollecting;
        update.setLabel(String.format("latency = %.3f ms,\n timeSinceLastRecvTime=%.3f,\n timeCollecting=%.3f,framesPerSecond= %.3f, updates=%d,numRigidBodies=%d,timeStamp=%.3f",
                last_frame_recieved.latency,
                last_frame_recieved.timeSinceLastRecvTime,
                timeCollecting,
                fps,
                updates,
                last_frame_recieved.rigid_body_array.length,
                last_frame_recieved.timestamp));
//        try {
        for (OptitrackUDPStream.RigidBody rb : last_frame_recieved.rigid_body_array) {
            boolean new_update
                    = UpdateOptitrackRigidBody(rb,
                            last_frame_recieved,
                            optitrack_print_stream,
                            update);
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
}
