package manager;

import static mindustry.Vars.netServer;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.gen.Call;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;

import data.PVars;
import data.TempData;
import util.ALog;
import util.AntiVpn;
import util.Strings;
import filter.FilterType;

public class BansManager {
  private static Seq<String> bannedClients = Seq.with("VALVE", "tuttop", "CODEX", "IGGGAMES","IgruhaOrg", "FreeTP.Org");

  public static void bansCommand(String[] arg) {
    if (PVars.unbanConfirm && !arg[0].equals("reset")) {
      Log.info("Confirmation canceled ...");
      PVars.unbanConfirm = false;
      return;
    }

    switch (arg[0]) {
      case "list":
        Seq<PlayerInfo> bans = netServer.admins.getBanned();
        Seq<String> ipbans = netServer.admins.getBannedIPs();

        if (bans.isEmpty()) Log.info("No ID-banned players have been found.");
        else {
          Log.info("Banned players [ID]: Total: " + bans.size);
          bans.each(info -> Log.info("| @ - Last name: '@' - Reason: @", info.id, info.lastName, PVars.bansReason.get(info.id, "<unknown>")));
        }

        if (ipbans.isEmpty()) Log.info("No IP-banned players have been found.");
        else {
          Log.info("Banned players [IP]: Total: " + ipbans.size);
          ipbans.each(ip -> {
            Seq<PlayerInfo> infos = netServer.admins.findByIPs(ip);

            if (infos.isEmpty()) Log.info("| + '@' (No name or info)", ip);
            else {
              Log.info("| + '@'", ip);
              infos.each(info -> Log.info("| | Last name: '@' - ID: '@' - Reason: @", info.lastName, info.id, PVars.bansReason.get(info.id, "<unknown>")));
            }
          });
        }
        break;

      case "add":
        if (arg.length > 2) {
          if (arg[1].equals("id")) {
            netServer.admins.banPlayerID(arg[2]);
            PVars.bansReason.put(arg[2], arg.length == 4 && !arg[3].isBlank() ? arg[3] : "<no_reason>");
            Log.info("ID banned for the reason: @", PVars.bansReason.get(arg[2]));
            ALog.write("Ban", "[Server] banned the id '@' for the reason: @", arg[2], PVars.bansReason.get(arg[2]));

          } else if (arg[1].equals("ip")) {
            netServer.admins.banPlayerIP(arg[2]);
            netServer.admins.findByIPs(arg[2]).each(info -> PVars.bansReason.put(info.id, arg.length == 4 && !arg[3].isBlank() ? arg[3] : "<no_reason>"));
            Log.info("IP banned for the reason: @", PVars.bansReason.get(arg[2]));
            ALog.write("Ban", "[Server] banned the ip '@' for the reason: @", arg[2], PVars.bansReason.get(arg[2]));

          } else {
            Log.err("Invalid type.");
            return;
          }

          TempData.each(d -> {
            if (netServer.admins.isIDBanned(d.player.uuid())) {
              Call.sendMessage("\n[gold]--------------------\n[scarlet]/!\\ " + d.nameColor + d.realName
                  + "[scarlet] has been banned of the server.\nReason: [white]" + PVars.bansReason.get(arg[2])
                  + "\n[gold]--------------------\n");
              ALog.write("Ban", "[Server] banned @ [@] for the reason: @", d.stripedName, d.player.uuid(), PVars.bansReason.get(arg[2]));
              if (arg.length == 3 || arg[3].isBlank()) d.player.kick(KickReason.banned);
              else d.player.kick("You are banned on this server!\n[scarlet]Reason: []" + PVars.bansReason.get(arg[2]));
            }
          });

        } else Log.err("Please specify a type and value! Example: ban add id abcdefghijkAAAAA012345==");
        break;

      case "remove":
        if (arg.length > 2) {
          if (netServer.admins.unbanPlayerID(arg[2])) {
            PlayerInfo info = netServer.admins.getInfoOptional(arg[2]);

            Log.info("Unbanned player @ [@]", info.lastName, info.id);
            ALog.write("Unban", "[Server] unbanned @ [@]", info.lastName, info.id);
            PVars.bansReason.remove(info.id);

          } else if (netServer.admins.unbanPlayerIP(arg[2])) {
            netServer.admins.findByIPs(arg[2]).each(info -> PVars.bansReason.remove(info.id));
            Log.info("IP unbanned");
            ALog.write("Unban", "[Server] unbanned the ip @", arg[2]);

          } else {
            Log.err("That IP/ID is not banned!");
            return;
          }

        } else Log.err("Please specify a type and value! Example: ban remove ip 0.0.0.0");
        break;

      case "reset":
        if (arg.length > 1 && !PVars.unbanConfirm) {
          Log.err("Use first: 'ban reset', before confirming the command.");
          return;
        } else if (!PVars.unbanConfirm) {
          Log.warn("Are you sure to unban all all IP and ID? (ban reset <y|n>)");
          PVars.unbanConfirm = true;
          return;
        } else if (arg.length == 1 && PVars.unbanConfirm) {
          Log.warn("Are you sure to unban all all IP and ID? (ban reset <y|n>)");
          PVars.unbanConfirm = true;
          return;
        }

        switch (arg[1]) {
          case "y": case "yes":
            netServer.admins.getBanned().each(unban -> {
              netServer.admins.unbanPlayerID(unban.id);
              PVars.bansReason.remove(unban.id);
            });
            netServer.admins.getBannedIPs().each(ip -> netServer.admins.unbanPlayerIP(ip));
            Log.info("All IP and ID have been unbanned!");
            ALog.write("Unban", "ALL IP AND ID HAVE BEEN UNBANNED!");
            break;

          default: Log.info("Confirmation canceled ...");
        }
        PVars.unbanConfirm = false;
        break;

      default: Log.err("Invalid arguments.");
    }
  }

  public static boolean checkName(mindustry.gen.Player player, String name) {
    boolean kicked = true;
    String message = "";

    if (name.startsWith(FilterType.prefix) ||
        name.startsWith("~"))
      message = "[scarlet]Your nickname must not start by [orange]" + FilterType.prefix + "[scarlet] or [orange] ~";
    else if (name.length() < 3) message = "[scarlet]Your nickname must be at least 3 characters long";
    else if (name.toLowerCase().equals("[server]") ||
             name.toLowerCase().equals("server"))
      message = "[scarlet]This nickname is banned!";
    else if (!player.admin &&
        netServer.admins.getAdmins().contains(p -> {
          String adminName = Strings.stripGlyphs(Strings.stripColors(p.lastName)).strip().toLowerCase();
          return adminName.contains(name.toLowerCase()) || name.toLowerCase().contains(adminName);
        }))
      message = "[scarlet]Spoofing an admin name is prohibited! [lightgray](even if not entirely)";
    else if (bannedClients.contains(name)) message = "Ingenuine copy of Mindustry.\n\nMindustry is free on: [royal]https://anuke.itch.io/mindustry[]\n";
    else kicked = false;

    if (kicked) {
      player.kick(message);
      util.ALog.write("Check", "Connection refused of @ [@] for reason: @", player.name, player.uuid(), message);
    }

    return kicked;
  }
}
