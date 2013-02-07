/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mahefa.mpath;

import java.awt.Color;
import javax.swing.UIManager;

/**
 *
 * @author Mahefa
 */
public class MPath {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        // or whatever
        try { 
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"); 
            //UIManager.put("nimbusBlueGrey", Color.CYAN);
            //UIManager.put("nimbusbase", Color.LIGHT_GRAY);
            //UIManager.put("control", Color.CYAN);
        } catch (Exception ex) { 
            ex.printStackTrace(); 
        }
        GUI mpath = new GUI();
        mpath.setLocationRelativeTo(null);
        mpath.setVisible(true);
    }
}
