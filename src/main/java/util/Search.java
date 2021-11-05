package util;

import arc.struct.Seq;

import mindustry.gen.Player;


public class Search {
	public Player player = null;
	public int[] XY = null;
	public String[] rest = {};
	public boolean error = false;
	
	public Search(String str, Player pDefault) {
		Players result = Players.findByName(str);
    	String co[];
    	Seq<String> temp = new Seq<String>().addAll(result.rest);
    	
    	if (result.player == null) {
    		if (temp.isEmpty()) return;
    		co = result.rest[0].split(",");
    		
    		if (co.length > 2) {
    			Players.err(pDefault, "Wrong coordinates!");
    			this.error = true;
    		} else if (!Strings.canParseInt(co[0]) || !Strings.canParseInt(co[1]) || co.length == 1) {
    			Players.err(pDefault, "This player doesn't exist or not connected!");
    			this.error = true;
    		} else {
    			temp.remove(0);
    			this.player = pDefault;
    			this.XY = new int[] {Strings.parseInt(co[0]), Strings.parseInt(co[1])};
    			this.rest = temp.toArray(String.class);
    		}
    		
    	} else {
    		this.player = result.player;
    		this.rest = temp.toArray(String.class);
    	}
	}
}
