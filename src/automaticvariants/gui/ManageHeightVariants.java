/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lev.gui.LHelpPanel;
import lev.gui.LNumericSetting;

/**
 *
 * @author Justin Swanson
 */
public class ManageHeightVariants extends DefaultsPanel {

    LNumericSetting stdDevSetting;
    HeightVarChart chart;
    
    static int maxStd = 40;
    static double cutoff = 2.5;

    public ManageHeightVariants(EncompassingPanel parent_) {
	super("Height Variance", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    chart = new HeightVarChart("Current Settings", parent.helpPanel.getBottomSize(),
		    Color.red, Color.BLUE, "Percent deviance from normal height", "Probability / Height");
	    chart.addSeries(Color.yellow);

	    stdDevSetting = new LNumericSetting("Height Difference", AVGUI.settingsFont, AVGUI.light,
		    1, maxStd, 1, AV.Settings.HEIGHT_STD, AV.save, parent.helpPanel);
	    last = setPlacement(stdDevSetting, last);
	    stdDevSetting.addChangeListener(new UpdateChartChangeHandler());
	    AddSetting(stdDevSetting);

	    alignRight();

	    updateChart();

	    return true;
	}
	return false;
    }

    void updateChart() {
	AV.save.update();
	chart.clear();
	double std = AV.save.getInt(AV.Settings.HEIGHT_STD) / 3.0;
	if (std == 0) {
	    return;
	}

	boolean hit = false;
	double scale = 1 / bellCurve(0, std);
	// only iterate over "accepted area"
	for (double i = cutoff * -std - 1; i <= cutoff * std + 1; i = i + .5) {
	    double value = bellCurve(i, std);
	    if (value > .0001) {
		chart.putPoint(0, i, value * scale);
		if (i >= cutoff * std) {
		    chart.max = (i / 100);
		    chart.putPoint(1, i, 1 +  chart.max);
		} else if (i <= - cutoff * std) {
		    chart.min = (i / 100);
		    chart.putPoint(1, i, 1 + chart.min);
		}
		hit = true;
	    } else if (hit) {
		break;
	    }
	}

	chart.resetDomain();
	chart.plot.getRangeAxis().setRange(0, 1.5) ;
    }

    // Equation just taken from wikipedia bell curve page.
    double bellCurve(double pos, double stdDev) {
	double exponent = -(Math.pow(pos, 2)
		/ (2 * Math.pow(stdDev, 2)));
	return (1 / stdDev / Math.sqrt(2 * Math.PI))
		* Math.pow(Math.E, exponent);
    }
//
//    private void updateChart() {
//	AV.save.update();
//
//	chart.clear();
//	
//	int intensity = AV.save.getInt(AV.Settings.HEIGHT_INTENSITY);
//	int width = AV.save.getInt(AV.Settings.HEIGHT_WIDTH);
//	double max = heightFunc(AV.save.getInt(AV.Settings.HEIGHT_MAX), width, intensity);
//	double div = max / 100;
//	System.out.println("Max: " + max + ", Div: " + div);
//	int[] buffer = new int[101];
//	for (int i = AV.save.getInt(AV.Settings.HEIGHT_STD); i <= AV.save.getInt(AV.Settings.HEIGHT_MAX); i++) {
//	    double num = heightFunc(i, width, intensity);
//	    System.out.println("Incrementing " + (int)(num/div));
//	    buffer[(int)(num/div)]++;
//	}
//
//	for (int i = 0 ; i < buffer.length ; i++) {
//	    chart.putPoint(i, buffer[i]);
//	}
//	
//	chart.resetDomain();
//    }
//
//    public double heightFunc(int seed, int width, int intensity) {
//	double pos = seed / 100.0;
//	double height = Math.pow(pos, intensity);
//	height /= width;
//	height = Math.pow(Math.E, height);
//	height -= 1;
//	return height;
//    }

    private class UpdateChartHandler implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent event) {
	    SwingUtilities.invokeLater(new Runnable() {

		@Override
		public void run() {
		    SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
			    updateChart();
			}
		    });
		}
	    });

	}
    }

    private class UpdateChartChangeHandler implements ChangeListener {

	@Override
	public void stateChanged(ChangeEvent event) {
	    SwingUtilities.invokeLater(new Runnable() {

		@Override
		public void run() {
		    SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
			    updateChart();
			}
		    });
		}
	    });

	}
    }

    @Override
    public void specialOpen(EncompassingPanel parent) {
	addChart(parent.helpPanel);
    }

    @Override
    protected void update() {
	SwingUtilities.invokeLater(new Runnable() {

	    @Override
	    public void run() {
		updateChart();  // Double nest to ensure it's the last listener
	    }
	});
    }

    public void addChart(LHelpPanel help) {
	help.addToBottomArea(chart);
    }
}
