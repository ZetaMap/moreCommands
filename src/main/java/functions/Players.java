package functions;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.gen.Player;


public class Players {
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
    
    public static Player find(String nameOrID) {
    	Player target = Groups.player.find(p -> p.uuid().equals(nameOrID));
    	if (target == null) target = Groups.player.find(p -> Strings.stripColors(nameOrID).equals(TempData.get(p).normalizedName));
     	if (target == null) target = Groups.player.find(p -> Strings.stripColors(nameOrID.replaceAll("_", " ")).equals(TempData.get(p).normalizedName));
     	
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
            this.normalizedName = Strings.stripColors(p.name);
        }
    	
    	public void setHue(int i) {
        	this.hue = i; 
        }
    	
    	public void setMsg(Player target) {
    		
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
}