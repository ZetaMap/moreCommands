package util;

import java.util.ArrayList;

import arc.Core;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;


public class AntiVpn {
	public static ArrayList<String> vpnServersList = new ArrayList<>();
	public static boolean isEnabled = false, vpnFileFound = true, fullLoaded = false;
	public static String apiToken = "";
	
	private boolean foundVpn = false;

	private AntiVpn() {
	}
	
	
	public static boolean checkIP(String ip) {
	    if (ip == null) throw new IllegalArgumentException("ip can't be null");
	  
		AntiVpn test = new AntiVpn();
		
		if (isEnabled) 
			Http.get("https://vpnapi.io/api/" + ip + (apiToken.isBlank() ? "" : "?key=" + apiToken), s -> {
				Jval content = Jval.read(s.getResultAsString());
				
				if (!content.has("security")) throw new Exception(content.getString("message"));
				test.foundVpn = content.get("security").get("vpn").asBool();
				
			}, f -> {
				Log.warn("Anti VPN: An error occurred while checking the player's IP");
				Log.warn("Error: " + (f.getLocalizedMessage().contains("error: 429") ? 
				    "Daily limit reached. Please enter an API key." : f.getLocalizedMessage()));
				
				if (vpnFileFound) {
					Log.debug("The search will be done by the reference file (less reliable).");
					test.foundVpn = vpnServersList.contains(ip);
				
				} else Log.debug("The reference file was not loaded. The player's IP will therefore not be verified.");
			});

		return test.foundVpn;
	}

	public static void init() { init(false); }
	public static void init(boolean loadSettings) {
	  if (loadSettings) {
		if (Core.settings.has("anti-vpn")) isEnabled = Core.settings.getBool("anti-vpn");
		else Core.settings.put("anti-vpn", isEnabled);
		
		if (Core.settings.has("anti-vpn-token")) apiToken = Core.settings.getString("anti-vpn-token");
		else Core.settings.put("anti-vpn-token", apiToken);
			    
	  }

		if (isEnabled) {
			arc.files.Fi file = Core.files.local("config/ip-vpn-list.txt");
			
	    	if (file.exists()) {
	    		try {
	    			Log.info("Loading anti VPN file...");
	    			
	    			for (Object line : file.readString().lines().toArray()) vpnServersList.add((String) line);
	    			if (vpnServersList.get(0).startsWith("-")) {
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
	    	
	    	Http.get("https://raw.githubusercontent.com/ZetaMap/moreCommands/main/ip-vpn-list.txt", s -> {
				file.writeBytes(s.getResult());

				vpnServersList.clear();
				for (Object line : file.readString().lines().toArray()) vpnServersList.add((String) line);
				vpnServersList.remove(0);
				
				Log.info("File upload successful!");
				fullLoaded = true;
			}, f -> {
				Log.err("The anti VPN file could not be downloaded from the web! Searches will therefore only be done by web...");
				vpnFileFound = false;
			});
		}
    }
	
	public static void saveSettings() {
		Core.settings.put("anti-vpn", isEnabled);
		Core.settings.put("anti-vpn-token", apiToken);
	}
}
