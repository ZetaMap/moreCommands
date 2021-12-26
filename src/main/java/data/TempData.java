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
	public mindustry.gen.Unit lastUnit;
	public final String realName, noColorName, stripedName;
	public String tag, noColorTag;
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
		this.lastUnit = p.unit();
        this.realName = p.name;
        this.noColorName = Strings.stripColors(p.name).strip();
        this.stripedName = Strings.stripGlyphs(this.noColorName).strip();
        this.isCreator = p.uuid().equals(creatorID);
    }

	public boolean spectate() {
		return this.spectate != null;
	}
	
	public void resetName() {
		this.player.name = this.tag + mindustry.core.NetClient.colorizeName(this.player.id, this.realName);
	}
	
	public void applyTag() {
		if (PVars.tags && PVars.playerTags.containsKey(this.player.uuid())) {
			this.tag = "[gold][[[white]" + PVars.playerTags.get(this.player.uuid()) + "[gold]] ";
			this.noColorTag = Strings.stripColors(this.tag).strip();
		
		} else if (PVars.tags && this.player.admin) {
			this.tag = "[gold][[[scarlet]<Admin>[gold]] ";
			this.noColorTag = "[<Admin>] ";
		
		} else {
			this.tag = "";
			this.noColorTag = this.tag;
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
		this.noColorTag = newData.noColorTag;
		this.hue = newData.hue;
		this.votedVNW = newData.votedVNW;
		this.votedRTV = newData.votedRTV;
		this.rainbowed = newData.rainbowed;
		this.hasEffect = newData.hasEffect;
		this.isMuted = newData.isMuted;
		this.inGodmode = newData.inGodmode;
		this.isCreator = newData.isCreator;
		
		this.applyTag();
		this.resetName();
	}
	
	public String toString() {
    	return "TempData{" 
    		+ "player: " + this.player + ", msgData: " + this.msgData
    		+ ", spectate: " + this.spectate + ", effect: " + this.effect 
    		+ ", realName: " + this.realName + ", noColorName: " + this.noColorName 
    		+ ", stripedName: " + this.stripedName + ", tag: " + this.tag
    		+ ", noColorTag:" + this.noColorTag + ", hue: " + this.hue
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
    	
    	data_.msgData.player = data_;
    	data.put(p, data_);
    	ordonedData.add(data_);
    	return data_;
    }
    
    public static TempData remove(Player p) {
    	TempData data_ = get(p);
    	
    	data_.msgData.removeTarget();
    	data.remove(p);
    	ordonedData.remove(data_);
    	return data_;
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
    	public TempData player = null, target = null;
    	public boolean targetOnline = false;
    	
    	private MSG() {
    	}
    	
    	public void setTarget(TempData target) {
    		if (target != null) {
    			this.target = target;
    			this.targetOnline = true;
    			target.msgData.target = this.player;
    			target.msgData.targetOnline = true;
    		}
    	}
    	
    	public void removeTarget() {
    		if (this.target != null) {
	    		this.target.msgData.targetOnline = false;
	    		this.targetOnline = false;
	    		
    		}
    	}
    }
}
