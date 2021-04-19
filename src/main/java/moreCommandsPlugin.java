import static mindustry.Vars.netServer;
import static mindustry.Vars.state;
import static mindustry.Vars.maps;

import java.util.Arrays;
import java.util.HashSet;

import arc.Events;
import arc.func.Boolf;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.core.NetServer;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;
import mindustry.world.Tile;

public class moreCommandsPlugin extends Plugin {
	Timer.Task task;
    private HashSet<String> votes = new HashSet<>();
    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();
    private static boolean confirm = false;

    
    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){
    	handler.register("unban-all", "[y/n]", "Unban all IP and ID", arg -> {
    		if (arg.length == 1 && confirm == false) {
    			Log.err("Use first: 'unban-all', before confirming the command.");
    			return;
    		} else if (confirm == false) {
    			Log.info("Are you sure to unban all all IP and ID ? (unban-all [y/n])");
    			confirm = true;
    			return;
    		} else if (arg.length == 0 && confirm == true) {
    			Log.info("Are you sure to unban all all IP and ID ? (unban-all [y/n])");
    			confirm = true;
    			return;
    		}
    		
    		switch (arg[0]) {
    			case "y":
    				Administration pBanned = netServer.admins;
    				pBanned.getBanned().each(unban -> pBanned.unbanPlayerID(unban.id));
    				pBanned.getBannedIPs().each(ip -> pBanned.unbanPlayerIP(String.valueOf(ip)));

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
    }
    
    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("ut","unit type", (args, player) ->{
           player.sendMessage(player.unit().type().name);
        });
        
        handler.<Player>register("vnw", "(VoteNewWave) Vote for Sending a new Wave", (args, player) -> {
        	double ratio = 0.6;
        	
        	if (this.votes.contains(player.uuid()) || this.votes.contains(netServer.admins.getInfo(player.uuid()).lastIP)) {
                player.sendMessage("You have Voted already.");
                return;
        	}

            this.votes.add(player.uuid());
            int cur = this.votes.size();
            int req = (int) Math.ceil(ratio * Groups.player.size());
            Call.sendMessage("[orange]" + NetClient.colorizeName(player.id, player.name) + "[][lightgray] has voted for a new wave,[green]" + cur + "[] votes, [green]" + req + "[] required");
 
            if (cur < req) return;

            this.votes.clear();
            Call.sendMessage("[green]Vote for Sending a New Wave is Passed. New Wave will be Spawned.");
            state.wavetime = 0f;
            task.cancel();
		});

        handler.<Player>register("maps", "[page]", "List all maps on server", (arg, player) -> {
            int page;
			if (!(arg.length == 0)) page = Strings.parseInt(arg[0]);
            else page = 1;

			int lines = 6;
            int index;
            Seq<Map> list = maps.all();
            int pages = Mathf.ceil(list.size / lines);
            if (list.size % lines != 0) pages++;
            index=(page-1)*lines;
            
            if (page > pages || page < 1) {
            	player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and [orange]" + pages + "[scarlet].");
            	return;
            }
            
            player.sendMessage("\n[orange]---- [][gold]Maps list [lightgray]" + page + "[white]/[lightgray]" + pages + "[orange] ----");
            for (int i=0; i<lines;i++) {
            	try {
            		player.sendMessage("[orange]  - []" + list.get(index).name() + "[][orange] | []" + list.get(index).width + "x" + list.get(index).height);
            		index++;
            	} catch (IndexOutOfBoundsException e) {
            		break;
            	}
            }
            player.sendMessage("[orange]-----------------------");
            
        });

        handler.<Player>register("info-all", "[username]", "Get all player information", (arg, player) -> {
        	StringBuilder builder = new StringBuilder();
        	ObjectSet<Administration.PlayerInfo> infos;
        	int type;
        	if (!player.admin()) {
            	 infos = netServer.admins.findByName(player.name);
            	 type = 0;
            } else {
            	 infos = netServer.admins.findByName(player.name);
            	 type = 0;
            	if(arg.length == 1) {
            		infos = netServer.admins.findByName(arg[0]);
            		type = 1;
            	}
            }
        	
            
            if (infos.size > 0) {
            	player.sendMessage("[gold]------------------------------------------");
            	if (!(type == 0)) {
                	player.sendMessage("[scarlet]-----"+
                		"\n[white]Players found: [gold]" + infos.size +
                		"\n[scarlet]-----");
                }
                int i = 1;
                for (Administration.PlayerInfo info : infos) {
                	if (type == 0) {
                		if (i > 1) break;
                		
                		player.sendMessage("Player name [accent]'" + info.lastName + "[accent]'[white] / UUID [accent]'" + info.id + "' ");
                	} else player.sendMessage("[gold][" + i + "] [white]Trace info for admin [accent]'" + info.lastName + "[accent]'[white] / UUID [accent]'" + info.id + "' ");
                	builder.append("[]- All names used: [accent]" + info.names +
                			"\n[]- IP: [accent]" + info.lastIP +
                			"\n[]- All IPs used: [accent]" + info.ips +
                			"\n[]- Is admin: [accent]" + info.admin +
                			"\n[]- Times joined: [green]" + info.timesJoined);
                	if (player.admin()) {
                		builder.append("\n[]- Times kicked: [scarlet]" + info.timesKicked +
                				"\n[]- Is baned: [accent]" + info.banned);
                	}
                	
                	builder.append("\n[][gold]------------------------------------------");
                	player.sendMessage(builder.toString());
                	builder = new StringBuilder();
                	i++;
                	
                }
           } else player.sendMessage("[accent]This player doesn't exist!");
        });
      
        handler.<Player>register("team", "[teamname]","change team", (args, player) ->{
            if(!player.admin()){
                player.sendMessage("[scarlet]Only admins can change team !");
                return;
            }
            
            if(rememberSpectate.containsKey(player)){
                player.sendMessage(">[orange] transferring back to last team");
                player.team(rememberSpectate.get(player));
                Call.setPlayerTeamEditor(player, rememberSpectate.get(player));
                rememberSpectate.remove(player);
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
                    default:
                        player.sendMessage("[scarlet]ABORT: Team not found[] - available teams:");
                        for (int i = 0; i < 6; i++) {
                            if (!Team.baseTeams[i].cores().isEmpty()) {
                                player.sendMessage(Team.baseTeams[i].name);
                            }
                        }
                        return;
                }
                if(retTeam.cores().isEmpty()){
                    player.sendMessage("This team has no core, can't change!");
                    return;
                }else{
                    Tile coreTile = retTeam.core().tileOn();
                    ret =  new coreTeamReturn(retTeam, coreTile.drawx(), coreTile.drawy());
                }
            }else ret = getPosTeamLoc(player);

            //move team mechanic
            if(ret != null) {
                Call.setPlayerTeamEditor(player, ret.team);
                player.team(ret.team);
                //maybe not needed
                Call.setPosition(player.con, ret.x, ret.y);
                player.unit().set(ret.x, ret.y);
                player.snapSync();
                Call.sendMessage(String.format("> [orange]%s []changed to team [sky]%s", player.name, ret.team));
            }else player.sendMessage("[scarlet]You can't change teams ...");
        });

        handler.<Player>register("spectate", "[scarlet]Admin only[]", (args, player) -> {
        	if (!adminVerif(player)) return;
        	
        	Team spectateTeam = Team.all[8];
            
            if(rememberSpectate.containsKey(player)){
                player.team(rememberSpectate.get(player));
                Call.setPlayerTeamEditor(player, rememberSpectate.get(player));
                rememberSpectate.remove(player);
                player.sendMessage("[gold]PLAYER MODE[]");
            }else{
                rememberSpectate.put(player, player.unit().team);
                player.team(spectateTeam);
                Call.setPlayerTeamEditor(player, spectateTeam);
                player.unit().kill();
                player.sendMessage("[green]SPECTATE MODE[]");
                player.sendMessage("use /team or /spectate to go back to player mode");
            }
        });
        
        handler.<Player>register("ac", "<message...>", "Admin Chat", (arg, player) -> {
        	if (!adminVerif(player)) return;
        	Groups.player.each(p -> p.admin, o -> o.sendMessage(arg[0], player, "[scarlet]<Admin>" + NetClient.colorizeName(player.id, player.name)));
        });
        
        handler.<Player>register("kick", "<username>", "Kick a person by name", (arg, player) -> {
            if (!adminVerif(player)) return;

            Player target = Groups.player.find(p -> p.name().equals(arg[0]));
            if (target != null) {
                Call.sendMessage("[scarlet]" + target.name() + "[scarlet] has been kicked by the server.");
                target.kick(KickReason.kick);
                info(player, "It is done.");
            } else info(player, "Nobody with that name could be found...");
        });
        
        handler.<Player>register("pardon", "<ID>", "Pardon a player by ID and allow them to join again", (arg, player) -> {
        	if (!adminVerif(player)) return;
        	
        	PlayerInfo info = netServer.admins.getInfoOptional(arg[0]);
        	
        	if (info != null) {
        		info.lastKicked = 0;
        		info(player, "Pardoned player: [accent]%s", info.lastName);
        	} else err(player, "That ID can't be found.");
        });
        
        handler.<Player>register("ban", "<type-id|name|ip> <username|IP|ID...>", "Ban a person", (arg, player) -> {
        	if (!adminVerif(player)) return;

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
                    Call.sendMessage("[scarlet]" + gPlayer.name + " has been banned.");
                    gPlayer.con.kick(KickReason.banned);
                }
            }
        });
        
        handler.<Player>register("unban", "<ip|ID>", "Unban a person", (arg, player) -> {
        	if (!adminVerif(player)) return;

            if (netServer.admins.unbanPlayerIP(arg[0]) || netServer.admins.unbanPlayerID(arg[0])) info(player, "Unbanned player: [accent]%s", arg[0]);
            else err(player, "That IP/ID is not banned!");
        });

        handler.<Player>register("players", "<all|online|ban>", "Gives the list of players according to the type of filter given", (arg, player) -> {
        	if (!adminVerif(player)) return;
        	StringBuilder builder = new StringBuilder();
        	Seq<PlayerInfo> bannedPlayers = netServer.admins.getBanned();
        	
            switch (arg[0]) {
            	case "ban":
            		builder.append("\nTotal banned players : [green]").append(netServer.admins.getBanned().size).append("[].\n[gold]--------------------------------[]").append("\n[accent]Banned Players:");
            		player.sendMessage(builder.toString());
            		bannedPlayers.each(p -> {
            			player.sendMessage("[white]======================================================================\n" +
            					"[lightgray]" + p.id +"[white] / Name: [lightgray]" + p.lastName + "[white]\n" +
            					" / IP: [lightgray]" + p.lastIP + "[white] / # kick: [lightgray]" + p.timesKicked);
            		});
            		break;
            
            	case "online":
            		builder.append("\nTotal online players: [green]").append(Groups.player.size()).append("[].\n[gold]--------------------------------[]").append("\n[accent]List of players: \n");
            		for (Player p : Groups.player) {
            			if (!p.admin) {
            				p.name = p.name.replaceAll("\\[", "[[");
            				builder.append("[white]");
            			}
            			if (p.admin) builder.append("[white]\uE828 ");
            			builder.append(" - [lightgray]").append(p.name).append("[]: [accent]'").append(p.uuid()).append("'[]");
            			if (p.admin) builder.append("[white] | [scarlet]Admin[]");
            			builder.append("\n[accent]");
            		}
            		player.sendMessage(builder.toString());
            		break;
            	
            	case "all":
            		Seq<PlayerInfo> all = netServer.admins.getWhitelisted();
            		builder.append("\nTotal players: [green]").append(all.size).append("[].\n[gold]--------------------------------[]").append("\n[accent]List of players: []\n");
            		for (PlayerInfo p : all) {
            			builder.append("[white] - [lightgray]Names: [accent]").append(p.names).append("[white] - [lightgray]ID: [accent]'").append(p.id).append("'");
            			if (p.admin) builder.append("[white] | [scarlet]Admin");
            			if (p.banned) builder.append("[white] | [orange]Banned");
            			for (Player pID: Groups.player) {
            				if (pID.ip() == p.lastIP) {
            					builder.append("[white] | [green]Online");
            					break;
            				}
            			}
            			builder.append("\n");
            		}
            		player.sendMessage(builder.toString());
            		break;
            	
            	default:
            		player.sendMessage("[scarlet]Invalid usage:[lightgray] Invalid arguments.");
            }
            
        });
             
        handler.<Player>register("kill", "[username]", "Kill a player", (arg, player) -> {
        	if (!adminVerif(player)) return;
            
        	if (arg.length == 0) player.unit().kill();
            else {
                Player other = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[0]));
                if (other != null) other.unit().kill();
                else player.sendMessage("[accent]This player doesn't exist!");
            }
        });
    
    }

    private void sendMessage(Player player, String fmt, Object... tokens) {
        player.sendMessage(String.format(fmt, tokens));
    }
    private void err(Player player, String fmt, Object... msg) {
        sendMessage(player, "[scarlet]Error: " + fmt, msg);
    }
    private void info(Player player, String fmt, Object... msg) {
        sendMessage(player, "Info: " + fmt, msg);
    }
    private boolean adminVerif(Player player) {
    	if(!player.admin()){
    		player.sendMessage("[scarlet]This command is only for admins.");
            return false;
    	} else return true;
    }
    
    //leave spectate mode
    public void SpectateLeave(){
        Events.on(PlayerLeave.class, event -> {
            if(rememberSpectate.containsKey(event.player)){
                rememberSpectate.remove(event.player);
            }
        });
    }
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
            Tile coreTile = newTeam.core().tileOn();
            return new coreTeamReturn(newTeam, coreTile.drawx(), coreTile.drawy());
        }
    }

    class coreTeamReturn{
        Team team;
        float x,y;
        public coreTeamReturn(Team _t, float _x, float _y){
            team = _t;
            x = _x;
            y = _y;
        }
    }
}
