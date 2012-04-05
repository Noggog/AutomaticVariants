/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.SwingUtilities;
import lev.gui.*;

/**
 *
 * @author Justin Swanson
 */
public abstract class DefaultsPanel extends LPanel {

    protected LButton defaults = new LButton("Set to Default");
    protected LButton save = new LButton("Revert to Saved");
    protected EncompassingPanel parent;
    public int spacing = 12;
    Point last;
    protected LLabel header;
    protected ArrayList<LUserSetting> settings = new ArrayList<LUserSetting>();
    protected LPanel settingsPanel;
    private ArrayList<Component> components = new ArrayList<Component>();
    private int rightMost = 0;
    protected LSaveFile saveFile;
    protected boolean initialized = false;

    public DefaultsPanel(String title, LSaveFile saveFile_, EncompassingPanel parent_) {
        super(AVGUI.fullDimensions);
        saveFile = saveFile_;
        parent = parent_;
        header = new LLabel(title, new Font("Serif", Font.BOLD, 26), AVGUI.orange);
    }

    public boolean initialize() {
        if (!initialized) {
            settingsPanel = new LPanel(AVGUI.middleDimensions);

            setVisible(true);
            int spacing = (settingsPanel.getWidth() - defaults.getWidth() - save.getWidth()) / 3;

            defaults.setLocation(spacing, settingsPanel.getHeight() - defaults.getHeight() - 15);
            defaults.addActionListener(getRevert(saveFile.defaultSettings));
            defaults.addMouseListener(getHover(saveFile.defaultSettings));

            save.setLocation(defaults.getX() + spacing + defaults.getWidth(), defaults.getY());
            save.addMouseListener(getHover(saveFile.saveSettings));
            save.addActionListener(getRevert(saveFile.saveSettings));

            header.addShadow();
            header.setLocation(settingsPanel.getWidth() / 2 - header.getWidth() / 2, 15);

            Add(defaults);
            Add(save);
            Add(header);
            add(settingsPanel);
            add(parent.helpPanel);
            last = new Point(settingsPanel.getWidth(), 65);
            initialized = true;
            return true;
        }
        return false;
    }

    public MouseListener getHover(final Map<Enum, Setting> in) {
        return new MouseListener() {

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                LSaveFile.copyTo(saveFile.curSettings, saveFile.tempCurSettings);
                for (LUserSetting s : settings) {
                    if (!s.revertTo(in)) {
                        s.highlightChanged();
                    }
                }
                update();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                for (LUserSetting s : settings) {
                    s.revertTo(saveFile.tempCurSettings);
                    s.clearHighlight();
                }
                update();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        };
    }

    public ActionListener getRevert(final Map<Enum, Setting> in) {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                for (LUserSetting s : settings) {
                    s.revertTo(in);
                }
                LSaveFile.copyTo(saveFile.curSettings, saveFile.tempCurSettings);
                update();
            }
        };
    }

    protected void update() {
    }

    @Override
    public final void Add(Component c) {
        settingsPanel.Add(c);
    }

    public void AddSetting(LUserSetting c) {
        settings.add(c);
        settingsPanel.Add(c);
    }

    public Point setPlacement(Component c, Point p) {
        return setPlacement(c, p.x, p.y);
    }

    public Point setPlacement(Component c, int x, int y) {
        c.setLocation(x / 2 - c.getWidth() / 2, y + spacing);
        components.add(c);
        if (c.getX() + c.getWidth() > rightMost) {
            rightMost = c.getX() + c.getWidth();
        }
        return new Point(x, c.getY() + c.getHeight());
    }

    public void alignRight() {
        for (Component c : components) {
            c.setLocation(rightMost - c.getWidth(), c.getY());
        }
    }

    public ActionListener getOpenHandler(final EncompassingPanel parent) {

        final DefaultsPanel cur = this;

        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                SwingUtilities.invokeLater(
                        new Runnable() {

                            @Override
                            public void run() {
                                parent.open();
                                initialize();
                                parent.add(cur);
                                specialOpen(parent);
                                parent.repaint();
                            }
                        });
            }
        };
    }

    public void specialOpen(EncompassingPanel parent) {

    }
}
