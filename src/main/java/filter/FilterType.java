package filter;

import arc.func.Func;
import arc.struct.Seq;

import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;


public enum FilterType {
	players("a", "all players", "@ @ players", true, t -> Seq.with(Groups.player).map(p -> p.unit())),
	trigger("p", "the player who triggered the command", "@ yourself", true, t -> Seq.with(t.unit())),
	random("r", "a random player", "@ @", true, t -> Seq.with(Seq.with(Groups.player).random().unit())),
	randomUnit("ru", "a random unit", "@ a @", false, t -> Seq.with(Seq.with(Groups.unit).random())),
	units("e", "all untis and players", "@ @ units and players", false, t -> Seq.with(Groups.unit)),
	withoutPlayers("u", "all units, except players", "@ @ units", false, t -> Seq.with(Groups.unit).filter(u -> u.getPlayer() == null)),
	team("t", "all units and players in the team", "@ @ units and players in team @", false, t -> Seq.with(Groups.unit).filter(u -> u.team.equals(t.team()))),
	playersInTeam("ta", "all players in the team", "@ @ players in team @", true, t -> Seq.with(Groups.player).filter(p -> p.team().equals(t.team())).map(p -> p.unit())),
	withoutPlayersInTeam("tu", "all units, except players, in the team", "@ @ units in team @", false, t -> Seq.with(Groups.unit).filter(u -> u.getPlayer() == null && u.team.equals(t.team())));

	public static String prefix = "@";
	public final Func<Player, Seq<Unit>> filter;
	public final String desc, formatedDesc;
	public final boolean onlyPlayers;
	private String value;
	
	private FilterType(String value, String desc, String format, boolean onlyPlayers, Func<Player, Seq<Unit>> filter) {
		this.value = value;
		this.desc = desc;
		this.formatedDesc = format;
		this.onlyPlayers = onlyPlayers;
		this.filter = filter;
	}

	public String getValue() {
		return prefix + this.value;
	}

	public enum Reponses {
		found,
		prefixFound,
		notFound,
		invalidArguments,
		disabled,
		permsDenied
	}
}