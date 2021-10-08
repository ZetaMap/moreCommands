import static mindustry.Vars.content;
import static mindustry.Vars.maps;
import static mindustry.Vars.netServer;
import static mindustry.Vars.state;
import static mindustry.Vars.world;

import java.util.ArrayList;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;

import mindustry.content.Blocks;
import mindustry.core.NetClient;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.net.Administration.Config;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;

import util.*;
import util.Players.*;


public class moreCommandsPlugin extends mindustry.mod.Plugin {
    private ArrayList<String> votesVNW = new ArrayList<>(), 
    	votesRTV = new ArrayList<>(), 
    	rainbowedPlayers = new ArrayList<>(), 
    	effects = new ArrayList<>(), 
    	adminCommands = new Seq<String>().addAll("team", "am", "kick", "pardon", "ban", "unban", "players", "kill", "tp", "core", "tchat", "spawn", "godmode", "mute", "unmute").list(),
    	bannedClients = new Seq<String>().addAll("VALVE", "tuttop", "CODEX", "IGGGAMES", "IgruhaOrg", "FreeTP.Org").list(),
    	defaultBannedNames = new Seq<String>().addAll("[Server]", "[server]", "@a", "@p", "@t", "~").list(),
    	defaultBannedIps = new ArrayList<>(),
    	bannedIps = new ArrayList<>(),
    	bannedNames = new ArrayList<>(),
    	mutedPlayers = new ArrayList<>();
    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();
    private ObjectMap<String, Float> godmodPlayers = new ObjectMap<>();
    private Map selectedMap;
    private float ratio = 0.6f;
    private boolean unbanConfirm = false, autoPause = false, tchat = true, niceWelcome = true, clearConfirm = false, canVote = true, antiVpn = false;
   
    //Called after all plugins have been created and commands have been registered.
    public void init() { 
    	netServer.admins.addChatFilter((p, m) -> null); //delete the tchat
    	CommandsManager.init(); //init the commands manager
    	
    	if (Groups.player.size() < 1 && autoPause) {
			state.serverPaused = true;
			Log.info("auto-pause: " + Groups.player.size() + " player connected -> Game paused...");
		}
    } 
    
	public moreCommandsPlugin() {
		Effects.init();
		loadSettings();

    	//clear VNW & RTV votes and disabled it on game over
        Events.on(EventType.GameOverEvent.class, e -> {
        	canVote = false;
        	votesVNW.clear();
            votesRTV.clear();
        });
        Events.on(EventType.WorldLoadEvent.class, e -> canVote = true); //re-enabled vote
        
        Events.on(EventType.PlayerConnect.class, e -> {
        	//check the nickname of this player
        	nameCheck(e.player);
        	
        	//check if the nickname is empty without colors
        	if (Strings.stripColors(e.player.name).isBlank()) e.player.kick(KickReason.nameEmpty);
        	
        	//prevent to duplicate nicknames
        	for (Player p : Groups.player) 
        		if (Strings.stripColors(p.name).strip().equals(Strings.stripColors(e.player.name).strip())) e.player.kick(KickReason.nameInUse);

        	TempData.put(e.player); //add player in TempData
        	MSG.setEmpty(e.player);
        });

        Events.on(EventType.PlayerJoin.class, e -> {
        	//for me =)
        	if (e.player.uuid().equals("k6uyrb9D3dEAAAAArLs28w==") && niceWelcome) 
        		Call.sendMessage("[scarlet]\uE80F " + NetClient.colorizeName(e.player.id, e.player.name) + "[scarlet] has connected! \uE80F");
        	
        	//unpause the game if one player is connected
        	if (Groups.player.size() == 1 && autoPause) {
        		state.serverPaused = false;
        		Log.info("auto-pause: " + Groups.player.size() + " player connected -> Game unpaused...");
        		Call.sendMessage("[scarlet][Server]:[] Game unpaused...");
        	}
        });
        
        Events.on(EventType.PlayerLeave.class, e -> {
        	//pause the game if no one is connected
        	if (Groups.player.size()-1 < 1 && autoPause) {
        		state.serverPaused = true;
        		Log.info("auto-pause: " + (Groups.player.size()-1) + " player connected -> Game paused...");
        	}
        	
        	TempData.remove(e.player); // remove player in TempData
        	MSG.remove(e.player);
        	
        	//remove the rainbow, spectate, god mode, effects of this player
        	if(rainbowedPlayers.contains(e.player.uuid())) rainbowedPlayers.remove(e.player.uuid());
        	if(rememberSpectate.containsKey(e.player)) rememberSpectate.remove(e.player);
        	if(godmodPlayers.containsKey(e.player.uuid())) godmodPlayers.remove(e.player.uuid());
        	if(effects.contains(e.player.uuid())) effects.remove(e.player.uuid());
        });

        //recreate the tchat for the command /tchat
        Events.on(EventType.PlayerChatEvent.class, e -> {
        	if (!e.message.startsWith("/")) {
        		if (mutedPlayers.contains(e.player.uuid())) Players.err(e.player, "You're muted, you can't speak.");
        		else if (tchat) {
        			Call.sendMessage(e.message,  NetClient.colorizeName(e.player.id, e.player.name), e.player);
        			Log.info("<" + e.player.name + ": " + e.message + ">");
        		} else {
        			
        			if (e.player.admin) {
        				Call.sendMessage(e.message, "[scarlet]<Admin>[]" + NetClient.colorizeName(e.player.id, e.player.name), e.player);
        				Log.info("<[Admin]" + e.player.name + ": " + e.message + ">");
        			} else e.player.sendMessage("[scarlet]The tchat is disabled, you can't write!");
        		}
    	   }
        }); 
        
        //for players in god mode 
        Events.on(EventType.BlockBuildBeginEvent.class, e -> {
        	Player player = e.unit.getPlayer();
        
        	if (player != null && godmodPlayers.containsKey(player.uuid())) {
        		try {
        			if (e.breaking) Call.deconstructFinish(e.tile, e.tile.block(), e.unit);
        			
        			/*/!\ DISABLED BECAUSE CREATES A GHOST BLOCK THAT CAN CRASH THE SERVER AND EVERYBODY /!\
        			* else Call.constructFinish(e.tile, e.tile.block(), e.unit, (byte)e.tile.build.rotation, e.team, e.tile.build.config());
        			*/
        		} catch (NullPointerException error) { error.printStackTrace(); }
        	}
        });

    }
    

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
    	setHandler(handler);
    	
    	handler.register("unban-all", "[y|n]", "Unban all IP and ID", arg -> {
    		if (arg.length == 1 && !unbanConfirm) {
    			Log.err("Use first: 'unban-all', before confirming the command.");
    			return;
    		} else if (!unbanConfirm) {
    			Log.warn("Are you sure to unban all all IP and ID ? (unban-all [y|n])");
    			unbanConfirm = true;
    			return;
    		} else if (arg.length == 0 && unbanConfirm) {
    			Log.warn("Are you sure to unban all all IP and ID ? (unban-all [y|n])");
    			unbanConfirm = true;
    			return;
    		}

    		switch (arg[0]) {
    			case "y": case "yes":
    				netServer.admins.getBanned().each(unban -> netServer.admins.unbanPlayerID(unban.id));
    				netServer.admins.getBannedIPs().each(ip -> netServer.admins.unbanPlayerIP(ip));
    				Log.info("All all IP and ID have been unbanned!");
    				unbanConfirm = false;
    				break;
    			default: 
    				Log.err("Confirmation canceled ...");
    				unbanConfirm = false;
    		}
        });
    	
    	handler.register("auto-pause", "Pause the game if there is no one connected", arg -> {
    		if (autoPause) {
    			autoPause = false;
    			saveSettings();
    			Log.info("Auto pause is disabled.");
    				
    	        state.serverPaused = false;
    	        Log.info("auto-pause: " + Groups.player.size() + " player(s) connected -> Game unpaused...");
    		} else {
    			autoPause = true;
    			saveSettings();
    			Log.info("Auto pause is enabled.");
    				
    			if (Groups.player.size() < 1 && autoPause) {
    				state.serverPaused = true;
    				Log.info("auto-pause: " + Groups.player.size() + " player connected -> Game paused...");
    			}
    		}
    	});
    	
        handler.register("tchat", "[on|off]", "Enabled/disabled the tchat", arg -> {
        	if (arg.length == 0) {
        		Log.info("The tchat is currently @.", tchat ? "enabled" : "disabled");
        		return;
        	}
        	
        	switch (arg[0]) {
        		case "on": case "true":
        			if (tchat) {
        				Log.err("Disabled first!");
        				return;
        			}
        			tchat = true;
        			saveSettings();
        			Log.info("Tchat enabled ...");
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is enabled! [lightgray](by [scarlet][[Server][]) \n[gold]--------------------\n");
        			break;
        		
        		case "off": case "false":
        			if (!tchat) {
        				Log.err("Enabled first!");
        				return;
        			}
        			tchat = false;
        			saveSettings();
        			Log.info("Tchat disabled ...");
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is disabled! [lightgray](by [scarlet][[Server][]) \n[gold]--------------------\n");
        			break;
        		
        		default: Log.err("Invalid arguments. \n - The tchat is currently @.", tchat ? "enabled" : "disabled");
        	}
        });
        
        handler.register("nice-welcome", "Nice welcome for me", arg -> {
        	niceWelcome = !niceWelcome;
        	Log.info(niceWelcome ? "Enabled" : "Disabled");
        });
        
        handler.removeCommand("whitelisted");
        handler.removeCommand("whitelist-add");
        handler.removeCommand("whitelist-remove");
        handler.register("whitelist", "[add/remove] [ID]", "Only members of the whitelist can connect to the server.", arg -> {
        	if (arg.length == 0) {
        		Seq<PlayerInfo> whitelist = netServer.admins.getWhitelisted();
        		
        		if(whitelist.isEmpty()) Log.info("No whitelisted players found.");
        		else {
        			Log.info("Whitelist:");
        			whitelist.each(p -> Log.info("- Name: @ / UUID: @", p.lastName, p.id));
        		}
        	} else {
        		if (arg.length == 2) {
        			PlayerInfo info = netServer.admins.getInfoOptional(arg[1]);
        		    
        			if(info == null) Log.err("Player ID not found. You must use the ID displayed when a player joins a server.");
        		    else {
        		    	switch (arg[0]) {
        					case "add":
        		            	netServer.admins.whitelist(arg[1]);
        		            	Log.info("Player '@' has been whitelisted.", info.lastName);
        		            	break;
        				
        					case "remove":
        						netServer.admins.unwhitelist(arg[1]);
        			            Log.info("Player '@' has been un-whitelisted.", info.lastName);
        						break;
        				
        					default:
        						Log.err("Invalid arguments");
        		    	}
        			}
        		} else Log.err("Invalid arguments");
        	}
        });
        
        handler.register("commands", "<list|commandName> [on|off]", "Enable/Disable a command. /!\\Requires server restart to apply changes.", arg -> {
        	if (arg[0].equals("list")) {
        		StringBuilder builder = new StringBuilder();
        		Seq<CommandsManager.Commands> client = new Seq<CommandsManager.Commands>().addAll(CommandsManager.copy().filter(c -> c.name.startsWith("/")));
        		Seq<CommandsManager.Commands> server = new Seq<CommandsManager.Commands>().addAll(CommandsManager.copy().filter(c -> !c.name.startsWith("/")));
        		int best1 = bestLength(client);
        		int best2 = bestLength(server);
        		
        		Log.info("List of all commands: ");
        		Log.info(Strings.fillAtRight("| Server commands: Total:" + server.size, 28+best2) + "Client commands: Total:" + client.size);
        		for (int i=0; i<Math.max(client.size, server.size); i++) {
        			try { builder.append(Strings.fillAtMiddle("| | Name: " + server.get(i).name, " - Enabled: " + (server.get(i).isActivate ? "true " : "false"), 27+best2)); } 
        			catch (IndexOutOfBoundsException e) { builder.append("|" + Strings.createSpaces(best1+20)); }
        			try { builder.append(Strings.fillAtRight(" | Name: " + client.get(i).name, 9+best1) + " - Enabled: " + client.get(i).isActivate); } 
        			catch (IndexOutOfBoundsException e) {}
        			
        			Log.info(builder.toString());
        			builder = new StringBuilder();
        		}
        	} else {
        		CommandsManager.Commands command = CommandsManager.get(arg[0]);
        		
        		if (command == null) Log.err("This command doesn't exist!");
        		else {
        			if (arg.length == 2) {
        				switch (arg[1]) {
        					case "on": case "true": case "1":
        						command.set(true);
        						Log.info("Enabled ...");
        						break;
        				
        					case "off": case "false": case "0":
        						command.set(false);
        						Log.info("Disabled ...");
        						break;
        				
        					default:
        						Log.err("Invalid value");
        						return;
        				}
        				CommandsManager.save();
        				CommandsManager.update(handler);
        			
        			} else Log.info("The command '" + command.name + "' is currently " + (command.isActivate ? "enabled" : "disabled"));
        		}
        	}
        });
        
        handler.register("clear-map", "[y|n]", "Kill all units and destroy all blocks except cores, on the current map.", arg -> {
        	if(!state.is(mindustry.core.GameState.State.playing)) Log.err("Not playing. Host first.");
            else {
            	if (arg.length == 1 && !clearConfirm) {
        			Log.err("Use first: 'clear-map', before confirming the command.");
        			return;
        		} else if (!clearConfirm) {
        			Log.warn("This command can crash the server! Are you sure you want it executed? (clear-map [y|n])");
        			clearConfirm = true;
        			return;
        		} else if (arg.length == 0 && clearConfirm) {
        			Log.warn("This command can crash the server! Are you sure you want it executed? (clear-map [y|n])");
        			clearConfirm = true;
        			return;
        		}

        		switch (arg[0]) {
        			case "y": case "yes":
        				Log.info("Begining ...");
        				Call.infoMessage("[scarlet]The map will be reset in [orange]10[] seconds! \n[]All units, players, and buildings (except core) will be destroyed.");
        				try { Thread.sleep(10000); } 
        				catch (InterruptedException e) {}
        				
        				mindustry.gen.Building block;
        				Groups.unit.each(u -> u.kill());
        				for (int x=0; x<world.width(); x++) {
        					for (int y=0; y<world.height(); y++) {
        						block = world.build(x, y);
        						
        						if (block != null && (block.block != Blocks.coreShard && block.block != Blocks.coreNucleus && block.block != Blocks.coreFoundation)) 
        							block.kill();
        					}
        				}
                		Groups.fire.each(a -> a.remove());
                		Groups.unit.each(u -> u.kill());
                		
                		Log.info("Map cleaned!");
        				clearConfirm = false;
        				break;
        			default: 
        				Log.err("Confirmation canceled ...");
        				clearConfirm = false;
        		}
            }
        });
        
        handler.register("gamemode", "[name]", "Change the gamemode of the current map", arg -> {
        	if(!state.is(mindustry.core.GameState.State.playing)) Log.err("Not playing. Host first.");
            else {
            	if (arg.length == 1) {
            		try { 
            			state.rules = state.map.applyRules(Gamemode.valueOf(arg[0]));
            			Groups.player.each(p -> {
            				Call.worldDataBegin(p.con);
                            netServer.sendWorldData(p);
            			});
            			Log.info("Gamemode of the map set to '@'", arg[0]);
            		} catch (Exception e) { Log.err("No gamemode '@' found.", arg[0]); }
            	} else Log.info("The gamemode of the map is curently '@'", state.rules.mode().name());
            }
        });
        
        handler.register("blacklist", "<list|add|remove|clear> <name|ip> [value...]", 
        		"Players using a nickname or ip in the blacklist cannot connect to the server (spaces on the sides and colors are cut off when checking out)", arg -> {
        	ArrayList<String> list = new ArrayList<>();
        	list.addAll(defaultBannedNames);
        	list.addAll(bannedClients);
        	
        	if (arg[0].equals("list")) {
        		StringBuilder builder = new StringBuilder();
        		
        		if (arg[1].equals("name")) {
        			int best = Strings.bestLength(bannedNames);
            		int max = best > 18+String.valueOf(bannedNames.size()).length() ? best+4 : 23+String.valueOf(bannedNames.size()).length();
            		
            		Log.info("List of banned names:");
            		Log.info(Strings.fillAtRight("| Custom list: Total: " + bannedNames.size(), max) + "  Default list: Total: " + list.size());
            		for (int i=0; i<Math.max(bannedNames.size(), list.size()); i++) {
            			try { builder.append(Strings.fillAtRight("| | " + bannedNames.get(i), max+1)); } 
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
            		Log.info(Strings.fillAtRight("| Custom list: Total: " + bannedIps.size(), max) + "  Default list: Total: " + defaultBannedIps.size() + " (Anti VPN list)");
            		for (int i=0; i<Math.max(bannedIps.size(), defaultBannedIps.size()); i++) {
            			try { builder.append(Strings.fillAtRight("| | " + bannedIps.get(i), max+1)); } 
            			catch (IndexOutOfBoundsException e) { 
            				builder.append("|" + Strings.createSpaces(max)); 
            				if (i > 20) break;
            			}
            			try { 
            				if (i == 20) builder.append(" | ...." + (defaultBannedIps.size()-i) + " more");
            				else if (i < 20) builder.append(" | " + defaultBannedIps.get(i));
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
        });
        
        handler.register("anti-vpn", "[on|off]", "Anti VPN service", arg -> {
        	if (arg.length == 0) {
        		Log.info("Anti VPN is currently @.", antiVpn ? "enabled" : "disabled");
        		return;
        	}
        	
        	switch (arg[0]) {
        		case "on": case "true":
        			if (antiVpn) {
        				Log.err("Disabled first!");
        				return;
        			}
        			antiVpn = true;
        			Log.info("Anti VPN enabled ...");
        			break;
        		
        		case "off": case "false":
        			if (!antiVpn) {
        				Log.err("Enabled first!");
        				return;
        			}
        			antiVpn = false;
        			Log.info("Anti VPN disabled ...");
        			break;
        		
        		default: 
        			Log.err("Invalid arguments. \n - Anti VPN is currently @.", antiVpn ? "enabled" : "disabled");
        			return;
        	}
        	
        	saveSettings();
        });
    }
    
    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
    	setHandler(handler);
    	
    	handler.removeCommand("help");
    	handler.<Player>register("help", "[page]", "Lists all commands", (arg, player) -> {
        	if(arg.length == 1 && !Strings.canParseInt(arg[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }
        	
        	ArrayList<CommandHandler.Command> commands = handler.getCommandList().list();
        	if (!player.admin) {
        		handler.getCommandList().forEach(command -> {
        			if (adminCommands.contains(command.text)) commands.remove(command);
        		});
        	}
        	
        	int lines = 8,
        			page = arg.length == 1 ? Strings.parseInt(arg[0]) : 1,
        			pages = Mathf.ceil(commands.size() / lines);
        	if (commands.size() % lines != 0) pages++;
        	
            if(page > pages || page < 1){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[].");
                return;
            }
            
            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Commands Page[lightgray] @[gray]/[lightgray]@[orange] --\n", page, pages));

            for(int i=(page-1)*lines; i<lines*page; i++){
            	try { result.append("\n[orange] " + handler.getPrefix() + commands.get(i).text + "[white] " + commands.get(i).paramText + "[lightgray] - " + commands.get(i).description); } 
            	catch (IndexOutOfBoundsException e) { break; }
            }
            player.sendMessage(result.toString());
        });
         
        handler.<Player>register("ut","unit type", (args, player) -> {
        	try { player.sendMessage("You're a [sky]" + player.unit().type().name + "[]."); }
        	catch (NullPointerException e) { player.sendMessage("You're [sky]invisible ..."); }
        });
        
        handler.<Player>register("msg", "<ID|username> <message...>","Send a message to a player", (arg, player) -> {
        	Players result = Players.findByNameOrID(arg[0] + " " + arg[1]);
        	Player target = result.player;
        	
            if(target == null) Players.err(player, "Player not connected or doesn't exist!");
            else {
            	if (Strings.stripColors(String.join(" ", result.rest)).isBlank()) Players.err(player, "Please don't send an empty message.");
            	else {
            		MSG.get(player).setTarget(target);
            		player.sendMessage("\n[gold]Private Message send to []" + target.name);
            		target.sendMessage("\n[gold]Private Message: [white]" + player.name + "[gold] --> [sky]you[gold]\n--------------------------------\n[white]" + String.join(" ", result.rest));
            	}
            	
            }
         });
        
        handler.<Player>register("r", "<message...>","Reply to the last private message received.", (arg, player) -> {
        	Player target = MSG.get(player).target;
        	
        	if (target == null) Players.err(player, "No one has sent you a private message");
        	else {
        		target = Players.findByID(target.uuid()).player;
        		
        		if (target == null) Players.err(player, "This player is disconnected");
        		else {
        			if (Strings.stripColors(arg[0]).isBlank()) Players.err(player, "Please don't send an empty message.");
                	else {
                		MSG.get(player).setTarget(target);
                		player.sendMessage("\n[gold]Private Message send to []" + target.name);
                		target.sendMessage("\n[gold]Private Message: [white]" + player.name + "[gold] --> [sky]you[gold]\n--------------------------------\n[white]" + arg[0]);
                	}
        		}
        	}
        });

        handler.<Player>register("maps", "[page]", "List all maps on server", (arg, player) -> {
        	if(arg.length == 1 && !Strings.canParseInt(arg[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }
        	
        	Seq<Map> list = mindustry.Vars.maps.all();
        	int page = arg.length == 1 ? Strings.parseInt(arg[0]) : 1,
        			lines = 8, 
        			pages = Mathf.ceil(list.size / lines);
        	if (list.size % lines != 0) pages++;
            
            if (page > pages || page < 1) {
            	player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and [orange]" + pages + "[].");
            	return;
            }
            
            player.sendMessage("\n[orange]---- [gold]Maps list [lightgray]" + page + "[gray]/[lightgray]" + pages + "[orange] ----");
            for (int i=(page-1)*lines; i<lines*page;i++) {
            	try { player.sendMessage("[orange]  - []" + list.get(i).name() + "[orange] | [white]" + list.get(i).width + "x" + list.get(i).height); } 
            	catch (IndexOutOfBoundsException e) { break; }
            }
            player.sendMessage("[orange]-----------------------");
        });
        
        handler.<Player>register("vnw", "(VoteNewWave) Vote for Sending a new Wave", (args, player) -> {
        	if (!canVote) return;
        	if (votesVNW.contains(player.uuid())) {
                player.sendMessage("You have Voted already.");
                return;
        	}

            votesVNW.add(player.uuid());
            int cur = votesVNW.size();
            int req = Mathf.ceil((float) ratio * Groups.player.size());
            Call.sendMessage(NetClient.colorizeName(player.id, player.name) + "[orange] has voted to send a new wave. [lightgray](" + (req-cur) + " missing)");
            
            if (cur < req) return;

            votesVNW.clear();
            Call.sendMessage("[green]Vote for Sending a New Wave is Passed. New Wave will be Spawned.");
            state.wavetime = 0f;
		});
        
        handler.<Player>register("rtv", "[mapName...]", "Rock the vote to change map", (arg, player) -> {
        	if (!canVote) return;
        	if (arg.length == 1) {
        		if (votesRTV.isEmpty()) {
        			selectedMap = maps.all().find(map -> Strings.stripColors(map.name().replace('_', ' ')).equalsIgnoreCase(Strings.stripColors(arg[0])));
        			if (selectedMap == null) selectedMap = maps.all().find(map -> Strings.stripColors(map.name().replace('_', ' ')).equalsIgnoreCase(Strings.stripColors(arg[0]).replace('_', ' ')));
        			if (selectedMap == null) {
        				Players.err(player, "No map with name '%s' found.", arg[0]);
        				return;
        			} else maps.queueNewPreview(selectedMap);
        			
        		} else {
        			Players.err(player, "A vote to change the map is already in progress! [lightgray](selected map: " + selectedMap.name() + "[lightgray])");
        			return;
        		}
        	} else if (votesRTV.isEmpty()) selectedMap = maps.getNextMap(Gamemode.valueOf(Core.settings.getString("lastServerMode")), state.map);
        	if (votesRTV.contains(player.uuid())) {
                player.sendMessage("You have Voted already.");
                return;
        	}
        	
        	votesRTV.add(player.uuid());
            int cur2 = votesRTV.size();
            int req2 = Mathf.ceil((float) ratio * Groups.player.size());
            Call.sendMessage("[scarlet]RTV: [accent]" + NetClient.colorizeName(player.id, player.name) + " [white]wants to change the map, [green]" + cur2 
            	+ "[white] votes, [green]" + req2 + "[white] required. [lightgray](selected map: [white]" + selectedMap.name() + "[lightgray])");
            
            if (cur2 < req2) return;
            
            votesRTV.clear();
            Call.sendMessage("[scarlet]RTV: [green]Vote passed, changing map ... [lightgray](selected map: [white]" + selectedMap.name() + "[lightgray])");
            new RTV(selectedMap, Team.crux);
        });

        handler.<Player>register("info-all", "[ID|username...]", "Get all player information", (arg, player) -> {
        	StringBuilder builder = new StringBuilder();
        	arc.struct.ObjectSet<PlayerInfo> infos;
        	PlayerInfo pI = netServer.admins.findByIP(player.ip());
        	boolean type = false;
        	
        	if(arg.length == 1) {
        		if (player.admin) {
            		infos = netServer.admins.searchNames(arg[0]);
            		type = true;
            	} else { 
            		Players.err(player, "You don't have the permission to use arguments!");
            		return;
            	}
        	} else infos = netServer.admins.searchNames(player.name);
			if (infos.size == 0 && arg.length >= 1) pI = netServer.admins.getInfoOptional(arg[0]);
			
            if (infos.size > 0) {
            	int i = 1;
            	
            	if (type) {
            		builder.append("[gold]----------------------------------------");
            		builder.append("\n[scarlet]-----"+ "\n[white]Players found: [gold]" + infos.size + "\n[scarlet]-----");
            		player.sendMessage(builder.toString());
            		builder = new StringBuilder();
            	}
                for (PlayerInfo info : infos) {
                	if (!type) {
                		if (i > 1) break;
                		builder.append("[white]Player name [accent]'" + infos.get(pI).lastName.replaceAll("\\[", "[[") + "[accent]'[white] / ID [accent]'" + infos.get(pI).id + "' \n");
                		builder.append("[gold]----------------------------------------[]\n");
                	}
                	else {
                		pI = info;
                		player.sendMessage("[gold][" + i++ + "] [white]Trace info for player [accent]'" + infos.get(pI).lastName.replaceAll("\\[", "[[") 
                			+ "[accent]'[white] / ID [accent]'" + infos.get(pI).id + "' ");
                	}
                	builder.append("[white] - All names used: [accent]" + infos.get(pI).names +
                			"\n[white] - IP: [accent]" + infos.get(pI).lastIP +
                			"\n[white] - All IPs used: [accent]" + infos.get(pI).ips +
                			(Players.findByID(infos.get(pI).id).player != null ? "\n[white] - [green]Online" : "") +
                			"\n[white] - Times joined: [green]" + infos.get(pI).timesJoined);
                	if (player.admin) builder.append("\n[white] - Times kicked: [scarlet]" + infos.get(pI).timesKicked + 
                								"\n[white] - Is baned: [accent]" + infos.get(pI).banned +
                								"\n[white] - Is admin: [accent]" + infos.get(pI).admin);
                	builder.append("\n[gold]----------------------------------------");
                	
                	if (!type) Call.infoMessage(player.con, builder.toString());
                	else player.sendMessage(builder.toString());
                	builder = new StringBuilder();
                }
           } else Players.err(player, "This player doesn't exist!");
        });
        
        handler.<Player>register("rainbow", "[ID|username...]", "[#ff0000]R[#ff7f00]A[#ffff00]I[#00ff00]N[#0000ff]B[#2e2b5f]O[#8B00ff]W[#ff0000]![#ff7f00]!", (arg, player) -> {
        	if (arg.length == 0) {
        		if(rainbowedPlayers.contains(player.uuid())) {
        			player.sendMessage("[sky]Rainbow effect toggled off.");
        			rainbowedPlayers.remove(player.uuid());
        			player.name = TempData.get(player).realName;
        		} else {
        			player.sendMessage("[sky]Rainbow effect toggled on.");
        			rainbowedPlayers.add(player.uuid());
        			if(effects.contains(player.uuid())) effects.remove(player.uuid());
        			TempData pData = TempData.get(player);
               
        			new Thread() {
        				public void run() {
        					while(rainbowedPlayers.contains(player.uuid())) {
        						try {
        							int hue = pData.hue;
                                    if (hue < 360) hue+=5;
                                    else hue = 0;
                                    
                                    for (int i=0; i<5; i++) Call.effect(mindustry.content.Fx.bubble, player.x, player.y, 10, 
                                    	arc.graphics.Color.valueOf(Integer.toHexString(java.awt.Color.getHSBColor(hue / 360f, 1f, 1f).getRGB()).substring(2)));
                                    player.name = Strings.RGBString(pData.normalizedName, hue);
                                    pData.setHue(hue);
                                    
                                    Thread.sleep(50);
        						} catch (InterruptedException e) {
                            	    e.printStackTrace();
                                }
        					}
        				}
        			}.start();
        		}
        	} else {
        		if (player.admin) {
        			Players.warn(player, "This will remove the rainbow from the person matching the argument.");
        			
        			Player rPlayer = Players.findByNameOrID(arg[0]).player;
        			
        			if(rPlayer != null && rainbowedPlayers.contains(rPlayer.uuid())) {
            			rainbowedPlayers.remove(rPlayer.uuid());
            			rPlayer.name = TempData.get(rPlayer).realName;
            			player.sendMessage("[sky]Rainbow effect toggled off for the player [accent]" + rPlayer.name + "[].");
        			} else Players.err(player, "This player don't have the rainbow or not connected.");
        			
        		} else Players.err(player, "You don't have the permission to use arguments!");
        	}
        	
        });
        
        handler.<Player>register("effect", "[list|name|id] [page|ID|username...]","Gives you a particles effect. [scarlet] May cause errors", (arg, player) -> {
        	Effects effect;
        	StringBuilder builder = new StringBuilder();
        	
        	if (arg.length >= 1 && arg[0].equals("list")) {
        		if(arg.length == 2 && !Strings.canParseInt(arg[1])){
                    player.sendMessage("[scarlet]'page' must be a number.");
                    return;
                }
        		
        		Seq<Effects> list = Effects.copy();
        		int page = arg.length == 2 ? Strings.parseInt(arg[1]) : 1,
        				lines = 12,
        				pages = Mathf.ceil(list.size / lines);
                if (list.size % lines != 0) pages++;
                Effects e;

                if(page > pages || page < 0){
                    player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                    return;
                }

                player.sendMessage("\n[orange]---- [gold]Effects list [lightgray]" + page + "[gray]/[lightgray]" + pages + "[orange] ----");
                for(int i=(page-1)*lines; i<lines*page;i++){
                	try {
                		e = list.get(i);
                		builder.append("  [orange]- [lightgray]ID:[white] " + e.id + "[orange] | [lightgray]Name:[white] " + e.name + "\n");
                	} catch (IndexOutOfBoundsException err) { break; }
                }
                player.sendMessage(builder.toString());
                return;
        	
        	} else if (arg.length == 0) {
        		if (effects.contains(player.uuid())) {
        			effects.remove(player.uuid());
        			player.sendMessage("[green]Removed particles effect.");
        			return;
        		} else {
        			if(rainbowedPlayers.contains(player.uuid())) rainbowedPlayers.remove(player.uuid());
        			effects.add(player.uuid());
        			int r = new java.util.Random().nextInt(172);
        			effect = Effects.getByID(r);
        			
        			player.sendMessage("Randomised effect ...");
        			player.sendMessage("[green]Start particles effect [accent]" + effect.id + "[scarlet] - []" + effect.name);
        		}
        	} else if (arg.length == 2) {
        		if (player.admin) {
        			Player target = Players.findByNameOrID(arg[1]).player;
        			
        			if(target == null) Players.err(player, "Player not connected or doesn't exist!");
        			else {
        				if (effects.contains(target.uuid())) {
        					effects.remove(target.uuid());
        					player.sendMessage("[green]Removed particles effect for [accent]" + target.name);
        				} else Players.err(player, "This player don't have particles effect");
        			}
        		} else Players.err(player, "You don't have the permission to use arguments!");
        		return;
        		
        	} else {
        		if (effects.contains(player.uuid())) {
        			Players.err(player, "Please disabled first [lightgray](tip: /effect)");
        			return;
        		} else effects.add(player.uuid());
        		if(rainbowedPlayers.contains(player.uuid())) rainbowedPlayers.remove(player.uuid());		
        		
        		if(Strings.canParseInt(arg[0])) effect = Effects.getByID(Strings.parseInt(arg[0])-1);
        		else effect = Effects.getByName(arg[0]);
        		
        		if (effect == null) {
        			Players.err(player, "Particle effect don't exist");
        			return;
        		} else player.sendMessage("[green]Start particles effect [accent]" + effect.id + "[scarlet] - []" + effect.name);
        	}

        	new Thread() {
        		public void run() {
        			while(effects.contains(player.uuid())) {
        				try { 
        					Call.effect(effect.effect, player.x, player.y, 10, arc.graphics.Color.green);
        					Thread.sleep(50); 
        				} catch (InterruptedException e) { e.printStackTrace(); }
        			}
        		}
        	}.start();
        });
        
        handler.<Player>register("team", "[teamname|list|vanish] [username...]","Change team", (args, player) ->{
            if(!player.admin()){
                player.sendMessage("[scarlet]Only admins can change team !");
                return;
            }
            StringBuilder builder = new StringBuilder();
            coreTeamReturn ret = null;
            Player target;
            
            if (args.length == 2) {
            	target = Players.findByName(args[1]).player;
            	
            	if (target == null) {
            		Players.err(player, "This player doesn't exist or not connected!");
            		return;
            	}
            } else target = player;
            
            if(rememberSpectate.containsKey(target)){
                player.sendMessage(">[orange] transferring back to last team");
                target.team(rememberSpectate.get(target));
                Call.setPlayerTeamEditor(target, rememberSpectate.get(target));
                rememberSpectate.remove(target);
                target.name = TempData.get(target).realName;
                return;
            }

            if(args.length >= 1){
                Team retTeam;
                switch (args[0]) {
                	case "sharded":
                        retTeam = Team.sharded;
                        break;
                    case "blue":
                        retTeam = Team.blue;
                        break;
                    case "crux":
                        retTeam = Team.crux;
                        break;
                    case "derelict":
                        retTeam = Team.derelict;
                        break;
                    case "green":
                        retTeam = Team.green;
                        break;
                    case "purple":
                        retTeam = Team.purple;
                        break;
                    
                    case "vanish":
                    	rememberSpectate.put(target, target.unit().team);
                    	if(rainbowedPlayers.contains(target.uuid())) rainbowedPlayers.remove(target.uuid());
                    	if(effects.contains(target.uuid())) effects.remove(target.uuid());
                    	
                        target.team(Team.all[8]);
                        Call.setPlayerTeamEditor(target, Team.all[8]);
                        target.unit().kill();
                        player.sendMessage("[green]VANISH MODE[] \nuse /team to go back to player mode.");
                        target.name = "";
                    	return;
                    	
                    default:
                    	Players.err(player, "This team don't exist!");
                    case "list":
                    	builder.append("available teams:\n");
                    	builder.append(" - [accent]vanish[]\n");
                        for (Team team : Team.baseTeams) {
                        	builder.append(" - [accent]" + team.name + "[]");
                        	if (!team.cores().isEmpty()) builder.append(" | [green]" + team.cores().size + "[] core(s) found");
                        	builder.append("\n");
                        }
                        player.sendMessage(builder.toString());
                        return;   
                    
                }
                if(retTeam.cores().isEmpty()) {
                	Players.warn(player,"This team has no core!");
                	target.team(retTeam);
                	target.unit().controlling.each(u -> u.team(retTeam));
                	player.sendMessage("> You changed [accent]" + (args.length == 2 ? target.name + " " : "") + "[white]to team [sky]" + retTeam);
                	return;
                }
                
                ret =  new coreTeamReturn(retTeam);
            } else ret = getPosTeamLoc(target);

            //move team mechanic
            if(ret != null) {
                Call.setPlayerTeamEditor(target, ret.team);
                target.team(ret.team);
                target.unit().controlling.each(u -> u.team(target.team()));
                player.sendMessage("> You changed [accent]" + (args.length == 2 ? target.name : "") + "[white] to team [sky]" + ret.team);
            } else Players.err(player, "Other team has no core, can't change!");
        });

        handler.<Player>register("am", "<message...>", "Send a message as admin", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	Call.sendMessage(arg[0], "[scarlet]<Admin>[]" + NetClient.colorizeName(player.id, player.name), player);
        });
        
        handler.<Player>register("players", "<all|online|ban>", "Gives the list of players", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	int size = 0;
        	StringBuilder builder = new StringBuilder();
        	
            switch (arg[0]) {
            	case "ban":
            		player.sendMessage("\nTotal banned players : [green]"+ netServer.admins.getBanned().size + ". \n[gold]-------------------------------- \n[accent]Banned Players:");
            		netServer.admins.getBanned().each(p -> {
            			player.sendMessage("[white]======================================================================\n" +
            					"[lightgray]" + p.id +"[white] / Name: [lightgray]" + p.lastName.replaceAll("\\[", "[[") + "[white]\n" +
            					" / IP: [lightgray]" + p.lastIP + "[white] / # kick: [lightgray]" + p.timesKicked);
            		});
            		break;
            
            	case "online":
            		size = Groups.player.size() + 3;
            		
            		builder.append("\nTotal online players: [green]").append(Groups.player.size()).append("[].\n[gold]--------------------------------[]").append("\n[accent]List of players: \n");
            		for (Player p : Groups.player) {
            			builder.append(" - [lightgray]").append(p.name.replaceAll("\\[", "[[")).append("[] : [accent]'").append(p.uuid()).append("'[]");
            			if (p.admin) builder.append("[white] | [scarlet]Admin[]");
            			builder.append("\n[accent]");
            		}
            		break;
            	
            	case "all":
            		size = Mathf.ceil(netServer.admins.getWhitelisted().size + 3 + netServer.admins.getWhitelisted().size /2);
            		
            		builder.append("\nTotal players: [green]").append(netServer.admins.getWhitelisted().size)
            			.append("[].\n[gold]--------------------------------[]").append("\n[accent]List of players: []\n");
            		for (PlayerInfo p : netServer.admins.getWhitelisted()) {
            			builder.append("[white] - [lightgray]Names: [accent]").append(p.names).append("[white] - [lightgray]ID: [accent]'").append(p.id).append("'");
            			if (p.admin) builder.append("[white] | [scarlet]Admin");
            			if (p.banned) builder.append("[white] | [orange]Banned");
            			
            			Player online = Groups.player.find(pl -> pl.uuid().equals(p.id));
            			if (online != null) builder.append("[white] | [green]Online");
            			builder.append("\n");
            		}
            		break;
            	
            	default: Players.err(player, "Invalid usage:[lightgray] Invalid arguments.");
            }
            
            if (size > 50) Call.infoMessage(player.con, builder.toString());
            else player.sendMessage(builder.toString());
        });

        handler.<Player>register("kill", "[@p|@a|@t|username...]", "Kill a player. @a: all unit. @p: all player. @t: all unit in actual team", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
            
        	if (arg.length == 0) player.unit().kill();
        	else {
        		switch (arg[0]) {
        			case "@p":
        				Groups.player.each(p -> p.unit().kill());
        				player.sendMessage("[green]Killed all players");
        				return;
        			case "@a":
        				Groups.unit.each(u -> u.kill());
        				player.sendMessage("[green]Killed all units");
        				return;
        			case "@t":
        				Groups.unit.each(t -> t.team().equals(player.team()), u -> u.kill());
        				player.sendMessage("[green]Killed all units in team [accent]" + player.team().name);
        				return;
        			default:
        				Player other = Players.findByName(arg[0]).player;
        				if (other != null) {
        					other.unit().kill();
        					player.sendMessage("[green]Killed [accent]" + other.name);
        				}
        				else player.sendMessage("[scarlet]This player doesn't exist or not connected!");
        		}
        	}
        });
        

        handler.<Player>register("core", "<small|medium|big>", "Spawn a core to your corrdinate", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	if(rememberSpectate.containsKey(player)) {
        		Players.err(player, "You can't build a core in vanish mode!");
        		return;
        	}
        	
        	mindustry.world.Block core;
        	switch (arg[0]) {
        		case "small": 
        			core = Blocks.coreShard;
        			break;
        		case "medium": 
        			core = Blocks.coreFoundation;
        			break;
        		case "big": 
        			core = Blocks.coreNucleus;
        			break;
        		default: 
        			core = Blocks.coreShard;
        	}
        	if (core != null) {
        		Call.constructFinish(player.tileOn(), core, player.unit(), (byte)0, player.team(), false);
        		player.sendMessage(player.tileOn().block() == core ? "[green]Core build." : "[scarlet]Error: Core not build.");
        	} else {
        		Players.warn(player, "This will destroy all the blocks which will be hampered by the construction of the core.");
        		Players.err(player, "Core type not found:[] Please use arguments small, medium, big.");
        	}
        });
        
        handler.<Player>register("tp", "<name|x,y> [to_name|x,y...]", "Teleport to position or player", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	int[] co;
            Player target;
            Search result = new Search(arg[0] + (arg.length == 2 ? " " + arg[1] : ""), player);
            
            if (result.error) return;
            else {
            	target = player;
            	
            	if (result.XY == null) co = new int[]{(int) result.player.x/8, (int) result.player.y/8};
            	else co = result.XY;
            }
            	
            if (arg.length == 2 && !String.join(" ", result.rest).isBlank()) {
            	arg[1] = String.join(" ", result.rest);
            	target = result.player;
            	
            	if (result.XY == null) {
					result = new Search(arg[1], player);
					
					if (result.error) return;
		            else {
		            	if (result.XY == null) co = new int[]{(int) result.player.x/8, (int) result.player.y/8};
		            	else co = result.XY;
		            }
				} else {
					player.sendMessage("[scarlet]Can't teleport a coordinate to a coordinate or to a player! [lightgray]It's not logic XD.");
            		return;
				}
            }
            
            if (co[0] > world.width() || co[0] < 0 || co[1] > world.height() || co[1] < 0) {
                player.sendMessage("[scarlet]Coordinates too large. Max: [orange]" + world.width() + "[]x[orange]" + world.height() + "[]. Min: [orange]0[]x[orange]0[].");
                return;
            }
            
            if (Config.strict.bool()) {
            	Config.strict.set(false);
            	target.set(co[0]*8, co[1]*8);
            	Call.setPosition(target.con, co[0]*8, co[1]*8);
				
            	new Thread() {
            		public void run() {
            			try { Thread.sleep(100); } 
            			catch (InterruptedException e) {}
            			Config.strict.set(true);
            		}
            	}.start();
            	
            } else {
            	target.set(co[0]*8, co[1]*8);
            	Call.setPosition(target.con, co[0]*8, co[1]*8);
            }
            target.snapSync();
            
            if (arg.length == 2) player.sendMessage("[green]You teleported [accent]" + target.name + "[green] to [accent]" + co[0] + "[green]x[accent]" + co[1] + "[green].");
            else player.sendMessage("[green]You teleported to [accent]" + co[0] + "[]x[accent]" + co[1] + "[].");
        });  
        
        handler.<Player>register("spawn", "<unit> [x,y|username] [teamname] [count...]", "Spawn a unit ('~': your team or coordinates).", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	StringBuilder builder = new StringBuilder();
        	mindustry.type.UnitType unit = content.units().find(b -> b.name.equals(arg[0]));
        	Player target = player;
        	Team team = target.team();
        	int count = 1, x = (int) target.x, y = (int) target.y;
        	Seq<String> newArg = new Seq<String>().addAll(arg);
        	newArg.remove(0);

        	if (arg.length >= 2) {
        		if (arg[1].equals("~")) {
        			target = player;
        			x = (int) target.x;
        			y = (int) target.y;
        			newArg.remove(0);
        		} else {
        			Search result = new Search(newArg.toString(" "), player);
        			newArg.set(new Seq<String>().addAll(result.rest));
        			
        			if (result.error) return;
                    else target = result.player;

        			if (result.XY == null) {
        				x = (int) target.x;
        				y = (int) target.y;
        			} else {
        				x = result.XY[0]*8;
        				y = result.XY[1]*8;
        			}
        		}
        		
        		if (!newArg.isEmpty()) {
        			switch (newArg.get(0)) {
            			case "~": 
            				team = target.team();
            				break;
            			case "sharded": 
            				team = Team.sharded;
            				break;
            			case "blue": 
            				team = Team.blue;
            				break;
            			case "crux": 
            				team = Team.crux;
            				break;
            			case "derelict": 
	            			team = Team.derelict;
	            			break;
            			case "green": 
            				team = Team.green;
            				break;
            			case "purple": 
            				team = Team.purple;
            				break;
            			default: 
            				Players.err(player, "Team not found! []\navailable teams: ");
            				for (Team teamList : Team.baseTeams) builder.append(" - [accent]" + teamList.name + "[]\n");
            				player.sendMessage(builder.toString());
            				return;	
        			}
        			newArg.remove(0);
        		} else team = target.team();
        		
        		if (!newArg.isEmpty()) {
        			if (!Strings.canParseInt(newArg.get(0))) {
        				Players.err(player, "'count' must be number!");
        				return;
        			} else count = Strings.parseInt(newArg.get(0));
        			newArg.remove(0);
        		}
        		
        		if (!newArg.isEmpty()) {
        			Players.err(player, "Too many arguments!");
        			return;
        		}
        	}

            if (unit == null) player.sendMessage("[scarlet]Available units: []" + content.units().toString("[scarlet], []"));
            else {
            	if (team.cores().isEmpty()) Players.err(player, "The [accent]" + team.name + "[] team has no core! Units cannot spawn");
            	else {
            		for (int i=0; i<count; i++) unit.spawn(team, x, y);
                
            		player.sendMessage("[green]You are spawning [accent]" + count + " " + unit + " []for [accent]" + team + " []team at [orange]" + x/8 + "[white],[orange]" + y/8);
            	}
            }	
        });
        
        handler.<Player>register("godmode", "[username...]", "[scarlet][God][]: [gold]I'm divine!", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	Player target;
        	if (arg.length == 0) target = player;
        	else target = Players.findByName(arg[0]).player;
        	
        	if (target != null) {
        		if (godmodPlayers.containsKey(target.uuid())) {
        			target.unit().type.buildSpeed = godmodPlayers.get(target.uuid());
        			target.unit().health = target.unit().maxHealth;
        			godmodPlayers.remove(target.uuid());
        		} else {
        			godmodPlayers.put(target.uuid(), target.unit().type.buildSpeed);
        			target.unit().health = Integer.MAX_VALUE;
        			target.unit().type.buildSpeed = Float.MAX_VALUE;
        		}
        		
        	} else {
        		player.sendMessage("[scarlet]This player doesn't exist or not connected!");
        		return;
        	}
        		
        	if (arg.length == 0) player.sendMessage("[gold]God mode is [green]" + (godmodPlayers.containsKey(target.uuid()) ? "enabled" : "disabled"));
        	else {
        		player.sendMessage("[gold]God mode is [green]" + (godmodPlayers.containsKey(target.uuid()) ? "enabled" : "disabled") + (arg.length == 0 ? "" : "[] for [accent]" + target.name));
        		target.sendMessage((godmodPlayers.containsKey(target.uuid()) ? "[green]You've been put into god mode" : "[red]You have been removed from creative mode") 
        			+ " by [accent]"+ player.name);
        	}
        	
        });
        
        handler.<Player>register("tchat", "[on|off]", "Enabled/disabled the tchat", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	if (arg.length == 0) {
        		Log.info("The tchat is currently @.", tchat ? "enabled" : "disabled");
        		return;
        	}
        	
        	switch (arg[0]) {
        		case "on": case "true":
        			if (tchat) {
        				Players.err(player, "Disabled first!");
        				return;
        			}
        			tchat = true;
        			saveSettings();
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is enabled! [lightgray](by " + player.name + "[lightgray]) \n[gold]--------------------\n");
        			Log.info("Tchat enabled by " + player.name + ".");
        			break;
        		case "off": case "false":
        			if (!tchat) {
        				Players.err(player, "Enabled first!");
        				return;
        			}
        			tchat = false;
        			saveSettings();
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is disabled! [lightgray](by " + player.name + "[lightgray]) \n[gold]--------------------\n");
        			Log.info("Tchat disabled by " + player.name + ".");
        			break;
        		default: Players.err(player, "Invalid arguments.[] \n - The tchat is currently [accent]%s[].", tchat ? "enabled" : "disabled");
        	}
        });   
        
        handler.<Player>register("mute", "<username|ID> [reason...]", "mute a person by name or ID", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;

            Players result = Players.findByNameOrID(arg[0] + (arg.length == 2 ? " " + arg[1] : ""));
        	Player target = result.player;
            
            if (target == null) Players.err(player, "Nobody with that name or ID could be found...");
            else {
            	mutedPlayers.add(target.uuid());
            	Call.sendMessage("[scarlet]/!\\" + NetClient.colorizeName(target.id, target.name) + "[scarlet] has been muted of the server.");
            	Call.infoMessage(target.con, "You have been muted! [lightgray](by " + player.name + "[lightgray]) \n[scarlet]Reason: []" + (arg[1].isBlank() ?  "<unknown>" : String.join(" ", result.rest)));
            }
        });
        
        handler.<Player>register("unmute", "<username|ID>", "unmute a person by name or ID", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;

            Player target = Players.findByNameOrID(arg[0]).player;
            
            if (target == null) Players.err(player, "Nobody with that name or ID could be found...");
            else {
            	mutedPlayers.remove(target.uuid());
            	Call.infoMessage(target.con, "You have been unmuted! [lightgray](by " + player.name + "[lightgray])");
            }
        });
        
        handler.<Player>register("kick", "<username|ID> [reason...]", "Kick a person by name or ID", (arg, player) -> {
            if (!Players.adminCheck(player)) return;

            Players result = Players.findByNameOrID(arg[0] + (arg.length == 2 ? " " + arg[1] : ""));
        	Player target = result.player;
            
            if (target == null) Players.err(player, "Nobody with that name or ID could be found...");
            else {
                Call.sendMessage("[scarlet]/!\\" + NetClient.colorizeName(target.id, target.name) + "[scarlet] has been kicked of the server.");
                if (arg.length == 2) target.kick("You have been kicked from the server!\n[scarlet]Reason: []" + (arg[1].isBlank() ?  "<unknown>" : String.join(" ", result.rest)));
                else target.kick(KickReason.kick);
            }
        });   
        
        handler.<Player>register("pardon", "<ID>", "Pardon a player by ID and allow them to join again", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);
        	
        	if (info != null) {
        		info.lastKicked = 0;
        		Players.info(player, "Pardoned player: [accent]%s", info.lastName);
        	} else Players.err(player, "That ID can't be found.");
        });

        handler.<Player>register("ban", "<username|ID> [reason...]", "Ban a person", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;

        	Players result = Players.findByNameOrID(arg[0] + (arg.length == 2 ? " " + arg[1] : ""));
        	Player target = result.player;
        	
        	if (target == null) Players.err(player, "No matches found.");
        	else {
        		netServer.admins.banPlayer(target.uuid());
        		Call.sendMessage("[scarlet]/!\\ " + NetClient.colorizeName(target.id, target.name) + "[scarlet] has been banned of the server.");
        		if (arg.length == 2) target.kick("You are banned on this server!!\n[scarlet]Reason: []" + (arg[1].isBlank() ?  "<unknown>" : String.join(" ", result.rest)));
                else target.kick(KickReason.banned);
        	}
        });
        
        handler.<Player>register("unban", "<ID>", "Unban a person", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
       
            if (netServer.admins.unbanPlayerID(arg[0])) Players.info(player, "Unbanned player: [accent]%s", arg[0]);
            else Players.err(player, "That IP/ID is not banned!");
        });

    }
    
    @SuppressWarnings("unchecked")
	private void loadSettings() {
    	try {
    		if (Core.settings.has("moreCommands")) {
        		String[] temp = Core.settings.getString("moreCommands").split(" \\| ");
        		autoPause = Boolean.parseBoolean(temp[0]);
        		tchat = Boolean.parseBoolean(temp[1]);
        		antiVpn = Boolean.parseBoolean(temp[2]);
        	} else saveSettings();
        	
        	if (Core.settings.has("bannedNamesList")) {
        		bannedNames = Core.settings.getJson("bannedNamesList", Seq.class, null).list(); 
        	} else saveSettings();
        	
        	if (Core.settings.has("bannedIpsList")) {
        		 bannedIps = Core.settings.getJson("bannedIpsList", Seq.class, null).list(); 
        	} else saveSettings();
        	
    	} catch (Exception e) {
    		saveSettings();
    		loadSettings();
    	}
    	
    	arc.files.Fi file = Core.files.local("config/ip-vpn-list.txt");

    	if (file.exists()) {
    		try { 
    			Object[] list = file.readString().lines().toArray();
    			for (Object line : list) defaultBannedIps.add((String) line);
    		} catch (Exception e) {
    			Core.net.httpGet("https://raw.githubusercontent.com/Susideur/moreCommands/main/ip-vpn-list.txt", s -> {
        			file.writeBytes(s.getResult());
        			defaultBannedIps = new Seq<String>().addAll((String[]) file.readString().lines().toArray()).list();
        		}, f -> {
        			Log.err("The anti VPN file could not be downloaded from the web! It will therefore be deactivated");
        			antiVpn = false;
        		});
    		}
    	
    	} else {
    		try { file.file().createNewFile(); } 
    		catch (java.io.IOException e) {}
    		
    		Core.net.httpGet("https://raw.githubusercontent.com/Susideur/moreCommands/main/ip-vpn-list.txt", s -> {
    			file.writeBytes(s.getResult());
    			Object[] list = file.readString().lines().toArray();
    			for (Object line : list) defaultBannedIps.add((String) line);
    		}, f -> {
    			Log.err("The anti VPN file could not be downloaded from the web! It will therefore be deactivated");
    			antiVpn = false;
    		});
    	}
    }
    
    private void saveSettings() {
    	Core.settings.put("moreCommands", String.join(" | ", autoPause+"", tchat+"", antiVpn+""));
    	Core.settings.putJson("bannedNamesList", new Seq<String>().addAll(bannedNames));
    	Core.settings.putJson("bannedIpsList", new Seq<String>().addAll(bannedIps));
    	Core.settings.forceSave();
    }

    private int bestLength(Seq<CommandsManager.Commands> list) {
    	return Strings.bestLength(list.map(c -> c.name).list());
    }
    
    private void setHandler(CommandHandler handler) {
    	new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
					CommandsManager.load(handler);
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
    	}.start();
    }

    private void nameCheck(Player player) {
    	String name = Strings.stripColors(player.name).strip();
    	
    	if (bannedNames.contains(name) || defaultBannedNames.contains(name)) 
    		player.kick("[scarlet]Invalid nickname: []Please don't use [accent]'" + bannedNames.get(bannedNames.indexOf(name)) + "'[white] in your nickname.");
    	else if (bannedClients.contains(name)) 
    		player.con.kick("Ingenuine copy of Mindustry.\n\n"
    			+ "Mindustry is free on: [royal]https://anuke.itch.io/mindustry[]\n"
    			+ "Mindustry est gratuit ici : [royal]https://anuke.itch.io/mindustry[]\n");
    	
    	else if (bannedIps.contains(player.con.address))
    		player.kick("[scarlet]The IP you are using is blacklisted. [lightgray](your ip: " + player.ip() +")");
    	else if (defaultBannedIps.contains(player.con.address) && antiVpn) 
    		player.kick("[scarlet]Anti VPN is activated on this server! []Please deactivate your VPN to be able to connect to the server.");
    }
    
    //search a possible team
    private Team getPosTeam(Player p){
        Team currentTeam = p.team();
        int c_index = java.util.Arrays.asList(Team.baseTeams).indexOf(currentTeam);
        int i = (c_index+1)%6;
        while (i != c_index){
            if (Team.baseTeams[i].cores().size > 0){
                return Team.baseTeams[i];
            }
            i = (i + 1) % Team.baseTeams.length;
        }
        return currentTeam;
    }

    private coreTeamReturn getPosTeamLoc(Player p){
        Team currentTeam = p.team();
        Team newTeam = getPosTeam(p);
        if (newTeam == currentTeam){
            return null;
        }else{
            return new coreTeamReturn(newTeam);
        }
    }

    class coreTeamReturn{
        Team team;
        public coreTeamReturn(Team _t){
            team = _t;
        }
    }
    
}
