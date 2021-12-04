package data;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import util.Strings;


public class Switcher {
	private static ObjectMap<String, Switcher> list = new ObjectMap<>();
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
	
	public ConnectReponse connect(mindustry.gen.Player player) {
		ConnectReponse reponse = new ConnectReponse();

		if (this.forAdmin && !player.admin) reponse.failed("Server only for admins.");
		else {
			arc.util.async.Threads.daemon("ServerPing_Player-" + player.id, () -> 
				mindustry.Vars.net.pingHost(this.ip, this.port, s -> {
					if (s.playerLimit > 0 && s.players >= s.playerLimit) reponse.failed("Server full. (" + s.players + "/" + s.playerLimit + ")");
					else if (s.version != mindustry.core.Version.build) reponse.failed("Incompatible version. Required: " + s.version);
					else mindustry.gen.Call.connect(player.con, this.ip, this.port);
					
					reponse.pingFinished = true;
					
				}, f -> {
					reponse.failed("The server not responding. (Connexion timed out!)");
					reponse.pingFinished = true;
				})
			);
			while (!reponse.pingFinished) {}
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
			
		} else new_.changed = list.put(stripedName, new_) == null ? false : true;
		
		return new_;
	}
	
	public static Switcher remove(String name) {
		name = Strings.stripGlyphs(Strings.stripColors(name.replace('_', ' '))).strip().toLowerCase();
		Switcher value;
		
		if (lobby != null && name.equals(lobby.name)) {
			value = lobby;
			lobby = null;
		
		} else value = list.remove(name);

		return value;
	}
	
	public static Switcher getByName(String name) {
		return list.get(Strings.stripGlyphs(Strings.stripColors(name.replace('_', ' '))).strip().toLowerCase());
	}
	
	public static Switcher getByIP(String ip) {
		 return list.values().toSeq().find(i -> ip.equals(i.address()));
	}
	
	public static Seq<String> names(boolean isAdmin) {
		if (isAdmin) return list.values().toSeq().map(i -> i.name);
		else return list.values().toSeq().filter(i -> !i.forAdmin).map(i -> i.name);
	}
	
	public static Seq<String> ips() {
		return list.values().toSeq().map(i -> i.ip);
	}
	
	public static Seq<Integer> ports() {
		return list.values().toSeq().map(i -> i.port);
	}
	
	public static void each(arc.func.Cons<Switcher> consumer) {
		list.values().toSeq().each(consumer);
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
		if (lobby == null) Core.settings.putJson("SwitchList", list.values().toSeq().asMap(i -> i.name, i -> i.address() + "-" + i.forAdmin));
		else Core.settings.putJson("SwitchList", list.values().toSeq().addAll(lobby).asMap(i -> i.name, i -> i.address() + "-" + i.forAdmin));
	}
	
	
	public static class ConnectReponse {
		private volatile boolean pingFinished = false;
		public boolean failed = false;
		public String message = "Connection success.";
		
		private ConnectReponse() {
		}
		
		public void failed(String message) {
			this.failed = true;
			this.message = message;
		}
	}
}
