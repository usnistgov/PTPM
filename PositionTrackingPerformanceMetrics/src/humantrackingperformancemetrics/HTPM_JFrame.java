/*
 * Primary frame for the application.
 */
package humantrackingperformancemetrics;

import diagapplet.plotter.PlotData;
import diagapplet.plotter.plotterJFrame;
import static humantrackingperformancemetrics.VelocityCorrellationSimulationAndTesting.point2DwTimeListToVelList;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Implements the primary JFrame for the application.
 *
 * @author Will Shackleford<shackle@nist.gov>
 */
public class HTPM_JFrame extends javax.swing.JFrame {

    /**
     * Creates new form HTPM_JFrame
     */
    public HTPM_JFrame() {
        initComponents();
        settings_file = new File(System.getProperty("user.home"), ".htpm");
        reloadSettings(settings_file);
        this.default_client = new MonitoredConnection(this);
        this.jTree1.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        if (null != sutlist && null != gtlist) {
            this.drawPanel1.tracks = new ArrayList<Track>();
            if (s.gt_on_top) {
                this.drawPanel1.tracks.addAll(sutlist);
                this.drawPanel1.tracks.addAll(gtlist);
            } else {
                this.drawPanel1.tracks.addAll(sutlist);
                this.drawPanel1.tracks.addAll(gtlist);
            }
            this.jSliderTime.setValue(0);
            this.jSliderZoom.setValue(0);
            this.setCurrentTime(inner_min_time + 0.001);
            this.updateDrawPanelViewport();
        }
        DefaultTreeCellRenderer renderer =
                new DefaultTreeCellRenderer() {
            private Track last_t = null;
            private ImageIcon last_image_icon = null;

            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree,
                    Object value,
                    boolean sel,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus) {

                super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row,
                        hasFocus);
                try {
                    if (leaf) {
                        DefaultMutableTreeNode n =
                                (DefaultMutableTreeNode) value;
                        final Track t = (Track) n.getUserObject();
//                                if (null != last_t
//                                        && null != last_image_icon
//                                        && last_t.hidden == t.hidden
//                                        && last_t.color == t.color
//                                        && last_t.selected == t.selected
//                                        && last_t.explicit_color == t.explicit_color) {
//                                    this.setIcon(this.last_image_icon);
//                                } else {
                        BufferedImage bi =
                                new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
                        Graphics g = bi.getGraphics();
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, 10, 10);
                        if (t.hidden) {
                            g.setColor(Color.GRAY);
                        } else {
                            g.setColor(t.color);
                        }
                        g.fillOval(1, 1, 8, 8);
                        bi.flush();
                        ImageIcon II = new ImageIcon(bi);
                        this.setText(t.name + " [" + t.data.size() + "]");
                        this.setIcon(II);
                        this.last_t = t;
                        this.last_image_icon = II;
//                                }
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                return this;
            }
        };
        this.jTree1.setCellRenderer(renderer);
        this.jCheckBoxMenuItemGtOnTop.setSelected(s.gt_on_top);
        this.updateMenuLabels();
        try {
            this.updateLogSingleFrameButton();
        } catch (Exception ex) {
            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        reloadRecentFileInfo();
    }

    public void reloadRecentFileInfo() {
        try {
            File recentFilesDir = new File(System.getProperty("user.home"), ".htpm_recent_files");
            File recentGTFilesDir = new File(recentFilesDir, ".gt");
            File gtfa[] = recentGTFilesDir.listFiles();
            if (null != gtfa) {
                List<File> gtfl = Arrays.asList(gtfa);
                Collections.sort(gtfl, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return Long.compare(o1.lastModified(), o2.lastModified());
                    }
                });
                Collections.reverse(gtfl);
                for (File f : gtfl) {
                    loadRecentFileInfo(f, true);
                }
            }
            File recentSUTFilesDir = new File(recentFilesDir, ".sut");
            File sutfa[] = recentSUTFilesDir.listFiles();
            if (null != sutfa) {
                List<File> sutfl = Arrays.asList(sutfa);
                Collections.sort(sutfl, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return Long.compare(o1.lastModified(), o2.lastModified());
                    }
                });
                Collections.reverse(sutfl);
                for (File f : sutfl) {
                    loadRecentFileInfo(f, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double[] stringToDoubleArray(String s) {
        String items[] = s.split("[ \t,\\[\\]]+");
        List<Double> l = new LinkedList<Double>();
        for (String item : items) {
            if (item == null || item.length() < 1) {
                continue;
            }
            try {
                double d = Double.valueOf(item);
                l.add(d);
            } catch (Exception e) {
                // do nothing
            }
        }
        double da[] = new double[l.size()];
        for (int i = 0; i < l.size(); i++) {
            da[i] = l.get(i);
        }
        return da;
    }

    public void loadRecentFileInfo(File f, boolean _is_groundtruth) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            CsvParseOptions o = new CsvParseOptions();
            String line = null;
            while ((line = br.readLine()) != null) {
                int eq_index = line.indexOf('=');
                if (eq_index < 1 || eq_index > line.length()) {
                    continue;
                }
                String var = line.substring(0, eq_index).trim();
                String val = line.substring(eq_index + 1);
                Field field = o.getClass().getField(var);
                Class fcls = field.getType();
                if (fcls.isAssignableFrom(double.class)) {
                    field.setDouble(o, Double.valueOf(val));
                } else if (fcls.isAssignableFrom(float.class)) {
                    field.setFloat(o, Float.valueOf(val));
                } else if (fcls.isAssignableFrom(long.class)) {
                    field.setLong(o, Long.valueOf(val));
                } else if (fcls.isAssignableFrom(int.class)) {
                    field.setInt(o, Integer.valueOf(val));
                } else if (fcls.isAssignableFrom(short.class)) {
                    field.setShort(o, Short.valueOf(val));
                } else if (fcls.isAssignableFrom(boolean.class)) {
                    field.setBoolean(o, Boolean.valueOf(val));
                } else if (fcls.isAssignableFrom(double[].class)) {
                    field.set(o, stringToDoubleArray(val));
                } else if (fcls.isAssignableFrom(boolean.class)) {
                    field.setBoolean(o, Boolean.valueOf(val));
                } else {
                    field.set(o, val);
                }
            }
            br.close();
            if (null == o.filename || o.filename.length() < 1) {
                return;
            }
            if (_is_groundtruth) {
                if (null == this.recent_gt_files) {
                    this.recent_gt_files = new HashMap<String, CsvParseOptions>();
                }
                if (!this.recent_gt_files.containsKey(o.filename)) {
                    this.recent_gt_files.put(o.filename, o);
                    this.jMenuRecentGroundTruthCsv.add(this.createRecentFileMenuItem(o, _is_groundtruth));
                }
            } else {
                if (null == this.recent_sut_files) {
                    this.recent_sut_files = new HashMap<String, CsvParseOptions>();
                }
                if (!this.recent_sut_files.containsKey(o.filename)) {
                    this.jMenuRecentSystemUnderTestCsv.add(this.createRecentFileMenuItem(o, _is_groundtruth));
                    this.recent_sut_files.put(o.filename, o);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    final protected void settingsToCurrent() {
        if (s != null) {
            TransformMatrixJPanel.transform_dir = s.transform_dir;
            this.drawPanel1.setTrack_tail_highlight_time(s.track_tail_highlight_time);
            this.drawPanel1.setGrid(s.grid);
            this.drawPanel1.show_background_image = s.show_background_image;
            this.drawPanel1.background_image_x = s.background_image_x;
            this.drawPanel1.background_image_y = s.background_image_y;
            this.drawPanel1.background_image_scale_pixels_per_m = s.background_image_scale_pixels_per_m;
            this.jCheckBoxMenuItemShowBackground.setSelected(s.show_background_image);
            this.jTextFieldGrid.setText(Double.toString(s.grid));
            this.jTextFieldGTRadius.setText(Double.toString(s.gt_radius_increase));
            this.jTextFieldSUTRadius.setText(String.format("%.3f", s.sut_radius_increase));
            int sv = (int) ((s.confidence_threshold)
                    * ((double) this.jSliderConfidence.getMaximum()));
            this.jSliderConfidence.setValue(sv);
            this.drawPanel1.x_max = s.x_max;
            this.drawPanel1.x_min = s.x_min;
            this.drawPanel1.y_max = s.y_max;
            this.drawPanel1.y_min = s.y_min;
            Dimension d = new Dimension(s.panel_pref_width, s.panel_pref_height);
            this.drawPanel1.setPreferredSize(d);
            JViewport vp = this.jScrollPaneDrawPanel.getViewport();
            Point pt = vp.getViewPosition();
            pt.x = s.view_point_x;
            pt.y = s.view_point_y;
            vp.setViewPosition(pt);
            this.jSliderZoom.setValue(s.zoom_v);
            this.drawPanel1.ROI[0] = s.roi_x_min;
            this.drawPanel1.ROI[1] = s.roi_y_min;
            this.drawPanel1.ROI[2] = s.roi_x_max;
            this.drawPanel1.ROI[3] = s.roi_y_max;
            this.jCheckBoxMenuItemIgnoreSUTVelocities.setSelected(s.ignore_sut_velocities);
            this.jCheckBoxMenuItemGrayTracks.setSelected(s.use_gray_tracks);
            this.drawPanel1.use_gray_tracks = this.jCheckBoxMenuItemGrayTracks.isSelected();
        }
    }

    final protected void reloadSettings(File f) {
        s = readSettings(f);
        if (null != s) {
            this.settingsToCurrent();
        } else {
            s = new settings();
        }
    }

    private void updateMenuLabels() {
        String new_label = this.jCheckBoxMenuItemAcceptSutData.getText();
        int pindex = new_label.indexOf('(');
        if (pindex > 0) {
            new_label = new_label.substring(0, pindex);
        }
        new_label = new_label + "(" + s.sut_port + ")";
        this.jCheckBoxMenuItemAcceptSutData.setText(new_label);
        new_label = this.jCheckBoxMenuItemAcceptGT.getText();
        pindex = new_label.indexOf('(');
        if (pindex > 0) {
            new_label = new_label.substring(0, pindex);
        }
        new_label = new_label + "(" + s.gt_port + ")";
        this.jCheckBoxMenuItemAcceptGT.setText(new_label);
    }

    public Color ChooseColor(String title, Color default_color) {
        return JColorChooser.showDialog(this, title, default_color);
    }
    public Map<String, CsvParseOptions> recent_gt_files = null;
    public Map<String, CsvParseOptions> recent_sut_files = null;

    /**
     * Class to user settable settings that should persist in the users .htpm
     * file in their home directory. Most likely only the single instance of the
     * class (s) will ever be needed.
     */
    public static class settings {

        /**
         * Show the Ground-truth on top of the System-Under-Test.
         */
        public boolean gt_on_top;
        /**
         * TCP port number from which ground-truth data is expected to be
         * received.
         */
        public short gt_port = 2113;
        /**
         * TCP port number from which system-under-test data is expected to be
         * received.
         */
        public short sut_port = 2112;
        /**
         * Time increment between updating statistics.
         */
        public double time_inc = 0.05;
        /**
         * Scale in pixels/meter for occupancy grid.
         */
        public double scale = 50.0;
        /**
         * Increase to radius of circle to draw around each ground-truth(gt)
         * person,trackable, or reciever.
         */
        public double gt_radius_increase = 1.0;
        /**
         * Increase to radius of circle to draw around each
         * system-under-test(sut) person,trackable, or reciever.
         */
        public double sut_radius_increase = 1.0;
        /**
         * Distance between grid lines in meters.
         */
        public double grid = 1.0;
        /**
         * Confidence level under which points may be drawn/treated differently
         * or ignored.
         */
        public double confidence_threshold = 0.0;
        /**
         * Hostname or IP address of the system running the optitrack software.
         */
        public String optitrack_host = "129.6.152.101";
        public String optitrack_trasform_filename;
        /**
         * Default order of the coordinates to use from optitrack.
         */
        public XyzOrderEnum optitrack_xyz_order = XyzOrderEnum.XY;
        public String gt_server_host = "129.6.152.101";
        public short gt_server_port = 5150;
        public String sut_server_host = "129.6.152.101";
        public short sut_server_port = 5150;
        public boolean show_background_image = false;
        public double background_image_x = 0.0;
        public double background_image_y = 10.0;
        public double background_image_scale_pixels_per_m = 1000.0;
        /**
         * Coordinate in meters of the maximum x to paint (may be hidden by
         * JScollPane)
         */
        public double x_max = 15.0;
        /**
         * Coordinate in meters of the minimum x to paint (may be hidden by
         * JScollPane)
         */
        public double x_min = -5.0;
        /**
         * Coordinate in meters of the maximum y to paint (may be hidden by
         * JScollPane)
         */
        public double y_max = 15.0;
        /**
         * Coordinate in meters of the minimum y to paint (may be hidden by
         * JScollPane)
         */
        public double y_min = -5.0;
        public int view_point_x;
        public int view_point_y;
        public int panel_pref_width = 1302;
        public int panel_pref_height = 480;
        public int zoom_v = 50;
        public double roi_x_min = 0.0;
        public double roi_y_min = 0.0;
        public double roi_x_max = 10.0;
        public double roi_y_max = 10.0;
        public double sut_radius_inc = 0.01;
        public double project_ahead_time = 0.0;
        public boolean ignore_sut_velocities = false;
        public String sut_open_file_dir = System.getProperty("user.home");
        public String gt_open_file_dir = System.getProperty("user.home");
        public String save_file_dir = System.getProperty("user.home");
        public String transform_dir = System.getProperty("user.home");
        /**
         * Number of track points after current to highlight.
         */
        public double track_tail_highlight_time = 1.0;
        public boolean use_gray_tracks = true;
    }
    /**
     * Single instance of class to user settable settings that should persist in
     * the users ".htpm" file in their home directory. Most likely only the
     * single instance of the class (s) will ever be needed.
     */
    public static settings s = new settings();
    /**
     * File where setting were read from and will be written to.
     */
    public static File settings_file = null;

    /**
     * Save settings to a text file. The file will be saved in var=val\n format.
     * If we need to save a string with an "=" in it there will be a problem.
     *
     * @param _s settings to save
     * @param f file to save them to
     */
    public static void saveSettings(settings _s, File f) {
        try {
            PrintStream ps = new PrintStream(f);
            for (Field field : settings.class.getFields()) {
                Object o = field.get(_s);
                if (o != null) {
                    ps.println(field.getName() + "=" + o.toString());
                }
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read settings from a file. The file is expected to be in var=val\n
     * format. If we need to save a string with an "=" in it there will be a
     * problem.
     *
     * @param f file to read settings from
     * @return settings read from file
     */
    public static settings readSettings(File f) {
        try {
            if (!f.exists()) {
                return null;
            }
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            s = new settings();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() < 2) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                String parts[] = line.split("[\t,=]+");
                if (parts == null || parts.length < 2) {
                    continue;
                }
                String var = parts[0];
                String val = parts[1];

                if (var.compareToIgnoreCase("optitrack_host") == 0) {
                    s.optitrack_host = val.trim();
                    continue;
                }
                if (var.compareToIgnoreCase("optitrack_xyz_order") == 0) {
                    s.optitrack_xyz_order = XyzOrderEnum.valueOf(val.trim());
                    continue;
                }
                for (Field field : settings.class.getFields()) {
                    try {
                        if (var.compareTo(field.getName()) == 0) {
                            if (field.getType().isAssignableFrom(double.class)) {
                                field.setDouble(s, Double.valueOf(val));
                            } else if (field.getType().isAssignableFrom(int.class)) {
                                field.setInt(s, Integer.valueOf(val));
                            } else if (field.getType().isAssignableFrom(short.class)) {
                                field.setShort(s, Short.valueOf(val));
                            } else if (field.getType().isAssignableFrom(boolean.class)) {
                                field.setBoolean(s, Boolean.valueOf(val));
                            } else {
                                field.set(s, val);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return s;
        } catch (IOException ex) {
            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupMode = new javax.swing.ButtonGroup();
        jDialog1 = new javax.swing.JDialog();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaStats = new javax.swing.JTextArea();
        jButtonStatsDialogOk = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        buttonGroupDragMode = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPaneTree = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jScrollPaneDrawPanel = new javax.swing.JScrollPane();
        drawPanel1 = new humantrackingperformancemetrics.DrawPanel();
        jSliderTime = new javax.swing.JSlider();
        jSliderZoom = new javax.swing.JSlider();
        jLabelTime = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSliderConfidence = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldGrid = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldGTRadius = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldSUTRadius = new javax.swing.JTextField();
        jCheckBoxLive = new javax.swing.JCheckBox();
        jToolBar1 = new javax.swing.JToolBar();
        jButtonNewLogFile = new javax.swing.JButton();
        jButtonLogSingleFrame = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        jRadioButtonMeasure = new javax.swing.JRadioButton();
        jRadioButtonPan = new javax.swing.JRadioButton();
        jRadioButtonSelectTracks = new javax.swing.JRadioButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu8 = new javax.swing.JMenu();
        jMenuItemSaveSettings = new javax.swing.JMenuItem();
        jMenuItemOpenSettings = new javax.swing.JMenuItem();
        jMenuItemResetSettings = new javax.swing.JMenuItem();
        jMenuItemOpenGroundTruth = new javax.swing.JMenuItem();
        jMenuItemOpenSutCsv = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItemSaveGroundTruth = new javax.swing.JMenuItem();
        jMenuItemSaveSut = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemSaveImages = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPlay = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemPlayAndMakeMovie = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemRecordLive = new javax.swing.JCheckBoxMenuItem();
        jMenuItemSaveImages = new javax.swing.JMenuItem();
        jMenuItemGotoTime = new javax.swing.JMenuItem();
        jMenuItemGotoPlotterMinTime = new javax.swing.JMenuItem();
        jMenuItemClearData = new javax.swing.JMenuItem();
        jMenuRecentGroundTruthCsv = new javax.swing.JMenu();
        jMenuRecentSystemUnderTestCsv = new javax.swing.JMenu();
        jMenuConnections = new javax.swing.JMenu();
        jCheckBoxMenuItemOptitrack = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemAcceptGT = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemAcceptSutData = new javax.swing.JCheckBoxMenuItem();
        jMenuItemConnectGTServer = new javax.swing.JMenuItem();
        jMenuItemConnectSUTServer = new javax.swing.JMenuItem();
        jMenuItemManageLiveConnections = new javax.swing.JMenuItem();
        jCheckBoxMenuItemPromptLogData = new javax.swing.JCheckBoxMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItemROI = new javax.swing.JMenuItem();
        jMenuItemEditTimeProj = new javax.swing.JMenuItem();
        jMenuView = new javax.swing.JMenu();
        jMenu6 = new javax.swing.JMenu();
        jCheckBoxMenuItemShowBackground = new javax.swing.JCheckBoxMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jCheckBoxMenuItemRepositionBackground = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemBackgroundGray = new javax.swing.JCheckBoxMenuItem();
        jMenuItemFit = new javax.swing.JMenuItem();
        jCheckBoxMenuItemDebug = new javax.swing.JCheckBoxMenuItem();
        jMenuItemRandomColors = new javax.swing.JMenuItem();
        jMenuItemDefaultColors = new javax.swing.JMenuItem();
        jMenuItemSetSelectedTracksColor = new javax.swing.JMenuItem();
        jMenuItemShowVelocities = new javax.swing.JMenuItem();
        jCheckBoxMenuItemShowLabels = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItemGtOnTop = new javax.swing.JCheckBoxMenuItem();
        jMenuItemChangeSourceLabel = new javax.swing.JMenuItem();
        jMenuItemShowTimeLocal = new javax.swing.JMenuItem();
        jCheckBoxMenuItemGrayTracks = new javax.swing.JCheckBoxMenuItem();
        jMenuItemSetTrackTailHighlightTime = new javax.swing.JMenuItem();
        jCheckBoxMenuItemShowDisconnected = new javax.swing.JCheckBoxMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItemShowROCCurve = new javax.swing.JMenuItem();
        jMenuItemShowStatVTime = new javax.swing.JMenuItem();
        jMenuItemTransformSelected = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jCheckBoxMenuItemIgnoreSUTVelocities = new javax.swing.JCheckBoxMenuItem();
        jMenuItem2DRegistration = new javax.swing.JMenuItem();
        jMenuItemComputeTimeOffset = new javax.swing.JMenuItem();
        jMenuItemShowComputedVelocities = new javax.swing.JMenuItem();
        jMenuItemComputeMinSUTRadius = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItemGenRandom = new javax.swing.JMenuItem();
        jMenuMode = new javax.swing.JMenu();
        jRadioButtonMenuItemModeNormal = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItemModeFalseOccupied = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItemModeFalseClear = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItemModeGTOccupied = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItemModeSUTOccupied = new javax.swing.JRadioButtonMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jCheckBoxMenuItemShowOnlySelected = new javax.swing.JCheckBoxMenuItem();
        jMenuItemShowAllTracks = new javax.swing.JMenuItem();
        jMenuItemHideAllTracks = new javax.swing.JMenuItem();
        jMenuItemShowSelectedTracks = new javax.swing.JMenuItem();
        jMenuItemHideSelectedTracks = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItemHelpOverview = new javax.swing.JMenuItem();

        jDialog1.setTitle("HTPM Global Stats");
        jDialog1.setMinimumSize(new java.awt.Dimension(400, 600));

        jTextAreaStats.setColumns(20);
        jTextAreaStats.setRows(5);
        jTextAreaStats.setText("Ground-Truth:\nnum_tracks=0\nnum_points=0\nmax_time_diff=0.0\nmin_time_diff=0.0\navg_time_diff=0.0\nmax_dist_diff=0.0\nmin_dist_diff=0.0\navg_dist_diff=0.0\navg_computed_vel=0.0\navg_reported_vel=0.0\n\n\nSystem-Under-Test:\nnum_tracks=0\nnum_points=0\nmax_time_diff=0.0\nmin_time_diff=0.0\navg_time_diff=0.0\nmax_dist_diff=0.0\nmin_dist_diff=0.0\navg_dist_diff=0.0\navg_computed_vel=0.0\navg_reported_vel=0.0\n");
        jScrollPane1.setViewportView(jTextAreaStats);

        jButtonStatsDialogOk.setText("Ok");
        jButtonStatsDialogOk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStatsDialogOkActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jDialog1Layout = new javax.swing.GroupLayout(jDialog1.getContentPane());
        jDialog1.getContentPane().setLayout(jDialog1Layout);
        jDialog1Layout.setHorizontalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonStatsDialogOk))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE))
                .addContainerGap())
        );
        jDialog1Layout.setVerticalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 515, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonStatsDialogOk)
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("HumanTracking Performance Analysis");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent evt) {
                formWindowStateChanged(evt);
            }
        });

        jSplitPane1.setDividerLocation(150);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("All");
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.setPreferredSize(new java.awt.Dimension(300, 66));
        jTree1.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            public void treeCollapsed(javax.swing.event.TreeExpansionEvent evt) {
                jTree1TreeCollapsed(evt);
            }
            public void treeExpanded(javax.swing.event.TreeExpansionEvent evt) {
                jTree1TreeExpanded(evt);
            }
        });
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPaneTree.setViewportView(jTree1);

        jSplitPane1.setLeftComponent(jScrollPaneTree);

        drawPanel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                drawPanel1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                drawPanel1MouseReleased(evt);
            }
        });
        drawPanel1.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                drawPanel1MouseDragged(evt);
            }
        });

        javax.swing.GroupLayout drawPanel1Layout = new javax.swing.GroupLayout(drawPanel1);
        drawPanel1.setLayout(drawPanel1Layout);
        drawPanel1Layout.setHorizontalGroup(
            drawPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1267, Short.MAX_VALUE)
        );
        drawPanel1Layout.setVerticalGroup(
            drawPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 644, Short.MAX_VALUE)
        );

        jScrollPaneDrawPanel.setViewportView(drawPanel1);

        jSplitPane1.setRightComponent(jScrollPaneDrawPanel);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 630, Short.MAX_VALUE)
        );

        jSliderTime.setMaximum(1000);
        jSliderTime.setPaintLabels(true);
        jSliderTime.setValue(500);
        jSliderTime.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderTimeStateChanged(evt);
            }
        });

        jSliderZoom.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderZoomStateChanged(evt);
            }
        });

        jLabelTime.setText("Time:");

        jLabel2.setText("Zoom:");

        jLabel3.setText("Conf:");

        jSliderConfidence.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderConfidenceStateChanged(evt);
            }
        });

        jLabel4.setText("Grid(in m.):");

        jTextFieldGrid.setText("1.0");
        jTextFieldGrid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldGridActionPerformed(evt);
            }
        });

        jLabel5.setText("GT Radius Inc:");

        jTextFieldGTRadius.setText("1.0");
        jTextFieldGTRadius.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldGTRadiusActionPerformed(evt);
            }
        });

        jLabel6.setText("SUT Radius Inc(in m):");

        jTextFieldSUTRadius.setText("1.0");
        jTextFieldSUTRadius.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSUTRadiusActionPerformed(evt);
            }
        });

        jCheckBoxLive.setText("Live");
        jCheckBoxLive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxLiveActionPerformed(evt);
            }
        });

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        jButtonNewLogFile.setText(" New Log File ");
        jButtonNewLogFile.setFocusable(false);
        jButtonNewLogFile.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonNewLogFile.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonNewLogFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNewLogFileActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonNewLogFile);

        jButtonLogSingleFrame.setText(" Log Single Frame to log.csv ");
        jButtonLogSingleFrame.setFocusable(false);
        jButtonLogSingleFrame.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonLogSingleFrame.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonLogSingleFrame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLogSingleFrameActionPerformed(evt);
            }
        });
        jToolBar1.add(jButtonLogSingleFrame);
        jToolBar1.add(jSeparator5);

        buttonGroupDragMode.add(jRadioButtonMeasure);
        jRadioButtonMeasure.setText(" Measure ");
        jRadioButtonMeasure.setFocusable(false);
        jRadioButtonMeasure.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jRadioButtonMeasure.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jRadioButtonMeasure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMeasureActionPerformed(evt);
            }
        });
        jToolBar1.add(jRadioButtonMeasure);

        buttonGroupDragMode.add(jRadioButtonPan);
        jRadioButtonPan.setSelected(true);
        jRadioButtonPan.setText(" Pan ");
        jRadioButtonPan.setFocusable(false);
        jRadioButtonPan.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jRadioButtonPan.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jRadioButtonPan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonPanActionPerformed(evt);
            }
        });
        jToolBar1.add(jRadioButtonPan);

        buttonGroupDragMode.add(jRadioButtonSelectTracks);
        jRadioButtonSelectTracks.setText(" Select Tracks ");
        jRadioButtonSelectTracks.setFocusable(false);
        jRadioButtonSelectTracks.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jRadioButtonSelectTracks.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jRadioButtonSelectTracks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonSelectTracksActionPerformed(evt);
            }
        });
        jToolBar1.add(jRadioButtonSelectTracks);

        jMenu1.setText("File");

        jMenu8.setText("Settings");

        jMenuItemSaveSettings.setText("Save Settings File ...");
        jMenuItemSaveSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveSettingsActionPerformed(evt);
            }
        });
        jMenu8.add(jMenuItemSaveSettings);

        jMenuItemOpenSettings.setText("Open Settings File ...");
        jMenuItemOpenSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenSettingsActionPerformed(evt);
            }
        });
        jMenu8.add(jMenuItemOpenSettings);

        jMenuItemResetSettings.setText("Reset Settings");
        jMenuItemResetSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemResetSettingsActionPerformed(evt);
            }
        });
        jMenu8.add(jMenuItemResetSettings);

        jMenu1.add(jMenu8);

        jMenuItemOpenGroundTruth.setText("Open Ground Truth CSV");
        jMenuItemOpenGroundTruth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenGroundTruthActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemOpenGroundTruth);

        jMenuItemOpenSutCsv.setText("Open System Under Test CSV");
        jMenuItemOpenSutCsv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenSutCsvActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemOpenSutCsv);
        jMenu1.add(jSeparator1);

        jMenuItemSaveGroundTruth.setText("Save Ground Truth CSV");
        jMenuItemSaveGroundTruth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveGroundTruthActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveGroundTruth);

        jMenuItemSaveSut.setText("Save System Under Test CSV");
        jMenuItemSaveSut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveSutActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveSut);
        jMenu1.add(jSeparator3);

        jCheckBoxMenuItemSaveImages.setText("Save False Occluded/Clear Images");
        jMenu1.add(jCheckBoxMenuItemSaveImages);

        jCheckBoxMenuItemPlay.setText("Play");
        jCheckBoxMenuItemPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPlayActionPerformed(evt);
            }
        });
        jMenu1.add(jCheckBoxMenuItemPlay);

        jCheckBoxMenuItemPlayAndMakeMovie.setText("Play and Make Movie");
        jCheckBoxMenuItemPlayAndMakeMovie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPlayAndMakeMovieActionPerformed(evt);
            }
        });
        jMenu1.add(jCheckBoxMenuItemPlayAndMakeMovie);

        jCheckBoxMenuItemRecordLive.setText("Record Live Data");
        jCheckBoxMenuItemRecordLive.setEnabled(false);
        jMenu1.add(jCheckBoxMenuItemRecordLive);

        jMenuItemSaveImages.setText("Save Image(s)");
        jMenuItemSaveImages.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveImagesActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemSaveImages);

        jMenuItemGotoTime.setText("Goto Time ...");
        jMenuItemGotoTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGotoTimeActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemGotoTime);

        jMenuItemGotoPlotterMinTime.setText("Goto Plotter Min Time");
        jMenuItemGotoPlotterMinTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGotoPlotterMinTimeActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemGotoPlotterMinTime);

        jMenuItemClearData.setText("Clear Data");
        jMenuItemClearData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemClearDataActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItemClearData);

        jMenuRecentGroundTruthCsv.setText("Recent Ground Truth CSV");
        jMenu1.add(jMenuRecentGroundTruthCsv);

        jMenuRecentSystemUnderTestCsv.setText("Recent System Under Test CSV");
        jMenu1.add(jMenuRecentSystemUnderTestCsv);

        jMenuBar1.add(jMenu1);

        jMenuConnections.setText("Connections");

        jCheckBoxMenuItemOptitrack.setText("Connect/Show Live Optitrack Data");
        jCheckBoxMenuItemOptitrack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemOptitrackActionPerformed(evt);
            }
        });
        jMenuConnections.add(jCheckBoxMenuItemOptitrack);

        jCheckBoxMenuItemAcceptGT.setText("Open Port to Accept GT data (2113)");
        jCheckBoxMenuItemAcceptGT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemAcceptGTActionPerformed(evt);
            }
        });
        jMenuConnections.add(jCheckBoxMenuItemAcceptGT);

        jCheckBoxMenuItemAcceptSutData.setText("Open Port to Accept SUT data(2112) ");
        jCheckBoxMenuItemAcceptSutData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemAcceptSutDataActionPerformed(evt);
            }
        });
        jMenuConnections.add(jCheckBoxMenuItemAcceptSutData);

        jMenuItemConnectGTServer.setText("Connect to Server of GT data ...");
        jMenuItemConnectGTServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectGTServerActionPerformed(evt);
            }
        });
        jMenuConnections.add(jMenuItemConnectGTServer);

        jMenuItemConnectSUTServer.setText("Connect to Server of SUT data ...");
        jMenuItemConnectSUTServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectSUTServerActionPerformed(evt);
            }
        });
        jMenuConnections.add(jMenuItemConnectSUTServer);

        jMenuItemManageLiveConnections.setText("Manage Live Connections ...");
        jMenuItemManageLiveConnections.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemManageLiveConnectionsActionPerformed(evt);
            }
        });
        jMenuConnections.add(jMenuItemManageLiveConnections);

        jCheckBoxMenuItemPromptLogData.setText("Prompt for Logging Data");
        jCheckBoxMenuItemPromptLogData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemPromptLogDataActionPerformed(evt);
            }
        });
        jMenuConnections.add(jCheckBoxMenuItemPromptLogData);

        jMenuBar1.add(jMenuConnections);

        jMenu2.setText("Edit");

        jMenuItemROI.setText("Change Region of Interest");
        jMenuItemROI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemROIActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemROI);

        jMenuItemEditTimeProj.setText("Change time interval for forward projection.");
        jMenuItemEditTimeProj.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemEditTimeProjActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItemEditTimeProj);

        jMenuBar1.add(jMenu2);

        jMenuView.setText("View");

        jMenu6.setText("Background");

        jCheckBoxMenuItemShowBackground.setSelected(true);
        jCheckBoxMenuItemShowBackground.setText("Show Background Image");
        jCheckBoxMenuItemShowBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemShowBackgroundActionPerformed(evt);
            }
        });
        jMenu6.add(jCheckBoxMenuItemShowBackground);
        jMenu6.add(jSeparator2);

        jCheckBoxMenuItemRepositionBackground.setText("Reposition Background");
        jCheckBoxMenuItemRepositionBackground.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemRepositionBackgroundActionPerformed(evt);
            }
        });
        jMenu6.add(jCheckBoxMenuItemRepositionBackground);

        jCheckBoxMenuItemBackgroundGray.setText("Show Background in Gray Scale");
        jCheckBoxMenuItemBackgroundGray.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemBackgroundGrayActionPerformed(evt);
            }
        });
        jMenu6.add(jCheckBoxMenuItemBackgroundGray);

        jMenuView.add(jMenu6);

        jMenuItemFit.setText("Fit");
        jMenuItemFit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemFitActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemFit);

        jCheckBoxMenuItemDebug.setText("Debug");
        jCheckBoxMenuItemDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemDebugActionPerformed(evt);
            }
        });
        jMenuView.add(jCheckBoxMenuItemDebug);

        jMenuItemRandomColors.setText("Randomize Colors");
        jMenuItemRandomColors.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRandomColorsActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemRandomColors);

        jMenuItemDefaultColors.setText("Default Colors");
        jMenuItemDefaultColors.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDefaultColorsActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemDefaultColors);

        jMenuItemSetSelectedTracksColor.setText("Set Selected Tracks Color");
        jMenuItemSetSelectedTracksColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetSelectedTracksColorActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemSetSelectedTracksColor);

        jMenuItemShowVelocities.setText("Show Velocities");
        jMenuItemShowVelocities.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowVelocitiesActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemShowVelocities);

        jCheckBoxMenuItemShowLabels.setText("Show Labels");
        jCheckBoxMenuItemShowLabels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemShowLabelsActionPerformed(evt);
            }
        });
        jMenuView.add(jCheckBoxMenuItemShowLabels);

        jCheckBoxMenuItemGtOnTop.setSelected(true);
        jCheckBoxMenuItemGtOnTop.setText("GT on Top");
        jCheckBoxMenuItemGtOnTop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemGtOnTopActionPerformed(evt);
            }
        });
        jMenuView.add(jCheckBoxMenuItemGtOnTop);

        jMenuItemChangeSourceLabel.setText("Chage Selected Source Label");
        jMenuItemChangeSourceLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChangeSourceLabelActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemChangeSourceLabel);

        jMenuItemShowTimeLocal.setText("Show Current Time in Local Format");
        jMenuItemShowTimeLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowTimeLocalActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemShowTimeLocal);

        jCheckBoxMenuItemGrayTracks.setSelected(true);
        jCheckBoxMenuItemGrayTracks.setText("Use Gray Tracks");
        jCheckBoxMenuItemGrayTracks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemGrayTracksActionPerformed(evt);
            }
        });
        jMenuView.add(jCheckBoxMenuItemGrayTracks);

        jMenuItemSetTrackTailHighlightTime.setText("Set Track Tail Highlight Time ... ");
        jMenuItemSetTrackTailHighlightTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSetTrackTailHighlightTimeActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemSetTrackTailHighlightTime);

        jCheckBoxMenuItemShowDisconnected.setText("Show Disconnected Points");
        jCheckBoxMenuItemShowDisconnected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemShowDisconnectedActionPerformed(evt);
            }
        });
        jMenuView.add(jCheckBoxMenuItemShowDisconnected);

        jMenuBar1.add(jMenuView);

        jMenu7.setText("Calculations");
        jMenu7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu7ActionPerformed(evt);
            }
        });

        jMenuItemShowROCCurve.setText("Compute ROC Curve");
        jMenuItemShowROCCurve.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowROCCurveActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItemShowROCCurve);

        jMenuItemShowStatVTime.setText("Compute Stats  Versus Time");
        jMenuItemShowStatVTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowStatVTimeActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItemShowStatVTime);

        jMenuItemTransformSelected.setText("Transform Selected Data");
        jMenuItemTransformSelected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTransformSelectedActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItemTransformSelected);

        jMenuItem1.setText("Show Global Stats");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem1);

        jCheckBoxMenuItemIgnoreSUTVelocities.setText("Ignore SUT Velocities");
        jCheckBoxMenuItemIgnoreSUTVelocities.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemIgnoreSUTVelocitiesActionPerformed(evt);
            }
        });
        jMenu7.add(jCheckBoxMenuItemIgnoreSUTVelocities);

        jMenuItem2DRegistration.setText("2D Registration");
        jMenuItem2DRegistration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2DRegistrationActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem2DRegistration);

        jMenuItemComputeTimeOffset.setText("Compute Time Offset from Velocity Correllation");
        jMenuItemComputeTimeOffset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemComputeTimeOffsetActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItemComputeTimeOffset);

        jMenuItemShowComputedVelocities.setText("Show Computed Velocities For Correllation");
        jMenuItemShowComputedVelocities.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowComputedVelocitiesActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItemShowComputedVelocities);

        jMenuItemComputeMinSUTRadius.setText("Compute Min SUT Radius");
        jMenuItemComputeMinSUTRadius.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemComputeMinSUTRadiusActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItemComputeMinSUTRadius);

        jMenuBar1.add(jMenu7);

        jMenu3.setText("Sim");

        jMenuItemGenRandom.setText("Generate Random Data");
        jMenuItemGenRandom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGenRandomActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItemGenRandom);

        jMenuBar1.add(jMenu3);

        jMenuMode.setText("Mode");

        buttonGroupMode.add(jRadioButtonMenuItemModeNormal);
        jRadioButtonMenuItemModeNormal.setSelected(true);
        jRadioButtonMenuItemModeNormal.setText("Normal");
        jRadioButtonMenuItemModeNormal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemModeNormalActionPerformed(evt);
            }
        });
        jMenuMode.add(jRadioButtonMenuItemModeNormal);

        buttonGroupMode.add(jRadioButtonMenuItemModeFalseOccupied);
        jRadioButtonMenuItemModeFalseOccupied.setText("False Occupied");
        jRadioButtonMenuItemModeFalseOccupied.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemModeFalseOccupiedActionPerformed(evt);
            }
        });
        jMenuMode.add(jRadioButtonMenuItemModeFalseOccupied);

        buttonGroupMode.add(jRadioButtonMenuItemModeFalseClear);
        jRadioButtonMenuItemModeFalseClear.setText("False Clear");
        jRadioButtonMenuItemModeFalseClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemModeFalseClearActionPerformed(evt);
            }
        });
        jMenuMode.add(jRadioButtonMenuItemModeFalseClear);

        buttonGroupMode.add(jRadioButtonMenuItemModeGTOccupied);
        jRadioButtonMenuItemModeGTOccupied.setText("GT Occupied");
        jRadioButtonMenuItemModeGTOccupied.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemModeGTOccupiedActionPerformed(evt);
            }
        });
        jMenuMode.add(jRadioButtonMenuItemModeGTOccupied);

        buttonGroupMode.add(jRadioButtonMenuItemModeSUTOccupied);
        jRadioButtonMenuItemModeSUTOccupied.setText("SUT Occupied");
        jRadioButtonMenuItemModeSUTOccupied.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemModeSUTOccupiedActionPerformed(evt);
            }
        });
        jMenuMode.add(jRadioButtonMenuItemModeSUTOccupied);

        jMenuBar1.add(jMenuMode);

        jMenu5.setText("Show/Hide Tracks");

        jCheckBoxMenuItemShowOnlySelected.setText("Show Only Selected");
        jCheckBoxMenuItemShowOnlySelected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemShowOnlySelectedActionPerformed(evt);
            }
        });
        jMenu5.add(jCheckBoxMenuItemShowOnlySelected);

        jMenuItemShowAllTracks.setText("Show All");
        jMenuItemShowAllTracks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowAllTracksActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItemShowAllTracks);

        jMenuItemHideAllTracks.setText("Hide All");
        jMenuItemHideAllTracks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHideAllTracksActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItemHideAllTracks);

        jMenuItemShowSelectedTracks.setText("Show Selected");
        jMenuItemShowSelectedTracks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemShowSelectedTracksActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItemShowSelectedTracks);

        jMenuItemHideSelectedTracks.setText("Hide Selected");
        jMenuItemHideSelectedTracks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHideSelectedTracksActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItemHideSelectedTracks);

        jMenuBar1.add(jMenu5);

        jMenu4.setText("Help");
        jMenu4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jMenu4.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jMenuItemHelpOverview.setText("Overview");
        jMenuItemHelpOverview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHelpOverviewActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItemHelpOverview);

        jMenuBar1.add(jMenu4);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jCheckBoxLive)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabelTime)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSliderTime, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSliderConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSliderZoom, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldGrid, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldGTRadius, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(4, 4, 4)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldSUTRadius, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 82, Short.MAX_VALUE))))
                    .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jSliderTime, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jLabelTime)
                                                .addComponent(jCheckBoxLive))
                                            .addContainerGap()))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addContainerGap()))
                                .addComponent(jSliderConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addContainerGap()))
                        .addComponent(jSliderZoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jTextFieldGrid, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(jTextFieldGTRadius, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldSUTRadius, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addContainerGap())))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemGenRandomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGenRandomActionPerformed
        this.ClearData();
        GenRandomData();
        this.updateEverything();
    }//GEN-LAST:event_jMenuItemGenRandomActionPerformed

    private void jSliderZoomStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderZoomStateChanged
        updateDrawPanelViewport();
    }//GEN-LAST:event_jSliderZoomStateChanged
    /**
     * Maximum scale to zoom to. The scale is the ratio of the height or width
     * of the draw panel to the area of it shown by the JScrollPane.
     */
    public double max_scale = 20.0;

    /**
     * Convert form slider value to JScrollPane scale.
     *
     * @return scale, the ratio of the height or width of the draw panel to the
     * area of it shown by the JScrollPane.
     */
    public double zoomSliderToScale() {
        int v = this.jSliderZoom.getValue();
        v = v - this.jSliderZoom.getMinimum();
        double dv = ((double) v) / (this.jSliderZoom.getMaximum() - this.jSliderZoom.getMinimum());
        dv = 1.0 + dv * (max_scale - 1.0);
        return dv;
    }

    /**
     * Convert JScrollPane scale to value to set slider to.
     *
     * @param dv scale, the ratio of the height or width of the draw panel to
     * the area of it shown by the JScrollPane.
     */
    public void scaleToZoomSlider(double dv) {
        dv = (max_scale - 1.0) * (dv - 1.0);
        dv = dv * (this.jSliderZoom.getMaximum() - this.jSliderZoom.getMinimum());
        int v = (int) (dv + this.jSliderZoom.getMinimum());
        this.jSliderZoom.setValue(v);
    }

    /**
     * After zooming or fitting update the JScrollPane's viewport.
     */
    public final void updateDrawPanelViewport() {
        JViewport vp = this.jScrollPaneDrawPanel.getViewport();
        Rectangle rect = vp.getViewRect();
        double scale = this.zoomSliderToScale();
        Dimension d = new Dimension();
        d.width = (int) (rect.width * scale);
        d.height = (int) (rect.height * scale);
        Dimension old_pref_size = this.drawPanel1.getPreferredSize();
        this.drawPanel1.setPreferredSize(d);
        this.drawPanel1.revalidate();
        this.jScrollPaneDrawPanel.revalidate();
        this.repaint();
        double old_center_x = rect.x + rect.width / 2;
        double old_center_y = rect.y + rect.height / 2;
        double center_x = old_center_x * d.width / ((double) old_pref_size.width);
        double center_y = old_center_y * d.height / ((double) old_pref_size.height);
        Point new_upper_corner = new Point((int) (center_x - rect.width / 2),
                ((int) center_y - rect.height / 2));
        if (new_upper_corner.x < 0) {
            new_upper_corner.x = 0;
        }
        if (new_upper_corner.y < 0) {
            new_upper_corner.y = 0;
        }
        vp.setViewPosition(new_upper_corner);
    }

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        updateDrawPanelViewport();
    }//GEN-LAST:event_formComponentResized

    /**
     * Read a csv file and treat it as ground-truth data.
     *
     * @param filename name of file to read
     */
    public static void LoadGroundTruthFile(String filename, CsvParseOptions o) {
        gtlist = LoadFile(filename, Color.red, true, o);
        recomputeTimeLimits();
    }

    /**
     * Read a csv file and treat it as system under test data.
     *
     * @param filename name of file to read
     */
    public static void LoadSystemUnderTestFile(String filename, CsvParseOptions o) {
        sutlist = LoadFile(filename, Color.blue, false, o);
        recomputeTimeLimits();
    }
    private String groundTruthFileName = null;

    public JMenuItem createRecentFileMenuItem(final CsvParseOptions o,
            final boolean _is_ground_truth) {
        JMenuItem mi = new JMenuItem();
        mi.setText(o.filename);
        mi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (_is_ground_truth) {
                    if (null == gtlist) {
                        gtlist = LoadFile(o.filename, Color.red, true, o);
                    } else {
                        List<Track> list_to_add = LoadFile(o.filename, Color.red,
                                true, o);
                        gtlist.addAll(list_to_add);
                    }
                } else {
                    if (null == sutlist) {
                        sutlist = LoadFile(o.filename, Color.blue, false, o);
                    } else {
                        List<Track> list_to_add = LoadFile(o.filename, Color.blue,
                                true, o);
                        sutlist.addAll(list_to_add);
                    }
                }
                updateEverything();
            }
        });
        return mi;
    }

    private void jMenuItemOpenGroundTruthActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenGroundTruthActionPerformed

        try {
            if (sutlist == null) {
                inner_min_time = Double.POSITIVE_INFINITY;
                inner_max_time = Double.NEGATIVE_INFINITY;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(s.gt_open_file_dir));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Comma Seperated Variable Files", "csv");
            chooser.setFileFilter(filter);
            chooser.setMultiSelectionEnabled(true);
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                gtlist = null;
                CsvParseOptions o = null;
                for (File f : chooser.getSelectedFiles()) {
                    groundTruthFileName = f.getCanonicalPath();
                    System.out.println("You chose to open this file ground truth file: "
                            + groundTruthFileName);
                    if (chooser.getSelectedFile().exists() && chooser.getSelectedFile().canRead()) {
                        s.gt_open_file_dir = chooser.getSelectedFile().getParentFile().getCanonicalPath();
                    }
                    if (o == null) {
                        o = CsvParseOptionsJPanel.showDialog(this, chooser.getSelectedFile());
                        if (o == null) {
                            return;
                        }
                    }
                    o.filename = groundTruthFileName;
                    File recentFilesDir = new File(System.getProperty("user.home"), ".htpm_recent_files");
                    File recentGTFilesDir = new File(recentFilesDir, ".gt");
                    recentGTFilesDir.mkdirs();
                    File infoFile = File.createTempFile(f.getName() + "_", "_info.txt", recentGTFilesDir);
                    PrintStream ps = new PrintStream(new FileOutputStream(infoFile));
                    ps.println(o.toString());
                    ps.close();
                    this.jMenuRecentGroundTruthCsv.add(this.createRecentFileMenuItem(o, true));
                    if (null == gtlist) {
                        gtlist = LoadFile(groundTruthFileName, Color.red, true, o);
                    } else {
                        List<Track> list_to_add = LoadFile(groundTruthFileName, Color.red,
                                true, o);
                        gtlist.addAll(list_to_add);
                    }
                    //sutlist = LoadFile("sut.csv", Color.blue, false);
                }
                updateEverything();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }//GEN-LAST:event_jMenuItemOpenGroundTruthActionPerformed

    private void addListAtTreeNode(DefaultMutableTreeNode top_child, List<Track> tracks) {
        if (null != tracks) {
            for (int gti = 0; gti < tracks.size(); gti++) {
                Track t = tracks.get(gti);
                String source = t.source;
                DefaultMutableTreeNode source_child = null;
                for (int ci = 0; ci < top_child.getChildCount(); ci++) {
                    DefaultMutableTreeNode potential_source_child = (DefaultMutableTreeNode) top_child.getChildAt(ci);
                    if (potential_source_child.getUserObject().toString().compareTo(source) == 0) {
                        source_child = potential_source_child;
                        break;
                    }
                }
                if (source_child == null) {
                    source_child = new DefaultMutableTreeNode(source);
                    top_child.add(source_child);
                }
                DefaultMutableTreeNode n = new DefaultMutableTreeNode(t);
                boolean already_added = false;
                String ts = t.toString();
                for (int ci = 0; ci < source_child.getChildCount(); ci++) {
                    DefaultMutableTreeNode cn = (DefaultMutableTreeNode) source_child.getChildAt(ci);
                    if (cn.getUserObject().toString().compareTo(ts) == 0) {
                        already_added = true;
//                        System.err.println("Node " + ts + " added to list " + source + " twice.");
//                        Thread.dumpStack();
                        break;
                    }
                }
                if (!already_added) {
                    source_child.add(n);
                }
            }
        }
    }

    /**
     * Update the tree display on the left side of the window.
     */
    public void updateTree(boolean new_model) {

        DefaultTreeModel model = null;
        DefaultTreeModel old_model = (DefaultTreeModel) this.jTree1.getModel();
        if (new_model) {
            DefaultMutableTreeNode root = null;
            try {
                if (null != old_model) {
                    root = (DefaultMutableTreeNode) old_model.getRoot();
                }
            } catch (Exception e) {
            }
            if (null != root) {
                root = new DefaultMutableTreeNode("All");
            }
            model = new DefaultTreeModel(root);
        } else {
            model = old_model;
        }

        DefaultMutableTreeNode top = (DefaultMutableTreeNode) model.getRoot();
        DefaultMutableTreeNode gt_top_child = null;
        Enumeration children_e = top.children();
        while (children_e.hasMoreElements()) {
            DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) children_e.nextElement();
            if (child.getUserObject().toString().compareTo("Ground-Truth") == 0) {
                gt_top_child = child;
                break;
            }
        }
        if (gt_top_child == null) {
            gt_top_child = new DefaultMutableTreeNode("Ground-Truth");
            top.add(gt_top_child);
        }
        addListAtTreeNode(gt_top_child, gtlist);
        DefaultMutableTreeNode sut_top_child = null;
        children_e = top.children();
        while (children_e.hasMoreElements()) {
            DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) children_e.nextElement();
            if (child.getUserObject().toString().compareTo("System-Under-Test") == 0) {
                sut_top_child = child;
                break;
            }
        }
        if (sut_top_child == null) {
            sut_top_child = new DefaultMutableTreeNode("System-Under-Test");
            top.add(sut_top_child);
        }
        addListAtTreeNode(sut_top_child, sutlist);
        if (new_model) {
            this.jTree1.setModel(model);
        }
        this.jTree1.revalidate();
        this.jTree1.repaint();
    }

    private void combineTracks() {
        this.drawPanel1.tracks = new ArrayList<Track>();
        if (s.gt_on_top) {
            if (null != sutlist) {
                this.drawPanel1.tracks.addAll(sutlist);
            }
            if (null != gtlist) {
                this.drawPanel1.tracks.addAll(gtlist);
            }
        } else {
            if (null != gtlist) {
                this.drawPanel1.tracks.addAll(gtlist);
            }
            if (null != sutlist) {
                this.drawPanel1.tracks.addAll(sutlist);
            }
        }
    }

    /**
     * Update all displays and components.
     */
    public void updateEverything() {

        recomputeTimeLimits();
        this.jSliderTime.setValue(0);
        this.jSliderZoom.setValue(0);
        this.setCurrentTime(inner_min_time + 0.001);
        this.combineTracks();
        this.updateTree(false);
        this.updateDrawPanelViewport();
        this.Fit();
    }

    /**
     * Find the current_index and interpolate currentPoint if necessary for all
     * tracks for the given time
     *
     * @param time time in seconds since 1970
     */
    public static void setTracksCurrentTimes(double time) {
        if (null != sutlist) {
            for (Track t : sutlist) {
                t.setCurrentTime(time);
            }
        }
        if (null != gtlist) {
            for (Track t : gtlist) {
                t.setCurrentTime(time);
            }
        }
    }
    /**
     * Time in seconds since 1970 for current position in log.
     */
    public static double CurrentTime = 0.0;

    public final void setCurrentTime(double time) {
        setCurrentTime(time, true);
    }

    /**
     * Set the current time for display/analysis to the passed parameter.
     *
     * @param time seconds since 1970
     */
    public final void setCurrentTime(double time, boolean slider_update) {
        try {
            if (time == CurrentTime) {
                return;
            }
            setTracksCurrentTimes(time);
            boolean save_images = this.jCheckBoxMenuItemSaveImages.isSelected();
            if (save_images) {
                updateStats(true);
            }

            this.drawPanel1.repaint();
            HTPM_JFrame.CurrentTime = time;
            if (slider_update) {
                setSliderFromTime(time);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Count pixels that are not zero (BLACK) in an image.
     *
     * @param bi buffered image to count pixels for
     * @return number of non-zero pixels
     */
    public static int countPixelsImage(BufferedImage bi) {
        int count = 0;
        bi.flush();
        int ia[] = new int[bi.getWidth() * bi.getHeight()];
        bi.getData().getPixels(0, 0, bi.getWidth(), bi.getHeight(), ia);
        for (int i_index = 0; i_index < ia.length; i_index++) {
            int i = ia[i_index];
            if (i != 0) {
                count++;
            }
        }
        return count;
    }
    /**
     * List of FrameStats accumulated while processing an entire period of time
     * for an experiment.
     */
    public static ArrayList<FrameStats> frame_stats_list = new ArrayList<FrameStats>();
    public static BufferedImage update_stat_image = null;

    private static BufferedImage getImageForUpdateStat() {
        if (!HTPM_JFrame.update_stat_keep_image) {
            HTPM_JFrame.update_stat_image = null;
        }
        if (HTPM_JFrame.update_stat_image != null) {
            return HTPM_JFrame.update_stat_image;
        }
        int img_w = (int) ((s.roi_x_max - s.roi_x_min) * HTPM_JFrame.s.scale);
        int img_h = (int) ((s.roi_y_max - s.roi_y_min) * HTPM_JFrame.s.scale);
        BufferedImage bi =
                new BufferedImage(img_w, img_h, BufferedImage.TYPE_BYTE_GRAY);
        if (HTPM_JFrame.update_stat_keep_image) {
            HTPM_JFrame.update_stat_image = bi;
        }
        return bi;
    }

    /**
     * Compute the FrameStats for one frame or one instant of time. Several
     * statistics related to the performance of the system under test in
     * reporting positions of humans(or trackables or recievers) are computed.
     * See the FrameStats class documentation for more info.
     *
     * @param save_images if true save images for later debugging/visualization.
     * @return
     */
    public static FrameStats updateStats(boolean save_images) {
        if (HTPM_JFrame.sutlist == null
                || HTPM_JFrame.sutlist.size() < 1
                || HTPM_JFrame.sutlist.get(0) == null) {
            return null;
        }
        if (HTPM_JFrame.sutlist == null
                || HTPM_JFrame.sutlist.size() < 1
                || HTPM_JFrame.sutlist.get(0) == null) {
            return null;
        }
        Track first_sut_track = HTPM_JFrame.sutlist.get(0);
        if (first_sut_track.cur_time_index < 1
                || first_sut_track.data == null
                || first_sut_track.cur_time_index > first_sut_track.data.size() - 1) {
            return null;
        }
        Track first_gt_track = HTPM_JFrame.gtlist.get(0);
        if (first_gt_track.cur_time_index < 1
                || first_gt_track.data == null
                || first_gt_track.cur_time_index > first_gt_track.data.size() - 1) {
            return null;
        }
        FrameStats fs = new FrameStats();
        fs.max_gt_to_sut_dist = 0;
        fs.max_sut_to_gt_dist = 0;
        for (Track gtt : HTPM_JFrame.gtlist) {
            double min_dist = Double.POSITIVE_INFINITY;
            if (gtt.currentPoint == null) {
                continue;
            }
            if (gtt.currentPoint.x < s.roi_x_min
                    || gtt.currentPoint.x > s.roi_x_max
                    || gtt.currentPoint.y < s.roi_y_min
                    || gtt.currentPoint.y > s.roi_y_max) {
                continue;
            }
            fs.gt_human_count++;
            for (Track sutt : HTPM_JFrame.sutlist) {
                if (sutt.currentPoint == null) {
                    continue;
                }
                double dist = gtt.currentPoint.distance(sutt.currentPoint);
                if (dist < min_dist) {
                    min_dist = dist;
                }
            }
            if (min_dist > fs.max_gt_to_sut_dist) {
                fs.max_gt_to_sut_dist = min_dist;
            }
            fs.total_gt_to_sut_dist += min_dist;
        }
        for (Track sutt : HTPM_JFrame.sutlist) {
            double min_dist = Double.POSITIVE_INFINITY;
            if (sutt.currentPoint == null) {
                continue;
            }
            if (sutt.currentPoint.x < s.roi_x_min
                    || sutt.currentPoint.x > s.roi_x_max
                    || sutt.currentPoint.y < s.roi_y_min
                    || sutt.currentPoint.y > s.roi_y_max) {
                continue;
            }
            fs.sut_human_count++;
            for (Track gtt : HTPM_JFrame.gtlist) {
                if (gtt.currentPoint == null) {
                    continue;
                }
                double dist = gtt.currentPoint.distance(sutt.currentPoint);
                if (dist < min_dist) {
                    min_dist = dist;
                }
            }
            fs.total_sut_to_gt_dist += min_dist;
            if (min_dist > fs.max_sut_to_gt_dist) {
                fs.max_sut_to_gt_dist = min_dist;
            }
        }
        if (fs.gt_human_count < 1 || fs.sut_human_count < 1) {
            return null;
        }
        fs.avg_gt_to_sut_dist = fs.total_gt_to_sut_dist / fs.gt_human_count;
        fs.avg_sut_to_gt_dist = fs.total_sut_to_gt_dist / fs.sut_human_count;
        BufferedImage bi = HTPM_JFrame.getImageForUpdateStat();
        Graphics g = bi.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        Dimension d = new Dimension(bi.getWidth(), bi.getHeight());
        double scale = s.scale;
        g.setColor(Color.WHITE);
        DrawPanel.fillTrackListCircles(g, d, s.scale,
                HTPM_JFrame.gtlist,
                true,
                s.confidence_threshold,
                s.roi_x_min,
                s.roi_y_min);
        fs.total_gt_occupied_area = countPixelsImage(bi) / 1.0e4;
        String flabel = "_" + s.confidence_threshold + "_" + CurrentTime;
        if (save_images) {
            try {
                bi.flush();
                File f = File.createTempFile("htpm_total_gt_occupied" + flabel,
                        ".jpg");
                ImageIO.write(bi, "jpg", f);
                System.out.println("wrote to " + f);
            } catch (IOException ex) {
                Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        g.setColor(Color.BLACK);
        DrawPanel.fillTrackListCircles(g, d, scale,
                HTPM_JFrame.sutlist,
                false,
                s.confidence_threshold,
                s.roi_x_min,
                s.roi_y_min);
        if (save_images) {
            try {
                bi.flush();
                File f = File.createTempFile("htpm_false_clear_area" + flabel,
                        ".jpg");
                ImageIO.write(bi, "jpg", f);
                System.out.println("wrote to " + f);
            } catch (IOException ex) {
                Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        fs.false_clear_area = countPixelsImage(bi) / 1.0e4;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        g.setColor(Color.WHITE);
        DrawPanel.fillTrackListCircles(g, d, s.scale,
                HTPM_JFrame.sutlist,
                false,
                s.confidence_threshold,
                s.roi_x_min,
                s.roi_y_min);
        if (save_images) {
            try {
                File f = File.createTempFile("total_sut_occupied_area" + flabel,
                        ".jpg");
                bi.flush();
                ImageIO.write(bi, "jpg", f);
                System.out.println("wrote to " + f);
            } catch (IOException ex) {
                Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        fs.total_sut_occupied_area = countPixelsImage(bi) / 1.0e4;
        g.setColor(Color.BLACK);
        DrawPanel.fillTrackListCircles(g, d, s.scale,
                HTPM_JFrame.gtlist,
                true,
                s.confidence_threshold,
                s.roi_x_min,
                s.roi_y_min);
        fs.false_occupied_area = countPixelsImage(bi) / 1.0e4;
        fs.true_occupied_area = fs.total_sut_occupied_area - fs.false_occupied_area;
        fs.true_clear_area = 100.0 - fs.total_sut_occupied_area - fs.false_clear_area;
        if (save_images) {
            try {
                File f = File.createTempFile("false_occupied_area" + flabel,
                        ".jpg");
                bi.flush();
                ImageIO.write(bi, "jpg", f);
                System.out.println("wrote to " + f);
                bi.flush();
            } catch (IOException ex) {
                Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE,
                        null, ex);
            }
        }
        frame_stats_list.add(fs);
        return fs;
    }

    private void setSliderFromTime(double time) {
        if (outer_min_time > 0 && !Double.isInfinite(outer_min_time) && !Double.isNaN(outer_min_time)
                && outer_max_time > outer_min_time && !Double.isInfinite(outer_max_time) && !Double.isNaN(outer_max_time)) {
            int v = this.jSliderTime.getValue();
            double jslide_min = this.jSliderTime.getMinimum();
            double jslide_max = this.jSliderTime.getMaximum();
            //double t = (v - jslide_min) / (jslide_max - jslide_min) * (outer_max_time - outer_min_time) + outer_min_time;
            double dv = 1 + (time - outer_min_time) * (jslide_max - jslide_min - 2) / (outer_max_time - outer_min_time) + jslide_min;
            int new_v = ((int) dv);
            if (v != new_v) {
                this.jSliderTime.setValue(new_v);
            }
        }
    }

    private void jSliderTimeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderTimeStateChanged
        if (playing_back) {
            return;
        }
        if (outer_min_time > 0
                && !Double.isInfinite(outer_min_time)
                && !Double.isNaN(outer_min_time)
                && outer_max_time > outer_min_time
                && !Double.isInfinite(outer_max_time)
                && !Double.isNaN(outer_max_time)) {
            double v = this.jSliderTime.getValue();
            double jslide_min = this.jSliderTime.getMinimum();
            double jslide_max = this.jSliderTime.getMaximum();
            double t = (v - jslide_min - 1) / (jslide_max - jslide_min - 2) * (outer_max_time - outer_min_time) + outer_min_time;
            setCurrentTime(t, false);
            DateFormat df = DateFormat.getDateTimeInstance();
            this.jSliderTime.setToolTipText(df.format(new Date((long) (t * 1e3)))
                    + ": " + String.format("%.3f", (t - outer_min_time)) + "secs from start, "
                    + String.format("%.3f", (outer_max_time - t)) + " secs to end");
        }
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jSliderTimeStateChanged

    private void formWindowStateChanged(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged
        this.updateDrawPanelViewport();
        if (null != playThread) {
            playThread.interrupt();
        }
    }//GEN-LAST:event_formWindowStateChanged
    private Thread playThread = null;
    private boolean playing_back = false;

    private void stopPlayAll() {
        try {
            play_thread_interupt_flag = true;
            try {
                Thread.sleep((long) (2000.0 * time_inc));
            } catch (Exception e) {
            }
            if (null != playThread) {
                Thread t = playThread;
                t.interrupt();
                t.join();
            }
            playing_back = false;
            drawPanel1.closeMovie();
            this.jCheckBoxMenuItemPlay.setSelected(false);
            this.jCheckBoxMenuItemPlayAndMakeMovie.setSelected(false);
            this.jSliderTime.setEnabled(true);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    public static double time_inc = 1.0 / 30.0;
    public static boolean play_thread_interupt_flag = false;

    private void startPlayAll() {
        boolean orig_jCheckBoxMenuItemPlay =
                jCheckBoxMenuItemPlay.isSelected();
        boolean orig_jCheckBoxMenuItemPlayAndMakeMovie =
                this.jCheckBoxMenuItemPlayAndMakeMovie.isSelected();
        stopPlayAll();
        this.jCheckBoxMenuItemPlay.setSelected(orig_jCheckBoxMenuItemPlay);
        this.jCheckBoxMenuItemPlayAndMakeMovie.setSelected(orig_jCheckBoxMenuItemPlayAndMakeMovie);
        this.jSliderTime.setEnabled(false);
        if ((HTPM_JFrame.sutlist == null || HTPM_JFrame.sutlist.size() < 1)
                && (HTPM_JFrame.gtlist == null || HTPM_JFrame.gtlist.size() < 1)) {
            return;
        }
        if (CurrentTime < outer_min_time && !Double.isInfinite(outer_min_time)) {
            CurrentTime = outer_min_time;
        }
        final double start_time = CurrentTime;
        time_inc = 1.0 / ((double) drawPanel1.getMovie_frames_per_second());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
//                double time_inc = (inner_max_time - inner_min_time)
//                        / (sutlist.get(0).data.size() + gtlist.get(0).data.size());
                frame_stats_list = new ArrayList<FrameStats>();
                playing_back = true;
                for (double t = start_time; t <= outer_max_time && playing_back; t += time_inc) {
                    final double t2 = t;
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    if (!playing_back || play_thread_interupt_flag) {
                        break;
                    }
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (!playing_back) {
                                    return;
                                }
                                int slider_v =
                                        (int) (((t2 - inner_min_time)
                                        / (inner_max_time - inner_min_time)
                                        * (jSliderTime.getMaximum() - jSliderTime.getMinimum()))
                                        + jSliderTime.getMinimum());
                                jSliderTime.setValue(slider_v);
                                setCurrentTime(t2);
                                if (make_movie) {
                                    if (!drawPanel1.addMovieFrame()) {
                                        playing_back = false;
                                    }
                                }
                            } catch (Exception exception) {
                                playing_back = false;
                                exception.printStackTrace();
                            }
                        }
                    });
                    if (Thread.currentThread().isInterrupted() || !playing_back) {
                        break;
                    }
                    try {
                        Thread.sleep((long) (time_inc * 1000.0));
                    } catch (Exception e) {
                    };
                }
                playThread = null;
                playing_back = false;
                drawPanel1.closeMovie();
                stopPlayAll();
            }
        });
        play_thread_interupt_flag = false;
        playThread = t;
        t.start();
    }
    public static File frameStatsCsvF = null;
    public static boolean update_stat_keep_image = true;
    public static boolean inc_sut_radius_on_false_clear = false;
    public static FrameStats last_combined_fs = null;

    /**
     * Process an entire experiment or time period.
     *
     * @return
     */
    public static FrameStats processAll() {
        PrintStream ps = null;
        try {
            if (null != frameStatsCsvF) {
                ps = new PrintStream(new FileOutputStream(frameStatsCsvF));
                String field_list_s = "time,";
                for (Field f : FrameStats.class.getFields()) {
                    field_list_s += f.getName() + ",";
                }
                ps.println(field_list_s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        frame_stats_list = new ArrayList<FrameStats>();
        FrameStats combined_fs = new FrameStats();
        for (double t = inner_min_time; t <= inner_max_time; t += s.time_inc) {
            setTracksCurrentTimes(t);
            FrameStats fs = updateStats(false);
            if (null == fs) {
                continue;
            }
            if (inc_sut_radius_on_false_clear
                    && fs.false_clear_area >= Double.MIN_NORMAL) {
                t = Math.max(inner_min_time, t - s.time_inc);
                s.sut_radius_increase += s.sut_radius_inc;
                continue;
            }
            if (HTPM_JFrame.processDataTask != null) {
                HTPM_JFrame.processDataTask.setTime(t, inner_min_time, inner_max_time);
            }
            if (HTPM_JFrame.rocTask != null) {
                HTPM_JFrame.rocTask.setTime(t, inner_min_time, inner_max_time);
            }
            if (HTPM_JFrame.progressMonitor != null
                    && HTPM_JFrame.progressMonitor.isCanceled()) {
                return null;
            }
            if (fs != null) {
                try {
                    if (null != ps) {
                        String field_list_s = "" + t + ",";
                        for (Field f : FrameStats.class.getFields()) {
                            field_list_s += f.getDouble(fs) + ",";
                        }
                        ps.println(field_list_s);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                combined_fs.false_clear_area += fs.false_clear_area;
                combined_fs.false_occupied_area += fs.false_occupied_area;
                combined_fs.total_gt_occupied_area += fs.total_gt_occupied_area;
                combined_fs.total_sut_occupied_area += fs.total_sut_occupied_area;
                combined_fs.true_clear_area += fs.true_clear_area;
                combined_fs.true_occupied_area += fs.true_occupied_area;
                combined_fs.gt_human_count += fs.gt_human_count;
                combined_fs.sut_human_count += fs.sut_human_count;
                combined_fs.total_sut_to_gt_dist += fs.total_sut_to_gt_dist;
                combined_fs.total_gt_to_sut_dist += fs.total_gt_to_sut_dist;
                if (combined_fs.max_gt_to_sut_dist < fs.max_gt_to_sut_dist) {
                    combined_fs.max_gt_to_sut_dist = fs.max_gt_to_sut_dist;
                }
                if (combined_fs.max_sut_to_gt_dist < fs.max_sut_to_gt_dist) {
                    combined_fs.max_sut_to_gt_dist = fs.max_sut_to_gt_dist;
                }
            }
        }
        if (combined_fs.gt_human_count < 1) {
            combined_fs.gt_human_count = 1;
        }
        if (combined_fs.sut_human_count < 1) {
            combined_fs.sut_human_count = 1;
        }

        combined_fs.avg_gt_to_sut_dist =
                combined_fs.total_gt_to_sut_dist / combined_fs.gt_human_count;
        combined_fs.avg_sut_to_gt_dist =
                combined_fs.total_sut_to_gt_dist / combined_fs.sut_human_count;
        if (null != ps) {
            ps.close();
        }
        frameStatsCsvF = null;
        last_combined_fs = combined_fs;
        return combined_fs;
    }

    private void jSliderConfidenceStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderConfidenceStateChanged
        double slider_range =
                this.jSliderConfidence.getMaximum() - this.jSliderConfidence.getMinimum();
        if (slider_range < Double.MIN_NORMAL) {
            return;
        }
        double confidence = 0.99 * (this.jSliderConfidence.getValue() - this.jSliderConfidence.getMinimum())
                / (slider_range);
        this.drawPanel1.confidence_threshold = confidence;
        HTPM_JFrame.s.confidence_threshold = confidence;
        this.updateDrawPanelViewport();
    }//GEN-LAST:event_jSliderConfidenceStateChanged
    private String systemUnderTestFileName = null;

    private void jMenuItemOpenSutCsvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenSutCsvActionPerformed
        try {
            if (gtlist == null) {
                inner_min_time = Double.POSITIVE_INFINITY;
                inner_max_time = Double.NEGATIVE_INFINITY;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(s.sut_open_file_dir));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Comma Seperated Variable Files", "csv");
            chooser.setFileFilter(filter);
            chooser.setMultiSelectionEnabled(true);
            int returnVal = chooser.showOpenDialog(this);
            sutlist = new ArrayList<Track>();
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                CsvParseOptions o = null;
                for (File f : chooser.getSelectedFiles()) {
                    if (o == null) {
                        o = CsvParseOptionsJPanel.showDialog(this, f);
                        if (o == null) {
                            return;
                        }
                    }
                    systemUnderTestFileName = f.getCanonicalPath();
                    if (chooser.getSelectedFile().exists() && f.canRead()) {
                        s.sut_open_file_dir = f.getParentFile().getCanonicalPath();
                    }
                    System.out.println("You chose to open this file system under test file: "
                            + systemUnderTestFileName);
                    o.filename = systemUnderTestFileName;
                    File recentFilesDir = new File(System.getProperty("user.home"), ".htpm_recent_files");
                    File recentSUTFilesDir = new File(recentFilesDir, ".sut");
                    recentSUTFilesDir.mkdirs();
                    File infoFile = File.createTempFile(f.getName() + "_", "_info.txt", recentSUTFilesDir);
                    PrintStream ps = new PrintStream(new FileOutputStream(infoFile));
                    ps.println(o.toString());
                    ps.close();
                    this.jMenuRecentSystemUnderTestCsv.add(this.createRecentFileMenuItem(o, false));
                    List<Track> newList = LoadFile(systemUnderTestFileName,
                            Color.blue, false, o);
                    sutlist.addAll(newList);
                }
                updateEverything();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }//GEN-LAST:event_jMenuItemOpenSutCsvActionPerformed

    private void fixJtreeSize() {
        // This is a somewhat poor heuristic for getting the size of the
        // jtree correct as it is expanded and collapsed. 
        // Too small means the user won't be able to see  or interact with the whole
        // tree.
        // Too big could make the user do too much scrolling.
        // Make it somewhat too big since too big is better than too small.
        //System.out.println("row_count = " + this.jTree1.getRowCount());
        this.jTree1.setVisibleRowCount(this.jTree1.getRowCount());
        //System.out.println("jTree1.getPreferredSize() = " + jTree1.getPreferredSize());
        Dimension d = this.jTree1.getPreferredSize();
        int fh = 2 * (this.jTree1.getFontMetrics(this.jTree1.getFont()).getHeight() + 1);
        if (fh < 16) {
            fh = 16;
        }
        if (d.height < fh * this.jTree1.getRowCount()) {
            d.height = fh * this.jTree1.getRowCount();
            this.jTree1.setPreferredSize(d);
        } else {
            fh = fh * 2;
            if (d.height > fh * this.jTree1.getRowCount()) {
                d.height = fh * this.jTree1.getRowCount();
                this.jTree1.setPreferredSize(d);
            }
        }
        this.jTree1.revalidate();
    }

    private void jTree1TreeExpanded(javax.swing.event.TreeExpansionEvent evt) {//GEN-FIRST:event_jTree1TreeExpanded
        fixJtreeSize();
    }//GEN-LAST:event_jTree1TreeExpanded

    private void jTree1TreeCollapsed(javax.swing.event.TreeExpansionEvent evt) {//GEN-FIRST:event_jTree1TreeCollapsed
        fixJtreeSize();
    }//GEN-LAST:event_jTree1TreeCollapsed

    private void markTreeChildrenSelected(DefaultMutableTreeNode node) {
        Object o = node.getUserObject();
        if (Track.class.isInstance(o)) {
            Track t = (Track) o;
            t.selected = true;
            if (t.is_groundtruth) {
                t.color = Color.orange;
            } else {
                t.color = Color.magenta;
            }
        }
        for (int ci = 0; ci < node.getChildCount(); ci++) {
            DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) node.getChildAt(ci);
            markTreeChildrenSelected(child);
        }
    }
    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
        if (null != gtlist) {
            for (Track t : gtlist) {
                if (!t.selected) {
                    continue;
                }
                if (null != t.explicit_color) {
                    t.color = t.explicit_color;
                } else {
                    t.color = Color.RED;
                }
                t.selected = false;
            }
        }
        if (null != sutlist) {
            for (Track t : sutlist) {
                if (!t.selected) {
                    continue;
                }
                if (null != t.explicit_color) {
                    t.color = t.explicit_color;
                } else {
                    t.color = Color.BLUE;
                }
                t.selected = false;
            }
        }
        TreePath tpa[] = this.jTree1.getSelectionPaths();
        if (null == tpa) {
            this.combineTracks();
            this.drawPanel1.repaint();
            return;
        }
        for (TreePath tp : tpa) {
            if (tp.getPath().length < 3) {
                continue;
            }
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) tp.getLastPathComponent();
            markTreeChildrenSelected(node);
        }
        this.combineTracks();
        this.drawPanel1.repaint();
        this.jTree1.repaint();
    }//GEN-LAST:event_jTree1ValueChanged

    private void jCheckBoxMenuItemPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPlayActionPerformed
        make_movie = false;
        if (this.jCheckBoxMenuItemPlay.isSelected()) {
            String s = JOptionPane.showInputDialog("Frames Per Second?",
                    this.drawPanel1.getMovie_frames_per_second());
            if (null != s) {
                int fps = Integer.valueOf(s);
                this.drawPanel1.setMovie_frames_per_second(fps);
            }
            this.startPlayAll();
        } else {
            this.stopPlayAll();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPlayActionPerformed
    private boolean make_movie = false;

    private void jTextFieldGridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldGridActionPerformed
        String grid_s = this.jTextFieldGrid.getText();
        double grid = Double.valueOf(grid_s);
        this.drawPanel1.setGrid(grid);
        s.grid = grid;
    }//GEN-LAST:event_jTextFieldGridActionPerformed

    private void jTextFieldGTRadiusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldGTRadiusActionPerformed
        String radius_s = this.jTextFieldGTRadius.getText();
        double radius = Double.valueOf(radius_s);
        s.gt_radius_increase = radius;
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jTextFieldGTRadiusActionPerformed

    private void currentValsToSettings() {
        if (null == s) {
            s = new settings();
        }
        s.grid = this.drawPanel1.getGrid();
        s.track_tail_highlight_time = this.drawPanel1.getTrack_tail_highlight_time();
        s.transform_dir = TransformMatrixJPanel.transform_dir;
        s.show_background_image = this.drawPanel1.show_background_image;
        s.background_image_x = this.drawPanel1.background_image_x;
        s.background_image_y = this.drawPanel1.background_image_y;
        s.background_image_scale_pixels_per_m = this.drawPanel1.background_image_scale_pixels_per_m;
        s.x_max = this.drawPanel1.x_max;
        s.x_min = this.drawPanel1.x_min;
        s.y_max = this.drawPanel1.y_max;
        s.y_min = this.drawPanel1.y_min;
        s.roi_x_min = this.drawPanel1.ROI[0];
        s.roi_y_min = this.drawPanel1.ROI[1];
        s.roi_x_max = this.drawPanel1.ROI[2];
        s.roi_y_max = this.drawPanel1.ROI[3];
        s.view_point_x = this.jScrollPaneDrawPanel.getViewport().getViewPosition().x;
        s.view_point_y = this.jScrollPaneDrawPanel.getViewport().getViewPosition().y;
        Dimension d = this.drawPanel1.getPreferredSize();
        s.panel_pref_height = d.height;
        s.panel_pref_width = d.width;
        s.zoom_v = this.jSliderZoom.getValue();
        s.gt_on_top = this.jCheckBoxMenuItemGtOnTop.isSelected();
        s.use_gray_tracks = this.jCheckBoxMenuItemGrayTracks.isSelected();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        this.currentValsToSettings();
        saveSettings(s, settings_file);
    }//GEN-LAST:event_formWindowClosing

    private void jCheckBoxMenuItemPlayAndMakeMovieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPlayAndMakeMovieActionPerformed
        if (this.jCheckBoxMenuItemPlayAndMakeMovie.isSelected()) {
            this.make_movie = false;
            String max_frame_s = JOptionPane.showInputDialog(this,
                    "Maximum number of frames to put on one file before splitting files?",
                    DrawPanel.max_frame_count);
            if (max_frame_s == null || max_frame_s.length() < 1) {
                this.jCheckBoxMenuItemPlayAndMakeMovie.setSelected(false);
                return;
            }
            DrawPanel.max_frame_count = Integer.valueOf(max_frame_s);
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(s.save_file_dir));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Movie Files", "avi", "mov");
            chooser.setFileFilter(filter);
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File f = chooser.getSelectedFile();
                    s.save_file_dir = f.getParentFile().getCanonicalPath();
                    this.drawPanel1.movie_filename = f.getCanonicalPath();
                    this.make_movie = true;
                } catch (IOException ex) {
                    Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            } else {
                this.jCheckBoxMenuItemPlayAndMakeMovie.setSelected(false);
                return;
            }
            String s = JOptionPane.showInputDialog("Frames Per Second?",
                    this.drawPanel1.getMovie_frames_per_second());
            if (null != s) {
                int fps = Integer.valueOf(s);
                this.drawPanel1.setMovie_frames_per_second(fps);
            }
            this.startPlayAll();
        } else {
            this.stopPlayAll();
            this.make_movie = false;
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPlayAndMakeMovieActionPerformed
    public OptitrackUDPStream ods = null;
    private List<Track> optitrack_tracks = null;
    static final Point2D zero2d = new Point2D.Float(0f, 0f);

    /**
     * Update tracks and displays using the current position of one rigid body
     * as reported by the optitrack.
     *
     * @param rb Optitrack rigid body data
     * @return whether displays and/or logs need to be updated.
     */
    public boolean UpdateOptitrackRigidBody(OptitrackUDPStream.RigidBody rb,
            PrintStream ps) throws Exception {
        boolean point_updated = false;
        Point3D pt = rb.pos;

        if (zero2d.distance(pt) < 0.001) {
            return false;
        }
        if (null == this.drawPanel1.tracks) {
            this.drawPanel1.tracks = new ArrayList<Track>();
        }
        if (null == optitrack_tracks) {
            optitrack_tracks = new LinkedList<Track>();
        }
        Track optitrack_track = null;
        String rb_name = Integer.toString(rb.ID);
        for (Track t : optitrack_tracks) {
            if (t.name.compareTo(rb_name) == 0) {
                optitrack_track = t;
                break;
            }
        }
        if (null == optitrack_track) {
            optitrack_track = new Track();
            optitrack_track.source = "optitrack";
            optitrack_track.name = Integer.toString(rb.ID);
            optitrack_track.is_groundtruth = this.optitrack_is_ground_truth;
            optitrack_tracks.add(optitrack_track);
            if (this.optitrack_is_ground_truth) {
                if (null == gtlist) {
                    gtlist = new ArrayList<Track>();
                }
                gtlist.add(optitrack_track);
            } else {
                if (null == sutlist) {
                    sutlist = new ArrayList<Track>();
                }
                sutlist.add(optitrack_track);
            }
            //this.drawPanel1.tracks.add(optitrack_track);
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateEverything();
                    //System.out.println("new optitrack track");
                }
            });
        }
        if (optitrack_track.currentPoint != null
                && optitrack_track.currentPoint.distance(pt) < 0.001) {
            return false;
        }
        TrackPoint tp = new TrackPoint(pt);
//        System.out.println("pre transfrom tp = " + tp);
        if (ods.apply_transform) {
            tp.applyTransform(ods.transform);
            optitrack_track.setTransform(ods.transform);
        }
//        System.out.println("post transform tp = " + tp);
        tp.time = System.currentTimeMillis() * 1e-3;
        tp.confidence = 1.0;
        optitrack_track.selected = true;

        optitrack_track.currentPoint = tp;
        optitrack_track.color = Color.RED;
        if (null == optitrack_track.data) {
            optitrack_track.data = new ArrayList<TrackPoint>();
        }
        optitrack_track.data.add(tp);
        if (null != ps) {
            this.printOneLine(tp, optitrack_track.name, ps);
        }
        if (optitrack_track.data.size() > 5000) {
            optitrack_track.data.remove(0);
        }
        optitrack_track.cur_time_index = optitrack_track.data.size() - 1;
        //setCurrentTime(tp.time + 0.00001);
        return true;
    }
    private Track optitrack_unaffiliated_track = null;

    /**
     * Update all the tracks and displays using all the latest data from
     * optitrack.
     */
    public void UpdateOptitrackData() {
        if (null == ods
                || null == ods.last_frame_recieved) {
            return;
        }
        try {
            double time = System.currentTimeMillis() * 1e-3;
            if (null != ods.last_frame_recieved.other_markers_array
                    && ods.last_frame_recieved.other_markers_array.length > 0) {
                if (null == this.optitrack_unaffiliated_track) {
                    this.optitrack_unaffiliated_track = new Track();
                    this.optitrack_unaffiliated_track.name = "optitrack_unaffiliated_track";
                    this.optitrack_unaffiliated_track.source = "optitrack";
                    this.optitrack_unaffiliated_track.disconnected = true;
                    if (null == optitrack_tracks) {
                        optitrack_tracks = new LinkedList<Track>();
                    }
                    optitrack_tracks.add(optitrack_unaffiliated_track);
                    if (this.optitrack_is_ground_truth) {
                        if (null == gtlist) {
                            gtlist = new LinkedList<Track>();
                        }
                        gtlist.add(optitrack_unaffiliated_track);
                    } else {
                        if (null == sutlist) {
                            sutlist = new LinkedList<Track>();
                        }
                        sutlist.add(optitrack_unaffiliated_track);
                    }
                }
                for (Point3D p3d : ods.last_frame_recieved.other_markers_array) {
                    TrackPoint tp = new TrackPoint(p3d);
                    tp.time = time;
                    if (ods.apply_transform) {
                        tp.applyTransform(ods.transform);
                        optitrack_unaffiliated_track.setTransform(ods.transform);
                    }
                    if (null == optitrack_unaffiliated_track.data) {
                        optitrack_unaffiliated_track.data = new ArrayList<TrackPoint>();
                        optitrack_unaffiliated_track.disconnected = true;
                    }
                    optitrack_unaffiliated_track.data.add(tp);
                    if (null != this.optitrack_print_stream) {
                        this.printOneLine(tp, optitrack_unaffiliated_track.name, optitrack_print_stream);
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
        if (null == ods
                || null == ods.last_frame_recieved
                || null == ods.last_frame_recieved.rigid_body_array) {
            return;
        }
        boolean point_updated = false;
        for (OptitrackUDPStream.RigidBody rb : ods.last_frame_recieved.rigid_body_array) {
            try {
                point_updated = point_updated
                        || this.UpdateOptitrackRigidBody(rb,
                        this.optitrack_print_stream);
            } catch (Exception ex) {
                ods.close();
                ods = null;
                this.jCheckBoxMenuItemOptitrack.setSelected(false);
                this.stopRecording();
                Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
                myShowMessageDialog(this,
                        "Failure encountered updating or recording optitrack data.");
            }
        }
        if (point_updated) {
            drawPanel1.repaint();
        }
    }
    public boolean optitrack_is_ground_truth = true;

    /**
     * Ask a yes/no question and return whether the user said yes.
     *
     * @param message string to prompt user
     * @param _default choose yes by default
     * @return
     */
    public boolean AskBoolean(String message, boolean _default) {
        return JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
                this,
                message,
                "",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null,
                _default ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION);
    }
    private PrintStream optitrack_print_stream = null;

    public String dateString() {
        GregorianCalendar c = new GregorianCalendar();
        return "" + c.get(Calendar.YEAR) + "-"
                + c.get(Calendar.MONTH) + "-"
                + c.get(Calendar.DAY_OF_MONTH)
                + "_" + String.format("%02d", c.get(Calendar.HOUR_OF_DAY))
                + "_" + String.format("%02d", c.get(Calendar.MINUTE))
                + "_" + String.format("%02d", c.get(Calendar.SECOND))
                + "." + String.format("%03d", c.get(Calendar.MILLISECOND));
    }

    public void startRecording() {

        try {
            stopRecording();
            JFileChooser chooser = new JFileChooser();
            File dir = new File(s.save_file_dir);
            System.out.println("dir = " + dir);
            chooser.setCurrentDirectory(dir);
            File f = null;
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Comma Seperated Variable Files", "csv");
            chooser.setFileFilter(filter);
            try {
                String name_base_s = "optitrac_" + dateString() + "_";
                System.out.println("name_base_s = " + name_base_s);
                f = File.createTempFile(name_base_s, ".csv",
                        dir);
                chooser.setSelectedFile(f);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                f = chooser.getSelectedFile();
                s.save_file_dir = f.getParentFile().getCanonicalPath();
                this.optitrack_print_stream = new PrintStream(new FileOutputStream(f));
                printCsvHeader(this.optitrack_print_stream);
                this.jCheckBoxMenuItemRecordLive.setSelected(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        this.jCheckBoxMenuItemRecordLive.setSelected(false);
        if (null != optitrack_print_stream) {
            optitrack_print_stream.close();
            optitrack_print_stream = null;
        }
    }

    private void jCheckBoxMenuItemOptitrackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemOptitrackActionPerformed
        if (ods != null) {
            ods.close();
            ods = null;
            this.jCheckBoxMenuItemRecordLive.setEnabled(false);
            stopRecording();
        }
        if (this.jCheckBoxMenuItemOptitrack.isSelected()) {
            final String server = JOptionPane.showInputDialog(this, "Optitrack IP Address",
                    s.optitrack_host);
            optitrack_is_ground_truth =
                    AskBoolean("Use Optitrack as Ground Truth?",
                    optitrack_is_ground_truth);
            this.jCheckBoxMenuItemRecordLive.setEnabled(true);
            System.out.println("optitrack_is_ground_truth = "
                    + optitrack_is_ground_truth);
            if (null != server) {
                ods = new OptitrackUDPStream(server);
                ods.transform_filename = s.optitrack_trasform_filename;
                TransformMatrixJPanel.showDialog(this, ods);
                s.optitrack_trasform_filename = ods.transform_filename;
                if (!ods.try_ping(1, 1000)) {
                    int o = JOptionPane.showConfirmDialog(this,
                            "No response to ping from optitrack. Continue?");
                    if (o != JOptionPane.YES_OPTION) {
                        ods.close();
                        ods = null;
                        return;
                    }
                }
                s.optitrack_host = server;
                boolean start_recording = AskBoolean("Start recording live data?",
                        false);
                if (start_recording) {
                    startRecording();
                }
                ods.addListener(new ActionListener() {
                    int action_count = 0;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (action_count < 2) {
                            s.optitrack_host = server;
                            s.optitrack_trasform_filename = ods.transform_filename;
                        }
                        action_count++;
                        UpdateOptitrackData();
                    }
                });
            }
        } else {
            boolean clear_old_data =
                    AskBoolean("Clear old data?", false);
            if (clear_old_data) {
                this.ClearData();
            }
        }
    }//GEN-LAST:event_jCheckBoxMenuItemOptitrackActionPerformed

    /**
     * Print one line of a csv file using one track point.
     *
     * @param tp point to save
     * @param name ID of person/trackable to associate track point to.
     * @param ps print stream of open csv file.
     * @throws Exception
     */
    public static void printOneLine(TrackPoint tp, String name, PrintStream ps) throws Exception {
        //<timestamp>, <person ID>, <person centroid X>, <person centroid Y>, <person centroid Z>,<bounding box top center X>, <bounding box top center Y>,  <bounding box top center Z>, <X velocity>, <Y velocity>, <Z velocity>, <ROI width>, <ROI height>,confidence
        // ps.println("timestamp,personID,personcentroidX,personcentroidY,personcentroidZ,boundingboxtopcenterX,boundingboxtopcenterY,boundingboxtopcenterZ,Xvelocity,Yvelocity,Zvelocity,ROIwidth,ROIheight,confidence,radius");

        ps.println(tp.time + ","
                + name + ","
                + tp.x + ","
                + tp.y + ","
                + tp.z + ","
                + tp.x + ","
                + tp.y + ","
                + tp.z + ","
                + tp.vel_x + ","
                + tp.vel_y + ","
                + tp.vel_z + ","
                + tp.ROI_width + ","
                + tp.ROI_height + ","
                + tp.confidence + ","
                + tp.radius);
    }
    static boolean check_times = true;

    /**
     * Parse one line received from a socket or csv file and create a
     * corresponding TrackPoint.
     *
     * @param line String received from socket or csv.
     * @param o options used for conversion including field indexes and scale
     * factors.
     * @return
     * @throws Exception
     */
    public static TrackPoint parseTrackPointLine(String line,
            final CsvParseOptions o) throws Exception {
        String fields[] = line.split(o.delim);
        int fields_needed = Math.max(o.TIME_INDEX, Math.max(o.X_INDEX, o.Y_INDEX)) + 2;
        if (fields.length < fields_needed) {
            throw new IllegalArgumentException("parseTrackPointLine() : Not enough fields(" + fields.length + "<" + fields_needed + ") in :" + line);
        }
        double time = Double.valueOf(fields[o.TIME_INDEX]) * o.TIME_SCALE + o.TIME_OFFSET;
        if (time < 1325376000.0 || time > 2587680000.0 && check_times) {
            int confirm = JOptionPane.showConfirmDialog(null, "Time = " + time + " (" + new Date((long) (time * 1e3)) + ") seems improbable. Continue?");
            System.out.println("line = " + line);
            System.out.println("fields[TIME_INDEX] = " + fields[o.TIME_INDEX]);
            if (confirm != JOptionPane.YES_OPTION) {
                throw new Exception("Bad time value");
            }
            check_times = false;
        }
        String name = "";
        if (fields.length > o.NAME_INDEX && o.NAME_INDEX >= 0) {
            name = fields[o.NAME_INDEX];
        }
        float x = (float) (Double.valueOf(fields[o.X_INDEX]) * o.DISTANCE_SCALE);
        float y = (float) (Double.valueOf(fields[o.Y_INDEX]) * o.DISTANCE_SCALE);
        float z = 0f;
        float vel_x = 0f;
        float vel_y = 0f;
        float vel_z = 0f;
        if (fields.length > o.Z_INDEX && o.Z_INDEX >= 0) {
            z = (float) (Double.valueOf(fields[o.Z_INDEX]) * o.DISTANCE_SCALE);
        }
        if (fields.length > o.VX_INDEX && o.VX_INDEX >= 0) {
            vel_x = (float) (Double.valueOf(fields[o.VX_INDEX]) * o.DISTANCE_SCALE);
        }
        if (fields.length > o.VY_INDEX && o.VY_INDEX >= 0) {
            vel_y = (float) (Double.valueOf(fields[o.VY_INDEX]) * o.DISTANCE_SCALE);
        }
        if (fields.length > o.VZ_INDEX && o.VZ_INDEX >= 0) {
            vel_z = (float) (Double.valueOf(fields[o.VX_INDEX]) * o.DISTANCE_SCALE);
        }
        TrackPoint tp = new TrackPoint(x, y, z, vel_x, vel_y, vel_z);
        tp.time = time;
        tp.name = name;
        if (fields.length > o.ROI_WIDTH_INDEX && o.ROI_WIDTH_INDEX >= 0) {
            tp.ROI_width = Double.valueOf(fields[o.ROI_WIDTH_INDEX]);
        }
        if (fields.length > o.ROI_HEIGHT_INDEX && o.ROI_HEIGHT_INDEX >= 0) {
            tp.ROI_height = Double.valueOf(fields[o.ROI_HEIGHT_INDEX]);
        }
        tp.confidence = 1.0;
        if (fields.length > o.CONFIDENCE_INDEX && o.CONFIDENCE_INDEX >= 0) {
            tp.confidence = Double.valueOf(fields[o.CONFIDENCE_INDEX]);
        }
        if (fields.length > o.RADIUS_INDEX && o.RADIUS_INDEX >= 0) {
            tp.radius = Double.valueOf(fields[o.RADIUS_INDEX]) * o.DISTANCE_SCALE;
        }
        if (null != o.transform) {
            tp.applyTransform(o.transform);
        }
        return tp;
    }

    public static void printCsvHeader(PrintStream ps) {
        ps.println("timestamp,personID,personcentroidX,personcentroidY,personcentroidZ,boundingboxtopcenterX,boundingboxtopcenterY,boundingboxtopcenterZ,Xvelocity,Yvelocity,Zvelocity,ROIwidth,ROIheight,confidence,radius");
    }

    /**
     * Save a list of tracks to a file.
     *
     * @param tracks list of tracks
     * @param f csv file to save tracks to
     */
    public void saveFile(List<Track> tracks, File f) {
        try {
            if (null == tracks || tracks.size() < 1) {
                myShowMessageDialog(this, "No data to save");
                return;
            }
            PrintStream ps = new PrintStream(new FileOutputStream(f));
            for (Track t : tracks) {
                for (TrackPoint tp : t.data) {
                    printOneLine(tp, t.name, ps);
                }
            }
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Ask user to select a file and save the tracks to that file.
     *
     * @param tracks list of tracks
     */
    public void selectAndSaveCsv(List<Track> tracks) {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(s.save_file_dir));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Comma Seperated Variable Files", "csv");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                s.save_file_dir = chooser.getSelectedFile().getParentFile().getCanonicalPath();
                saveFile(tracks, chooser.getSelectedFile());
            }
        } catch (Exception ex) {
            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void jMenuItemSaveGroundTruthActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveGroundTruthActionPerformed
        this.selectAndSaveCsv(gtlist);
    }//GEN-LAST:event_jMenuItemSaveGroundTruthActionPerformed

    private void jMenuItemSaveSutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveSutActionPerformed
        this.selectAndSaveCsv(sutlist);
    }//GEN-LAST:event_jMenuItemSaveSutActionPerformed

    private void jMenuItemSaveImagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveImagesActionPerformed
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(s.save_file_dir));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "JPEG Files", "jpg");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                BufferedImage bi = this.drawPanel1.getImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
                s.save_file_dir = chooser.getSelectedFile().getParentFile().getCanonicalPath();
                File f1 = chooser.getSelectedFile();
                ImageIO.write(bi, "jpg", f1);
                System.out.println("wrote to " + f1);
            }
            updateStats(true);
        } catch (Exception ex) {
            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemSaveImagesActionPerformed

    private void jCheckBoxMenuItemShowLabelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemShowLabelsActionPerformed
        this.drawPanel1.show_labels = this.jCheckBoxMenuItemShowLabels.isSelected();
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jCheckBoxMenuItemShowLabelsActionPerformed

    private void jCheckBoxMenuItemShowOnlySelectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemShowOnlySelectedActionPerformed
        this.drawPanel1.show_only_selected = this.jCheckBoxMenuItemShowOnlySelected.isSelected();
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jCheckBoxMenuItemShowOnlySelectedActionPerformed
    private plotterJFrame plotter_frame = null;

    private void jMenuItemShowStatVTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowStatVTimeActionPerformed
        try {
            if (!this.updateROI()) {
                return;
            }
            HTPM_JFrame.frameStatsCsvF =
                    File.createTempFile("htpm_stats_", ".csv");
            final File f = HTPM_JFrame.frameStatsCsvF;
            String time_inc_s = JOptionPane.showInputDialog(this,
                    "Time Increment?",
                    HTPM_JFrame.s.time_inc);
            HTPM_JFrame.s.time_inc = Double.valueOf(time_inc_s);
            String scale_s = JOptionPane.showInputDialog(this,
                    "Occupancy cells per meter?",
                    String.format("%.0f", HTPM_JFrame.s.scale));
            HTPM_JFrame.s.scale = Double.valueOf(scale_s);
            this.startProcessTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        plotter_frame =
                                new plotterJFrame();
                        plotterJFrame.lock_value_for_plot_versus_line_number(false,
                                true);
                        plotterJFrame.setForcedLineFilterPattern("",
                                true);
                        plotterJFrame.setFieldSeparator(",");
                        plotter_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        plotter_frame.LoadFile(f.getCanonicalPath());
                        plotter_frame.setVisible(true);
                        PlayBeep();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }//GEN-LAST:event_jMenuItemShowStatVTimeActionPerformed

    private void jMenuItemShowROCCurveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowROCCurveActionPerformed
        String time_inc_s = JOptionPane.showInputDialog(this, "Time Increment?",
                HTPM_JFrame.s.time_inc);
        HTPM_JFrame.s.time_inc = Double.valueOf(time_inc_s);
        String scale_s = JOptionPane.showInputDialog(this, "Occupancy cells per meter?",
                String.format("%.0f", HTPM_JFrame.s.scale));
        HTPM_JFrame.s.scale = Double.valueOf(scale_s);
        this.startRocTask(new Runnable() {
            @Override
            public void run() {
                try {
                    RocData rd = HTPM_JFrame.last_roc_data;
                    if (null != rd) {
                        plotterJFrame plotter_frame =
                                new plotterJFrame();
                        plotter_frame.LoadXYFloatArrays("ROC",
                                rd.neg_ratio,
                                rd.pos_ratio);
                        float zero_one[] = {0f, 1f};
                        plotter_frame.LoadXYFloatArrays("FP=TP line",
                                zero_one,
                                zero_one);
                        plotter_frame.setVisible(true);
                        plotter_frame.FitToGraph();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }//GEN-LAST:event_jMenuItemShowROCCurveActionPerformed

    private void jMenuItemShowVelocitiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowVelocitiesActionPerformed
        List<Track> tracks = this.drawPanel1.tracks;
        if (null != tracks) {
            plotterJFrame plotter_frame =
                    new plotterJFrame();
            plotter_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            for (Track t : tracks) {
                PlotData pd = new PlotData();
                if (false) {
                    pd.name = t.toString() + "_vel_mag";
                    plotter_frame.AddPlot(pd);
                    for (TrackPoint tp : t.data) {
                        plotter_frame.AddPointToPlot(pd, tp.time,
                                Math.sqrt(tp.vel_x * tp.vel_x + tp.vel_y * tp.vel_y),
                                true);
                    }
//                    pd = new PlotData();
//                    pd.name = t.toString() + "_vel_x";
//                    plotter_frame.AddPlot(pd);
//                    for (TrackPoint tp : t.data) {
//                        plotter_frame.AddPointToPlot(pd, tp.time,
//                                tp.vel_x,
//                                true);
//                    }
//                    pd = new PlotData();
//                    pd.name = t.toString() + "_vel_y";
//                    plotter_frame.AddPlot(pd);
//                    for (TrackPoint tp : t.data) {
//                        plotter_frame.AddPointToPlot(pd, tp.time,
//                                tp.vel_y,
//                                true);
//                    }
//                    pd = new PlotData();
//                    pd.name = t.toString() + "_vel_z";
//                    plotter_frame.AddPlot(pd);
//                    for (TrackPoint tp : t.data) {
//                        plotter_frame.AddPointToPlot(pd, tp.time,
//                                tp.vel_z,
//                                true);
//                }
                }
                //else {
                t.compute_vel_from_positions();
                pd.name = t.toString() + "_vel_computed_from_pos_mag";
                plotter_frame.AddPlot(pd);
                for (TrackPoint tp : t.data) {
                    plotter_frame.AddPointToPlot(pd, tp.time,
                            Math.sqrt(tp.computed_vel_x * tp.computed_vel_x + tp.computed_vel_y * tp.computed_vel_y),
                            true);
                }
                //}
            }
            plotter_frame.setVisible(true);
            plotter_frame.setTitle("Velocities");
            plotter_frame.FitToGraph();
        }
    }//GEN-LAST:event_jMenuItemShowVelocitiesActionPerformed

    private void jMenuItemSetSelectedTracksColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetSelectedTracksColorActionPerformed
        List<Track> tracks = this.drawPanel1.tracks;
        if (null != tracks) {
            Color c = ChooseColor("Please select color for selected tracks", Color.RED);
            for (Track t : tracks) {
                if (t.selected) {
                    t.color = c;
                    t.explicit_color = c;
                }
            }
            this.updateTree(false);
            this.drawPanel1.repaint();
        }
    }//GEN-LAST:event_jMenuItemSetSelectedTracksColorActionPerformed

    private void jMenuItemDefaultColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDefaultColorsActionPerformed
        List<Track> tracks = this.drawPanel1.tracks;
        if (null != tracks) {
            for (Track t : tracks) {
                if (t.is_groundtruth) {
                    t.color = Color.RED;
                } else {
                    t.color = Color.BLUE;
                }
            }
        }
        this.updateTree(false);
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jMenuItemDefaultColorsActionPerformed

    private void jMenuItemRandomColorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRandomColorsActionPerformed
        List<Track> tracks = this.drawPanel1.tracks;
        if (null != tracks) {
            Random r = new Random();
            for (Track t : tracks) {
                Color c = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
                t.color = c;
            }
        }
        this.updateTree(false);
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jMenuItemRandomColorsActionPerformed

    private void jCheckBoxMenuItemDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemDebugActionPerformed
        OptitrackUDPStream.debug = this.jCheckBoxMenuItemDebug.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemDebugActionPerformed

    private void jMenuItemFitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemFitActionPerformed
        Fit();
    }//GEN-LAST:event_jMenuItemFitActionPerformed

    private void UpdateDisplayMode() {
        if (this.jRadioButtonMenuItemModeNormal.isSelected()) {
            this.drawPanel1.show_false_occupied_area = false;
            this.drawPanel1.show_false_clear_area = false;
            this.drawPanel1.show_GT_occupied_area = false;
            this.drawPanel1.show_SUT_occupied_area = false;
        } else if (this.jRadioButtonMenuItemModeFalseOccupied.isSelected()) {
            this.drawPanel1.show_false_occupied_area = true;
            this.drawPanel1.show_false_clear_area = false;
            this.drawPanel1.show_GT_occupied_area = false;
            this.drawPanel1.show_SUT_occupied_area = false;
        } else if (this.jRadioButtonMenuItemModeFalseClear.isSelected()) {
            this.drawPanel1.show_false_occupied_area = false;
            this.drawPanel1.show_false_clear_area = true;
            this.drawPanel1.show_GT_occupied_area = false;
            this.drawPanel1.show_SUT_occupied_area = false;
        } else if (this.jRadioButtonMenuItemModeGTOccupied.isSelected()) {
            this.drawPanel1.show_false_occupied_area = false;
            this.drawPanel1.show_false_clear_area = false;
            this.drawPanel1.show_GT_occupied_area = true;
            this.drawPanel1.show_SUT_occupied_area = false;
        } else if (this.jRadioButtonMenuItemModeSUTOccupied.isSelected()) {
            this.drawPanel1.show_false_occupied_area = false;
            this.drawPanel1.show_false_clear_area = false;
            this.drawPanel1.show_GT_occupied_area = false;
            this.drawPanel1.show_SUT_occupied_area = true;
        }
        this.drawPanel1.repaint();
    }

    private void jRadioButtonMenuItemModeFalseClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemModeFalseClearActionPerformed
        this.UpdateDisplayMode();
    }//GEN-LAST:event_jRadioButtonMenuItemModeFalseClearActionPerformed

    private void jRadioButtonMenuItemModeNormalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemModeNormalActionPerformed
        this.UpdateDisplayMode();
    }//GEN-LAST:event_jRadioButtonMenuItemModeNormalActionPerformed

    private void jRadioButtonMenuItemModeFalseOccupiedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemModeFalseOccupiedActionPerformed
        this.UpdateDisplayMode();
    }//GEN-LAST:event_jRadioButtonMenuItemModeFalseOccupiedActionPerformed

    private void jTextFieldSUTRadiusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSUTRadiusActionPerformed
        String radius_s = this.jTextFieldSUTRadius.getText();
        double radius = Double.valueOf(radius_s);
        s.sut_radius_increase = radius;
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jTextFieldSUTRadiusActionPerformed

    public void CopyResourceToDir(String resource_name, File dir) {
        try {
            dir.mkdirs();
            InputStream is = getClass().getResourceAsStream(resource_name);
            String resource_tail = resource_name;
            int li = resource_tail.lastIndexOf("/");
            if (li > 0) {
                resource_tail = resource_tail.substring(li + 1);
            }
            File f = new File(dir, resource_tail);
            FileOutputStream fos = new FileOutputStream(f);
            while (is.available() > 0) {
                byte ba[] = new byte[Math.max(4096, is.available())];
                is.read(ba);
                fos.write(ba);
            }
            is.close();
            fos.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    private void jMenuItemHelpOverviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHelpOverviewActionPerformed
        try {
            Desktop desktop = Desktop.getDesktop();
            File help_dir = new File(System.getProperty("user.home"), "htpm_help");
            help_dir.mkdirs();
//            Enumeration<URL> en = this.getClass().getClassLoader().getResources("htpm_resources");
//            System.out.println("en = " + en);
//            while (en.hasMoreElements()) {
//                URL url = en.nextElement();
//                //System.out.println("url = " + url);
//                URI uri = url.toURI();
//                //System.out.println("uri = " + uri);
//                String proto = url.getProtocol();
//                //System.out.println("proto = " + proto);
//                if (proto.compareTo("file") == 0) {
//                    File f = new File(uri);
//                    String sa[] = f.list();
//                    for (String s : sa) {
//                        //System.out.println("s = " + s);
//                        CopyResourceToDir("/htpm_resources/" + s, help_dir);
//                    }
//                } else if (proto.compareTo("jar") == 0) {
//                    JarURLConnection juc = (JarURLConnection) url.openConnection();
//                    JarFile jf = juc.getJarFile();
//                    Enumeration<JarEntry> enje = jf.entries();
//                    while(enje.hasMoreElements()) {
//                        JarEntry je = enje.nextElement();
//                        //System.out.println("je = " + je);
//                        String s = je.getName();
//                        if(je.isDirectory()) {
//                            continue;
//                        }
//                        System.out.println("s = " + s);
//                        if(s.startsWith("htpm_resources/")) {
//                            CopyResourceToDir("/"+s,help_dir);
//                        }
//                    }
//                }
//            }
            CopyResourceToDir("/htpm_resources/htpm_helpmain.html", help_dir);
            CopyResourceToDir("/htpm_resources/Screenshot_HumanTrackingPerformanceAnalysis.png", help_dir);
            File help_file_main = new File(help_dir, "htpm_helpmain.html");
            desktop.browse(help_file_main.toURI());
        } catch (Exception ex) {
            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemHelpOverviewActionPerformed
    private int bg_scale_from_x = -1;
    private int bg_scale_from_y = -1;
    private int mouse_pressed_x = -1;
    private int mouse_pressed_y = -1;

    private boolean checkIsLive() {
        if (!this.jCheckBoxLive.isSelected()) {
            return false;
        }
        if (this.jCheckBoxMenuItemAcceptGT.isSelected()
                || this.jCheckBoxMenuItemAcceptSutData.isSelected()
                || this.jCheckBoxMenuItemOptitrack.isSelected()) {
            return true;
        }
        return false;
    }

    private void jCheckBoxMenuItemAcceptGTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemAcceptGTActionPerformed
        if (checkIsLive()) {
            this.jSliderTime.setEnabled(false);
            this.jSliderTime.setValue(this.jSliderTime.getMaximum());
        } else {
            this.jSliderTime.setEnabled(true);
        }
        if (jCheckBoxMenuItemAcceptGT.isSelected()) {
            String port = JOptionPane.showInputDialog(this,
                    "TCP port for Ground-Truth data? ", s.gt_port);
            if (port == null) {
                return;
            }
            s.gt_port = Short.valueOf(port);
            OpenGTThread();
            this.updateMenuLabels();
        } else {
            CloseGTThread();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemAcceptGTActionPerformed

    private void jCheckBoxMenuItemAcceptSutDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemAcceptSutDataActionPerformed
        if (checkIsLive()) {
            this.jSliderTime.setEnabled(false);
            this.jSliderTime.setValue(this.jSliderTime.getMaximum());
        }
        if (jCheckBoxMenuItemAcceptSutData.isSelected()) {
            String port = JOptionPane.showInputDialog(this,
                    "TCP port for Ground-Truth data? ", s.sut_port);
            s.sut_port = Short.valueOf(port);
            OpenSUTThread();
            this.updateMenuLabels();
        } else {
            CloseSUTThread();
        }
    }//GEN-LAST:event_jCheckBoxMenuItemAcceptSutDataActionPerformed

    private void jCheckBoxMenuItemGtOnTopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemGtOnTopActionPerformed
        s.gt_on_top = this.jCheckBoxMenuItemGtOnTop.isSelected();
        this.combineTracks();
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jCheckBoxMenuItemGtOnTopActionPerformed

    private void jMenuItemShowAllTracksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowAllTracksActionPerformed
        for (Track t : this.drawPanel1.tracks) {
            t.hidden = false;
        }
        this.updateTree(false);
        this.drawPanel1.repaint();
        this.jTree1.repaint();
    }//GEN-LAST:event_jMenuItemShowAllTracksActionPerformed

    private void jMenuItemHideAllTracksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHideAllTracksActionPerformed
        for (Track t : this.drawPanel1.tracks) {
            t.hidden = true;
        }
        this.updateTree(false);
        this.drawPanel1.repaint();
        this.jTree1.repaint();
    }//GEN-LAST:event_jMenuItemHideAllTracksActionPerformed

    private void jMenuItemShowSelectedTracksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowSelectedTracksActionPerformed
        for (Track t : this.drawPanel1.tracks) {
            if (t.selected) {
                t.hidden = false;
            }
        }
        this.updateTree(false);
        this.drawPanel1.repaint();
        this.jTree1.repaint();
    }//GEN-LAST:event_jMenuItemShowSelectedTracksActionPerformed

    private void jMenuItemHideSelectedTracksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHideSelectedTracksActionPerformed
        for (Track t : this.drawPanel1.tracks) {
            if (t.selected) {
                t.hidden = true;
            }
        }
        this.updateTree(false);
        this.drawPanel1.repaint();
        this.jTree1.repaint();
    }//GEN-LAST:event_jMenuItemHideSelectedTracksActionPerformed

    private void jMenuItemGotoTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGotoTimeActionPerformed
        String time_s = JOptionPane.showInputDialog(this, "Time ?", this.CurrentTime);
        if (null != time_s) {
            this.setCurrentTime(Double.valueOf(time_s));
        }
    }//GEN-LAST:event_jMenuItemGotoTimeActionPerformed

    private void jMenuItemGotoPlotterMinTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGotoPlotterMinTimeActionPerformed
        if (null != plotter_frame) {
            this.setCurrentTime(this.plotter_frame.getXMin());
        }
    }//GEN-LAST:event_jMenuItemGotoPlotterMinTimeActionPerformed

    private void ClearData() {
        this.optitrack_tracks = null;
        HTPM_JFrame.gtlist = null;
        HTPM_JFrame.sutlist = null;
        this.drawPanel1.tracks = null;
        this.optitrack_unaffiliated_track = null;
        this.updateTree(true);
        this.drawPanel1.repaint();
    }
    private void jMenuItemClearDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemClearDataActionPerformed
        this.ClearData();
    }//GEN-LAST:event_jMenuItemClearDataActionPerformed

    private void jMenuItemChangeSourceLabelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChangeSourceLabelActionPerformed
        String new_label = JOptionPane.showInputDialog(this, "New source label?");
        for (Track t : this.drawPanel1.tracks) {
            if (t.selected) {
                t.source = new_label;
            }
        }
        this.updateTree(true);
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jMenuItemChangeSourceLabelActionPerformed

    private void jMenuItemConnectGTServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectGTServerActionPerformed
        try {
            String svr_host = JOptionPane.showInputDialog(this, "Server hostname or IP address?",
                    s.gt_server_host);
            String svr_port_s = JOptionPane.showInputDialog(this, "TCP Port Number?",
                    s.gt_server_port);
            short port = Short.valueOf(svr_port_s);
            MonitoredConnection c = new MonitoredConnection(this);
            TransformMatrixJPanel.showDialog(this, c);
            System.out.println("c.transform_filename = " + c.transform_filename);
            c.socket = new Socket(svr_host, port);
            if (gt_connections == null) {
                gt_connections = new LinkedList<MonitoredConnection>();
            }
            c.is_groundtruth = true;
            c.source = c.socket.getInetAddress().toString() + "/" + c.socket.getPort();
            gt_connections.add(c);
            s.gt_server_host = svr_host;
            s.gt_server_port = port;
            c.start();
        } catch (Exception e) {
            e.printStackTrace();
            final String msgS = e.getMessage();
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    myShowMessageDialog(HTPM_JFrame.main_frame, "Error connecting to  GT server : " + msgS);
                }
            });
        }
    }//GEN-LAST:event_jMenuItemConnectGTServerActionPerformed
    private ConnectionsJFrame connections_jframe = null;

    private void jMenuItemManageLiveConnectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemManageLiveConnectionsActionPerformed
        if (null != connections_jframe) {
            connections_jframe.setVisible(false);
            connections_jframe.dispose();
        }
        connections_jframe = new ConnectionsJFrame();
        connections_jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        connections_jframe.setVisible(true);
        connections_jframe.main_frame = this;
        connections_jframe.UpdateList();
    }//GEN-LAST:event_jMenuItemManageLiveConnectionsActionPerformed

    private void jMenuItemConnectSUTServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectSUTServerActionPerformed
        try {
            String svr_host = JOptionPane.showInputDialog(this, "Server hostname or IP address?",
                    s.sut_server_host);
            String svr_port_s = JOptionPane.showInputDialog(this, "TCP Port Number?",
                    s.sut_server_port);
            short port = Short.valueOf(svr_port_s);
            MonitoredConnection c = new MonitoredConnection(this);
            c.socket = new Socket(svr_host, port);
            if (sut_connections == null) {
                sut_connections = new LinkedList<MonitoredConnection>();
            }
            c.is_groundtruth = false;
            c.source = c.socket.getInetAddress().toString() + "/" + c.socket.getPort();
            sut_connections.add(c);
            s.sut_server_host = svr_host;
            s.sut_server_port = port;
            c.start();
        } catch (Exception e) {
            e.printStackTrace();
            final String msgS = e.getMessage();
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    myShowMessageDialog(HTPM_JFrame.main_frame,
                            "Error connecting to  SUT server : " + msgS);
                }
            });
        }
    }//GEN-LAST:event_jMenuItemConnectSUTServerActionPerformed

    private void jCheckBoxMenuItemShowBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemShowBackgroundActionPerformed
        this.drawPanel1.show_background_image =
                this.jCheckBoxMenuItemShowBackground.isSelected();
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jCheckBoxMenuItemShowBackgroundActionPerformed

    private void jCheckBoxMenuItemBackgroundGrayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemBackgroundGrayActionPerformed
        if (this.jCheckBoxMenuItemBackgroundGray.isSelected()) {
            this.drawPanel1.backgroundImage =
                    this.drawPanel1.backgroundImageGray;
        } else {
            this.drawPanel1.backgroundImage =
                    this.drawPanel1.backgroundImageColor;
        }
        this.drawPanel1.scaledBackgroundImage = null;
        this.drawPanel1.subBackgroundImage = this.drawPanel1.backgroundImage;
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jCheckBoxMenuItemBackgroundGrayActionPerformed

    private void jCheckBoxMenuItemRepositionBackgroundActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemRepositionBackgroundActionPerformed
        if (this.jCheckBoxMenuItemRepositionBackground.isSelected()) {
            myShowMessageDialog(this, "Drag background image into position. Hold shift key and drag to resize background image.");
        }
    }//GEN-LAST:event_jCheckBoxMenuItemRepositionBackgroundActionPerformed

    public static void myShowMessageDialog(Component d, String msg) {
        System.err.println(msg);
        for (int p = 60; p < msg.length(); p += 60) {
            if (msg.length() > p + 2) {
                msg = msg.substring(0, p) + "\r\n" + msg.substring(p);
            }
        }
        if (msg.length() > 300) {
            msg = msg.substring(0, 296).trim() + "...";
        }
        JOptionPane.showMessageDialog(d, msg);
    }

    private boolean updateROI() {
        try {
            String roi = Arrays.toString(this.drawPanel1.ROI);
            String new_roiS = JOptionPane.showInputDialog(this, "ROI: Please enter the in the form [x_min,y_min,x_max,y_max] ",
                    roi);
            if (new_roiS == null) {
                return false;
            }
            new_roiS = new_roiS.trim();
            if (new_roiS.startsWith("[")) {
                new_roiS = new_roiS.substring(1);
            }
            if (new_roiS.endsWith("]")) {
                new_roiS = new_roiS.substring(0, new_roiS.length() - 1);
            }
            String fields[] = new_roiS.split(",");
            if (fields.length < this.drawPanel1.ROI.length) {
                myShowMessageDialog(this, "Not enough fields");
                return updateROI();
            }
            for (int i = 0; i < fields.length && i < this.drawPanel1.ROI.length; i++) {
                this.drawPanel1.ROI[i] = Double.valueOf(fields[i]);
            }
            s.roi_x_min = this.drawPanel1.ROI[0];
            s.roi_y_min = this.drawPanel1.ROI[1];
            s.roi_x_max = this.drawPanel1.ROI[2];
            s.roi_y_max = this.drawPanel1.ROI[3];
        } catch (Exception exception) {
            exception.printStackTrace();
            myShowMessageDialog(this, exception.getMessage());
            return updateROI();
        }
        return true;
    }

    private void jMenuItemROIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemROIActionPerformed
        this.updateROI();
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jMenuItemROIActionPerformed

    private void jMenuItemComputeMinSUTRadiusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemComputeMinSUTRadiusActionPerformed
        try {
            if (!this.updateROI()) {
                return;
            }
            HTPM_JFrame.frameStatsCsvF = null;
            String time_inc_s = JOptionPane.showInputDialog(this, "Time Increment?",
                    HTPM_JFrame.s.time_inc);
            HTPM_JFrame.s.time_inc = Double.valueOf(time_inc_s);
            s.sut_radius_increase = 0.0;
            String sut_radius_inc_s = JOptionPane.showInputDialog(this,
                    "SUT Radius Increment?",
                    s.sut_radius_inc);
            s.sut_radius_inc = Double.valueOf(sut_radius_inc_s);
            String project_ahead_time_s = JOptionPane.showInputDialog(this,
                    "Project Time ahead over interval in seconds for decelleration?",
                    s.project_ahead_time);
            s.project_ahead_time = Double.valueOf(project_ahead_time_s);
            String scale_s = JOptionPane.showInputDialog(this,
                    "Occupancy cells per meter?",
                    String.format("%.0f", HTPM_JFrame.s.scale));
            HTPM_JFrame.s.scale = Double.valueOf(scale_s);
            final boolean orig_inc_sut_radius_on_false_clear = HTPM_JFrame.inc_sut_radius_on_false_clear;
            HTPM_JFrame.inc_sut_radius_on_false_clear = true;
            this.startProcessTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        HTPM_JFrame.inc_sut_radius_on_false_clear =
                                orig_inc_sut_radius_on_false_clear;
                        PlayBeep();
                        drawPanel1.repaint();
                        jTextFieldSUTRadius.setText(String.format("%.3f", s.sut_radius_increase));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });
            //            HTPM_JFrame.processAll();
            //            plotterJFrame plotter_frame =
            //                    new plotterJFrame();
            //            plotter_frame.LoadFile(f.getCanonicalPath());
            //            plotter_frame.setVisible(true);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }//GEN-LAST:event_jMenuItemComputeMinSUTRadiusActionPerformed

    private void jRadioButtonMenuItemModeGTOccupiedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemModeGTOccupiedActionPerformed
        this.UpdateDisplayMode();
    }//GEN-LAST:event_jRadioButtonMenuItemModeGTOccupiedActionPerformed

    private void jRadioButtonMenuItemModeSUTOccupiedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemModeSUTOccupiedActionPerformed
        this.UpdateDisplayMode();
    }//GEN-LAST:event_jRadioButtonMenuItemModeSUTOccupiedActionPerformed

    private void jMenuItemEditTimeProjActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemEditTimeProjActionPerformed
        String project_ahead_time_s = JOptionPane.showInputDialog(this,
                "Project Time ahead over interval in seconds for decelleration?",
                s.project_ahead_time);
        s.project_ahead_time = Double.valueOf(project_ahead_time_s);
    }//GEN-LAST:event_jMenuItemEditTimeProjActionPerformed

    private void jMenuItemTransformSelectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTransformSelectedActionPerformed
        List<Track> tracks = this.drawPanel1.tracks;
        if (null != tracks) {
            for (Track t : tracks) {
                if (t.selected && null != t.data) {
                    double transform[] = TransformMatrixJPanel.showDialog(this,
                            "Transform for " + t.name,
                            t.getTransform());
                    if (null == transform) {
                        continue;
                    }
                    for (TrackPoint tp : t.data) {
                        tp.applyTransform(transform);
                    }
                    if (null != t.currentPoint) {
                        t.currentPoint.applyTransform(transform);
                    }
                }
            }
            this.drawPanel1.repaint();
        }
    }//GEN-LAST:event_jMenuItemTransformSelectedActionPerformed

    private void jMenuItemOpenSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenSettingsActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            reloadSettings(chooser.getSelectedFile());
            this.updateTree(false);
            this.updateDrawPanelViewport();
        }
    }//GEN-LAST:event_jMenuItemOpenSettingsActionPerformed

    private void jMenuItemSaveSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveSettingsActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            this.currentValsToSettings();
            saveSettings(s, chooser.getSelectedFile());
        }
    }//GEN-LAST:event_jMenuItemSaveSettingsActionPerformed

    private void jMenuItemResetSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemResetSettingsActionPerformed
        s = new settings();
        this.settingsToCurrent();
        this.updateEverything();
    }//GEN-LAST:event_jMenuItemResetSettingsActionPerformed

    private void jCheckBoxMenuItemIgnoreSUTVelocitiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemIgnoreSUTVelocitiesActionPerformed
        s.ignore_sut_velocities =
                this.jCheckBoxMenuItemIgnoreSUTVelocities.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemIgnoreSUTVelocitiesActionPerformed

    private void jButtonStatsDialogOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStatsDialogOkActionPerformed
        this.jDialog1.setVisible(false);
    }//GEN-LAST:event_jButtonStatsDialogOkActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        String s = getGlobalStatsString();
        this.jTextAreaStats.setText(s);
        System.out.println(s);
        this.jDialog1.setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed
    protected boolean live = false;

    public void setLive(boolean _live) {
        this.setLive(_live, true);
    }

    public void setLive(boolean _live, boolean set_checkbox) {
        if (this.live != _live) {
            this.live = _live;
            this.jLabelTime.setEnabled(!live);
            this.jSliderTime.setEnabled(!live);
            if (live) {
                this.jSliderTime.setValue(this.jSliderTime.getMaximum());
            }
            if (!Double.isInfinite(outer_max_time) && !Double.isNaN(outer_max_time)) {
                this.setCurrentTime(outer_max_time, false);
            } else if (!Double.isInfinite(inner_max_time) && !Double.isNaN(inner_max_time)) {
                this.setCurrentTime(inner_max_time, false);
            }
            if (set_checkbox) {
                this.jCheckBoxLive.setSelected(live);
            }
            this.drawPanel1.repaint();
        }
    }

    private void jCheckBoxLiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxLiveActionPerformed
        this.setLive(this.jCheckBoxLive.isSelected(), false);
    }//GEN-LAST:event_jCheckBoxLiveActionPerformed

    private void jMenuItemShowTimeLocalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowTimeLocalActionPerformed
        long t = (long) (CurrentTime * 1e3);
        Date d = new Date(t);
        DateFormat df = DateFormat.getDateTimeInstance();
        String s = df.format(d);
        System.out.println("CurrentTime = " + CurrentTime + ": \t" + s);
        myShowMessageDialog(this, "CurrentTime = " + CurrentTime + ": \t" + s);

    }//GEN-LAST:event_jMenuItemShowTimeLocalActionPerformed

    private double[][] convertDataTo2DFloatArray(List<Track> lt, int count) {
        int len = 0;
        double dist = 0.0;
        for (Track t : lt) {
            if (null == t.data) {
                continue;
            }
            if (t.data.size() < 3) {
                continue;
            }
            len += t.data.size();
            for (int i = 1; i < t.data.size(); i++) {
                dist += t.data.get(i).distance(t.data.get(i - 1));
            }
        }
        Collections.sort(lt, new Comparator<Track>() {
            @Override
            public int compare(Track o1, Track o2) {
                if (o1.data == null || o1.data.size() < 3 || o1.data.get(0) == null) {
                    return 0;
                }
                if (o2.data == null || o2.data.size() < 3 || o2.data.get(0) == null) {
                    return 0;
                }
                double start_diff = o1.data.get(0).time - o2.data.get(0).time;
                int o1l = o1.data.size();
                int o2l = o2.data.size();
                double end_diff = o1.data.get(o1l - 1).time - o2.data.get(o2l - 1).time;
                return Double.compare(end_diff + start_diff, 0);
            }
        });
        double f2aa[][] = new double[count][];
        int index = 0;
        double dist_out = 0.0;
        final double dist_inc = dist / (count + 1);
        double dist_next_out = dist_out + dist / count;
        for (Track t : lt) {
            if (null == t.data) {
                continue;
            }
            if (t.data.size() < 3) {
                continue;
            }
            for (int i = 1; i < t.data.size(); i++) {
                double start_dist = dist_out;
                double cur_dist = t.data.get(i).distance(t.data.get(i - 1));
                double next_dist = dist_out + cur_dist;
                while (next_dist > dist_next_out) {
                    dist_out = dist_next_out;
                    double s0 = (next_dist - dist_out) / (next_dist - start_dist);
                    double s1 = 1 - s0;
                    f2aa[index] = new double[3];
                    f2aa[index][0] = t.data.get(i).x * s1 + t.data.get(i - 1).x * s0;
                    f2aa[index][1] = t.data.get(i).y * s1 + t.data.get(i - 1).y * s0;
                    f2aa[index][2] = 1.0;
                    index = index + 1;
                    dist_next_out = dist_out + dist_inc;
                }
                dist_out = next_dist;
            }
        }
        return f2aa;
    }

    private void jMenuItem2DRegistrationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2DRegistrationActionPerformed
        double f2aa_sut[][] = convertDataTo2DFloatArray(HTPM_JFrame.sutlist, 10);
        double f2aa_gt[][] = convertDataTo2DFloatArray(HTPM_JFrame.gtlist, 10);
        Jama.Matrix m_sut = new Jama.Matrix(f2aa_sut);
        Jama.Matrix m_gt = new Jama.Matrix(f2aa_gt);
        Jama.Matrix reg = m_sut.solve(m_gt);
        reg.print(6, 3);
    }//GEN-LAST:event_jMenuItem2DRegistrationActionPerformed

    private void jCheckBoxMenuItemGrayTracksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemGrayTracksActionPerformed
        this.drawPanel1.use_gray_tracks = this.jCheckBoxMenuItemGrayTracks.isSelected();
        this.drawPanel1.repaint();
    }//GEN-LAST:event_jCheckBoxMenuItemGrayTracksActionPerformed

    public static List<Point2DwTime> convertTracksToSinglePoint2DList(List<Track> lt) {
        if (null == lt) {
            return null;
        }
        List<Point2DwTime> l = new LinkedList<Point2DwTime>();
        Collections.sort(lt, new Comparator<Track>() {
            @Override
            public int compare(Track o1, Track o2) {
                double t1 = Double.POSITIVE_INFINITY;
                double t2 = Double.POSITIVE_INFINITY;
                try {
                    t1 = o1.data.get(0).time;
                } catch (Exception e) {
                }
                try {
                    t2 = o2.data.get(0).time;
                } catch (Exception e) {
                }
                return Double.compare(t1, t2);
            }
        });
        double time = Double.NEGATIVE_INFINITY;
        for (Track t : lt) {
            if (t.data == null) {
                continue;
            }
            if (t.data.get(t.data.size() - 1).time <= time) {
                continue;
            }
            for (TrackPoint tp : t.data) {
                if (tp.time <= time) {
                    continue;
                }
                Point2DwTime p = new Point2DwTime(tp.x, tp.y, tp.time);
                l.add(p);
                time = tp.time;
            }
        }
        return l;
    }
    private void jMenuItemComputeTimeOffsetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemComputeTimeOffsetActionPerformed
        List<Point2DwTime> lsut = convertTracksToSinglePoint2DList(sutlist);
        List<Point2DwTime> lgt = convertTracksToSinglePoint2DList(gtlist);
        double mid_sut_time = (lsut.get(0).time + lsut.get(lsut.size() - 1).time) / 2.0;
        double mid_gt_time = (lgt.get(0).time + lgt.get(lgt.size() - 1).time) / 2.0;
        double start_offset = mid_sut_time - mid_gt_time - 2.0;
        double corr[] = VelocityCorrellationSimulationAndTesting.computeVelCorrArrayFromPoints(lsut, lgt,
                start_offset, 0.2, 20);
        diagapplet.plotter.plotterJFrame.ShowDoubleArray("velocity_corr", corr);
    }//GEN-LAST:event_jMenuItemComputeTimeOffsetActionPerformed

    private void jMenuItemShowComputedVelocitiesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemShowComputedVelocitiesActionPerformed
        try {
            List<Point2DwTime> lsut = convertTracksToSinglePoint2DList(sutlist);
            List<Point2DwTime> lgt = convertTracksToSinglePoint2DList(gtlist);
            double mid_sut_time = (lsut.get(0).time + lsut.get(lsut.size() - 1).time) / 2.0;
            double mid_gt_time = (lgt.get(0).time + lgt.get(lgt.size() - 1).time) / 2.0;
            double offset = 0;//mid_sut_time - mid_gt_time;
//        double corr[] =
//                VelocityCorrellationSimulationAndTesting.computeVelCorrArrayFromPoints(lsut, lgt,
//                start_offset, 0.2, 20);
            List<VelocityCorrellationSimulationAndTesting.VelWTime> lvgt = point2DwTimeListToVelList(lgt);
            List<VelocityCorrellationSimulationAndTesting.VelWTime> lvsut = point2DwTimeListToVelList(lsut);
            double vc = VelocityCorrellationSimulationAndTesting.computeVelCorr(lvgt, lvsut, offset);
            System.out.println("vc = " + vc);
            diagapplet.plotter.plotterJFrame.ShowXYObjectsList("vcorr", "time",
                    VelocityCorrellationSimulationAndTesting.velCorrList);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }//GEN-LAST:event_jMenuItemShowComputedVelocitiesActionPerformed

    private void jMenu7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu7ActionPerformed
    private File promptedLogFile = null;

    private void LogSingleFrame(File f) throws Exception {
        boolean existed = f.exists();
        PrintStream ps = new PrintStream(new FileOutputStream(f, true));
        if (!existed) {
            printCsvHeader(ps);
        }
        List<Track> tracks = this.drawPanel1.tracks;
        for (Track t : tracks) {
            if (null != t.data) {
                TrackPoint last_pt = t.data.get(t.data.size() - 1);
                printOneLine(last_pt, t.name, ps);
            }
        }
        ps.close();
    }

    private void PromptSaveLogFile() {
        try {
            if (null == promptedLogFile) {
                return;
            }
            if (!this.jCheckBoxMenuItemPromptLogData.isSelected()) {
                return;
            }
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, "Record again?")) {
                if (null == promptedLogFile) {
                    return;
                }
                if (!this.jCheckBoxMenuItemPromptLogData.isSelected()) {
                    return;
                }
                LogSingleFrame(promptedLogFile);
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        PromptSaveLogFile();
                    }
                });
            } else {
                this.promptedLogFile = null;
                this.jCheckBoxMenuItemPromptLogData.setSelected(false);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            this.promptedLogFile = null;
            this.jCheckBoxMenuItemPromptLogData.setSelected(false);
        }
    }

    private void jCheckBoxMenuItemPromptLogDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemPromptLogDataActionPerformed
        try {
            if (this.jCheckBoxMenuItemPromptLogData.isSelected()) {
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File(s.save_file_dir));
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "Comma Seperated Variable Files", "csv");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showSaveDialog(this);
                promptedLogFile = null;
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    promptedLogFile = chooser.getSelectedFile();
                    s.save_file_dir = promptedLogFile.getParentFile().getCanonicalPath();
                }
                if (null == promptedLogFile) {
                    this.jCheckBoxMenuItemPromptLogData.setSelected(false);
                    return;
                }
                if (!promptedLogFile.exists()) {
                    PrintStream ps = new PrintStream(new FileOutputStream(promptedLogFile));
                    printCsvHeader(ps);
                    ps.close();
                }
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        PromptSaveLogFile();
                    }
                });
            } else {
                this.promptedLogFile = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.promptedLogFile = null;
            this.jCheckBoxMenuItemPromptLogData.setSelected(false);
        }
    }//GEN-LAST:event_jCheckBoxMenuItemPromptLogDataActionPerformed
    private File outputLogFile = new File(System.getProperty("user.home"), "log.csv");

    private void updateLogSingleFrameButton() throws Exception {
        this.jButtonLogSingleFrame.setText(" Log Single Frame to " + outputLogFile.getCanonicalPath() + " ");
    }

    private void jButtonNewLogFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewLogFileActionPerformed
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(s.save_file_dir));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Comma Seperated Variable Files", "csv");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                s.save_file_dir = chooser.getSelectedFile().getParentFile().getCanonicalPath();
                outputLogFile = chooser.getSelectedFile();
                this.updateLogSingleFrameButton();
            }
        } catch (Exception ex) {
            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonNewLogFileActionPerformed

    private void jButtonLogSingleFrameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLogSingleFrameActionPerformed
        try {
            this.LogSingleFrame(this.outputLogFile);
        } catch (Exception ex) {
            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jButtonLogSingleFrameActionPerformed

    private void jMenuItemSetTrackTailHighlightTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSetTrackTailHighlightTimeActionPerformed
        try {
            String s = JOptionPane.showInputDialog("Track Tail Highlight Length?",
                    this.drawPanel1.getTrack_tail_highlight_time());
            if (null != s && s.length() > 0) {
                double ttht = Double.valueOf(s);
                this.drawPanel1.setTrack_tail_highlight_time(ttht);
            }
        } catch (Exception ex) {
            Logger.getLogger(HTPM_JFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItemSetTrackTailHighlightTimeActionPerformed

    private void jCheckBoxMenuItemShowDisconnectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemShowDisconnectedActionPerformed
        this.drawPanel1.show_disconnected = this.jCheckBoxMenuItemShowDisconnected.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuItemShowDisconnectedActionPerformed
    private Point last_dragged_evt_point = null;

    private void drawPanel1MouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_drawPanel1MouseDragged
//        System.out.println("evt = " + evt);
        Point evt_point = evt.getPoint();
        switch (this.drawPanel1.getDragEnum()) {
            case MEASURE:
                this.drawPanel1.setMeasureEnd(evt.getX(), evt.getY());
                break;

            case PAN:
//                System.out.println("evt_point = " + evt_point);
                if (null != this.mouse_pressed_evt_point
                        && null != this.mouse_pressed_viewport_position
                        && null != this.last_dragged_evt_point
                        && (evt_point.x != last_dragged_evt_point.x || evt_point.y != last_dragged_evt_point.y)) {
//                JViewport view_port = this.jScrollPaneDrawPanel.getViewport();
//                Point pt = new Point();
//                pt.x = this.mouse_pressed_viewport_position.x - (evt_point.x - this.mouse_pressed_evt_point.x);
//                pt.y = this.mouse_pressed_viewport_position.y - (evt_point.y - this.mouse_pressed_evt_point.y);
//                System.out.println("pt = " + pt);
//                view_port.setViewPosition(pt);
                    this.drawPanel1.x_max = this.mouse_pressed_x_max;
                    this.drawPanel1.x_min = this.mouse_pressed_x_min;
                    this.drawPanel1.y_max = this.mouse_pressed_y_max;
                    this.drawPanel1.y_min = this.mouse_pressed_y_min;
                    Point2D.Double evt_w_point = this.drawPanel1.img2WorldPoint(evt_point);
//                    System.out.println("evt_w_point = " + evt_w_point);
                    this.drawPanel1.x_max = this.mouse_pressed_x_max - (evt_w_point.x - this.mouse_pressed_evt_w_point.x);
                    this.drawPanel1.y_max = this.mouse_pressed_y_max - (evt_w_point.y - this.mouse_pressed_evt_w_point.y);
                    this.drawPanel1.x_min = this.mouse_pressed_x_min - (evt_w_point.x - this.mouse_pressed_evt_w_point.x);
                    this.drawPanel1.y_min = this.mouse_pressed_y_min - (evt_w_point.y - this.mouse_pressed_evt_w_point.y);
                    this.drawPanel1.update_img_to_world_scale(this.drawPanel1.getPreferredSize());
                    Point2D.Double new_evt_w_point = this.drawPanel1.img2WorldPoint(evt_point);
                    if (new_evt_w_point.distance(this.mouse_pressed_evt_w_point) > 0.1) {
                        System.err.println("pan check failed.");
                    }
                    this.drawPanel1.repaint();
                    //this.jScrollPaneDrawPanel.repaint();
                    //this.drawPanel1.repaint();
//                this.jScrollPaneDrawPanel.setViewport(view_port);
//                this.jScrollPaneDrawPanel.invalidate();
                }
                break;

            case SELECT_TRACKS:
                Rectangle r = new Rectangle();
                r.x = Math.min(this.mouse_pressed_evt_point.x, evt_point.x);
                r.y = Math.min(this.mouse_pressed_evt_point.y, evt_point.y);
                r.width = Math.abs(this.mouse_pressed_evt_point.x - evt_point.x);
                r.height = Math.abs(this.mouse_pressed_evt_point.y - evt_point.y);
                this.drawPanel1.setSelectRect(r);
                break;
        }
    }//GEN-LAST:event_drawPanel1MouseDragged
    Point mouse_pressed_viewport_position = null;
    Point mouse_pressed_evt_point = null;
    Point2D.Double mouse_pressed_evt_w_point = null;
    double mouse_pressed_x_min;
    double mouse_pressed_x_max;
    double mouse_pressed_y_min;
    double mouse_pressed_y_max;

    private void drawPanel1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_drawPanel1MousePressed
        mouse_pressed_evt_point = evt.getPoint();
        this.last_dragged_evt_point = evt.getPoint();
        JViewport mouse_pressed_viewport = this.jScrollPaneDrawPanel.getViewport();
        mouse_pressed_viewport_position = mouse_pressed_viewport.getViewPosition();
        System.out.println("mouse_pressed_viewport_position = " + mouse_pressed_viewport_position);
        this.mouse_pressed_evt_w_point = this.drawPanel1.img2WorldPoint(this.mouse_pressed_evt_point);
        mouse_pressed_x_min = this.drawPanel1.x_min;
        mouse_pressed_x_max = this.drawPanel1.x_max;
        mouse_pressed_y_min = this.drawPanel1.y_min;
        mouse_pressed_y_max = this.drawPanel1.y_max;
//        System.out.println("evt = " + evt);
        if (this.jRadioButtonMeasure.isSelected()) {
            this.drawPanel1.setMeasureStart(evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_drawPanel1MousePressed

    public void updateDragEnum() {
        if (this.jRadioButtonMeasure.isSelected()) {
            this.drawPanel1.setDragEnum(DrawPanelDragEnum.MEASURE);
        } else if (this.jRadioButtonPan.isSelected()) {
            this.drawPanel1.setDragEnum(DrawPanelDragEnum.PAN);
        } else if (this.jRadioButtonSelectTracks.isSelected()) {
            this.drawPanel1.setDragEnum(DrawPanelDragEnum.SELECT_TRACKS);
        }
    }
    private void jRadioButtonMeasureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMeasureActionPerformed
        this.updateDragEnum();
    }//GEN-LAST:event_jRadioButtonMeasureActionPerformed

    private void jRadioButtonPanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonPanActionPerformed
        this.updateDragEnum();
    }//GEN-LAST:event_jRadioButtonPanActionPerformed

    public void selectTracksByRectangle(Rectangle2D.Double wrect) {
        for (Track t : this.drawPanel1.tracks) {
            t.selected = false;
            if (null != t.explicit_color) {
                t.color = t.explicit_color;
            } else {
                if (t.is_groundtruth) {
                    t.color = Color.red;
                } else {
                    t.color = Color.blue;
                }
            }
            if (null != t.data) {
                for (TrackPoint tp : t.data) {
                    if (tp.x > wrect.x && tp.x < wrect.x + wrect.width
                            && tp.y > wrect.y && tp.y < wrect.y + wrect.height) {
                        t.selected = true;
                        if (t.is_groundtruth) {
                            t.color = Color.orange;
                        } else {
                            t.color = Color.magenta;
                        }
                        break;
                    }
                }
            }
        }
        this.updateEverything();
    }

    private void drawPanel1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_drawPanel1MouseReleased
//        System.out.println("evt = " + evt);
        this.mouse_pressed_evt_point = null;
        this.mouse_pressed_viewport_position = null;
        this.last_dragged_evt_point = null;
        switch (this.drawPanel1.getDragEnum()) {
            case SELECT_TRACKS:
                Rectangle r = this.drawPanel1.getSelectRect();
                if (null != r) {
                    selectTracksByRectangle(this.drawPanel1.img2WorldRectangle(this.drawPanel1.getSelectRect()));
                    this.drawPanel1.setSelectRect(null);
                }
                break;

            default:
                break;
        }
    }//GEN-LAST:event_drawPanel1MouseReleased

    private void jRadioButtonSelectTracksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonSelectTracksActionPerformed
        this.updateDragEnum();
    }//GEN-LAST:event_jRadioButtonSelectTracksActionPerformed

    static public class GlobalStats {

        int num_tracks;
        int num_points;
        int num_time_diffs;
        double min_x;
        double min_y;
        double max_x;
        double max_y;
        double min_radius;
        double max_radius;
        double sum_radius;
        double avg_radius;
        double min_confidence;
        double max_confidence;
        double sum_confidence;
        double avg_confidence;
        double max_time_diff;
        double min_time_diff;
        double total_time;
        double avg_time_diff;
        double max_dist_diff;
        double min_dist_diff;
        double total_dist;
        double total_vel;
        double avg_dist_diff;
        double avg_computed_vel;
        double avg_reported_vel;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("");
            sb.append("num_tracks=").append(num_tracks).append("\n");
            sb.append("num_points=").append(num_points).append("\n");
            sb.append("max_time_diff=").append(max_time_diff).append("\n");
            sb.append("min_time_diff=").append(min_time_diff).append("\n");
            sb.append("avg_time_diff=").append(avg_time_diff).append("\n");
            sb.append("max_dist_diff=").append(max_dist_diff).append("\n");
            sb.append("min_dist_diff=").append(min_dist_diff).append("\n");
            sb.append("avg_dist_diff=").append(avg_dist_diff).append("\n");
            sb.append("max_radius=").append(max_radius).append("\n");
            sb.append("min_radius=").append(min_radius).append("\n");
            sb.append("avg_radius=").append(avg_radius).append("\n");
            sb.append("max_confidence=").append(max_confidence).append("\n");
            sb.append("min_confidence=").append(min_confidence).append("\n");
            sb.append("avg_confidence=").append(avg_confidence).append("\n");
            sb.append("max_x=").append(max_x).append("\n");
            sb.append("min_x=").append(min_x).append("\n");
            sb.append("max_y=").append(max_y).append("\n");
            sb.append("min_y=").append(min_y).append("\n");
            sb.append("avg_computed_vel=").append(avg_computed_vel).append("\n");
            sb.append("avg_reported_vel=").append(avg_reported_vel).append("\n");
            return sb.toString();
        }
    }
    static public GlobalStats gs_gt = null;
    static public GlobalStats gs_sut = null;

    static public GlobalStats computeGlobalStats(GlobalStats g,
            List<Track> tracks) {
        if (null == g) {
            g = new GlobalStats();
        }
        if (null != tracks) {
            g.num_tracks = tracks.size();
            g.max_time_diff = 0.0;
            g.min_time_diff = Double.POSITIVE_INFINITY;
            g.total_time = 0.0;
            g.max_dist_diff = 0.0;
            g.min_dist_diff = Double.POSITIVE_INFINITY;
            g.total_dist = 0.0;
            g.total_vel = 0.0;
            g.max_x = Double.NEGATIVE_INFINITY;
            g.min_x = Double.POSITIVE_INFINITY;
            g.max_y = Double.NEGATIVE_INFINITY;
            g.min_y = Double.POSITIVE_INFINITY;
            g.max_radius = Double.NEGATIVE_INFINITY;
            g.min_radius = Double.POSITIVE_INFINITY;
            g.max_confidence = Double.NEGATIVE_INFINITY;
            g.min_confidence = Double.POSITIVE_INFINITY;
            for (Track t : tracks) {
                if (null != t.data) {
                    g.num_points += t.data.size();
                    for (int i = 0; i < t.data.size() - 1; i++) {
                        TrackPoint pt = t.data.get(i);
                        if (g.min_x > pt.x) {
                            g.min_x = pt.x;
                        }
                        if (g.max_x < pt.x) {
                            g.max_x = pt.x;
                        }
                        if (g.min_y > pt.y) {
                            g.min_y = pt.y;
                        }
                        if (g.max_y < pt.y) {
                            g.max_y = pt.y;
                        }
                        g.sum_confidence += pt.confidence;
                        if (g.min_confidence > pt.confidence) {
                            g.min_confidence = pt.confidence;
                        }
                        if (g.max_confidence < pt.confidence) {
                            g.max_confidence = pt.confidence;
                        }
                        g.sum_radius += pt.radius;
                        if (g.min_radius > pt.radius) {
                            g.min_radius = pt.radius;
                        }
                        if (g.max_radius < pt.radius) {
                            g.max_radius = pt.radius;
                        }
                        TrackPoint next_pt = t.data.get(i + 1);
                        g.total_vel +=
                                Math.sqrt(pt.vel_x * pt.vel_x + pt.vel_y * pt.vel_y);
                        double time_diff =
                                (next_pt.time - pt.time);
                        if (g.max_time_diff < time_diff) {
                            g.max_time_diff = time_diff;
                        }
                        if (g.min_time_diff > time_diff) {
                            g.min_time_diff = time_diff;
                        }
                        g.total_time += time_diff;
                        double dist_diff =
                                (next_pt.distance(pt));
                        if (g.max_dist_diff < dist_diff) {
                            g.max_dist_diff = dist_diff;
                        }
                        if (g.min_dist_diff > dist_diff) {
                            g.min_dist_diff = dist_diff;
                        }
                        g.total_dist += dist_diff;
                        ++g.num_time_diffs;
                    }
                }
            }
            if (g.num_time_diffs > 0) {
                g.avg_time_diff = g.total_time / g.num_time_diffs;
                g.avg_dist_diff = g.total_dist / g.num_time_diffs;
                g.avg_computed_vel = g.avg_dist_diff / g.avg_time_diff;
                g.avg_reported_vel = g.total_vel / g.num_time_diffs;
                g.avg_confidence = g.sum_confidence / g.num_time_diffs;
                g.avg_radius = g.sum_radius / g.num_time_diffs;
            }
        }
        return g;
    }

    static public String getGlobalStatsString() {
        StringBuilder sb = new StringBuilder("");
        if (null != gtlist) {
            gs_gt = computeGlobalStats(gs_gt, gtlist);
            sb.append("Ground-Truth:\n").append(gs_gt.toString());
        }
        if (null != sutlist) {
            gs_sut = computeGlobalStats(gs_sut, sutlist);
            sb.append("\n\nSystem-Under-Test:\n").append(gs_sut.toString());
        }
        try {
            if (null == last_combined_fs) {
                last_combined_fs = HTPM_JFrame.processAll();
            }
            if (null != last_combined_fs) {
                sb.append("\n\nLast Comparison:\n");
                for (Field f : FrameStats.class.getFields()) {
                    String name = f.getName();
                    Object o = f.get(last_combined_fs);
                    if (null == o) {
                        continue;
                    }
                    if (!Double.class.isAssignableFrom(o.getClass())) {
                        continue;
                    }
                    sb.append(name).append("=").append(f.getDouble(last_combined_fs)).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (null != s) {
                sb.append("\n\nSettings:\n");
                for (Field f : settings.class.getFields()) {
                    String name = f.getName();
                    Object o = f.get(s);
                    if (null == o) {
                        continue;
                    }
                    sb.append(name).append("=").append(o.toString()).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    static public void PlayAlert() {
        URL url = main_frame.getClass().getResource("/htpm_resources/train.wav");
        AudioClip clip = Applet.newAudioClip(url);
        clip.play();
    }

    static public void PlayBeep() {
        URL url = main_frame.getClass().getResource("/htpm_resources/alert.wav");
        AudioClip clip = Applet.newAudioClip(url);
        clip.play();
    }

    public void UpdateConnectionsList() {
        if (null != this.connections_jframe) {
            this.connections_jframe.UpdateList();
        }
    }

    public void panelRepaint() {
        this.drawPanel1.repaint();
    }

    public void UpdateConnectionCount(MonitoredConnection c) {
        if (null != this.connections_jframe) {
            this.connections_jframe.UpdateCount(c);
        }
    }

    public List<Track> getTracks() {
        if (this.drawPanel1.tracks == null) {
            this.drawPanel1.tracks = new ArrayList<Track>();
        }
        return this.drawPanel1.tracks;
    }
    MonitoredConnection default_client = null;
    private Thread gt_thread = null;
    private ServerSocket gt_server_socket = null;
    public LinkedList<MonitoredConnection> gt_connections = null;

    public void CloseGTThread() {
        try {
            if (null != gt_connections) {
                for (MonitoredConnection c : gt_connections) {
                    c.close();
                }
                gt_connections = null;
            }
            if (null != gt_server_socket) {
                gt_server_socket.close();
            }
            if (null != gt_thread) {
                gt_thread.interrupt();
                gt_thread.join(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenGTThread() {
        CloseGTThread();
        try {
            gt_server_socket = new ServerSocket();
            gt_server_socket.setReuseAddress(true);
            gt_server_socket.bind(new InetSocketAddress(s.gt_port));
            gt_thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!gt_thread.isInterrupted()) {
                            MonitoredConnection c = new MonitoredConnection(humantrackingperformancemetrics.HTPM_JFrame.this);
                            c.socket = gt_server_socket.accept();
                            if (gt_connections == null) {
                                gt_connections = new LinkedList<MonitoredConnection>();
                            }
                            c.is_groundtruth = true;
                            c.source = c.socket.getInetAddress().toString() + "/" + c.socket.getPort();
                            gt_connections.add(c);
                            c.start();
                        }
                    } catch (SocketException se) {
                        // We expect an exception to be thrown when the user
                        // requests the close() in CloseGTThread()
                        // anything else we should probably take a look at.
                        if (se.getMessage().indexOf("closed") < 0) {
                            se.printStackTrace();
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });
            gt_thread.start();
        } catch (Exception exception) {
            exception.printStackTrace();
            myShowMessageDialog(this, exception.getMessage());
        }
    }
    private Thread sut_thread = null;
    private ServerSocket sut_server_socket = null;
    public LinkedList<MonitoredConnection> sut_connections = null;

    public void CloseSUTThread() {
        try {
            if (null != sut_connections) {
                for (MonitoredConnection c : sut_connections) {
                    c.close();
                }
                sut_connections = null;
            }
            if (null != sut_server_socket) {
                sut_server_socket.close();
            }
            if (null != sut_thread) {
                sut_thread.interrupt();
                sut_thread.join(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenSUTThread() {
        CloseSUTThread();
        try {
            sut_server_socket = new ServerSocket();
            sut_server_socket.setReuseAddress(true);
            sut_server_socket.bind(new InetSocketAddress(s.sut_port));
            sut_thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!sut_thread.isInterrupted()) {
                            MonitoredConnection c = new MonitoredConnection(humantrackingperformancemetrics.HTPM_JFrame.this);
                            c.socket = sut_server_socket.accept();
                            if (sut_connections == null) {
                                sut_connections = new LinkedList<MonitoredConnection>();
                            }
                            c.is_groundtruth = false;
                            c.source = c.socket.getInetAddress().toString() + "/" + c.socket.getPort();
                            sut_connections.add(c);
                            c.start();
                        }
                    } catch (SocketException se) {
                        // We expect an exception to be thrown when the user
                        // requests the close() in CloseSUTThread()
                        // anything else we should probably take a look at.
                        if (se.getMessage().indexOf("closed") < 0) {
                            se.printStackTrace();
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });
            sut_thread.start();
        } catch (Exception exception) {
            exception.printStackTrace();
            myShowMessageDialog(this, exception.getMessage());
        }
    }

    static public class RocData {

        public float pos_ratio[];
        public float neg_ratio[];
    };
    static public int num_roc_thresholds = 10;

    static public RocData computeRoc() {
        RocData ret = new RocData();
        float inc = 1f / (num_roc_thresholds - 1);
        ret.neg_ratio = new float[num_roc_thresholds];
        ret.pos_ratio = new float[num_roc_thresholds];
        for (int i = 0; i < num_roc_thresholds; i++) {
            HTPM_JFrame.s.confidence_threshold = inc * i;
            if (null != rocTask) {
                rocTask.set_conf_index(i);
            }
            FrameStats fs = HTPM_JFrame.processAll();
//                System.out.println("" + fs.true_occupied_area / fs.total_gt_occupied_area
//                        + "," + fs.false_occupied_area / fs.total_gt_occupied_area);
            ret.pos_ratio[i] = (float) (fs.true_occupied_area / fs.total_gt_occupied_area);
            ret.neg_ratio[i] = (float) (fs.false_occupied_area / fs.total_gt_occupied_area);
        }
        return ret;
    }

    /**
     * Find the limits of the current data and adjust display to zoom in on only
     * that area.
     */
    public void Fit() {
        double limits[] = this.drawPanel1.fit();
        if (null == limits) {
            return;
        }
        JViewport vp = this.jScrollPaneDrawPanel.getViewport();
        Rectangle rect = vp.getViewRect();
        vp.setPreferredSize(new Dimension(rect.width, rect.height));
        this.jSliderZoom.setValue(this.jSliderZoom.getMinimum());
        this.updateDrawPanelViewport();
    }
    public static double outer_min_time = Double.POSITIVE_INFINITY;
    public static double outer_max_time = Double.NEGATIVE_INFINITY;
    public static double inner_min_time = Double.POSITIVE_INFINITY;
    public static double inner_max_time = Double.NEGATIVE_INFINITY;
    public static double gt_min_time = Double.POSITIVE_INFINITY;
    public static double gt_max_time = Double.NEGATIVE_INFINITY;
    public static double sut_min_time = Double.POSITIVE_INFINITY;
    public static double sut_max_time = Double.NEGATIVE_INFINITY;
    /**
     * List of ground-truth tracks.
     */
    public static List<Track> gtlist;
    /**
     * List of system-under-test tracks.
     */
    public static List<Track> sutlist;
    static HTPM_JFrame main_frame = null;
    static boolean bad_time_warning_given = false;

    public static void AddTrackPointToTracks(List<Track> tracks,
            TrackPoint pt,
            boolean is_groundtruth,
            String source,
            Color c,
            boolean live,
            int line_num,
            boolean source_has_vel_info,
            CsvParseOptions o) {
        Track cur_track = null;
        for (Track t : tracks) {
            if (t.name.compareTo(pt.name) == 0
                    && t.source.compareTo(source) == 0) {
                cur_track = t;
                break;
            }
        }
        if (cur_track == null) {
//            Thread.dumpStack();
//            System.out.println("creating new track "+pt.name + " from source ="+source);
            cur_track = new Track();
            cur_track.is_groundtruth = is_groundtruth;
            cur_track.name = pt.name;
            if (pt.name.length() < 1 || pt.name.indexOf("unaffiliated") > 0) {
                cur_track.disconnected = true;
            }
            cur_track.color = c;
            cur_track.source = source;
            tracks.add(cur_track);
//            if (is_groundtruth) {
//                if (null == gtlist) {
//                    gtlist = new ArrayList<Track>();
//                }
//                gtlist.add(cur_track);
//            } else {
//                if (null == sutlist) {
//                    sutlist = new ArrayList<Track>();
//                }
//                sutlist.add(cur_track);
//            }
            if (main_frame != null && live) {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (main_frame != null) {
                            main_frame.updateTree(true);
                            main_frame.drawPanel1.repaint();
                        }
                    }
                });
            }
        }

        if (null == cur_track.data) {
            cur_track.data = new ArrayList<TrackPoint>();
        }
//        if (is_groundtruth) {
//            if (pt.time > gt_max_time) {
//                gt_max_time = pt.time;
//            }
//            if (pt.time < gt_min_time) {
//                gt_min_time = pt.time;
//            }
//        } else {
//            if (pt.time > sut_max_time) {
//                sut_max_time = pt.time;
//            }
//            if (pt.time < sut_min_time) {
//                sut_min_time = pt.time;
//            }
//        }

        if (cur_track.data.size() > 0) {
            double diff = pt.time - cur_track.data.get(cur_track.data.size() - 1).time;
            if (diff <= 0.0 && pt.name.length() > 0 && !cur_track.disconnected) {
                final String msgS = "New point in track(" + pt.name + ") from "
                        + source + " on line " + line_num + "  has time(=" + pt.time + ") less than or equal to previos point. (diff="
                        + diff + ")";
                if (!bad_time_warning_given) {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            myShowMessageDialog(HTPM_JFrame.main_frame, msgS);
                        }
                    });
                    bad_time_warning_given = true;
                }
                System.out.println(msgS);
            }
        }
        cur_track.data.add(pt);
        if (live) {
            cur_track.cur_time_index = cur_track.data.size() - 1;
            cur_track.currentPoint = pt;
        }
        if (o.transform != null) {
            cur_track.setTransform(o.transform);
        }
        if (!cur_track.source_has_vel_info) {
            cur_track.source_has_vel_info = source_has_vel_info;
        }
    }

    static public double computeGpsTimeOffset(String filename, String delim) {
        double time_offset = Double.NaN;
        BufferedReader br = null;
        PrintStream ps = null;
        final boolean debug = false;
        try {
            br = new BufferedReader(new FileReader(filename));
            br.readLine();
            String line = br.readLine();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a");
            if (debug) {
                System.out.println("Now  = " + sdf.format(new Date()));
            }
            double min_before_offset = Double.POSITIVE_INFINITY;
            double max_before_offset = Double.NEGATIVE_INFINITY;
            double min_after_offset = Double.POSITIVE_INFINITY;
            double max_after_offset = Double.NEGATIVE_INFINITY;
            double first_l1 = 0;
            double first_d2 = 0;
            double last_l1 = 0;
            double last_d2 = 0;
            double first_offset = 0;
            double last_offset = 0;
            int count = 0;
            while (line != null) {
                count++;
                line = line.trim();
                if (line.length() < 1) {
                    line = br.readLine();
                    continue;
                }
                String fields[] = null;
                try {
                    fields = line.split(delim);
                    double l1 = 0.0;
                    if (fields[0].indexOf('/') > 0 || fields[0].indexOf(':') > 0) {
                        Date d = sdf.parse(fields[0]);
                        l1 = d.getTime() * 1e-3;
                    } else {
                        l1 = Double.valueOf(fields[0]);
                    }
                    //System.out.println("l1 = " + l1);
                    double d2 = Double.valueOf(fields[1]);
                    double offset = d2 - l1;
                    if (first_l1 == 0 && first_d2 == 0) {
                        first_l1 = l1;
                        first_d2 = d2;
                        first_offset = offset;
                        last_l1 = l1;
                        last_d2 = d2;
                        last_offset = offset;
                        continue;
                    }
                    offset -= first_offset;
                    if (last_l1 != l1) {
                        double before_offset = last_offset;
                        //System.out.println("before_offset = " + before_offset);
                        double after_offset = offset;
                        //System.out.println("after_offset = " + after_offset);
                        if (before_offset > max_before_offset) {
                            max_before_offset = before_offset;
                            if (debug) {
                                System.out.println("max_before_offset = " + max_before_offset);
                                System.out.println("count = " + count);
                                System.out.println("line = " + line);
                            }
                        }
                        if (before_offset < min_before_offset) {
                            min_before_offset = before_offset;
                            if (debug) {
                                System.out.println("min_before_offset = " + min_before_offset);
                                System.out.println("count = " + count);
                                System.out.println("line = " + line);
                            }
                        }
                        if (after_offset > max_after_offset) {
                            max_after_offset = after_offset;
                            if (debug) {
                                System.out.println("max_after_offset = " + max_after_offset);
                                System.out.println("count = " + count);
                                System.out.println("line = " + line);
                            }
                        }
                        if (after_offset < min_after_offset) {
                            min_after_offset = after_offset;
                            if (debug) {
                                System.out.println("min_after_offset = " + min_after_offset);

                                System.out.println("count = " + count);
                                System.out.println("line = " + line);
                            }
                        }
                    }

                    last_l1 = l1;
                    last_d2 = d2;
                    last_offset = offset;
                } catch (Exception e) {
                    System.err.println("line=" + line);
                    if (null != fields) {
                        System.err.println("fields.length=" + fields.length);
                        System.err.println("fields=" + Arrays.deepToString(fields));
                    }
                    e.printStackTrace();
                }
                line = br.readLine();
                // end while loop
            }
            br.close();
            br = null;
            time_offset = (max_before_offset - 1.0 + min_after_offset) / 2 + first_offset;
            if (debug) {
                System.out.println("max_before_offset = " + max_before_offset);
                System.out.println("min_before_offset = " + min_before_offset);
                System.out.println("min_after_offset = " + min_after_offset);
                System.out.println("max_after_offset = " + max_after_offset);
                System.out.println("time_offset = " + time_offset);
                ps = new PrintStream(new FileOutputStream("time_chk.csv"));
            }
            br = new BufferedReader(new FileReader(filename));
            br.readLine();
            if (debug && null != ps) {
                ps.println("t,New Time,Old Time,GpsTime,err,derr");
            }
            line = br.readLine();
            long total_err = 0;
            int pos_errs = 0;
            int neg_errs = 0;
            LinkedList<Double> derrl = new LinkedList<Double>();
            while (line != null) {
                String fields[] = null;
                try {
                    fields = line.split(delim);
                    long l = 0;
                    if (fields[0].indexOf('/') > 0 || fields[0].indexOf(':') > 0) {
                        Date d = sdf.parse(fields[0]);
                        l = d.getTime();
                    } else {
                        l = Double.valueOf(fields[0]).longValue();
                    }
                    double t = Double.valueOf(fields[1]) - time_offset;
                    long e = ((long) t) - l / 1000;
                    double derr = 0.0;
                    if (e > 0) {
                        pos_errs++;
                        derr = t - l / 1000.0 - 0.999;
                        derrl.add(derr);
                    }
                    if (e < 0) {
                        neg_errs++;
                        derr = t - l / 1000.0;
                        derrl.add(derr);
                    }
                    total_err += e;
                    if (debug) {
                        Date d = new Date((long) (t * 1e3));
                        ps.println(t + "," + sdf.format(d) + " + " + (t - Math.floor(t)) + "s ," + fields[0] + "," + fields[1] + "," + e + "," + derr);
                    }
                } catch (Exception e) {
                    System.err.println("line=" + line);
                    if (null != fields) {
                        System.err.println("fields.length=" + fields.length);
                    }
                    e.printStackTrace();
                }
                line = br.readLine();
            }
            if (null != ps) {
                ps.close();
                ps = null;
            }
            br.close();
            br = null;
            if (debug) {
                System.out.println("pos_errs = " + pos_errs);
                System.out.println("neg_errs=" + neg_errs);
                System.out.println("total_err = " + total_err);
            }
            Collections.sort(derrl);
            if (debug) {
                for (Double Derr : derrl) {
                    System.out.print(Derr + ",");
                }
                System.out.println("");
            }
            double mid_derrl = derrl.get(derrl.size() / 2);
            if (debug) {
                System.out.println("mid_derrl = " + mid_derrl);
            }
            time_offset += mid_derrl;
            if (!debug) {
                return -time_offset;
            }
            ps = new PrintStream(new FileOutputStream("time_chk.csv"));
            br = new BufferedReader(new FileReader(filename));
            br.readLine();
            ps.println("t,New Time,Old Time,GpsTime,err,derr");
            line = br.readLine();
            total_err = 0;
            pos_errs = 0;
            neg_errs = 0;
            derrl = new LinkedList<Double>();
            while (line != null) {
                String fields[] = line.split(delim);
                long l = sdf.parse(fields[0]).getTime();
                double t = Double.valueOf(fields[1]) - time_offset;
                long e = ((long) t) - l / 1000;
                double derr = 0.0;
                if (e > 0) {
                    pos_errs++;
                    derr = t - l / 1000.0 - 0.999;
                    derrl.add(derr);
                }
                if (e < 0) {
                    neg_errs++;
                    derr = t - l / 1000.0;
                    derrl.add(derr);
                }
                total_err += e;
                Date d = new Date((long) (t * 1e3));
                ps.println(t + "," + sdf.format(d) + " + " + (t - Math.floor(t)) + "s ," + fields[0] + "," + fields[1] + "," + e + "," + derr);
                line = br.readLine();
            }
            ps.close();
            ps = null;
            br.close();
            br = null;
            System.out.println("pos_errs = " + pos_errs);
            System.out.println("neg_errs=" + neg_errs);
            System.out.println("total_err = " + total_err);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (Exception e) {
                }
                br = null;
            }
            if (null != ps) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
                ps = null;
            }
        }
        return -time_offset;
    }

    /**
     * Read a csv file and return the parsed contents in a list of tracks.
     *
     * @param filename name of file to read
     * @param c default color for displaying these tracks.
     * @param is_groundtruth Should this file be considered ground-truth data?
     * @return
     */
    static public ArrayList<Track> LoadFile(String filename,
            Color c,
            boolean is_groundtruth,
            CsvParseOptions o) {
        ArrayList<Track> tracks = null;
        try {
            tracks = new ArrayList<Track>();
            BufferedReader br = new BufferedReader(new FileReader(filename));
            br.readLine(); // skip line with headings
            int line_num = 2;
            String line = br.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() < 1) {
                    line = br.readLine();
                    continue;
                }
                try {
                    TrackPoint pt = parseTrackPointLine(line, o);
                    if (null != pt) {
                        HTPM_JFrame.AddTrackPointToTracks(tracks,
                                pt,
                                is_groundtruth, filename, c, false,
                                line_num,
                                (o.VX_INDEX > 0 || o.VY_INDEX > 0),
                                o);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line " + line_num + " from " + filename);
                    System.err.println(line);
                    e.printStackTrace();
                }
                line = br.readLine();
                line_num++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tracks;
    }

    public static void recomputeTimeLimits() {
        try {
            gt_min_time = Double.POSITIVE_INFINITY;
            gt_max_time = Double.NEGATIVE_INFINITY;
            sut_min_time = Double.POSITIVE_INFINITY;
            sut_max_time = Double.NEGATIVE_INFINITY;
            if (null != gtlist) {
                for (Track t : gtlist) {
                    if (null == t.data || t.data.size() < 1) {
                        continue;
                    }
                    Collections.sort(t.data, new Comparator<TrackPoint>() {
                        @Override
                        public int compare(TrackPoint o1, TrackPoint o2) {
                            return (int) (1000.0 * (o1.time - o2.time));
                        }
                    });

                    if (t.data.get(0).time > gt_max_time) {
                        gt_max_time = t.data.get(0).time;
                    }
                    if (t.data.get(t.data.size() - 1).time > gt_max_time) {
                        gt_max_time = t.data.get(t.data.size() - 1).time;
                    }
                    if (t.data.get(0).time < gt_min_time) {
                        gt_min_time = t.data.get(0).time;
                    }
                    if (t.data.get(t.data.size() - 1).time < gt_min_time) {
                        gt_min_time = t.data.get(t.data.size() - 1).time;
                    }
                }
            }
            if (null != sutlist) {
                for (Track t : sutlist) {
                    if (null == t.data || t.data.size() < 1) {
                        continue;
                    }
                    Collections.sort(t.data, new Comparator<TrackPoint>() {
                        @Override
                        public int compare(TrackPoint o1, TrackPoint o2) {
                            return (int) (1000.0 * (o1.time - o2.time));
                        }
                    });

                    if (t.data.get(0).time > sut_max_time) {
                        sut_max_time = t.data.get(0).time;
                    }
                    if (t.data.get(t.data.size() - 1).time > sut_max_time) {
                        sut_max_time = t.data.get(t.data.size() - 1).time;
                    }
                    if (t.data.get(0).time < sut_min_time) {
                        sut_min_time = t.data.get(0).time;
                    }
                    if (t.data.get(t.data.size() - 1).time < sut_min_time) {
                        sut_min_time = t.data.get(t.data.size() - 1).time;
                    }
                }
            }
            inner_min_time = Math.max(gt_min_time, sut_min_time);
            inner_max_time = Math.min(gt_max_time, sut_max_time);
            outer_min_time = Math.min(gt_min_time, sut_min_time);
            outer_max_time = Math.max(gt_max_time, sut_max_time);
            if (gt_min_time >= gt_max_time || sut_min_time >= sut_max_time) {
                inner_min_time = outer_min_time;
                inner_max_time = outer_max_time;
            }
        } catch (ConcurrentModificationException cme) {
            cme.printStackTrace();
            final Map<Thread, StackTraceElement[]> thread_map = Thread.getAllStackTraces();
            final Set<Thread> threads = thread_map.keySet();
            for (final Thread t : threads) {
                StackTraceElement sta[] = thread_map.get(t);
                System.err.println("");
                System.err.println("Thread : " + t.getName());
                for (StackTraceElement ste : sta) {
                    System.err.println("\t" + ste.toString());
                }
            }
        }
    }

    /**
     * Generate random data csv files for ground-truth ("gt.csv") and a
     * corresponding system under test ("sut.csv").
     */
    public static void GenRandomData() {
        class GTHumanState {

            double x, y, speed, yaw;
            int id;
            int sut_id[];
            double radius;
        }
        try {
            int next_id = 1;
            File gtfile = new File("gt.csv");
            PrintStream ps_gt = new PrintStream(new FileOutputStream(gtfile));
            File sutfile = new File("sut.csv");
            PrintStream ps_sut = new PrintStream(new FileOutputStream(sutfile));
            printCsvHeader(ps_gt);
            printCsvHeader(ps_sut);
            LinkedList<GTHumanState> generated_gtlist = new LinkedList<GTHumanState>();
            Random r = new Random();
            ArrayList<Integer> sutIdList = new ArrayList<Integer>();
            for (int i = 0; i < 500; i++) {
                sutIdList.add(i);
            }
            Collections.shuffle(sutIdList);
            int num_humans_to_start =
                    Integer.valueOf(JOptionPane.showInputDialog("Number of humans to start", (r.nextInt(3) + 1)));
            int max_humans =
                    Integer.valueOf(JOptionPane.showInputDialog("Maximum number of humans", 10));
            for (int i = 0; i < num_humans_to_start; i++) {
                GTHumanState h = new GTHumanState();
                h.x = r.nextDouble() * 3.0 + 5.0;
                h.y = r.nextDouble() * 3.0 + 5.0;
                h.speed = r.nextDouble() * 2.0;
                h.yaw = r.nextDouble() * 2 * Math.PI;
                h.id = next_id;
                h.radius = r.nextDouble() * 0.5 + 0.35;
                if (true) {
                    h.sut_id = new int[r.nextInt(5) + 1];
                    for (int j = 0; j < h.sut_id.length; j++) {
                        h.sut_id[j] = sutIdList.get((h.id + j) % sutIdList.size());
                        next_id++;
                    }
                }
                next_id++;
                generated_gtlist.add(h);
            }
            double gt_time = new Date().getTime();
            double last_gt_write_time = 0.0;
            double last_sut_write_time = 0.0;
            double sim_cycle_time = 0.001;
            double gt_cycle_time = 0.005;
            double sut_cycle_time = 0.033;
            double sut_time = gt_time + r.nextDouble() * 0.01 - 0.005;
            int cycles = 0;
            while (cycles < 10000) {
                cycles++;
                gt_time += sim_cycle_time;
                sut_time += Math.max(sim_cycle_time * .5,
                        sim_cycle_time + r.nextGaussian() * 0.001 * sim_cycle_time);
                if (generated_gtlist.size() < 1
                        || (generated_gtlist.size() < max_humans && r.nextDouble() < 0.005 * (10 - generated_gtlist.size()))) {
                    GTHumanState h = new GTHumanState();
                    h.x = r.nextDouble() * 3.0 + 5.0;
                    h.y = r.nextDouble() * 3.0 + 5.0;
                    if (r.nextBoolean()) {
                        if (r.nextBoolean()) {
                            h.y = 0;
                            h.yaw = r.nextDouble() * Math.PI;
                        } else {
                            h.y = 10;
                            h.yaw = -r.nextDouble() * Math.PI;
                        }
                    } else {
                        if (r.nextBoolean()) {
                            h.x = 0;
                            h.yaw = r.nextDouble() * Math.PI - Math.PI / 2;
                        } else {
                            h.x = 10;
                            h.yaw = r.nextDouble() * Math.PI + Math.PI / 2;
                        }
                    }
                    h.speed = r.nextDouble() * 2.0;
                    h.radius = r.nextDouble() * 0.5 + 0.35;
                    h.id = next_id;
                    if (true) {
                        h.sut_id = new int[r.nextInt(5) + 1];
                        for (int j = 0; j < h.sut_id.length; j++) {
                            h.sut_id[j] = sutIdList.get((h.id + j) % sutIdList.size());
                            next_id++;
                        }
                    }
                    next_id++;
                    generated_gtlist.add(h);
                }
                for (int j = 0; j < generated_gtlist.size(); j++) {
                    GTHumanState h = generated_gtlist.get(j);
                    double vx = h.speed * Math.cos(h.yaw);
                    double vy = h.speed * Math.sin(h.yaw);
                    h.yaw += r.nextGaussian() * Math.PI / 72.0; // 5 degrees random change in heading
                    h.speed += r.nextGaussian() * 0.0025;
                    if (h.speed > 2.0) {
                        h.speed = 2.0;
                    }
                    if (h.speed < 0.0) {
                        h.speed = 0.0;
                    }
                    h.x += vx * sim_cycle_time;
                    h.y += vy * sim_cycle_time;
                    h.radius += r.nextGaussian() * 0.01;
                    if (h.radius < 0.2) {
                        h.radius = 0.2;
                    }
                    if (h.x < -1.0 || h.x > 11.0 || h.y < -1.0 || h.y > 11.0) {
                        generated_gtlist.remove(j);
                    }
                }
                if (gt_time - last_gt_write_time > gt_cycle_time) {
                    for (int j = 0; j < generated_gtlist.size(); j++) {
                        GTHumanState h = generated_gtlist.get(j);
                        double vx = h.speed * Math.cos(h.yaw);
                        double vy = h.speed * Math.sin(h.yaw);
                        TrackPoint tp = new TrackPoint();
                        tp.time = gt_time;
                        tp.x = (float) h.x;
                        tp.y = (float) h.y;
                        tp.vel_x = (float) vx;
                        tp.vel_y = (float) vy;
                        tp.radius = h.radius;
                        tp.confidence = 1.0;
                        printOneLine(tp, Integer.toString(h.id), ps_gt);
                        //ps_gt.println("" + gt_time + "," + h.id + "," + h.x + "," + h.y + ",0.0," + vx + "," + vy + ",0.0,1.0," + h.radius);
                    }
                    last_gt_write_time = gt_time;
                }
                if (sut_time - last_sut_write_time > sut_cycle_time) {
                    for (int j = 0; j < generated_gtlist.size(); j++) {
                        GTHumanState h = generated_gtlist.get(j);
                        if (null != h.sut_id) {
                            double vx = h.speed * Math.cos(h.yaw);
                            double vy = h.speed * Math.sin(h.yaw);
                            for (int k = 0; k < h.sut_id.length; k++) {
                                double sutx = (h.x + r.nextGaussian() * 0.10);
                                double suty = (h.y + r.nextGaussian() * 0.10);
                                double sutvx = (vx + r.nextGaussian() * 0.1);
                                double sutvy = (vy + r.nextGaussian() * 0.1);
                                double confidence = (0.99 * r.nextDouble());
                                TrackPoint tp = new TrackPoint();
                                tp.time = sut_time;
                                tp.x = (float) sutx;
                                tp.y = (float) suty;
                                tp.vel_x = (float) sutvx;
                                tp.vel_y = (float) sutvy;
                                tp.radius = (h.radius + r.nextGaussian() * 0.1);
                                if (tp.radius < 0.1) {
                                    tp.radius = 0.1;
                                }
                                tp.confidence = confidence;
                                printOneLine(tp, Integer.toString(h.sut_id[k]), ps_sut);
                                //ps_sut.println("" + sut_time + "," +  + "," + sutx + "," + suty + ",0.0," + sutvx + "," + sutvy + ",0.0," + confidence + "," + (h.radius + r.nextGaussian() * 0.1));
                            }
                        }
                        last_sut_write_time = sut_time;
                    }
                }
            }
            ps_sut.close();
            ps_gt.close();
            gtlist = LoadFile(gtfile.getCanonicalPath(), Color.red, true, CsvParseOptions.DEFAULT);
            sutlist = LoadFile(sutfile.getCanonicalPath(),
                    Color.blue, false, CsvParseOptions.DEFAULT);
            //sutlist = LoadFile("sut.csv", Color.blue, false);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    static private ProgressMonitor progressMonitor = null;
    static ProcessDataTask processDataTask = null;
    static public RocData last_roc_data = null;

    class ProcessDataTask extends SwingWorker<Void, Void> {

        public Runnable run_when_done = null;

        public void setTime(double time, double min, double max) {
            if (max <= min) {
                return;
            }
            if (time <= min) {
                setProgress(0);
                return;
            }
            if (time >= max) {
                setProgress(100);
                return;
            }
            int progress = (int) (100.0 * (time - min) / (max - min));
            setProgress(progress);
        }

        @Override
        public Void doInBackground() {
            setProgress(0);
            try {
                HTPM_JFrame.processAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
            progressMonitor.close();
            if (null != run_when_done) {
                run_when_done.run();
            }
        }
    }

    public void startProcessTask(Runnable _run_when_done) {
        progressMonitor = new ProgressMonitor(this,
                "Computing Statistics",
                "", 0, 100);
        progressMonitor.setProgress(0);
        processDataTask = new ProcessDataTask();
        processDataTask.run_when_done = _run_when_done;
        processDataTask.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ProcessDataTask task = HTPM_JFrame.processDataTask;
                if ("progress" == evt.getPropertyName()) {
                    int progress = (Integer) evt.getNewValue();
                    progressMonitor.setProgress(progress);
                    String message =
                            String.format("Completed %d%%.\n", progress);
                    progressMonitor.setNote(message);
                    if (progressMonitor.isCanceled() || task.isDone()) {
                        if (progressMonitor.isCanceled()) {
                            PlayBeep();
                            task.cancel(true);
                        }
                    }
                }
            }
        });
        processDataTask.execute();
    }
    /**
     * Instance of the rocTask. Only one instance of this class should need to
     * exist at a time.
     */
    public static RocTask rocTask = null;

    /**
     * Internal class wraps the SwingWorker so the ROC Computations can be done
     * in the background and monitored or canceled by the user.
     */
    class RocTask extends SwingWorker<Void, Void> {

        public Runnable run_when_done = null;
        public int conf_index = 0;

        public void set_conf_index(int _conf_index) {
            this.conf_index = _conf_index;
            this.conf_index_progress(this.conf_index);
            setProgress(conf_index_progress(this.conf_index));
        }

        private int conf_index_progress(int ci) {
            if (ci < 1) {
                return 0;
            }
            if (ci >= HTPM_JFrame.num_roc_thresholds) {
                return 100;
            }
            int progress = (int) (100.0 * ci / HTPM_JFrame.num_roc_thresholds);
            return progress;
        }

        public void setTime(double time, double min, double max) {
            if (max <= min) {
                return;
            }
            if (time <= min) {
                setProgress(this.conf_index_progress(this.conf_index));
                return;
            }
            if (time >= max) {
                setProgress(this.conf_index_progress(this.conf_index + 1));
                return;
            }
            int progress = this.conf_index_progress(this.conf_index);
            int progress_inc = (int) (100.0 / HTPM_JFrame.num_roc_thresholds * (time - min) / (max - min));
            progress += progress_inc;
            setProgress(progress);
        }

        @Override
        public Void doInBackground() {
            setProgress(0);
            try {
                HTPM_JFrame.last_roc_data = HTPM_JFrame.computeRoc();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void done() {
            PlayBeep();
            progressMonitor.close();
            if (null != run_when_done) {
                run_when_done.run();
            }
        }
    }

    public void startRocTask(Runnable _run_when_done) {
        progressMonitor = new ProgressMonitor(this,
                "Computing ROC Statistics",
                "", 0, 100);
        progressMonitor.setProgress(0);
        rocTask = new RocTask();
        rocTask.run_when_done = _run_when_done;
        rocTask.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                RocTask task = HTPM_JFrame.rocTask;
                if ("progress" == evt.getPropertyName()) {
                    int progress = (Integer) evt.getNewValue();
                    progressMonitor.setProgress(progress);
                    String message =
                            String.format("Completed %d%%.\n", progress);
                    progressMonitor.setNote(message);
                    if (progressMonitor.isCanceled() || task.isDone()) {
                        PlayAlert();
                        if (progressMonitor.isCanceled()) {
                            task.cancel(true);
                        }
                    }
                }
            }
        });
        rocTask.execute();
    }

    /**
     * main function This is not the main for the jar. That is in
     * HumanTrackingPerformanceMetrics
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(HTPM_JFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(HTPM_JFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(HTPM_JFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(HTPM_JFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                main_frame = new HTPM_JFrame();
                main_frame.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupDragMode;
    private javax.swing.ButtonGroup buttonGroupMode;
    private humantrackingperformancemetrics.DrawPanel drawPanel1;
    private javax.swing.JButton jButtonLogSingleFrame;
    private javax.swing.JButton jButtonNewLogFile;
    private javax.swing.JButton jButtonStatsDialogOk;
    private javax.swing.JCheckBox jCheckBoxLive;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemAcceptGT;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemAcceptSutData;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemBackgroundGray;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemDebug;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemGrayTracks;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemGtOnTop;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemIgnoreSUTVelocities;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemOptitrack;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPlay;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPlayAndMakeMovie;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemPromptLogData;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemRecordLive;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemRepositionBackground;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemSaveImages;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowBackground;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowDisconnected;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowLabels;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemShowOnlySelected;
    private javax.swing.JDialog jDialog1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabelTime;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenu jMenu8;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuConnections;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2DRegistration;
    private javax.swing.JMenuItem jMenuItemChangeSourceLabel;
    private javax.swing.JMenuItem jMenuItemClearData;
    private javax.swing.JMenuItem jMenuItemComputeMinSUTRadius;
    private javax.swing.JMenuItem jMenuItemComputeTimeOffset;
    private javax.swing.JMenuItem jMenuItemConnectGTServer;
    private javax.swing.JMenuItem jMenuItemConnectSUTServer;
    private javax.swing.JMenuItem jMenuItemDefaultColors;
    private javax.swing.JMenuItem jMenuItemEditTimeProj;
    private javax.swing.JMenuItem jMenuItemFit;
    private javax.swing.JMenuItem jMenuItemGenRandom;
    private javax.swing.JMenuItem jMenuItemGotoPlotterMinTime;
    private javax.swing.JMenuItem jMenuItemGotoTime;
    private javax.swing.JMenuItem jMenuItemHelpOverview;
    private javax.swing.JMenuItem jMenuItemHideAllTracks;
    private javax.swing.JMenuItem jMenuItemHideSelectedTracks;
    private javax.swing.JMenuItem jMenuItemManageLiveConnections;
    private javax.swing.JMenuItem jMenuItemOpenGroundTruth;
    private javax.swing.JMenuItem jMenuItemOpenSettings;
    private javax.swing.JMenuItem jMenuItemOpenSutCsv;
    private javax.swing.JMenuItem jMenuItemROI;
    private javax.swing.JMenuItem jMenuItemRandomColors;
    private javax.swing.JMenuItem jMenuItemResetSettings;
    private javax.swing.JMenuItem jMenuItemSaveGroundTruth;
    private javax.swing.JMenuItem jMenuItemSaveImages;
    private javax.swing.JMenuItem jMenuItemSaveSettings;
    private javax.swing.JMenuItem jMenuItemSaveSut;
    private javax.swing.JMenuItem jMenuItemSetSelectedTracksColor;
    private javax.swing.JMenuItem jMenuItemSetTrackTailHighlightTime;
    private javax.swing.JMenuItem jMenuItemShowAllTracks;
    private javax.swing.JMenuItem jMenuItemShowComputedVelocities;
    private javax.swing.JMenuItem jMenuItemShowROCCurve;
    private javax.swing.JMenuItem jMenuItemShowSelectedTracks;
    private javax.swing.JMenuItem jMenuItemShowStatVTime;
    private javax.swing.JMenuItem jMenuItemShowTimeLocal;
    private javax.swing.JMenuItem jMenuItemShowVelocities;
    private javax.swing.JMenuItem jMenuItemTransformSelected;
    private javax.swing.JMenu jMenuMode;
    private javax.swing.JMenu jMenuRecentGroundTruthCsv;
    private javax.swing.JMenu jMenuRecentSystemUnderTestCsv;
    private javax.swing.JMenu jMenuView;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JRadioButton jRadioButtonMeasure;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemModeFalseClear;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemModeFalseOccupied;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemModeGTOccupied;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemModeNormal;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemModeSUTOccupied;
    private javax.swing.JRadioButton jRadioButtonPan;
    private javax.swing.JRadioButton jRadioButtonSelectTracks;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneDrawPanel;
    private javax.swing.JScrollPane jScrollPaneTree;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JSlider jSliderConfidence;
    private javax.swing.JSlider jSliderTime;
    private javax.swing.JSlider jSliderZoom;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextArea jTextAreaStats;
    private javax.swing.JTextField jTextFieldGTRadius;
    private javax.swing.JTextField jTextFieldGrid;
    private javax.swing.JTextField jTextFieldSUTRadius;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables
}
