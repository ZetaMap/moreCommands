package data;

import arc.func.Boolf;
import arc.struct.ObjectMap;
import arc.struct.Seq;

import mindustry.gen.Call;
import mindustry.gen.Player;

import util.Strings;

public class TempData {
	private static ObjectMap<Player, TempData> data = new ObjectMap<>();
	public static final String creatorID = "k6uyrb9D3dEAAAAArLs28w==";
	
	public final Player player;
	public final MSG msgData = new MSG();
	public mindustry.game.Team spectate = null;
	public Effects effect = Effects.getByName("none");
	public Thread rainbowLoop, effectLoop;
	public final String realName, noColorName, stripedName;
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
        this.setRainbowLoop();
        this.setEffectLoop();
    }

	public boolean spectate() {
		return this.spectate != null;
	}
	
	public void setRainbowLoop() {
		TempData target = this;
		
		this.rainbowLoop = new Thread("RainbowLoop_Player-" + this.player.id) {
			public void run() {
				while(target.rainbowed) {
					try {
                        if (target.hue < 360) target.hue+=5;
                        else target.hue = 0;
                        
                        for (int i=0; i<5; i++) 
                        	Call.effectReliable(mindustry.content.Fx.bubble, target.player.x, target.player.y, 10, 
                        		arc.graphics.Color.valueOf(Integer.toHexString(java.awt.Color.getHSBColor(target.hue / 360f, 1f, 1f).getRGB()).substring(2)));
                        target.player.name = Strings.RGBString(target.noColorName, target.hue);
                        
                        Thread.sleep(64);
					} catch (InterruptedException e) { return; }
				}
			}
		};
		this.rainbowLoop.setDaemon(true);
	}
	
	public void setEffectLoop() {
		TempData target = this;
		
		this.effectLoop = new Thread("EffectLoop_Player-" + this.player.id) {
    		public void run() {
        		while(target.hasEffect) {
    				try { 
    					Call.effectReliable(target.effect.effect, target.player.x, target.player.y, 10, arc.graphics.Color.green);
    					Thread.sleep(64); 
    				} catch (InterruptedException e) { return; }
    			}
    		}
    	};
    	this.effectLoop.setDaemon(true);
	}

	public void reset() {
		TempData newData = new TempData(this.player);
		
		this.player.name = this.realName;
		if (spectate()) this.player.team(this.spectate);
		this.player.unit().health = this.player.unit().maxHealth;
		this.msgData.removeTarget();
		this.spectate = newData.spectate;
		this.effect = newData.effect;
		this.rainbowLoop = newData.rainbowLoop;
		this.effectLoop = newData.effectLoop;
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
    		+ ", rainbowLoop: " + this.rainbowLoop + ", effectLoop: " + this.effectLoop
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
    
    public static TempData putDefault(Player p) {
    	TempData data_ = new TempData(p);
    	data_.msgData.player = p;
    	data.put(p, data_);
    	return data_;
    }
    
    public static void remove(Player p) {
    	TempData data_ = get(p);
    	
    	data_.msgData.removeTarget();
    	data_.rainbowLoop.interrupt();
    	data_.effectLoop.interrupt();
    	data.remove(p);
    }

    public static boolean contains(Player p) {
    	return data.containsKey(p);
    }
    
    public static void setField(arc.func.Cons<TempData> item) {
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
