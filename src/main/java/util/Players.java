package util;

import static mindustry.Vars.net;
import static mindustry.Vars.state;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;

import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.net.WorldReloader;


public class Players {
	public final Player player;
	public final String find;
	public final String[] rest;
	
	private Players(Player p, String args, String s) {
		Seq<String> temp = new Seq<String>().addAll(args.split(" "));
		if (temp.size > 1 && temp.get(0).isBlank()) temp.remove(0);
		
		this.player = p;
		this.find = s;
		this.rest = temp.toArray(String.class);
	}
	
	public static void err(Player player, String fmt, Object... msg) {
    	player.sendMessage("[scarlet]Error: " + String.format(fmt, msg));
    }
    public static void info(Player player, String fmt, Object... msg) {
    	player.sendMessage("Info: " + String.format(fmt, msg));
    }
    public static void warn(Player player, String fmt, Object... msg) {
    	player.sendMessage("[gold]Warning: []" + String.format(fmt, msg));
    }
    
    //check the player if admin 
    public static boolean adminCheck(Player player) {
    	if(!player.admin()){
    		player.sendMessage("[scarlet]This command is only for admins!");
            return false;
    	} else return true;
    }
    
    public static Players findByName(String args) {
    	final String arg= args + " ";
    	Player target = Groups.player.find(p -> arg.startsWith(p.name + " "));
    	if (target == null) target = Groups.player.find(p -> arg.startsWith(Strings.stripColors(p.name) + " "));
    	
    	if (target == null) return new Players(target, arg, "");
    	else return new Players(target, arg.substring(Strings.stripColors(target.name).length()), arg.substring(0, arg.length()-Strings.stripColors(target.name).length()));
    }
    
    public static Players findByID(String args) {
    	final String arg= args + " ";
    	Player target = Groups.player.find(p -> arg.startsWith(p.uuid() + " "));
    	
    	if (target == null) return new Players(target, arg, "");
    	else return new Players(target, arg.substring(target.uuid().length()), arg.substring(0, arg.length()-target.uuid().length()));
    }
    
    public static Players findByNameOrID(String str) {
    	Players target = Players.findByName(str);
    	if (target == null) target = Players.findByID(str);
    	
    	return target;
    }
    
    
    public static class TempData {
    	private static ObjectMap<Player, TempData> tempData = new ObjectMap<>(); // uuid, data
    	public final Player player;
    	public final String realName;
    	public final String normalizedName;
    	public int hue;
    	
    	private TempData(int hue, Player p){
            this.hue = hue;
    		this.player = p;
            this.realName = p.name;
            this.normalizedName = Strings.stripColors(p.name).strip();
        }
    	
    	public void setHue(int i) {
        	this.hue = i; 
        }
    	
    	public static Seq<TempData> copy() {
    		return tempData.values().toSeq();
    	}
    	
    	public static TempData get(Player p) {
    		return tempData.get(p);
    	}
    	
        public static TempData put(Player p) {
        	return tempData.put(p, new TempData(0, p));
        }
        
        public static void remove(Player p) {
        	tempData.remove(p);
        }

        public static boolean contains(Player p) {
        	return tempData.containsKey(p);
        }
    }
    
    
    public static class MSG {
    	private static ObjectMap<Player, MSG> data = new ObjectMap<>();	
    	public final Player player;
    	public Player target;
    	
    	private MSG(Player p, Player t) {
    		this.player = p;
    		this.target = t;
    	}

    	public void setTarget(Player target) {
    		this.target = target;
    		data.get(target).target = this.player;
    	}
    	
    	public static MSG setEmpty(Player p) {
    		return data.put(p, new MSG(p, null));
    	}
    	
    	public static MSG set(Player p, Player target) {
    		return data.put(p, new MSG(p, target));
    	}
    	
    	public static MSG get(Player p) {
    		return data.get(p);
    	}
    	
    	public static void remove(Player p) {
    		data.remove(p);
    	}
    	
    	public static boolean contains(Player p) {
    		return data.containsKey(p);
    	}
    	
    	public static Seq<MSG> copy() {
    		return data.values().toSeq();
    	}
    }
    
    
    public static class Search {
    	public Player player = null;
    	public int[] XY = null;
    	public String[] rest = {};
    	public boolean error = false;
    	
    	public Search(String str, Player pDefault) {
    		Players result = findByName(str);
        	String co[];
        	Seq<String> temp = new Seq<String>().addAll(result.rest);
        	
        	if (result.player == null) {
        		if (temp.isEmpty()) return;
        		co = result.rest[0].split(",");
        		
        		if (co.length > 2) {
        			Players.err(pDefault, "Wrong coordinates!");
        			this.error = true;
        		} else if (!Strings.canParseInt(co[0]) || !Strings.canParseInt(co[1]) || co.length == 1) {
        			Players.err(pDefault, "This player doesn't exist or not connected!");
        			this.error = true;
        		} else {
        			temp.remove(0);
        			this.player = pDefault;
        			this.XY = new int[] {Strings.parseInt(co[0]), Strings.parseInt(co[1])};
        			this.rest = temp.toArray(String.class);
        		}
        		
        	} else {
        		this.player = result.player;
        		this.rest = temp.toArray(String.class);
        	}
    	}
    }
    
    
    public static class RTV {
    	private mindustry.game.Gamemode lastMode = state.rules.mode();

		public RTV(Map map, mindustry.game.Team winner) {
			Map temp = state.map;
			try { 
				state.map = null;
				arc.Events.fire(new mindustry.game.EventType.GameOverEvent(null)); 
				Call.gameOver(winner);
			} catch (NullPointerException e) { state.map = temp; }
			
            if (state.rules.waves) Log.info("Game over! Reached wave @ with @ players online on map @.", state.wave, Groups.player.size(), Strings.capitalize(Strings.stripColors(state.map.name())));
            else Log.info("Game over! Team @ is victorious with @ players online on map @.", winner.name, Groups.player.size(), Strings.capitalize(Strings.stripColors(state.map.name())));

            //set next map to be played
            if (map != null) {
                Call.infoMessage((state.rules.pvp
                ? "[accent]The " + winner.name + " team is victorious![]\n" : "[scarlet]Game over![]\n")
                + "\nNext selected map:[accent] " + Strings.stripColors(map.name()) + "[]"
                + (map.tags.containsKey("author") && !map.tags.get("author").trim().isEmpty() ? " by[accent] " + map.author() + "[white]" : "") + "." +
                "\nNew game begins in 12 seconds.");

                state.gameOver = true;
                Call.updateGameOver(winner);
                Log.info("Selected next map to be @.", Strings.stripColors(map.name()));

                Runnable r = () -> {
                    WorldReloader reloader = new WorldReloader();
                    reloader.begin();

					mindustry.Vars.world.loadMap(map, map.applyRules(lastMode));
                    state.rules = state.map.applyRules(lastMode);
                    mindustry.Vars.logic.play();

                    reloader.end();
                };
                arc.util.Timer.schedule(new arc.util.Timer.Task() {
                	@Override
                	public void run() {
                		try { r.run(); } 
                		catch (mindustry.maps.MapException e) {
                			Log.err(e.map.name() + ": " + e.getMessage());
                			net.closeServer();
                		}
                	}
                }, 12);
                
            } else {
            	mindustry.Vars.netServer.kickAll(mindustry.net.Packets.KickReason.gameover);
                state.set(mindustry.core.GameState.State.menu);
                net.closeServer();
            }
    	}
    }
}