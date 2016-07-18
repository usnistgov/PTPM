package humantrackingperformancemetrics;

/**
 * Class to record a single point in a track.
 * Extends java.awt.geom.Point2D for easier drawing.
 * @author Will Shackleford<shackle@nist.gov>
 */
public class TrackPoint extends Point3D implements Cloneable{
    
    public final double orig_vel_x;
    public final double orig_vel_y;
    public final double orig_vel_z;
        
        private double latency;

    /**
     * Get the value of latency
     *
     * @return the value of latency
     */
    public double getLatency() {
        return latency;
    }

    /**
     * Set the value of latency
     *
     * @param latency new value of latency
     */
    public void setLatency(double latency) {
        this.latency = latency;
    }

    public double radius = 0.0;
    
    public TrackPoint() {
        super(0,0,0);
        vel_x = 0;
        vel_y = 0;
        vel_z = 0;
        orig_vel_x=vel_x;
        orig_vel_y=vel_y;
        orig_vel_z=vel_z;
    }
    
    public TrackPoint(double _x,double _y,double _z,
            double _vel_x, double _vel_y, double _vel_z) {
        super(_x,_y,_z);
        vel_x = _vel_x;
        vel_y = _vel_y;
        vel_z = _vel_z;
        orig_vel_x=vel_x;
        orig_vel_y=vel_y;
        orig_vel_z=vel_z;
    }
    
    public TrackPoint(Point3D pt) {
        super(pt.orig_x,pt.orig_y,pt.orig_z);
        vel_x = 0;
        vel_y = 0;
        vel_z = 0;
        orig_vel_x=vel_x;
        orig_vel_y=vel_y;
        orig_vel_z=vel_z;
    }
    
    
    public void applyTransform(float transform[]) {
        super.applyTransform(transform);
        vel_x = orig_vel_x *transform[0] + orig_vel_y*transform[1] + orig_vel_z*transform[2];
        vel_y = orig_vel_x *transform[4] + orig_vel_y*transform[5] + orig_vel_z*transform[6];
        vel_z = orig_vel_x *transform[8] + orig_vel_y*transform[9] + orig_vel_z*transform[10];
    }
    
    public void applyTransform(double transform[]) {
        super.applyTransform(transform);
        vel_x = (float) (orig_vel_x *transform[0] + orig_vel_y*transform[1] + orig_vel_z*transform[2]);
        vel_y = (float) (orig_vel_x *transform[4] + orig_vel_y*transform[5] + orig_vel_z*transform[6]);
        vel_z = (float) (orig_vel_x *transform[8] + orig_vel_y*transform[9] + orig_vel_z*transform[10]);
    }
    /**
     * Id for the track this TrackPoint should belong to
     */
    public String name;
    
    /**
     * Timestamp in seconds since 1970.
     */
    public double time;
    
    
    /**
     * Velocity in x direction in meters/second.
     */
    public double vel_x=0f;
    
    /**
     * Velocity in y direction in meters/second.
     */
    public double vel_y=0f;
    
     /**
     * Velocity in z direction in meters/second.
     */
    public double vel_z=0f;
    
    /**
     * Velocity in x direction in meters/second.
     */
    public double computed_vel_x=0f;
    
    /**
     * Velocity in y direction in meters/second.
     */
    public double computed_vel_y=0f;
    
     /**
     * Velocity in z direction in meters/second.
     */
    public double computed_vel_z=0f;
    
    /**
     * Confidence in the classification that this is really a person.
     */
    public double confidence=1.0;
    
    public double ROI_width=0.0;
    public double ROI_height=0.0;
    
    public String source = null;
    
    @Override
    public TrackPoint clone() {
        TrackPoint pt = new TrackPoint(orig_x,orig_y,orig_z,
                orig_vel_x,orig_vel_y,orig_vel_z);
        pt.x = this.x;
        pt.y = this.y;
        pt.z = this.z;
        pt.time = this.time;
        pt.vel_x = this.vel_x;
        pt.vel_y = this.vel_y;
        pt.vel_z = this.vel_z;
        pt.computed_vel_x = this.computed_vel_x;
        pt.computed_vel_y = this.computed_vel_y;
        pt.computed_vel_z = this.computed_vel_z;
        pt.confidence = this.confidence;
        pt.radius = this.radius;
        pt.ROI_height = this.ROI_height;
        pt.ROI_width = this.ROI_width;
        pt.source = this.source;
        return pt;
    }
}
