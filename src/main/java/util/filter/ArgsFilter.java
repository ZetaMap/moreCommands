package util.filter;

import arc.Core;
import arc.struct.ObjectMap;

import mindustry.gen.Player;
import mindustry.gen.Unit;

import data.TempData;
import util.filter.FilterType.*;


public class ArgsFilter {
	private static ObjectMap<Arguments, Object> defaultArguments = new ObjectMap<>();
	public static String[] filters = new arc.struct.Seq<FilterType>().addAll(FilterType.values()).map(f -> f.getValue()).toArray(String.class);
	public static boolean enabled = true;
	
	public final FilterType type;
	public final Player player;
	public final TempData data;
	public Unit unit;

	protected ArgsFilter(FilterType type, Player player) { this(type, player, player.unit()); }
	protected ArgsFilter(FilterType type, Player player, Unit unit) {
		this.type = type;
		this.data = TempData.get(player);
		this.player = player;
		this.unit = unit;
	}
	
	public static FilterSearchReponse hasFilter(Player trigger, String arg) { return hasFilter(trigger, new String[]{arg}); }
	public static FilterSearchReponse hasFilter(Player trigger, String[] args) {
		String arg = String.join(" ", args) + " ";
		
		if (trigger.admin) {
			if (arg.startsWith(FilterType.prefix)) {
				for (FilterType filter : FilterType.values()) {
					if (arg.startsWith(filter.getValue() + " ")) 
						return new FilterSearchReponse(filter, enabled ? Reponses.found : Reponses.disabled, trigger, arg);
				}
				
				return new FilterSearchReponse(null, enabled ? Reponses.prefixFound : Reponses.disabled, trigger, arg);
			} else return new FilterSearchReponse(null, Reponses.notFound, trigger, arg);
		} else return new FilterSearchReponse(null, Reponses.permsDenied, trigger, arg);
	}
	
	public static void load() {
		if (Core.settings.has("ArgsFilter")) enabled = Core.settings.getBool("ArgsFilter");
		else saveSettings();
	}
	
	public static void saveSettings() {
		Core.settings.put("ArgsFilter", enabled);
	}
}
