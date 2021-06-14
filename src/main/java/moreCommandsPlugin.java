import static mindustry.Vars.content;
import static mindustry.Vars.maps;
import static mindustry.Vars.netServer;
import static mindustry.Vars.state;
import static mindustry.Vars.world;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.CommandHandler.Command;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer.Task;

import functions.PlayerFunctions;
import functions.TempPlayerData;

import mindustry.content.Blocks;
import mindustry.core.NetClient;
import mindustry.game.EventType.BlockBuildBeginEvent;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.game.EventType.PlayerConnect;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.NetConnection;
import mindustry.net.Packets.KickReason;
import mindustry.type.UnitType;
import mindustry.world.Block;


public class moreCommandsPlugin extends Plugin {
    Task task;
    private double ratio = 0.6;
    private HashSet<String> votesVNW = new HashSet<>(), votesRTV = new HashSet<>();
    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();
    private static ArrayList<String> rainbowedPlayers = new ArrayList<>();
    private static ObjectMap<Player, Integer> creativePlayers = new ObjectMap<>();
    private boolean confirm = false, autoPause = false, tchat = true;
    
    public void init() { netServer.admins.addChatFilter((player, message) -> null); } //delete the tchat
	public moreCommandsPlugin() {
    	Events.on(PlayerJoin.class, e -> TempPlayerData.tempPlayerDatas.put(e.player.uuid(), new TempPlayerData(0, e.player.name, e.player.id))); // add player in TempPlayerData
		Events.on(PlayerLeave.class, e -> TempPlayerData.tempPlayerDatas.remove(e.player.uuid())); // remove player in TempPlayerData

    	//clear VNW & RTV votes on game over
        Events.on(GameOverEvent.class, e -> {
        	votesVNW.clear();
            votesRTV.clear();
        });
        
        //kick the player if there is [Server] or [server] in his nickname
        Events.on(PlayerConnect.class, e -> {
        	if (e.player.name.contains("[Server]") || e.player.name.contains("[server]")) {
        		e.player.kick("Please don't use [Server] or [server] in your username!");
        		return;
        	}
        });

        //unpause the game if one player is connected.
        Events.on(PlayerJoin.class, e -> {
        	if (Groups.player.size() >= 1 && autoPause) {
        		state.serverPaused = false;
        		Log.info("auto-pause: " + Groups.player.size() + " player connected -> Game unpaused...");
        		Call.sendMessage("auto-pause: " + Groups.player.size() + " player connected -> Game unpaused...");
        	}
        });

        //pause the game if no one is connected. Remove the rainbow and spectate mode of this player.
        Events.on(PlayerLeave.class, e -> {
        	if (Groups.player.size()-1 < 1 && autoPause) {
        		state.serverPaused = true;
        		Log.info("auto-pause: " + (Groups.player.size()-1) + " player connected -> Game paused...");
        		Call.sendMessage("auto-pause: " + (Groups.player.size()-1) + " player connected -> Game paused...");
        	}

        	if(rainbowedPlayers.contains(e.player.uuid())) rainbowedPlayers.remove(e.player.uuid());
        	
        	if(rememberSpectate.containsKey(e.player)) rememberSpectate.remove(e.player);
        });

        //recreate the tchat for the command /tchat
        Events.on(PlayerChatEvent.class, e -> {
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
        
        //for creative player
        Events.on(BlockBuildBeginEvent.class, (e) -> {
        	Player player = e.unit.getPlayer();
        	
        	if (player != null) {
        		player.sendMessage(e.tile.block().name+"");
        		
        		if (creativePlayers.containsKey(player)) {
        			if (e.breaking) Call.deconstructFinish(e.tile, e.tile.block(), e.unit);
        			else Call.constructFinish(e.tile, e.tile.block(), e.unit, (byte)0, e.team, false);
        		}
        	}
        	
        });
    }
    

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
    	handler.register("unban-all", "[y|n]", "Unban all IP and ID", arg -> {
    		if (arg.length == 1 && !confirm) {
    			Log.err("Use first: 'unban-all', before confirming the command.");
    			return;
    		} else if (!confirm) {
    			Log.warn("Are you sure to unban all all IP and ID ? (unban-all [y|n])");
    			confirm = true;
    			return;
    		} else if (arg.length == 0 && confirm) {
    			Log.warn("Are you sure to unban all all IP and ID ? (unban-all [y|n])");
    			confirm = true;
    			return;
    		}

    		switch (arg[0]) {
    			case "y":
    				netServer.admins.getBanned().each(unban -> netServer.admins.unbanPlayerID(unban.id));
    				netServer.admins.getBannedIPs().each(ip -> netServer.admins.unbanPlayerIP(ip));
    				Log.info("All all IP and ID have been unbanned!");
    				confirm = false;
    				break;
    			case "n":
    				Log.info("You canceled the confirmation...");
    				confirm = false;
    				break;
    			default: 
    				Log.err("Invalid arguments!");
    				confirm = false;
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
/*        
        handler.register("strict-nicknames", "<on|off>", "Automatically replace all spaces in nicknames with '_'", arg -> {
        	if(state.is(State.playing)){
                Log.err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }
        	
        	switch (arg[0]) {
    			case "on":
    				if (strictName) {
    					Log.err("Disabled first!");
    					return;
    				}
    				strictName = true;
    				Log.info("Strict names is enabled ... All spaces in nicknames will be replaced by '_'");
    				break;
    		
    			case "off":
    				if (!strictName) {
    					Log.err("Enabled first!");
    					return;
    				}
    				strictName = false;
    				Log.info("Strict names is disabled ... Nicknames are no longer subject to the rule");
    				break;
    		
    			default: Log.err("Invalid arguments. \n - Strict names is currently @.", strictName ? "enabled" : "disabled");
    		}
        });
*/
    }
    
    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
    	handler.removeCommand("help");
    	handler.<Player>register("help", "[page]", "Lists all commands", (arg, player) -> {
        	if(arg.length > 0 && !Strings.canParseInt(arg[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }
        	
        	int adminCommands = player.admin ? 0 : 12;
        	int commandsPerPage = 8;
            int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
            int pages = Mathf.ceil(((float)handler.getCommandList().size - adminCommands) / commandsPerPage);
            
            page --;

            if(page >= pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Commands Page[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page + 1), pages));

            for(int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), handler.getCommandList().size - adminCommands); i++){
                Command command = handler.getCommandList().get(i);
                result.append("[orange] /").append(command.text).append("[white] ").append(command.paramText).append("[lightgray] - ").append(command.description).append("\n");
            }
            player.sendMessage(result.toString());
        });
    	
    	handler.<Player>register("*","If you see an order with '*' in its description, it means you can replace all spaces with '_' in the name.", (args, player) -> {
            player.sendMessage("If you see an order with '*' in its description, it means you can replace all spaces with '_' in the name.");
         });
         
    	
        handler.<Player>register("ut","unit type", (args, player) -> {
           player.sendMessage("You're a [sky]" + player.unit().type().name + "[].");
        });
        
        handler.<Player>register("dm", "<ID|username> <message...>","Send a message to a player *", (args, player) -> {
        	Player target = Groups.player.find(p -> p.name().equalsIgnoreCase(args[0]) || p.uuid().equalsIgnoreCase(args[0]));
        	if (target == null) target = Groups.player.find(p -> p.name().equalsIgnoreCase(args[0].replaceAll("_", " ")));
        	
            if(target == null) err(player, "Player not connected or doesn't exist!");
            else {
            	 player.sendMessage("\n[gold]Private Message: [sky]you[gold] --> [white]" + target.name + "[gold]\n--------------------------------\n[white]" + args[1]);
            	 target.sendMessage("\n[gold]Private Message: [white]" + player.name + "[gold] --> [sky]you[gold]\n--------------------------------\n[white]" + args[1]);
            }
         });

        handler.<Player>register("maps", "[page]", "List all maps on server", (arg, player) -> {
        	int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
			int lines = 8;
            Seq<Map> list = maps.all();
            int pages = Mathf.ceil(list.size / lines);
            if (list.size % lines != 0) pages++;
            int index=(page-1)*lines;
            
            if (page > pages || page < 1) {
            	player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and [orange]" + pages + "[].");
            	return;
            }
            
            player.sendMessage("\n[orange]---- [gold]Maps list [lightgray]" + page + "[gray]/[lightgray]" + pages + "[orange] ----");
            for (int i=0; i<lines;i++) {
            	try {
            		player.sendMessage("[orange]  - []" + list.get(index).name() + "[orange] | [white]" + list.get(index).width + "x" + list.get(index).height);
            		index++;
            	} catch (IndexOutOfBoundsException e) {
            		break;
            	}
            }
            player.sendMessage("[orange]-----------------------");
        });
        
        handler.<Player>register("vnw", "(VoteNewWave) Vote for Sending a new Wave", (args, player) -> {
        	if (votesVNW.contains(player.uuid()) || votesVNW.contains(netServer.admins.getInfo(player.uuid()).lastIP)) {
                player.sendMessage("You have Voted already.");
                return;
        	}

            votesVNW.add(player.uuid());
            int cur = votesVNW.size();
            int req = (int) Math.ceil(ratio * Groups.player.size());
            Call.sendMessage("[scarlet]VNW: [orange]" + NetClient.colorizeName(player.id, player.name) + "[white] has voted for a new wave, [green]" + cur + "[white] votes, [green]" + req + "[white] required.");
 
            if (cur < req) return;

            votesVNW.clear();
            Call.sendMessage("[scarlet]VNW: [green]Vote passed. New Wave will be Spawned!");
            state.wavetime = 0f;
            try { 
            	task.cancel();
            } catch (Exception e) {}
		});
        
        handler.<Player>register("rtv", "Rock the vote to change map", (arg, player) -> {
        	if (votesRTV.contains(player.uuid()) || votesRTV.contains(netServer.admins.getInfo(player.uuid()).lastIP)) {
                player.sendMessage("You have Voted already.");
                return;
        	}
        	
        	votesRTV.add(player.uuid());
            int cur2 = votesRTV.size();
            int req2 =  (int) Math.ceil(ratio * Groups.player.size());
            Call.sendMessage("[scarlet]RTV: [accent]" + NetClient.colorizeName(player.id, player.name) + " [white]wants to change the map, [green]" + cur2 + "[white] votes, [green]" + req2 + "[white] required.");
            
            if (cur2 < req2) return;
            
            votesRTV.clear();
            Call.sendMessage("[scarlet]RTV: [green]Vote passed, changing map...");
            Events.fire(new GameOverEvent(Team.crux));
        });

        handler.<Player>register("info-all", "[ID|username...]", "Get all player information", (arg, player) -> {
        	StringBuilder builder = new StringBuilder();
        	ObjectSet<PlayerInfo> infos;
        	PlayerInfo pI = netServer.admins.findByIP(player.ip());
        	boolean type = false;
        	
        	if(arg.length >= 1) {
        		if (player.admin()) {
            		infos = netServer.admins.findByName(arg[0]);
            		type = true;
            	} else { 
            		err(player, "You don't have the permission to use arguments!");
            		return;
            	}
        	} else infos = netServer.admins.findByName(player.name);
			if (infos.size == 0 && arg.length >= 1) pI = netServer.admins.getInfo(arg[0]);
			
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
                			"\n[white] - Times joined: [green]" + infos.get(pI).timesJoined);
                	if (player.admin()) builder.append("\n[white] - Times kicked: [scarlet]" + infos.get(pI).timesKicked + 
                								"\n[white] - Is baned: [accent]" + infos.get(pI).banned +
                								"\n[white] - Is admin: [accent]" + infos.get(pI).admin);
                	builder.append("\n[gold]----------------------------------------");
                	
                	if (!type) Call.infoMessage(player.con, builder.toString());
                	else player.sendMessage(builder.toString());
                	builder = new StringBuilder();
                }
           } else err(player, "This player doesn't exist!");
        });
        
        handler.<Player>register("rainbow", "[ID]", "[#ff0000]R[#ff7f00]A[#ffff00]I[#00ff00]N[#0000ff]B[#2e2b5f]O[#8B00ff]W[#ff0000]![#ff7f00]!", (arg, player) -> {
        	if (arg.length == 0) {
        		if(rainbowedPlayers.contains(player.uuid())) {
        			player.sendMessage("[sky]Rainbow effect toggled off.");
        			rainbowedPlayers.remove(player.uuid());
        			player.name = TempPlayerData.tempPlayerDatas.get(player.uuid()).realName;
        		} else {
        			player.sendMessage("[sky]Rainbow effect toggled on.");
        			rainbowedPlayers.add(player.uuid());
        			TempPlayerData pData = TempPlayerData.tempPlayerDatas.get(player.uuid());
               
        			Thread rainbowLoop = new Thread() {
        				public void run() {
        					while(rainbowedPlayers.contains(player.uuid())) {
        						try {
        							Integer hue = pData.hue;
                                    if (hue < 360) hue++;
                                    else hue = 0;
                                   
                                    String hex = "[#" + Integer.toHexString(Color.getHSBColor(hue / 360f, 1f, 1f).getRGB()).substring(2) + "]";
                                    
                                    
                                    player.name = hex + pData.nameNotColor;
                                    pData.setHue(hue);
                                    TempPlayerData.tempPlayerDatas.replace(player.uuid(), pData);
//##################################################################################################################
                                    Thread.sleep(50);
        						} catch (InterruptedException e) {
                            	    e.printStackTrace();
                                }
        					}
        				}
        			};
        			rainbowLoop.start();
        		}
        	} else {
        		if (player.admin) {
        			warn(player, "This will remove the rainbow from the person matching the argument.");
        			
        			Player rPlayer = Groups.player.find(p -> p.uuid().equalsIgnoreCase(arg[0]));
        			
        			if(rPlayer != null && rainbowedPlayers.contains(arg[0])) {
            			rainbowedPlayers.remove(arg[0]);
            			rPlayer.name = TempPlayerData.tempPlayerDatas.get(arg[0]).realName;
            			player.sendMessage("[sky]Rainbow effect toggled off for the player [accent]" + rPlayer.name + "[].");
        			} else err(player, "This player not have the rainbow or doesn't exist.");
        			
        		} else err(player, "You don't have the permission to use arguments!");
        	}
        	
        });
       
        handler.<Player>register("team", "[teamname|list|vanish]","change team", (args, player) ->{
            if(!player.admin()){
                player.sendMessage("[scarlet]Only admins can change team !");
                return;
            }
            StringBuilder builder = new StringBuilder();
            
            if(rememberSpectate.containsKey(player)){
                player.sendMessage(">[orange] transferring back to last team");
                player.team(rememberSpectate.get(player));
                Call.setPlayerTeamEditor(player, rememberSpectate.get(player));
                rememberSpectate.remove(player);
                player.name = TempPlayerData.tempPlayerDatas.get(player.uuid()).realName;
                return;
            }
            coreTeamReturn ret = null;
            if(args.length == 1){
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
                    	rememberSpectate.put(player, player.unit().team);
                        player.team(Team.all[8]);
                        Call.setPlayerTeamEditor(player, Team.all[8]);
                        player.unit().kill();
                        player.sendMessage("[green]VANISH MODE[] \nuse /team to go back to player mode.");
                        player.name = "";
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
                    	err(player, "Invalid arguments");
                    	return;
                    
                }
                if(retTeam.cores().isEmpty()) {
                	warn(player,"This team has no core!");
                	player.team(retTeam);
                	player.sendMessage("> You changed to team [sky]" + retTeam);
                	return;
                }
                
                ret =  new coreTeamReturn(retTeam);
            } else ret = getPosTeamLoc(player);

            //move team mechanic
            if(ret != null) {
                Call.setPlayerTeamEditor(player, ret.team);
                player.team(ret.team);
                player.sendMessage("> You changed to team [sky]" + ret.team);
            } else err(player, "Other team has no core, can't change!");
        });

        handler.<Player>register("am", "<message...>", "Send a message as admin", (arg, player) -> {
        	if (!adminCheck(player)) return;
        	Call.sendMessage(arg[0], "[scarlet]<Admin>[]" + NetClient.colorizeName(player.id, player.name), player);
        });
        
        handler.<Player>register("players", "<all|online|ban>", "Gives the list of players according to the type of filter given", (arg, player) -> {
        	if (!adminCheck(player)) return;
        	
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
            			Player online = Groups.player.find(pl -> pl.uuid().equalsIgnoreCase(p.id));
            			if (online != null) builder.append("[white] | [green]Online");
            			builder.append("\n");
            		}
            		break;
            	
            	default: err(player, "Invalid usage:[lightgray] Invalid arguments.");
            }
            
            if (size > 50) Call.infoMessage(player.con, builder.toString());
            else player.sendMessage(builder.toString());
        });

        handler.<Player>register("kill", "[username...]", "Kill a player", (arg, player) -> {
        	if (!adminCheck(player)) return;
            
        	if (arg.length == 0) player.unit().kill();
            else { 
                Player other = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[0]));
                if (other != null) other.unit().kill();
                else player.sendMessage("[scarlet]This player doesn't exist or not connected!");
            }
        });
        

        handler.<Player>register("core", "<small|meduim|big>", "Spawn a core to your corrdinate", (arg, player) -> {
        	if (!adminCheck(player)) return;
        	if(rememberSpectate.containsKey(player)) {
        		err(player, "You can't build a core in vanish mode!");
        		return;
        	}
        	warn(player, "This will destroy all the blocks which will be hampered by the construction of the core.");
        	
        	Block core;
        	if (arg[0].equals("small")) core = Blocks.coreShard;
        	else if (arg[0].equals("medium")) core = Blocks.coreFoundation; 
        	else if (arg[0].equals("big")) core = Blocks.coreNucleus;
        	else {
        		err(player, "Core type not found:[] Please use arguments small, medium, big.");
                return;
        	}

            Call.constructFinish(player.tileOn(), core, player.unit(), (byte)0, player.team(), false);
            player.sendMessage(player.tileOn().block() == core ? "[green]Core build." : "[scarlet]Error: Core not build.");
        });
        
        handler.<Player>register("tp", "<name|x,y> [to_name|x,y]", "Teleport to position or player *", (arg, player) -> {
        	if (!adminCheck(player)) return;
        	
        	int x, y;
        	String[] __temp__;
            Player name = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[0])), toName;
            NetConnection playerCon;
            
            
            if (name == null) name = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[0].replaceAll("_", " ")));
			if (name == null) {
				try {
        			__temp__ = arg[0].split(",");
        			x = Integer.parseInt(__temp__[0]);
        			y = Integer.parseInt(__temp__[1]);
        		
        			if (__temp__.length > 2) {
        				err(player, "Wrong coordinates!");
        				return;
        			}
        		} catch (NumberFormatException e) {
        			err(player, "Player [orange]" + arg[0] + "[] doesn't exist or not connected!");
    				return;
        		} catch (ArrayIndexOutOfBoundsException e) {
        			err(player, "Wrong coordinates!");
        			return;
        		}
				
				if (arg.length == 2) {
					player.sendMessage("[scarlet]Cannot teleport Coordinates to a Coordinates or Coordinates to a player! [lightgray]It's not logic XD.");
            		return;
				} else playerCon = player.con;
				
            } else {
            	playerCon = name.con;
            	x = Math.round(name.x/8);
				y = Math.round(name.y/8);
            }
            
			if (arg.length == 2) {
            	toName = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[1]));
            	
            	if (toName == null) Groups.player.find(p -> p.name().equalsIgnoreCase(arg[1].replaceAll("_", " ")));
            	if (toName == null) {
            		try {
            			__temp__ = arg[1].split(",");
            			x = Integer.parseInt(__temp__[0]);
            			y = Integer.parseInt(__temp__[1]);
            		
            			if (__temp__.length > 2) {
            				err(player, "Wrong coordinates!");
            				return;
            			}
            		} catch (NumberFormatException e) {
            			err(player, "Player [orange]" + arg[1] + "[] doesn't exist or not connected!");
        				return;
            		} catch (ArrayIndexOutOfBoundsException e) {
            			err(player, "Wrong coordinates!");
            			return;
            		}

            	} else {
            		x = Math.round(toName.x/8);
    				y = Math.round(toName.y/8);
                }
            } else toName = null;
            
			
            if (x > world.width() || x < 0 || y > world.height() || y < 0) {
            	player.sendMessage("[scarlet]Coordinates too large. Max: [orange]" + world.width() + "[]x[orange]" + world.height() + "[]. Min : [orange]0[]x[orange]0[].");
            	return;
            }

            playerCon.player.unit().set(x*8, y*8);
            Call.setPosition(playerCon, x*8, y*8);
            playerCon.player.snapSync();
            
            if (arg.length == 2) player.sendMessage("You teleported [accent]" + playerCon.player.name + "[] to [accent]" + x + "[]x[accent]" + y + "[].");
            else player.sendMessage("You teleported to [accent]" + x + "[]x[accent]" + y + "[].");
            
            try { Thread.sleep(500);
			} catch (InterruptedException e) { e.printStackTrace(); }
        });  
        
        handler.<Player>register("spawn", "<unit> [x,y|username] [teamname] [count]", "Spawn a unit (you can use '~' for your local team or coordinates). *", (arg, player) -> {
        	if (!adminCheck(player)) return;
        	
        	StringBuilder builder = new StringBuilder();
        	UnitType unit = content.units().find(b -> b.name.equals(arg[0]));
        	Player target;
        	Team team;
        	int count, x, y;
        	
        	if (arg.length >= 2) {
        		if (arg[1].equals("~")) target = player;
        		else target = Groups.player.find(p -> p.name().equals(arg[1]));
        		if (target == null) Groups.player.find(p -> p.name().equals(arg[1].replaceAll("_", " ")));
        		
        		if (target == null) {
        			target = player;
        			try {
        				String __temp__[] = arg[1].split(",");
        				x = Integer.parseInt(__temp__[0])*8;
        				y = Integer.parseInt(__temp__[1])*8;
            		
        				if (__temp__.length > 2) {
        					err(player, "Wrong coordinates!");
        					return;
        				}
        			} catch (NumberFormatException e) {
                		err(player, "Player doesn't exist or wrong coordinates!");
                		return;
                	} catch (ArrayIndexOutOfBoundsException e) {
                		err(player, "Wrong coordinates!");
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
            			err(player, "available teams: ");
            			for (Team teamList : Team.baseTeams) builder.append(" - [accent]" + teamList.name + "[]\n");
            			player.sendMessage(builder.toString());
            			return;
        		}
        	} else team = player.team();
        	
            if (arg.length == 4) {
            	try {
            		count = Integer.parseInt(arg[3]);
            	} catch (NumberFormatException e) {
            		err(player, "Count must be number!");
            		return;
            	}
            } else count = 1;
            
            if (unit != null) {
                for (int i=0; i<count; i++) unit.spawn(team, x, y);
                
                player.sendMessage("[green]You are spawning [accent]" + count + " " + unit + " []for [accent]" + team + " []team at [orange]" + x/8 + "[white],[orange]" + y/8);
            } else {
            	builder.append("[scarlet]Units:[]");
            	for (UnitType unitName : content.units()) builder.append(" [accent]" + unitName.name + "[],");          
                player.sendMessage(builder.toString());
            }
        });
        
        handler.<Player>register("creative", "[username|ID...]", "Instantly build/destroy the desired structures.", (arg, player) -> {
        	if (!adminCheck(player)) return;
        	
        	Player target;
        	if (arg.length == 0) target = player;
        	else {
        		target = Groups.player.find(p -> p.name().equals(arg[0]));
        		if (target == null) target = Groups.player.find(p -> p.uuid().equals(arg[0]));
        	}
        	
        	if (target != null) {
        		if (creativePlayers.containsKey(target)) {
        			target.unit().health = creativePlayers.get(target);
        			creativePlayers.remove(target);
        		} else {
        			creativePlayers.put(target, (int) player.unit().health);
        			target.unit().health = Integer.MAX_VALUE;
        		}
        		
        	} else {
        		player.sendMessage("[scarlet]This player doesn't exist or not connected!");
        		return;
        	}
        		
        	if (arg.length == 0) player.sendMessage("[green]Creative mode is " + (creativePlayers.containsKey(target) ? "enabled" : "disabled"));
        	else {
        		player.sendMessage("[green]Creative mode is " + (creativePlayers.containsKey(target) ? "enabled" : "disabled") + (arg.length == 0 ? "" : " for [accent]" + target.name));
        		target.sendMessage("[green]" + (creativePlayers.containsKey(target) ? "You've been put into creative mode" : "You have been removed from creative mode") + " by [accent]"+ player.name);
        	}
        	
        });
        
        handler.<Player>register("tchat", "<on|off>", "Enabled/disabled the tchat", (arg, player) -> {
        	if (!adminCheck(player)) return;
        	
        	switch (arg[0]) {
        		case "on":
        			if (tchat) {
        				err(player, "Disabled first!");
        				return;
        			}
        			tchat = true;
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is enabled! [lightgray](by " + player.name + "[lightgray]) \n[gold]--------------------\n");
        			Log.info("Tchat enabled by " + player.name + ".");
        			break;
        		case "off":
        			if (!tchat) {
        				err(player, "Enabled first!");
        				return;
        			}
        			tchat = false;
        			Call.sendMessage("\n[gold]-------------------- \n[scarlet]/!\\[orange] The tchat is disabled! [lightgray](by " + player.name + "[lightgray]) \n[gold]--------------------\n");
        			Log.info("Tchat disabled by " + player.name + ".");
        			break;
        		default: err(player, "Invalid arguments.[] \n - The tchat is currently [accent]%s[].", tchat ? "enabled" : "disabled");
        	}
        });   
        
        handler.<Player>register("kick", "<ID|username...>", "Kick a person by name or ID", (arg, player) -> {
            if (!adminCheck(player)) return;

            Player target = Groups.player.find(p -> p.name().equals(arg[0]));
            if (target == null) target = Groups.player.find(p -> p.uuid().equals(arg[0]));
            
            if (target != null) {
                Call.sendMessage("[scarlet]/!\\[] " + target.name() + "[scarlet] has been kicked of the server.");
                target.kick(KickReason.kick);
                info(player, "It is done.");
            } else info(player, "Nobody with that name or ID could be found...");
        });   
        
        handler.<Player>register("pardon", "<ID>", "Pardon a player by ID and allow them to join again", (arg, player) -> {
        	if (!adminCheck(player)) return;
        	
        	PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);
        	
        	if (info != null) {
        		info.lastKicked = 0;
        		info(player, "Pardoned player: [accent]%s", info.lastName);
        	} else err(player, "That ID can't be found.");
        });

        handler.<Player>register("ban", "<id|name|ip> <username|IP|ID...>", "Ban a person", (arg, player) -> {
        	if (!adminCheck(player)) return;

            if (arg[0].equals("id")) {
                netServer.admins.banPlayerID(arg[1]);
                info(player, "Banned.");
            } else if (arg[0].equals("name")) {
                Player target = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[1]));
                if (target != null) {
                    netServer.admins.banPlayer(target.uuid());
                    info(player, "Banned.");
                } else err(player, "No matches found.");
            } else if (arg[0].equals("ip")) {
                netServer.admins.banPlayerIP(arg[1]);
                info(player, "Banned.");
            } else err(player, "Invalid type.");

            
            for (Player gPlayer : Groups.player) {
                if (netServer.admins.isIDBanned(gPlayer.uuid())) {
                    Call.sendMessage("[scarlet]/!\\[] " + gPlayer.name + "[scarlet] has been banned of the server.");
                    gPlayer.con.kick(KickReason.banned);
                }
            }
        });
        
        handler.<Player>register("unban", "<ip|ID>", "Unban a person", (arg, player) -> {
        	if (!adminCheck(player)) return;

            if (netServer.admins.unbanPlayerIP(arg[0]) || netServer.admins.unbanPlayerID(arg[0])) info(player, "Unbanned player: [accent]%s", arg[0]);
            else err(player, "That IP/ID is not banned!");
        });
        
    }

    private void err(Player player, String fmt, Object... msg) { PlayerFunctions.err(player, fmt, msg); }
    private void info(Player player, String fmt, Object... msg) { PlayerFunctions.info(player, fmt, msg); }
    private void warn(Player player, String fmt, Object... msg) { PlayerFunctions.warn(player, fmt, msg); }
    private boolean adminCheck(Player player) { return PlayerFunctions.adminCheck(player); }
    
    //search a possible team
    private Team getPosTeam(Player p){
        Team currentTeam = p.team();
        int c_index = Arrays.asList(Team.baseTeams).indexOf(currentTeam);
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