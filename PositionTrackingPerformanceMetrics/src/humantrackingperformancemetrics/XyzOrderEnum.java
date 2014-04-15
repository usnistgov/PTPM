package humantrackingperformancemetrics;

/**
 * Coordinates to use out of x,y,z.
 * The Optitrack is easier to set the coordinate system to XZ being 
 * horizontal since the frame establishes the XZ coordinates and it is easier
 * to use it laying on a table or the floor. For completeness all the combinations
 * are included.
 * @author Will Shackleford<shackle@nist.gov>
 */
public enum XyzOrderEnum {
    XY,XZ,YX,YZ,ZX,ZY
}
