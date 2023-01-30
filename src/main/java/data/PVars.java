package data;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Timer.Task;
import arc.util.Timekeeper;

import mindustry.gen.Call;


public class PVars {
  public static Task rtvSession = new Task() {
    @Override
    public void run() {
      Call.sendMessage("[scarlet]Vote failed! []Not enough votes to change to the [accent] " + selectedMap.name() + "[white] map.");
      selectedMap = null;
      cancel();
    }
    
    @Override
    public void cancel() {
      TempData.each(p -> p.votedRTV = false);
      rtvCooldown.reset();
      super.cancel();
    }
    }, vnwSession = new Task() {
    @Override
    public void run() {
      Call.sendMessage("[scarlet]Vote for "+ (waveVoted == 1 ? "sending a new wave" : "skiping [scarlet]" + waveVoted + "[] waves")  + " failed! []Not enough votes.");
      waveVoted = 0;
      cancel();
    }
    
    @Override
    public void cancel() {
      TempData.each(p -> p.votedVNW = false);
      vnwCooldown.reset();
      super.cancel();
    }
    };
  public static ObjectMap<String, String> playerTags = new ObjectMap<>(),
    bansReason = new ObjectMap<>();
  public static String ALogPath = "config/admin-logs/";
  public static Seq<String> adminCommands = new Seq<>(),
    recentMutes = new Seq<>();
  public static mindustry.maps.Map selectedMap;
  public static Timekeeper rtvCooldown = new Timekeeper(3 * 60), 
    vnwCooldown = new Timekeeper(3 * 60);
  public static short waveVoted = 1;
  public static boolean chat = true,
    autoPause = false, 
    niceWelcome = true, 
    unbanConfirm = false, 
    clearConfirm = false, 
    canVote = true,
    alogConfirm = false,
    tags = true;
  
  static {
    rtvCooldown.reset();
    vnwCooldown.reset();
  }
}
