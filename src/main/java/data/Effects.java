package data;

import arc.Core;
import arc.struct.Seq;

import mindustry.entities.Effect;


public class Effects {
	public static Seq<Effects> effects = new Seq<>();
	
	public final Effect effect;
	public final String name;
	public final int id;
	public boolean disabled = false;
	
	private Effects (Effect effect, String name, int id) {
		this.effect = effect;
		this.name = name;
		this.id = id;
	}
	
	public static Effects getByID(int id) {
		if (id < 0 || id >= effects.size) return null;
		else return effects.get(id);
	}
	
	public static Effects getByName(String name) {
		return effects.find(e -> e.name.equals(name));
	}
	
	public static int size() {
		return effects.size;
	}
	
	public static void setToDefault() {
		Effects ef;
		String[] list = {"none", "unitSpawn", "unitControl", "unitDespawn", "unitSpirit", "itemTransfer", "pointBeam", "lightning", "unitWreck", "rocketSmoke", 
			"rocketSmokeLarge", "fireSmoke", "melting", "wet", "muddy", "oily", "dropItem", "impactcloud", "unitShieldBreak", "coreLand"};
		
		effects.each(e -> e.disabled = false);
		for (String name : list) {
			ef = getByName(name);
			if (ef != null) ef.disabled = true;
		}
		
		
	}
	
	public static void init() {
		for (java.lang.reflect.Field f : mindustry.content.Fx.class.getDeclaredFields()) {
			try { effects.add(new Effects((Effect) f.get(null), f.getName(), effects.size+1)); } 
			catch (IllegalArgumentException e) { e.printStackTrace(); } 
			catch (IllegalAccessException e) {}
		}
		
		if (Core.settings.has("removedEffects")) {
			try {
				for (String line : Core.settings.getString("removedEffects").split(" \\| ")) {
					Effects effect = getByName(line);
					if (effect != null) effect.disabled = true;
				}

			} catch (Exception e) { saveSettings(); }
		} else saveSettings();
	}
	
	public static void saveSettings() {
		Core.settings.put("removedEffects", effects.select(e -> e.disabled).map(e -> e.name).toString(" | "));
	}
}
