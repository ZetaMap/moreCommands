package util;

import arc.Core;
import arc.util.Log;

import data.PVars;


public class ALog {
	private static arc.files.Fi file;
	public static int files = 0, maxSize = 1_048_576;
	public static boolean isEnabled = true;
	
	public static void write(String label, String text, Object... arg) {
		if (isEnabled) {
			file.writeString(Strings.format("[@] [@] @\n", 
				java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss").format(java.time.LocalDateTime.now()), label, Strings.format(text, arg)), true);
			
			if (file.length() > maxSize) {
				Log.warn("Admin logs --> file '@ALog-@.txt reached the limit of 1 MB. Creating new file ...", PVars.ALogPath, files);
				newFile();
			}
		}
	}
	
	public static void init() {
		//init settings
		if (Core.settings.has("ALogFiles")) files = Core.settings.getInt("ALogFiles");
		else Core.settings.put("ALogFiles", files);
		
		if (Core.settings.has("ALogEnabled")) isEnabled = Core.settings.getBool("ALogEnabled");
		else Core.settings.put("ALogEnabled", isEnabled);
		
		if (isEnabled) {
			//load lastest file
			file = Core.files.local(PVars.ALogPath);
			if (!file.exists()) file.mkdirs();
			
			files--;
			newFile();	
		}
	}
	
	public static void saveSettings() {
		Core.settings.put("ALogFiles", files);
		Core.settings.put("ALogEnabled", isEnabled);
	}
	
	private static void newFile() {
		files++;
		file = Core.files.local(PVars.ALogPath + "ALog-" + files + ".txt");
		
		if (!file.exists()) {
			try { file.file().createNewFile(); } 
			catch (java.io.IOException e) { e.printStackTrace(); }
		
		} else if (file.length() > maxSize) {
			Log.warn("Admin logs --> file '@ALog-@.txt reached the limit of 1 MB. Find another file ...", PVars.ALogPath, files);
			newFile();
			return;
		
		} else saveSettings();
		Log.info("Admin logs --> file '@ALog-@.txt' load and ready to write", PVars.ALogPath, files);
	}
}
