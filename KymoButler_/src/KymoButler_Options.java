/**
*
*  KymoButler_Options.java, 20 juil. 2019
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

import KymoButler.KymoButlerIO;
import ij.IJ;
import ij.Prefs;
import ij.gui.NonBlockingGenericDialog;
import ij.plugin.PlugIn;

/**
 * This class is aimed at tuning wome options used by the KymoButler for ImageJ plugin
 * @author Fabrice P. Cordelieres
 *
 */
public class KymoButler_Options implements PlugIn{
	/** Use local Wolfram Engine **/
	boolean useLocal=true;
	
	/** WolframScript path **/
	String wolframScriptPath=Prefs.get("KymoButler_wolframscript.string", "wolframscript");
	
	/** Local KymoButler path **/
	String localKymoButlerPath=Prefs.get("KymoButler_localPath.string", "");
	
	/** Local output directory **/
	String localOutputDir=Prefs.get("KymoButler_outputDir.string", System.getProperty("java.io.tmpdir"));
	
	/** Target device **/
	String targetDevice=Prefs.get("KymoButler_targetDevice.string", "GPU");
	
	/** Use physical units in postprocessing **/
	boolean pprocUsePhysical=Prefs.get("KymoButler_pprocUsePhysical.boolean", true);
	
	/** Debug tag: true to save JSON in IJ installation folder **/
	boolean debug=Prefs.get("KymoButler_debug.boolean", false);
	
	/**
	 * Displays the GUI and stores the parameters
	 */
	@Override
	public void run(String arg) {
		if(localKymoButlerPath==null || localKymoButlerPath.trim().isEmpty()) {
			localKymoButlerPath=KymoButlerIO.guessLocalKymoPath();
		}
		NonBlockingGenericDialog gd=new NonBlockingGenericDialog("KymoButler for ImageJ");
		gd.addMessage("Cloud mode is deprecated. Local mode is always enabled.");
		gd.addStringField("WolframScript_path", wolframScriptPath, 30);
		gd.addStringField("KymoButler_local_path", localKymoButlerPath, 30);
		gd.addStringField("Local_output_directory", localOutputDir, 30);
		gd.addChoice("Target_device", new String[] {"GPU","CPU"}, targetDevice);
		gd.addCheckbox("PProc_use_physical_units", pprocUsePhysical);
		gd.addCheckbox("Debug_mode (default: false)", debug);
		gd.showDialog();
		
		if(gd.wasOKed()) {
			useLocal=true;
			wolframScriptPath=gd.getNextString();
			localKymoButlerPath=gd.getNextString();
			localOutputDir=gd.getNextString();
			targetDevice=gd.getNextChoice();
			pprocUsePhysical=gd.getNextBoolean();
			debug=gd.getNextBoolean();
			
			storePreferences();
			
			if(useLocal && (localKymoButlerPath==null || localKymoButlerPath.trim().isEmpty() 
					|| !(new java.io.File(localKymoButlerPath, "packages"+java.io.File.separator+"KymoButler.wl").exists()))) {
				IJ.showMessage("KymoButler local path is invalid", 
						"Couldn't find packages/KymoButler.wl under:\n"+localKymoButlerPath+
						"\n\nSet a valid path in KymoButler Options.");
			}
		}
	}
	
	/**
	 * Stores preferences, based on the user input
	 */
	public void storePreferences() {
		Prefs.set("KymoButler_useLocal.boolean", true);
		Prefs.set("KymoButler_wolframscript.string", wolframScriptPath);
		Prefs.set("KymoButler_localPath.string", localKymoButlerPath);
		Prefs.set("KymoButler_outputDir.string", localOutputDir);
		Prefs.set("KymoButler_targetDevice.string", targetDevice);
		Prefs.set("KymoButler_pprocUsePhysical.boolean", pprocUsePhysical);
		Prefs.set("KymoButler_debug.boolean", debug);
	}
}
