/**
 *
 *  KymoButler_ImproveKymo, 7 jan. 2026
 *
 *  Implements the Improve Kymo wavelet-like filtering using successive
 *  Gaussian blur differences, adapted from KymoToolBox.
 *
 */

/*
 * This class incorporates code and ideas from the KymoToolBox plugin:
 *   Improve_Kymo.java
 *   (C) Fabrice P. Cordelieres
 *   IJ-Plugin_KymoToolBox (GPL-3.0)
 *   https://github.com/fabricecordelieres/IJ-Plugin_KymoToolBox
 *
 * This file is part of KymoButler for ImageJ and is distributed
 * under the terms of the GNU General Public License v3.0 or later.
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.Blitter;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;

public class KymoButler_ImproveKymo implements ExtendedPlugInFilter, DialogListener {
	public static int start = 1;
	public static int stop = 15;
	@SuppressWarnings("unused")
	private int nPasses = 1;
	private int flags = DOES_8G | DOES_16 | DOES_32;
	private ImagePlus imp;
	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		
		if (IJ.versionLessThan("1.42k")) return DONE;
		
		return flags;
	}
	
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		if (imp.getNSlices() > 1) imp.setSlice(imp.getNSlices() / 2);
		GenericDialog gd = new GenericDialog(command);
		gd.addNumericField("Start", start, 0);
		gd.addNumericField("Stop", stop, 0);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();
		if (gd.wasCanceled()) return DONE;
		IJ.register(this.getClass());
		return IJ.setupDialog(imp, flags);
	}
	
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		start = (int) gd.getNextNumber();
		stop = (int) gd.getNextNumber();
		if (start > stop || start < 1 || gd.invalidNumber()) return false;
		return true;
	}
	
	public void setNPasses(int nPasses) {
		this.nPasses = nPasses;
	}
	
	public void run(ImageProcessor ip) {
		applyToProcessor(this.imp, ip, start, stop);
	}
	
	public static void apply(ImagePlus imp, int start, int stop) {
		if (imp == null) return;
		ImageProcessor ip = imp.getProcessor();
		if (ip == null) return;
		applyToProcessor(imp, ip, start, stop);
	}
	
	private static void applyToProcessor(ImagePlus imp, ImageProcessor ip, int start, int stop) {
		ImageProcessor ipFloat = ip.convertToFloatProcessor();
		ImageProcessor result = ipFloat.createProcessor(ipFloat.getWidth(), ipFloat.getHeight());
		result.setValue(0);
		result.fill();
		
		GaussianBlur gb = new GaussianBlur();
		for (int i = start; i <= stop; i++) {
			ImageProcessor plane1 = ipFloat.duplicate();
			ImageProcessor plane2 = ipFloat.duplicate();
			gb.blurGaussian(plane1, i - 1, i - 1, 0.00001);
			gb.blurGaussian(plane2, i, i, 0.00001);
			plane1.copyBits(plane2, 0, 0, Blitter.SUBTRACT);
			result.copyBits(plane1, 0, 0, Blitter.ADD);
		}
		
		switch (imp.getBitDepth()) {
			case 8:
				ip.setIntArray(result.convertToShort(true).convertToByte(true).getIntArray());
				break;
			case 16:
				ip.setIntArray(result.convertToShort(true).getIntArray());
				break;
			case 32:
				ip.setFloatArray(result.getFloatArray());
				break;
		}
		imp.resetDisplayRange();
	}
}
