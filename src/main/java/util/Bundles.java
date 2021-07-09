package util;

import java.util.Locale;

import arc.Core;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.I18NBundle;
import arc.util.Log;

public class Bundles {
	public static boolean dontUseBundle = false;
	private static Locale country = Locale.getDefault();
	private static Fi baseBundle = Core.files.local("config/bundles/bundle.properties");
	private static Fi file = Core.files.local(String.format("config/bundles/bundle_%s.properties", country.getLanguage()));
	private static String bundlesURL = "https://raw.githubusercontent.com/Anuken/Mindustry/master/core/assets/bundles/bundle_%s.properties";
	private static String[] bundleKeys = {"server.closing",
			"server.kicked.kick",
			"server.kicked.whitelist",
			"server.kicked.serverClose",
			"server.kicked.vote",
			"server.kicked.clientOutdated",
			"server.kicked.serverOutdated",
			"server.kicked.banned",
			"server.kicked.typeMismatch",
			"server.kicked.playerLimit",
			"server.kicked.recentKick",
			"server.kicked.nameInUse",
			"server.kicked.nameEmpty",
			"server.kicked.idInUse",
			"server.kicked.customClient",
			"server.kicked.gameover",
			"server.kicked.serverRestarting"};
	
	public static void init() {
		if (findBundle() == null) {
			Log.info("No bundle found for your language. Try to download bundle ...");
			
			Core.net.httpGet(String.format(bundlesURL, country.getLanguage()), reponse -> {
				Log.info("Bundle found! Bundle installation in progress ...");
				file.writeBytes(reponse.getResult());
				String content = searchKeys(file.readString());
				file.writeString(content);
				baseBundle.writeString(content);
				
			}, error -> {
				Log.err("Failed to download bundle in your language!");
				dontUseBundle = true;
				return;
			});
		}
		
		try { Core.bundle = I18NBundle.createBundle(Core.files.local("config/bundles/bundle")); }
		catch (Exception e) { 
			Log.err("Error: An error occurred while loading the bundle! The server will do without translations.");
			dontUseBundle = true;
		} 
	}
	
	private static String searchKeys(String content) {
		Seq<Object> lines = new Seq<>().addAll(content.lines().toArray());
		Seq<String> newContent = new Seq<>();
		
		for (String key : bundleKeys) {
			lines.forEach(line ->  {
				if (((String) line).startsWith(key)) newContent.add((String) line);
			});
		}
		return newContent.toString("\n");
	}
	
	private static Fi findBundle() {
		if (file.exists()) {
			if (file.readString().isBlank()) return null;
			else {
				Seq<Object> lines = new Seq<>().addAll(file.readString().lines().toArray());
				Seq<String> proof = new Seq<>();
				
				for (String key : bundleKeys) {
					for (Object line : lines) {
						if (((String) line).startsWith(key)) {
							proof.add(key);
							break;
						} 
					}
				}
				return proof.size == bundleKeys.length ? file : null;
			}
		}
		try { file.file().createNewFile(); } 
		catch (Exception e) {}
		
		file.writeString("");
		return null;
		
	}
}
