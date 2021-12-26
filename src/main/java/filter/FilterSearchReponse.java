package filter;

import arc.struct.Seq;

import mindustry.gen.Player;
import mindustry.gen.Unit;

import util.Players;
import filter.FilterType.*;


public class FilterSearchReponse {
	public FilterType type;
	public final Reponses reponse;
	public final Player trigger;
	public String[] rest;
	public String filterArgs;
	
	protected FilterSearchReponse(FilterType type, Reponses reponse, Player trigger, String args) {
		this.type = type;
		this.reponse = reponse;
		this.trigger = trigger;
		this.filterArgs = "";
		
		Seq<String> temp = Seq.with((type != null ? args.substring(type.getValue().length()) : args).split(" "));
		if (temp.size > 1 && temp.get(0).isBlank()) temp.remove(0);
		
		this.rest = temp.toArray(String.class);
	}
	
	public boolean sendIfError() {
		boolean error = true;
		
		if (this.reponse == Reponses.disabled) Players.err(this.trigger, "Filters are disabled!");
		else if (this.reponse == Reponses.permsDenied) Players.err(this.trigger, "You don't have the permission to use filters!");
		else if (this.reponse == Reponses.prefixFound) 
			Players.err(this.trigger, "[scarlet]No filter found with name '" + this.rest[0] + "'. [lightgray]Use '/help filter' to display help of all filters.");
		else if (this.reponse == Reponses.found) error = false;
		
		return error;
	}
	
	public int execute(arc.func.Cons<ArgsFilter> code) {
		return execute(ctx -> {
			code.get(ctx);
			return true;
		});
	}
	public int execute(arc.func.Boolf<ArgsFilter> code) {
		if (this.reponse == Reponses.found && this.type != null) {
			Seq<Unit> units = this.type.filter.get(this.trigger);
			int counter = 0;
			
			for (Unit u : units) {
				try { if (code.get(new ArgsFilter(this.type, u.getPlayer(), u))) counter++; } 
				catch (Exception e) { Players.err(this.trigger, "[]" + e.getMessage()); }
			}

			return counter;

		} else return 0;
	}
}