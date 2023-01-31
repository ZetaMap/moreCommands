package data;

import java.util.Locale;

import arc.func.Boolf;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.gen.Player;

import util.Strings;

public class TempData {
  private static ObjectMap<Player, TempData> data = new ObjectMap<>();
  public static ObjectMap<String, Seq<TempData>> localeOrdonedPlayer = new ObjectMap<>();
  public static final String creatorID = "k6uyrb9D3dEAAAAArLs28w==";

  public final Player player;
  public final MSG msgData = new MSG();
  public mindustry.game.Team spectate = null;
  public Effects effect = Effects.getByName("none");
  public mindustry.gen.Unit lastUnit;
  public final Locale locale;
  public final String realName, noColorName, stripedName, nameColor;
  public String tag = "", noColorTag = "", rainbowedName = "";
  public int hue = 0;
  public boolean votedVNW = false, votedRTV = false, rainbowed = false, hasEffect = false, isMuted = false, inGodmode = false, isCreator;

  private TempData(Player p) {
    this.player = p;
    this.lastUnit = p.unit();
    this.locale = Locale.forLanguageTag(p.locale().replace('_', '-'));
    this.realName = p.name;
    this.noColorName = Strings.stripColors(p.name).strip();
    this.stripedName = Strings.stripGlyphs(this.noColorName).strip();
    this.nameColor = "[#" + this.player.color.toString() + "]";
    this.isCreator = p.uuid().equals(creatorID);
  }

  public boolean spectate() {
    return this.spectate != null;
  }

  public void applyTag() {
    if (PVars.tags && PVars.playerTags.containsKey(this.player.uuid())) {
      this.tag = "[gold][[[white]" + PVars.playerTags.get(this.player.uuid()) + "[gold]] ";
      this.noColorTag = Strings.stripColors(this.tag).strip().substring(1) + " ";

    } else if (PVars.tags && this.player.admin) {
      this.tag = "[gold][[[scarlet]<Admin>[gold]] ";
      this.noColorTag = "[<Admin>] ";

    } else {
      this.tag = "";
      this.noColorTag = this.tag;
    }

    this.player.name = (this.tag.isBlank() ? "" : this.tag + this.nameColor) + this.realName;
  }

  public String getName() {
    return this.rainbowed ? this.rainbowedName : this.nameColor + this.realName;
  }

  public void reset() {
    TempData newData = new TempData(this.player);

    if (spectate()) this.player.team(this.spectate);
    this.player.unit().health = this.player.unit().maxHealth;
    this.lastUnit = newData.lastUnit;
    this.msgData.removeTarget();
    this.spectate = newData.spectate;
    this.effect = newData.effect;
    this.hue = newData.hue;
    this.votedVNW = newData.votedVNW;
    this.votedRTV = newData.votedRTV;
    this.rainbowed = newData.rainbowed;
    this.hasEffect = newData.hasEffect;
    this.isMuted = newData.isMuted;
    this.inGodmode = newData.inGodmode;
    this.isCreator = newData.isCreator;
  }

  public static TempData getByName(String name) {
    return find(p -> p.player.name.equals(name));
  }

  public static TempData getByID(String id) {
    return find(p -> p.player.uuid().equals(id));
  }

  public static TempData get(Player p) {
    if (p == null) return null;
    return data.get(p);
  }

  public static TempData put(Player p) {
    TempData data_ = new TempData(p);

    data_.msgData.player = data_;
    data.put(p, data_);

    if (!localeOrdonedPlayer.containsKey(p.locale)) localeOrdonedPlayer.put(p.locale, Seq.with(data_));
    else localeOrdonedPlayer.get(p.locale).add(data_);

    return data_;
  }

  public static TempData remove(Player p) {
    TempData data_ = get(p);

    data_.msgData.removeTarget();
    data.remove(p);

    if (localeOrdonedPlayer.get(p.locale).size < 2) localeOrdonedPlayer.remove(p.locale);
    else localeOrdonedPlayer.get(p.locale).remove(data_);

    return data_;
  }

  public static boolean contains(Player p) {
    return data.containsKey(p);
  }

  public static void each(Cons<TempData> item) {
    data.each((k, v) -> item.get(v));
  }

  public static void each(Boolf<TempData> pred, Cons<TempData> item) {
    data.each((k, v) -> {
      if (pred.get(v)) item.get(v);
    });
  }

  public static int count(Boolf<TempData> pred) {
    int size = 0;

    for (TempData p : data.values()) {
      if (pred.get(p)) size++;
    }

    return size;
  }

  public static TempData find(Boolf<TempData> pred) {
    for (TempData p : data.values()) {
      if (pred.get(p)) return p;
    }

    return null;
  }

  public class MSG {
    public TempData player = null, target = null;
    public boolean targetOnline = false;

    private MSG() {}

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
