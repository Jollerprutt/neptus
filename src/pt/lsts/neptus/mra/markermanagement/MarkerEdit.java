/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel Ribeiro
 * Feb 11, 2015
 */

package pt.lsts.neptus.mra.markermanagement;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.util.ImageUtils;


/**
 * @author Manuel R.
 *
 */
@SuppressWarnings("serial")
public class MarkerEdit extends JFrame {

    private final int RULER_SIZE = 15;
    private int selectMarkerRowIndex = -1;
    private JPanel panel = new JPanel(); 
    private MarkerManagement parent;
    private AbstractAction save, del;
    private LogMarkerItem selectedMarker;
    private JLabel markerImage, nameLabelValue, timeStampValue, locationValue, altitudeValue, depthValue;
    private JComboBox<String> classifValue;
    private JTextArea annotationValue;
    private JButton rectDrawBtn, circleDrawBtn, freeDrawBtn, exportImgBtn;
    private int mouseX, mouseY, initialX, initialY, lastMouseX, lastMouseY;
    private boolean enableFreeDraw = false;
    private boolean enableRectDraw = false;
    private boolean enableCircleDraw = false;
    private boolean enableGrid = false;
    private boolean enableRuler = true;
    private boolean toDeleteDraw = false;
    private BufferedImage layer,  rulerLayer, image, drawImageOverlay;
    private ArrayList<Point> pointsList = new ArrayList<>();

    public MarkerEdit(MarkerManagement parent) {
        this.parent = parent;

        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 590, 395);
        setIconImage(Toolkit.getDefaultToolkit().getImage(MarkerEdit.class.getResource("/images/menus/edit.png")));

        setupMenu();
        initialize();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initialize() {

        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new MigLayout("", "[][][][][grow][][][][grow]", "[][][][][][][grow][][grow]"));

        markerImage = new JLabel() { 
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (image!=null && layer!=null) {
                    Graphics2D lg2d = (Graphics2D) layer.getGraphics();
                    lg2d.setBackground(new Color(100, 100, 255, 0));
                    lg2d.clearRect(0, 0, layer.getWidth(), layer.getHeight());

                    Graphics2D rg2d = (Graphics2D) rulerLayer.getGraphics();
                    rg2d.setBackground(new Color(100, 100, 255, 0));
                    rg2d.clearRect(0, 0, rulerLayer.getWidth(), rulerLayer.getHeight());

                    g.drawImage(image, RULER_SIZE+1, RULER_SIZE+1, null);

                    if (enableRectDraw) 
                        drawRect(layer.getGraphics(), 0, 0);

                    if (enableFreeDraw)
                        drawFree(layer.getGraphics());

                    if (enableCircleDraw)
                        drawCircle(layer.getGraphics(), 0, 0);

                    g.drawImage(layer, RULER_SIZE+1, RULER_SIZE+1, null);

                    if (drawImageOverlay != null)
                        g.drawImage(drawImageOverlay, RULER_SIZE+1, RULER_SIZE+1, null);

                    //Draw ruler
                    if (enableRuler) {
                        drawRuler(rg2d);
                        g.drawImage(rulerLayer, 0, 0, null);
                    }
                }
            }
        };

        markerImage.setHorizontalAlignment(SwingConstants.CENTER);
        markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource("/images/unknown.png")));

        // setupDrawPopup();

        // markerImage.setComponentPopupMenu(drawPopupMenu);

        markerImage.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                //before ((JPanel) e.getSource()).repaint();
                markerImage.repaint();

                if (SwingUtilities.isLeftMouseButton(e)) {
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (enableFreeDraw && e.getClickCount() == 2) {
                        pointsList.clear();
                    }
                    mouseX = e.getX();
                    mouseY = e.getY();
                    initialX = mouseX;
                    initialY = mouseY;
                }

                //before ((JPanel) e.getSource()).repaint();
                markerImage.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // System.out.println("Exited");
                mouseX = mouseY = -1;    

                //repaint();
                //markerImage.repaint();
            }

        });

        markerImage.addMouseMotionListener(new MouseMotionListener() {


            @Override
            public void mouseMoved(MouseEvent e) {
                //System.out.println("moved");
                mouseX = e.getX();
                mouseY = e.getY();
            }   

            @Override
            public void mouseDragged(MouseEvent e) {                    
                mouseX = e.getX();
                mouseY = e.getY();
                if ((mouseX > RULER_SIZE && mouseX < image.getWidth()+RULER_SIZE && mouseY > 0) &&
                        (mouseY < image.getHeight())) {

                    pointsList.add(new Point(mouseX-RULER_SIZE-1, mouseY-RULER_SIZE-1));

                }
                //before ((JPanel) e.getSource()).repaint();
                markerImage.repaint();
            }
        });

        panel.add(markerImage, "cell 0 0 7 7,alignx left,aligny top");

        JLabel nameLabel = new JLabel("Label:");
        nameLabelValue = new JLabel();
        JLabel timeStampLabel = new JLabel("Timestamp:");
        timeStampValue = new JLabel("TS");
        JLabel locationLabel = new JLabel("Location:");
        locationValue = new JLabel("LOCATION");
        JLabel altitudeLabel = new JLabel("Altitude:");
        altitudeValue = new JLabel("ALTITUDE");
        JLabel classifLabel = new JLabel("Classification:");
        classifValue = new JComboBox<>();
        JLabel annotationLabel = new JLabel("Annotation:");
        JScrollPane scrollPane = new JScrollPane();
        annotationValue = new JTextArea();
        JLabel depthLabel = new JLabel(" / Depth:");
        depthValue = new JLabel("DEPTH");

        nameLabelValue.setBackground(Color.WHITE);
        nameLabelValue.setText("MARKER_LABEL");
        panel.add(nameLabel, "cell 7 0,alignx left");
        panel.add(nameLabelValue, "cell 8 0,alignx left");
        panel.add(timeStampLabel, "cell 7 1,alignx left");
        panel.add(timeStampValue, "cell 8 1,alignx left");
        panel.add(locationLabel, "cell 7 2,alignx left");
        panel.add(locationValue, "cell 8 2,alignx left");
        panel.add(altitudeLabel, "cell 7 3,alignx left");
        panel.add(altitudeValue, "flowx,cell 8 3,alignx left");
        panel.add(classifLabel, "cell 7 4,alignx trailing");
        classifValue.setBackground(Color.WHITE);
        classifValue.setModel(new DefaultComboBoxModel(Classification.values()));
        panel.add(classifValue, "cell 8 4,alignx left");
        panel.add(annotationLabel, "cell 7 5");
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, "cell 7 6 2 1,grow");
        annotationValue.setText("<Your annotations here>");
        annotationValue.setLineWrap(true); //Auto down line if the line is too long
        annotationValue.setWrapStyleWord(true); //Auto set up the style of words
        annotationValue.setRows(8);
        scrollPane.setViewportView(annotationValue);
        panel.add(depthLabel, "cell 8 3");
        panel.add(depthValue, "cell 8 3");
    }

    private void drawRect(Graphics g, int endX, int endY) {
        if (mouseX == -1 && lastMouseX == -1) 
            return;
        Graphics2D g2 = (Graphics2D) g;

        if (endX != 0 && endY != 0) {
            mouseX = endX;
            mouseY = endY;
        }

        int x = Math.min(initialX - RULER_SIZE - 1, mouseX - RULER_SIZE -1);
        int y = Math.min(initialY - RULER_SIZE - 1, mouseY - RULER_SIZE -1);
        int w = Math.max(initialX - RULER_SIZE -1, mouseX - RULER_SIZE -1) - Math.min(initialX - RULER_SIZE -1, mouseX - RULER_SIZE -1);
        int h = Math.max(initialY - RULER_SIZE -1, mouseY - RULER_SIZE -1) - Math.min(initialY - RULER_SIZE -1, mouseY - RULER_SIZE -1);


        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, w, h);
    }

    private void drawFree(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        for (Point p : pointsList) {
            g2.setColor(Color.WHITE);
            g2.drawLine(p.x+2, p.y, p.x, p.y);
        }
    }

    private void drawCircle(Graphics g, int endX, int endY) {
        if (mouseX == -1 && lastMouseX == -1) 
            return;

        if (endX != 0 && endY != 0) {
            mouseX = endX;
            mouseY = endY;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.WHITE);

        int x = Math.min(initialX - RULER_SIZE - 1, mouseX - RULER_SIZE -1);
        int y = Math.min(initialY - RULER_SIZE - 1, mouseY - RULER_SIZE -1);
        int w = Math.max(initialX - RULER_SIZE -1, mouseX - RULER_SIZE -1) - Math.min(initialX - RULER_SIZE -1, mouseX - RULER_SIZE -1);
        int h = Math.max(initialY - RULER_SIZE -1, mouseY - RULER_SIZE -1) - Math.min(initialY - RULER_SIZE -1, mouseY - RULER_SIZE -1);

        g2.drawOval(x,y,w,h);
    }

    private void drawRuler(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        int fontSize = 11;
        int margin = 8;
        int lineWith = 9;
        int y = image.getHeight()+RULER_SIZE;

        g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        g2d.setColor(Color.BLACK);

        //draw zero
        g2d.drawString("0", RULER_SIZE-5, y+lineWith);

        double range = selectedMarker.getRange();
        float zoomRangeStep = 1;
        if (range > 10.0 && range < 30.0)
            zoomRangeStep = 2;
        else {
            if (range > 30.0)
                zoomRangeStep = 5;
        }
        //System.out.println("Range "+ range);

        //horizontal black rectangle
        //g2d.setColor(new Color(.3f, .4f, .5f, .6f));
        g2d.setColor(new Color(.5f, .6f, .7f,.8f));
        Rectangle horizRect = new Rectangle(RULER_SIZE, image.getHeight()+3, image.getWidth()+1, 12);
        g2d.fill(horizRect);

        //vertical black rectangle
        Rectangle vertRect = new Rectangle(RULER_SIZE, RULER_SIZE+1, RULER_SIZE, image.getHeight()-12);
        g2d.fill(vertRect);

        g2d.setColor(Color.BLACK);
        //horizontal line
        g2d.drawLine(RULER_SIZE, y, image.getWidth()+RULER_SIZE, y);

        //vertical line
        g2d.drawLine(RULER_SIZE, RULER_SIZE+1, RULER_SIZE, y);

        // horizontal ruler (range)

        double step = zoomRangeStep * (image.getWidth()+margin) / range;
        double r = zoomRangeStep;
        int c = margin + (int) step;
        g2d.setColor(Color.WHITE);

        for (; c<=image.getWidth()+margin; c += step , r += zoomRangeStep) {
            int length = (int)(Math.log10(r)+1);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(c, y, c, y-lineWith);
            if (enableGrid)
                g2d.drawLine(c, RULER_SIZE, c, y);

            if (length >= 2) {
                margin = 13;
            }
            g2d.drawString("" + (int) r, c - margin, y-1);
            g2d.setColor(Color.BLACK);
            g2d.drawLine(c, y, c, y+lineWith);

        }

        // vertical ruler (height)
        double height = selectedMarker.getHeight();

        double zoomRangeStepV = 2.0;
        double stepV = zoomRangeStepV * (image.getHeight()) / height;
        double rV = 0;
        int cV = y;

        //System.out.println("height "+ height);

        for (; cV >= 0 ; cV -= stepV , rV += zoomRangeStepV) {
            if (cV < y && rV < height) {
                g2d.setColor(Color.WHITE);
                g2d.drawLine(RULER_SIZE+1, cV, (RULER_SIZE+3)+lineWith, cV);
                g2d.drawString("" + (int) rV,  RULER_SIZE + 4 , cV+11);
                if (enableGrid)
                    g2d.drawLine(RULER_SIZE+1, cV, image.getWidth()+RULER_SIZE, cV);

                g2d.setColor(Color.BLACK);
                g2d.drawLine(RULER_SIZE+1, cV, (RULER_SIZE)-lineWith, cV);
            }
        }
    }

    public void loadMarker(LogMarkerItem log, int rowIndex) {
        toDeleteDraw = false;
        selectedMarker = log;
        selectMarkerRowIndex = rowIndex;
        int prefWidth = 265;
        int prefHeight = 80;

        if (selectedMarker.getSidescanImgPath() != null ) {
            try {

                image = ImageIO.read(selectedMarker.getSidescanImgPath());

                int width = image.getWidth();
                int height = image.getHeight();

                markerImage.repaint();
                markerImage.setPreferredSize(new Dimension(width + RULER_SIZE + 10, height + RULER_SIZE + 10));

                setBounds(100, 100, width + prefWidth + RULER_SIZE + 10, height + prefHeight + RULER_SIZE + 10);
                setLocation(parent.getwindowLocation());

                if (selectedMarker.getDrawImgPath() != null && !selectedMarker.getDrawImgPath().toString().equals("null")) {
                    drawImageOverlay = ImageIO.read(selectedMarker.getDrawImgPath());
                } else 
                    drawImageOverlay = null;

                layer = ImageUtils.createCompatibleImage(image.getWidth(), 
                        image.getHeight(), Transparency.TRANSLUCENT);
                rulerLayer = ImageUtils.createCompatibleImage(markerImage.getPreferredSize().width, 
                        markerImage.getPreferredSize().height, Transparency.TRANSLUCENT);
                clearLayer();
            } catch (IOException e) {
                NeptusLog.pub().error("Error reading image file for maker: "+ selectedMarker.getLabel() + " ...");
                image = null;
                markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource("/images/unknown.png")));
                markerImage.setPreferredSize(new Dimension(markerImage.getIcon().getIconWidth(), markerImage.getIcon().getIconHeight()));
                setBounds(100, 100, markerImage.getIcon().getIconWidth() + prefWidth, markerImage.getIcon().getIconHeight() + prefHeight);
                setLocation(parent.getwindowLocation());
            }
        } else {
            image = null;
            markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource("/images/unknown.png")));
            markerImage.setPreferredSize(new Dimension(markerImage.getIcon().getIconWidth(), markerImage.getIcon().getIconHeight()));
            setBounds(100, 100, markerImage.getIcon().getIconWidth() + prefWidth + RULER_SIZE + 10, markerImage.getIcon().getIconHeight() + prefHeight + RULER_SIZE + 10);
            setLocation(parent.getwindowLocation());
        }
        if (image==null || layer==null) {
            rectDrawBtn.setEnabled(false);
            circleDrawBtn.setEnabled(false);
            freeDrawBtn.setEnabled(false);
            exportImgBtn.setEnabled(false);
        } else {
            rectDrawBtn.setEnabled(true);
            circleDrawBtn.setEnabled(true);
            freeDrawBtn.setEnabled(true);
            exportImgBtn.setEnabled(true);
        }
        enableCircleDraw = enableFreeDraw = enableRectDraw = false;
        nameLabelValue.setText(selectedMarker.getLabel());
        nameLabelValue.setToolTipText(selectedMarker.getLabel());
        timeStampValue.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(selectedMarker.getTimestamp()));
        locationValue.setText(selectedMarker.getLocation().toString());
        altitudeValue.setText(Double.toString(selectedMarker.getAltitude()));
        depthValue.setText(Double.toString(selectedMarker.getDepth()));
        classifValue.setSelectedItem(selectedMarker.getClassification());
        annotationValue.setText(selectedMarker.getAnnotation());
        nameLabelValue.setSize(nameLabelValue.getPreferredSize() );

        setTitle("Marker: " + nameLabelValue.getText() + " - " + timeStampValue.getText());
    }

    private void showSuccessDlg(String path) {
        if (!path.endsWith(".png"))
            path = path + ".png";

        JOptionPane.showMessageDialog(this, "Image exported to: "+path, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private String chooseSaveFile(BufferedImage image, String path) {

        JFileChooser fileChooser = new JFileChooser(new File(path));
        fileChooser.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "*.png";
            }

            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                } else {
                    String path = file.getAbsolutePath().toLowerCase();
                    if (path.endsWith(".png")) {
                        return true;
                    }
                }
                return false;
            }
        });

        int status = fileChooser.showSaveDialog(null);

        String fileName = null;

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                fileName = selectedFile.getCanonicalPath();
                if (!fileName.endsWith(".png")) {
                    selectedFile = new File(fileName + ".png");
                }
                ImageIO.write(image, "png", selectedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fileName;
    }

    private JButton createBtn(String iconPath, String toolTipTxt) {
        JButton btn = new JButton();

        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setIcon(ImageUtils.getIcon(iconPath));
        btn.setToolTipText(toolTipTxt);

        return btn;
    }

    private void setupMenu() {

        final JPopupMenu popup = new JPopupMenu();
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        JButton saveBtn = createBtn("images/menus/save.png", "Save");
        JButton delBtn = createBtn("images/menus/editdelete.png", "Delete");
        rectDrawBtn = createBtn("images/menus/rectdraw.png", "Draw rectangle");
        circleDrawBtn = createBtn("images/menus/circledraw.png", "Draw circle");
        freeDrawBtn = createBtn("images/menus/freedraw.png", "Draw");
        JButton clearDrawBtn = createBtn("images/menus/clear.png", "Clear all");
        JButton showGridBtn = createBtn("images/menus/grid.png", "Show grid");
        JButton showRulerBtn = createBtn("images/menus/ruler.png", "Show ruler");
        exportImgBtn = createBtn("images/menus/export.png", "Export");

        save = new AbstractAction(I18n.text("Save"), ImageUtils.getIcon("images/menus/save.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                Classification classif = (Classification) classifValue.getSelectedItem();
                String annotation = annotationValue.getText();

                //save drawing image
                BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();
                String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/markers/";

                // drawRect(layer.getGraphics(), lastMouseX, lastMouseY);

                //g2d.drawImage(image, 0, 0, null);
                if (enableRectDraw)
                    drawRect(g2d, lastMouseX, lastMouseY);
                if (enableCircleDraw)
                    drawCircle(g2d, lastMouseX, lastMouseY);
                if (enableFreeDraw)
                    drawFree(g2d);

                if (drawImageOverlay != null)
                    g2d.drawImage(drawImageOverlay, 0, 0, null);

                File drawFile = new File(path, selectedMarker.getLabel() + "_draw.png");
                // save image to file
                try {
                    ImageIO.write(img, "PNG", drawFile);
                } catch (IOException ie) {
                    NeptusLog.pub().error("Error writing image to file...");
                }

                g2d.dispose();


                //end save drawing image
                selectedMarker.setDrawImgPath(drawFile);
                selectedMarker.setClassification(classif);
                selectedMarker.setAnnotation(annotation);
                if (toDeleteDraw)
                    selectedMarker.setDrawImgPath(new File("null"));
                parent.updateLogMarker(selectedMarker, selectMarkerRowIndex);
                markerImage.repaint();
            }
        };

        del = new AbstractAction(I18n.text("Delete"), ImageUtils.getIcon("images/menus/editdelete.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                int res = showDelDialog();
                if (res==0)  { 
                    parent.deleteLogMarker(selectedMarker, selectMarkerRowIndex);
                    dispose();
                }
            }
        };

        AbstractAction showGrid = new AbstractAction(I18n.text("Show grid"), ImageUtils.getIcon("images/menus/grid.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (enableGrid)
                    enableGrid = false;
                else
                    enableGrid = true;

                markerImage.repaint();
            }
        };
        AbstractAction showRuler = new AbstractAction(I18n.text("Show ruler"), ImageUtils.getIcon("images/menus/ruler.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (enableRuler)
                    enableRuler = false;
                else
                    enableRuler = true;

                markerImage.repaint();
            }
        };

        AbstractAction clearDrawings = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                enableFreeDraw = false;
                enableRectDraw = false;
                enableCircleDraw = false;
                drawImageOverlay = null;
                clearLayer();

                //delete draw image if exists
                toDeleteDraw = true;
            }
        };
        AbstractAction drawRect = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                clearLayer();
                enableFreeDraw = false;
                enableRectDraw = true;
                enableCircleDraw = false;
            }
        };

        AbstractAction drawCircle = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                clearLayer();
                enableRectDraw = false;
                enableCircleDraw = true;
                enableFreeDraw = false;
            }
        };

        AbstractAction drawFree = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pointsList.clear();
                clearLayer();
                enableRectDraw = false;
                enableCircleDraw = false;
                enableFreeDraw = true;
            }
        };

        AbstractAction exportImgOnly = new AbstractAction(I18n.text("Image only")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (markerImage != null) {
                    BufferedImage img = new BufferedImage(markerImage.getWidth(), markerImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();

                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();
                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/markers/";

                    // save image to file
                    String fileName = chooseSaveFile(img, path);
                    // show saved dialog
                    if (fileName != null)
                        showSuccessDlg(fileName);
                }
            }
        };

        AbstractAction exportImageWruler = new AbstractAction(I18n.text("Image w/ ruler")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (markerImage != null && rulerLayer != null) {

                    BufferedImage img = new BufferedImage(markerImage.getWidth()+RULER_SIZE, markerImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();

                    g2d.drawImage(image, RULER_SIZE, 0, null);
                    g2d.drawImage(rulerLayer, 0, -RULER_SIZE, null);
                    g2d.dispose();
                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/markers/";

                    // save image to file
                    String fileName = chooseSaveFile(img, path);
                    // show saved dialog
                    if (fileName != null)
                        showSuccessDlg(fileName);
                }
            }
        };

        AbstractAction exportImgWdrawing = new AbstractAction(I18n.text("Image w/ drawing")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (markerImage != null && rulerLayer != null) {
                    BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();
                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/markers/";

                    // drawRect(layer.getGraphics(), lastMouseX, lastMouseY);

                    g2d.drawImage(image, 0, 0, null);
                    if (enableRectDraw)
                        drawRect(g2d, lastMouseX, lastMouseY);
                    if (enableCircleDraw)
                        drawCircle(g2d, lastMouseX, lastMouseY);
                    if (enableFreeDraw)
                        drawFree(g2d);

                    if (drawImageOverlay != null)
                        g2d.drawImage(drawImageOverlay, 0, 0, null);

                    g2d.dispose();

                    // save image to file
                    String fileName = chooseSaveFile(img, path);
                    // show saved dialog
                    if (fileName != null)
                        showSuccessDlg(fileName);
                }
            }
        };

        AbstractAction exportAll = new AbstractAction(I18n.text("All")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (markerImage != null && rulerLayer != null) {

                    BufferedImage img = new BufferedImage(markerImage.getWidth()+RULER_SIZE, markerImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();

                    g2d.drawImage(image, RULER_SIZE, 0, null);
                    g2d.drawImage(layer, 0, -RULER_SIZE, null);
                    g2d.drawImage(rulerLayer, 0, -RULER_SIZE, null);

                    g2d.dispose();
                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/markers/";

                    // save image to file
                    String fileName = chooseSaveFile(img, path);
                    // show saved dialog
                    if (fileName != null)
                        showSuccessDlg(fileName);
                }
            }
        };

        //add buttons to toolbar
        toolBar.add(saveBtn);
        toolBar.add(delBtn);
        toolBar.addSeparator(); 
        toolBar.add(rectDrawBtn);
        toolBar.add(circleDrawBtn);
        toolBar.add(freeDrawBtn);
        toolBar.add(clearDrawBtn);
        toolBar.addSeparator(); 
        toolBar.add(showGridBtn);
        toolBar.add(showRulerBtn);
        toolBar.addSeparator();
        toolBar.add(exportImgBtn);
        
        popup.add(new JMenuItem(exportImgOnly));
        popup.add(new JMenuItem(exportImageWruler));
        popup.add(new JMenuItem(exportImgWdrawing));
        popup.add(new JMenuItem(exportAll));

        //setup actions
        saveBtn.addActionListener(save);
        delBtn.addActionListener(del);
        rectDrawBtn.addActionListener(drawRect);
        circleDrawBtn.addActionListener(drawCircle);
        freeDrawBtn.addActionListener(drawFree);
        clearDrawBtn.addActionListener(clearDrawings);
        showGridBtn.addActionListener(showGrid);
        showRulerBtn.addActionListener(showRuler);
        
        exportImgBtn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX()+10, e.getY()+15);
            }
        });

        //setup shortcuts - key bindings
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK), "save");
        panel.getActionMap().put("save", save);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK), "delete");
        panel.getActionMap().put("delete", del);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK), "showGrid");
        panel.getActionMap().put("showGrid", showGrid);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK), "showRuler");
        panel.getActionMap().put("showRuler", showRuler);

        add(toolBar, BorderLayout.PAGE_START);
    }


    private int showDelDialog() {
        Object[] options = {"Yes, please", "No, thanks"};
        int n = JOptionPane.showOptionDialog(this,
                "Are you sure you want to delete this marker?",
                "Confirm delete",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        return n;
    }

    private void clearLayer(){
        initialX = initialY = mouseX = mouseY = -1;
        Graphics2D g2d = (Graphics2D) layer.getGraphics();
        g2d.setBackground(new Color(100, 100, 255, 0));
        g2d.clearRect(0, 0, layer.getWidth(), layer.getHeight());
        g2d.dispose();
        markerImage.repaint();
    }
}