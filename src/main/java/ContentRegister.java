import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

import arc.Events;
import arc.func.Cons;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.CommandHandler.CommandRunner;
import arc.util.async.Threads;

import mindustry.core.NetClient;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration.ActionType;
import mindustry.net.Packets.KickReason;
import data.CM;
import data.TempData;
import data.PVars;


public class ContentRegister {
	public static void initFilters() {
		//filter for muted, rainbowed players and disabled chat
    	netServer.admins.addChatFilter((p, m) -> {
    		TempData data = TempData.get(p);
    		
    		if (PVars.tchat) {
    			if (data.isMuted) {
    				util.Players.err(p, "You're muted, you can't speak.");
    				m = null;
    			
    			} else if (data.rainbowed) {
    				Log.info("RAINBOWED: <@: @>", data.realName, m);
    				Call.sendMessage(m, p.name, p);
    				m = null;
    			}

    		} else {
    			if (p.admin) {
    				Call.sendMessage(m, "[scarlet]<Admin>[]" + NetClient.colorizeName(p.id, p.name), p);
    				if (data.rainbowed) Log.info("ADMIN: RAINBOWED: <@: @>",  p.name, m);
    				else Log.info("ADMIN: <@: @>",  p.name, m);
    			
    			} else p.sendMessage("[scarlet]The tchat is disabled, you can't write!");
    			m = null;
    		}
    		
    		return m;
    	});
    	
    	//filter for players in GodMode
    	netServer.admins.addActionFilter(a -> {
    		if (a.player != null && TempData.get(a.player).inGodmode) {
    			if (a.type == ActionType.placeBlock) {
    				Call.constructFinish(a.tile, a.block, a.unit, (byte) a.rotation, a.player.team(), a.config);
    				return false;
    			
    			} else if (a.type == ActionType.breakBlock) {
    				Call.deconstructFinish(a.tile, a.block, a.unit);
    				return false;
    			}
    		}
    		
    		return true;
    	});
	}
	
	public static void initEvents() {
    	//clear VNW & RTV votes and disabled it on game over
        Events.on(EventType.GameOverEvent.class, e -> {
        	PVars.canVote = false;
        	TempData.setAll(p -> p.votedVNW = false);
        	TempData.setAll(p -> p.votedRTV = false);
        });
        
        Events.on(EventType.WorldLoadEvent.class, e -> PVars.canVote = true); //re-enabled votes
        
        Events.on(EventType.PlayerConnect.class, e -> 
        	Threads.daemon("ConnectCheck_Player-" + e.player.id, () -> {
        		String name = TempData.putDefault(e.player).stripedName; //add player in TempData
        		data.BM.nameCheck(e.player); //check the nickname of this player
	        	
	        	//check if the nickname is empty without colors and emoji
	        	if (name.isBlank()) e.player.kick(KickReason.nameEmpty);
	        	
	        	//prevent to duplicate nicknames
	        	if (Groups.player.contains(p -> TempData.get(p).stripedName.equals(name))) e.player.kick(KickReason.nameInUse);		
        	})
        );
        
        Events.on(EventType.PlayerJoin.class, e -> {
        	//for me =)
        	if (TempData.get(e.player).isCreator) { 
        		if (PVars.niceWelcome) 
        			Call.sendMessage("[scarlet]\ue80f " + NetClient.colorizeName(e.player.id, e.player.name) + "[scarlet] has connected! \ue80f   [lightgray]Everyone say: Hello creator! XD");
        		Call.infoMessage(e.player.con, "Hello creator ! =)");
        	}
        	
        	//unpause the game if one player is connected
        	if (Groups.player.size() == 1 && PVars.autoPause) {
        		state.serverPaused = false;
        		Log.info("auto-pause: " + Groups.player.size() + " player connected -> Game unpaused...");
        		Call.sendMessage("[scarlet][Server]:[] Game unpaused...");
        	}
        	
        	//fix the admin bug
        	if (e.player.getInfo().admin) e.player.admin = true;
        });
        
        Events.on(EventType.PlayerLeave.class, e -> {
        	//pause the game if no one is connected
        	if (Groups.player.size()-1 < 1 && PVars.autoPause) {
        		state.serverPaused = true;
        		Log.info("auto-pause: " + (Groups.player.size()-1) + " player connected -> Game paused...");
        	}
        	
        	TempData.remove(e.player); //remove player in TempData
        });
	}
	
	public static CommandsRegister setHandler(CommandHandler handler) {
		Threads.daemon("CommandManagerWaiter-" + handler.getPrefix(), () -> {
			try {
				Thread.sleep(1000);
				CM.load(handler);
			} catch (InterruptedException e) { e.printStackTrace(); }
		});
		
		return new CommandsRegister(handler);
	}
	
	
	public static class CommandsRegister {
		public CommandHandler handler;
		
		private CommandsRegister(CommandHandler handler) {
			this.handler = handler;
		}
		
		public void add(String name, String desc, CommandRunner<Player> runner) { add(name, "", desc, runner); }
		public void add(String name, String params, String desc, CommandRunner<Player> runner) {
			this.handler.<Player>register(name, params, desc, (arg, player) -> 
				Threads.daemon("ClientCommandRunner_Player-" + player.id, () -> runner.accept(arg, player))
			);
			
		}
		
		public void add(String name, String desc, Cons<String[]> runner) { add(name, "", desc, runner); }
		public void add(String name, String params, String desc, Cons<String[]> runner) {
			this.handler.register(name, params, desc, (arg) -> 
				Threads.daemon("ServerCommandRunner_Name-" + name, () -> runner.get(arg))
			);
		}
	}
}
