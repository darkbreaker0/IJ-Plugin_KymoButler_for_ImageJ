/**
*
*  KymoButlerIO.java, 20 juil. 2019
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

package KymoButler;

import java.awt.Polygon;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.plugin.frame.RoiManager;

/**
 * This class is aimed at pushing a kymograph to the KymoButler cloud, and retrieving both an image with tracks overlayed 
 * together with a table containing all detected tracks
 * @author Fabrice P. Cordelieres
 *
 */
public class KymoButlerIO{
	/** KymoButler API URL **/
	String URL=Prefs.get("KymoButler_URL.string", "");
	
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
	
	/** Use bidirectional model **/
	boolean useBidirectional=Prefs.get("KymoButler_useBidirectional.boolean", false);
	
	/** Bidirectional decision threshold **/
	double decisionThreshold=Prefs.get("KymoButler_decisionThreshold.double", 0.5);
	
	/** Use physical units in postprocessing **/
	boolean pprocUsePhysical=Prefs.get("KymoButler_pprocUsePhysical.boolean", true);
	
	//Image to be processed, as a byte array
	byte[] img=null;
	
	/** Current image for local processing **/
	ImagePlus currentImage=null;
	
	/** Local output directory used by last run **/
	String lastOutputDir=null;
	
	/** Local processing calibration **/
	double timeSize=1.0;
	double spaceSize=1.0;
	
	//Parameter p (Threshold), default value 0.2
	String p="0.2";
	
	//Parameter minimumSize, default value 3
	String minimumSize="3";
	
	//Parameter minimumFrames, default value 3
	String minimumFrames="3";
	
	//Parameter tracks, default null
	String tracks=null;
	
	//Parameter simplifyTracks, default value true
	boolean simplifyTracks=true;
	
	/** Stores the time at which the analysis was started**/
	long startTime=(long) 0;
	
	/** The server timout response (default: 2 minutes) **/
	long timeOut=(long) Prefs.get("KymoButler_timeOut.double", 120000);
	
	/** The http POST request **/
	HttpPost httpPost =null;
	
	/** The http response **/
	HttpResponse response=null;
	
	/** The server response, as a JSON object containing the kymograph image, the overlay image and the tracks as a CSV formatted string **/
	JSONObject result;
	
	/** Keeps track of the user pressing the escape key: will cancel all the process **/
	boolean escPressed=false;
	
	/** Debug tag: true to save JSON in IJ installation folder **/
	static boolean debug=Prefs.get("KymoButler_debug.boolean", false);
	
	
	
	
	/**
	 * Builds a new KymoButlerIO object (a kymograph should be set before launching analysis)
	 */
	public KymoButlerIO(){}
	
	/**
	 * Sets the kymograph: should be called before analysis takes place.
	 * It takes the active image as the ImagePlus to analyse
	 */
	public void setCurrentImageAsKymograph() {
		setKymograph(WindowManager.getCurrentImage());
	}
	
	/**
	 * Sets the kymograph: should be called before analysis takes place
	 * @param imagePath a String containing the path to the kymograph to analyse
	 */
	public void setKymograph(String imagePath) {
		try {
			img = FileUtils.readFileToByteArray(new File(imagePath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			IJ.log("Something went wrong when trying to load the image: please check path ("+imagePath+") and file format");
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the kymograph: should be called before analysis takes place
	 * @param ip an ImagePlus containing the kymograph to analyse
	 */
	public void setKymograph(ImagePlus ip) {
		currentImage=ip;
		if(ip!=null && ip.getCalibration()!=null) {
			if(ip.getCalibration().frameInterval>0) timeSize=ip.getCalibration().frameInterval;
			if(ip.getCalibration().pixelWidth>0) spaceSize=ip.getCalibration().pixelWidth;
		}
		updateOutputDirFromImage(ip);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			boolean isVisible=ip.isVisible();
			if(!isVisible) ip.show();
			ImageIO.write(ip.getBufferedImage(), "bmp", baos ); //Is not working if using tif...
			if(!isVisible) ip.hide();
			baos.flush();
			img= baos.toByteArray();
			baos.close();
		} catch (IOException e) {
			IJ.log("Something went wrong when turning the input ImagePlus to a byte array");
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the URL: should be called before analysis takes place
	 * @param URL the URL, as a String
	 */
	public void setURL(String URL) {
		this.URL=URL;
	}
	
	/**
	 * Returns the current URL parameter, as a String
	 * @return the current URL parameter, as a String
	 */
	public String getURL() {
		return URL;
	}
	
	/**
	 * Sets the threshold: should be called before analysis takes place
	 * @param threshold the detection threshold, as a float
	 */
	public void setThreshold(float threshold) {
		p=""+threshold;
	}
	
	/**
	 * Returns the current threshold parameter, as a float
	 * @return the current threshold parameter, as a float
	 */
	public float getThreshold() {
		return Float.parseFloat(p);
	}
	
	/**
	 * Sets the minimum track size: should be called before analysis takes place
	 * @param minimumSize the minimum expected number of pixels traveled for a track to be detected, as a float
	 */
	public void setMinimumSize(float minimumSize) {
		this.minimumSize=""+minimumSize;
	}
	
	/**
	 * Returns the current minimumSize parameter, as a float
	 * @return the current minimumSize parameter, as a float
	 */
	public float getMinimumSize() {
		return Float.parseFloat(minimumSize);
	}
	
	/**
	 * Sets the minimum frame number: should be called before analysis takes place
	 * @param minimumFrames the minimum expected number of frames composing a track for a track to be detected, as a float
	 */
	public void setMinimumFrames(float minimumFrames) {
		this.minimumFrames=""+minimumFrames;
	}
	
	/**
	 * Returns the current minimumFrames parameter, as a float
	 * @return the current minimumFrames parameter, as a float
	 */
	public float getMinimumFrames() {
		return Float.parseFloat(minimumFrames);
	}
	
	
	/**
	 * Sets the server's response timeout 
	 * @param timeOut the server's timeout, in seconds
	 */
	public void setTimeout(int timeOut) {
		this.timeOut=timeOut*1000;
	}
	
	/**
	 * Returns the server's response timeout, in seconds
	 */
	public int getTimeout() {
		return (int) timeOut/1000;
	}
	
	/**
	 * Extracts the rois from the ROI Manager as a JSON segment and stores them for further analysis
	 */
	public void setTracks() {
		tracks=roiManagerToJSON();
	}
	
	/**
	 * Converts the ROIs set to a JSON segment and stores them for further analysis
	 * @param rois the ROIs set to convert
	 */
	public void setTracks(Roi[] rois) {
		tracks=roiSetToJSON(rois);
	}
	
	/**
	 * Returns the content of the ROI Manager as a String, JSON formatted segment 
	 */
	public String getTracks() {
		return tracks;
	}
	
	/**
	 * Requests the server to send back some usage statistics about the KymoButler API
	 * @return a String JSON formatted, containing the response (messages, MaxKymograph, KymographsLeft)
	 */
	public String getStatistics() {
		IJ.log("Cloud mode is deprecated. Statistics endpoint is disabled.");
		IJ.showMessage("KymoButler", "Cloud mode is deprecated.\nStatistics endpoint is disabled.");
		return "{\"error\":true,\"messages\":\"Cloud mode is deprecated. Statistics endpoint is disabled.\"}";
	}
	
	/**
	 * Legacy cloud statistics request.
	 */
	public String getStatisticsCloud() {
		prepareRequestState();
		MultipartEntityBuilder builder=MultipartEntityBuilder.create()
				.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
				.addTextBody(KymoButlerFields.QUERY_FIELD_TAG, KymoButlerFields.QUERY_STATS_FIELD_TAG);
		HttpEntity multiPartEntity = builder.build();
		
		httpPost = new HttpPost(URL);
		httpPost.setEntity(multiPartEntity);
		
		HttpClient client = HttpClientBuilder.create().build();
		startTime=System.currentTimeMillis();
		
		showMessage();
		
		
		try {
			response=client.execute(httpPost);
			
			IJ.showStatus("Informations retrieved in "+getElapsedTime());
			
			String out=EntityUtils.toString(response.getEntity(), "UTF-8");
			
			httpPost.releaseConnection();
			
			return out;
		
		} catch (IOException e) {
			IJ.log("Something went wrong while sending the request/getting the response to/from the server");
			//e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Pushes the image data and parameters to the KymoButler webapp.
	 * @return a String JSON formatted, containing the response (two images, kymograph and overlay, and the tracks as a CSV-style file)
	 */
	public String getAnalysisResults() {
		refreshLocalPrefs();
		return getAnalysisResultsLocal();
	}
	
	/**
	 * Legacy cloud analysis request.
	 */
	public String getAnalysisResultsCloud() {
		prepareRequestState();
		MultipartEntityBuilder builder=MultipartEntityBuilder.create()
				.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
				.addTextBody(KymoButlerFields.QUERY_FIELD_TAG, KymoButlerFields.QUERY_ANALYSIS_FIELD_TAG)
				.addBinaryBody(KymoButlerFields.KYMOGRAPH_FIELD_TAG, img)
				.addTextBody(KymoButlerFields.THRESHOLD_FIELD_TAG, p)
				.addTextBody(KymoButlerFields.MINIMUM_SIZE_FIELD_TAG, minimumSize)
				.addTextBody(KymoButlerFields.MINIMUM_FRAMES_FIELD_TAG, minimumFrames);
		HttpEntity multiPartEntity = builder.build();
		
		httpPost = new HttpPost(URL);
		httpPost.setEntity(multiPartEntity);
		
		HttpClient client = HttpClientBuilder.create().build();
		startTime=System.currentTimeMillis();
		
		showMessage();
		
		
		try {
			response=client.execute(httpPost);
			
			IJ.showStatus("Analysis performed in "+getElapsedTime());
			
			String out=EntityUtils.toString(response.getEntity(), "UTF-8");
			
			httpPost.releaseConnection();
			
			return out;
		
		} catch (IOException e) {
			IJ.log("Something went wrong while sending the request/getting the response to/from the server");
			//e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns true if local processing is enabled.
	 * @return true if local processing is enabled.
	 */
	public boolean isLocalMode() {
		refreshLocalPrefs();
		return useLocal;
	}
	
	/**
	 * Returns the output directory used during the last local run.
	 * @return the last output directory, or null if none was used.
	 */
	public String getLastOutputDir() {
		return lastOutputDir;
	}
	
	public String getLastTracksCsvPath() {
		if(lastOutputDir==null) return null;
		return new File(lastOutputDir, sanitizeBaseName(currentImage!=null?currentImage.getTitle():"kymograph")+"_tracks_long.csv").getAbsolutePath();
	}
	
	public String getLastPprocTablePath() {
		if(lastOutputDir==null) return null;
		return new File(lastOutputDir, sanitizeBaseName(currentImage!=null?currentImage.getTitle():"kymograph")+"_pproc_table.csv").getAbsolutePath();
	}

	/**
	 * Runs the analysis locally using Wolfram Engine.
	 * @return a JSON string compatible with KymoButlerResponseParser, or null on failure.
	 */
	public String getAnalysisResultsLocal() {
		if(currentImage==null) {
			IJ.log("Local mode: no image set for analysis.");
			return null;
		}
		
		if(localKymoButlerPath==null || localKymoButlerPath.trim().isEmpty()) {
			localKymoButlerPath=guessLocalKymoPath();
		}
		
		if(localKymoButlerPath==null || localKymoButlerPath.trim().isEmpty()) {
			IJ.log("Local mode: KymoButler local path is not set.");
			return null;
		}
		
		File packageFile=new File(localKymoButlerPath, "packages"+File.separator+"KymoButler.wl");
		if(!packageFile.exists()) {
			IJ.log("Local mode: KymoButler.wl not found at "+packageFile.getAbsolutePath());
			return null;
		}
		
		updateOutputDirFromImage(currentImage);
		String timeStamp=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		String baseName=sanitizeBaseName(currentImage.getTitle());
		File sessionDir=new File(localOutputDir, "KymoButlerLocal_"+timeStamp+"_"+baseName);
		if(!sessionDir.exists() && !sessionDir.mkdirs()) {
			IJ.log("Local mode: unable to create output directory "+sessionDir.getAbsolutePath());
			return null;
		}
		
		String inputPath=new File(sessionDir, baseName+"_input.png").getAbsolutePath();
		String responsePath=new File(sessionDir, baseName+"_response.json").getAbsolutePath();
		String overlayPath=new File(sessionDir, baseName+"_overlay.tif").getAbsolutePath();
		String tracksCsvPath=new File(sessionDir, baseName+"_tracks_long.csv").getAbsolutePath();
		String pprocTablePath=new File(sessionDir, baseName+"_pproc_table.csv").getAbsolutePath();
		String pprocHistVPath=new File(sessionDir, baseName+"_pproc_hist_v.png").getAbsolutePath();
		String pprocHistTPath=new File(sessionDir, baseName+"_pproc_hist_t.png").getAbsolutePath();
		String pprocHistDistPath=new File(sessionDir, baseName+"_pproc_hist_dist.png").getAbsolutePath();
		String scriptPath=new File(sessionDir, baseName+"_local.wls").getAbsolutePath();
		
		try {
			ImageIO.write(currentImage.getBufferedImage(), "png", new File(inputPath));
		} catch (IOException e) {
			IJ.log("Local mode: unable to write input image to "+inputPath);
			return null;
		}
		
		String script=buildLocalScript(inputPath, responsePath, overlayPath, tracksCsvPath,
				pprocTablePath, pprocHistVPath, pprocHistTPath, pprocHistDistPath);
		
		try {
			FileUtils.writeStringToFile(new File(scriptPath), script, "UTF-8");
		} catch (IOException e) {
			IJ.log("Local mode: unable to write WolframScript file to "+scriptPath);
			return null;
		}
		
		lastOutputDir=sessionDir.getAbsolutePath();
		
		ProcessBuilder pb=new ProcessBuilder(wolframScriptPath, "-file", scriptPath);
		pb.redirectErrorStream(true);
		
		startTime=System.currentTimeMillis();
		Process proc=null;
		try {
			proc=pb.start();
			logProcessOutput(proc);
			boolean finished=proc.waitFor(timeOut, TimeUnit.MILLISECONDS);
			if(!finished) {
				proc.destroyForcibly();
				IJ.log("Local mode: process timed out.");
				return null;
			}
		} catch (IOException | InterruptedException e) {
			IJ.log("Local mode: failed to run WolframScript.");
			return null;
		}
		
		if(proc.exitValue()!=0) {
			IJ.log("Local mode: WolframScript returned a non-zero status.");
		}
		
		try {
			return FileUtils.readFileToString(new File(responsePath), "UTF-8");
		} catch (IOException e) {
			IJ.log("Local mode: unable to read response file "+responsePath);
			return null;
		}
	}
	
	/**
	 * Pushes the image data and the tracks to the KymoButler webapp to correct and retrain the network.
	 * @return a String JSON formatted, containing the response
	 */
	public String upload() {
		IJ.log("Cloud mode is deprecated. Upload endpoint is disabled.");
		IJ.showMessage("KymoButler", "Cloud mode is deprecated.\nUpload endpoint is disabled.");
		return "{\"error\":true,\"messages\":\"Cloud mode is deprecated. Upload endpoint is disabled.\"}";
	}
	
	/**
	 * Legacy cloud upload request.
	 */
	public String uploadCloud() {
		prepareRequestState();
		MultipartEntityBuilder builder=MultipartEntityBuilder.create()
				.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
				.addTextBody(KymoButlerFields.QUERY_FIELD_TAG, KymoButlerFields.QUERY_UPLOAD_FIELD_TAG)
				.addBinaryBody(KymoButlerFields.KYMOGRAPH_FIELD_TAG, img)
				.addTextBody(KymoButlerFields.TRACKS_FIELD_TAG, tracks);
		HttpEntity multiPartEntity = builder.build();
		
		httpPost = new HttpPost(URL);
		httpPost.setEntity(multiPartEntity);
		
		HttpClient client = HttpClientBuilder.create().build();
		startTime=System.currentTimeMillis();
		
		showMessage();
		
		
		try {
			response=client.execute(httpPost);
			
			IJ.showStatus("Upload performed in "+getElapsedTime());
			
			String out=EntityUtils.toString(response.getEntity(), "UTF-8");
			
			httpPost.releaseConnection();
			
			return out;
		
		} catch (IOException e) {
			IJ.log("Something went wrong while sending the request/getting the response to/from the server");
			//e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Save a string to a file
	 * @param content the String content to save
	 * @param outputPath path to the file where the JSON content will be saved
	 */
	public void saveResults(String content, String outputPath) {
		try {
			FileUtils.writeStringToFile(new File(outputPath), content, "UTF-8");
		} catch (IOException e) {
			IJ.log("Something went wrong while saving the analysis results to the provided path "+outputPath);
			e.printStackTrace();
		}
	}
	
	/**
	 * This methods handles the display of the elapsed time in the status bar. It creates a new thread so that 
	 * display does not interfere with the analysis process while allowing the display to be updated.
	 */
	private void showMessage() {
		Thread t=new Thread() {
			public void run() {
				while(response==null && !escPressed && (System.currentTimeMillis()-startTime)<timeOut) {
					
					if(!IJ.escapePressed()) {
						IJ.showStatus("Process started "+getElapsedTime()+" ago, waiting for response");
					}else {
						httpPost.abort();
						IJ.showStatus("Process cancelled");
						escPressed=true;
					}
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    }
				
				if((System.currentTimeMillis()-startTime)>timeOut)  httpPost.abort();
			}
		};
		t.start();
	}
	
	/**
	 * Computes the elapsed time since the "startTime" as stored in the class variable
	 * @return the elapsed time since startTime as a string, using the mm:ss format
	 */
	private String getElapsedTime() {
		Date elapsedTime=new Date(System.currentTimeMillis()-startTime);
		SimpleDateFormat sdf=new SimpleDateFormat("mm:ss");
		return sdf.format(elapsedTime);
	}

	private void prepareRequestState() {
		response=null;
		escPressed=false;
		IJ.resetEscape();
	}

	private void refreshLocalPrefs() {
		useLocal=true;
		Prefs.set("KymoButler_useLocal.boolean", true);
		wolframScriptPath=Prefs.get("KymoButler_wolframscript.string", "wolframscript");
		localKymoButlerPath=Prefs.get("KymoButler_localPath.string", "");
		localOutputDir=Prefs.get("KymoButler_outputDir.string", System.getProperty("java.io.tmpdir"));
		targetDevice=Prefs.get("KymoButler_targetDevice.string", "GPU");
		useBidirectional=Prefs.get("KymoButler_useBidirectional.boolean", false);
		decisionThreshold=Prefs.get("KymoButler_decisionThreshold.double", 0.5);
		pprocUsePhysical=Prefs.get("KymoButler_pprocUsePhysical.boolean", true);
		updateOutputDirFromImage(currentImage);
	}
	
	private String buildLocalScript(String inputPath, String responsePath, String overlayPath, String tracksCsvPath,
			String pprocTablePath, String pprocHistVPath, String pprocHistTPath, String pprocHistDistPath) {
		StringBuilder sb=new StringBuilder();
		
		sb.append("$HistoryLength=0;\n");
		sb.append("kbPath=\"").append(escapeForWolfram(localKymoButlerPath)).append("\";\n");
		sb.append("inputPath=\"").append(escapeForWolfram(inputPath)).append("\";\n");
		sb.append("responsePath=\"").append(escapeForWolfram(responsePath)).append("\";\n");
		sb.append("overlayPath=\"").append(escapeForWolfram(overlayPath)).append("\";\n");
		sb.append("tracksCsvPath=\"").append(escapeForWolfram(tracksCsvPath)).append("\";\n");
		sb.append("pprocTablePath=\"").append(escapeForWolfram(pprocTablePath)).append("\";\n");
		sb.append("pprocHistVPath=\"").append(escapeForWolfram(pprocHistVPath)).append("\";\n");
		sb.append("pprocHistTPath=\"").append(escapeForWolfram(pprocHistTPath)).append("\";\n");
		sb.append("pprocHistDistPath=\"").append(escapeForWolfram(pprocHistDistPath)).append("\";\n");
		sb.append("p=").append(p).append(";\n");
		sb.append("minSz=").append(minimumSize).append(";\n");
		sb.append("minFr=").append(minimumFrames).append(";\n");
		sb.append("tsz=").append(timeSize).append(";\n");
		sb.append("xsz=").append(spaceSize).append(";\n");
		sb.append("useBi=").append(useBidirectional ? "True" : "False").append(";\n");
		sb.append("vthr=").append(decisionThreshold).append(";\n");
		sb.append("usePhys=").append(pprocUsePhysical ? "True" : "False").append(";\n");
		sb.append("device=\"").append(escapeForWolfram(targetDevice)).append("\";\n");
		sb.append("Get[FileNameJoin[{kbPath,\"packages\",\"KymoButler.wl\"}]];\n");
		sb.append("Get[FileNameJoin[{kbPath,\"packages\",\"KymoButlerPProc.wl\"}]];\n");
		sb.append("models=Quiet[loadDefaultNets[kbPath]];\n");
		sb.append("kym=Import[inputPath];\n");
		sb.append("res=If[useBi,\n");
		sb.append("  BiKymoButler[kym, p, vthr, device, models[\"binet\"], models[\"decnet\"], minSz, minFr],\n");
		sb.append("  UniKymoButler[kym, p, device, models[\"uninet\"], minSz, minFr]\n");
		sb.append("];\n");
		sb.append("If[res===$Failed || Head[res]=!=List,\n");
		sb.append("  Export[responsePath, ExportString[<|\"error\"->True,\"messages\"->\"Local KymoButler processing failed.\"|>,\"JSON\"],\"String\"]; Exit[1];\n");
		sb.append("];\n");
		sb.append("overlay=res[[3]];\n");
		sb.append("If[useBi,\n");
		sb.append("  tracks=res[[5]];\n");
		sb.append("  antrks={}; retrks={};,\n");
		sb.append("  antrks=res[[5]];\n");
		sb.append("  retrks=res[[6]];\n");
		sb.append("  tracks=Join[antrks,retrks];\n");
		sb.append("];\n");
		sb.append("Export[overlayPath, overlay];\n");
		sb.append("makeRows[trks_, dir_, startId_]:=Module[{rows={}, tid=startId},\n");
		sb.append("  Do[rows=Join[rows, Map[{tid, #[[1]], #[[2]], dir} &, trk]]; tid++, {trk, trks}];\n");
		sb.append("  {rows, tid}\n");
		sb.append("];\n");
		sb.append("If[useBi,\n");
		sb.append("  {rowsA,nextId}=makeRows[tracks, \"bidirectional\", 1];\n");
		sb.append("  rows=rowsA;,\n");
		sb.append("  {rowsA,nextId}=makeRows[antrks, \"anterograde\", 1];\n");
		sb.append("  {rowsR,nextId2}=makeRows[retrks, \"retrograde\", nextId];\n");
		sb.append("  rows=Join[rowsA, rowsR];\n");
		sb.append("];\n");
		sb.append("rows=Map[Append[#, #[[2]]*tsz]&, rows];\n");
		sb.append("rows=Map[Append[#, #[[3]]*xsz]&, rows];\n");
		sb.append("rows=Join[{{\"track_id\",\"t\",\"x\",\"dir\",\"t_phys\",\"x_phys\"}}, rows];\n");
		sb.append("Export[tracksCsvPath, rows];\n");
		sb.append("If[!usePhys, tsz=1; xsz=1;];\n");
		sb.append("pp=pprocLocal[tracks, tsz, xsz];\n");
		sb.append("Export[pprocTablePath, pp[[2]]];\n");
		sb.append("Export[pprocHistVPath, pp[[1,1]]];\n");
		sb.append("Export[pprocHistTPath, pp[[1,2]]];\n");
		sb.append("Export[pprocHistDistPath, pp[[1,3]]];\n");
		sb.append("kymoData=ImageData[ColorConvert[res[[1]], \"Grayscale\"]];\n");
		sb.append("overlayData=ImageData[ColorConvert[overlay, \"RGB\"]];\n");
		sb.append("json=ExportString[<|\"Kymograph\"->kymoData,\"overlay\"->overlayData,\"tracks\"->tracks|>,\"JSON\"];\n");
		sb.append("Export[responsePath, json, \"String\"];\n");
		
		return sb.toString();
	}
	
	private void logProcessOutput(Process proc) {
		try (BufferedReader reader=new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
			String line;
			while((line=reader.readLine())!=null) {
				if(!line.trim().isEmpty()) IJ.log(line);
			}
		} catch (IOException e) {
			// ignore logging errors
		}
	}
	
	private String sanitizeBaseName(String title) {
		if(title==null || title.trim().isEmpty()) return "kymograph";
		String base=title;
		int dot=base.lastIndexOf(".");
		if(dot>0) base=base.substring(0, dot);
		return base.replaceAll("[^A-Za-z0-9._-]", "_");
	}
	
	private String escapeForWolfram(String path) {
		if(path==null) return "";
		return path.replace("\\", "\\\\").replace("\"", "\\\"");
	}
	
	private void updateOutputDirFromImage(ImagePlus ip) {
		if(ip==null) return;
		FileInfo info=ip.getOriginalFileInfo();
		if(info!=null && info.directory!=null && !info.directory.trim().isEmpty()) {
			localOutputDir=info.directory;
		}
	}
	
	public static String guessLocalKymoPath() {
		String env=System.getenv("KYMOBUTLER_PATH");
		if(env!=null && !env.trim().isEmpty()) {
			if(new File(env, "packages"+File.separator+"KymoButler.wl").exists()) return env;
		}
		
		String userHome=System.getProperty("user.home");
		String[] candidates=new String[] {
			userHome+File.separator+"KymoButler",
			userHome+File.separator+"KymoButler-master",
			userHome+File.separator+"Desktop"+File.separator+"KymoButler",
			userHome+File.separator+"Desktop"+File.separator+"KymoButler-master",
			userHome+File.separator+"Desktop"+File.separator+"Apps"+File.separator+"KymoButler-master"+File.separator+"KymoButler-master",
			IJ.getDirectory("imageJ")+File.separator+"KymoButler"
		};
		
		for(String candidate : candidates) {
			if(candidate==null) continue;
			File pkg=new File(candidate, "packages"+File.separator+"KymoButler.wl");
			if(pkg.exists()) return candidate;
		}
		
		return "";
	}
	
	/**
	 * Encodes the input Roi as a JSON segment
\t * Example format: nested track coordinates in JSON-like braces.
	 * @param roi the input Roi
	 * @return a String containing the Roi's coordinates encoded as a JSON segment
	 */
	private String roiToJSON(Roi roi) {
		String out="{";
		Polygon pol=roi.getPolygon();
		
		for(int i=0; i<pol.npoints; i++) out+="{"+pol.ypoints[i]+","+pol.xpoints[i]+"}"+(i!=pol.npoints-1?",":"}");
		
		return out;
	}
	
	/**
	 * Encodes the input ROIs set as a JSON segment
\t * Example format: nested track coordinates in JSON-like braces.
	 * @param rois the ROIs set to convert
	 * @return a String containing the Roi's coordinates encoded as a JSON segment
	 */
	private String roiSetToJSON(Roi[] rois) {
		String out="{";
		
		for(int i=0; i<rois.length; i++) out+=roiToJSON(rois[i])+(i!=rois.length-1?",":"}");
		
		return out;
	}
	
	/**
	 * Encodes the content of the RoiManager as a JSON segment
\t * Example format: nested track coordinates in JSON-like braces.
	 * @return a String containing the Roi's coordinates encoded as a JSON segment
	 */
	private String roiManagerToJSON() {
		return roiSetToJSON(RoiManager.getRoiManager().getRoisAsArray());
	}
	
	/**
	 * Checks that the required libraries are installed, and displays an error message if they are not
	 * @return true if all required libraries are installed, false otherwise
	 */
	public static boolean checkForLibraries() {
		/*
		HashMap<String, String> classesToFind=new HashMap<String, String>(){
			private static final long serialVersionUID = 1L;

			{
				put("commons-io-2.6", "org.apache.commons.io.FileUtils");
				put("commons-logging-1.2", "org.apache.commons.logging.Log");
				put("commons-codec-1.11", "org.apache.commons.codec.BinaryDecoder");
				put("httpclient-4.5.9", "org.apache.http.client.HttpClient");
				put("httpcore-4.4.11", "org.apache.http.HttpEntity");
				put("httpmime-4.5.9", "org.apache.http.entity.mime.HttpMultipartMode");
				put("json-20180813", "org.json.JSONObject");
			}
		};
		*/
		
		String[][] jarCandidates = new String[][] {
			{"commons-io-2.6.jar", "commons-io-2.17.0.jar"},
			{"commons-logging-1.2.jar", "commons-logging-1.3.4.jar"},
			{"json-20180813.jar", "json-20240303.jar"}
		};
		
		String msg="";
		String pluginsJars=IJ.getDirectory("plugins")+File.separator+"jars"+File.separator;
		String appJars=IJ.getDirectory("imagej")+File.separator+"jars"+File.separator;
		
		for(String[] candidates : jarCandidates) {
			boolean found=false;
			for(String jar : candidates) {
				if(new File(pluginsJars+jar).exists() || new File(appJars+jar).exists()) {
					found=true;
					break;
				}
			}
			if(debug) IJ.log("Check "+candidates[0]+": "+(found?"":"not ")+"found");
			if(!found) msg=msg+(!msg.isEmpty()?"\n":"")+candidates[0];
		}
		
		if(!msg.isEmpty()) IJ.error("The following libraries are missing:\n"+msg);
		
		return msg.isEmpty();
	}
}
