package humantrackingperformancemetrics;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Class which represents a track or the list of positions corresponding to a
 * single person/trackable/receiver and associated meta-data.
 *
 * @author Will Shackleford<shackle@nist.gov>
 */
public class Track {

    /**
     * List of points in chronological order.
     */
    public ArrayList<TrackPoint> data;
    
    /**
     * Color to draw this track by default.
     */
    public Color pointColor;
    
    /**
     * Color that was explicitly set by user if there is one.
     */
    public Color explicitPointColor;
   
    /**
     * Color to draw this track by default.
     */
    public Color lineColor;
    
    /**
     * Color that was explicitly set by user if there is one.
     */
    public Color explicitLineColor;
   
    /**
     * Identifier associated with this track.
     */
    public String name;
    /**
     * Index into data list for the point just before the current time.
     */
    public int cur_time_index;
    /**
     * Seconds since 1970.
     */
    private double CurrentTime;
    /**
     * Interpolated point for the current time from the closest point(s) in data
     * list.
     */
    public TrackPoint currentPoint;
    /**
     * Is this track currently selected?
     */
    public boolean selected = false;
    /**
     * Suppresses display of this track
     */
    public boolean hidden = false;
    /**
     * Points are disconnected set of points with no relationship. Optitrack
     * doesn't provide ID's for single points so single all single points in a
     * frame have no relationship to the points in the previous frame.
     *
     */
    public boolean disconnected = false;
    public boolean source_has_vel_info = false;
    public boolean vel_computed_from_positions = false;

    public void compute_vel_from_positions() {
        if (null == data || data.size() < 2 || disconnected) {
            this.vel_computed_from_positions = false;
            return;
        }
        final int n = data.size() - 1;
        double td0 = data.get(1).time - data.get(0).time;
        if (td0 > Double.MIN_NORMAL) {
            data.get(0).computed_vel_x = (data.get(1).x - data.get(0).x) / td0;
            data.get(0).computed_vel_y = (data.get(1).y - data.get(0).y) / td0;
            data.get(0).computed_vel_z = (data.get(1).z - data.get(0).z) / td0;
        }
        double tdn = data.get(n).time - data.get(n - 1).time;
        if (tdn > Double.MIN_NORMAL) {
            data.get(n).computed_vel_x = (data.get(n).x - data.get(n - 1).x) / tdn;
            data.get(n).computed_vel_y = (data.get(n).y - data.get(n - 1).y) / tdn;
            data.get(n).computed_vel_z = (data.get(n).z - data.get(n - 1).z) / tdn;
        }
        for (int i = 1; i < n; i++) {
            TrackPoint tp = data.get(i);
            tp.computed_vel_x = 0;
            tp.computed_vel_y = 0;
            tp.computed_vel_z = 0;
        }
        for (int i = 1; i < n; i++) {
            TrackPoint last_pt = data.get(i - 1);
            TrackPoint tp = data.get(i);
            TrackPoint next_pt = data.get(i + 1);
            double td = next_pt.time - last_pt.time;
            if(td < 0 ) {
                System.err.println("i="+i+",td = "+td+",name="+name+",next_pt="+next_pt+",last_pt="+last_pt);
                break;
            }
            if (td <= Double.MIN_NORMAL) {
                continue;
            }
            tp.computed_vel_x = (next_pt.x - last_pt.x) / td;
            tp.computed_vel_y = (next_pt.y - last_pt.y) / td;
            tp.computed_vel_z = (next_pt.z - last_pt.z) / td;
        }
        this.vel_computed_from_positions = true;
    }
    /**
     * String to identify source.
     */
    public String source = "";
    private double radius = 1.0;

    public void setRadius(double _radius) {
        this.radius = _radius;
        if (null != data) {
            for (int i = 0; i < data.size(); i++) {
                TrackPoint tp = data.get(i);
                tp.radius = _radius;
                data.set(i, tp);
            }
        }
    }

    public double getRadius() {
        return this.radius;
    }

    @Override
    public String toString() {
        return source + ":" + name;
    }

    /**
     * Get currentTime as seconds since 1970.
     *
     * @return current time
     */
    public double getCurrentTime() {
        return CurrentTime;
    }

    private void findCurrentTimeIndex(double _time) {

        if (null == this.data || _time < this.data.get(0).time
                || this.disconnected) {
            this.cur_time_index = -1;
            this.currentPoint = null;
            return;
        }
        if (this.cur_time_index < 0) {
            this.cur_time_index = 0;
        }
        if (this.cur_time_index > this.data.size() - 1) {
            this.cur_time_index = this.data.size() - 1;
        }
        while (this.cur_time_index >= 0
                && (this.data.get(this.cur_time_index)).time > _time) {
            this.cur_time_index--;
        }
        while (this.cur_time_index < this.data.size() - 1
                && (this.data.get(this.cur_time_index + 1)).time < _time) {
            this.cur_time_index++;
        }
    }
    /**
     * Does this track come from ground-truth source?
     */
    public boolean is_groundtruth = false;
    
    
    private InterpolationMethodEnum interpolatonMethod = InterpolationMethodEnum.LAST_POINT;

    /**
     * Get the value of interpolatonMethod
     *
     * @return the value of interpolatonMethod
     */
    public InterpolationMethodEnum getInterpolatonMethod() {
        return interpolatonMethod;
    }

    /**
     * Set the value of interpolatonMethod
     *
     * @param interpolatonMethod new value of interpolatonMethod
     */
    public void setInterpolatonMethod(InterpolationMethodEnum interpolatonMethod) {
        this.interpolatonMethod = interpolatonMethod;
    }
    
    private TrackPoint linearInterpolate(TrackPoint pt, TrackPoint ptp1, double _time) {
        TrackPoint new_pt = (TrackPoint) pt.clone();
                //TrackPoint ptp1 = this.data.get(this.cur_time_index + 1);
                double diff = ptp1.time - pt.time;
                double s1 = (_time - pt.time) / diff;
                double s2 = (ptp1.time - _time) / diff;
                if (s1 < 0 || s1 > 1 || s2 < 0 || s2 > 1) {
                    return null;
                }
                new_pt.x = (float) (pt.x * s2 + ptp1.x * s1);
                new_pt.y = (float) (pt.y * s2 + ptp1.y * s1);
                new_pt.z = (float) (pt.z * s2 + ptp1.z * s1);
                return new_pt;
    }

    private TrackPoint cubicInterpolate(
            TrackPoint pA, 
            TrackPoint pB, 
            TrackPoint pC,
            TrackPoint pD,
            double _time) {
        TrackPoint new_pt = (TrackPoint) pB.clone();
       
        double a3 = (-pA.x + 3 * (pB.x - pC.x) + pD.x) / 6;
        double a2 = (pA.x - 2 * pB.x + pC.x) / 2;
        double a1 = (pC.x - pA.x) / 2;
        double a0 = (pA.x + 4 * pB.x + pC.x) / 6;
//            System.out.println("a0 = " + a0);
//            System.out.println("(a0+a1+a2+a3) = " + (a0 + a1 + a2 + a3));
        double b3 = (-pA.y + 3 * (pB.y - pC.y) + pD.y) / 6;
        double b2 = (pA.y - 2 * pB.y + pC.y) / 2;
        double b1 = (pC.y - pA.y) / 2;
        double b0 = (pA.y + 4 * pB.y + pC.y) / 6;
        
        double c3 = (-pA.z + 3 * (pB.z - pC.z) + pD.z) / 6;
        double c2 = (pA.z - 2 * pB.z + pC.z) / 2;
        double c1 = (pC.z - pA.z) / 2;
        double c0 = (pA.z + 4 * pB.z + pC.z) / 6;
        double t = (_time - pB.time)/(pC.time - pB.time);
        new_pt.x =  ((a3 * t + a2) * t + a1) * t + a0;
        new_pt.y  = ((b3 * t + b2) * t + b1) * t + b0;
        new_pt.z  = ((c3 * t + c2) * t + c1) * t + c0;
        
        return new_pt;
    }
    
    public TrackPoint interpolate(double _time, InterpolationMethodEnum _interpolationMethod) {
        if(data == null || data.size() < 0) {
            return null;
        }
        if(data.get(0).time > _time) {
            return null;
        }
        if(data.get(data.size()-1).time < _time ) {
            return null;
        }
        this.findCurrentTimeIndex(_time);
        switch(_interpolationMethod) {
            case LAST_POINT:
                return data.get(cur_time_index).clone();
                
            case NEXT_POINT:
                if(cur_time_index > data.size()-2) {
                    return null;
                }
                return data.get(cur_time_index+1).clone();
                
            case CLOSEST_POINT:
                if(cur_time_index > data.size()-2) {
                    return data.get(cur_time_index).clone();
                }
                if(Math.abs(_time-data.get(cur_time_index).time) <= Math.abs(_time-data.get(cur_time_index+1).time)){
                    return data.get(cur_time_index).clone();
                } else {
                    return data.get(cur_time_index+1).clone();    
                }
            
            
            case VELOCITY:
                this.currentPoint = data.get(cur_time_index).clone();
                this.projectAheadToTime(_time);
                return this.currentPoint;
                
            case LINEAR:
                if(cur_time_index > data.size()-2) {
                    return null;
                }
                return linearInterpolate(data.get(cur_time_index),data.get(cur_time_index+1),_time);
                
            case CUBIC:
                if(cur_time_index > data.size()-2) {
                    return null;
                }
                if(cur_time_index > data.size()-3 || cur_time_index < 2) {
                    return linearInterpolate(data.get(cur_time_index),data.get(cur_time_index+1),_time);
                }
                return cubicInterpolate(
                            data.get(cur_time_index-1),
                            data.get(cur_time_index),
                            data.get(cur_time_index+1),
                            data.get(cur_time_index+2),
                            _time);
                
        }
        return null;
    }

    TrackPoint interpolate(double _time) {
        return interpolate(_time,this.interpolatonMethod);
    }
    /**
     * Setting the current time involves finding and setting the cur_time_index
     * to the closest point in the data list before this time and computing the
     * currentPoint by interpolation.
     *
     * @param _time time in seconds since 1970 to set the current time to
     */
    public void setCurrentTime(double _time) {
        this.CurrentTime = _time;
        if (this.disconnected) {
            this.currentPoint = null;
            return;
        }
        findCurrentTimeIndex(_time);
        if (this.disconnected) {
            this.currentPoint = null;
            return;
        }
        if (this.cur_time_index >= 0 && this.cur_time_index < this.data.size()) {
            TrackPoint pt = this.data.get(this.cur_time_index);
            this.currentPoint = interpolate(_time);
            if (null != this.currentPoint) {
                this.currentPoint.time = _time;
                this.radius = this.currentPoint.radius;
            }
        }
    }
    static public boolean ignore_sut_velocities = false;
    
    private double transform[] = null;
    public void setTransform(double _transform[]) {
        this.transform = _transform;
    }
    
    public double []getTransform() {
        return this.transform;
    }
    
    public void applyTransform(double _transform[]) {
        this.setTransform(_transform);
        if(null != data) {
            for(TrackPoint tp: this.data) {
                tp.applyTransform(transform);
            }
        }
    }

    public void projectAheadToTime(double _time) {
        if (this.disconnected) {
            this.currentPoint = null;
            return;
        }
        TrackPoint pt = this.currentPoint;
        if (null == pt) {
            return;
        }
        if (this.cur_time_index == this.data.size() - 1) {
            TrackPoint last_pt = this.data.get(this.cur_time_index);
            double avg_inc = (last_pt.time - this.data.get(0).time) / this.data.size();
            if (_time - last_pt.time > avg_inc + 0.002) {
                this.currentPoint = null;
                return;
            }
        }
        if (HTPM_JFrame.s.ignore_sut_velocities) {
            this.currentPoint.x = (float) pt.x;
            this.currentPoint.y = (float) pt.y;
            this.currentPoint.z = (float) pt.z;
        } else {
            double diff = _time - pt.time;
            this.currentPoint.x = (float) (pt.x + diff * pt.vel_x);
            this.currentPoint.y = (float) (pt.y + diff * pt.vel_y);
            this.currentPoint.z = (float) (pt.z + diff * pt.vel_z);
        }
        this.currentPoint.time = _time;
        if(this.currentPoint.z < -2) {
            System.out.println("this.currentPoint="+this.currentPoint);
        }
    }
}
