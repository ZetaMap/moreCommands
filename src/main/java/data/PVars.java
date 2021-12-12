package data;

import arc.struct.Seq;


public class PVars {
	public static Seq<String> adminCommands = new Seq<>();
    public static mindustry.maps.Map selectedMap;
    public static short waveVoted = 1;
    public static boolean tchat = true,
    	autoPause = false, 
    	niceWelcome = true, 
    	unbanConfirm = false, 
    	clearConfirm = false, 
    	canVote = true;
}
