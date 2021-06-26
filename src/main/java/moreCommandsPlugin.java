import static mindustry.Vars.content;
import static mindustry.Vars.maps;
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

import functions.TempData;
import functions.Players;
import functions.Effects;

import mindustry.content.Blocks;
import mindustry.content.Fx;
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
import mindustry.net.Administration.Config;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;
import mindustry.type.UnitType;


public class moreCommandsPlugin extends Plugin {
	arc.util.Timer.Task task;
    private double ratio = 0.6;
    private ArrayList<String> votesVNW = new ArrayList<>(), votesRTV = new ArrayList<>(), rainbowedPlayers = new ArrayList<>(), effects = new ArrayList<>();
    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();
    private static ObjectMap<Player, Integer> godmodPlayers = new ObjectMap<>();
    private boolean confirm = false, autoPause = false, tchat = true, niceWelcome = true;
    
    public void init() { netServer.admins.addChatFilter((player, message) -> null); } //delete the tchat
	public moreCommandsPlugin() {
		Effects.init();
		
    	Events.on(PlayerJoin.class, e -> TempData.put(e.player)); // add player in TempPlayerData
		Events.on(PlayerLeave.class, e -> TempData.remove(e.player)); // remove player in TempPlayerData

    	//clear VNW & RTV votes on game over
        Events.on(GameOverEvent.class, e -> {
        	votesVNW.clear();
            votesRTV.clear();
        });
        
        Events.on(PlayerConnect.class, e -> {
        	//kick the player if there is [Server], [server], or @a in his nickname
        	nameCheck(e.player, new String[]{"[Server]", "[server]", "@a", "@p", "~"});
        	
        	//prevent to duplicate nicknames
        	for (Player p : Groups.player) {
        		if (Strings.stripColors(p.name).equals(Strings.stripColors(e.player.name))) e.player.kick(KickReason.nameInUse);
        	}
        	
        	//check if the nickname is empty without the colors
        	if (Strings.stripColors(e.player.name).equals("")) e.player.kick(KickReason.nameEmpty);
        });

        
        Events.on(PlayerJoin.class, e -> {
        	//for me =)
        	if (e.player.uuid().equals("k6uyrb9D3dEAAAAArLs28w==") && niceWelcome) Call.sendMessage("[scarlet]So cool![] " + NetClient.colorizeName(e.player.id, e.player.name) + "[scarlet] has connected!");
        	
        	//unpause the game if one player is connected
        	if (Groups.player.size() == 1 && autoPause) {
        		state.serverPaused = false;
        		Log.info("auto-pause: " + Groups.player.size() + " player connected -> Game unpaused...");
        		Call.sendMessage("[scarlet][Server][]: Game unpaused...");
        	}
        });
        
        Events.on(PlayerLeave.class, e -> {
        	//pause the game if no one is connected
        	if (Groups.player.size()-1 < 1 && autoPause) {
        		state.serverPaused = true;
        		Log.info("auto-pause: " + (Groups.player.size()-1) + " player connected -> Game paused...");
        	}

        	//remove the rainbow, spectate, god mode of this player
        	if(rainbowedPlayers.contains(e.player.uuid())) rainbowedPlayers.remove(e.player.uuid());
        	if(rememberSpectate.containsKey(e.player)) rememberSpectate.remove(e.player);
        	if(godmodPlayers.containsKey(e.player)) godmodPlayers.remove(e.player);
        	
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
        
        /*
         * WARNING: Possibility of crashing the server!
         */
        //for players in god mode 
        Events.on(BlockBuildBeginEvent.class, (e) -> {
        	Player player = e.unit.getPlayer();
        	
        	if (player != null) {
        		if (godmodPlayers.containsKey(player)) {
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
        
        handler.register("nice-welcome", "Nice welcome for me", arg -> {
        	niceWelcome = niceWelcome ? false : true;
        	Log.info(niceWelcome ? "Enabled" : "Disabled");
        });
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
        	
        	int adminCommands = player.admin ? 0 : 13;
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
            	arc.util.CommandHandler.Command command = handler.getCommandList().get(i);
                result.append("[orange] /").append(command.text).append("[white] ").append(command.paramText).append("[lightgray] - ").append(command.description).append("\n");
            }
            player.sendMessage(result.toString());
        });
    	
    	handler.<Player>register("*","If you see an order with '*' in its description, it means you can replace all spaces with '_' in the name.", (arg, player) -> {
            player.sendMessage("If you see an order with '*' in its description, it means you can replace all spaces with '_' in the name.");
         });
         
    	
        handler.<Player>register("ut","unit type", (args, player) -> {
        	try { player.sendMessage("You're a [sky]" + player.unit().type().name + "[]."); }
        	catch (NullPointerException e) {}
        });
        
        handler.<Player>register("msg", "<ID|username> <message...>","Send a message to a player *", (arg, player) -> {
        	Player target = Players.find(arg[0]);
        	
            if(target == null) Players.err(player, "Player not connected or doesn't exist!");
            else {
            	 player.sendMessage("\n[gold]Private Message: [sky]you[gold] --> [white]" + target.name + "[gold]\n--------------------------------\n[white]" + arg[1]);
            	 target.sendMessage("\n[gold]Private Message: [white]" + player.name + "[gold] --> [sky]you[gold]\n--------------------------------\n[white]" + arg[1]);
            }
         });

        handler.<Player>register("maps", "[page]", "List all maps on server", (arg, player) -> {
        	if(arg.length > 0 && !Strings.canParseInt(arg[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }
        	
        	int page = arg.length > 0 ? Strings.parseInt(arg[0]) : 1;
			int lines = 8;
			Seq<mindustry.maps.Map> list = maps.all();
            int pages = Mathf.ceil(list.size / lines);
            if (list.size % lines != 0) pages++;
            int index=(page-1)*lines;
            Map map;
            
            if (page > pages || page < 1) {
            	player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and [orange]" + pages + "[].");
            	return;
            }
            
            player.sendMessage("\n[orange]---- [gold]Maps list [lightgray]" + page + "[gray]/[lightgray]" + pages + "[orange] ----");
            for (int i=0; i<lines;i++) {
            	try {
            		map = list.get(index) ;
            		player.sendMessage("[orange]  - []" + map.name() + "[orange] | [white]" + map.width + "x" + map.height);
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
        	arc.struct.ObjectSet<PlayerInfo> infos;
        	PlayerInfo pI = netServer.admins.findByIP(player.ip());
        	boolean type = false;
        	
        	if(arg.length >= 1) {
        		if (player.admin) {
            		infos = netServer.admins.findByName(arg[0]);
            		type = true;
            	} else { 
            		Players.err(player, "You don't have the permission to use arguments!");
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
        			TempData pData = TempData.get(player);
               
        			Thread rainbowLoop = new Thread() {
        				public void run() {
        					while(rainbowedPlayers.contains(player.uuid())) {
        						try {
        							int hue = pData.hue;
                                    if (hue < 360) hue+=5;
                                    else hue = 0;
                                    
                                    for (int i=0; i<5; i++) Call.effectReliable(Fx.bubble, player.x, player.y, 10, arc.graphics.Color.valueOf(Integer.toHexString(Color.getHSBColor(hue / 360f, 1f, 1f).getRGB()).substring(2)));
                                    player.name = putColor(pData.normalizedName, hue);
                                    pData.setHue(hue);
                                    
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

        		int page = arg.length == 2 ? Strings.parseInt(arg[1]) : 1;
    			int lines = 12;
    			Seq<Effects> list = Effects.copy();
                int pages = Mathf.ceil(list.size / lines);
                if (list.size % lines != 0) pages++;
                int index = (page-1)*lines;
                Effects e;

                if(page > pages || page < 0){
                    player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                    return;
                }

                player.sendMessage("\n[orange]---- [gold]Effects list [lightgray]" + page + "[gray]/[lightgray]" + pages + "[orange] ----");
                for(int i=0; i<lines;i++){
                	try {
                		e = list.get(index) ;
                		builder.append("  [orange]- [lightgray]ID:[white] " + e.id + "[orange] | [lightgray]Name:[white] " + e.name + "\n");
                		index++;
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
            			
        		if(Strings.canParseInt(arg[0])) effect = Effects.getByID(Strings.parseInt(arg[0])-1);
        		else effect = Effects.getByName(arg[0]);
        		
        		if (effect == null) {
        			Players.err(player, "Particle effect don't exist");
        			return;
        		} else player.sendMessage("[green]Start particles effect [accent]" + effect.id + "[scarlet] - []" + effect.name);
        	}

        	Thread effectsLoop = new Thread() {
        		public void run() {
        			while(effects.contains(player.uuid())) {
        				try { 
        					Call.effectReliable(effect.effect, player.x, player.y, 5, arc.graphics.Color.green);
        					Thread.sleep(50); 
        				} catch (InterruptedException e) { e.printStackTrace(); }
        			}
        		}
        	};
        	effectsLoop.start();
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
            			Player online = Groups.player.find(pl -> pl.uuid().equalsIgnoreCase(p.id));
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
        	if (arg[0].equals("small")) core = Blocks.coreShard;
        	else if (arg[0].equals("medium")) core = Blocks.coreFoundation; 
        	else if (arg[0].equals("big")) core = Blocks.coreNucleus;
        	else {
        		Players.err(player, "Core type not found:[] Please use arguments small, medium, big.");
                return;
        	}

            Call.constructFinish(player.tileOn(), core, player.unit(), (byte)0, player.team(), false);
            player.sendMessage(player.tileOn().block() == core ? "[green]Core build." : "[scarlet]Error: Core not build.");
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
            
            if ((boolean) Config.valueOf("strict").get()) {
            	Config.valueOf("strict").set(false);
            	Core.settings.forceSave();
            	target.unit().set(co[0]*8, co[1]*8);
            	Call.setPosition(target.con, co[0]*8, co[1]*8);
            	Config.valueOf("strict").set(true);
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
        	} else team = player.team();
       
            if (arg.length == 4) {
            	if (!Strings.canParseInt(arg[3])) {
            		Players.err(player, "'count' must be number!");
            		return;
            	} else count = Strings.parseInt(arg[3]);

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
        
        handler.<Player>register("godmode", "[username|ID...]", "[scarlet][God][]: [gold]I'm divine!", (arg, player) -> {
        	if (!Players.adminCheck(player)) return;
        	
        	Player target;
        	if (arg.length == 0) target = player;
        	else target = Players.find(arg[0]);
        	
        	if (target != null) {
        		if (godmodPlayers.containsKey(target)) {
        			target.unit().health = godmodPlayers.get(target);
        			godmodPlayers.remove(target);
        		} else {
        			godmodPlayers.put(target, (int) player.unit().health);
        			target.unit().health = Integer.MAX_VALUE;
        		}
        		
        	} else {
        		player.sendMessage("[scarlet]This player doesn't exist or not connected!");
        		return;
        	}
        		
        	if (arg.length == 0) player.sendMessage("[gold]God mode is [green]" + (godmodPlayers.containsKey(target) ? "enabled" : "disabled"));
        	else {
        		player.sendMessage("[gold]God mode is [green]" + (godmodPlayers.containsKey(target) ? "enabled" : "disabled") + (arg.length == 0 ? "" : " for [accent]" + target.name));
        		target.sendMessage("[green]" + (godmodPlayers.containsKey(target) ? "You've been put into god mode" : "You have been removed from creative mode") + " by [accent]"+ player.name);
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