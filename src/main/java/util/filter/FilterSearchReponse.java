package util.filter;

import java.util.Random;

import arc.func.Cons;
import arc.struct.Seq;

import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;

import util.Players;
import util.filter.FilterType.*;

public class FilterSearchReponse {
	public FilterType type;
	public Arguments arguments;
	public final Reponses reponse;
	public final Player trigger;
	public String[] rest;
	public String filterArgs;
	
	protected FilterSearchReponse(FilterType type, Reponses reponse, Player trigger, String args) {
		this.type = type;
		this.arguments = Arguments.none;
		this.reponse = reponse;
		this.trigger = trigger;
		this.filterArgs = "";
		
		Seq<String> temp = new Seq<String>().addAll((type != null ? args.substring(type.getValue().length()) : args).split(" "));
		if (temp.size > 1 && temp.get(0).isBlank()) temp.remove(0);
		
		this.rest = temp.toArray(String.class);
	}
	
	public void evalArgs() {
		// TODO: evaluate the filter arguments 
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
	
	public int execute(Cons<ArgsFilter> code) {
		Seq<Unit> units = new Seq<Unit>().addAll(Groups.unit);
		int counter = 1;
		
		if (this.reponse == Reponses.found) {
			if (this.type == FilterType.players) {
				counter = Groups.player.size();
				Groups.player.each(p -> code.get(new ArgsFilter(type, p, p.unit())));	
			
			} else if (this.type == FilterType.trigger) execute(new ArgsFilter(type, this.trigger), code);
				
			else if (this.type == FilterType.random) execute(new ArgsFilter(type, Groups.player.index(new Random().nextInt(Groups.player.size()))), code);
			
			else if (this.type == FilterType.randomUnit) {
				Unit unit = units.get(new Random().nextInt(units.size));
				execute(new ArgsFilter(type, unit.getPlayer(), unit), code);
			
			} else if (this.type == FilterType.units) {
				counter = units.size;
				units.each(u -> execute(new ArgsFilter(type, u.getPlayer(), u), code));
			
			} else if (this.type == FilterType.withoutPlayers) {
				counter = units.count(u -> u.getPlayer() == null);
				units.each(u -> u.getPlayer() == null, u -> execute(new ArgsFilter(type, null, u), code));
			
			} else if (this.type == FilterType.team) {
				counter = units.count(u -> u.team.equals(this.trigger.team()));
				units.each(u -> u.team.equals(this.trigger.team()), u -> execute(new ArgsFilter(type, u.getPlayer(), u), code));
			
			} else if (this.type == FilterType.playersInTeam) {
				counter = Groups.player.count(p -> p.team().equals(this.trigger.team()));
				Groups.player.each(p -> p.team().equals(this.trigger.team()), p -> execute(new ArgsFilter(type, p), code));
			
			} else if (this.type == FilterType.withoutPlayersInTeam) {
				counter = units.count(u -> u.team.equals(this.trigger.team()) && u.getPlayer() == null);
				units.each(u -> u.team.equals(this.trigger.team()) && u.getPlayer() == null, u -> execute(new ArgsFilter(type, null, u), code));
			
			} else counter = 0;
		} else counter = 0;

		return counter;
	}
	
	public void execute(ArgsFilter filter, Cons<ArgsFilter> code) {
		try { code.get(filter); }
		catch (Exception e) { Players.err(this.trigger, "[]" + e.getMessage()); }
	}
}