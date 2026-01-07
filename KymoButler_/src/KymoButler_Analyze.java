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

import KymoButler.KymoButlerIO;
import KymoButler.KymoButlerResponseParser;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.io.DirectoryChooser;
import ij.plugin.PlugIn;

/**
 * This class is aimed at launching the analysis of kymographs using the KymoButler webservice
 * @author Fabrice P. Cordelieres
 *
 */
public class KymoButler_Analyze implements PlugIn{
	/** Use local Wolfram Engine **/
	boolean useLocal=Prefs.get("KymoButler_useLocal.boolean", true);
	
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
	
	String helpMsg="<html>Version 1.0.0, 18 nov. 2019<br>"
			+ "This plugin is powered by <a href=\"https://deepmirror.ai/software/kymobutler/\">KymoButler</a><br>"
			+ "a webservice provided by Andrea Dimitracopoulos and Max Jakobs<br>"
			+ "based on their <a href=\"https://doi.org/10.7554/eLife.42288\">publication</a> you should cite when using the website/plugin:<br><br>"
			+ "This plugin heavily relies on external libraries:"
			+ "<ul>"
			+ "	<li>commons-io, v2.6</li>"
			+ "	<li>org.apache.httpcomponents/httpclient, v4.5.9</li>"
			+ "	<li>org.json/json, v20180813</li>"
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
		GenericDialog gd=new GenericDialog("KymoButler for ImageJ");
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
		
		gd.addHelp(helpMsg);
		gd.showDialog();
		
		if(gd.wasOKed()) {
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
	}
	
	/**
	 * Launches analysis once all parameters have been set, returns all images and ROIs
	 */
	public void runAnalysis() {
		if(showKymo || showOverlay || addToManager) {
			Calibration cal=ip.getCalibration();
			
			ImagePlus analysisImage=ip;
			if(improveBeforeAnalysis) {
				analysisImage=ip.duplicate();
				analysisImage.setTitle(ip.getTitle());
				if(ip.getOriginalFileInfo()!=null) analysisImage.setFileInfo(ip.getOriginalFileInfo());
				KymoButler_ImproveKymo.apply(analysisImage, improveStart, improveStop);
			}
			
			kbio.setKymograph(analysisImage);
			kbio.setThreshold(p);
			kbio.setMinimumSize(minimumSize);
			kbio.setMinimumFrames(minimumFrames);
			
			String response=kbio.getAnalysisResults();
			
			if(response==null) {
				IJ.showStatus("Process cancelled, either by server or by user");
			}else {
				if(KymoButlerResponseParser.isJSON(response)){
					KymoButlerResponseParser pkr=new KymoButlerResponseParser(response);

					/** Check if KB returns an error before running parsing*/
					if(pkr.hasError()){
						if(pkr.hasMessages()){ 
							IJ.log(pkr.getMessages());
						}else{
							IJ.log("Undefined Error!");	
						}		
					}else{	
						if(addToManager) pkr.pushRoisToRoiManager(simplifyTracks, clearManager);
						if(showKymo) pkr.showKymograph(cal);
						if(showOverlay) pkr.showOverlay(cal);
					
						if(addToManager && allowCorrections && !kbio.isLocalMode()) {
							WaitForUserDialog wfud= new WaitForUserDialog("Correct and re-train", "From the current detections list you may:"+"\n"
																							+" \n"
																							+ "1-Correct the detections:"+"\n"
																							+ "    a-Click on the track to correct in the ROI Manager"+"\n"
																							+ "    b-Modify the ROI on the image"+"\n"
																							+ "    c-Click 'update' button in the ROI Manager"+"\n"
																							+ "    d-Repeat for all tracks you want to modify"+"\n"
																							+" \n"
																							+ "2-Add detections:"+"\n"
																							+ "    a-Activate the polyline tool"+"\n"
																							+ "    b-Draw the missing track on the image"+"\n"
																							+ "    c-Click 'add' button in the ROI Manager"+"\n"
																							+ "    d-Repeat for all the missing tracks"+"\n"
																							+" \n"
																							+"Once done, please click on Ok"
																							);
							wfud.show();
							new KymoButler_Upload().run(null);
						}else if(addToManager && allowCorrections && kbio.isLocalMode()) {
							IJ.showStatus("Local mode: corrections upload is not available.");
						}
					
					
						if(debug && pkr.hasSomethingToLog()) IJ.log(pkr.getSomethingToLog());
					}
				}else {
					IJ.showStatus("The response doesn't seem to be properly formatted");
				}
			}
			
			if(debug) kbio.saveResults(response, IJ.getDirectory("imageJ")+(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()))+"_debug_KymoButler.json");			
			if(kbio.isLocalMode() && kbio.getLastOutputDir()!=null) {
				IJ.log("Local outputs saved to: "+kbio.getLastOutputDir());
				if(openLocalTables) openLocalTables();
			}
			
			if(analysisImage!=ip) analysisImage.close();
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
		}
		
		showKymo=origShowKymo;
		showOverlay=origShowOverlay;
		openLocalTables=origOpenTables;
		
		IJ.showProgress(1);
		IJ.showStatus("Batch complete: "+files.size()+" image(s)");
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
