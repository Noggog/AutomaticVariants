/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import automaticvariants.AV;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lev.gui.LAreaChart;
import lev.gui.LHelpPanel;
import lev.gui.LLabel;
import lev.gui.LNumericSetting;

/**
 *
 * @author Justin Swanson
 */
public class ManageHeightVariants extends DefaultsPanel {

    LNumericSetting minSetting;
    LNumericSetting maxSetting;
    LNumericSetting widthSetting;
    LNumericSetting intensitySetting;
    LAreaChart chart;

    public ManageHeightVariants(EncompassingPanel parent_) {
        super("Height Variance", AV.settings, parent_);
    }

    @Override
    public boolean initialize() {
        if (super.initialize()) {

            minSetting = new LNumericSetting("Height Minimum", AVGUI.settingsFont, AVGUI.light, 
		    0, 99, 1, AV.Settings.HEIGHT_MIN, parent.helpPanel);
            last = setPlacement(minSetting, last);
            minSetting.addChangeListener(new UpdateChartChangeHandler());
            AddSetting(minSetting);

	    maxSetting = new LNumericSetting("Height Maximum", AVGUI.settingsFont, AVGUI.light, 
		    1, 100, 1, AV.Settings.HEIGHT_MAX, parent.helpPanel);
            last = setPlacement(maxSetting, last);
            maxSetting.addChangeListener(new UpdateChartChangeHandler());
            AddSetting(maxSetting);
	    
	    widthSetting = new LNumericSetting("Height Width", AVGUI.settingsFont, AVGUI.light, 
		    1, 100, 1, AV.Settings.HEIGHT_WIDTH, parent.helpPanel);
            last = setPlacement(widthSetting, last);
            widthSetting.addChangeListener(new UpdateChartChangeHandler());
            AddSetting(widthSetting);
	    
            alignRight();

            chart = new LAreaChart("Current Settings", parent.helpPanel.getBottomSize(), Color.cyan);
            return true;
        }
        return false;
    }

    private void updateChart() {
//        int centerPoint = 50;
//        int offSet = 15;
//        SaveFile.settings.update();
//        int lowTierTrans = centerPoint - SaveFile.settings.getInt(SaveFile.Settings.ACTORS_LOW_TIER_REDUC) + 1;
//        int highTierTrans = centerPoint + SaveFile.settings.getInt(SaveFile.Settings.ACTORS_HIGH_TIER_REDUC) - 1;
//        int epicTierTrans = centerPoint + SaveFile.settings.getInt(SaveFile.Settings.ACTORS_HIGH_TIER_CUT);
//
//        double[] probArray = Probability.generateProbArray(centerPoint);
//        probArray = Probability.calibrateEpic(probArray, centerPoint);
//
//        chart.clear();
//        boolean doOnce = true;
//        for (int i = 0; i < probArray.length; i++) {
//            if (probArray[i] != 0) {
//                if (doOnce) {
//                    doOnce = false;
//                    chart.lowTier.add(i - centerPoint - 1, 0);
//                }
//                if (probArray[i] < 2) {
//                    probArray[i] = 2;
//                }
//
//                if (Probability.isLow(i, centerPoint, false) || i == lowTierTrans) {
//                    chart.lowTier.add(i - centerPoint, probArray[i]);
//                }
//                if (Probability.isNorm(i, centerPoint, false)) {
//                    chart.normTier.add(i - centerPoint, probArray[i]);
//                }
//                if (Probability.isHigh(i, centerPoint, false) || i == highTierTrans || i == epicTierTrans) {
//                    chart.highTier.add(i - centerPoint, probArray[i]);
//                }
//                if (Probability.isEpic(i, centerPoint, false) && SaveFile.settings.getBool(SaveFile.Settings.ACTORS_EPIC_BUTTON)) {
//                    chart.epicTier.add(i - centerPoint, probArray[i]);
//                }
//            }
//        }
//
//        chart.resetDomain();
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
        updateChart();
        help.addToBottomArea(chart);
    }
}
