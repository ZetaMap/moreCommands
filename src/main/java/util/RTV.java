package util;

import static mindustry.Vars.state;

import arc.util.Log;

import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.maps.Map;
import mindustry.net.WorldReloader;

public class RTV {
	public RTV(Map map, mindustry.game.Team winner) {
		Map temp = state.map;
		try { 
			state.map = null;
			arc.Events.fire(new mindustry.game.EventType.GameOverEvent(null)); 
			Call.gameOver(winner);
		} catch (NullPointerException e) {}
		state.map = temp;
		
        if (state.rules.waves) Log.info("Game over! Reached wave @ with @ players online on map @.", state.wave, Groups.player.size(), Strings.capitalize(Strings.stripColors(state.map.name())));
        else Log.info("Game over! Vote to change map with @ players online on map @.", Groups.player.size(), Strings.capitalize(Strings.stripColors(state.map.name())));

        //set next map to be played
        Call.infoMessage(Strings.format("@![]\n \nNext selected map:[accent] @ [white] by [accent]@ [white].\nNew game begins in 10 seconds.", 
        	state.rules.pvp ? "[accent]Vote to change map" : "[scarlet]Game over", map.name(), map.author()));

        state.gameOver = true;
        Call.updateGameOver(winner);
        Log.info("Selected next map to be @.", Strings.stripColors(map.name()));

        arc.util.Timer.schedule(() -> {
        	try {
        		WorldReloader reloader = new WorldReloader();
                reloader.begin();

				mindustry.Vars.world.loadMap(map, map.applyRules(state.rules.mode()));
                state.rules = state.map.applyRules(state.rules.mode());
                mindustry.Vars.logic.play();

                reloader.end();
        		
        	} catch (mindustry.maps.MapException e) {
        		Log.err(e.map.name() + ": " + e.getMessage());
        		mindustry.Vars.net.closeServer();
        	}
        	
        }, 10);
	}
}
