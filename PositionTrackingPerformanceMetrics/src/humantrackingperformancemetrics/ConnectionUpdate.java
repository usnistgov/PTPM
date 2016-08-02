/*
 * This is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * 
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain. NIST Real-Time Control System software is an experimental
 * system. NIST assumes no responsibility whatsoever for its use by other
 * parties, and makes no guarantees, expressed or implied, about its
 * quality, reliability, or any other characteristic. We would appreciate
 * acknowledgement if the software is used. This software can be
 * redistributed and/or modified freely provided that any derivative works
 * bear some notice that they are derived from it, and any modified
 * versions bear some notice that they have been modified.
 * 
 */
package humantrackingperformancemetrics;

import humantrackingperformancemetrics.HTPM_JFrame.Settings;
import java.io.PrintStream;
import java.util.List;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class ConnectionUpdate {

    
    private List<Track> gtlist;
    private List<Track> sutlist;
    private List<Track> currentDeviceTracks;
    private List<Track> allTracks;
    private Runnable newTrackRunnable;
    private boolean addNewFrameLines;
    private boolean addUnaffiliatedMarkers;
    private Track unaffiliatedTrack;
    private String label;
    private PrintStream printStream;
    private CsvLinePrinterInterface csvLinePrinter;
    private Settings settings;

    /**
     * @return the gtlist
     */
    public List<Track> getGtlist() {
        return gtlist;
    }

    /**
     * @param gtlist the gtlist to set
     */
    public void setGtlist(List<Track> gtlist) {
        this.gtlist = gtlist;
    }

    /**
     * @return the sutlist
     */
    public List<Track> getSutlist() {
        return sutlist;
    }

    /**
     * @param sutlist the sutlist to set
     */
    public void setSutlist(List<Track> sutlist) {
        this.sutlist = sutlist;
    }

    /**
     * @return the addNewFrameLines
     */
    public boolean isAddNewFrameLines() {
        return addNewFrameLines;
    }

    /**
     * @param addNewFrameLines the addNewFrameLines to set
     */
    public void setAddNewFrameLines(boolean addNewFrameLines) {
        this.addNewFrameLines = addNewFrameLines;
    }

    /**
     * @return the addUnaffiliatedMarkers
     */
    public boolean isAddUnaffiliatedMarkers() {
        return addUnaffiliatedMarkers;
    }

    /**
     * @param addUnaffiliatedMarkers the addUnaffiliatedMarkers to set
     */
    public void setAddUnaffiliatedMarkers(boolean addUnaffiliatedMarkers) {
        this.addUnaffiliatedMarkers = addUnaffiliatedMarkers;
    }

    /**
     * @return the unaffiliatedTrack
     */
    public Track getUnaffiliatedTrack() {
        return unaffiliatedTrack;
    }

    /**
     * @param unaffiliatedTrack the unaffiliatedTrack to set
     */
    public void setUnaffiliatedTrack(Track unaffiliatedTrack) {
        this.unaffiliatedTrack = unaffiliatedTrack;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the currentDeviceTracks
     */
    public List<Track> getCurrentDeviceTracks() {
        return currentDeviceTracks;
    }

    /**
     * @param currentDeviceTracks the currentDeviceTracks to set
     */
    public void setCurrentDeviceTracks(List<Track> currentDeviceTracks) {
        this.currentDeviceTracks = currentDeviceTracks;
    }

    /**
     * @return the printStream
     */
    public PrintStream getPrintStream() {
        return printStream;
    }

    /**
     * @param printStream the printStream to set
     */
    public void setPrintStream(PrintStream printStream) {
        this.printStream = printStream;
    }

    /**
     * @return the csvLinePrinter
     */
    public CsvLinePrinterInterface getCsvLinePrinter() {
        return csvLinePrinter;
    }

    /**
     * @param csvLinePrinter the csvLinePrinter to set
     */
    public void setCsvLinePrinter(CsvLinePrinterInterface csvLinePrinter) {
        this.csvLinePrinter = csvLinePrinter;
    }

    /**
     * @return the allTracks
     */
    public List<Track> getAllTracks() {
        return allTracks;
    }

    /**
     * @param allTracks the allTracks to set
     */
    public void setAllTracks(List<Track> allTracks) {
        this.allTracks = allTracks;
    }

    /**
     * @return the newTrackRunnable
     */
    public Runnable getNewTrackRunnable() {
        return newTrackRunnable;
    }

    /**
     * @param newTrackRunnable the newTrackRunnable to set
     */
    public void setNewTrackRunnable(Runnable newTrackRunnable) {
        this.newTrackRunnable = newTrackRunnable;
    }

    /**
     * @return the settings
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * @param settings the settings to set
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}
