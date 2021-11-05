package data;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Strings;

import mindustry.game.Team;
import mindustry.gen.Player;


public class TempData {
	private static ObjectMap<Player, TempData> data = new ObjectMap<>();
	public static final String creatorID = "k6uyrb9D3dEAAAAArLs28w==";
	public final Player player;
	public Team spectate = null;
	public final String realName, stripedName;
	public float savedBuildSpeed = 0;
	public int hue = 0;
	public boolean votedVNW = false, 
		votedRTV = false, 
		rainbowed = false, 
		hasEffect = false, 
		isMuted = false,
		inGodmode = false,
		isCreator = false;
	
	public TempData(Player p){
		this.player = p;
        this.realName = p.name;
        this.stripedName = Strings.stripGlyphs(Strings.stripColors(p.name.strip()));
        this.isCreator = p.uuid().equals(creatorID);
    }
	
	public boolean spectate() {
		return this.spectate != null;
	}
	
	
	public static Seq<TempData> copy() {
		return data.values().toSeq();
	}
	
	public static TempData getByName(String name) {
		return get(data.keys().toSeq().find(p -> p.name.equals(name)));
	}
	
	public static TempData getByID(String uuid) {
		return get(data.keys().toSeq().find(p -> p.uuid().equals(uuid)));
	}
	
	public static TempData get(Player p) {
		if (p == null) return null;
		return data.get(p);
	}
	
    public static TempData put(TempData data) {
    	TempData.data.put(data.player, data);
    	return data;
    }
    
    public static TempData putDefault(Player p) {
    	return put(new TempData(p));
    }
    
    public static void remove(Player p) {
    	data.remove(p);
    }

    public static boolean contains(Player p) {
    	return data.containsKey(p);
    }
    
    public static void setAll(arc.func.Cons<TempData> item) {
    	data.forEach(d -> item.get(d.value));
    }
    
    public static Seq<TempData> filter(arc.func.Boolf<TempData> pred) {
    	return data.values().toSeq().filter(pred);
    }
}
