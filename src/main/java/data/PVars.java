package data;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Timer.Task;

import mindustry.gen.Call;


public class PVars {
	public static ObjectMap<String, String> playerTags = new ObjectMap<>(),
		bansReason = new ObjectMap<>();
	public static String settingsPath = "config/moreCommands_settings/",
		ALogPath = "config/admin-logs/";
	public static Seq<String> adminCommands = new Seq<>(),
		recentMutes = new Seq<>();
    public static mindustry.maps.Map selectedMap;
    public static short waveVoted = 1;
    public static boolean tchat = true,
    	autoPause = false, 
    	niceWelcome = true, 
    	unbanConfirm = false, 
    	clearConfirm = false, 
    	canVote = true,
    	alogConfirm = false,
    	tags = true,
    	bubbleChat = false;
    public static Task rtvSession = new Task() {
		@Override
		public void run() {
			Call.sendMessage("[scarlet]Vote failed! []Not enough votes to change to the [accent] " + selectedMap.name() + "[white] map.");
			TempData.setField(p -> p.votedRTV = false);
			selectedMap = null;
            cancel();
		}
    	
    }, vnwSession = new Task() {
		@Override
		public void run() {
			Call.sendMessage("[scarlet]Vote for"+ (waveVoted == 1 ? "Sending a new wave" : "Skiping [scarlet]" + waveVoted + "[] waves") 
				+ " failed! []Not enough votes.");
			TempData.setField(p -> p.votedVNW = false);
			waveVoted = 0;
			cancel();
		}
    	
    };
}
