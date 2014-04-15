/*
 * DrawPanel
 * 
 * This class implements the low-level drawing functions for the the data 
 * displayed in the drawpanel component of the UI.
 */
package humantrackingperformancemetrics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.color.ColorSpace;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Class responsible for the widget where tracks, data points etc are drawn. The
 * widget is expected to be embedded in a JScrollPane.
 *
 * @author Will Shackleford<shackle@nist.gov>
 */
public class DrawPanel extends JPanel {

    /**
     * Called whenever panel needs to be repainted. Implement drawing functions
     * here.
     *
     * @param g Graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension d = this.getPreferredSize();
        paintData(g, d, this.use_sub_images_for_background);
    }
    public boolean show_false_occupied_area = false;
    public boolean show_false_clear_area = false;
    public boolean show_GT_occupied_area = false;
    public boolean show_SUT_occupied_area = false;
    /**
     * Points with an associated confidence below the confidence_threshold may
     * be drawn differently or ignored.
     */
    public double confidence_threshold = 0.0;
    /**
     * Reference to wrapper class for creating movies.
     */
    public HtpmMovieWriter htpm_mw = null;
    /**
     * Name of next or last movie created.
     */
    public String movie_filename = "foo.avi";

    public static BufferedImage getResourceImage(String resource_name) {
        try {
            URL url = DrawPanel.class.getResource(resource_name);
            return ImageIO.read(url);
        } catch (IOException ex) {
            Logger.getLogger(DrawPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static BufferedImage toGray(BufferedImage img_in) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        return op.filter(img_in, null);
    }
    public BufferedImage backgroundImageColor = getResourceImage("/htpm_resources/ISAT_Overhead.bmp");
    public BufferedImage backgroundImageGray = toGray(backgroundImageColor);
    public BufferedImage backgroundImage = backgroundImageColor;
    public BufferedImage subBackgroundImage = backgroundImage;
    public int scaled_bg_width = backgroundImage.getWidth();
    public int scaled_bg_height = backgroundImage.getHeight();
    public int sub_scaled_bg_width = subBackgroundImage.getWidth();
    public int sub_scaled_bg_height = subBackgroundImage.getHeight();
    public int sub_bg_width = backgroundImage.getWidth();
    public int sub_bg_height = backgroundImage.getHeight();
    public int sub_bg_x = 0;
    public int sub_bg_y = 0;
    public Image scaledBackgroundImage = null;
    public double scaledBackgroundImageScale = -1.0;
    public boolean show_background_image = false;
    public double background_image_x = 0.0;
    public double background_image_y = 10.0;
    public double background_image_scale_pixels_per_m = 1000.0;

    /**
     * Change the movie name to the next movie that does not already exist.
     *
     * @return name of movie to save to.
     * @throws Exception
     */
    public String next_movie() throws Exception {
        File f = new File(movie_filename);
        String fname = f.getCanonicalPath();
        String base = fname.substring(0, fname.length() - 4);
        while (Character.isDigit(base.charAt(base.length() - 1))
                || base.charAt(base.length() - 1) == '_') {
            base = base.substring(0, base.length() - 1);
        }
        String ext = fname.substring(fname.length() - 4);
        int count = 0;
        while (f.exists()) {
            closeMovie();
            count++;
            fname = base + "_" + count + ext;
            f = new File(fname);
        }
        return movie_filename = fname;
    }

    /**
     * Start writer for creating movie with movie_filename.
     *
     * @return true if everything seems to be ok. false if there was an error.
     */
    public boolean openMovie(int _framesPerSecond) {
        if (htpm_mw == null) {
            htpm_mw = new HtpmMovieWriter(movie_filename, 640, 480, _framesPerSecond);
        }
        return htpm_mw.isOk();
    }
    /**
     * Maximum number of frames to put in each movie file.
     */
    public static int max_frame_count = 300;
    /**
     * Show only tracks with the selected field set.
     */
    public boolean show_only_selected = false;
    /**
     * Show the labels for all tracks.
     */
    public boolean show_labels = false;
    public boolean use_sub_images_for_background = true;

    /**
     * Draw the current display to an image.
     *
     * @param _w width
     * @param _h height
     * @param _image_type type of image (eg. BufferedImage.TYPE_3BYTE_BGR,...)
     * @return
     */
    public BufferedImage getImage(int _w, int _h, int _image_type) {
        BufferedImage bi = new BufferedImage(_w, _h, _image_type);
        Dimension d = new Dimension(_w, _h);
        Graphics g = bi.getGraphics();
        g.setColor(this.getBackground());
        g.fillRect(0, 0, d.width, d.height);
        paintData(g, d, false);
        bi.flush();
        return bi;
    }
    private int movie_frames_per_second = 30;

    /**
     * Get the value of movie_frames_per_second
     *
     * @return the value of movie_frames_per_second
     */
    public int getMovie_frames_per_second() {
        return movie_frames_per_second;
    }

    /**
     * Set the value of movie_frames_per_second
     *
     * @param movie_frames_per_second new value of movie_frames_per_second
     */
    public void setMovie_frames_per_second(int movie_frames_per_second) {
        this.movie_frames_per_second = movie_frames_per_second;
    }

    /**
     * Add one frame to the open movie file we have been writing to.
     *
     * @return true if everything seems ok, false if there was an error.
     * @throws Exception
     */
    public boolean addMovieFrame() throws Exception {
        if (!openMovie(movie_frames_per_second)) {
            return false;
        }
        if (null == htpm_mw) {
            return false;
        }
        //System.out.println("htpm_mw.frame_count = " + htpm_mw.frame_count);
        if (htpm_mw.frame_count > max_frame_count && max_frame_count > 0) {
            next_movie();
            openMovie(movie_frames_per_second);
        }
        BufferedImage bi = getImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
        htpm_mw.addFrame(bi);
        return htpm_mw.isOk();
    }

    /**
     * Close the movie file if we were writing to one.
     */
    public void closeMovie() {
        if (null != htpm_mw) {
            htpm_mw.close();
            htpm_mw = null;
        }
    }
    private double grid = 1.0;

    /**
     * Get the value of grid, the distance between horizontal and vertical lines
     * drawn on display panel.
     *
     * @return the value of grid
     */
    public double getGrid() {
        return grid;
    }

    /**
     * Set the value of grid, the distance between horizontal and vertical lines
     * drawn on display panel.
     *
     * @param grid new value of grid
     */
    public void setGrid(double grid) {
        this.grid = grid;
        this.repaint();
    }
    public boolean use_gray_tracks = true;
    /**
     * Number of track points after current to highlight.
     */
    private double track_tail_highlight_time = 1.0;

    /**
     * Paint one track( the set of points associated with one
     * trackable,reciever, or person)
     *
     * @param g Graphics context
     * @param d size of the panel (including area not shown by JScrollPane) or
     * the resolution of movie being made.
     * @param t track to paint
     * @param show_gray show gray points (not selected or below confidence)
     * @param show_selected paint selected tracks (selected tracks should be
     * painted later with a separate call)
     */
    public void paintTrack(Graphics g, Dimension d, Track t,
            boolean show_gray,
            boolean show_selected) {
        if (null == t) {
            return;
        }
        if (null == t.data) {
            return;
        }
        if (this.show_only_selected && !t.selected) {
            return;
        }
        if (t.selected != show_selected) {
            return;
        }
        if (t.hidden) {
            return;
        }
        double radius_increase = HTPM_JFrame.s.gt_radius_increase;
        if (!t.is_groundtruth) {
            radius_increase = HTPM_JFrame.s.sut_radius_increase;
        }
        this.update_img_to_world_scale(d);
        if (null != t.color) {
            g.setColor(t.color);
        }
        if (t.currentPoint != null) {
            int oval_size = ((int) ((t.currentPoint.radius + radius_increase) / this.img_to_world_scale));
            TrackPoint pt = t.currentPoint;
            if (pt.confidence >= this.confidence_threshold) {
                Point ipt = this.world2ImgPoint(pt);
                g.fillOval(ipt.x - 10,
                        ipt.y - 10,
                        20,
                        20);
                if (oval_size > 10) {
                    g.drawOval(ipt.x - oval_size,
                            ipt.y - oval_size,
                            oval_size * 2,
                            oval_size * 2);
                }
                if (!t.disconnected
                        && t.cur_time_index >= 0 && t.cur_time_index < t.data.size()) {
                    TrackPoint last_pt = t.data.get(t.cur_time_index);
                    if (null != last_pt) {
                        Point last_ipt = this.world2ImgPoint(last_pt);
                        g.drawLine(ipt.x, ipt.y,
                                last_ipt.x, last_ipt.y);
                    }
                }
            }
        } else {
            if (!show_gray) {
                return;
            }
            if (use_gray_tracks) {
                g.setColor(Color.lightGray);
            } else {
                g.setColor(t.color);
            }
        }
        TrackPoint pt = t.data.get(0);
        Point last_ipt = this.world2ImgPoint(pt);
        for (int i = 0; i < t.data.size(); i++) {
            pt = t.data.get(i);
            Point ipt = this.world2ImgPoint(pt);
            if (pt.confidence >= this.confidence_threshold
                    && t.currentPoint != null
                    && t.cur_time_index >= i
                    && (this.track_tail_highlight_time <= 0
                    || t.currentPoint.time - pt.time <= this.track_tail_highlight_time)) {
                g.setColor(t.color);
                g.fillRect(ipt.x, ipt.y, 3, 3);
            } else {
                if (!show_gray) {
                    return;
                }
                if (use_gray_tracks) {
                    g.setColor(Color.lightGray);
                } else {
                    g.setColor(t.color);
                }
            }
            if (ipt.x == last_ipt.x && ipt.y == last_ipt.y
                    || t.disconnected) {
                g.fillRect(ipt.x, ipt.y, 3, 3);
            }
            if (!t.disconnected) {
                g.drawLine(ipt.x, ipt.y,
                        last_ipt.x, last_ipt.y);
            }
            last_ipt = ipt;
        }
    }
    public boolean show_disconnected = false;

    /**
     * Paint all tracks in list ( the set of points associated with one
     * trackable,receiver, or person)
     *
     * @param g Graphics Context
     * @param d size of panel( including area hidden by JScrollPane) or the
     * resolution of movie being made.
     * @param _tracks list of tracks
     * @param show_gray show gray points (not selected or below confidence)
     * @param show_selected paint selected tracks (selected tracks should be
     * painted later with a separate call)
     */
    public void paintTracks(Graphics g, Dimension d,
            List<Track> _tracks,
            boolean show_gray,
            boolean show_selected) {
        if (null != _tracks) {
            for (int i = 0; i < tracks.size(); i++) {
                Track t = tracks.get(i);
                if (t.disconnected && !show_disconnected) {
                    continue;
                }
                paintTrack(g, d, t, show_gray, show_selected);
            }
        }
    }

    public Track getClosestGTTrack(Point ipt) {
        Point2D.Double pt = this.img2WorldPoint(ipt);
        double min_dist = Double.POSITIVE_INFINITY;
        Track closest_track = null;
        if (null == tracks || null == HTPM_JFrame.gtlist) {
            return null;
        }
        for (Track t : HTPM_JFrame.gtlist) {
            if (t.currentPoint != null) {
                double dist = t.currentPoint.distance(pt);
                if (min_dist > dist) {
                    min_dist = dist;
                    closest_track = t;
                }
            }
        }
        return closest_track;
    }

    public Track getClosestSUTTrack(Point ipt) {
        Point2D.Double pt = this.img2WorldPoint(ipt);
        double min_dist = Double.POSITIVE_INFINITY;
        Track closest_track = null;
        if (null == tracks || null == HTPM_JFrame.sutlist) {
            return null;
        }
        for (Track t : HTPM_JFrame.sutlist) {
            if (t.currentPoint != null) {
                double dist = t.currentPoint.distance(pt);
                if (min_dist > dist) {
                    min_dist = dist;
                    closest_track = t;
                }
            }
        }
        return closest_track;
    }
    private double img_to_world_scale = 1.0;
    Dimension cur_dimension = this.getPreferredSize();

    public void update_img_to_world_scale(Dimension d) {
        this.cur_dimension = d;
        double x_scale = (x_max - x_min) / d.width;
        double y_scale = (y_max - y_min) / d.height;
        this.img_to_world_scale = x_scale;
        if (y_scale > x_scale) {
            this.img_to_world_scale = y_scale;
        }
        //System.out.println("img_to_world_scale = " + img_to_world_scale);
    }

    public Point world2ImgPoint(Point2D.Double wpt) {
        Point ipt = new Point();
        Dimension d = this.cur_dimension;
        ipt.x = (int) ((wpt.x - x_min) / img_to_world_scale);
        ipt.y = d.height - (int) ((wpt.y - y_min) / img_to_world_scale);
        return ipt;
    }

    public Point2D.Double img2WorldPoint(Point ipt) {
        Point2D.Double wpt = new Point2D.Double();
        Dimension d = this.cur_dimension;
        wpt.x = ipt.x * img_to_world_scale + x_min;
        wpt.y = (d.height - ipt.y) * img_to_world_scale + y_min;
        return wpt;
    }

    public Rectangle2D.Double img2WorldRectangle(Rectangle irect) {
        Rectangle2D.Double wrect = new Rectangle2D.Double();
        Dimension d = this.cur_dimension;
        wrect.x = irect.x * img_to_world_scale + x_min;
        wrect.y = (d.height - irect.y) * img_to_world_scale + y_min;
        wrect.width = irect.width * img_to_world_scale;
        wrect.height = irect.height * img_to_world_scale;
        wrect.y -= wrect.height;
        return wrect;
    }

    /**
     * Paint all the data to the panel. This is called by paintComponent() and
     * addMovieFrame()
     *
     * @param g Graphics Context
     * @param d size of the panel or area to paintData to.
     */
    public void paintData(Graphics g, Dimension d, boolean use_sub_images) {
        if (d.height < 1 || d.width < 1) {
            return;
        }
        update_img_to_world_scale(d);
        double x_scale_m_per_pixel = (x_max - x_min) / d.width;
        double y_scale_m_per_pixel = (y_max - y_min) / d.height;
        double scale_m_per_pixel = x_scale_m_per_pixel;
        if (y_scale_m_per_pixel > x_scale_m_per_pixel) {
            scale_m_per_pixel = y_scale_m_per_pixel;
        }
        if (show_false_occupied_area) {
            paintFalseOccupied(g, d, scale_m_per_pixel, x_min, y_min);
            this.drawROIRect(g, d, scale_m_per_pixel);
            return;
        } else if (show_false_clear_area) {
            paintFalseClear(g, d, scale_m_per_pixel, x_min, y_min);
            this.drawROIRect(g, d, scale_m_per_pixel);
            return;
        } else if (show_GT_occupied_area) {
            paintGTOccupied(g, d, scale_m_per_pixel, x_min, y_min);
            this.drawROIRect(g, d, scale_m_per_pixel);
            return;
        } else if (show_SUT_occupied_area) {
            paintSUTOccupied(g, d, scale_m_per_pixel, x_min, y_min);
            this.drawROIRect(g, d, scale_m_per_pixel);
            return;
        }
        if (this.show_background_image && this.backgroundImage != null) {

            // Dividing a distance in meters by as scale in meter/pixel to get pixel units
            int image_pix_x = (int) ((background_image_x - x_min) / scale_m_per_pixel);
            int image_pix_y = (int) ((background_image_y - y_min) / scale_m_per_pixel);

            Point pt = null;
            Dimension vpd = d;
//            if (use_sub_images) {
//                try {
//                    JViewport vp = (JViewport) this.getParent();
//                    pt = vp.getViewPosition();
//                    vpd = vp.getSize();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    // ignore 
//                }
//            }
            double s = this.background_image_scale_pixels_per_m * scale_m_per_pixel;
            scaled_bg_width = (int) (this.backgroundImage.getWidth() / s);
            scaled_bg_height = (int) (this.backgroundImage.getHeight() / s);
            //double oversize = 0.0;
//            if (null != pt) {
//                oversize = Math.min(scaled_bg_width / vpd.width, scaled_bg_height / vpd.height);
//                if (oversize > 4) {
//                    int rescale = ((int) (oversize / 2.0));
//                    int new_sub_bg_width = (int) this.backgroundImage.getWidth() / rescale;
//                    int new_sub_bg_height = (int) this.backgroundImage.getHeight() / rescale;
//                    int x = (int) ((pt.x * scale_m_per_pixel + this.x_min - this.background_image_x)
//                            * this.background_image_scale_pixels_per_m);
//                    if (x < 0) {
//                        x = 0;
//                    }
//                    if (x > this.backgroundImage.getWidth() - 1) {
//                        x = this.backgroundImage.getWidth() - 1;
//                    }
//                    System.out.println("x = " + x);
//                    int new_sub_bg_x = sub_bg_width * (x / sub_bg_width);
//                    int y = (int) ((pt.y * scale_m_per_pixel + this.y_min - this.background_image_y)
//                            * this.background_image_scale_pixels_per_m);
//                    y = -y;
//                    if (y < 0) {
//                        y = 0;
//                    }
//                    if (y > this.backgroundImage.getHeight() - 1) {
//                        y = this.backgroundImage.getHeight() - 1;
//                    }
//                    System.out.println("y = " + y);
//                    int new_sub_bg_y = sub_bg_height * (y / sub_bg_height);
//                    System.out.println("pt = " + pt);
//                    System.out.println("vpd = " + vpd);
//                    System.out.println("new_sub_bg_x = " + new_sub_bg_x);
//                    System.out.println("new_sub_bg_y = " + new_sub_bg_y);
//                    System.out.println("new_sub_bg_width = " + new_sub_bg_width);
//                    System.out.println("new_sub_bg_height = " + new_sub_bg_height);
//                    if (new_sub_bg_width != sub_bg_width
//                            || new_sub_bg_height != sub_bg_height
//                            || new_sub_bg_x != sub_bg_x
//                            || new_sub_bg_y != sub_bg_y) {
//                        sub_bg_x = new_sub_bg_x;
//                        sub_bg_y = new_sub_bg_y;
//                        sub_bg_width = new_sub_bg_width;
//                        sub_bg_height = new_sub_bg_height;
//                        this.subBackgroundImage = this.backgroundImage.getSubimage(sub_bg_x,
//                                sub_bg_y,
//                                sub_bg_width,
//                                sub_bg_height);
//                        this.scaledBackgroundImage = null;
//                    }
//                    image_pix_x += (int) (sub_bg_x / s);
//                    image_pix_y += (int) (sub_bg_y / 2);
//                    System.out.println("image_pix_x = " + image_pix_x);
//                    System.out.println("image_pix_y = " + image_pix_y);
//                } else {
//                    sub_bg_width = this.backgroundImage.getWidth();
//                    sub_bg_height = this.backgroundImage.getHeight();
//                    sub_bg_x = 0;
//                    sub_bg_y = 0;
//                    if (this.subBackgroundImage != this.backgroundImage) {
//                        this.subBackgroundImage = this.backgroundImage;
//                        this.scaledBackgroundImage = null;
//                    }
//                }
//            }
            sub_bg_width = this.backgroundImage.getWidth();
            sub_bg_height = this.backgroundImage.getHeight();
            sub_bg_x = 0;
            sub_bg_y = 0;
            if (null == this.scaledBackgroundImage
                    || scale_m_per_pixel != this.scaledBackgroundImageScale) {
                sub_scaled_bg_width = (int) (sub_bg_width / s);
                sub_scaled_bg_height = (int) (sub_bg_height / s);
                this.scaledBackgroundImage = this.subBackgroundImage.getScaledInstance(sub_scaled_bg_width,
                        sub_scaled_bg_height, Image.SCALE_DEFAULT);
                this.scaledBackgroundImageScale = scale_m_per_pixel;
            }
            g.drawImage(scaledBackgroundImage,
                    image_pix_x,
                    d.height - image_pix_y, null);
        }
        g.setColor(Color.lightGray);
        this.paintGridLabels(g, d, scale_m_per_pixel);
        if (!this.show_only_selected) {
            paintTracks(g, d, tracks, true, false);
            paintTracks(g, d, tracks, false, false);
        }
        paintTracks(g, d, tracks, true, true);
        paintTracks(g, d, tracks, false, true);

        g.setColor(Color.BLACK);
        this.paintGridLines(g, d, scale_m_per_pixel);
        if (this.show_labels) {
            this.paintTrackLabels(g, d);
        }
        this.drawROIRect(g, d, scale_m_per_pixel);
        if (this.showMeasurement) {
        }
        switch (this.dragEnum) {
            case MEASURE:
                Graphics2D g2d = (Graphics2D) g;
                Stroke orig_stroke = g2d.getStroke();
                BasicStroke new_stroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        1f,
                        new float[]{5f, 2f},
                        0.0f);
                g2d.setStroke(new_stroke);
                int max_x = Math.max(this.measure_start_x, this.measure_end_x);
                int min_x = Math.min(this.measure_start_x, this.measure_end_x);
                int max_y = Math.max(this.measure_start_y, this.measure_end_y);
                if (Math.abs(this.measure_start_x - this.measure_end_x) > 10) {
                    g.drawLine(this.measure_start_x, max_y,
                            this.measure_end_x, max_y);
                    g.drawString(String.format("%.3g", this.measure_x_diff),
                            min_x,
                            max_y + 10);
                }
                if (Math.abs(this.measure_end_x - this.measure_end_y) > 10) {
                    g.drawLine(max_x, this.measure_start_y,
                            max_x, this.measure_end_y);
                    g.drawString(String.format("%.3g", this.measure_y_diff),
                            max_x,
                            (this.measure_end_y * 3 / 4 + this.measure_start_y / 4));
                }
                double x_scale = (x_max - x_min) / d.width;
                double y_scale = (y_max - y_min) / d.height;
                double scale = x_scale;
                if (y_scale > x_scale) {
                    scale = y_scale;
                }
                if (Math.abs(this.measure_end_x - this.measure_end_y) > 10
                        || Math.abs(this.measure_end_x - this.measure_end_y) > 10) {
                    g.drawLine(this.measure_start_x, this.measure_start_y,
                            this.measure_end_x, this.measure_end_y);
                    g.drawString(String.format("%.3g", this.measure_diff_mag),
                            (this.measure_start_x + this.measure_end_x) / 2,
                            (this.measure_end_y + this.measure_start_y) / 2);
                    g.drawString(String.format("(%.3g,%.3g)", this.measure_start_x * scale + this.x_min,
                            (d.height - this.measure_start_y) * scale + this.y_min),
                            this.measure_start_x,
                            this.measure_start_y);
                }
                g.drawString(String.format("(%.3g,%.3g)", this.measure_end_x * scale + this.x_min,
                        (d.height - this.measure_end_y) * scale + this.y_min),
                        this.measure_end_x,
                        this.measure_end_y);
                g2d.setStroke(orig_stroke);
                break;

            case SELECT_TRACKS:
                if (null != this.selectRect) {
                    g.setColor(Color.BLACK);
                    g.drawRect(this.selectRect.x, this.selectRect.y,
                            this.selectRect.width, this.selectRect.height);
                }
                break;

            default:
                break;

        }
    }

    /**
     * Paint a circle into the given graphics context at the currentPoint for
     * the given track. Assume the area to be painted starts an 0,0.
     *
     * @param g Graphics Context
     * @param d Dimension of the area to paint
     * @param t track
     * @param scale scale of image in pixels/meter
     * @param is_groundtruth the Track will be ignored if the is_groundtruth
     * parameter does not match t.is_groundtruth
     * @param confidence_threshold the Track will be ignored if the t.confidence
     * less than this parameter. Confidence is an indication of how likely the
     * track represents the object of interest (eg. a human).
     *
     */
    public static void fillTrackCircle(Graphics g,
            Dimension d,
            Track t,
            double scale,
            boolean is_groundtruth,
            double confidence_threshold) {
        fillTrackCircle(g, d, t,
                scale, is_groundtruth, confidence_threshold,
                0.0, 0.0);
    }

    /**
     * Paint a circle into the given graphics context at the currentPoint for
     * the given track. Assume the area to be painted starts an 0,0.
     *
     * @param g Graphics Context
     * @param d Dimension of the area to paint
     * @param t track
     * @param scale scale of image in pixels/meter
     * @param is_groundtruth the Track will be ignored if the is_groundtruth
     * parameter does not match t.is_groundtruth
     * @param confidence_threshold the Track will be ignored if the t.confidence
     * less than this parameter. Confidence is an indication of how likely the
     * track represents the object of interest (eg. a human).
     * @param _x_min x value corresponding to the left side of area of being
     * painted.
     * @param _y_min y value corresponding to the bottom of the area being
     * painted
     *
     */
    public static void fillTrackCircle(Graphics g,
            Dimension d,
            Track t,
            double scale,
            boolean is_groundtruth,
            double confidence_threshold,
            double _x_min,
            double _y_min) {
        if (t.hidden) {
            return;
        }
        TrackPoint pt = t.currentPoint;
        if (null != pt
                && t.is_groundtruth == is_groundtruth
                && pt.confidence >= confidence_threshold) {
            int pix_x = (int) ((pt.x - _x_min) * scale);
            int pix_y = (int) ((pt.y - _y_min) * scale);
            double radius_inc = pt.radius;
            if (is_groundtruth) {
                radius_inc = radius_inc + HTPM_JFrame.s.gt_radius_increase;
            } else {
                radius_inc = radius_inc + HTPM_JFrame.s.sut_radius_increase;
            }
            double radius = t.currentPoint.radius + radius_inc;
            int circle_size = (int) (scale * radius * 2);
            g.fillOval(((int) (pix_x - circle_size / 2)),
                    ((int) (d.height - pix_y - circle_size / 2)),
                    circle_size,
                    circle_size);
        }
    }

    /**
     * Paint a circle into the given graphics context at the currentPoint for
     * all the tracks in the list.
     *
     * @param g Graphics Context
     * @param d Dimension of the area to be painted.
     * @param _scale Scale in meters/pixel
     * @param _lt list of tracks
     * @param _circle_size circle diameter in pixels
     * @param is_groundtruth the Track will be ignored if the is_groundtruth
     * parameter does not match t.is_groundtruth
     * @param confidence_threshold the Track will be ignored if the t.confidence
     * less than this parameter. Confidence is an indication of how likely the
     * track represents the object of interest (eg. a human).
     * @param _x_min x value corresponding to the left side of area of being
     * painted.
     * @param _y_min y value corresponding to the bottom of the area being
     * painted
     */
    public static void fillTrackListCircles(Graphics g,
            Dimension d,
            double _scale,
            List<Track> _lt,
            boolean is_groundtruth,
            double confidence_threshold,
            double _x_min,
            double _y_min) {
        for (Track t : _lt) {
            if (null != t.currentPoint) {
                fillTrackCircle(g, d, t,
                        _scale, is_groundtruth, confidence_threshold,
                        _x_min, _y_min);
            } else if (!t.is_groundtruth) {
                continue;
            }
            if (HTPM_JFrame.s.project_ahead_time > 0) {
                double start_time = t.getCurrentTime();
                double end_time = start_time + HTPM_JFrame.s.project_ahead_time;
                for (double _projected_t = start_time; _projected_t <= end_time;
                        _projected_t += HTPM_JFrame.s.time_inc) {
                    if (t.is_groundtruth) {
                        t.setCurrentTime(_projected_t);
                        if (null == t.currentPoint) {
                            continue;
                        }
                    } else {
                        t.projectAheadToTime(_projected_t);
                        if (null == t.currentPoint) {
                            break;
                        }
                    }
                    fillTrackCircle(g, d, t, _scale,
                            is_groundtruth, confidence_threshold,
                            _x_min, _y_min);
                }
                t.setCurrentTime(start_time);
            }
        }
    }

    /**
     * Paint the scene black except for false occupied regions white.
     *
     * @param g Graphics context
     * @param d Dimension of the area to paint
     * @param inverse_scale scale of drawing in meters/pixel
     * @param _x_min x value in meters of left side of drawing area
     * @param _y_min y value in meters of the bottom side of the drawing area
     */
    public static void paintFalseOccupied(Graphics g,
            Dimension d,
            double inverse_scale,
            double _x_min,
            double _y_min) {
        HTPM_JFrame.settings s = HTPM_JFrame.s;
        double scale = 1 / inverse_scale;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, d.width, d.height);
        g.setColor(Color.WHITE);
        fillTrackListCircles(g, d, scale,
                HTPM_JFrame.sutlist,
                false,
                s.confidence_threshold,
                _x_min, _y_min);
        g.setColor(Color.BLACK);
        fillTrackListCircles(g, d, scale,
                HTPM_JFrame.gtlist,
                true,
                s.confidence_threshold,
                _x_min, _y_min);
    }

    /**
     * Paint the scene black except for false clear regions white.
     *
     * @param g Graphics context
     * @param d Dimension of the area to paint
     * @param inverse_scale scale of drawing in meters/pixel
     * @param _x_min x value in meters of left side of drawing area
     * @param _y_min y value in meters of the bottom side of the drawing area
     */
    public static void paintFalseClear(Graphics g,
            Dimension d,
            double inverse_scale,
            double _x_min,
            double _y_min) {
        HTPM_JFrame.settings s = HTPM_JFrame.s;
        double scale = 1 / inverse_scale;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, d.width, d.height);
        g.setColor(Color.WHITE);
        fillTrackListCircles(g, d, scale,
                HTPM_JFrame.gtlist,
                true,
                s.confidence_threshold,
                _x_min, _y_min);
        g.setColor(Color.BLACK);
        fillTrackListCircles(g, d, scale,
                HTPM_JFrame.sutlist,
                false,
                s.confidence_threshold,
                _x_min, _y_min);
    }

    /**
     * Paint the scene black except for false clear regions white.
     *
     * @param g Graphics context
     * @param d Dimension of the area to paint
     * @param inverse_scale scale of drawing in meters/pixel
     * @param _x_min x value in meters of left side of drawing area
     * @param _y_min y value in meters of the bottom side of the drawing area
     */
    public static void paintGTOccupied(Graphics g,
            Dimension d,
            double inverse_scale,
            double _x_min,
            double _y_min) {
        HTPM_JFrame.settings s = HTPM_JFrame.s;
        double scale = 1 / inverse_scale;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, d.width, d.height);
        g.setColor(Color.WHITE);
        fillTrackListCircles(g, d, scale,
                HTPM_JFrame.gtlist,
                true,
                s.confidence_threshold,
                _x_min, _y_min);
    }

    /**
     * Paint the scene black except for false clear regions white.
     *
     * @param g Graphics context
     * @param d Dimension of the area to paint
     * @param inverse_scale scale of drawing in meters/pixel
     * @param _x_min x value in meters of left side of drawing area
     * @param _y_min y value in meters of the bottom side of the drawing area
     */
    public static void paintSUTOccupied(Graphics g,
            Dimension d,
            double inverse_scale,
            double _x_min,
            double _y_min) {
        HTPM_JFrame.settings s = HTPM_JFrame.s;
        double scale = 1 / inverse_scale;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, d.width, d.height);
        g.setColor(Color.WHITE);
        fillTrackListCircles(g, d, scale,
                HTPM_JFrame.sutlist,
                false,
                s.confidence_threshold,
                _x_min, _y_min);
    }

    /**
     * Paint labels showing the start, end and current location of each track.
     *
     * @param g Graphics Context
     * @param d Dimension of device to paint to
     */
    public void paintTrackLabels(Graphics g, Dimension d) {
        if (d.height < 1 || d.width < 1) {
            return;
        }
        double x_scale = (x_max - x_min) / d.width;
        double y_scale = (y_max - y_min) / d.height;
        double scale = x_scale;
        if (y_scale > x_scale) {
            scale = y_scale;
        }

        for (Track t : tracks) {
            if (this.show_only_selected && !t.selected) {
                continue;
            }
            if (null == t.data || t.data.size() < 1) {
                continue;
            }
            TrackPoint pt = t.data.get(0);
            int pix_x = (int) ((pt.x - x_min) / scale);
            int pix_y = (int) ((pt.y - y_min) / scale);
            g.drawString("Start(" + t.toString() + String.format(")={%.2f,%.2f})",
                    pt.x,
                    pt.y),
                    pix_x,
                    d.height - pix_y);
            pt = t.currentPoint;
            if (null != pt) {
                pix_x = (int) ((pt.x - x_min) / scale);
                pix_y = (int) ((pt.y - y_min) / scale);
                g.drawString("Current(" + t.toString() + String.format(")={%.2f,%.2f})",
                        pt.x,
                        pt.y),
                        pix_x,
                        d.height - pix_y);
            }
            pt = t.data.get(t.data.size() - 1);
            pix_x = (int) ((pt.x - x_min) / scale);
            pix_y = (int) ((pt.y - y_min) / scale);
            g.drawString("End(" + t.toString() + String.format(")={%.2f,%.2f})",
                    pt.x,
                    pt.y),
                    pix_x,
                    d.height - pix_y);
        }
    }

    /**
     * Paint Grid Lines
     *
     * @param g Graphics Context
     * @param d size of display panel (including area hidden by JScrollPane) or
     * the resolution of movie being made.
     * @param scale meters per pixel
     */
    public void paintGridLines(Graphics g, Dimension d, double scale) {
        for (double grid_x = Math.floor(x_min); true; grid_x += grid) {
            int pix_x = (int) ((grid_x - x_min) / scale);
            if (pix_x > 0 && pix_x < d.width) {
                g.drawLine(pix_x, 0, pix_x, d.height);
            }
            if (pix_x > d.width) {
                break;
            }
        }
        for (double grid_y = Math.floor(y_min); true; grid_y += grid) {
            int pix_y = (int) ((grid_y - y_min) / scale);
            if (pix_y > 0 && pix_y < d.height) {
                g.drawLine(0, d.height - pix_y, d.width, d.height - pix_y);
            }
            if (pix_y > d.height) {
                break;
            }
        }
    }

    /**
     * Find the limits of the data to be painted so that the display may be fit
     * to show only the relevant area.
     *
     * @return limits in the order min_x,max_x,min_y,max_y
     */
    public double[] fit() {
        double min_x = Double.POSITIVE_INFINITY;
        double max_x = Double.NEGATIVE_INFINITY;
        double min_y = Double.POSITIVE_INFINITY;
        double max_y = Double.NEGATIVE_INFINITY;
        if (null != tracks) {
            for (int ti = 0; ti < tracks.size(); ti++) {
                Track t = tracks.get(ti);
                if (this.show_only_selected && !t.selected) {
                    continue;
                }
                double radius_increase = HTPM_JFrame.s.gt_radius_increase;
                if (!t.is_groundtruth) {
                    radius_increase = HTPM_JFrame.s.sut_radius_increase;
                }
                if (t.currentPoint != null) {
                    double radius = t.currentPoint.radius + radius_increase;
                    if (max_x < t.currentPoint.x + radius + 0.1) {
                        max_x = t.currentPoint.x + radius + 0.1;
                    }
                    if (min_x > t.currentPoint.x - radius + 0.1) {
                        min_x = t.currentPoint.x - radius + 0.1;
                    }
                    if (max_y < t.currentPoint.y + radius + 0.1) {
                        max_y = t.currentPoint.y + radius + 0.1;
                    }
                    if (min_y > t.currentPoint.y - radius + 0.1) {
                        min_y = t.currentPoint.y - radius + 0.1;
                    }
                }
                if (null != t.data) {
                    for (int tpi = 0; tpi < t.data.size(); tpi++) {
                        TrackPoint tp = t.data.get(tpi);
                        if (max_x < tp.x + 0.1) {
                            max_x = tp.x + 0.1;
                        }
                        if (min_x > tp.x - 0.1) {
                            min_x = tp.x - 0.1;
                        }
                        if (max_y < tp.y + 0.1) {
                            max_y = tp.y + 0.1;
                        }
                        if (min_y > tp.y - 0.1) {
                            min_y = tp.y - 0.1;
                        }
                    }
                }
            }
        }
        if (this.show_background_image) {
            if (max_x < this.background_image_x) {
                max_x = this.background_image_x;
            }
            if (min_x > this.background_image_x) {
                min_x = this.background_image_x;
            }
            if (max_y < this.background_image_y) {
                max_y = this.background_image_y;
            }
            if (min_y > this.background_image_y) {
                min_y = this.background_image_y;
            }
            double bg_image_x_max = this.background_image_x
                    + this.scaled_bg_width * this.scaledBackgroundImageScale;
            double bg_image_y_max = this.background_image_y
                    - this.scaled_bg_height * this.scaledBackgroundImageScale;
            if (max_x < bg_image_x_max) {
                max_x = bg_image_x_max;
            }
            if (min_x > bg_image_x_max) {
                min_x = bg_image_x_max;
            }
            if (max_y < bg_image_y_max) {
                max_y = bg_image_y_max;
            }
            if (min_y > bg_image_y_max) {
                min_y = bg_image_y_max;
            }

        }
        if (Double.isInfinite(min_x) || Double.isNaN(min_x)) {
            return null;
        }
        if (Double.isInfinite(max_x) || Double.isNaN(max_x)) {
            return null;
        }
        if (Double.isInfinite(min_y) || Double.isNaN(min_y)) {
            return null;
        }
        if (Double.isInfinite(max_y) || Double.isNaN(max_y)) {
            return null;
        }
        double limits[] = {min_x, max_x, min_y, max_y};
        this.x_max = grid * (Math.ceil(max_x / grid));
        this.x_min = grid * (Math.floor(min_x / grid));
        this.y_max = grid * (Math.ceil(max_y / grid));
        this.y_min = grid * (Math.floor(min_y / grid));
        return limits;
    }
    public double ROI[] = {0.0, 0.0, 10.0, 10.0};

    public void drawROIRect(Graphics g, Dimension d, double inverse_scale) {
        int pix_x1 = (int) ((ROI[0] - x_min) / inverse_scale);
        int pix_y1 = d.height - (int) ((ROI[1] - y_min) / inverse_scale);
        int pix_x2 = (int) ((ROI[2] - x_min) / inverse_scale);
        int pix_y2 = d.height - (int) ((ROI[3] - y_min) / inverse_scale);
        g.setColor(Color.RED);
        g.drawRect(pix_x1, pix_y2, (pix_x2 - pix_x1), (pix_y1 - pix_y2));
    }
    private Rectangle selectRect;

    /**
     * Get the value of selectRect
     *
     * @return the value of selectRect
     */
    public Rectangle getSelectRect() {
        return selectRect;
    }

    /**
     * Set the value of selectRect
     *
     * @param selectRect new value of selectRect
     */
    public void setSelectRect(Rectangle selectRect) {
        this.selectRect = selectRect;
        this.repaint();
    }

    /**
     * Paint Grid Labels
     *
     * @param g Graphics Context
     * @param d size of display panel (including area hidden by JScrollPane) or
     * the resolution of movie being made.
     * @param scale meters per pixel
     */
    public void paintGridLabels(Graphics g, Dimension d, double scale) {
        int last_pix_x_labeled = -100;
        int last_pix_y_labeled = -100;
        for (double grid_y = Math.floor(y_min); true; grid_y += grid) {
            int pix_y = (int) ((grid_y - y_min) / scale);
            if (pix_y > 0 && pix_y < d.height) {
                for (double grid_x = Math.floor(x_min); true; grid_x += grid) {
                    int pix_x = (int) ((grid_x - x_min) / scale);
                    if (pix_x > 0 && pix_x < d.width) {
                        if (pix_x > last_pix_x_labeled + 100
                                || pix_y > last_pix_y_labeled + 100) {
                            g.drawString(String.format("(%.0f,%.0f)", grid_x, grid_y),
                                    pix_x + 5, (d.height - pix_y) + 20);
                            last_pix_x_labeled = pix_x;
                            last_pix_y_labeled = pix_y;
                        }
                    }
                    if (pix_x > d.width) {
                        break;
                    }
                }
            }
            if (pix_y > d.height) {
                break;
            }
        }
    }
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
    /**
     * List of tracks to paint.
     */
    public List<Track> tracks;

    /**
     * @return the track_tail_highlight_length
     */
    public double getTrack_tail_highlight_time() {
        return track_tail_highlight_time;
    }

    /**
     * @param _track_tail_highlight_time the track_tail_highlight_length to set
     */
    public void setTrack_tail_highlight_time(double _track_tail_highlight_time) {
        this.track_tail_highlight_time = _track_tail_highlight_time;
    }
    private boolean showMeasurement = false;

    public void setShowMeasurement(boolean _showMeasurment) {
        this.showMeasurement = _showMeasurment;
    }
    private double measure_x_diff;
    private double measure_y_diff;
    private double measure_diff_mag;
    private int measure_end_x;
    private int measure_end_y;
    private int measure_start_x;
    private int measure_start_y;

    private void updateMeasurementStats() {
        measure_x_diff = Math.abs(measure_end_x - measure_start_x) * this.img_to_world_scale;
        measure_y_diff = Math.abs(measure_end_y - measure_start_y) * this.img_to_world_scale;
        measure_diff_mag = Math.sqrt(measure_x_diff * measure_x_diff + measure_y_diff * measure_y_diff);
        this.repaint();
    }

    public void setMeasureEnd(int x, int y) {
        measure_end_x = x;
        measure_end_y = y;
        this.updateMeasurementStats();
    }

    public void setMeasureStart(int x, int y) {
        measure_start_x = x;
        measure_start_y = y;
        this.updateMeasurementStats();
    }
    private DrawPanelDragEnum dragEnum = DrawPanelDragEnum.PAN;

    /**
     * Get the value of dragEnum
     *
     * @return the value of dragEnum
     */
    public DrawPanelDragEnum getDragEnum() {
        return dragEnum;
    }

    /**
     * Set the value of dragEnum
     *
     * @param dragEnum new value of dragEnum
     */
    public void setDragEnum(DrawPanelDragEnum dragEnum) {
        this.dragEnum = dragEnum;
        this.showMeasurement = (dragEnum == DrawPanelDragEnum.MEASURE);
    }
}
