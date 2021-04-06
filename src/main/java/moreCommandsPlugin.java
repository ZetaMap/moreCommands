import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

import java.util.Arrays;
import java.util.HashSet;

import arc.Events;
import arc.struct.ObjectMap;
import arc.util.CommandHandler;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;
import mindustry.world.Tile;


public class moreCommandsPlugin extends Plugin {
    Timer.Task task;
    private long TEAM_CD = 10000L;
    private ObjectMap<Player, Long> teamTimers = new ObjectMap<>();
    private static double ratio = 0.6;
    private HashSet<String> votes = new HashSet<>();
    private Team spectateTeam = Team.all[8];
    private ObjectMap<Player, Team> rememberSpectate = new ObjectMap<>();
    
    private void sendMessage(Player player, String fmt, Object... tokens) {
        player.sendMessage(String.format(fmt, tokens));
    }
    private void err(Player player, String fmt, Object... msg) {
        sendMessage(player, "[scarlet]Error: " + fmt, msg);
    }
    private void info(Player player, String fmt, Object... msg) {
        sendMessage(player, "Info: " + fmt, msg);
    }

    public void SpectateLeave(){
        Events.on(PlayerLeave.class, event -> {
            if(rememberSpectate.containsKey(event.player)){
                rememberSpectate.remove(event.player);
            }
            if(teamTimers.containsKey(event.player)){
                teamTimers.remove(event.player);
            }
        });
    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("ut","unit type", (args, player) ->{
           player.sendMessage(player.unit().type().name);
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
            }else{
                ret = getPosTeamLoc(player);
            }

            //move team mechanic
            if(ret != null) {
                Call.setPlayerTeamEditor(player, ret.team);
                player.team(ret.team);
                //maybe not needed
                Call.setPosition(player.con, ret.x, ret.y);
                player.unit().set(ret.x, ret.y);
                player.snapSync();
                teamTimers.put(player, System.currentTimeMillis()+TEAM_CD);
                Call.sendMessage(String.format("> [orange]%s []changed to team [sky]%s", player.name, ret.team));
            }else{
                player.sendMessage("[scarlet]You can't change teams ...");
            }
        });

        handler.<Player>register("spectate", "[scarlet]Admin only[]", (args, player) -> {
            if(!player.admin()){
               player.sendMessage("[scarlet]This command is only for admins.");
               return;
            }
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

        handler.<Player>register("vnw", "", "(VoteNewWave) Vote for Sending a new Wave", (args, player) -> {
        	if (this.votes.contains(player.uuid()) || this.votes.contains(netServer.admins.getInfo(player.uuid()).lastIP)) {
                player.sendMessage("You have Voted already.");
                return;
        	}

            this.votes.add(player.uuid());
            int cur = this.votes.size();
            int req = (int) Math.ceil(ratio * Groups.player.size());
            Call.sendMessage("[orange]" + NetClient.colorizeName(player.id, player.name) + "[][lightgray] has voted for a new wave,[green]" + cur + "[] votes, [green]" + req + "[] required");
 
            if (cur < req) {
                return;
            }

            this.votes.clear();
            Call.sendMessage("[green]Vote for Sending a New Wave is Passed. New Wave will be Spawned.");
            state.wavetime = 0f;
            task.cancel();
		});
        
        handler.<Player>register("maps", "List all maps on server", (arg, player) -> {
            if (!Vars.maps.all().isEmpty()) {
                info(player, "Maps:");
                for (Map map : Vars.maps.all()) {
                    info(player, "  @: @", map.name(), map.custom ? "Custom" : "Default");
                }
            } else {
                info(player, "No maps found.");
            }
        });

        handler.<Player>register("kick", "<username...>", "Kick a person by name", (arg, player) -> {
            if (!player.admin()) {
                player.sendMessage("[scarlet]This command is only for admins.");
                return;
            }

            Player target = Groups.player.find(p -> p.name().equals(arg[0]));
            if (target != null) {
                Call.sendMessage("[scarlet]" + target.name() + "[scarlet] has been kicked by the server.");
                target.kick(KickReason.kick);
                info(player, "It is done.");
            } else {
                info(player, "Nobody with that name could be found...");
            }
        });
        
        handler.<Player>register("ban", "<type-id|name|ip> <username|IP|ID...>", "Ban a person", (arg, player) -> {
            if (!player.admin()) {
                player.sendMessage("[scarlet]This command is only for admins.");
                return;
            }

            if (arg[0].equals("id")) {
                Vars.netServer.admins.banPlayerID(arg[1]);
                info(player, "Banned.");
            } else if (arg[0].equals("name")) {
                Player target = Groups.player.find(p -> p.name().equalsIgnoreCase(arg[1]));
                if (target != null) {
                    Vars.netServer.admins.banPlayer(target.uuid());
                    info(player, "Banned.");
                } else {
                    err(player, "No matches found.");
                }
            } else if (arg[0].equals("ip")) {
                Vars.netServer.admins.banPlayerIP(arg[1]);
                info(player, "Banned.");
            } else {
                err(player, "Invalid type.");
            }

            for (Player gPlayer : Groups.player) {
                if (Vars.netServer.admins.isIDBanned(gPlayer.uuid())) {
                    Call.sendMessage("[scarlet]" + gPlayer.name + " has been banned.");
                    gPlayer.con.kick(KickReason.banned);
                }
            }
        });
        
        handler.<Player>register("pardon", "<ID>", "Pardon a votekicked player by ID and allow them to join again", (arg, player) -> {
        	if (!player.admin()) {
        		player.sendMessage("[scarlet]This command is only for admins.");
        		return;
        		}

        	PlayerInfo info = Vars.netServer.admins.getInfoOptional(arg[0]);
        	
        	if (info != null) {
        		info.lastKicked = 0;
        		info(player, "Pardoned player: @", info.lastName);
        		} else {
        			err(player, "That ID can't be found.");
        		}
        });
        
        handler.<Player>register("unban", "<ip|ID>", "Unban a person", (arg, player) -> {
            if (!player.admin()) {
                player.sendMessage("[scarlet]This command is only for admins.");
                return;
            }

            if (Vars.netServer.admins.unbanPlayerIP(arg[0]) || Vars.netServer.admins.unbanPlayerID(arg[0])) {
                info(player, "Unbanned player: @", arg[0]);
            } else {
                err(player, "That IP/ID is not banned!");
            }
        });

        handler.<Player>register("ac", "<message...>", "Admin Chat", (arg, player) -> {
            if(player.admin()) Groups.player.each(p -> p.admin, o -> o.sendMessage(arg[0], player, "[red]<Admin>" + NetClient.colorizeName(player.id, player.name)));
        });
        
        handler.<Player>register("players", "[all|connect|ban]", "Gives the list of players according to the type of filter given", (arg, player) -> {
            if (!player.admin()) {
                player.sendMessage("[scarlet]This command is only for admins.");
                return;
            }
            
        	return;
        });
        
        handler.<Player>register("info-all", "<username|IP|ID...>", "List all information related to the given player", (arg, player) -> {
            if (!player.admin()) {
                player.sendMessage("[scarlet]This command is only for admins.");
                return;
            }
            
            return;
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