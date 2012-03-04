/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lev.gui.ImagePane;
import lev.gui.LLabel;
import lev.gui.LTextPane;
import lev.gui.resources.LFonts;
import lev.gui.resources.LImages;

/**
 *
 * @author Justin Swanson
 */
public class AVGUI extends JFrame {

    ImagePane backgroundPanel;
    LLabel patching;
    LTextPane description;
    ImagePane skyprocLogo;
    ImagePane AVLogo;

    /**
     * Creates and displays the SkyProc default GUI.
     * @param yourPatcherName This will be used as the title on the GUI.
     * @param yourDescription This will be displayed under the title.
     */
    public AVGUI(final String yourPatcherName, final String yourDescription) {
        super(yourPatcherName);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    init(yourPatcherName, yourDescription);
                } catch (IOException ex) {
                    Logger.getLogger(skyproc.SPDefaultGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

    }

    final void init(String pluginName, String descriptionText) throws IOException {

        // Set up frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width / 2 - getWidth() / 2, dim.height / 2 - getHeight() / 2);
        setResizable(false);
        setLayout(null);

        // Background Panel
        backgroundPanel = new ImagePane(LImages.multipurpose());
        super.add(backgroundPanel);
        skyprocLogo = new ImagePane(skyproc.SPDefaultGUI.class.getResource("SkyProc Logo Small.png"));
        skyprocLogo.setLocation(5, this.getHeight() - skyprocLogo.getHeight() - 30);
        backgroundPanel.add(skyprocLogo,0);


        // Label
        AVLogo = new ImagePane(AVGUI.class.getResource("AutoVarGUITitle.png"));
        AVLogo.setLocation(this.getWidth() / 2 - AVLogo.getWidth() / 2, 
		0);
        backgroundPanel.add(AVLogo,0);


        //Description
        description = new LTextPane(new Dimension(this.getWidth() - 100, 220), new Color(200, 200, 200));
        description.centerIn(this, AVLogo.getY() + AVLogo.getHeight() - 25);
        description.setEditable(false);
        description.setText(descriptionText);
        description.setFontSize(14);
        description.centerText();
        backgroundPanel.add(description,0);


        //Patch Button
        try {
            patching = new LLabel("Creating patch...", LFonts.Typo3(25), new Color(210, 210, 210));
        } catch (Exception ex) {
        }
        patching.addShadow();
        patching.centerIn(this, this.getHeight() - 75 - patching.getHeight());
        backgroundPanel.add(patching,0);

        setVisible(true);
    }

    void finishRun() {
        patching.setText("Patch is complete!");
        patching.centerIn(this, patching.getY());
        patching.setFontColor(Color.orange);
    }

    @Override
    public Component add(final Component c) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        backgroundPanel.add(c,1);
                    }
                });
            }
        });
        return c;
    }
    
    /**
     * Tells the default GUI to switch the text and tell the user the patch is complete.
     */
    public void finished() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                finishRun();
            }
        });
    }
}
