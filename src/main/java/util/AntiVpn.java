package util;

import java.util.ArrayList;

import arc.Core;
import arc.util.Log;
import arc.util.serialization.Jval;


public class AntiVpn {
	public static ArrayList<String> vpnServersList = new ArrayList<>();
	public static int timesLeft = 10, timesLimit = timesLeft;
	public static boolean isEnabled = false, vpnFileFound = true, fullLoaded = false;
	
	private boolean foundVpn = false;

	private AntiVpn() {
	}
	
	
	public static boolean checkIP(String ip) {
		AntiVpn test = new AntiVpn();
		
		if (isEnabled) 
			Core.net.httpGet("https://vpnapi.io/api/" + ip, s -> {
				Jval content = Jval.read(s.getResultAsString());
				
				if (content.get("security") != null) test.foundVpn = content.get("security").asArray().get(0).asBool();
				timesLeft = timesLimit;
				
			}, f -> {
				Log.err("Anti VPN: An error occurred while finding or processing the player's IP address."
					+ "\nError: " + f.getMessage());
				
				if (vpnFileFound) {
					Log.info("The search will be done by the reference file (less reliable).");
					test.foundVpn = vpnServersList.contains(ip);
					timesLeft = timesLimit;
				
				} else {
					Log.err("The reference file was not loaded. The player's IP will therefore not be verified.");
					
					if (timesLeft++ == timesLimit) {
						Log.warn("The unsuccessful search limit has been reached. Anti VPN will be deactivated...");
						isEnabled = false;
					
					} else Log.warn("If this happens another '@' times, the anti VPN will be disabled!", timesLimit-timesLeft);
				}	
			});

		return test.foundVpn;
	}

	public static void init() { init(false); }
	public static void init(boolean loadSettings) {
		if (loadSettings && Core.settings.has("AntiVpn")) {
			try {
				String[] temp = Core.settings.getString("AntiVpn").split(" \\| ");
				isEnabled = Boolean.parseBoolean(temp[0]);
				timesLimit = Integer.parseInt(temp[1]);
				
			} catch (Exception e) { saveSettings(); }
		}
		
		if (isEnabled) {
			arc.files.Fi file = Core.files.local("config/ip-vpn-list.txt");
			
	    	if (file.exists()) {
	    		try {
	    			Log.info("Loading anti VPN file...");
	    			
	    			for (Object line : file.readString().lines().toArray()) vpnServersList.add((String) line);
	    			if (vpnServersList.get(0).equals("### Vpn servers list ###")) {
	    				vpnServersList.remove(0);
	    				
	    				fullLoaded = true;
	    				Log.info("File loaded!");
	    				return;
	    			
	    			} else {
	    				vpnServersList.clear();
	    				Log.warn("You have an old version of the file, downloading the new file...");
	    			}
	    		} catch (Exception e) { Log.err("The anti VPN file could not be load! Try to download the file..."); }
	    	
	    	} else {
	    		Log.err("The anti VPN file was not found! Downloading the file from the web...");
	    		try { file.file().createNewFile(); } 
	    		catch (java.io.IOException e) {}
	    	}
	    	
			Core.net.httpGet("https://raw.githubusercontent.com/ZetaMap/moreCommands/main/ip-vpn-list.txt", s -> {
				file.writeBytes(s.getResult());
				Object[] list = file.readString().lines().toArray();
				for (Object line : list) vpnServersList.add((String) line);
				
				Log.info("File upload successful!");
				fullLoaded = true;
			}, f -> {
				Log.err("The anti VPN file could not be downloaded from the web! Searches will therefore only be done by web...");
				vpnFileFound = false;
			});
		}
    }
	
	public static void saveSettings() {
		Core.settings.put("AntiVpn", isEnabled + " | " + timesLimit);
	}
}
