
package humantrackingperformancemetrics;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 *
 * @author shackle
 */
public class CsvParseOptions {

    public double DISTANCE_SCALE = 1f;
    public double TIME_SCALE = 1f;
    public double TIME_OFFSET = 0f;
    public int TIME_INDEX = 0;
    public int NAME_INDEX = 1;
    public int X_INDEX = 2;
    public int Y_INDEX = 3;
    public int Z_INDEX = 4;
    public int VX_INDEX = 8;
    public int VY_INDEX = 9;
    public int VZ_INDEX = 10;
    public int ROI_WIDTH_INDEX = 11;
    public int ROI_HEIGHT_INDEX = 12;
    public int CONFIDENCE_INDEX = 13;
    public int RADIUS_INDEX = 14;
    public double transform[] = null;
    public String trasfrom_file = null;
    public String filename = null;
    public String delim=",";
    
    public static final CsvParseOptions DEFAULT= new CsvParseOptions();
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field f : this.getClass().getFields()) {
            try {
                Object val = f.get(this);
                if(val == null || val == DEFAULT || val == this) {
                    continue;
                }
                sb.append(f.getName());
                sb.append("=");
                if(val.getClass().isArray()) {
                    if(val.getClass().isAssignableFrom(double [].class)) {
                        sb.append(Arrays.toString((double [])val));
                    }else if(val.getClass().isAssignableFrom(float [].class)) {
                        sb.append(Arrays.toString((float [])val));
                    } else {
                        sb.append(Arrays.toString((Object [])val));
                    }
                } else {
                    sb.append(val);
                }
                sb.append("\n");
            } catch (IllegalArgumentException illegalArgumentException) {
            } catch (IllegalAccessException illegalAccessException) {
            }
        }
        return sb.toString();
    }
}
