import static mindustry.Vars.content;
import static mindustry.Vars.maps;
import static mindustry.Vars.netServer;
import static mindustry.Vars.state;
import static mindustry.Vars.world;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.serialization.Jval;

import mindustry.content.Blocks;
import mindustry.core.NetClient;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.net.Administration.ActionType;
import mindustry.net.Administration.Config;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;

import util.*;
import data.*;


public class moreCommandsPlugin extends mindustry.mod.Plugin {
    private static Seq<String> adminCommands = new Seq<String>().addAll("team", "am", "kick", "pardon", "ban", "unban", "players", "kill", "tp", "core", "tchat", "spawn", "godmode", "mute", "unmute");
    private static Map selectedMap;
    private static float ratio = 0.6f;
    private static int waveVoted = 1;
    private static boolean unbanConfirm = false,
    	autoPause = false,
    	tchat = true,
    	niceWelcome = true,
    	clearConfirm = false,
    	canVote = true;
   
    //Called after all plugins have been created and commands have been registered.
    public void init() {
    	checkUpdates(); //Check if a new update is available 
    	
    	//Filter for muted players and if tchat is disabled
    	netServer.admins.addChatFilter((p, m) -> {
    		if (tchat) {
    			if (TempData.get(p).isMuted) {
    				Players.err(p, "You're muted, you can't speak.");
    				m = null;
    			}
    		
    		} else {
    			if (p.admin) Call.sendMessage(m, "[scarlet]<Admin>[]" + NetClient.colorizeName(p.id, p.name), p);
    			else p.sendMessage("[scarlet]The tchat is disabled, you can't write!");
    			m = null;
    		}
    		
    		return m;
    	});
    	
    	//Filter for players in GodMode
    	netServer.admins.addActionFilter(a -> {
    		if ((a.type == ActionType.placeBlock || a.type == ActionType.breakBlock) && TempData.get(a.player).inGodmode) {
    			if (a.type == ActionType.placeBlock) Call.constructFinish(a.tile, a.block, a.unit, (byte) a.rotation, a.player.team(), a.config);
    			else Call.deconstructFinish(a.tile, a.block, a.unit);
    			return false;
    		}
    		return true;
    	});

    	
    	CM.init(); //init the commands manager
    	
    	//pause the game if no one is connected
    	if (Groups.player.size() < 1 && autoPause) {
			state.serverPaused = true;
			Log.info("auto-pause: " + Groups.player.size() + " player connected -> Game paused...");
		}
    } 
    
	public moreCommandsPlugin() {
		Effects.init();
		BM.init();
		loadSettings();

    	//clear VNW & RTV votes and disabled it on game over
        Events.on(EventType.GameOverEvent.class, e -> {
        	canVote = false;
        	TempData.setAll(p -> p.votedVNW = false);
        	TempData.setAll(p -> p.votedRTV = false);
        });
        
        Events.on(EventType.WorldLoadEvent.class, e -> canVote = true); //re-enabled votes
        
        Events.on(EventType.PlayerConnect.class, e -> {
        	String name = TempData.putDefault(e.player).stripedName; //add player in TempData
        	BM.nameCheck(e.player); //check the nickname of this player
        	
        	//check if the nickname is empty without colors
        	if (name.isBlank()) e.player.kick(KickReason.nameEmpty);
        	
        	//prevent to duplicate nicknames
        	for (Player p : Groups.player) 
        		if (TempData.get(p).stripedName.equals(name)) e.player.kick(KickReason.nameInUse);

        	MSG.setEmpty(e.player);
        });
        
        Events.on(EventType.PlayerJoin.class, e -> {
        	//for me =)
        	if (TempData.get(e.player).isCreator) { 
        		if (niceWelcome) Call.sendMessage("[scarlet]\uE80F " + NetClient.colorizeName(e.player.id, e.player.name) + "[scarlet] has connected! \uE80F   [lightgray]Everyone say: Hello creator! XD");
        		Call.infoMessage(e.player.con, "Hello creator ! =)");
        	}
        	
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
        	Log.info(niceWelcome ? "Enabled..." : "Disabled...");
        });
        
        handler.register("commands", "<list|commandName|reset> [on|off]", "Enable/Disable a command. /!\\Requires server restart to apply changes.", arg -> {
        	if (arg[0].equals("list")) {
        		StringBuilder builder = new StringBuilder();
        		Seq<CM.Commands> client = new Seq<CM.Commands>().addAll(CM.copy().filter(c -> c.name.startsWith("/")));
        		Seq<CM.Commands> server = new Seq<CM.Commands>().addAll(CM.copy().filter(c -> !c.name.startsWith("/")));
        		int best1 = Strings.bestLength(client.map(c -> c.name).list());
        		int best2 = Strings.bestLength(server.map(c -> c.name).list());
        		
        		Log.info("List of all commands: ");
        		Log.info(Strings.lJust("| Server commands: Total:" + server.size, 28+best2) + "Client commands: Total:" + client.size);
        		for (int i=0; i<Math.max(client.size, server.size); i++) {
        			try { builder.append(Strings.mJust("| | Name: " + server.get(i).name, " - Enabled: " + (server.get(i).isActivate ? "true " : "false"), 27+best2)); } 
        			catch (IndexOutOfBoundsException e) { builder.append("|" + Strings.createSpaces(best1+20)); }
        			try { builder.append(Strings.lJust(" | Name: " + client.get(i).name, 9+best1) + " - Enabled: " + client.get(i).isActivate); } 
        			catch (IndexOutOfBoundsException e) {}
        			
        			Log.info(builder.toString());
        			builder = new StringBuilder();
        		}
        		
        	} else if (arg[0].equals("reset")) {
        		CM.copy().each(c -> CM.get(c.name).set(true));
        		CM.save();
        		CM.update(handler);
				Log.info("All command statuses have been reset.");
        		
        		
        	} else {
        		CM.Commands command = CM.get(arg[0]);
        		
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
        				CM.save();
        				CM.update(handler);
        			
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
        				int unitCounter = 0, blockCounter = 0;
        				
        				unitCounter += Groups.unit.size();
        				Groups.unit.each(u -> u.kill());
        				for (int x=0; x<world.width(); x++) {
        					for (int y=0; y<world.height(); y++) {
        						block = world.build(x, y);
        						
        						if (block != null && (block.block != Blocks.coreShard && block.block != Blocks.coreNucleus && block.block != Blocks.coreFoundation)) {
        							blockCounter++;
        							block.kill();
        						}
        					}
        				}
                		Groups.fire.each(a -> a.remove());
                		unitCounter += Groups.unit.size();
                		Groups.unit.each(u -> u.kill());
                		
                		Log.info(Strings.format("Map cleaned! (Killed @ units and destroy @ blocks)", unitCounter, blockCounter));
                		Call.infoMessage(Strings.format("[green]Map cleaned! [lightgray](Killed [scarlet]@[] units and destroy [scarlet]@[] blocks)", unitCounter, blockCounter));
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
        	BM.blacklistCommand(arg);
        });
        
        handler.register("anti-vpn", "[on|off]", "Anti VPN service", arg -> {
        	if (arg.length == 0) {
        		Log.info("Anti VPN is currently @.", BM.antiVpn ? "enabled" : "disabled");
        		return;
        	}
        	
        	if (BM.antiVPNEnabled) {
        		switch (arg[0]) {
	        		case "on": case "true":
	        			if (BM.antiVpn) {
	        				Log.err("Disabled first!");
	        				return;
	        			}
	        			BM.antiVpn = true;
	        			Log.info("Anti VPN enabled ...");
	        			saveSettings();
	        			break;
	        		
	        		case "off": case "false":
	        			if (!BM.antiVpn) {
	        				Log.err("Enabled first!");
	        				return;
	        			}
	        			BM.antiVpn = false;
	        			Log.info("Anti VPN disabled ...");
	        			saveSettings();
	        			break;
	        		
	        		default: 
	        			Log.err("Invalid arguments. \n - Anti VPN is currently @.", BM.antiVpn ? "enabled" : "disabled");
	        			return;
        		}
        	} else Log.err("Anti VPN cannot be activated because it could not load properly.");
        	
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
        	
        	Seq<CommandHandler.Command> commands = handler.getCommandList();
        	if (!player.admin) {
        		handler.getCommandList().forEach(command -> {
        			if (adminCommands.contains(command.text)) commands.remove(command);
        		});
        	}
        	
        	int lines = 8,
        			page = arg.length == 1 ? Strings.parseInt(arg[0]) : 1,
        			pages = Mathf.ceil(commands.size / lines);
        	if (commands.size % lines != 0) pages++;
        	
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
            	String message = String.join(" ", result.rest);
            	if (Strings.stripColors(message).isBlank()) Players.err(player, "Please don't send an empty message.");
            	else {
            		MSG.get(player).setTarget(target);
            		player.sendMessage("\n[gold]Private Message send to [white]" + target.name + "\n[gold]Content: [white]" + message);
            		target.sendMessage("\n[gold]Private Message: [white]" + player.name + "[gold] --> [sky]you[gold]\n--------------------------------\n[white]" + message);
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
                		player.sendMessage("\n[gold]Private Message send to [white]" + target.name + "\n[gold]Content: [white]" + arg[0]);
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
        	
        	StringBuilder builder = new StringBuilder();
        	Seq<Map> list = mindustry.Vars.maps.all();
        	Map map;
        	int page = arg.length == 1 ? Strings.parseInt(arg[0]) : 1,
        			lines = 8, 
        			pages = Mathf.ceil(list.size / lines);
        	if (list.size % lines != 0) pages++;
            
            if (page > pages || page < 1) {
            	player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and [orange]" + pages + "[].");
            	return;
            }
            
            builder.append("\n[orange]---- [gold]Maps list [lightgray]" + page + "[gray]/[lightgray]" + pages + "[orange] ----");
            for (int i=(page-1)*lines; i<lines*page;i++) {
            	try { 
            		map = list.get(i);
            		builder.append("\n[orange]  - [white]" +  map.name() + 
            		"[orange] | [white]" + map.width + "x" + map.height + 
            		"[orange] | [green]" + (map.custom ? "Custom" : "Builtin") +
            		"[orange] | By: [sky]" + map.author());
            	} catch (IndexOutOfBoundsException e) { break; }
            }
            builder.append("\n[orange]-----------------------");
            player.sendMessage(builder.toString());
        });
        
        handler.<Player>register("vnw", "[number]", "(VoteNewWave) Vote for Sending a new Wave", (arg, player) -> {
        	if (!canVote) return;
        	TempData target = TempData.get(player);
        	if (target.votedVNW) {
                player.sendMessage("You have Voted already.");
                return;
        	}
        	
        	if (arg.length == 1) {
        		if (player.admin) {
        			if(Strings.canParseInt(arg[0])) waveVoted = Strings.parseInt(arg[0]);
        			else {
                    	Players.err(player, "Please type a number");
                        return;
                    }
        		} else {
        			Players.err(player, "You don't have the permission to use arguments!");
        			return;
        		}
        	} else waveVoted = 1;
        	
            target.votedVNW = true;
            int cur = TempData.filter(p -> p.votedVNW).size;
            int req = Mathf.ceil((float) ratio * Groups.player.size());
            Call.sendMessage(NetClient.colorizeName(player.id, player.name) + 
            	"[orange] has voted to "+ (waveVoted == 1 ? "send a new wave" : "skip [green]" + waveVoted + " waves") + ". [lightgray](" + (req-cur) + " votes missing)");
            
            if (cur < req) return;

            TempData.setAll(p -> p.votedVNW = false);
            Call.sendMessage("[green]Vote for "+ (waveVoted == 1 ? "Sending a new wave" : "Skiping [scarlet]" + waveVoted + "[] waves") + " is Passed. New Wave will be Spawned.");
            state.wave += waveVoted-1;
            state.wavetime = 0f;
		});
        
        handler.<Player>register("rtv", "[mapName...]", "Rock the vote to change map", (arg, player) -> {
        	if (!canVote) return;
        	if(Groups.player.size() < 3 && !player.admin){
                player.sendMessage("[scarlet]At least 3 players are needed to start a vote.");
                return;
            }
        	
        	int RTVsize = TempData.filter(p -> p.votedRTV).size;
        	TempData target = TempData.get(player);
        	
        	if (arg.length == 1) {
        		if (RTVsize == 0) {
        			selectedMap = maps.all().find(map -> Strings.stripColors(map.name().replace('_', ' ')).equalsIgnoreCase(Strings.stripColors(arg[0])));
        			if (selectedMap == null) selectedMap = maps.all().find(map -> Strings.stripColors(map.name().replace('_', ' ')).equalsIgnoreCase(Strings.stripColors(arg[0]).replace('_', ' ')));
        			if (selectedMap == null) {
        				Players.err(player, "No map with name '%s' found.", arg[0]);
        				return;
        			} else maps.queueNewPreview(selectedMap);
        			
        		} else {
        			Players.err(player, "A vote to change the map is already in progress! [lightgray](selected map:[white] " + selectedMap.name() + "[lightgray])");
        			return;
        		}
        	} else if (RTVsize == 0) selectedMap = maps.getNextMap(Gamemode.valueOf(Core.settings.getString("lastServerMode")), state.map);
        	if (target.votedRTV) {
                player.sendMessage("You have Voted already.");
                return;
        	}
        	
        	target.votedRTV = true;
        	RTVsize += 1;
            int req2 = Mathf.ceil((float) ratio * Groups.player.size());
            Call.sendMessage("[scarlet]RTV: [accent]" + NetClient.colorizeName(player.id, player.name) + " [white]wants to change the map, [green]" + RTVsize 
            	+ "[white] votes, [green]" + req2 + "[white] required. [lightgray](selected map: [white]" + selectedMap.name() + "[lightgray])");
            
            if (RTVsize < req2) return;
            
            TempData.setAll(p -> p.votedRTV = false);
            Call.sendMessage("[scarlet]RTV: [green]Vote passed, changing map ... [lightgray](selected map: [white]" + selectedMap.name() + "[lightgray])");
            new RTV(selectedMap, Team.crux);
        });

        handler.<Player>register("info-all", "[ID|username...]", "Get all player information", (arg, player) -> {
        	StringBuilder builder = new StringBuilder();
        	ObjectSet<PlayerInfo> infos = ObjectSet.with(player.getInfo());
			int i = 1;
        	boolean mode = true;
        	
        	if (arg.length == 1) {
        		if (player.admin) {
        			Player test = Players.findByNameOrID(arg[0]).player;
        			
        			if (test == null) infos = netServer.admins.searchNames(arg[0]);
        			if (infos.size == 0) infos = ObjectSet.with(netServer.admins.getInfoOptional(arg[0]));
        			if (infos.size == 0) {
        				Players.err(player, "This player doesn't exist!");
        				return;
        			} else;
        			
        		} else {
        			Player test = Players.findByName(arg[0]).player;
        			
        			if (test == null) {
        				test = Players.findByID(arg[0]).player;
        				
        				if (test == null) Players.err(player, "Player not connected or doesn't exist!");
        				else  Players.err(player, "You don't have permission to search a player by their ID!");
        				return;
        			
        			} else infos = ObjectSet.with(test.getInfo());
        		}
        		mode = false;
        	} 
        	
        	if (player.admin && !mode) {
        		builder.append("[gold]----------------------------------------");
            	builder.append("\n[scarlet]-----"+ "\n[white]Players found: [gold]" + infos.size + "\n[scarlet]-----");
            	player.sendMessage(builder.toString());
            	builder = new StringBuilder();
        	}
        	
        	for (PlayerInfo pI : infos) {
        		if (player.admin && !mode) 
        			player.sendMessage("[gold][" + i++ + "] [white]Trace info for player [accent]'" + pI.lastName.replaceAll("\\[", "[[") 
                			+ "[accent]'[white] / ID [accent]'" + pI.id + "' ");
        		 else {
        			builder.append("[white]Player name [accent]'" + pI.lastName.replaceAll("\\[", "[[") + "[accent]'"+ (mode ? "[white] / ID [accent]'" + pI.id + "'" : ""));
             		builder.append("\n[gold]----------------------------------------[]\n");
        		}

        		builder.append("[white] - All names used: [accent]" + pI.names + 
                	(Players.findByID(pI.id).player != null ? "\n[white] - [green]Online" : "") +
                	(TempData.creatorID.equals(pI.id) ? "\n[white] - [sky]Creator of moreCommands [lightgray](the plugin that this server uses)" : "") +
                	(player.admin ? "\n[white] - IP: [accent]" + pI.lastIP + "\n[white] - All IPs used: [accent]" + pI.ips : "") +
                	"\n[white] - Times joined: [green]" + pI.timesJoined +
                	"\n[white] - Times kicked: [scarlet]" + pI.timesKicked +
                	(player.admin ? "\n[white] - Is baned: [accent]" + pI.banned : "") +
                	"\n[white] - Is admin: [accent]" + pI.admin +
                	"\n[gold]----------------------------------------");
                	
                if (mode) Call.infoMessage(player.con, builder.toString());
                else player.sendMessage(builder.toString());
                builder = new StringBuilder();
        	}
        });
        
        handler.<Player>register("rainbow", "[ID|username...]", "[#ff0000]R[#ff7f00]A[#ffff00]I[#00ff00]N[#0000ff]B[#2e2b5f]O[#8B00ff]W[#ff0000]![#ff7f00]!", (arg, player) -> {
        	TempData target = TempData.get(player);
        	
        	if (arg.length == 0) {
        		if(target.rainbowed) {
        			player.sendMessage("[sky]Rainbow effect toggled off.");
        			target.rainbowed = false;
        			player.name = target.realName;
        		} else {
        			player.sendMessage("[sky]Rainbow effect toggled on.");
        			target.rainbowed = true;
        			target.hasEffect = false;

        			new Thread() {
        				public void run() {
        					while(target.rainbowed) {
        						try {
                                    if (target.hue < 360) target.hue+=5;
                                    else target.hue = 0;
                                    
                                    for (int i=0; i<5; i++) Call.effect(mindustry.content.Fx.bubble, player.x, player.y, 10, 
                                    	arc.graphics.Color.valueOf(Integer.toHexString(java.awt.Color.getHSBColor(target.hue / 360f, 1f, 1f).getRGB()).substring(2)));
                                    player.name = Strings.RGBString(target.stripedName, target.hue);
                                    
                                    Thread.sleep(50);
        						} catch (InterruptedException e) { e.printStackTrace(); }
        					}
        				}
        			}.start();
        		}
        	} else {
        		if (player.admin) {
        			Players.warn(player, "This will remove the rainbow from the person matching the argument.");
        			
        			TempData rPlayer = TempData.get(Players.findByNameOrID(arg[0]).player);
        			
        			if(rPlayer != null && rPlayer.rainbowed) {
        				rPlayer.rainbowed = false;
            			rPlayer.player.name = rPlayer.realName;
            			player.sendMessage("[sky]Rainbow effect toggled off for the player [accent]" + rPlayer.player.name + "[].");
        			} else Players.err(player, "This player don't have the rainbow or not connected.");
        			
        		} else Players.err(player, "You don't have the permission to use arguments!");
        	}
        	
        });
        
        handler.<Player>register("effect", "[list|name|id] [page|ID|username...]","Gives you a particles effect. [scarlet] May cause errors", (arg, player) -> {
        	Effects effect;
        	StringBuilder builder = new StringBuilder();
        	TempData target = TempData.get(player);
        	
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
        		if (target.hasEffect) {
        			target.hasEffect = false;
        			player.sendMessage("[green]Removed particles effect.");
        			return;
        		} else {
        			target.rainbowed = false;
        			target.hasEffect = true;
        			int r = new java.util.Random().nextInt(172);
        			effect = Effects.getByID(r);
        			
        			player.sendMessage("Randomised effect ...");
        			player.sendMessage("[green]Start particles effect [accent]" + effect.id + "[scarlet] - []" + effect.name);
        		}
        	} else if (arg.length == 2) {
        		if (player.admin) {
        			TempData target2 = TempData.get(Players.findByNameOrID(arg[1]).player);
        			
        			if(target2 == null) Players.err(player, "Player not connected or doesn't exist!");
        			else {
        				if (target2.hasEffect) {
        					target2.hasEffect = false;
        					player.sendMessage("[green]Removed particles effect for [accent]" + target2.player.name);
        				} else Players.err(player, "This player don't have particles effect");
        			}
        		} else Players.err(player, "You don't have the permission to use arguments!");
        		return;
        		
        	} else {
        		if (target.hasEffect) {
        			Players.err(player, "Please disabled first [lightgray](tip: /effect)");
        			return;
        		} else target.hasEffect = true;
        		target.rainbowed = false;		
        		
        		if(Strings.canParseInt(arg[0])) effect = Effects.getByID(Strings.parseInt(arg[0])-1);
        		else effect = Effects.getByName(arg[0]);
        		
        		if (effect == null) {
        			Players.err(player, "Particle effect don't exist");
        			return;
        		} else player.sendMessage("[green]Start particles effect [accent]" + effect.id + "[scarlet] - []" + effect.name);
        	}

        	new Thread() {
        		public void run() {
        			while(target.hasEffect) {
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
            TempData target;
            
            if (args.length == 2) {
            	target = TempData.get(Players.findByName(args[1]).player);
            	
            	if (target == null) {
            		Players.err(player, "This player doesn't exist or not connected!");
            		return;
            	}
            } else target = TempData.get(player);
            
            if(target.spectate()){
                player.sendMessage(">[orange] transferring back to last team");
                target.player.team(target.spectate);
                Call.setPlayerTeamEditor(target.player, target.spectate);
                target.spectate = null;
                target.player.name = target.realName;
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
                    	target.spectate = target.player.unit().team;
                    	target.rainbowed = false;
                    	target.hasEffect = false;
                    	
                        target.player.team(Team.all[8]);
                        Call.setPlayerTeamEditor(target.player, Team.all[8]);
                        target.player.unit().kill();
                        player.sendMessage("[green]VANISH MODE[] \nuse /team to go back to player mode.");
                        target.player.name = "";
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
                	target.player.team(retTeam);
                	target.player.unit().controlling.each(u -> u.team(retTeam));
                	player.sendMessage("> You changed [accent]" + (args.length == 2 ? target.player.name + " " : "") + "[white]to team [sky]" + retTeam);
                	return;
                }
                
                ret =  new coreTeamReturn(retTeam);
            } else ret = getPosTeamLoc(target.player);

            //move team mechanic
            if(ret != null) {
                Call.setPlayerTeamEditor(target.player, ret.team);
                target.player.team(ret.team);
                target.player.unit().controlling.each(u -> u.team(target.player.team()));
                player.sendMessage("> You changed [accent]" + (args.length == 2 ? target.player.name : "") + "[white] to team [sky]" + ret.team);
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
            		if (netServer.admins.getBanned().isEmpty()) {
            			player.sendMessage("[green]No player banned");
            			return;
            		}
            		builder.append("\nTotal banned players : [green]"+ netServer.admins.getBanned().size + ". \n[gold]-------------------------------- \n[accent]Banned Players:");
            		netServer.admins.getBanned().each(p -> {
            			builder.append("\n[white]======================================================================\n" +
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

        handler.<Player>register("kill", "[@e|@a|@t|username...]", "Kill a player. @e: all entity. @a: all player. @t: all entity in your team", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
            
        	if (arg.length == 0) player.unit().kill();
        	else {
        		int counter;
        		
        		switch (arg[0]) {
        			case "@a":
        				counter = Groups.player.size();
        				Groups.player.each(p -> p.unit().kill());
        				player.sendMessage("[green]Killed " + counter + " players");
        				return;
        			case "@e":
        				counter = Groups.unit.size();
        				Groups.unit.each(u -> u.kill());
        				player.sendMessage("[green]Killed " + counter + " entities");
        				return;
        			case "@t":
        				counter = Groups.unit.count(t -> t.team().equals(player.team()));
        				Groups.unit.each(t -> t.team().equals(player.team()), u -> u.kill());
        				player.sendMessage("[green]Killed " + counter + " units in team [accent]" + player.team().name);
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
        

        handler.<Player>register("core", "[small|medium|big]", "Spawn a core to your corrdinate", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	if(TempData.get(player).spectate()) {
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
        		default: core = Blocks.coreShard;
        	}
        	
        	Players.warn(player, "This will destroy all the blocks which will be hampered by the construction of the core.");
        	Call.constructFinish(player.tileOn(), core, player.unit(), (byte)0, player.team(), false);
        	player.sendMessage(player.tileOn().block() == core ? "[green]Core build." : "[scarlet]Error: Core not build.");
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
        	TempData target = TempData.get(player);
        	if (Players.adminCheck(player) || (target.isCreator && niceWelcome));
        	else return;
        	
        	if (arg.length != 0) target = TempData.get(Players.findByName(arg[0]).player);
        	
        	if (target != null) {
        		if (target.inGodmode) {
        			target.player.unit().type.buildSpeed = target.savedBuildSpeed;
        			target.player.unit().health = target.player.unit().maxHealth;
        			target.inGodmode = false;
        		
        		} else {
        			target.savedBuildSpeed = target.player.unit().type.buildSpeed;
        			target.player.unit().health = Integer.MAX_VALUE;
        			target.player.unit().type.buildSpeed = Float.MAX_VALUE;
        			target.inGodmode = true;
        		}
        		
        	} else {
        		player.sendMessage("[scarlet]This player doesn't exist or not connected!");
        		return;
        	}
        		
        	if (arg.length == 0) player.sendMessage("[gold]God mode is [green]" + (target.inGodmode ? "enabled" : "disabled"));
        	else {
        		player.sendMessage("[gold]God mode is [green]" + (target.inGodmode ? "enabled" : "disabled") + (arg.length == 0 ? "" : "[] for [accent]" + target.player.name));
        		target.player.sendMessage((target.inGodmode ? "[green]You've been put into god mode" : "[red]You have been removed from creative mode") + " by [accent]"+ player.name);
        	}
        	
        });
        
        handler.<Player>register("tchat", "[on|off]", "Enabled/disabled the tchat", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	if (arg.length == 0) {
        		player.sendMessage("The tchat is currently "+ (tchat ? "enabled." : "disabled."));
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
        	TempData target = TempData.get(result.player);
            
            if (target == null) Players.err(player, "Nobody with that name or ID could be found...");
            else {
            	String message = String.join(" ", result.rest);
            	target.isMuted = true;
            	Call.sendMessage("[scarlet]/!\\" + NetClient.colorizeName(target.player.id, target.player.name) + "[scarlet] has been muted of the server.");
            	Call.infoMessage(target.player.con, "You have been muted! [lightgray](by " + player.name + "[lightgray]) \n[scarlet]Reason: []" + (message.isBlank() ?  "<unknown>" : message));
            }
        });
        
        handler.<Player>register("unmute", "<username|ID>", "unmute a person by name or ID", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;

            TempData target = TempData.get(Players.findByNameOrID(arg[0]).player);
            
            if (target == null) Players.err(player, "Nobody with that name or ID could be found...");
            else {
            	target.isMuted = false;
            	Call.infoMessage(target.player.con, "You have been unmuted! [lightgray](by " + player.name + "[lightgray])");
            }
        });
        
        handler.<Player>register("kick", "<username|ID> [reason...]", "Kick a person by name or ID", (arg, player) -> {
            if (!Players.adminCheck(player)) return;

            Players result = Players.findByNameOrID(arg[0] + (arg.length == 2 ? " " + arg[1] : ""));
        	Player target = result.player;
            
            if (target == null) Players.err(player, "Nobody with that name or ID could be found...");
            else {
            	String message = String.join(" ", result.rest);
                Call.sendMessage("[scarlet]/!\\" + NetClient.colorizeName(target.id, target.name) + "[scarlet] has been kicked of the server.");
                if (arg.length == 2) target.kick("You have been kicked from the server!\n[scarlet]Reason: []" + (message.isBlank() ?  "<unknown>" : message));
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
        		String message = String.join(" ", result.rest);
        		netServer.admins.banPlayer(target.uuid());
        		Call.sendMessage("[scarlet]/!\\ " + NetClient.colorizeName(target.id, target.name) + "[scarlet] has been banned of the server.");
        		if (arg.length == 2) target.kick("You are banned on this server!!\n[scarlet]Reason: []" + (message.isBlank() ?  "<unknown>" : message));
                else target.kick(KickReason.banned);
        	}
        });
        
        handler.<Player>register("unban", "<ID>", "Unban a person", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
       
            if (netServer.admins.unbanPlayerID(arg[0])) Players.info(player, "Unbanned player: [accent]%s", arg[0]);
            else Players.err(player, "That IP/ID is not banned!");
        });

    }
    
	private void loadSettings() {
    	try {
    		if (Core.settings.has("moreCommands")) {
        		String[] temp = Core.settings.getString("moreCommands").split(" \\| ");
        		autoPause = Boolean.parseBoolean(temp[0]);
        		tchat = Boolean.parseBoolean(temp[1]);
        		BM.antiVpn = Boolean.parseBoolean(temp[2]);
        	} else saveSettings();

    	} catch (Exception e) {
    		saveSettings();
    		loadSettings();
    	}
    }
    
    private void saveSettings() {
    	Core.settings.put("moreCommands", String.join(" | ", autoPause+"", tchat+"", BM.antiVpn+""));
    	Core.settings.forceSave();
    }

    private void setHandler(CommandHandler handler) {
    	new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
					CM.load(handler);
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
    	}.start();
    }
    
    private void checkUpdates() {
    	Core.net.httpGet(mindustry.Vars.ghApi+"/repos/ZetaMap/moreCommands/releases/latest", s -> {
    		Jval content = Jval.read(s.getResultAsString());
    		if (Strings.parseFloat(content.get("tag_name").asString().substring(1)) > Strings.parseFloat(mindustry.Vars.mods.getMod("morecommands").meta.version))
    			Log.info("A new version of moreCommands is available! See https://github.com/ZetaMap/moreCommands/releases/ to download it!");
    	}, f -> Log.err(f));
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