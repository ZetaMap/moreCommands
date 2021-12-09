package data;

import arc.struct.Seq;
import mindustry.maps.Map;

public class PVars {
	public static Seq<String> adminCommands = new Seq<String>().addAll("team", 
		"am", 
		"kick", 
		"pardon", 
		"ban", 
		"unban", 
		"players", 
		"kill", 
		"tp", 
		"core", 
		"chat", 
		"spawn", 
		"godmode", 
		"mute", 
		"unmute", 
		"reset");
    public static Map selectedMap;
    public static byte waveVoted = 1;
    public static boolean tchat = true,
    	autoPause = false, 
    	niceWelcome = true, 
    	unbanConfirm = false, 
    	clearConfirm = false, 
    	canVote = true;
}
