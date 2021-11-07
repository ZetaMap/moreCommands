package data;

import java.util.ArrayList;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;

import util.AntiVpn;
import util.Strings;


public class BM {
	private static ArrayList<String> bannedClients = new Seq<String>().addAll("VALVE", "tuttop", "CODEX", "IGGGAMES", "IgruhaOrg", "FreeTP.Org").list(),
	    defaultBannedNames = new Seq<String>().addAll("[Server]", "[server]", "@e", "@a", "@p", "@t", "~").list(),
	    bannedIps = new ArrayList<>(),
	    bannedNames = new ArrayList<>();
	
	public static void blacklistCommand(String[] arg) {
		ArrayList<String> list = new ArrayList<>();
    	list.addAll(defaultBannedNames);
    	list.addAll(bannedClients);
    	
    	if (arg[0].equals("list")) {
    		StringBuilder builder = new StringBuilder();
    		
    		if (arg[1].equals("name")) {
    			int best = Strings.bestLength(bannedNames);
        		int max = best > 18+String.valueOf(bannedNames.size()).length() ? best+4 : 23+String.valueOf(bannedNames.size()).length();
        		
        		Log.info("List of banned names:");
        		Log.info(Strings.lJust("| Custom list: Total: " + bannedNames.size(), max) + "  Default list: Total: " + list.size());
        		for (int i=0; i<Math.max(bannedNames.size(), list.size()); i++) {
        			try { builder.append(Strings.lJust("| | " + bannedNames.get(i), max+1)); } 
        			catch (IndexOutOfBoundsException e) { builder.append("|" + Strings.createSpaces(max)); }
        			try { builder.append(" | " + list.get(i)); } 
        			catch (IndexOutOfBoundsException e) {}
        			
        			Log.info(builder.toString());
        			builder = new StringBuilder();
        		}
    		
    		} else if (arg[1].equals("ip")) {
    			int best = Strings.bestLength(bannedIps);
        		int max = best > 18+String.valueOf(bannedIps.size()).length() ? best+4 : 23+String.valueOf(bannedIps.size()).length();

        		Log.info("List of banned ip:");
        		Log.info(Strings.lJust("| Custom list: Total: " + bannedIps.size(), max) + "  Default list: Total: " + AntiVpn.vpnServersList.size() + " (Anti VPN list)");
        		for (int i=0; i<Math.max(bannedIps.size(), AntiVpn.vpnServersList.size()); i++) {
        			try { builder.append(Strings.lJust("| | " + bannedIps.get(i), max+1)); } 
        			catch (IndexOutOfBoundsException e) { 
        				builder.append("|" + Strings.createSpaces(max)); 
        				if (i > 20) break;
        			}
        			try { 
        				if (i == 20) builder.append(" | ...." + (AntiVpn.vpnServersList.size()-i) + " more");
        				else if (i < 20) builder.append(" | " + AntiVpn.vpnServersList.get(i));
        			} catch (IndexOutOfBoundsException e) {}
        			
        			Log.info(builder.toString());
        			builder = new StringBuilder();
        			
        			if (i > 20 && bannedIps.size() < 20) break;
        		}
    		
    		} else Log.err("Invalid argument. possible arguments: name, ip");
    		
    	} else if (arg[0].equals("add")) {
    		if (arg.length == 3) {
    			if (arg[1].equals("name")) {
        			if (arg[2].length() > 40) Log.err("A nickname cannot exceed 40 characters");
        			else if (bannedNames.contains(arg[2])) Log.err("'@' is already in the blacklist", arg[2]);
        			else {
        				bannedNames.add(arg[2]);
        				saveSettings();
        				Log.info("'@' was added to the blacklist", arg[2]);
        			}
        			
        		} else if (arg[1].equals("ip")) {
        			if (arg[2].split("\\.").length != 4 || !Strings.canParseByteList(arg[2].split("\\."))) Log.err("Incorrect format for IPv4");
        			else if (bannedIps.contains(arg[2])) Log.err("'@' is already in the blacklist", arg[2]);
        			else {
        				bannedIps.add(arg[2]);
        				saveSettings();
        				Log.info("'@' was added to the blacklist", arg[2]);
        			}
        			
        		} else Log.err("Invalid argument. possible arguments: name, ip");
    		} else Log.err("Please enter a value");
    	
    	} else if (arg[0].equals("remove")) {
    		if (arg.length == 3) {
    			if (arg[1].equals("name")) {
    				if (!bannedNames.contains(arg[2])) Log.err("'@' isn't in custom blacklist", arg[2]);
        			else if (list.contains(arg[2])) Log.err("You can't remove a name from the default list");	
        			else {
        				bannedNames.remove(arg[2]);
        				saveSettings();
        				Log.info("'@' has been removed from the blacklist", arg[2]);
        			}
        			
        		} else if (arg[1].equals("ip")) {
        			if (arg[2].split("\\.").length != 4 || !Strings.canParseByteList(arg[2].split("\\."))) Log.err("Incorrect format for IPv4");
        			else {
        				bannedIps.remove(arg[2]);
        				saveSettings();
        				Log.info("'@' has been removed from the blacklist", arg[2]);
        			}
        			
        		} else Log.err("Invalid argument. possible arguments: name, ip");
    		} else Log.err("Please enter a value");
    		
    	} else if (arg[0].equals("clear")) {
    		if (arg[1].equals("name")) {
    			bannedNames.clear();
    			saveSettings();
    			Log.info("Name blacklist emptied!");
    			
    		} else if (arg[1].equals("ip")) {
    			bannedIps.clear();
    			saveSettings();
    			Log.info("IP blacklist emptied!");
    			
    		} else Log.err("Invalid argument. possible arguments: name, ip");
    		
    	} else Log.err("Invalid argument. possible arguments: list, add, remove");
	}
	
	
	public static void nameCheck(mindustry.gen.Player player) {
    	String name = TempData.get(player).stripedName;
    	
    	if (bannedNames.contains(name) || defaultBannedNames.contains(name)) 
    		player.kick("[scarlet]Invalid nickname: []Please don't use [accent]'" + bannedNames.get(bannedNames.indexOf(name)) + "'[white] in your nickname.");
    	
    	else if (bannedClients.contains(name)) 
    		player.con.kick("Ingenuine copy of Mindustry.\n\n"
    			+ "Mindustry is free on: [royal]https://anuke.itch.io/mindustry[]\n"
    			+ "Mindustry est gratuit ici : [royal]https://anuke.itch.io/mindustry[]\n");
    	
    	else if (bannedIps.contains(player.con.address))
    		player.kick("[scarlet]The IP you are using is blacklisted. [lightgray](your ip: " + player.ip() +")");
    	
    	else if (AntiVpn.isEnabled && AntiVpn.checkIP(player.ip())) 
    		player.kick("[scarlet]Anti VPN is activated on this server! []Please deactivate your VPN to be able to connect to the server.");	
    }
		
	@SuppressWarnings("unchecked")
	public static void init() {
    	try {
        	if (Core.settings.has("bannedNamesList")) {
        		bannedNames = Core.settings.getJson("bannedNamesList", Seq.class, Seq::new).list(); 
        	} else saveSettings();
        	
        	if (Core.settings.has("bannedIpsList")) {
        		 bannedIps = Core.settings.getJson("bannedIpsList", Seq.class, Seq::new).list(); 
        	} else saveSettings();
        	
    	} catch (Exception e) { saveSettings(); }
    }
    
	public static void saveSettings() {
    	Core.settings.putJson("bannedNamesList", new Seq<String>().addAll(bannedNames));
    	Core.settings.putJson("bannedIpsList", new Seq<String>().addAll(bannedIps));
    }
}
