package humantrackingperformancemetrics;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 *
 * @author shackle
 */
public class CsvParseOptions implements Cloneable {

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
    public String delim = ",";

    public static final CsvParseOptions DEFAULT = new CsvParseOptions();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field f : this.getClass().getFields()) {
            try {
                Object val = f.get(this);
                if (val == null || val == DEFAULT || val == this) {
                    continue;
                }
                sb.append(f.getName());
                sb.append("=");
                if (val.getClass().isArray()) {
                    if (val.getClass().isAssignableFrom(double[].class)) {
                        sb.append(Arrays.toString((double[]) val));
                    } else if (val.getClass().isAssignableFrom(float[].class)) {
                        sb.append(Arrays.toString((float[]) val));
                    } else {
                        sb.append(Arrays.toString((Object[]) val));
                    }
                } else {
                    sb.append(val);
                }
                sb.append(System.lineSeparator());
            } catch (IllegalArgumentException illegalArgumentException) {
            } catch (IllegalAccessException illegalAccessException) {
            }
        }
        return sb.toString();
    }

    @Override
    public CsvParseOptions clone() {
        CsvParseOptions ret = new CsvParseOptions();
        ret.CONFIDENCE_INDEX = this.CONFIDENCE_INDEX;
        ret.DISTANCE_SCALE = this.DISTANCE_SCALE;
        ret.NAME_INDEX = this.NAME_INDEX;
        ret.RADIUS_INDEX = this.RADIUS_INDEX;
        ret.ROI_HEIGHT_INDEX = this.ROI_HEIGHT_INDEX;
        ret.ROI_WIDTH_INDEX = this.ROI_WIDTH_INDEX;
        ret.TIME_INDEX = this.TIME_INDEX;
        ret.TIME_OFFSET = this.TIME_OFFSET;
        ret.TIME_SCALE = this.TIME_SCALE;
        ret.VX_INDEX = this.VX_INDEX;
        ret.VY_INDEX = this.VY_INDEX;
        ret.VZ_INDEX = this.VZ_INDEX;
        ret.X_INDEX = this.X_INDEX;
        ret.Y_INDEX = this.Y_INDEX;
        ret.Z_INDEX = this.Z_INDEX;
        ret.delim = this.delim;
        ret.DISTANCE_SCALE = this.DISTANCE_SCALE;
        if (null != this.transform) {
            ret.transform = new double[16];
            System.arraycopy(this.transform, 0, ret.transform, 0, ret.transform.length);
        } else {
            ret.transform = null;
        }
        ret.trasfrom_file = this.trasfrom_file;
        return ret;
    }

    public CsvParseOptions() {
        if (DEFAULT != null && this != DEFAULT) {
            this.CONFIDENCE_INDEX = DEFAULT.CONFIDENCE_INDEX;
            this.DISTANCE_SCALE = DEFAULT.DISTANCE_SCALE;
            this.NAME_INDEX = DEFAULT.NAME_INDEX;
            this.RADIUS_INDEX = DEFAULT.RADIUS_INDEX;
            this.ROI_HEIGHT_INDEX = DEFAULT.ROI_HEIGHT_INDEX;
            this.ROI_WIDTH_INDEX = DEFAULT.ROI_WIDTH_INDEX;
            this.TIME_INDEX = DEFAULT.TIME_INDEX;
            this.TIME_OFFSET = DEFAULT.TIME_OFFSET;
            this.TIME_SCALE = DEFAULT.TIME_SCALE;
            this.VX_INDEX = DEFAULT.VX_INDEX;
            this.VY_INDEX = DEFAULT.VY_INDEX;
            this.VZ_INDEX = DEFAULT.VZ_INDEX;
            this.X_INDEX = DEFAULT.X_INDEX;
            this.Y_INDEX = DEFAULT.Y_INDEX;
            this.Z_INDEX = DEFAULT.Z_INDEX;
            this.delim = DEFAULT.delim;
            this.DISTANCE_SCALE = DEFAULT.DISTANCE_SCALE;
            if (null != DEFAULT.transform) {
                this.transform = new double[16];
                System.arraycopy(DEFAULT.transform, 0, this.transform, 0, this.transform.length);
            } else {
                this.transform = null;
            }
            this.trasfrom_file = DEFAULT.trasfrom_file;
        }
    }

}
