import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

import arc.Events;
import arc.func.Cons;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandRunner;
import arc.util.Log;
import arc.util.Threads;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration.ActionType;
import mindustry.net.Packets.KickReason;

import data.PVars;
import data.TempData;
import manager.CommandsManager;
import util.ALog;
import util.Players;
import util.Strings;

public class ContentRegister {
  public static void initFilters() {
    // test if Nucleaus plugin is present to keep auto-translated chat
    mindustry.mod.Mod nucleusPlugin = Vars.mods.getMod("xpdustry-nucleus") == null ? null : Vars.mods.getMod("xpdustry-nucleus").main;

    // filter for muted, rainbowed players, disabled chat, and tags
    // register this filter at second position after anti-spam of mindustry and
    // before others potential other filters
    netServer.admins.chatFilters.insert(1, (p, m) -> {
      TempData data = TempData.get(p);

      if (PVars.chat && data.isMuted) util.Players.err(p, "You're muted, you can't speak.");
      else if (!PVars.chat && !p.admin) p.sendMessage("[scarlet]Chat disabled, only admins can't speak!");
      else {
        Log.info(Strings.format("&lk@&fb&ly@&fr<&fi&lc@&fr: &fb&lw@&fr>", data.rainbowed ? "RAINBOWED: " : data.spectate() ? "VANISHED: " : "", data.noColorTag, data.realName, m));

        if (data.spectate()) Call.sendChatMessage("[coral][[]:[white] " + m);
        else if (nucleusPlugin != null) {
          final fr.xpdustry.nucleus.core.translation.Translator translator = ((fr.xpdustry.nucleus.mindustry.NucleusPlugin) nucleusPlugin).getTranslator();
          final String stripedMessage = Strings.stripColors(m);

          TempData.localeOrdonedPlayer.each((k, v) -> {
            String newMessage = m;

            try {
              newMessage += " [lightgray]("
                + translator.translate(stripedMessage, data.locale, v.first().locale)
                  .orTimeout(3l, java.util.concurrent.TimeUnit.SECONDS).join()
                + ")";
            } catch (Exception e) {
              Log.debug("Failed to translate message '" + stripedMessage + "' in language " + v.first().player.locale);
              Log.debug("Error: " + e.getLocalizedMessage());
            }

            for (int i = 0; i < v.size; i++) {
              Call.sendMessage(v.items[i].player.con, (PVars.tags ? data.tag : "")
                  + "[coral][[" + data.getName() + "[coral]]:[white] "
                  + (v.items[i].player == p ? m : newMessage), v.items[i].player == p ? m : newMessage, p);
            }

          });

        } else Call.sendMessage((PVars.tags ? data.tag : "") + "[coral][[" + data.getName() + "[coral]]:[white] " + m, m, p);
      }

      return null;
    });

    // filter for players in GodMode
    netServer.admins.addActionFilter(a -> {
      TempData p = TempData.get(a.player);
      if (p != null && p.inGodmode) {
        if (a.type == ActionType.placeBlock) Call.constructFinish(a.tile, a.block, a.player.unit(), (byte) a.rotation, a.player.team(), a.config);
        else if (a.type == ActionType.breakBlock) Call.deconstructFinish(a.tile, a.block, a.player.unit());
      }
      return true;
    });
  }

  public static void initEvents() {
    // try to modify destroy event to prevent potential error from nucleus
    Events.on(EventType.BlockDestroyEvent.class, e -> {

    });

    // clear VNW & RTV votes and disabled it on game over
    Events.on(EventType.GameOverEvent.class, e -> {
      PVars.canVote = false;
      TempData.each(p -> p.votedVNW = false);
      TempData.each(p -> p.votedRTV = false);
      PVars.rtvSession.cancel();
      PVars.vnwSession.cancel();
    });

    Events.on(EventType.WorldLoadEvent.class, e -> {
      PVars.canVote = true;// re-enabled votes
      state.set(State.playing);
    });

    Events.on(EventType.PlayerConnect.class, e -> Threads.daemon("ConnectCheck_Player-" + e.player.id, () -> {
      String name = Strings.stripGlyphs(Strings.stripColors(e.player.name)).strip();

      // fix the admin bug
      if (e.player.getInfo().admin) e.player.admin = true;

      // check if the nickname is empty without colors and emojis
      if (name.isBlank()) {
        e.player.kick(KickReason.nameEmpty);
        return;
      }

      // check the nickname of this player
      if (manager.BansManager.checkName(e.player, name)) return;

      // prevent to duplicate nicknames
      if (TempData.count(d -> d.stripedName.equals(name)) != 0) e.player.kick(KickReason.nameInUse);

      // check if player have a VPN
      if (util.AntiVpn.checkIP(e.player.ip())) {
        e.player.kick("[scarlet]VPN detected! []Please deactivate it to be able to connect to this server.");
        ALog.write("VPN", "VPN found on player @ [@]", name, e.player.uuid());
        return;
      }
    }));

    Events.on(EventType.PlayerJoin.class, e -> {
      TempData data = TempData.put(e.player); // add player in TempData

      // for me =)
      if (data.isCreator) {
        if (PVars.niceWelcome) Call.sendMessage("[scarlet]\ue80f " + data.realName + "[scarlet] has connected! \ue80f [lightgray](Everyone must say: Hello creator! XD)");
        Call.infoMessage(e.player.con, "Welcome creator! =)");
      }

      // unpause the game if one player is connected
      if (PVars.autoPause && Groups.player.size() == 1) {
        state.set(State.playing);
        Log.info("auto-pause: Game unpaused...");
        Call.sendMessage("[scarlet][Server]:[] Game unpaused...");
      }

      // mute the player if the player has already been muted
      if (PVars.recentMutes.contains(e.player.uuid())) data.isMuted = true;

      // apply the tag of this player
      data.applyTag();
    });

    Events.on(EventType.PlayerLeave.class, e -> {
      // pause the game if no one is connected
      if (PVars.autoPause && Groups.player.size() - 1 == 0) {
        state.set(State.paused);
        Log.info("auto-pause: Game paused...");
      }

      e.player.name = TempData.remove(e.player).realName; // remove player in TempData
    });

    Events.on(EventType.PlayerBanEvent.class, e -> 
      ALog.write("Ban", "@ [@] has been banned of server", netServer.admins.getInfoOptional(e.uuid).lastName, e.uuid)
    );

    // save the unit of the player for the godmode
    Events.on(EventType.UnitChangeEvent.class, e -> {
      TempData data = TempData.get(e.player);

      if (data != null) {
        if (data.inGodmode) {
          data.lastUnit.health = data.lastUnit.maxHealth;
          e.unit.health = Integer.MAX_VALUE;
        }
        data.lastUnit = e.unit;
      }
    });
  }

  public static CommandsRegister setHandler(CommandHandler handler) {
    Threads.daemon("CommandManagerWaiter-" + handler.getPrefix(), () -> {
      try {
        Thread.sleep(1000);
        CommandsManager.load(handler);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    return new CommandsRegister(handler);
  }

  public static class CommandsRegister {
    public CommandHandler handler;

    private CommandsRegister(CommandHandler handler) {
      this.handler = handler;
    }

    public void add(String name, String params, String desc, boolean forAdmin, boolean inThread, CommandRunner<TempData> runner) {
      if (forAdmin) PVars.adminCommands.add(name);

      this.handler.<Player>register(name, params, desc, (arg, player) -> {
        if (forAdmin && !Players.adminCheck(player)) return;

        if (inThread)
          Threads.daemon("ClientCommandRunner_" + player.toString() + "-" + name, () -> runner.accept(arg, TempData.get(player)));
        else
          Timer.schedule(() -> {
            try { runner.accept(arg, TempData.get(player)); } 
            catch (Exception e) {
              Log.err("Exception in Timer \"ClientCommandRunner_" + player.toString() + "-" + name + "\"");
              e.printStackTrace();
            }
          }, 0);
      });

    }

    public void add(String name, String desc, Cons<String[]> runner) {
      add(name, "", desc, runner);
    }

    public void add(String name, String params, String desc, Cons<String[]> runner) {
      this.handler.register(name, params, desc, (arg) -> Timer.schedule(() -> {
        try { runner.get(arg); } 
        catch (Exception e) {
          Log.err("Exception in Timer \"ServerCommandRunner_" + name + "\"");
          e.printStackTrace();
        }
      }, 0));
    }
  }
}
