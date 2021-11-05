package data;

import arc.struct.ObjectMap;

import mindustry.gen.Player;


public class MSG {
	private static ObjectMap<Player, MSG> data = new ObjectMap<>();	
	public final Player player;
	public Player target;
	
	private MSG(Player p, Player t) {
		this.player = p;
		this.target = t;
	}

	public void setTarget(Player target) {
		this.target = target;
		data.get(target).target = this.player;
	}
	
	public static MSG setEmpty(Player p) {
		return data.put(p, new MSG(p, null));
	}
	
	public static MSG set(Player p, Player target) {
		return data.put(p, new MSG(p, target));
	}
	
	public static MSG get(Player p) {
		return data.get(p);
	}
	
	public static void remove(Player p) {
		data.remove(p);
	}
	
	public static boolean contains(Player p) {
		return data.containsKey(p);
	}
	
	public static arc.struct.Seq<MSG> copy() {
		return data.values().toSeq();
	}
}
