/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mpath;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.table.DefaultTableModel;




/**
 *
 * @author Mahefa
 */

class ImageMap{
    public URL lastUrl = null;
    Path p;
    GraphPath gp;
    int zoom,xSize,ySize;
    double dpos = 0.02, dlat=0,dlng=0;
    String maptype;
    ImageIcon map;
    int nPrev = 100;
    boolean zoomed;
    
    ImageMap(){
    }
    
    public void paintScrollPane(javax.swing.JScrollPane scrl) {
        if(lastUrl!=null) try {
            dpos = 1/(10*(double)zoom);
            //if(!Util.checkConnection()) return;
            lastUrl = new URL(MapFunc.formUrlForPath(p, gp, zoom, xSize, ySize, maptype,dlat,dlng, zoomed));
            map = new ImageIcon(ImageIO.read(lastUrl));
            //map = new ImageIcon(ImageIO.read(new File("D:\\stm.png")));
            System.out.println("done with urlmap");
            JViewport vp = scrl.getViewport();
            vp.removeAll();
            vp.add(new JLabel(map));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //public 
    
    public void setParams(Path p, GraphPath gp, int zoom, int xSize, int ySize, String maptype, boolean zoomed) throws MalformedURLException
    {
        this.p = p;
        this.gp = gp;
        this.zoom = zoom;
        this.xSize = xSize;
        this.ySize = ySize;
        this.maptype = maptype;
        this.zoomed = zoomed;
        lastUrl = new URL(MapFunc.formUrlForPath(p, gp, zoom, xSize, ySize, maptype,dlat,dlng, zoomed));
    }
    
    public void setParams(int zoom, int xSize, int ySize, String maptype, boolean zoomed) throws MalformedURLException
    {
        this.zoom = zoom;
        this.xSize = xSize;
        this.ySize = ySize;
        this.maptype = maptype;
        this.zoomed = zoomed;
        lastUrl = new URL(MapFunc.formUrlForPath(p, gp, zoom, xSize, ySize, maptype,dlat,dlng, zoomed));
    }
    
    public void zoomScal(JScrollPane scrl, int n){
        JViewport vp = scrl.getViewport();
        System.out.println("scrolling "+n);
        Image img = map.getImage();
        Point p = vp.getViewPosition();
        System.out.println("view position "+p.x+","+p.y);
        double k = 1;
        if(n!=0) k = 1+(double)(n-nPrev)/(double)n;
        p = new Point((int)(p.x*k), (int)(p.y*k));
        int w = (map.getIconWidth()*n)/100;
        int h = (map.getIconHeight()*n)/100;
        Image newimg = img.getScaledInstance( w, h,  java.awt.Image.SCALE_SMOOTH ) ;  
        vp.removeAll();
        vp.add(new JLabel(new ImageIcon( newimg )));
        vp.setViewPosition(p);
        nPrev = n;
    }
}

public class GUI extends javax.swing.JFrame {
    
    protected int xPopPos,yPopPos;
    protected String enteredAddress;
    
    private int xSize=640, ySize=640, zoom=13;
    private String maptype = "hybrid";
    private URL lastUrl = null;
    private ScrollablePicture picMap;
    private Path p;
    private GraphPath gp;
    private double dpos = 0.02, dlat=0 ,dlng=0;
    private ImageMap imgmap;
    
    /**
     * Creates new form GUI
     */
    public GUI() {
        initComponents();
        this.imgmap = new ImageMap();
    }
    
    public void find_path() throws IOException{
        /******************* search function ***********************/
        DefaultTableModel dtm = (DefaultTableModel) this.tables.getModel();
        String begin = this.comboOrig.getSelectedItem().toString();
        String end = this.comboDest.getSelectedItem().toString();
        String[] waypoints = new String[dtm.getRowCount()];
        for(int i=0;i<waypoints.length;i++)
            waypoints[i] = dtm.getValueAt(i, 1).toString();
        
        if(begin.length()==0){
            Util.debugAlert("Error: Origin address not set");
            return;
        }
        if(end.length()==0){
            Util.debugAlert("Error: Destination address not set");
            return;
        }
        if(waypoints==null||waypoints.length==0){
            Util.debugAlert("Error: No waypoints set");
            return;
        }
        
        ArrayList<Address> addrs = new ArrayList<Address>();
        for(String s: waypoints){
            addrs.add(MapFunc.getAddress(s));
            //System.out.println("waypoints "+MapFunc.getAddress(s));
        }
        System.out.println("Origin: "+MapFunc.getAddress(begin)+"\nDestination: "+MapFunc.getAddress(end));
        GraphPath gp = new GraphPath(MapFunc.getAddress(begin),MapFunc.getAddress(end),addrs);
        String t = gp.formMatr();
        if(t!=null){
            labelDebug.setText(t);
            return;
        }
        Path mnpt = gp.getMinPath();
        System.out.println("\n*********\nMinimum cost path sounds to be : "+mnpt+"\ni.e.:");
        for(int v : mnpt.toArray())
            System.out.println("\t# "+gp.get(v));
        System.out.println("*********");
        dtm.getDataVector().removeAllElements();
        dtm.addRow(new Object[]{"[A]",gp.get(0).toString(),0,"0 sec"});
        double len =0;
        long time = 0;
        int[] ptArr = mnpt.toArray();
        for(int i=1;i<ptArr.length;i++){
            len += (double)gp.distDir[ptArr[i-1]][ptArr[i]]/(double)1000;
            time += (double)gp.dir[ptArr[i-1]][ptArr[i]].time;
            dtm.addRow(new Object[]{
                "["+MapFunc.labels[ptArr[i%MapFunc.labels.length]]+"]",
                gp.get(ptArr[i]).toString(),
                len,
                Util.getTimeString(time)
            });
        }
        this.tables.repaint();
        try {
            this.imgmap.setParams(mnpt, gp, zoom, xSize, ySize, maptype, this.jCheckBox1.isSelected());
            this.imgmap.paintScrollPane(this.jScrollPane2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        comboDest = new javax.swing.JComboBox();
        comboOrig = new javax.swing.JComboBox();
        comboPass = new javax.swing.JComboBox();
        butOrig = new javax.swing.JButton();
        butDest = new javax.swing.JButton();
        butPass = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tables = new javax.swing.JTable();
        spinZoom = new javax.swing.JSpinner();
        jScrollPane2 =  new javax.swing.JScrollPane();
        butFind = new javax.swing.JButton();
        butRem = new javax.swing.JButton();
        butDown = new javax.swing.JButton();
        butUp = new javax.swing.JButton();
        butRight = new javax.swing.JButton();
        butLeft = new javax.swing.JButton();
        butClear = new javax.swing.JButton();
        labelDebug = new javax.swing.JLabel();
        jSlider1 = new javax.swing.JSlider();
        jCheckBox1 = new javax.swing.JCheckBox();
        butRenew = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        menFile = new javax.swing.JMenu();
        mitemReset = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mitemQuit = new javax.swing.JMenuItem();
        menPref = new javax.swing.JMenu();
        menMapType = new javax.swing.JMenu();
        mitemRoad = new javax.swing.JMenuItem();
        mitemSat = new javax.swing.JMenuItem();
        mitemTer = new javax.swing.JMenuItem();
        mitemHybr = new javax.swing.JMenuItem();
        menHelp = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("MPath");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setResizable(false);

        jLabel1.setText("Origin:");

        jLabel2.setText("Destination:");

        jLabel3.setText("Pass by:");

        comboDest.setEditable(true);
        comboDest.setMaximumRowCount(15);
        comboDest.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        comboOrig.setEditable(true);
        comboOrig.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        comboPass.setEditable(true);
        comboPass.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        comboPass.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
                comboMenuInvisible(evt);
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
            }
        });

        butOrig.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/check.png"))); // NOI18N
        butOrig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butOrigActionPerformed(evt);
            }
        });

        butDest.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/check.png"))); // NOI18N
        butDest.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butDestActionPerformed(evt);
            }
        });

        butPass.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/check.png"))); // NOI18N
        butPass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butPassActionPerformed(evt);
            }
        });

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        tables.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Label", "Address", "Total distance (km)", "Total time"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tables.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tables);
        tables.getColumnModel().getColumn(0).setResizable(false);
        tables.getColumnModel().getColumn(0).setPreferredWidth(3);

        spinZoom.setEnabled(false);
        spinZoom.setValue(13);

        jScrollPane2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Map", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.ABOVE_TOP, null, new java.awt.Color(0, 51, 255)));
        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane2.setViewportBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane2.setPreferredSize(new java.awt.Dimension(550, 400));

        butFind.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/search1.png"))); // NOI18N
        butFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butFindActionPerformed(evt);
            }
        });

        butRem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/remove.png"))); // NOI18N
        butRem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRemActionPerformed(evt);
            }
        });

        butDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/down.png"))); // NOI18N

        butUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/up.png"))); // NOI18N
        butUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butUpActionPerformed(evt);
            }
        });

        butRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/right.png"))); // NOI18N
        butRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRightActionPerformed(evt);
            }
        });

        butLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mpath/left.png"))); // NOI18N

        butClear.setText("Clear");
        butClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butClearActionPerformed(evt);
            }
        });

        labelDebug.setText("...");

        jSlider1.setValue(100);
        jSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider1StateChanged(evt);
            }
        });

        jCheckBox1.setText("zoom");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        butRenew.setText("Renew");
        butRenew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRenewActionPerformed(evt);
            }
        });

        menFile.setMnemonic('F');
        menFile.setText("File");

        mitemReset.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mitemReset.setMnemonic('R');
        mitemReset.setText("Reset");
        mitemReset.setToolTipText("");
        mitemReset.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        menFile.add(mitemReset);
        menFile.add(jSeparator1);

        mitemQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        mitemQuit.setMnemonic('Q');
        mitemQuit.setText("Quit");
        menFile.add(mitemQuit);

        jMenuBar1.add(menFile);

        menPref.setMnemonic('P');
        menPref.setText("Preferences");
        menPref.setToolTipText("");

        menMapType.setMnemonic('M');
        menMapType.setText("MapType");

        mitemRoad.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_1, java.awt.event.InputEvent.ALT_MASK));
        mitemRoad.setMnemonic('R');
        mitemRoad.setText("roadmap");
        menMapType.add(mitemRoad);

        mitemSat.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_2, java.awt.event.InputEvent.ALT_MASK));
        mitemSat.setMnemonic('S');
        mitemSat.setText("sattelite");
        menMapType.add(mitemSat);

        mitemTer.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_3, java.awt.event.InputEvent.ALT_MASK));
        mitemTer.setMnemonic('T');
        mitemTer.setText("terrain");
        menMapType.add(mitemTer);

        mitemHybr.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_4, java.awt.event.InputEvent.ALT_MASK));
        mitemHybr.setMnemonic('H');
        mitemHybr.setText("hybrid");
        mitemHybr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mitemHybrActionPerformed(evt);
            }
        });
        menMapType.add(mitemHybr);

        menPref.add(menMapType);

        jMenuBar1.add(menPref);

        menHelp.setMnemonic('H');
        menHelp.setText("Help");
        jMenuBar1.add(menHelp);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(butClear)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(butRem, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(7, 7, 7)
                                .addComponent(butFind, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(comboDest, 0, 431, Short.MAX_VALUE)
                                    .addComponent(comboPass, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(comboOrig, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(butPass, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(butDest, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(butOrig, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 545, Short.MAX_VALUE))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(layout.createSequentialGroup()
                                            .addComponent(butDown)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 306, Short.MAX_VALUE)
                                            .addComponent(butLeft)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(butRight))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                            .addComponent(butUp)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jCheckBox1)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(spinZoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 463, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(106, 106, 106)
                                .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(butRenew))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(labelDebug, javax.swing.GroupLayout.PREFERRED_SIZE, 542, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(butOrig)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(comboOrig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addComponent(butUp))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(spinZoom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckBox1)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(butDest, javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(comboDest, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(butDown)
                    .addComponent(butRight)
                    .addComponent(butLeft))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(butPass)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel3)
                                .addComponent(comboPass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 344, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(butClear))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(butRem, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(butFind, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addComponent(butRenew))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelDebug, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(211, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void mitemHybrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mitemHybrActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_mitemHybrActionPerformed
    
    private void butUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butUpActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_butUpActionPerformed
    
    private void butRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRightActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_butRightActionPerformed
    
    private void butOrigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butOrigActionPerformed
        try {
            ArrayList<Address> possibility = MapFunc.geoCode((String)this.comboOrig.getSelectedItem());
            setChoices(this.comboOrig, possibility);
        } catch (IOException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_butOrigActionPerformed
    
    private void butDestActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butDestActionPerformed
        try {
            ArrayList<Address> possibility = MapFunc.geoCode((String)this.comboDest.getSelectedItem());
            setChoices(this.comboDest, possibility);
        } catch (IOException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_butDestActionPerformed
    
    private void butPassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butPassActionPerformed
        try {
            ArrayList<Address> possibility = MapFunc.geoCode((String)this.comboPass.getSelectedItem());
            setChoices(this.comboPass, possibility);
        } catch (IOException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_butPassActionPerformed
    
    private void comboMenuInvisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_comboMenuInvisible
        DefaultTableModel dtm = (DefaultTableModel) this.tables.getModel();
        if(((String)((javax.swing.JComboBox)evt.getSource()).getSelectedItem().toString()).isEmpty()) return;
        dtm.addRow(new Object[]{"", this.comboPass.getSelectedItem(), 0, ""});
        this.comboPass.removeAllItems();
        this.comboPass.getUI().setPopupVisible(this.comboPass, false);
    }//GEN-LAST:event_comboMenuInvisible
    
    private void butFindActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butFindActionPerformed
        try {
            this.find_path();
        } catch (IOException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_butFindActionPerformed
    
    private void butRemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRemActionPerformed
        int selection = this.tables.getSelectedRow();
        if(selection==-1) return;
        DefaultTableModel dtm = (DefaultTableModel) this.tables.getModel();
        dtm.removeRow(selection);
        this.tables.repaint();
    }//GEN-LAST:event_butRemActionPerformed
    
    private void butClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butClearActionPerformed
        DefaultTableModel dtm = (DefaultTableModel) this.tables.getModel();
        dtm.getDataVector().removeAllElements();
        this.tables.repaint();
    }//GEN-LAST:event_butClearActionPerformed

    private void jSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider1StateChanged
        this.imgmap.zoomScal(this.jScrollPane2, ((javax.swing.JSlider)evt.getSource()).getValue());
    }//GEN-LAST:event_jSlider1StateChanged

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        javax.swing.JCheckBox chbx = (javax.swing.JCheckBox)evt.getSource();
        if(chbx.isSelected())
            this.spinZoom.setEnabled(true);
        else
            this.spinZoom.setEnabled(false);
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void butRenewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRenewActionPerformed
        try {
            this.imgmap.setParams((int)this.spinZoom.getValue(), xSize, ySize, maptype, this.jCheckBox1.isSelected());
            this.imgmap.paintScrollPane(this.jScrollPane2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_butRenewActionPerformed
    
    private void setChoices(javax.swing.JComboBox combo, ArrayList<Address> possibility){
        DefaultTableModel dtm = (DefaultTableModel) this.tables.getModel();
        if(possibility==null){
            labelDebug.setForeground(Color.RED);
            labelDebug.setText("Error: Could not match entered address. Please, make sure you typed the address correctly.");
        }
        else if(possibility.size()==1){
            if(combo==this.comboPass){
                dtm.addRow(new Object[]{"", possibility.get(0), 0, ""});
                combo.removeAllItems();
            }
            else{
                combo.removeAllItems();
                combo.addItem(possibility.get(0));
                combo.setSelectedIndex(0);
            }
        }
        else{
            combo.removeAllItems();
            for(Address addr: possibility){
                combo.addItem(addr);
            }
            combo.setSelectedIndex(0);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GUI().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton butClear;
    private javax.swing.JButton butDest;
    private javax.swing.JButton butDown;
    private javax.swing.JButton butFind;
    private javax.swing.JButton butLeft;
    private javax.swing.JButton butOrig;
    private javax.swing.JButton butPass;
    private javax.swing.JButton butRem;
    private javax.swing.JButton butRenew;
    private javax.swing.JButton butRight;
    private javax.swing.JButton butUp;
    private javax.swing.JComboBox comboDest;
    private javax.swing.JComboBox comboOrig;
    private javax.swing.JComboBox comboPass;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSlider jSlider1;
    public static javax.swing.JLabel labelDebug;
    private javax.swing.JMenu menFile;
    private javax.swing.JMenu menHelp;
    private javax.swing.JMenu menMapType;
    private javax.swing.JMenu menPref;
    private javax.swing.JMenuItem mitemHybr;
    private javax.swing.JMenuItem mitemQuit;
    private javax.swing.JMenuItem mitemReset;
    private javax.swing.JMenuItem mitemRoad;
    private javax.swing.JMenuItem mitemSat;
    private javax.swing.JMenuItem mitemTer;
    private javax.swing.JSpinner spinZoom;
    private javax.swing.JTable tables;
    // End of variables declaration//GEN-END:variables
}
