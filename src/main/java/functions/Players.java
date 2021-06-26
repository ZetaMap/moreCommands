package functions;

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
}