/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package humantrackingperformancemetrics;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import javax.swing.JOptionPane;

/**
 * Class which represents a monitored connection to a source of data (by default
 * via TCP socket) of Tracks
 *
 * @author Will Shackleford <shackle@nist.gov>
 */
public class MonitoredConnection implements Runnable {

    Socket socket;
    boolean is_groundtruth;
    String source = "Default";
    private boolean alert_closed = true;
    public volatile long updates = 0;
    long required_updates_per_period = 100;
    boolean use_time_recieved = false;
    boolean monitor_connection = false;
    private long last_monitor_updates = 0;
    private Thread t;
    private java.util.Timer monitor_timer = null;
    private java.util.TimerTask monitor_timer_task = null;
    private PrintStream ps = null;
    private boolean closed = false;
    private int monitor_period = 5000;
    volatile private int update_pending_count = 0;
    volatile private int alert_pending_count = 0;
    public HTPM_JFrame htpm_jframe = null;
    public double transform[];
    public boolean apply_transform=false;
    public String transform_filename;
    
    public MonitoredConnection(final HTPM_JFrame _htpm_jframe) {
        this.htpm_jframe = htpm_jframe;
        init();
    }

    public MonitoredConnection() {
        init();
    }
    
    private void init() {
        if (null == htpm_jframe) {
            this.htpm_jframe = HTPM_JFrame.main_frame;
        }
        if (null != htpm_jframe
                && null != htpm_jframe.default_client) {
            htpm_jframe.setLive(true);
            monitor_connection = htpm_jframe.default_client.monitor_connection;
            monitor_period = htpm_jframe.default_client.monitor_period;
            use_time_recieved = htpm_jframe.default_client.use_time_recieved;
            required_updates_per_period = htpm_jframe.default_client.required_updates_per_period;
            this.apply_transform = htpm_jframe.default_client.apply_transform;
            this.transform = htpm_jframe.default_client.transform;
        }
    }

    public int get_monitor_period() {
        return monitor_period;
    }

    public void set_monitor_period(int _monitor_period) {
        this.monitor_period = _monitor_period;
        restart_monitor();
    }

    public void restart_monitor() {
        if (null != monitor_timer_task) {
            monitor_timer_task.cancel();
            monitor_timer_task = null;
        }
        if (null != monitor_timer) {
            monitor_timer.cancel();
            monitor_timer.purge();
            monitor_timer = null;
        }
        if (!monitor_connection 
                || (null != htpm_jframe && this == htpm_jframe.default_client)) {
            return;
        }
        monitor_timer_task = new java.util.TimerTask() {
            @Override
            public void run() {
                if (closed || !htpm_jframe.live) {
                    return;
                }
                if (null != ps && updates == last_monitor_updates
                        && alert_pending_count == 0) {
                    ps.println("checking connection");
                    if (ps.checkError()) {
                        alert_pending_count++;
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                HTPM_JFrame.PlayAlert();
                                int o = JOptionPane.showConfirmDialog(HTPM_JFrame.main_frame,
                                        "Connection check failed to " + source + " : Keep Monitoring?");
                                if (o != JOptionPane.YES_OPTION) {
                                    monitor_connection = false;
                                    restart_monitor();
                                    htpm_jframe.UpdateConnectionsList();
                                }
                                alert_pending_count--;
                            }
                        });
                        close();
                    }
                }
                if (updates - last_monitor_updates < required_updates_per_period
                        && alert_pending_count == 0) {
                    alert_pending_count++;
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            HTPM_JFrame.PlayAlert();
                            int o = JOptionPane.showConfirmDialog(HTPM_JFrame.main_frame,
                                    "Failed to get sufficient updates from  " + source + " : Keep Monitoring?");
                            if (o != JOptionPane.YES_OPTION) {
                                monitor_connection = false;
                                restart_monitor();
                                htpm_jframe.UpdateConnectionsList();
                            }
                            alert_pending_count--;
                        }
                    });
                }
                last_monitor_updates = updates;
            }
        };
        monitor_timer = new java.util.Timer();
        monitor_timer.schedule(monitor_timer_task, 
                this.monitor_period, 
                this.monitor_period);
    }

    public void start() throws Exception {
        if (this == htpm_jframe.default_client) {
            return;
        }
        t = new Thread(this);
        ps = new PrintStream(socket.getOutputStream());
        t.start();
        this.restart_monitor();
        htpm_jframe.UpdateConnectionsList();
    }

    @Override
    public String toString() {
        return source;
    }

    public void incUpdates() {
        updates++;
        final MonitoredConnection c = this;
        if (update_pending_count == 0) {
            update_pending_count++;
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    htpm_jframe.UpdateConnectionCount(c);
                    update_pending_count--;
                }
            });
        }
    }

    public double TIME_SCALE = 1.0;
    
    @Override
    public void run() {
        String line = "";
        try {
            //System.out.println("this = " + this);
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            int skips = 0;
            int line_num=1;
            while ((line = br.readLine()) != null && !t.isInterrupted() && !closed) {
                line_num++;
                double time_recvd = System.currentTimeMillis() / 1000.0;
                incUpdates();
                line = line.trim();
                if(line.length() < 0) {
                    continue;
                }
                if (line.startsWith("source=")) {
                    this.source = line.substring(7);
                    continue;
                }
                if(!Character.isDigit(line.charAt(0)) &&
                        line.charAt(0) != '.') {
                    continue;
                }
                TrackPoint tp = HTPM_JFrame.parseTrackPointLine(line,
                        new CsvParseOptions());
                if (this.use_time_recieved) {
                    tp.time = time_recvd;
                }
                if(this.apply_transform) {
                    tp.applyTransform(transform);
                }

                if (is_groundtruth) {
                    HTPM_JFrame.AddTrackPointToTracks(htpm_jframe.getTracks(), 
                            tp, true, source, Color.red,
                            htpm_jframe.live,line_num,
                            (line.split(CsvParseOptions.DEFAULT.delim).length > CsvParseOptions.DEFAULT.VX_INDEX),
                            CsvParseOptions.DEFAULT);
                } else {
                    HTPM_JFrame.AddTrackPointToTracks(htpm_jframe.getTracks(), 
                            tp, false, source, Color.blue, 
                            htpm_jframe.live,line_num,
                            (line.split(CsvParseOptions.DEFAULT.delim).length > CsvParseOptions.DEFAULT.VX_INDEX),
                            CsvParseOptions.DEFAULT);
                }
                HTPM_JFrame.inner_min_time = Math.max(HTPM_JFrame.gt_min_time, 
                        HTPM_JFrame.sut_min_time);
                HTPM_JFrame.inner_max_time = Math.min(HTPM_JFrame.gt_max_time, 
                        HTPM_JFrame.sut_max_time);
                HTPM_JFrame.outer_min_time = Math.min(HTPM_JFrame.gt_min_time, 
                        HTPM_JFrame.sut_min_time);
                HTPM_JFrame.outer_max_time = Math.max(HTPM_JFrame.gt_max_time, 
                        HTPM_JFrame.sut_max_time);
                if (HTPM_JFrame.gt_min_time >= HTPM_JFrame.gt_max_time 
                        || HTPM_JFrame.sut_min_time >= HTPM_JFrame.sut_max_time) {
                    HTPM_JFrame.inner_min_time = 
                            HTPM_JFrame.outer_min_time;
                    HTPM_JFrame.inner_max_time = 
                            HTPM_JFrame.outer_max_time;
                }
                if(is.available() < 1 || skips > 10) {
                    if(htpm_jframe.live) {
                        htpm_jframe.panelRepaint();
                    }
                    skips=0;
                } else {
                    skips++;
                }
            }
            br.close();
            is.close();
        } catch (Exception exception) {
            exception.printStackTrace();
            final String msgS = exception.getMessage();
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    HTPM_JFrame.PlayAlert();
                    JOptionPane.showMessageDialog(HTPM_JFrame.main_frame, "Error from " + source + " : " + msgS);
                }
            });
        }
        if (this.alert_closed && this.monitor_connection && line == null) {
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    HTPM_JFrame.PlayAlert();
                    JOptionPane.showMessageDialog(HTPM_JFrame.main_frame, "Closing connection from " + source + " : readLine() returned null.");
                }
            });
        }
        close();
    }

    public void close() {
        try {
            this.alert_closed = false;
            closed = true;
            if (null != t) {
                t.interrupt();
            }
            if (null != socket) {
                socket.close();
                socket = null;
            }
            if (null != this.monitor_timer_task) {
                this.monitor_timer_task.cancel();
                this.monitor_timer_task = null;
            }
            if (null != monitor_timer) {
                monitor_timer.cancel();
                monitor_timer = null;
            }
            if (null != ps) {
                ps.close();
                ps = null;
            }
            if (null != t) {
                if (t != Thread.currentThread()) {
                    t.join(1000);
                }
                t = null;
            }
            if (null != htpm_jframe.sut_connections) {
                htpm_jframe.sut_connections.remove(this);
            }
            if (null != htpm_jframe.gt_connections) {
                htpm_jframe.gt_connections.remove(this);
            }
            htpm_jframe.UpdateConnectionsList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ;
    }
}
