package filter;

import arc.Core;

import mindustry.gen.Player;
import mindustry.gen.Unit;

import data.TempData;
import filter.FilterType.*;


public class ArgsFilter {
	public static String[] filters = new arc.struct.Seq<FilterType>().addAll(FilterType.values()).map(f -> f.getValue()).toArray(String.class);
	public static boolean enabled = true;
	
	public final FilterType type;
	public final Player player;
	public final TempData data;
	public Unit unit;

	public ArgsFilter(FilterType type, Player player, Unit unit) {
		this.type = type;
		this.data = TempData.get(player);
		this.player = player;
		this.unit = unit;
	}
	
	public static FilterSearchReponse hasFilter(Player trigger, String[] args) { return hasFilter(trigger, String.join(" ", args)); }
	public static FilterSearchReponse hasFilter(Player trigger, String arg) {
		arg = arg + " ";
		
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
