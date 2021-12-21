package util;

import static mindustry.Vars.net;
import static mindustry.Vars.state;

import arc.util.Log;

import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.maps.Map;
import mindustry.net.WorldReloader;

public class RTV {
	private mindustry.game.Gamemode lastMode = state.rules.mode();

	public RTV(Map map, mindustry.game.Team winner) {
		Map temp = state.map;
		try { 
			state.map = null;
			arc.Events.fire(new mindustry.game.EventType.GameOverEvent(null)); 
			Call.gameOver(winner);
		} catch (NullPointerException e) { state.map = temp; }
		
        if (state.rules.waves) Log.info("Game over! Reached wave @ with @ players online on map @.", state.wave, Groups.player.size(), Strings.capitalize(Strings.stripColors(state.map.name())));
        else Log.info("Game over! Team @ is victorious with @ players online on map @.", winner.name, Groups.player.size(), Strings.capitalize(Strings.stripColors(state.map.name())));

        //set next map to be played
        if (map != null) {
            Call.infoMessage((state.rules.pvp
            ? "[accent]The " + winner.name + " team is victorious![]\n" : "[scarlet]Game over![]\n")
            + "\nNext selected map:[accent] " + Strings.stripColors(map.name()) + "[]"
            + (map.tags.containsKey("author") && !map.tags.get("author").trim().isEmpty() ? " by[accent] " + map.author() + "[white]" : "") + "." +
            "\nNew game begins in 12 seconds.");

            state.gameOver = true;
            Call.updateGameOver(winner);
            Log.info("Selected next map to be @.", Strings.stripColors(map.name()));

            arc.util.Timer.schedule(() -> {
            	try {
            		WorldReloader reloader = new WorldReloader();
                    reloader.begin();

    				mindustry.Vars.world.loadMap(map, map.applyRules(lastMode));
                    state.rules = state.map.applyRules(lastMode);
                    mindustry.Vars.logic.play();

                    reloader.end();
            		
            	} catch (mindustry.maps.MapException e) {
            		Log.err(e.map.name() + ": " + e.getMessage());
        			net.closeServer();
            	}
            	
            }, 12);
            
        } else {
        	mindustry.Vars.netServer.kickAll(mindustry.net.Packets.KickReason.gameover);
            state.set(mindustry.core.GameState.State.menu);
            net.closeServer();
        }
	}
}
