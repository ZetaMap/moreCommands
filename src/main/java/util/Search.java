package util;

import arc.struct.Seq;

import mindustry.gen.Player;


public class Search {
	public Player player = null;
	public int[] XY = null;
	public String[] rest = {};
	public boolean error = false;
	
	public Search(String[] str, Player pDefault) {
		Players result = Players.findByName(str);
    	
    	if (result.found) {
    		this.player = result.player;
    		this.rest = result.rest;
    	
    	} else {
    		if (result.rest.length == 0) return;
    		else if (result.rest[0].contains(",")) {
    			String co[] = result.rest[0].split(",");
    			
    			if (co.length == 2 && Strings.canParseInt(co[0]) && Strings.canParseInt(co[1])) {
    				Seq<String> rest = Seq.with(result.rest);
    				rest.remove(0);
    				
	    			this.player = pDefault;
	    			this.XY = new int[] {Strings.parseInt(co[0]), Strings.parseInt(co[1])};
	    			this.rest = rest.toArray(String.class);
	    			return;
    			
    			} else Players.err(pDefault, "Wrong coordinates!");
    		} else Players.errNotOnline(pDefault);
    		
    		this.error = true;
    	}
	}
}
