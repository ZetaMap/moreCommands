package filter;

public enum FilterType {
	players("a", "all players"),
	trigger("p", "the player who triggered the command"),
	random("r", "a random player"),
	randomUnit("ru", "a random unit"),
	units("e", "all untis and players"),
	withoutPlayers("u", "all units, except players"),
	team("t", "all units and players in the team"),
	playersInTeam("ta", "all players in the team"),
	withoutPlayersInTeam("tu", "all units, except players, in the team");

	public static String prefix = "@";
	private String value, desc;
	
	private FilterType(String value, String desc) {
		this.value = value;
		this.desc = desc;
	}

	public String getValue() {
		return prefix + this.value;
	}
	
	public String getDesc() {
		return this.desc;
	}
	
	public boolean onlyPlayers() {
		if (this == FilterType.players || this == FilterType.random || this == FilterType.trigger || this == FilterType.playersInTeam) 
			return true;
		else return false;
	}
	

	public enum Reponses {
		found,
		prefixFound,
		notFound,
		invalidArguments,
		disabled,
		permsDenied
	}
	
	
	public enum Arguments {
		type,
		data,
		distance,
		dx,
		dy,
		name,
		team,
		x,
		y,
		none
	}
}