/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mpath;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 *
 * @author Mahefa
 */
public class ScrollablePicture extends JLabel implements Scrollable, MouseMotionListener{
    
    private int maxUnitIncrement = 1;
    private boolean missingPicture = false;
    private ImageIcon i;
    
    ScrollablePicture(ImageIcon i, int m) {
        super(i);
        this.i = i;
        if (i == null) {
            missingPicture = true;
            setText("Error while loading map.");
            setHorizontalAlignment(CENTER);
            setOpaque(true);
            setBackground(Color.white);
        }
        maxUnitIncrement = m;
        setAutoscrolls(true);
        addMouseMotionListener(this);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(i.getIconWidth(), i.getIconHeight());
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }
    
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL) {
            currentPosition = visibleRect.x;
        } else {
            currentPosition = visibleRect.y;
        }
        
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / maxUnitIncrement)* maxUnitIncrement;
            return (newPosition == 0) ? maxUnitIncrement : newPosition;
        } else {
            return ((currentPosition / maxUnitIncrement) + 1)*maxUnitIncrement - currentPosition;
        }
    }
    
    public int getScrollableBlockIncrement(Rectangle visibleRect,
            int orientation,
            int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width - maxUnitIncrement;
        } else {
            return visibleRect.height - maxUnitIncrement;
        }
    }
    
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }
    
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
    
    public void setMaxUnitIncrement(int pixels) {
        maxUnitIncrement = pixels;
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}