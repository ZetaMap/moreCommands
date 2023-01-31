package util;

import mindustry.game.Team;
import mindustry.gen.Player;
import data.TempData;


public class Players {
	public final Player player;
	public final TempData data;
	public final String[] rest;
	public final boolean found;
	
	
	private Players(TempData d, String args) {
		String[] test = args.strip().split(" ");
		
		if (d == null) this.player = null;
		else this.player = d.player;
		this.data = d;
		this.rest = test.length == 1 && test[0].isBlank() ? new String[]{} : test;
		this.found = this.player != null;
	}
	
	public static void errNotOnline(Player player) {
		err(player, "Player not connected or doesn't exist!");
	}

	public static void errPermDenied(Player player) {
		err(player, "You don't have the permission to use arguments!");
	}
	
	public static boolean errFilterAction(String action, filter.FilterSearchReponse filter, boolean type) {
		if (!filter.type.onlyPlayers) {
			if (type) err(filter.trigger, "@ is only for players!", action);
			else err(filter.trigger, "Can @ only players!", action);
			return true;
		} else return false;
	}
	
	public static void err(Player player, String fmt, Object... msg) {
    	player.sendMessage("[scarlet]Error: " + Strings.format(fmt, msg));
    }
    public static void info(Player player, String fmt, Object... msg) {
    	player.sendMessage("Info: " + Strings.format(fmt, msg));
    }
    public static void warn(Player player, String fmt, Object... msg) {
    	player.sendMessage("[gold]Warning: []" + Strings.format(fmt, msg));
    }
    
    //check the player if admin 
    public static boolean adminCheck(Player player) {
    	if(!player.admin()){
    		player.sendMessage("[scarlet]This command is only for admins!");
            return false;
    	} else return true;
    }
    
    public static Players findByName(String[] args) { return findByName(String.join(" ", args)); }
    public static Players findByName(String arg) {
    	String args = arg + " ";
    	TempData target = TempData.find(p -> args.startsWith(p.realName + " "));
    	byte type = 0;
    	
    	if (target == null) {
    		target = TempData.find(p -> args.startsWith(p.noColorName + " "));
    		type = 1;
    	}
    	if (target == null) {
    		target = TempData.find(p -> args.startsWith(p.stripedName + " "));
    		type = 2;
    	}
    	
    	return target == null ? new Players(null, args) : new Players(target, args.substring((type == 0 ? target.realName : type == 1 ? target.noColorName : target.stripedName).length()));
    }
    
    public static Players findByID(String[] args) { return findByID(String.join(" ", args)); }
    public static Players findByID(String arg) {
    	String args = arg + " ";
    	TempData target = TempData.find(p -> args.startsWith(p.player.uuid() + " "));
    	
    	return target == null ? new Players(null, args) : new Players(target, args.substring(target.player.uuid().length()));
    }
    
    public static Players findByNameOrID(String[] args) { return findByNameOrID(String.join(" ", args)); }
    public static Players findByNameOrID(String arg) {
    	Players target = Players.findByName(arg);
    	return target.found ? target : Players.findByID(arg);
    }
    
    public static void tpPlayer(mindustry.gen.Unit unit, int x, int y) {
    	Thread tp = new Thread("UnitTeleport_Unit-" + unit.id) {
    		int limit = 30;
    		Player player = unit.getPlayer();
    		
    		@Override
    		public void run() {
		    	for (int i=0; i<10; i++) move();
		    	while (!unit.within(x, y, 2*mindustry.Vars.tilesize) && limit-- > 0) move(); 		
    		}
    		
    		public void move() {
	    		unit.set(x, y);
				if (player != null) {
	    			player.set(x, y);
	            	mindustry.gen.Call.setPosition(player.con, x, y);
				}
	
				try { Thread.sleep(50); }
				catch (Exception e) {}	
    		}
    	};
    	tp.setDaemon(true);
    	tp.start();
    }
    
    public static Team findTeam(String name) {
    	if (name == "purple") return Team.malis;
    	for (Team team : Team.all) {
    		if (team.name.equals(name)) return team;
    	}
    	return null;
    }
}