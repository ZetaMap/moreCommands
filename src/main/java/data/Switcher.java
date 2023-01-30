package data;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import mindustry.net.Host;

import util.Strings;


public class Switcher {
  private static ObjectMap<String, Switcher> list = new ObjectMap<>();
  private static Seq<Switcher> ordonedList = new Seq<>();
  public static Switcher lobby = null;
  
  private boolean error = false;
  public String name = "", ip = "";
  public int port = 6567;
  public boolean changed = false;
  public boolean forAdmin = false;
  
  private Switcher(String name, String ip, boolean admin) {
    if (name.isBlank()) this.error = true;
    else this.name = name;
    
    String[] temp;
    
    if (ip.contains(":")) {
      temp = ip.split("\\:");
      
      if (temp.length == 2 && !temp[0].isBlank() && !temp[1].isBlank() && Strings.canParseInt(temp[1])) {
        int port = Strings.parseInt(temp[1]);
        
        if (port > 0 && port < 65535) {
          this.ip = temp[0];
          this.port = port;
        
        } else this.error = true;
      } else this.error = true;
    } else this.ip = ip;
    
    this.forAdmin = admin;
  }
  
  public String address() {
    return this.ip + ":" + this.port;
  }
  
  public Host ping() {
    Ping ping = new Ping();
    
    arc.util.Threads.daemon("ServerPing_Name-" + this.name, () -> 
      mindustry.Vars.net.pingHost(this.ip, this.port, s -> {
        ping.reponse = s;
        ping.finished = true;
      }, f -> ping.finished = true)
    );
    
    while (!ping.finished) {}
    return ping.reponse;
  }
  
  public ConnectReponse connect(mindustry.gen.Player player) {
    ConnectReponse reponse = new ConnectReponse();

    if (this.forAdmin && !player.admin) reponse.failed("Server only for admins.");
    else {
      Host ping = ping();
      
      if (ping != null) {
         arc.util.Log.info("@ @ @ @ @ @", ping.address, ping.description, ping.mapname, ping.modeName, ping.versionType, ping.ping);
      }
     
      if (ping == null) reponse.failed("The server not responding. (Connexion timed out!)");
      else if (ping.playerLimit > 0 && ping.players >= ping.playerLimit) reponse.failed("Server full. (" + ping.players + "/" + ping.playerLimit + ")");
      else if (ping.version != mindustry.core.Version.build) reponse.failed("Incompatible version. Required: " + ping.version);
      else mindustry.gen.Call.connect(player.con, this.ip, this.port);
    }

    return reponse;
  }
  
  public static Switcher put(String name, String ip, boolean admin) {
    name = name.replace('_', ' ').strip();
    String stripedName = Strings.stripGlyphs(Strings.stripColors(name)).strip().toLowerCase();
    Switcher new_ = new Switcher(name, ip, admin);
    
    if (new_.error) return null;
    
    else if (stripedName.equals("lobby")) {
      if (lobby != null) new_.changed = true;
      lobby = new_;
      lobby.name = stripedName;
      
    } else {
      new_.changed = list.put(stripedName, new_) == null ? false : true;
      if (new_.changed) ordonedList.remove(s -> s.name.equals(new_.name));
      ordonedList.add(new_);
    }
    
    return new_;
  }
  
  public static Switcher remove(String name) {
    name = Strings.stripGlyphs(Strings.stripColors(name.replace('_', ' '))).strip().toLowerCase();
    Switcher value;
    
    if (lobby != null && name.equals(lobby.name)) {
      value = lobby;
      lobby = null;
    
    } else {
      value = list.remove(name);
      ordonedList.remove(value);
    }

    return value;
  }
  
  public static Switcher getByName(String name) {
    return list.get(Strings.stripGlyphs(Strings.stripColors(name.replace('_', ' '))).strip().toLowerCase());
  }
  
  public static Switcher getByIP(String ip) {
     return ordonedList.find(i -> ip.equals(i.address()));
  }
  
  public static Seq<String> names() {
    return ordonedList.map(i -> i.name);
  }
  
  public static Seq<String> ips() {
    return ordonedList.map(i -> i.ip);
  }
  
  public static Seq<Integer> ports() {
    return ordonedList.map(i -> i.port);
  }
  
  public static void each(boolean isAdmin, arc.func.Cons<Switcher> consumer) {
    if (isAdmin) ordonedList.each(consumer);
    else ordonedList.select(s -> !s.forAdmin).each(consumer);
  }
  
  public static boolean isEmpty() {
    return list.isEmpty();
  }
  
  public static int size() {
    return list.size;
  }
  
  @SuppressWarnings("unchecked")
  public static void load() {
    if (Core.settings.has("SwitchList"))
      Core.settings.getJson("SwitchList", ObjectMap.class, ObjectMap::new).each((k, v) -> {
        String value = (String) v;
        put((String) k, value.subSequence(0, value.lastIndexOf('-')).toString(), Boolean.valueOf(value.substring(value.lastIndexOf('-')+1)));
      });
    
    else saveSettings();
  }
  
  public static void saveSettings() {
    if (lobby == null) Core.settings.putJson("SwitchList", ordonedList.asMap(i -> i.name, i -> i.address() + "-" + i.forAdmin));
    else Core.settings.putJson("SwitchList", ordonedList.addAll(lobby).asMap(i -> i.name, i -> i.address() + "-" + i.forAdmin));
  }
  
  
  public static class ConnectReponse {
    public boolean failed = false;
    public String message = "Connection success.";
    
    private ConnectReponse() {}
    
    public void failed(String message) {
      this.failed = true;
      this.message = message;
    }
  }
  
  
  private static class Ping {
    volatile boolean finished = false;
    Host reponse = null;
  }
}
