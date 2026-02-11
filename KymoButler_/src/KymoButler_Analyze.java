/**
*
*  KymoButler_Analyze.java, 20 juil. 2019
   Fabrice P Cordelieres, fabrice.cordelieres at gmail.com

   Copyright (C) 2019 Fabrice P. Cordelieres

   License:
   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import KymoButler.KymoButlerIO;
import KymoButler.KymoButlerResponseParser;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.NonBlockingGenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

/**
 * This class is aimed at launching the analysis of kymographs using the KymoButler webservice
 * @author Fabrice P. Cordelieres
 *
 */
public class KymoButler_Analyze implements PlugIn{
	/** Use local Wolfram Engine **/
	boolean useLocal=true;
	
	/** The ImagePlus that is present at startup (or null) **/
	ImagePlus ip=null;
	
	/** KymoButlerIO object: will handle all the analysis process **/
	KymoButlerIO kbio=new KymoButlerIO();
	
	/** Preferences: threshold **/
	float p=(float) Prefs.get("KymoButler_p.double", kbio.getThreshold());
	
	/** Preferences: minimumSize **/
	float minimumSize=(float) Prefs.get("KymoButler_minimumSize.double", kbio.getMinimumSize());
	
	/** Preferences: minimumFrames **/
	float minimumFrames=(float) Prefs.get("KymoButler_minimumFrames.double", kbio.getMinimumFrames());
	
	/** Preferences: use bidirectional model **/
	boolean useBidirectional=Prefs.get("KymoButler_useBidirectional.boolean", false);
	
	/** Preferences: bidirectional decision threshold **/
	float decisionThreshold=(float) Prefs.get("KymoButler_decisionThreshold.double", 0.5);
	
	/** Preferences: open local tables **/
	boolean openLocalTables=Prefs.get("KymoButler_openLocalTables.boolean", true);
	
	/** Preferences: batch mode **/
	boolean batchMode=Prefs.get("KymoButler_batchMode.boolean", false);
	
	/** Preferences: batch recursive **/
	boolean batchRecursive=Prefs.get("KymoButler_batchRecursive.boolean", true);
	
	/** Preferences: show outputs during batch **/
	boolean batchShowOutputs=Prefs.get("KymoButler_batchShowOutputs.boolean", false);
	
	/** Preferences: Improve Kymo before analysis **/
	boolean improveBeforeAnalysis=Prefs.get("KymoButler_improveBeforeAnalysis.boolean", false);
	
	/** Preferences: Improve Kymo start **/
	int improveStart=(int) Prefs.get("KymoButler_improveStart.double", 1);
	
	/** Preferences: Improve Kymo stop **/
	int improveStop=(int) Prefs.get("KymoButler_improveStop.double", 15);
	
	/** Preferences: addToManager **/
	boolean addToManager=Prefs.get("KymoButler_addToManager.boolean", true);
	
	/** Preferences: simplifyRois **/
	boolean simplifyTracks=Prefs.get("KymoButler_simplifyTracks.boolean", true);
	
	/** Preferences: addToManager **/
	boolean clearManager=Prefs.get("KymoButler_clearManager.boolean", true);
	
	/** Preferences: showKymo **/
	boolean showKymo=Prefs.get("KymoButler_showKymo.boolean", true);
	
	/** Preferences: showOverlay **/
	boolean showOverlay=Prefs.get("KymoButler_showOverlay.boolean", true);
	
	/** Preferences: allowCorrections **/
	boolean allowCorrections=Prefs.get("KymoButler_allowCorrections.boolean", false);
	
	/** Debug tag: true to save JSON in IJ installation folder **/
	boolean debug=Prefs.get("KymoButler_debug.boolean", false);

	/** Optional parameters JSON loading **/
	boolean loadParamsFromJson=Prefs.get("KymoButler_loadParamsFromJson.boolean", false);
	String paramsJsonPath=Prefs.get("KymoButler_paramsJsonPath.string", "");

	/** Hover hint popup **/
	private java.awt.Dialog hoverHintDialog=null;
	private java.awt.Label hoverHintLabel=null;
	
	String helpMsg="<html>Version 1.0.0, 18 nov. 2019<br>"
			+ "This plugin is powered by <a href=\"https://deepmirror.ai/software/kymobutler/\">KymoButler</a><br>"
			+ "a webservice provided by Andrea Dimitracopoulos and Max Jakobs<br>"
			+ "based on their <a href=\"https://doi.org/10.7554/eLife.42288\">publication</a> you should cite when using the website/plugin:<br><br>"
			+ "This plugin heavily relies on external libraries:"
			+ "<ul>"
			+ "	<li>commons-io, v2.17.0</li>"
			+ "	<li>org.json/json, v20240303</li>"
			+ "</ul>"
			+ "<br><br>"
			+ "The plugin is brought to you by F.P. Cordelières <a href=\"mailto:fabrice.cordelieres@gmail.com?subject=KymoButler for IJ\">fabrice.cordelieres@gmail.com</a>";
	

	@Override
	public void run(String arg) {
		ip=WindowManager.getCurrentImage();
		if(KymoButlerIO.checkForLibraries()) {
			showGUI();
		}else {
			IJ.showStatus("Installation of the required libraries needs to be done");	
		}
	}
	
	/**
	 * Displays the GUI, stores the parameters and launches the analysis
	 */
	public void showGUI() {
		NonBlockingGenericDialog gd=new NonBlockingGenericDialog("KymoButler for ImageJ");
		gd.addMessage("Cloud mode is deprecated. Local mode is always enabled.");
		gd.addCheckbox("Load_parameters_from_JSON", loadParamsFromJson);
		gd.addStringField("Parameters_JSON_path", paramsJsonPath, 40);
		gd.addMessage("Parameters");
		gd.addNumericField("Threshold (default: 0.2)", p, 2);
		gd.addNumericField("Minimum_size (default: 3)", minimumSize, 0);
		gd.addNumericField("Minimum_frames (default: 3)", minimumFrames, 0);
		gd.addCheckbox("Use_bidirectional_model", useBidirectional);
		gd.addNumericField("Bidirectional_decision_threshold (default: 0.5)", decisionThreshold, 2);
		
		gd.addMessage("");
		
		gd.addMessage("Output");
		gd.addCheckbox("Add to manager", addToManager);
		gd.addCheckbox("Simplify tracks", simplifyTracks);
		gd.addCheckbox("Clear manager before adding", clearManager);
		gd.addCheckbox("Show_kymograph", showKymo);
		gd.addCheckbox("Show_overlay", showOverlay);
		gd.addCheckbox("Open_local_output_tables", openLocalTables);
		gd.addCheckbox("Batch_mode (folder)", batchMode);
		gd.addCheckbox("Batch_include_subfolders", batchRecursive);
		gd.addCheckbox("Batch_show_outputs", batchShowOutputs);
		gd.addCheckbox("Improve_kymo_before_analysis", improveBeforeAnalysis);
		gd.addNumericField("Improve_start", improveStart, 0);
		gd.addNumericField("Improve_stop", improveStop, 0);
		
		gd.addMessage("Note: Local processing uses Wolfram Engine on your machine.");
		applyTooltips(gd);
		
		gd.addHelp(helpMsg);
		gd.showDialog();
		
		if(gd.wasOKed()) {
			loadParamsFromJson=gd.getNextBoolean();
			paramsJsonPath=gd.getNextString();
			if(loadParamsFromJson) {
				if(paramsJsonPath==null || paramsJsonPath.trim().isEmpty()) {
					OpenDialog od=new OpenDialog("Select parameters JSON file", OpenDialog.getLastDirectory(), "*.json");
					String dir=od.getDirectory();
					String name=od.getFileName();
					if(dir!=null && name!=null) paramsJsonPath=dir+name;
				}
				loadParametersFromJson(paramsJsonPath);
			}
			p=(float) gd.getNextNumber();
			minimumSize=(float) gd.getNextNumber();
			minimumFrames=(float) gd.getNextNumber();
			useBidirectional=gd.getNextBoolean();
			decisionThreshold=(float) gd.getNextNumber();
			
			addToManager=gd.getNextBoolean();
			simplifyTracks=gd.getNextBoolean();
			clearManager=gd.getNextBoolean();
			showKymo=gd.getNextBoolean();
			showOverlay=gd.getNextBoolean();
			openLocalTables=gd.getNextBoolean();
			batchMode=gd.getNextBoolean();
			batchRecursive=gd.getNextBoolean();
			batchShowOutputs=gd.getNextBoolean();
			improveBeforeAnalysis=gd.getNextBoolean();
			improveStart=(int) gd.getNextNumber();
			improveStop=(int) gd.getNextNumber();
			
			Prefs.set("KymoButler_useLocal.boolean", true);
			storePreferences();
			
			if(!batchMode && ip==null) {
				IJ.showMessage("Nothing to do, please open an image first");
				return;
			}
			
			if(batchMode) {
				runBatch();
			}else {
				runAnalysis();
			}
		}
	}
	
	/**
	 * Stores preferences, based on the user input
	 */
	public void storePreferences() {
		Prefs.set("KymoButler_useLocal.boolean", true);
		Prefs.set("KymoButler_p.double", p);
		Prefs.set("KymoButler_minimumSize.double", minimumSize);
		Prefs.set("KymoButler_minimumFrames.double", minimumFrames);
		Prefs.set("KymoButler_useBidirectional.boolean", useBidirectional);
		Prefs.set("KymoButler_decisionThreshold.double", decisionThreshold);
		Prefs.set("KymoButler_addToManager.boolean", addToManager);
		Prefs.set("KymoButler_simplifyTracks.boolean", simplifyTracks);
		Prefs.set("KymoButler_clearManager.boolean", clearManager);
		Prefs.set("KymoButler_showKymo.boolean", showKymo);
		Prefs.set("KymoButler_showOverlay.boolean", showOverlay);
		Prefs.set("KymoButler_allowCorrections.boolean", allowCorrections);
		Prefs.set("KymoButler_openLocalTables.boolean", openLocalTables);
		Prefs.set("KymoButler_batchMode.boolean", batchMode);
		Prefs.set("KymoButler_batchRecursive.boolean", batchRecursive);
		Prefs.set("KymoButler_batchShowOutputs.boolean", batchShowOutputs);
		Prefs.set("KymoButler_improveBeforeAnalysis.boolean", improveBeforeAnalysis);
		Prefs.set("KymoButler_improveStart.double", improveStart);
		Prefs.set("KymoButler_improveStop.double", improveStop);
		Prefs.set("KymoButler_loadParamsFromJson.boolean", loadParamsFromJson);
		Prefs.set("KymoButler_paramsJsonPath.string", paramsJsonPath==null?"":paramsJsonPath);
	}
	
	/**
	 * Launches analysis once all parameters have been set, returns all images and ROIs
	 */
	public void runAnalysis() {
		if(showKymo || showOverlay || addToManager) {
			long analysisStart=System.currentTimeMillis();
			IJ.log("[KymoButler] Analysis started");
			Calibration cal=ip.getCalibration();
			
			ImagePlus analysisImage=ip;
			if(improveBeforeAnalysis) {
				long t0=System.currentTimeMillis();
				IJ.log("[KymoButler] Step: Improve Kymo");
				analysisImage=ip.duplicate();
				analysisImage.setTitle(ip.getTitle());
				if(ip.getOriginalFileInfo()!=null) analysisImage.setFileInfo(ip.getOriginalFileInfo());
				KymoButler_ImproveKymo.apply(analysisImage, improveStart, improveStop);
				IJ.log("[KymoButler] Step complete: Improve Kymo ("+elapsedMs(t0)+" ms)");
			}
			
			long t1=System.currentTimeMillis();
			IJ.log("[KymoButler] Step: Prepare input");
			kbio.setKymograph(analysisImage);
			kbio.setThreshold(p);
			kbio.setMinimumSize(minimumSize);
			kbio.setMinimumFrames(minimumFrames);
			IJ.log("[KymoButler] Step complete: Prepare input ("+elapsedMs(t1)+" ms)");
			
			long t2=System.currentTimeMillis();
			IJ.log("[KymoButler] Step: Run local analysis");
			String response=kbio.getAnalysisResults();
			IJ.log("[KymoButler] Step complete: Run local analysis ("+elapsedMs(t2)+" ms)");
			
			if(response==null) {
				IJ.showStatus("Process cancelled, either by server or by user");
			}else {
				if(KymoButlerResponseParser.isJSON(response)){
					long t3=System.currentTimeMillis();
					IJ.log("[KymoButler] Step: Parse response");
					KymoButlerResponseParser pkr=new KymoButlerResponseParser(response);

					/** Check if KB returns an error before running parsing*/
					if(pkr.hasError()){
						if(pkr.hasMessages()){ 
							IJ.log(pkr.getMessages());
							IJ.showMessage("KymoButler", pkr.getMessages());
						}else{
							IJ.log("Undefined Error!");	
							IJ.showMessage("KymoButler", "Undefined error while parsing local response.");
						}		
					}else{	
						long outStep=System.currentTimeMillis();
						if(addToManager) pkr.pushRoisToRoiManager(simplifyTracks, clearManager);
						if(showKymo) pkr.showKymograph(cal);
						if(showOverlay) pkr.showOverlay(cal);

						if(addToManager && allowCorrections) {
							IJ.showStatus("Local mode: corrections upload is not available.");
						}
						IJ.log("[KymoButler] Step complete: Render outputs ("+elapsedMs(outStep)+" ms)");
					
					
						if(debug && pkr.hasSomethingToLog()) IJ.log(pkr.getSomethingToLog());
					}
					IJ.log("[KymoButler] Step complete: Parse response ("+elapsedMs(t3)+" ms)");
				}else {
					IJ.log("The response doesn't seem to be properly formatted");
					IJ.showMessage("KymoButler", "Invalid JSON response: unable to parse local output.");
				}
			}
			
			if(debug) kbio.saveResults(response, IJ.getDirectory("imageJ")+(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()))+"_debug_KymoButler.json");			
			if(kbio.isLocalMode() && kbio.getLastOutputDir()!=null) {
				IJ.log("Local outputs saved to: "+kbio.getLastOutputDir());
				saveParameterLog(kbio.getLastOutputDir(), analysisStart, System.currentTimeMillis());
				if(openLocalTables) openLocalTables();
			}
			
			if(analysisImage!=ip) analysisImage.close();
			IJ.log("[KymoButler] Analysis finished ("+elapsedMs(analysisStart)+" ms)");
		}else {
			IJ.showStatus("Nothing to do, please check at least one option");
		}
	}
	
	private void openLocalTables() {
		String tracksPath=kbio.getLastTracksCsvPath();
		String pprocPath=kbio.getLastPprocTablePath();
		
		openResultsTable(tracksPath, "KymoButler Tracks");
		openResultsTable(pprocPath, "KymoButler PProc");
	}
	
	private void openResultsTable(String path, String title) {
		if(path==null) return;
		try {
			ResultsTable rt=ResultsTable.open(path);
			if(rt!=null) rt.show(title);
		} catch (Exception e) {
			IJ.log("Unable to open results table: "+path);
		}
	}
	
	private void runBatch() {
		long batchStart=System.currentTimeMillis();
		DirectoryChooser dc=new DirectoryChooser("Select folder with images");
		String folder=dc.getDirectory();
		if(folder==null) return;
		
		java.util.List<java.io.File> files=new java.util.ArrayList<java.io.File>();
		collectFiles(new java.io.File(folder), batchRecursive, files);
		
		if(files.isEmpty()) {
			IJ.showMessage("Batch mode", "No files found in:\n"+folder);
			return;
		}
		
		IJ.showStatus("Batch started: "+files.size()+" image(s)");
		IJ.showProgress(0);
		
		boolean origShowKymo=showKymo;
		boolean origShowOverlay=showOverlay;
		boolean origOpenTables=openLocalTables;
		
		if(!batchShowOutputs) {
			showKymo=false;
			showOverlay=false;
			openLocalTables=false;
		}
		
		for(int i=0; i<files.size(); i++) {
			java.io.File file=files.get(i);
			long fileStart=System.currentTimeMillis();
			IJ.showStatus("Batch "+(i+1)+"/"+files.size()+": "+file.getName());
			IJ.showProgress(i, files.size());
			ImagePlus img=IJ.openImage(file.getAbsolutePath());
			if(img==null) {
				IJ.log("Batch skipped (unsupported format): "+file.getAbsolutePath());
				continue;
			}
			
			ip=img;
			runAnalysis();
			img.close();
			IJ.log("[KymoButler] Batch item done: "+file.getName()+" ("+elapsedMs(fileStart)+" ms)");
		}
		
		showKymo=origShowKymo;
		showOverlay=origShowOverlay;
		openLocalTables=origOpenTables;
		
		IJ.showProgress(1);
		IJ.showStatus("Batch complete: "+files.size()+" image(s)");
		IJ.log("[KymoButler] Batch complete ("+elapsedMs(batchStart)+" ms)");
	}

	private long elapsedMs(long startMs) {
		return System.currentTimeMillis()-startMs;
	}

	private void applyTooltips(NonBlockingGenericDialog gd) {
		@SuppressWarnings("rawtypes")
		Vector numFields=gd.getNumericFields();
		if(numFields!=null && numFields.size()>=6) {
			attachHoverHint((java.awt.Component)numFields.get(0), "Threshold: higher values reduce false positives but may miss weak tracks.");
			attachHoverHint((java.awt.Component)numFields.get(1), "Minimum size: minimum spatial displacement (pixels) to accept a track.");
			attachHoverHint((java.awt.Component)numFields.get(2), "Minimum frames: minimum time points required for one track.");
			attachHoverHint((java.awt.Component)numFields.get(3), "Decision threshold for bidirectional model classification.");
			attachHoverHint((java.awt.Component)numFields.get(4), "Improve start scale for preprocessing.");
			attachHoverHint((java.awt.Component)numFields.get(5), "Improve stop scale for preprocessing.");
		}
		@SuppressWarnings("rawtypes")
		Vector checks=gd.getCheckboxes();
		if(checks!=null) {
			for(int i=0; i<checks.size(); i++) {
				java.awt.Component c=(java.awt.Component) checks.get(i);
				if(i==0) attachHoverHint(c, "Load parameters from a saved JSON before running.");
				if(i==1) attachHoverHint(c, "Enable bidirectional model in local KymoButler.");
				if(i==2) attachHoverHint(c, "Add detected tracks to ROI Manager.");
				if(i==3) attachHoverHint(c, "Simplify tracks by removing redundant points.");
				if(i==4) attachHoverHint(c, "Clear ROI Manager before adding new tracks.");
				if(i==5) attachHoverHint(c, "Show reconstructed kymograph image.");
				if(i==6) attachHoverHint(c, "Show color overlay output.");
				if(i==7) attachHoverHint(c, "Open local CSV result tables automatically.");
				if(i==8) attachHoverHint(c, "Process all images in a selected folder.");
				if(i==9) attachHoverHint(c, "Include subfolders during batch mode.");
				if(i==10) attachHoverHint(c, "Display image outputs during batch runs.");
				if(i==11) attachHoverHint(c, "Apply Improve Kymo preprocessing before analysis.");
			}
		}
	}

	private void attachHoverHint(final java.awt.Component c, final String msg) {
		if(c==null || msg==null) return;
		c.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				IJ.showStatus("[Hint] "+msg);
				showHoverHint(e, msg);
			}
			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				hideHoverHint();
			}
		});
	}

	private void showHoverHint(java.awt.event.MouseEvent e, String msg) {
		try {
			if(hoverHintDialog==null) {
				hoverHintDialog=new java.awt.Dialog((java.awt.Frame)null);
				hoverHintDialog.setModal(false);
				hoverHintDialog.setUndecorated(true);
				hoverHintLabel=new java.awt.Label();
				hoverHintLabel.setBackground(new java.awt.Color(255, 255, 220));
				hoverHintDialog.add(hoverHintLabel);
				hoverHintDialog.setAlwaysOnTop(true);
			}
			hoverHintLabel.setText(" "+msg+" ");
			hoverHintDialog.pack();
			java.awt.Point p=e.getLocationOnScreen();
			hoverHintDialog.setLocation(p.x+12, p.y+18);
			hoverHintDialog.setVisible(true);
		} catch (Exception ex) {
			IJ.log("[KymoButler] Hover hint unavailable.");
		}
	}

	private void hideHoverHint() {
		if(hoverHintDialog!=null) hoverHintDialog.setVisible(false);
	}

	private void loadParametersFromJson(String path) {
		if(path==null || path.trim().isEmpty()) {
			IJ.log("[KymoButler] Parameter JSON path is empty; skipping load.");
			return;
		}
		try {
			String txt=FileUtils.readFileToString(new java.io.File(path), "UTF-8");
			JSONObject j=new JSONObject(txt);
			if(j.has("threshold")) p=(float) j.getDouble("threshold");
			if(j.has("minimumSize")) minimumSize=(float) j.getDouble("minimumSize");
			if(j.has("minimumFrames")) minimumFrames=(float) j.getDouble("minimumFrames");
			if(j.has("useBidirectional")) useBidirectional=j.getBoolean("useBidirectional");
			if(j.has("decisionThreshold")) decisionThreshold=(float) j.getDouble("decisionThreshold");
			if(j.has("addToManager")) addToManager=j.getBoolean("addToManager");
			if(j.has("simplifyTracks")) simplifyTracks=j.getBoolean("simplifyTracks");
			if(j.has("clearManager")) clearManager=j.getBoolean("clearManager");
			if(j.has("showKymo")) showKymo=j.getBoolean("showKymo");
			if(j.has("showOverlay")) showOverlay=j.getBoolean("showOverlay");
			if(j.has("openLocalTables")) openLocalTables=j.getBoolean("openLocalTables");
			if(j.has("batchMode")) batchMode=j.getBoolean("batchMode");
			if(j.has("batchRecursive")) batchRecursive=j.getBoolean("batchRecursive");
			if(j.has("batchShowOutputs")) batchShowOutputs=j.getBoolean("batchShowOutputs");
			if(j.has("improveBeforeAnalysis")) improveBeforeAnalysis=j.getBoolean("improveBeforeAnalysis");
			if(j.has("improveStart")) improveStart=j.getInt("improveStart");
			if(j.has("improveStop")) improveStop=j.getInt("improveStop");
			IJ.log("[KymoButler] Parameters loaded from: "+path);
		} catch (Exception e) {
			IJ.log("[KymoButler] Unable to load parameters JSON: "+path);
			IJ.showMessage("KymoButler", "Could not load parameters JSON.\nUsing dialog values instead.");
		}
	}

	private void saveParameterLog(String outputDir, long startMs, long endMs) {
		try {
			JSONObject j=new JSONObject();
			j.put("timestamp", new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
			j.put("analysisDurationMs", endMs-startMs);
			j.put("threshold", p);
			j.put("minimumSize", minimumSize);
			j.put("minimumFrames", minimumFrames);
			j.put("useBidirectional", useBidirectional);
			j.put("decisionThreshold", decisionThreshold);
			j.put("addToManager", addToManager);
			j.put("simplifyTracks", simplifyTracks);
			j.put("clearManager", clearManager);
			j.put("showKymo", showKymo);
			j.put("showOverlay", showOverlay);
			j.put("openLocalTables", openLocalTables);
			j.put("batchMode", batchMode);
			j.put("batchRecursive", batchRecursive);
			j.put("batchShowOutputs", batchShowOutputs);
			j.put("improveBeforeAnalysis", improveBeforeAnalysis);
			j.put("improveStart", improveStart);
			j.put("improveStop", improveStop);
			j.put("imageTitle", ip!=null?ip.getTitle():"");
			java.io.File out=new java.io.File(outputDir, "kymobutler_parameters_log.json");
			FileUtils.writeStringToFile(out, j.toString(2), "UTF-8");
			IJ.log("[KymoButler] Parameters log saved: "+out.getAbsolutePath());
		} catch (Exception e) {
			IJ.log("[KymoButler] Unable to save parameters log JSON.");
		}
	}
	
	private void collectFiles(java.io.File dir, boolean recursive, java.util.List<java.io.File> out) {
		if(dir==null || !dir.exists()) return;
		java.io.File[] entries=dir.listFiles();
		if(entries==null) return;
		for(java.io.File entry : entries) {
			if(entry.isDirectory()) {
				if(recursive) collectFiles(entry, true, out);
			}else {
				out.add(entry);
			}
		}
	}
}
