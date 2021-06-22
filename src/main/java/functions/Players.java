package functions;

import arc.util.Strings;

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
    
    public static Player find(String name) {
    	Player target = TempData.findPlayer(Strings.stripColors(name));
     	if (target == null) target = TempData.findPlayer(Strings.stripColors(name.replaceAll("_", " ")));
     	
    	return target;
    }
}