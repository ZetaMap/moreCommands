package manager;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import util.Strings;


public class Rank {
  private static Seq<Rank> rankList = new Seq<>(), defaultRanks = Seq.with(new Rank("player", 0), new Rank("admin", "[scarlet]<Admin>", 1, true));
  public static boolean isEnabled = false, tags = true, bubbleChat = false;
  
  public final Seq<String> commands = new Seq<>(), players = new Seq<>();
  public String name, tag = "", noColorTag = "";
  public final int level;
  public boolean isAdmin = false;
  
  private Rank(String name, int level) {  
    this.name = name;
    this.level = level;
  }
  
  private Rank(String name, String tag, int level, boolean isAdmin) {
    this.name = name;
    this.tag = tag;
    this.noColorTag = Strings.stripColors(this.tag).strip();
    this.level = level;
    this.isAdmin = isAdmin;
  }
  
  public boolean tagValid() {
    return !this.tag.equals("") && !this.noColorTag.equals("");
  }
  
  public void addPlayer(String playerID) {
    this.players.add(playerID);
  }
  
  public boolean changeRank(String playerID, int level) {
    boolean found = this.players.remove(playerID);
    
    if (found) get(level, true).players.add(playerID);
    return found;
  }
  
  
  public static Seq<Rank> copy(boolean defaultRanks) {
    if (defaultRanks) return  Rank.defaultRanks.copy();
    else return rankList.copy();
  }
  
  public static Rank get(String playerID, boolean defaultRanks) {
    if (defaultRanks) return Rank.defaultRanks.find(r -> r.players.contains(playerID));
    else return rankList.find(r -> r.players.contains(playerID));
  }
  
  public static Rank get(int level, boolean defaultRanks) {
    if (defaultRanks) return Rank.defaultRanks.get(level);
    else return rankList.get(level);
  }

  public static void ranksCommand(String[] arg) {
    
  }
  
  @SuppressWarnings("unchecked")
  public static void load() {
    if (Core.settings.has("Ranks")) isEnabled = Core.settings.getBool("Ranks");
    else Core.settings.put("Ranks", isEnabled);
    
    if (Core.settings.has("Tags")) tags = Core.settings.getBool("Tags");
    else Core.settings.put("Tags", tags);
    
    if (Core.settings.has("Bubble")) bubbleChat = Core.settings.getBool("Bubble");
    else Core.settings.putJson("Bubble", bubbleChat);
    
    if (Core.settings.has("RanksList")) rankList = Core.settings.getJson("RankList", Seq.class, Seq::new);
    else Core.settings.putJson("RanksList", new Seq<Rank>());
    
    if (Core.settings.has("DefaultRanksPlayers")) Core.settings.getJson("DefaultRanksPlayers", ObjectMap.class, ObjectMap::new).each((k, v) -> get((int) k, true).players.addAll((String) v));
    else Core.settings.putJson("DefaultRanksPlayers", new ObjectMap<Integer, Seq<String>>());
  }
  
  public static void saveSettings() {
    Core.settings.put("Ranks", isEnabled);
    Core.settings.put("Tags", tags);
    Core.settings.put("Bubble", bubbleChat);
    Core.settings.putJson("RanksList", rankList);
    Core.settings.putJson("DefaultRanksPlayers", defaultRanks.asMap(k -> k.level, v -> v.players));
  }
}
