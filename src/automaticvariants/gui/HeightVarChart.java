/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package automaticvariants.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import lev.gui.LAreaChart;

/**
 *
 * @author Justin Swanson
 */
public class HeightVarChart extends LAreaChart {

    double max = 0;
    double min = 0;
    static int peak = 66;
    static int floor = 155;
    int height = floor - peak;
    static int offsetX = 4;
    static int width = 125;
    static int chartHeight = 80;

    public HeightVarChart(String title_, Dimension size_, Color titleColor, Color seriesColor,
	    String XLabel, String YLabel) {
	super(title_, size_, titleColor, seriesColor, XLabel, YLabel);
    }

    @Override
    public void paint(Graphics g) {
	super.paint(g);
	g.setColor(Color.red);

	// Normal line
	g.drawLine(this.getSize().width / 2 + offsetX - width, peak,
		this.getSize().width / 2 + offsetX + width, peak);


	// Min Height
	g.drawLine(this.getSize().width / 2 + offsetX - width, peak
		- (int)Math.round(min * height),
		this.getSize().width / 2 + offsetX + width, peak
		- (int)Math.round(min * height));

	// Max Height
	g.drawLine(this.getSize().width / 2 + offsetX - width, peak
		- (int)Math.round(max * height),
		this.getSize().width / 2 + offsetX + width, peak
		- (int)Math.round(max * height));
    }
}
