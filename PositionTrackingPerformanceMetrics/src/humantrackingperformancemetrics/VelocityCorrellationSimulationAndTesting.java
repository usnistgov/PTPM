/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package humantrackingperformancemetrics;

import diagapplet.plotter.PlotData;
import diagapplet.plotter.plotterJFrame;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Will Shackleford <shackle@nist.gov>
 */
public class VelocityCorrellationSimulationAndTesting {

    private static final File home_dir = new File(System.getProperty("user.home"));
    private static final File doc_dir = new File(home_dir, "Documents");
    private static final File log_dir = new File(doc_dir, "htpm_logs");

    static public PrintStream OpenPrintStream(String s) throws FileNotFoundException, IOException {
        log_dir.mkdirs();
        File f = new File(log_dir, s);
        //System.out.println("opening "+f.getCanonicalPath());
        return new PrintStream(new FileOutputStream(f));
    }

    static public BufferedReader OpenBufferedReader(String s) throws FileNotFoundException, IOException {
        log_dir.mkdirs();
        File f = new File(log_dir, s);
        //System.out.println("opening "+f.getCanonicalPath());
        return new BufferedReader(new FileReader(f));
    }


    public static Point2D.Double diff(Point2D.Double p1, Point2D.Double p2) {
        return new Point2D.Double(p1.x - p2.x, p1.y - p2.y);
    }

    public static double mag(Point2D.Double p) {
        return Math.sqrt(p.x * p.x + p.y * p.y);
    }

    public static List<Point2DwTime> GenSimulatedGT(Point2D.Double start,
            double amax,
            double vmax,
            double dwell,
            double period,
            List<Point2D.Double> waypoints) throws Exception {
        List<Point2DwTime> l = new LinkedList<Point2DwTime>();
        double v = 0;
        double time = 0;
        Point2D.Double pos = start;
        double dwell_end_time = time + dwell;
        while (time < dwell_end_time) {
            time += period;
            l.add(new Point2DwTime(pos.x, pos.y, time));
        }
        time += period;
        l.add(new Point2DwTime(pos.x, pos.y, time));
        double old_v = v;
        for (Point2D.Double wpt : waypoints) {
            double dist = pos.distance(wpt);
            while (dist > 0.0 && dist > period * period * amax / 2.0) {
                double dleft = dist - amax * period * period / 2.0;
                if (v > Math.sqrt(dleft * amax * 2)) {
                    v -= amax * period;
                    if (v < 0) {
                        v = 0;
                    }
                } else {
                    v += amax * period;
                }
                if (v > vmax) {
                    v = vmax;
                }
                Point2D.Double pos_diff = diff(wpt, pos);
                double pos_diff_mag = mag(pos_diff);
                pos.x += period * (v + old_v) / 2.0 * (pos_diff.x) / pos_diff_mag;
                pos.y += period * (v + old_v) / 2.0 * (pos_diff.y) / pos_diff_mag;
                dist = pos.distance(wpt);
                //System.out.println(time+","+pos.x+","+pos.y+","+v+","+dist+","+(old_v-v)/period);
                time += period;
                l.add(new Point2DwTime(pos.x, pos.y, time));
                if (Math.abs(old_v - v) / period > amax * 1.00000001) {
                    throw new Exception("amax limit exceeded");
                }
                old_v = v;
            }
            dwell_end_time = time + dwell;
            while (time < dwell_end_time) {
                time += period;
                v = old_v = 0;
                l.add(new Point2DwTime(pos.x, pos.y, time));
            }
        }
        return l;
    }

    public static int FindTimeIndexList(int start_index,
            List<Point2DwTime> gt,
            double t) {
        for (int i = start_index; i < gt.size(); i++) {
            if (gt.get(i).time > t) {
                return (i - 1);
            }
        }
        return gt.size();
    }
    static double mean_sut_err = 0.0;
    static double total_sut_err = 0.0;
    static double max_sut_err = 0.0;

    public static List<Point2DwTime> GenSimulatedSUT(
            double time_offset,
            double x_offset,
            double y_offset,
            double yaw_offset,
            double start_time,
            double end_time,
            double period,
            double gauss_pos_err,
            List<Point2DwTime> gt) {
        List<Point2DwTime> l = new LinkedList<Point2DwTime>();
        double v = 0;
        double time = start_time;
        int last_i = 0;
        max_sut_err = 0;
        total_sut_err = 0;
        Point2DwTime gtpos = gt.get(0);
        Point2DwTime gtpos0 = gt.get(0);
        Point2DwTime gtpos1 = gt.get(0);
        while (time < end_time) {
            int i = FindTimeIndexList(last_i, gt, time);
            if (i < 0) {
                gtpos0 = gt.get(0);
                gtpos1 = gt.get(0);
                gtpos = new Point2DwTime(gtpos0.x, gtpos0.y, time);
            } else if (i >= gt.size()) {
                gtpos0 = gt.get(gt.size() - 1);
                gtpos1 = gt.get(gt.size() - 1);
                gtpos = new Point2DwTime(gtpos0.x, gtpos0.y, time);
            } else {
                gtpos0 = gt.get(i);
                gtpos1 = gt.get(i + 1);
                double time_diff = gtpos1.time - gtpos0.time;
                if (time_diff > Double.MIN_NORMAL) {
                    gtpos = new Point2DwTime(0.0, 0.0, time);
                    double s0 = (gtpos1.time - time) / time_diff;
                    double s1 = (time - gtpos0.time) / time_diff;
                    gtpos.x = gtpos0.x * s0 + gtpos1.x * s1;
                    gtpos.y = gtpos0.y * s0 + gtpos1.y * s1;
                }
            }
            last_i = i;
            Point2DwTime sutpos = new Point2DwTime(
                    gtpos.x * Math.cos(yaw_offset) + gtpos.y * Math.sin(yaw_offset),
                    -gtpos.x * Math.sin(yaw_offset) + gtpos.y * Math.cos(yaw_offset),
                    time + time_offset);
            double err_mag = r.nextGaussian() * gauss_pos_err;
            double err_dir = Math.PI * 2 * r.nextDouble();
            double xerr = err_mag * Math.cos(err_dir);
            double yerr = err_mag * Math.sin(err_dir);
            sutpos.x += xerr;
            sutpos.y += yerr;
            double err = Math.sqrt(xerr * xerr + yerr * yerr);
            if (err > max_sut_err) {
                max_sut_err = err;
            }
            total_sut_err += err;
            sutpos.x += x_offset;
            sutpos.y += y_offset;
            l.add(sutpos);
            time += period;
        }
        mean_sut_err = total_sut_err / l.size();
//        System.out.println("gauss_pos_err = " + gauss_pos_err);
//        System.out.println("mean_sut_err = " + mean_sut_err);
        return l;
    }

    public static class VelWTime {

        public VelWTime(double _vel, double _time) {
            this.vel = _vel;
            this.time = _time;
        }
        double vel;
        double time;
    }

    static public List<VelWTime> point2DwTimeListToVelList(List<Point2DwTime> lin) {
        if(lin == null) {
            return null;
        }
        LinkedList<VelWTime> lout = new LinkedList<VelWTime>();
        Point2DwTime last_pt = lin.get(0);
        Point2DwTime pt = lin.get(1);
        double dist_last = pt.distance(last_pt);
        double td_last = pt.time - last_pt.time;
        double vel_last = 0.0;
        if (td_last > 0.0) {
            vel_last = dist_last / td_last;
        }
        for (int i = 2; i < lin.size(); i++) {
            Point2DwTime next_pt = lin.get(i);
            double dist_next = next_pt.distance(pt);
            double td_next = next_pt.time - pt.time;
            double vel_next = vel_last;
            if (td_next > 0.0) {
                vel_next = dist_next / td_next;
            }
            lout.add(new VelWTime((vel_next + vel_last) / 2.0, pt.time));
            last_pt = pt;
            pt = next_pt;
            dist_last = dist_next;
            td_last = td_next;
            vel_last = vel_next;
        }
        return lout;
    }

    public static class VelCorrData {
        public double time;
        public double td;
        public double l1av;
        public double l2av;
    }
    
    public static List<VelCorrData> velCorrList = null;
    
    public static double computeVelCorr(List<VelWTime> l1,
            List<VelWTime> l2,
            double offset) {
        double corr = 0;
        PrintStream ps = null;
        velCorrList = null;
        try {
            final boolean dbg = false;
            if (dbg) {
                ps = OpenPrintStream("vcorr_diffs.csv");
                ps.println("t,diff,max_diff,l1_td,l2_td,l1t-l2t+offset,l1nt-l2nt+offset,l1v,l1nv,l1av,l2v,l2nv,l2av,l1v-l2v,l1nv-l2nv");
            }
            corr = 0;
            int l1i = 1;
            int l2i = 1;
            VelWTime l1_velwtime = l1.get(0);
            VelWTime l2_velwtime = l2.get(0);
            double l1v, l1t, l1nt, l1nv, l1av;
            double l2v, l2t, l2nt, l2nv, l2av;
            l1t = l1_velwtime.time + offset;
            l2t = l2_velwtime.time;
            double last_t = Math.max(l1t, l2t);
            VelWTime next_l1_velwtime = l1.get(1);
            VelWTime next_l2_velwtime = l2.get(1);
            double max_diff = 0.0;
            LinkedList<VelCorrData> newVelCorrList = new LinkedList<VelCorrData>();
            while (l1i < l1.size() - 1 && l2i < l2.size() - 1) {
                double next_t = last_t;
                if (next_l1_velwtime.time + offset < next_l2_velwtime.time) {
                    next_t = next_l1_velwtime.time + offset;
                } else {
                    next_t = next_l2_velwtime.time;
                }
                if (next_t > last_t + 1e-6) {
                    double td = next_t - last_t;
                    double t = (next_t + last_t) / 2.0;
                    l1t = l1_velwtime.time + offset;
                    l1nt = next_l1_velwtime.time + offset;
                    l1v = l1_velwtime.vel;
                    l1nv = next_l1_velwtime.vel;
                    l2t = l2_velwtime.time;
                    l2nt = next_l2_velwtime.time;
                    l2v = l2_velwtime.vel;
                    l2nv = next_l2_velwtime.vel;
                    if (t < l1t) {
                        throw new Exception("bug: ttest1 < l1_velwtime.time");
                    }
                    if (t < l2t) {
                        throw new Exception("bug: ttest2 < l2_velwtime.time");
                    }
                    if (t > l1nt) {
                        throw new Exception("bug: ttest1 > next_l1_velwtime.time");
                    }
                    if (t > l2nt) {
                        throw new Exception("bug: ttest1 > next_l2_velwtime.time");
                    }
                    double l1_td = (l1nt - l1t);
                    double s1l = (t - l1t) / l1_td;
                    double s1n = (l1nt - t) / l1_td;
                    l1av = s1l * l1nv + s1n * l1v;
                    double l2_td = (l2nt - l2t);
                    double s2l = (t - l2t) / l2_td;
                    double s2n = (l2nt - t) / l2_td;
                    l2av = s2l * l2nv + s2n * l2v;
                    double diff = Math.abs(l1av - l2av);
                    if (max_diff < diff) {
                        max_diff = diff;
                    }
                    if (null != ps) {
                        ps.printf("%.4f,%.4f,%f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f\n",
                                t, diff, max_diff, l1_td, l2_td, l1t - l2t, l1nt - l2nt, l1v, l1nv, l1av, l2v, l2nv, l2av, l1v - l2v, l1nv - l2nv);
                    }
                    double vpv = l1av * l2av;
                    corr += td * vpv;
                    last_t = next_t;
                    VelCorrData vcd = new VelCorrData();
                    vcd.l1av = l1av;
                    vcd.l2av = l2av;
                    vcd.td = td;
                    vcd.time = t;
                    newVelCorrList.add(vcd);
                }
                if (next_l1_velwtime.time + offset - next_t <= 0.0) {
                    l1i++;
                    l1_velwtime = next_l1_velwtime;
                    next_l1_velwtime = l1.get(l1i);
                }
                if (next_l2_velwtime.time - next_t <= 0.0) {
                    l2i++;
                    l2_velwtime = next_l2_velwtime;
                    next_l2_velwtime = l2.get(l2i);
                }
            }
            if (null != ps) {
                ps.close();
                ps = null;
                System.exit(0);
            }
            velCorrList = newVelCorrList;
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (null != ps) {
                ps.close();
                ps = null;
            }
        }
        return corr;
    }
//    static public double next_xc(double x0,double xc, double x1,
//               double y0, double yc, double y1) {
//        double new_xc = 0;
//        double[][] vals = {{x0*x0,x0,1},{xc*xc,xc,1.0},{x1*x1,x1,1.0}};
//        Matrix A = new Matrix(vals);
//        double[][] yvals = {{y0},{yc},{y1}};
//        Matrix b = new Matrix(yvals);
//        Matrix x = A.solve(b);
//        System.out.println("x="+x);
//        System.out.println(x.get(0,0)+","+x.get(1,0)+","+x.get(2,0));
//        System.out.println(x.get(0,0)*x0*x0+x.get(1,0)*x0+x.get(2,0));
//        System.out.println(x.get(0,0)*xc*xc+x.get(1,0)*xc+x.get(2,0));
//        System.out.println(x.get(0,0)*x1*x1+x.get(1,0)*x1+x.get(2,0));
//        
//        double ans = -x.get(1,0)/(2*x.get(0,0));
//        System.out.println("ans = " + ans);
//        return ans;
//    }
    static double time_offset_err = 0.0;
    static double true_offset_percent_of_max_correlation = 0.0;
    static double max_corr_time_offset = 0;
    static int test_number = 0;

    static public double [] computeVelCorrArrayFromVels(List<VelWTime> lvgt,
            List<VelWTime> lvsut,
            double tstart, double tinc, int tcount) {
        double outA[] = new double[tcount];
        for(int ti = 0; ti < tcount; ti++) {
            outA[ti] =
                    computeVelCorr(lvgt,lvsut,(tstart+tinc*ti));
        }        
        return outA;
    }
    
    static public double [] computeVelCorrArrayFromPoints(List<Point2DwTime> lsut,
            List<Point2DwTime> lgt,
            double tstart, double tinc, int tcount) {
        List<VelWTime> lvgt = point2DwTimeListToVelList(lgt);
        List<VelWTime> lvsut = point2DwTimeListToVelList(lsut);
        return computeVelCorrArrayFromVels(lvgt,lvsut,tstart,tinc,tcount);
    }
    
    static public void testSystem(
            boolean save_files,
            double gauss_pos_err,
            double time_offset,
            double sut_period,
            int moves) {
        PrintStream ps = null;
        try {
            test_number++;
            time_offset_err = 0.0;
            true_offset_percent_of_max_correlation = 0.0;
            max_corr_time_offset = 0;
            mean_sut_err = 0.0;
            total_sut_err = 0.0;
            max_sut_err = 0.0;
            LinkedList<Point2D.Double> waypoints = new LinkedList<Point2D.Double>();
            for (int i = 0; i < (moves + 3) / 4; i++) {
                waypoints.add(new Point2D.Double(0.0, 3.0));
                if (moves - i * 4 > 1) {
                    waypoints.add(new Point2D.Double(5.0, 3.0));
                }
                if (moves - i * 4 > 2) {
                    waypoints.add(new Point2D.Double(3.0, 0.0));
                }
                if (moves - i * 4 > 3) {
                    waypoints.add(new Point2D.Double(0.0, 0.0));
                }
            }
            List<Point2DwTime> lgt = GenSimulatedGT(
                    new Point2D.Double(), // start
                    0.5, // amax
                    1.0, // vmax
                    1.0, // dwell
                    0.025, // period
                    waypoints);
            if (save_files) {
                ps = OpenPrintStream("gt_vcsat" + test_number + ".csv");
                ps.println("time,x,y");
                for (Point2DwTime p : lgt) {
                    ps.println(p.time + "," + p.x + "," + p.y);
                }
                ps.close();
            }
            List<Point2DwTime> lsut = GenSimulatedSUT(
                    time_offset, //time_offset,
                    0.9, //x_offset
                    -0.3, // y_offset,
                    Math.PI / 6.0, // yaw_offset
                    lgt.get(0).time + r.nextDouble() * 0.1, // start_time,
                    lgt.get(lgt.size() - 1).time - r.nextDouble() * 0.1, //end_time,
                    sut_period,//period,
                    gauss_pos_err, //gauss_pos_err,
                    lgt);
            if (save_files) {
                ps = OpenPrintStream("sut_vcsat" + test_number + ".csv");
                ps.println("time,x,y");
                for (Point2DwTime p : lsut) {
                    ps.println(p.time + "," + p.x + "," + p.y);
                }
                ps.close();
            }
            List<VelWTime> lvgt = point2DwTimeListToVelList(lgt);
            if (save_files) {
                ps = OpenPrintStream("gt_vt" + test_number + ".csv");
                ps.println("time,vel");
                for (VelWTime p : lvgt) {
                    ps.println(p.time + "," + p.vel);
                }
                ps.close();
            }
            List<VelWTime> lvsut = point2DwTimeListToVelList(lsut);
            if (save_files) {
                ps = OpenPrintStream("sut_vt" + test_number + ".csv");
                ps.println("time,vel");
                for (VelWTime p : lvsut) {
                    ps.println(p.time + "," + p.vel);
                }
                ps.close();
            }
            double max_corr = 0.0;
            if (save_files) {
                ps = OpenPrintStream("vcorr" + test_number + ".csv");
                ps.println("offset,corr");
            }
            double x0 = -0.1;
            double xc = 0.0;
            double x1 = +0.1;
            int iterations = 0;
            double last_corr = 0.0;
            double time_offset_corr = computeVelCorr(lvgt, lvsut, time_offset);
            double diff = x1 - x0;
            double y0 = computeVelCorr(lvgt, lvsut, x0);
            double yc = computeVelCorr(lvgt, lvsut, xc);
            double y1 = computeVelCorr(lvgt, lvsut, x1);
            double corr = Math.max(y0, Math.max(y1, yc));
            while (diff > 0.0005) {
                for (xc = x0; xc < x1; xc += diff * 0.05) {
                    last_corr = corr;
                    iterations++;
                    yc = computeVelCorr(lvgt, lvsut, xc);
//                if( y0 < y1) {
//                        x0 = xc;
//                        y0 = yc;
//                    if(yc > y1) {
//                        xc = (xc + x1)/2.0;
//                        yc = computeVelCorr(lvgt,lvsut,xc);
//                    } else {
//                        double  new_x1 = x1 + (x1-xc);
//                        xc = x1;
//                        yc = y1;
//                        x1 = new_x1;
//                        y1 = computeVelCorr(lvgt,lvsut,x1);
//                    }
//                } else {
//                    y1=yc;
//                    x1=xc;
//                    if(yc > y0) {
//                        xc = (xc + x0)/2.0;
//                        yc = computeVelCorr(lvgt,lvsut,xc);
//                    } else {
//                        double  new_x0 = x0 - (xc-x0);
//                        xc = x0;
//                        yc = y0;
//                        x0 = new_x0;
//                        y0 = computeVelCorr(lvgt,lvsut,x0);
//                    }
//                }
//                System.out.println("x0 = " + x0);
//                System.out.println("x1 = " + x1);
//                System.out.println("xc = " + xc);
//                if(y0 > max_corr) {
//                    max_corr_time_offset = x0;
//                    max_corr = y0;
//                    ps.println(max_corr_time_offset+","+max_corr);
//                }
//                if(y1 > max_corr) {
//                    max_corr_time_offset = x1;
//                    max_corr = y1;
//                    ps.println(max_corr_time_offset+","+max_corr);
//                }
                    if (yc > max_corr) {
                        max_corr_time_offset = xc;
                        max_corr = yc;
                        //ps.println(max_offset+","+max_corr);
                    }
                    if (save_files) {
                        ps.println(xc + "," + yc);
                    }
                }
                diff = diff / 10.0;
                x1 = max_corr_time_offset + diff / 2;
                x0 = max_corr_time_offset - diff / 2;
            }
            if (null != ps) {
                ps.close();
                ps = null;
            }
            //System.out.println("iterations = " + iterations);
//            System.out.println("max_offset = " + max_corr_time_offset);
//            System.out.println("time_offset = " + time_offset);
            time_offset_err = max_corr_time_offset - time_offset;
//            System.out.println("time_offset_err = " + time_offset_err);
            true_offset_percent_of_max_correlation =
                    time_offset_corr / max_corr;
//            System.out.println("true_offset_percent_of_max_correlation = " + true_offset_percent_of_max_correlation);
        } catch (Exception ex) {
            Logger.getLogger(VelocityCorrellationSimulationAndTesting.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (null != ps) {
                ps.close();
                ps = null;
            }
        }
    }

    public static double average(List<Double> l) {
        double total = 0;
        for (Double d : l) {
            total += d;
        }
        return total / l.size();
    }

    public static double stddev(List<Double> l) {
        if (l.size() < 3) {
            return Double.NaN;
        }
        double avg = average(l);
        double total = 0;
        for (Double d : l) {
            total += (d - avg) * (d - avg);
        }
        return Math.sqrt(total / (l.size() - 1));
    }

    public static LinkedList<Double> abs(List<Double> l) {
        LinkedList<Double> abs_l = new LinkedList<Double>();
        for (Double d : l) {
            abs_l.add(Math.abs(d));
        }
        return abs_l;
    }

    public static double max(List<Double> l) {
        double ret = Double.NEGATIVE_INFINITY;
        for (Double d : l) {
            if (ret < d) {
                ret = d;
            }
        }
        return ret;
    }

    public static void createVcorrSum(String fname) {
        BufferedReader br = null;
        PrintStream ps = null;
        try {
            br = OpenBufferedReader(fname);
            final plotterJFrame pjf = new plotterJFrame();
            String line = br.readLine();
            String field_names[] = line.split(",");
            int time_offset_err_index = -1;
            int pos_err_index = -1;
            int m_index = -1;
            for (int i = 0; i < field_names.length; i++) {
                if ("time_offset_err".compareTo(field_names[i]) == 0) {
                    time_offset_err_index = i;
                    continue;
                }
                if ("pos_err".compareTo(field_names[i]) == 0) {
                    pos_err_index = i;
                    continue;
                }
                if ("m".compareTo(field_names[i]) == 0) {
                    m_index = i;
                    continue;
                }
            }
            HashMap<Double, HashMap<Integer, LinkedList<Double>>> pos_move_err_map = new HashMap<Double, HashMap<Integer, LinkedList<Double>>>();
            while ((line = br.readLine()) != null) {
                String fields[] = line.split(",");
                Double pos_err = Double.valueOf(fields[pos_err_index]);
                Double time_offset_err = Double.valueOf(fields[time_offset_err_index]);
                Integer m = Integer.valueOf(fields[m_index]);
                HashMap<Integer, LinkedList<Double>> move_err_map = pos_move_err_map.get(pos_err);
                if (move_err_map == null) {
                    move_err_map = new HashMap<Integer, LinkedList<Double>>();
                }
                LinkedList<Double> l = move_err_map.get(m);
                if (null == l) {
                    l = new LinkedList<Double>();
                }
                l.add(time_offset_err);
                move_err_map.put(m, l);
                pos_move_err_map.put(pos_err, move_err_map);
            }
            for (Double pos_err : pos_move_err_map.keySet()) {
                HashMap<Integer, LinkedList<Double>> move_err_map = pos_move_err_map.get(pos_err);
                String name = "pos_err_" + pos_err + ".csv";
                ps = OpenPrintStream(name);
                PlotData pd = new PlotData();
                pjf.AddPlot(pd, name);
                ps.println("m,mean_time_offset_err,stddev_time_offset_err,max_time_offset_err,n,pos_err");
                LinkedList<Integer> move_list = new LinkedList<Integer>(move_err_map.keySet());
                Collections.sort(move_list);
                for (Integer m : move_list) {
                    List<Double> l = move_err_map.get(m);
                    double mean_time_offset_err = average(abs(l));
                    double stddev_time_offset_err = stddev(l);
                    double max_time_offset_err = max(l);
                    int n = l.size();
                    pjf.AddPointToArrayPlot(pd, m, mean_time_offset_err);
                    //pd.addPlotPoint(new PlotPoint(m,mean_time_offset_err));
                    ps.printf("%d,%.5f,%.5f,%.5f,%d,%.5f\n",
                            m, mean_time_offset_err, stddev_time_offset_err,
                            max_time_offset_err, n, pos_err);
                }
                ps.close();
                ps = null;
            }
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    pjf.setVisible(true);
                    pjf.setUseShortname(false);
                    pjf.setBackground(Color.WHITE);
                    pjf.FitToGraph();
                }
            });
            Thread.sleep(300000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != ps) {
                ps.close();
                ps = null;
            }
            if (null != br) {
                try {
                    br.close();
                } catch (Exception e) {
                };
                br = null;
            }
        }
    }
    private static final Random r = new Random();

    public static void main(String args[]) {
        PrintStream ps = null;
        PrintStream tol_ps = null;
        try {
            createVcorrSum("test_vcorr.csv");
            System.exit(0);
            boolean save_files = true;
            double pos_err = 0.0;
            double sut_period;
            final int NUM_TIME_OFFSETS = 20;
            final int NUM_SUT_PERIODS = 1;
            final int NUM_POS_ERRORS = 5;
            final int NUM_MOVES = 100;
            final int MOVE_INC = 5;
            final double BASE_SUT_PERIOD = 0.035;
            final double BASE_POS_ERR = 0.005;
            HashMap<Integer, LinkedList<Double>> time_offsets = new HashMap<Integer, LinkedList<Double>>();
            ps = OpenPrintStream("test_vcorr.csv");
            ps.println("test_number,i,j,k,m,sut_period,pos_err,time_offset,max_corr_time_offset,time_offset_err,true_offset_percent_of_max_correlation-1");
            for (int i = 0; i < NUM_TIME_OFFSETS; i++) {
                for (int j = 0; j < NUM_POS_ERRORS; j++) {
                    pos_err = j * BASE_POS_ERR;
                    double time_offset = r.nextDouble() * 0.199 - 0.0995;
                    for (int k = 1; k < NUM_SUT_PERIODS + 1; k++) {
                        sut_period = BASE_SUT_PERIOD * k;
                        for (int m = 1; m < NUM_MOVES + 1; m += MOVE_INC) {
                            if (m == 6) {
                                m = 5;
                            }
                            testSystem(save_files,
                                    pos_err,
                                    time_offset,
                                    sut_period,
                                    m);
                            ps.printf("%d,%d,%d,%d,%d,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f\n",
                                    test_number, i, j, k, m, sut_period, pos_err, time_offset, max_corr_time_offset, time_offset_err, true_offset_percent_of_max_correlation - 1);
                            System.out.println("test_number=" + test_number + ",i=" + i + ",j=" + j + ",pos_err=" + pos_err + ",k=" + k + ",sut_period=" + sut_period + ",m=" + m + ",time_offset_err=" + time_offset_err);
                            int key = (j * (NUM_SUT_PERIODS + 1) + k) * (NUM_MOVES + 1) + m;
                            LinkedList<Double> tol = time_offsets.get(key);
                            if (tol == null) {
                                tol = new LinkedList<Double>();
                            }
                            tol.add(time_offset_err);
                            time_offsets.put(key, tol);
//                            if (Math.abs(time_offset_err) < 0.005) {
//                                break;
//                            }
                        }
                    }
                }
            }
            ps.close();
            ps = null;
            ps = OpenPrintStream("test_vcorr_sum.csv");
            ps.println("pos_err,sut_period,moves,mean,stddev,max,size");
            for (Integer key : time_offsets.keySet()) {
                int ki = key.intValue();
                int moves = ki % (NUM_MOVES + 1);
                ki /= (NUM_MOVES + 1);
                pos_err = (ki / (NUM_SUT_PERIODS + 1)) * BASE_POS_ERR;
                sut_period = (ki % (NUM_SUT_PERIODS + 1)) * BASE_SUT_PERIOD;
                LinkedList<Double> tol = time_offsets.get(key);
                if (null == tol) {
                    continue;
                }
                tol_ps =
                        OpenPrintStream(String.format("tol_pos_err_moves_%d_%.3f_sut_period_%.3f_.csv",
                        moves, pos_err, sut_period));
                Collections.sort(tol);
                double total = 0;
                double total2 = 0;
                double maxa = 0.0;
                for (Double to : tol) {
                    tol_ps.println(to);
                    to = Math.abs(to);
                    if (to > maxa) {
                        maxa = to;
                    }
                    total += to;
                    total2 += to * to;
                }
                tol_ps.close();
                tol_ps = null;
                double mean = total / tol.size();
                double stddev = Math.sqrt(total2 / tol.size() - total * total / (tol.size() * tol.size()));
                ps.println(pos_err + "," + sut_period + "," + moves + "," + mean + "," + stddev + "," + maxa + "," + tol.size());
            }
        } catch (Exception ex) {
            Logger.getLogger(VelocityCorrellationSimulationAndTesting.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (null != ps) {
                ps.close();
                ps = null;
            }
            if (null != tol_ps) {
                tol_ps.close();
                tol_ps = null;
            }
        }
    }
}
