package functions;

import arc.struct.ArrayMap;
import arc.struct.Seq;

import mindustry.content.Fx;
import mindustry.entities.Effect;

public class Effects {
	private static ArrayMap<String, Effects> effects = new ArrayMap<>();
	public Effect effect;
	public String name;
	public int id;
	
	private Effects (Effect effect, String name, int id) {
		this.effect = effect;
		this.name = name;
		this.id = id;
	}
	
	public static Effects getByID(int id) {
		return effects.getValueAt(id);	
	}
	
	public static Effects getByName(String name) {
		return effects.get(name);
	}
	
	public static int size() {
		return effects.size;
	}
	
	public static Seq<Effects> copy() {
		return effects.values().toArray();
	}
	
	public static void init() {
		for (java.lang.reflect.Field f : Fx.class.getDeclaredFields()) {
			try { effects.put(f.getName(), new Effects((Effect) f.get(f), f.getName(), effects.size+1)); } 
			catch (IllegalArgumentException | IllegalAccessException e) { e.printStackTrace(); }
		}
	}
}
