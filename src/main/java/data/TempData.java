package data;

import arc.func.Boolf;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import mindustry.gen.Player;

import util.Strings;

public class TempData {
	private static ObjectMap<Player, TempData> data = new ObjectMap<>();
	private static Seq<TempData> ordonedData = new Seq<>();
	public static final String creatorID = "k6uyrb9D3dEAAAAArLs28w==";
	
	public final Player player;
	public final MSG msgData = new MSG();
	public mindustry.game.Team spectate = null;
	public Effects effect = Effects.getByName("none");
	public final String realName, noColorName, stripedName;
	public String tag = "", stripedTag = tag;
	public int hue = 0;
	public boolean votedVNW = false, 
		votedRTV = false,
		rainbowed = false, 
		hasEffect = false, 
		isMuted = false,
		inGodmode = false,
		isCreator = false;
	
	private TempData(Player p){
		this.player = p;
        this.realName = p.name;
        this.noColorName = Strings.stripColors(p.name).strip();
        this.stripedName = Strings.stripGlyphs(this.noColorName).strip();
        this.isCreator = p.uuid().equals(creatorID);
    }

	public boolean spectate() {
		return this.spectate != null;
	}
	
	public void applyTag() {
		if (PVars.playerTags.containsKey(this.player.uuid())) {
			this.tag = PVars.playerTags.get(this.player.uuid());
			this.stripedTag =  Strings.stripGlyphs(Strings.stripColors(this.tag)).strip();
			this.tag += "[coral]: ";
		
		} else if (this.player.admin) {
			this.tag = "[scarlet]<Admin>[]: ";
			this.stripedTag = "<Admin>";
		}
	}

	public void reset() {
		TempData newData = new TempData(this.player);
		
		this.player.name = this.realName;
		if (spectate()) this.player.team(this.spectate);
		this.player.unit().health = this.player.unit().maxHealth;
		this.msgData.removeTarget();
		this.spectate = newData.spectate;
		this.effect = newData.effect;
		this.tag = newData.tag;
		this.stripedTag = newData.stripedTag;
		this.hue = newData.hue;
		this.votedVNW = newData.votedVNW;
		this.votedRTV = newData.votedRTV;
		this.rainbowed = newData.rainbowed;
		this.hasEffect = newData.hasEffect;
		this.isMuted = newData.isMuted;
		this.inGodmode = newData.inGodmode;
		this.isCreator = newData.isCreator;
	}
	
	public String toString() {
    	return "TempData{" 
    		+ "player: " + this.player + ", msgData: " + this.msgData
    		+ ", spectate: " + this.spectate + ", effect: " + this.effect 
    		+ ", realName: " + this.realName + ", noColorName: " + this.noColorName 
    		+ ", stripedName: " + this.stripedName + ", tag: " + this.tag
    		+ ", stripedTag:" + this.stripedTag + ", hue: " + this.hue
    		+ ", votedVNW: " + this.votedVNW + ", votedRTV: " + this.votedRTV
    		+ ", rainbowed: " + this.rainbowed + ", hasEffect: " + this.hasEffect
    		+ ", isMuted: " + this.isMuted + ", inGodmode: " + this.inGodmode
    		+ ", isCreator: " + this.isCreator 
    		+ "}";
    }
	
	public static Seq<TempData> copy() {
		return ordonedData;
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
    
    public static TempData put(Player p) {
    	TempData data_ = new TempData(p);
    	
    	data_.msgData.player = p;
    	data.put(p, data_);
    	ordonedData.add(data_);
    	return data_;
    }
    
    public static void remove(Player p) {
    	TempData data_ = get(p);
    	
    	data_.msgData.removeTarget();
    	data.remove(p);
    	ordonedData.remove(data_);
    }

    public static boolean contains(Player p) {
    	return data.containsKey(p);
    }
    
    public static void setField(arc.func.Cons<TempData> item) {
    	data.forEach(d -> item.get(d.value));
    	ordonedData.set(data.values().toSeq());
    }
    
    public static int count(Boolf<TempData> pred) {
    	return ordonedData.count(pred);
    }
    
    public static TempData find(Boolf<TempData> pred) {
    	return ordonedData.find(pred);
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
    			t.msgData.targetOnline = true;
    		}
    	}
    	
    	public void removeTarget() {
    		if (this.target != null) {
	    		TempData data = TempData.get(this.target);
	    		
	    		if (data != null) data.msgData.targetOnline = false;
	    		this.targetOnline = false;
	    		
    		}
    	}
    }
}
