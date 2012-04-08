/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lev.gui.LImagePane;
import lev.gui.LLabel;
import lev.gui.LProgressBarFrame;
import lev.gui.LProgressBarInterface;
import skyproc.SPGUI;
import skyproc.SPGlobal;

/**
 *
 * @author Justin Swanson
 */
public class AVGUI extends JFrame {

    static AVGUI singleton = null;
    static String header = "AVGUI";
    public static LProgressBarFrame progress = new LProgressBarFrame(
	    new Font("SansSerif", Font.PLAIN, 12), Color.GRAY,
	    new Font("SansSerif", Font.PLAIN, 10), Color.lightGray);
    public static Rectangle leftDimensions;
    public static Rectangle middleDimensions;
    public static Rectangle rightDimensions;
    public static Rectangle middleRightDimensions;
    public static Rectangle fullDimensions;

//    static Color green = new Color(35,174,10);
    static Color green = new Color(67,162,10);
    static Color darkGreen = new Color(61,128,21);
//    static Color orange = new Color(234,165,18);
    static Color orange = new Color(247,163,52);
    static Color blue = new Color(0,147,196);
    static Color yellow = new Color(255,204,26);
    static Color lightGray = new Color(190,190,190);
    static Color lightred = Color.red;

    static Font settingsFont = new Font("Serif", Font.BOLD, 16);

    public static boolean exitRequested = false;

    // Non static
    LImagePane backgroundPanel;
    LLabel willMakePatch;
    JTextArea statusUpdate;
    static SettingsMainMenu settingsMenu;

    private AVGUI() {
	super("Automatic Variants");
    }

    void addComponents() {
	try {

	    backgroundPanel = new LImagePane(AVGUI.class.getResource("background.jpg"));
	    add(backgroundPanel);

	    willMakePatch = new LLabel("A patch will be generated upon exit.", new Font("SansSerif", Font.PLAIN, 10), Color.GRAY);
	    willMakePatch.setLocation(backgroundPanel.getWidth() - willMakePatch.getWidth() - 7, 5);
	    backgroundPanel.add(willMakePatch);

	    progress.addWindowListener(new WindowListener() {

		@Override
		public void windowClosed(WindowEvent arg0) {
		}

		@Override
		public void windowActivated(WindowEvent arg0) {
		}

		@Override
		public void windowClosing(WindowEvent arg0) {
		    if (progress.closeOp == JFrame.DISPOSE_ON_CLOSE) {
			SPGlobal.log(header, "Progress bar window closing");
			AV.exitProgram();
		    }
		}

		@Override
		public void windowDeactivated(WindowEvent arg0) {
		}

		@Override
		public void windowDeiconified(WindowEvent arg0) {
		}

		@Override
		public void windowIconified(WindowEvent arg0) {
		}

		@Override
		public void windowOpened(WindowEvent arg0) {
		}
	    });
	    progress.setGUIref(singleton);

	    statusUpdate = new JTextArea();
	    statusUpdate.setSize(250, 18);
	    statusUpdate.setLocation(5, getFrameHeight() - statusUpdate.getHeight());
	    statusUpdate.setForeground(Color.LIGHT_GRAY);
	    statusUpdate.setOpaque(false);
	    statusUpdate.setText("Started application");
	    statusUpdate.setEditable(false);
	    statusUpdate.setVisible(true);
	    backgroundPanel.add(statusUpdate);

	    SPGUI.progress = new AVProgress();

	    leftDimensions = new Rectangle(0, 0, 299, statusUpdate.getY() - 10);
	    middleDimensions = new Rectangle(leftDimensions.x + leftDimensions.width + 7, 0, 330, getFrameHeight());
	    rightDimensions = new Rectangle(middleDimensions.x + middleDimensions.width + 7, 0, 305, getFrameHeight());
	    middleRightDimensions = new Rectangle(leftDimensions.x + leftDimensions.width + 7, 0, middleDimensions.width + rightDimensions.width + 7, getFrameHeight());
	    fullDimensions = new Rectangle(0, 0, getWidth(), getHeight());

	    settingsMenu = new SettingsMainMenu(getSize());
	    backgroundPanel.add(settingsMenu);
	    settingsMenu.open();

	    singleton.setVisible(true);

	} catch (IOException ex) {
	    SPGlobal.logException(ex);
	}

    }

    public static void open() {
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		if (singleton == null) {
		    singleton = new AVGUI();
		    singleton.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		    singleton.setResizable(false);
		    singleton.setSize(954, 658);
		    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		    singleton.setLocation(dim.width / 2 - singleton.getWidth() / 2, dim.height / 2 - singleton.getHeight() / 2);
		    singleton.setLayout(null);

		    singleton.addWindowListener(new WindowListener() {

			@Override
			public void windowClosed(WindowEvent arg0) {
			}

			@Override
			public void windowActivated(WindowEvent arg0) {
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
			    closingGUIwindow();
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
			}

			@Override
			public void windowOpened(WindowEvent arg0) {
			}
		    });

		    singleton.addComponents();

		    AV.runProgram();
		}
	    }
	});
    }

    int getFrameHeight() {
	return this.getHeight() - 28;
    }

    private static void closingGUIwindow() {
	SPGlobal.log(header, "Window Closing.");

	progress.setExitOnClose();
	progress.open(new ChangeListener(){

	    @Override
	    public void stateChanged(ChangeEvent e) {
		AV.exitProgram();
	    }
	});
	exitRequested = true;
	AV.runProgram();
    }

    public class AVProgress implements LProgressBarInterface {

	@Override
	public void setMax(int in) {
	    progress.setMax(in);
	}

	@Override
	public void setMax(int in, String status) {
	    progress.setMax(in, status);
	    if (!progress.paused()) {
		statusUpdate.setText(status);
	    }
	}

	@Override
	public void setStatus(String status) {
	    progress.setStatus(status);
	    if (!progress.paused()) {
		statusUpdate.setText(status);
	    }
	}

	@Override
	public void setStatus(int min, int max, String status) {
	    progress.setStatus(min, max, status);
	    if (!progress.paused()) {
		statusUpdate.setText(status);
	    }
	}

	@Override
	public void incrementBar() {
	    progress.incrementBar();
	}

	@Override
	public void reset() {
	    progress.incrementBar();
	}

	@Override
	public void setBar(int in) {
	    progress.setBar(in);
	}

	@Override
	public int getBar() {
	    return progress.getBar();
	}

	@Override
	public int getMax() {
	    return progress.getMax();
	}

	@Override
	public void pause(boolean on) {
	    progress.pause(on);
	}

	@Override
	public boolean paused() {
	    return progress.paused();
	}

	@Override
	public void done() {
	    progress.done();
	}
    }
}
