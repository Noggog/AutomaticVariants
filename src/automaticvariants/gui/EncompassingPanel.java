/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lev.gui.LHelpPanel;

/**
 *
 * @author Justin Swanson
 */
public abstract class EncompassingPanel extends JPanel {

    protected int xPlacement = AVGUI.leftDimensions.width - 25;
    public LHelpPanel helpPanel;
    protected int defaultSpacing = 55;
    protected Close closeHandler = new Close();
    protected Open openHandler = new Open();

    public EncompassingPanel(Dimension d) {
        this.setLayout(null);
        setSize(d);
        setLocation(0, 0);
	helpPanel = new LHelpPanel(AVGUI.rightDimensions, new Font("Serif", Font.BOLD, 25), AVGUI.green, AVGUI.lightGray, true, 10);
        setOpaque(false);
    }

    protected void Add(JComponent c) {
        c.setVisible(true);
        add(c);
    }

    public ActionListener getOpenHandler() {
        return openHandler;
    }

    public void open() {
        setVisible(true);
        add(helpPanel);
        repaint();
    }

    public void close() {
        SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {
                        setVisible(false);
//                        if (openMainMenu != null)
//                            openMainMenu.fire();
                    }
                });

    }

    public class Close implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            close();
        }
    }

    public class Open implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            SwingUtilities.invokeLater(
                    new Runnable() {

                        @Override
                        public void run() {
                            open();
                        }
                    });
        }
    }
}
