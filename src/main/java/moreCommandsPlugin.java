import static mindustry.Vars.content;
import static mindustry.Vars.netServer;
import static mindustry.Vars.state;
import static mindustry.Vars.world;

import java.awt.Color;
import java.util.ArrayList;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;

import functions.Players;
import functions.Players.TempData;
import functions.Players.MSG;
import functions.CommandsManager;
import functions.Effects;

import mindustry.content.Blocks;
import mindustry.core.NetClient;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration.Config;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;
import mindustry.type.UnitType;


public class moreCommandsPlugin extends mindustry.mod.Plugin {
    private double ratio = 0.6;
    private ArrayList<String> votesVNW = new ArrayList<>(), 
    		votesRTV = new ArrayList<>(), 
    		rainbowedPlayers = new ArrayList<>(), 
    		effects = new ArrayList<>(), 
    		godmodPlayers = new ArrayList<>(), 
    		adminCommands = new Seq<String>().addAll("team", "am", "kick", "pardon", "ban", "unban", "players", "kill", "tp", "core", "tchat", "spawn", "godmode").list();
    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();
    private boolean unbanConfirm = false, autoPause = false, tchat = true, niceWelcome = true, clearConfirm = false, canVote = true;
   
    //Called after all plugins have been created and commands have been registered.
    public void init() { 
    	netServer.admins.addChatFilter((p, m) -> null); //delete the tchat
    	CommandsManager.init(); //init the commands manager
    } 
    
	public moreCommandsPlugin() {
		Effects.init();

    	//clear VNW & RTV votes and disabled it on game over
        Events.on(EventType.GameOverEvent.class, e -> {
        	canVote = false;
        	votesVNW.clear();
            votesRTV.clear();
        });
        
        //re-enabled vote
        Events.on(EventType.WorldLoadEvent.class, e -> { 
        	canVote = true;
        });
        
        Events.on(EventType.PlayerConnect.class, e -> {
        	//kick the player if there is [Server], [server], or @a in his nickname
        	nameCheck(e.player, new String[]{"[Server]", "[server]", "@a", "@p", "@t", "~"});
        	
        	//prevent to duplicate nicknames
        	for (Player p : Groups.player) {
        		if (Strings.stripColors(p.name).equals(Strings.stripColors(e.player.name))) e.player.kick(KickReason.nameInUse);
        	}
        	
        	//check if the nickname is empty without the colors
        	if (Strings.stripColors(e.player.name).equals("")) e.player.kick(KickReason.nameEmpty);
        	
        	TempData.put(e.player); // add player in TempData
        	MSG.setEmpty(e.player);
        });

        Events.on(EventType.PlayerJoin.class, e -> {
        	//for me =)
        	if (e.player.uuid().equals("k6uyrb9D3dEAAAAArLs28w==") && niceWelcome) Call.sendMessage("[scarlet]\uE80F " + NetClient.colorizeName(e.player.id, e.player.name) + "[scarlet] has connected! \uE80F");
        	
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
        	if(godmodPlayers.contains(e.player.uuid())) godmodPlayers.remove(e.player.uuid());
        	if(effects.contains(e.player.uuid())) effects.remove(e.player.uuid());
        });

        //recreate the tchat for the command /tchat
        Events.on(EventType.PlayerChatEvent.class, e -> {
        	if (!e.message.startsWith("/")) {
        		if (tchat) {
        			Call.sendMessage(e.message,  NetClient.colorizeName(e.player.id, e.player.name), e.player);
        			Log.info("<" + e.player.name + ": " + e.message + ">");
        		}
        		else {
        			if (e.player.admin) {
        				Call.sendMessage(e.message, "[scarlet]<Admin>[]" + NetClient.colorizeName(e.player.id, e.player.name), e.player);
        				Log.info("<[Admin]" + e.player.name + ": " + e.message + ">");
        			}
        			else e.player.sendMessage("[scarlet]The tchat is disabled, you can't write!");
        		}
    	   }
        }); 
        
        //for players in god mode 
        Events.on(EventType.BlockBuildBeginEvent.class, e -> {
        	Player player = e.unit.getPlayer();
        
        	if (player != null) {
        		if (godmodPlayers.contains(player.uuid())) {
        			try {
        				if (e.breaking) Call.deconstructFinish(e.tile, e.tile.block(), e.unit);
        				
        				/*/!\ DISABLED BECAUSE CREATES A GHOST BLOCK THAT CAN CRASH THE SERVER AND EVERYBODY /!\
        				 * else Call.constructFinish(e.tile, e.tile.block(), e.unit, (byte)0, e.team, false);
        				 */
        			} catch (NullPointerException error) {}
        		}
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
    			Log.info("Auto pause is disabled.");
    				
    	        state.serverPaused = false;
    	        Log.info("auto-pause: " + Groups.player.size() + " player(s) connected -> Game unpaused...");
    		} else {
    			autoPause = true;
    			Log.info("Auto pause is enabled.");
    				
    			if (Groups.player.size() < 1 && autoPause) {
    				state.serverPaused = true;
    				Log.info("auto-pause: " + Groups.player.size() + " player connected -> Game paused...");
    			}
    		}
    	});
    	
        handler.register("tchat", "<on|off>", "Enabled/disabled the tchat", arg -> {
        	switch (arg[0]) {
        		case "on":
        			if (tchat) {
        				Log.err("Disabled first!");
        				return;
        			}
        			tchat = true;
        			Log.info("Tchat enabled ...");
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is enabled! [lightgray](by [scarlet][[Server][]) \n[gold]--------------------\n");
        			break;
        		
        		case "off":
        			if (!tchat) {
        				Log.err("Enabled first!");
        				return;
        			}
        			tchat = false;
        			Log.info("Tchat disabled ...");
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is disabled! [lightgray](by [scarlet][[Server][]) \n[gold]--------------------\n");
        			break;
        		
        		default: Log.err("Invalid arguments. \n - The tchat is currently @.", tchat ? "enabled" : "disabled");
        	}
        });
        
        handler.register("nice-welcome", "Nice welcome for me", arg -> {
        	niceWelcome = niceWelcome ? false : true;
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
        
        handler.register("manage-commands", "<list|commandName> [on|off]", "Enable/Disable a command. /!\\Requires server restart to apply changes.", arg -> {
        	if (arg[0].equals("list")) {
        		StringBuilder builder = new StringBuilder();
        		Seq<CommandsManager> client = new Seq<>();
        		Seq<CommandsManager> server = new Seq<>();
        		CommandsManager.copy().forEach(command -> {
        			if (command.name.startsWith("/")) client.add(command);
        			else server.add(command);
        		});
        		int best1 = bestLength(client);
        		int best2 = bestLength(server);

        		Log.info("List of all commands: ");
        		Log.info("| Server commands: Total:" + server.size + createSpaces(1+best2) + "Client commands: Total:" + client.size);
        		for (int i=0; i<Math.max(client.size, server.size); i++) {
        			try { builder.append("| | Name: " + server.get(i).name + createSpaces(best2-server.get(i).name.length()) + " - Enabled: " + server.get(i).isActivate + (server.get(i).isActivate ? " " : ""));
        			} catch (IndexOutOfBoundsException e) { builder.append("|" + createSpaces(best1+20)); }
        			try { builder.append(" | Name: " + client.get(i).name + createSpaces(best1-client.get(i).name.length()) + " - Enabled: " + client.get(i).isActivate);
        			} catch (IndexOutOfBoundsException e) {}
        			
        			Log.info(builder.toString());
        			builder = new StringBuilder();
        		}
        	} else {
        		Boolean command = CommandsManager.get(arg[0]);
        		
        		if (command == null) Log.err("This command doesn't exist!");
        		else {
        			if (arg.length == 2) {
        				switch (arg[1]) {
        					case "on": case "true": case "1":
        						CommandsManager.set(arg[0], true);
        						Log.info("Enabled ...");
        						break;
        				
        					case "off": case "false": case "0":
        						CommandsManager.set(arg[0], false);
        						Log.info("Disabled ...");
        						break;
        				
        					default:
        						Log.err("Invalid value");
        						return;
        				}
        				CommandsManager.save();
        				CommandsManager.update(handler);
        			} else Log.info("The command '" + arg[0] + "' is currently " + (command ? "enabled" : "disabled"));
        			
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
    	
    	handler.<Player>register("*","If you see an order with '*' in its description, it means you can replace all spaces with '_' in the name.", (arg, player) -> {
            player.sendMessage("If you see an order with '*' in its description, it means you can replace all spaces with '_' in the name.");
         });
         
        handler.<Player>register("ut","unit type", (args, player) -> {
        	try { player.sendMessage("You're a [sky]" + player.unit().type().name + "[]."); }
        	catch (NullPointerException e) { player.sendMessage("You're [sky]invisible ..."); }
        });
        
        handler.<Player>register("msg", "<ID|username> <message...>","Send a message to a player *", (arg, player) -> {
        	Player target = Players.find(arg[0]);
        	
            if(target == null) Players.err(player, "Player not connected or doesn't exist!");
            else {
            	MSG.get(player).setTarget(target);
            	player.sendMessage("\n[gold]Private Message send to []" + target.name);
            	target.sendMessage("\n[gold]Private Message: [white]" + player.name + "[gold] --> [sky]you[gold]\n--------------------------------\n[white]" + arg[1]);
            }
         });
        
        handler.<Player>register("r", "<message...>","Reply to the last private message received.", (arg, player) -> {
        	Player target = MSG.get(player).target;
        	
        	if (target == null) Players.err(player, "No one has sent you a private message");
        	else {
        		target = Players.find(target.uuid());
        		
        		if (target == null) Players.err(player, "This player is disconnected");
        		else {
        			MSG.get(player).setTarget(target);
                	player.sendMessage("\n[gold]Private Message send to []" + target.name);
                	target.sendMessage("\n[gold]Private Message: [white]" + player.name + "[gold] --> [sky]you[gold]\n--------------------------------\n[white]" + arg[0]);
        		}
        	}
        });

        handler.<Player>register("maps", "[page]", "List all maps on server", (arg, player) -> {
        	if(arg.length == 1 && !Strings.canParseInt(arg[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }
        	
        	Seq<mindustry.maps.Map> list = mindustry.Vars.maps.all();
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
        	if (votesVNW.contains(player.uuid()) || votesVNW.contains(netServer.admins.getInfo(player.uuid()).lastIP)) {
                player.sendMessage("You have Voted already.");
                return;
        	}

            votesVNW.add(player.uuid());
            int cur = votesVNW.size();
            int req = Mathf.ceil((float) ratio * Groups.player.size());
            Call.sendMessage("[orange]"  + NetClient.colorizeName(player.id, player.name) + "[white] has voted to send a new wave. [lightgray](" + (req-cur) + " missing)");
            
            if (cur < req) return;

            votesVNW.clear();
            Call.sendMessage("[green]Vote for Sending a New Wave is Passed. New Wave will be Spawned.");
            state.wavetime = 0f;
		});
        
        handler.<Player>register("rtv", "Rock the vote to change map", (arg, player) -> {
        	if (!canVote) return;
        	if (votesRTV.contains(player.uuid()) || votesRTV.contains(netServer.admins.getInfo(player.uuid()).lastIP)) {
                player.sendMessage("You have Voted already.");
                return;
        	}
        	
        	votesRTV.add(player.uuid());
            int cur2 = votesRTV.size();
            int req2 = Mathf.ceil((float) ratio * Groups.player.size());
            Call.sendMessage("[scarlet]RTV: [accent]" + NetClient.colorizeName(player.id, player.name) + " [white]wants to change the map, [green]" + cur2 + "[white] votes, [green]" + req2 + "[white] required.");
            
            if (cur2 < req2) return;
            
            votesRTV.clear();
            Call.sendMessage("[scarlet]RTV: [green]Vote passed, changing map...");
            Events.fire(new EventType.GameOverEvent(Team.crux));
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
                		player.sendMessage("[gold][" + i++ + "] [white]Trace info for player [accent]'" + infos.get(pI).lastName.replaceAll("\\[", "[[") + "[accent]'[white] / ID [accent]'" + infos.get(pI).id + "' ");
                	}
                	builder.append("[white] - All names used: [accent]" + infos.get(pI).names +
                			"\n[white] - IP: [accent]" + infos.get(pI).lastIP +
                			"\n[white] - All IPs used: [accent]" + infos.get(pI).ips +
                			(Players.find(infos.get(pI).id) != null ? "\n[white] - [green]Online" : "") +
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
                                    
                                    for (int i=0; i<5; i++) Call.effectReliable(mindustry.content.Fx.bubble, player.x, player.y, 10, arc.graphics.Color.valueOf(Integer.toHexString(Color.getHSBColor(hue / 360f, 1f, 1f).getRGB()).substring(2)));
                                    player.name = putColor(pData.normalizedName, hue);
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
        			
        			Player rPlayer = Players.find(arg[0]);
        			
        			if(rPlayer != null && rainbowedPlayers.contains(arg[0])) {
            			rainbowedPlayers.remove(arg[0]);
            			rPlayer.name = TempData.get(rPlayer).realName;
            			player.sendMessage("[sky]Rainbow effect toggled off for the player [accent]" + rPlayer.name + "[].");
        			} else Players.err(player, "This player not have the rainbow or doesn't exist.");
        			
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
        			Player target = Players.find(arg[1]);
        			
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
        					Call.effectReliable(effect.effect, player.x, player.y, 10, arc.graphics.Color.green);
        					Thread.sleep(50); 
        				} catch (InterruptedException e) { e.printStackTrace(); }
        			}
        		}
        	}.start();
        });
        
        handler.<Player>register("team", "[teamname|list|vanish] [username...]","change team", (args, player) ->{
            if(!player.admin()){
                player.sendMessage("[scarlet]Only admins can change team !");
                return;
            }
            StringBuilder builder = new StringBuilder();
            coreTeamReturn ret = null;
            Player target;
            
            if (args.length == 2) {
            	target = Players.find(args[1]);
            	
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
                    case "list":
                    	player.sendMessage("available teams:");
                    	builder.append(" - [accent]vanish[]\n");
                        for (Team team : Team.baseTeams) {
                        	builder.append(" - [accent]" + team.name + "[]");
                        	if (!team.cores().isEmpty()) builder.append(" | [green]" + team.cores().size + "[] core(s) found");
                        	builder.append("\n");
                        }
                        player.sendMessage(builder.toString());
                        return;   
                    default: 
                    	Players.err(player, "[lightgray]Invalid arguments");
                    	return;
                    
                }
                if(retTeam.cores().isEmpty()) {
                	Players.warn(player,"This team has no core!");
                	target.team(retTeam);
                	target.unit().controlling.each(u -> u.team(retTeam));
                	player.sendMessage("> You changed to team [sky]" + retTeam);
                	return;
                }
                
                ret =  new coreTeamReturn(retTeam);
            } else ret = getPosTeamLoc(target);

            //move team mechanic
            if(ret != null) {
                Call.setPlayerTeamEditor(target, ret.team);
                target.team(ret.team);
                target.unit().controlling.each(u -> u.team(target.team()));
                player.sendMessage("> You changed to team [sky]" + ret.team);
            } else Players.err(player, "Other team has no core, can't change!");
        });

        handler.<Player>register("am", "<message...>", "Send a message as admin", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	Call.sendMessage(arg[0], "[scarlet]<Admin>[]" + NetClient.colorizeName(player.id, player.name), player);
        });
        
        handler.<Player>register("players", "<all|online|ban>", "Gives the list of players according to the type of filter given", (arg, player) -> {
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
            		
            		builder.append("\nTotal players: [green]").append(netServer.admins.getWhitelisted().size).append("[].\n[gold]--------------------------------[]").append("\n[accent]List of players: []\n");
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
        				Groups.unit.each(u -> {if (u.team().equals(player.team())) u.kill();});
        				player.sendMessage("[green]Killed all units in team [accent]" + player.team().name);
        				return;
        			default:
        				Player other = Players.find(arg[0]);
        				if (other != null) {
        					other.unit().kill();
        					player.sendMessage("[green]Killed [accent]" + other.name);
        				}
        				else player.sendMessage("[scarlet]This player doesn't exist or not connected!");
        		}
        	}
        });
        

        handler.<Player>register("core", "<small|meduim|big>", "Spawn a core to your corrdinate", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	if(rememberSpectate.containsKey(player)) {
        		Players.err(player, "You can't build a core in vanish mode!");
        		return;
        	}
        	Players.warn(player, "This will destroy all the blocks which will be hampered by the construction of the core.");
        	
        	mindustry.world.Block core;
        	switch (arg[0]) {
        		case "small": 
        			core = Blocks.coreShard;
        			break;
        		case "meduim": 
        			core = Blocks.coreFoundation;
        			break;
        		case "big": 
        			core = Blocks.coreNucleus;
        			break;
        		default: 
        			core = null;
        	}
        	if (core != null) {
        		Call.constructFinish(player.tileOn(), core, player.unit(), (byte)0, player.team(), false);
        		player.sendMessage(player.tileOn().block() == core ? "[green]Core build." : "[scarlet]Error: Core not build.");
        	} else Players.err(player, "Core type not found:[] Please use arguments small, medium, big.");
        });
        
        handler.<Player>register("tp", "<name|x,y> [to_name|x,y]", "Teleport to position or player *", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	int[] co;
            Player target;
            Object[] result = tpSearsh(arg[0], player);
            
            if (result[0] == null && result[1] == null) return;
            else {
            	target = player;
            	if (result[1] == null) {
            		Player __temp__ = (Player) result[0];
            		co = new int[]{(int) __temp__.x/8, (int) __temp__.y/8};
            	} else co = (int[]) result[1];
            }
            	
            if (arg.length == 2) {
            	target = (Player) result[0];
            	
            	if (result[1] == null) {
					result =  tpSearsh(arg[1], player);
					
					if (result[0] == null && result[1] == null) return;
		            else {
		            	if (result[1] == null) {
		            		Player __temp__ = (Player) result[0];
		            		co = new int[]{(int) __temp__.x/8, (int) __temp__.y/8};
		            	} else co = (int[]) result[1];
		            }
				} else {
					player.sendMessage("[scarlet]Can't teleport a coordinate to a coordinate or to a player! [lightgray]It's not logic XD.");
            		return;
				}
            }
            
            if (co[0] > world.width() || co[0] < 0 || co[1] > world.height() || co[1] < 0) {
                player.sendMessage("[scarlet]Coordinates too large. Max: [orange]" + world.width() + "[]x[orange]" + world.height() + "[]. Min : [orange]0[]x[orange]0[].");
                return;
            }
            
            if (Config.strict.bool()) {
            	Config.strict.set(false);
            	Core.settings.forceSave();
            	target.unit().set(co[0]*8, co[1]*8);
            	Call.setPosition(target.con, co[0]*8, co[1]*8);
            	Config.strict.set(true);
            	Core.settings.forceSave();
            } else {
            	target.unit().set(co[0]*8, co[1]*8);
            	Call.setPosition(target.con, co[0]*8, co[1]*8);
            }
            target.snapSync();
            
            if (arg.length == 2) player.sendMessage("[green]You teleported [accent]" + target.name + "[green] to [accent]" + co[0] + "[green]x[accent]" + co[1] + "[green].");
            else player.sendMessage("[green]You teleported to [accent]" + co[0] + "[]x[accent]" + co[1] + "[].");
        });  
        
        handler.<Player>register("spawn", "<unit> [x,y|username] [teamname] [count]", "Spawn a unit ('~': your local team or coordinates). *", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	StringBuilder builder = new StringBuilder();
        	UnitType unit = content.units().find(b -> b.name.equals(arg[0]));
        	Player target;
        	Team team;
        	int count, x, y;
        	
        	if (arg.length >= 2) {
        		if (arg[1].equals("~")) target = player;
        		else target = Players.find(arg[1]);
        		
        		if (target == null) {
        			target = player;
        			try {
        				String __temp__[] = arg[1].split(",");
        				x = Strings.parseInt(__temp__[0])*8;
        				y = Strings.parseInt(__temp__[1])*8;
            		
        				if (__temp__.length > 2) {
        					Players.err(player, "Wrong coordinates!");
        					return;
        				}
        			} catch (NumberFormatException e) {
                		Players.err(player, "Player doesn't exist or wrong coordinates!");
                		return;
                	} catch (ArrayIndexOutOfBoundsException e) {
                		Players.err(player, "Wrong coordinates!");
                		return;
                	}
        		} else {
        			x = Math.round(target.x);
        			y = Math.round(target.y);
        		}
        	} else {
        		target = player;
        		x = Math.round(target.x);
        		y = Math.round(target.y);
        	}
        	
        	if (arg.length >= 3) {
        		switch (arg[2]) {
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
            			Players.err(player, "available teams: ");
            			for (Team teamList : Team.baseTeams) builder.append(" - [accent]" + teamList.name + "[]\n");
            			player.sendMessage(builder.toString());
            			return;	
        		}
        	} else team = target.team();
       
            if (arg.length == 4) {
            	if (!Strings.canParseInt(arg[3])) {
            		Players.err(player, "'count' must be number!");
            		return;
            	} else count = Strings.parseInt(arg[3]);

            } else count = 1;
            
            if (unit != null) {
            	if (team.cores().isEmpty()) Players.err(player, "The [accent]" + team.name + "[] team has no core! Units cannot spawn");
            	else {
            		for (int i=0; i<count; i++) unit.spawn(team, x, y);
                
            		player.sendMessage("[green]You are spawning [accent]" + count + " " + unit + " []for [accent]" + team + " []team at [orange]" + x/8 + "[white],[orange]" + y/8);
            	}
            } else player.sendMessage("[scarlet]Available units: []" + content.units().toString("[scarlet], []"));
        });
        
        handler.<Player>register("godmode", "[username|ID...]", "[scarlet][God][]: [gold]I'm divine!", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	Player target;
        	if (arg.length == 0) target = player;
        	else target = Players.find(arg[0]);
        	
        	if (target != null) {
        		if (godmodPlayers.contains(target.uuid())) {
        			godmodPlayers.remove(target.uuid());
        			target.unit().health = target.unit().maxHealth;
        		} else {
        			godmodPlayers.add(target.uuid());
        			target.unit().health = Integer.MAX_VALUE;
        		}
        		
        	} else {
        		player.sendMessage("[scarlet]This player doesn't exist or not connected!");
        		return;
        	}
        		
        	if (arg.length == 0) player.sendMessage("[gold]God mode is [green]" + (godmodPlayers.contains(target.uuid()) ? "enabled" : "disabled"));
        	else {
        		player.sendMessage("[gold]God mode is [green]" + (godmodPlayers.contains(target.uuid()) ? "enabled" : "disabled") + (arg.length == 0 ? "" : " for [accent]" + target.name));
        		target.sendMessage("[green]" + (godmodPlayers.contains(target.uuid()) ? "You've been put into god mode" : "You have been removed from creative mode") + " by [accent]"+ player.name);
        	}
        	
        });
        
        handler.<Player>register("tchat", "<on|off>", "Enabled/disabled the tchat", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	switch (arg[0]) {
        		case "on":
        			if (tchat) {
        				Players.err(player, "Disabled first!");
        				return;
        			}
        			tchat = true;
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is enabled! [lightgray](by " + player.name + "[lightgray]) \n[gold]--------------------\n");
        			Log.info("Tchat enabled by " + player.name + ".");
        			break;
        		case "off":
        			if (!tchat) {
        				Players.err(player, "Enabled first!");
        				return;
        			}
        			tchat = false;
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is disabled! [lightgray](by " + player.name + "[lightgray]) \n[gold]--------------------\n");
        			Log.info("Tchat disabled by " + player.name + ".");
        			break;
        		default: Players.err(player, "Invalid arguments.[] \n - The tchat is currently [accent]%s[].", tchat ? "enabled" : "disabled");
        	}
        });   
        
        handler.<Player>register("kick", "<ID|username...>", "Kick a person by name or ID", (arg, player) -> {
            if (!Players.adminCheck(player)) return;

            Player target = Players.find(arg[0]);
            
            if (target != null) {
                Call.sendMessage("[scarlet]/!\\[] " + target.name + "[scarlet] has been kicked of the server.");
                target.kick(KickReason.kick);
                Players.info(player, "It is done.");
            } else Players.err(player, "Nobody with that name or ID could be found...");
        });   
        
        handler.<Player>register("pardon", "<ID>", "Pardon a player by ID and allow them to join again", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);
        	
        	if (info != null) {
        		info.lastKicked = 0;
        		Players.info(player, "Pardoned player: [accent]%s", info.lastName);
        	} else Players.err(player, "That ID can't be found.");
        });

        handler.<Player>register("ban", "<id|name|ip> <username|IP|ID...>", "Ban a person", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;

        	switch (arg[0]) {
        		case "id":
        			netServer.admins.banPlayerID(arg[1]);
        			break;
        		case "name":
        			Player target = Players.find(arg[1]);
        			if (target != null) netServer.admins.banPlayer(target.uuid());
        			else Players.err(player, "No matches found.");
        			break;
        		case "ip":
        			netServer.admins.banPlayerIP(arg[1]);
        			break;
        		default:
        			Players.err(player, "Invalid type.");
        			return;
        	}
            Players.info(player, "Banned.");
            
            for (Player gPlayer : Groups.player) {
                if (netServer.admins.isIDBanned(gPlayer.uuid())) {
                    Call.sendMessage("[scarlet]/!\\[] " + gPlayer.name + "[scarlet] has been banned of the server.");
                    gPlayer.con.kick(KickReason.banned);
                }
            }
        });
        
        handler.<Player>register("unban", "<ip|ID>", "Unban a person", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
       
            if (netServer.admins.unbanPlayerIP(arg[0]) || netServer.admins.unbanPlayerID(arg[0])) Players.info(player, "Unbanned player: [accent]%s", arg[0]);
            else Players.err(player, "That IP/ID is not banned!");
        });

    }
    
	private String createSpaces(int length) {
    	String spaces = "";
    	for (int i=0; i<length; i++) spaces+=" ";
    	return spaces;
    }
    
    private int bestLength(Seq<CommandsManager> list) {
    	int best = 0;
    	
    	for (CommandsManager str : list) {
    		if (str.name.length() > best) best = str.name.length();
    	}
    	return best;
    }
    
    private void setHandler(CommandHandler handler) {
    	new Thread() {
			public void run() {
				try {
					Thread.sleep(1000);
					CommandsManager.load(handler, netServer.clientCommands.getPrefix().equals(handler.getPrefix()));
				} catch (InterruptedException e) { e.printStackTrace(); }
			}
    	}.start();
    }

    private void nameCheck(Player player, String[] bannedNames) {
    	for (String name : bannedNames) {
    		if (Strings.stripColors(player.name).equals(name)) {
    			player.kick("[scarlet]Invalid nickname: []Please don't use [accent]'" + name + "'[white] in your nickname.");
    			return;
    		}
    	}
    }
    
    private String putColor(String str, int hue) {
    	String out = "";
    	for (char c : str.toCharArray()) {
    		if (hue < 360) hue+=10;
    		else hue = 0;
    		
    		out += "[#" + Integer.toHexString(Color.getHSBColor(hue / 360f, 1f, 1f).getRGB()).substring(2) + "]" + c;
    	}
        return out;
    }
    
    private Object[] tpSearsh(String str, Player pDefault) {
    	Player target = Players.find(str);
    	String __temp__[];
    	
    	if (target == null) {
    		__temp__ = str.split(",");
    		
    		if (!Strings.canParseInt(__temp__[0]) || !Strings.canParseInt(__temp__[1])) {
    			Players.err(pDefault, "Player [orange]" + str + "[] doesn't exist or not connected!");
				return new Object[]{null, null};
    		} else if (__temp__.length != 2) {
    			Players.err(pDefault, "Wrong coordinates!");
    			return new Object[]{null, null};
    		} else return new Object[]{pDefault, new int[]{Strings.parseInt(__temp__[0]), Strings.parseInt(__temp__[1])}};

    	} else return new Object[]{target, null};
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