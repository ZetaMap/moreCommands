package util;

import arc.struct.Seq;

import mindustry.gen.Groups;
import mindustry.gen.Player;

import data.TempData;


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
    	if (target == null) target = Groups.player.find(p -> arg.startsWith(TempData.get(p).stripedName + " "));
    	
    	if (target == null) return new Players(target, arg, "");
    	else return new Players(target, arg.substring(TempData.get(target).stripedName.length()), arg.substring(0, arg.length()-TempData.get(target).stripedName.length()));
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
    
}