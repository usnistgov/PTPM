/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package humantrackingperformancemetrics;

import java.awt.geom.Point2D;

/**
 * 3D point extended from AWT Point2D for easier drawing.
 */
public class Point3D extends Point2D.Double {
    
    public final double orig_x;
    public final double orig_y;
    public final double orig_z;
    
    public void applyTransform(float transform[]) {
        x = (double) (orig_x *transform[0] + orig_y*transform[1] + orig_z*transform[2]+transform[3]);
        y = (double) (orig_x *transform[4] + orig_y*transform[5] + orig_z*transform[6]+transform[7]);
        z = (double) (orig_x *transform[8] + orig_y*transform[9] + orig_z*transform[10]+transform[11]);
    }
    
    public void applyTransform(double transform[]) {
        x = (double) (orig_x *transform[0] + orig_y*transform[1] + orig_z*transform[2]+transform[3]);
        y = (double) (orig_x *transform[4] + orig_y*transform[5] + orig_z*transform[6]+transform[7]);
        z = (double) (orig_x *transform[8] + orig_y*transform[9] + orig_z*transform[10]+transform[11]);
    }
    
    /**
     * z coordinate.
     */
    public double z;

    /**
     * Default constructor.
     */
    /**
     * Default constructor.
     */
    public Point3D() {
        super(0, 0);
        this.z = 0;
        orig_x=0;
        orig_y=0;
        orig_z=0;
    }

    /**
     * Convenience Constructor
     * @param _x x coordinate
     * @param _y y coordinate
     * @param _z z coordinate
     */
    public Point3D(double _x, double _y, double _z) {
        super(_x, _y);
        this.z = _z;
        orig_x = _x;
        orig_y = _y;
        orig_z = _z;
    }

    public void setLocation(double _x, double _y, double _z) {
        super.setLocation(_x, _y); 
        this.z = _z;
    }

   
    public double distance(Point3D pt) {
        return Math.sqrt(distanceSq(pt));
    }

    public double distanceSq(Point3D pt) {
        return (pt.x-this.x)*(pt.x-this.x) + (pt.y-this.y)*(pt.y-this.y)+(pt.z-this.z)*(pt.z-this.z);
    }

    public void setLocation(Point3D p) {
        this.setLocation(p.x,p.y,p.z); 
    }
    
   
    @Override
    public String toString() {
        return String.format("(%.4f,%.4f,%.4f)", x, y, z);
    }
    
}
