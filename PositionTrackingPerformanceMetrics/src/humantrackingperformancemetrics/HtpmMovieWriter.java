/*
 * Wrap the movie writer class from MonteMedia to isolate its use.
 */
package humantrackingperformancemetrics;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.MovieWriter;
import org.monte.media.Registry;
import org.monte.media.VideoFormatKeys;
import org.monte.media.math.Rational;

/**
 * Class for encoding silent movies from generated buffered images.
 * It just wraps the MonteMedia tools.
 * 
 * @author Will Shackleford<shackle@nist.gov>
 */
public class HtpmMovieWriter {

    private File output_file = null;
    private int width = 0;
    private int height =0;
    private int track = 0;
    private MovieWriter monte_media_mw=null;
    private org.monte.media.Buffer buf = null;
    public int frame_count = 0;
    
    /**
     * Create a HtpmMovieWriter to write a movie to a file with the given
     * height and width
     * @param filename name of movie file to write (must end in .avi or .mov)
     * @param _width width of movie frame in pixels
     * @param _height height of movie frame in pixels
     */
    public HtpmMovieWriter(String filename, int _width, int _height, int _framesPerSecond) {
        this(new File(filename),_width,_height,_framesPerSecond);
    }
    
    private boolean ok=false;
    
    /**
     * Check to see if the writer has encountered an error so far.
     * @return true if no error has occurred.
     */
    public boolean isOk() {
        return this.ok;
    }
    
    /**
     * Create a HtpmMovieWriter to write a movie to a file with the given
     * height and width
     * @param file file reference to write to
     * @param _width width of movie frame in pixels
     * @param _height height of movie frame in pixels
     */
    public HtpmMovieWriter(File file, int _width, int _height, int _framesPerSecond) {
        try {
            ok=false;
            output_file = file;
            this.width = _width;
            this.height = _height;
            monte_media_mw = Registry.getInstance().getWriter(file);
            Format format = new Format(
                    FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, //
                    FormatKeys.FrameRateKey, new Rational(_framesPerSecond, 1),//
                    VideoFormatKeys.WidthKey, _width, //
                    VideoFormatKeys.HeightKey, _height,//
                    VideoFormatKeys.DepthKey, 24);
            if(file.getName().endsWith(".avi")) {
                format = format.append(FormatKeys.EncodingKey,
                        VideoFormatKeys.ENCODING_AVI_MJPG);
            }
            if(file.getName().endsWith(".mov")) {
                format = format.append(FormatKeys.EncodingKey,
                        VideoFormatKeys.ENCODING_QUICKTIME_ANIMATION); 
                //format.append(VideoFormatKeys.,VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_CINEPAK);
                format = format.append(VideoFormatKeys.CompressorNameKey,
                        VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_ANIMATION);
            }
            System.out.println("file = " + file);
            System.out.println("format = " + format);
            this.track = monte_media_mw.addTrack(format);
            this.buf = new org.monte.media.Buffer();
            this.buf.format = new Format(VideoFormatKeys.DataClassKey, BufferedImage.class);
            this.buf.sampleDuration = format.get(FormatKeys.FrameRateKey).inverse();
            ok=true;
        } catch (IOException ex) {
            Logger.getLogger(HtpmMovieWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    /**
     * Add an image as the next frame of the movie
     * @param frame image to add
     */
    public void addFrame(BufferedImage frame) {
        try {
            frame_count++;
            buf.data = frame;
            monte_media_mw.write(track, buf);
        } catch (IOException iOException) {
            Logger.getLogger(HtpmMovieWriter.class.getName()).log(Level.SEVERE, null, iOException);
            close();
        }
    }
    
    /**
     * Close the file and release memory associated with this writer.
     */
    public void close() {
        try {
            if(null != monte_media_mw) {
                this.monte_media_mw.close();
                this.monte_media_mw = null;
            }
        } catch(Exception e) {
            
        }
        ok=false;
    }
    
    @Override
    public void finalize() {
        try {
            close();
            super.finalize();
        } catch (Throwable ex) {
            Logger.getLogger(HtpmMovieWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
