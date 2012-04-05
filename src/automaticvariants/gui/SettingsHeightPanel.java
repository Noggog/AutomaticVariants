/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import java.awt.Dimension;
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
public class SettingsHeightPanel extends DefaultsPanel {

    LNumericSetting stdDevSetting;
    HeightVarChart chart;
    static int minStd = 3;
    static int maxStd = 40;
    static double cutoff = 2.5;

    public SettingsHeightPanel(EncompassingPanel parent_) {
	super("Height Variants", AV.save, parent_);
    }

    @Override
    public boolean initialize() {
	if (super.initialize()) {

	    chart = new HeightVarChart("Current Settings", new Dimension (parent.helpPanel.getBottomSize().width , 190),
		    AVGUI.yellow, AVGUI.orange, "Percent deviance from normal height", "Probability / Height");
	    chart.addSeries(AVGUI.darkGreen);

	    stdDevSetting = new LNumericSetting("Height Difference", AVGUI.settingsFont, AVGUI.yellow,
		    0, maxStd, 1, AV.Settings.HEIGHT_STD, AV.save, parent.helpPanel);
	    last = setPlacement(stdDevSetting, last);
	    stdDevSetting.addChangeListener(new SettingsHeightPanel.UpdateChartChangeHandler());
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
	double std = (AV.save.getInt(AV.Settings.HEIGHT_STD) + minStd) / 3.0;
	if (std == 0) {
	    return;
	}

	double scale = 1 / bellCurve(0, std);
	// only iterate over "accepted area"
	for (double i = cutoff * -std - 1; i <= cutoff * std + 1; i = i + .5) {
	    double value = bellCurve(i, std);
	    chart.putPoint(0, i, value * scale);
	    if (i >= cutoff * std) {
		chart.max = (i / 100);
		chart.putPoint(1, i, 1 + chart.max);
	    } else if (i <= -cutoff * std) {
		chart.min = (i / 100);
		chart.putPoint(1, i, 1 + chart.min);
	    }
	}

	chart.plot.getDomainAxis().setRange(cutoff * -std, cutoff * std);
	chart.plot.getRangeAxis().setRange(0, 1.5);
    }

    // Equation just taken from wikipedia bell curve page.
    double bellCurve(double pos, double stdDev) {
	double exponent = -(Math.pow(pos, 2)
		/ (2 * Math.pow(stdDev, 2)));
	return (1 / stdDev / Math.sqrt(2 * Math.PI))
		* Math.pow(Math.E, exponent);
    }

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
	help.setBottomAreaHeight(190);
    }
}
