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

import java.awt.Dialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author William Shackleford<shackle@nist.gov>
 */
public class MatchTableJPanel extends javax.swing.JPanel {

    /**
     * Creates new form MatchTableJPanel
     */
    public MatchTableJPanel() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButtonCancel = new javax.swing.JButton();
        jButtonLoadSelectedFiles = new javax.swing.JButton();

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jButtonCancel.setText("Cancel");
        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCancelActionPerformed(evt);
            }
        });

        jButtonLoadSelectedFiles.setText("Load Selected Files");
        jButtonLoadSelectedFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadSelectedFilesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonLoadSelectedFiles)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonCancel))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonCancel)
                    .addComponent(jButtonLoadSelectedFiles))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCancelActionPerformed
        this.cancelled = true;
        this.dialog.setVisible(false);
    }//GEN-LAST:event_jButtonCancelActionPerformed

    private void jButtonLoadSelectedFilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadSelectedFilesActionPerformed
        this.cancelled = false;
        this.dialog.setVisible(false);
    }//GEN-LAST:event_jButtonLoadSelectedFilesActionPerformed

    private JDialog dialog = null;
    private boolean cancelled = false;

    public void loadFileToTable(File f) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            List<String> lines = new LinkedList<>();
            String line = null;
            while (null != (line = br.readLine())) {
                lines.add(line);
            }
            br.close();
            br = null;
            String first_line_fa[] = lines.get(0).split("[, \t]+");
            DefaultTableModel tm = new DefaultTableModel(first_line_fa, lines.size() - 1){

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if(columnIndex > 5) {
                        return Double.class;
                    }
                    return super.getColumnClass(columnIndex); 
                }
                
            };
            for (int i = 1; i < lines.size(); i++) {
                line = lines.get(i);
                String fa[] = line.split("[, \t]+");
                for (int j = 0; j < fa.length; j++) {
                    if(j > 5) {
                        tm.setValueAt(Double.valueOf(fa[j]), i-1, j);
                    } else {
                        tm.setValueAt(fa[j], i - 1, j);
                    }
                }
            }
            this.jTable1.setModel(tm);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (Exception e) {
                };
                br = null;
            }
        }
    }

    static public class TableReturn {
        List<String> gtfilenames;
        List<String> sutfilenames;
    }
    
    static public TableReturn showDialog(Frame parent, File f) {
        try {
            MatchTableJPanel matchTableJPanel = new MatchTableJPanel();
            matchTableJPanel.loadFileToTable(f);
            JDialog dialog = new JDialog(parent, f.getName(), Dialog.ModalityType.APPLICATION_MODAL);
            matchTableJPanel.setVisible(true);
            matchTableJPanel.dialog = dialog;
            dialog.add(matchTableJPanel);
            dialog.pack();
        //dialog.setModal(Dialog.ModalityType.APPLICATION_MODAL);
            //OR, you can do the following...
            //JDialog dialog = new JDialog();
            //dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

            //dialog.setBounds(transform_frame.getBounds());
            dialog.setVisible(true);
            if (matchTableJPanel.cancelled) {
                return null;
            }
            int rows[] =matchTableJPanel.jTable1.getSelectedRows();
            if(rows == null || rows.length < 1) {
                return null;
            }
            TableReturn ret = new TableReturn();
            ret.gtfilenames = new LinkedList<>();
            ret.sutfilenames = new LinkedList<>();
            for(int r : rows) {
                String gtfname = (String) matchTableJPanel.jTable1.getValueAt(r, 1) 
                        + File.separator + (String) matchTableJPanel.jTable1.getValueAt(r, 0);
                String sutfname = (String) matchTableJPanel.jTable1.getValueAt(r, 3) 
                        + File.separator + (String) matchTableJPanel.jTable1.getValueAt(r, 2);
                if(!ret.gtfilenames.contains(gtfname)) {
                 ret.gtfilenames.add(gtfname);
                }
                if(!ret.sutfilenames.contains(sutfname)) {
                 ret.sutfilenames.add(sutfname);
                }
            }
            return ret;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonCancel;
    private javax.swing.JButton jButtonLoadSelectedFiles;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
