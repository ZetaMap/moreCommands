package data;

import arc.func.Boolf;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import mindustry.gen.Player;

import util.Strings;

public class TempData {
	private static ObjectMap<Player, TempData> data = new ObjectMap<>();
	public static final String creatorID = "k6uyrb9D3dEAAAAArLs28w==";
	public final Player player;
	public MSG msgData = new MSG();
	public mindustry.game.Team spectate = null;
	public final String realName, noColorName, stripedName;
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
        this.noColorName = Strings.stripColors(p.name.strip());
        this.stripedName = String.valueOf(Strings.stripGlyphs(this.noColorName));
        this.isCreator = p.uuid().equals(creatorID);
    }

	public boolean spectate() {
		return this.spectate != null;
	}
	
	public String toString() {
    	return "TempData{" 
    		+ "player: " + this.player + ", spectate: " + this.spectate 
    		+ ", realName: " + this.realName + ", noColorName: " + this.noColorName 
    		+ ", stripedName: " + this.stripedName + ", hue: " + this.hue
    		+ ", votedVNW: " + this.votedVNW + ", votedRTV: " + this.votedRTV
    		+ ", rainbowed: " + this.rainbowed + ", hasEffect: " + this.hasEffect
    		+ ", isMuted: " + this.isMuted + ", inGodmode: " + this.inGodmode
    		+ ", isCreator: " + this.isCreator 
    		+ "}";
    }
	
	
	public static Seq<TempData> copy() {
		return data.values().toSeq();
	}
	
	public static TempData getByName(String name) {
		return get(data.keys().toSeq().find(p -> p.name.equals(name)));
	}

	public static TempData getByID(String id) {
		return get(data.keys().toSeq().find(p -> p.uuid().equals(id)));
	}
	
	public static TempData get(Player p) {
		if (p == null) return null;
		return data.get(p);
	}
	
    public static TempData put(TempData data) {
    	data.msgData.player = data.player;
    	TempData.data.put(data.player, data);
    	return data;
    }
    
    public static TempData putDefault(Player p) {
    	return put(new TempData(p));
    }
    
    public static void remove(Player p) {
    	get(p).msgData.removeTarget();
    	data.remove(p);
    }

    public static boolean contains(Player p) {
    	return data.containsKey(p);
    }
    
    public static void setAll(arc.func.Cons<TempData> item) {
    	data.forEach(d -> item.get(d.value));
    }
    
    public static Seq<TempData> filter(Boolf<TempData> pred) {
    	return data.values().toSeq().filter(pred);
    }
    
    public static TempData find(Boolf<TempData> pred) {
    	return data.values().toSeq().find(pred);
	}
    
    
    public class MSG {
    	public Player player = null, target = null;
    	public boolean targetOnline = false;
    	
    	private MSG() {
    	}
    	
    	public void setTarget(Player target) {
    		TempData t = TempData.get(target);
    		
    		if (t != null) {
    			this.target = target;
    			this.targetOnline = true;
    			t.msgData.target = this.player;
    		}
    	}
    	
    	public void removeTarget() {
    		if (this.target != null) {
	    		MSG t = TempData.get(this.target).msgData;
	    		t.target = null;
	    		t.targetOnline = false;
    		}
    	}
    }
}
