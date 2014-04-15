
package humantrackingperformancemetrics;

import java.awt.geom.Point2D;

/**
 *
 * @author shackle
 */
public class Point2DwTime extends Point2D.Double{

    public double time;

        public Point2DwTime(double _x, double _y, double _time) {
            super(_x, _y);
            this.time = _time;
        }
}
