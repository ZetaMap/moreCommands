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
  private static Seq<String> bannedClients = new Seq<String>().addAll("VALVE", "tuttop", "CODEX", "IGGGAMES","IgruhaOrg", "FreeTP.Org"),
    bannedIps = new Seq<>(),
    bannedNames = new Seq<>();

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

          saveSettings();
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
          saveSettings();

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
            saveSettings();
            break;

          default: Log.info("Confirmation canceled ...");
        }
        PVars.unbanConfirm = false;
        break;

      default: Log.err("Invalid arguments.");
    }
  }

  public static void blacklistCommand(String[] arg) {
    Seq<String> list = new Seq<String>().addAll("[server]", "server", FilterType.prefix, "~").addAll(bannedClients);

    switch (arg[0]) {
      case "list":
        StringBuilder builder = new StringBuilder();

        if (arg[1].equals("name")) {
          int best = Strings.bestLength(bannedNames);
          int max = best > 18 + String.valueOf(bannedNames.size).length() ? best + 4 : 23 + String.valueOf(bannedNames.size).length();

          Log.info("List of banned names:");
          Log.info(Strings.lJust("| Custom list: Total: " + bannedNames.size, max) + "  Default list: Total: " + list.size);
          for (int i = 0; i < Math.max(bannedNames.size, list.size); i++) {
            try { builder.append(Strings.lJust("| | " + bannedNames.get(i), max + 1)); } 
            catch (IndexOutOfBoundsException e) { builder.append("|" + Strings.repeat(" ", max)); }
            try { builder.append(" | " + list.get(i)); } 
            catch (IndexOutOfBoundsException e) {}

            Log.info(builder.toString());
            builder = new StringBuilder();
          }

        } else if (arg[1].equals("ip")) {
          int best = Strings.bestLength(bannedIps);
          int max = best > 18 + String.valueOf(bannedIps.size).length() ? best + 4 : 23 + String.valueOf(bannedIps.size).length();

          Log.info("List of banned ip:");
          Log.info(Strings.lJust("| Custom list: Total: " + bannedIps.size, max) + "  Default list: Total: " + AntiVpn.vpnServersList.size() + " (Anti VPN list)");
          for (int i = 0; i < Math.max(bannedIps.size, AntiVpn.vpnServersList.size()); i++) {
            try { builder.append(Strings.lJust("| | " + bannedIps.get(i), max + 1)); } 
            catch (IndexOutOfBoundsException e) {
              builder.append("|" + Strings.repeat(" ", max));
              if (i > 20) break;
            }
            try {
              if (i == 20) builder.append(" | ...." + (AntiVpn.vpnServersList.size() - i) + " more");
              else if (i < 20) builder.append(" | " + AntiVpn.vpnServersList.get(i));
            } catch (IndexOutOfBoundsException e) {}

            Log.info(builder.toString());
            builder = new StringBuilder();

            if (i > 20 && bannedIps.size < 20) break;
          }

        } else Log.err("Invalid argument. possible arguments: name, ip");
        break;

      case "add":
        if (arg.length == 3) {
          if (arg[1].equals("name")) {
            if (arg[2].length() > 40) Log.err("A nickname cannot exceed 40 characters");
            else if (bannedNames.contains(arg[2])) Log.err("'@' is already in the blacklist", arg[2]);
            else {
              bannedNames.add(arg[2]);
              saveSettings();
              Log.info("'@' was added to the blacklist", arg[2]);
            }

          } else if (arg[1].equals("ip")) {
            if (arg[2].split("\\.").length != 4 || !Strings.canParseByteList(arg[2].split("\\."))) Log.err("Incorrect format for IPv4");
            else if (bannedIps.contains(arg[2])) Log.err("'@' is already in the blacklist", arg[2]);
            else {
              bannedIps.add(arg[2]);
              saveSettings();
              Log.info("'@' was added to the blacklist", arg[2]);
            }

          } else Log.err("Invalid argument. possible arguments: name, ip");
        } else Log.err("Please enter a value");
        break;

      case "remove":
        if (arg.length == 3) {
          if (arg[1].equals("name")) {
            if (!bannedNames.contains(arg[2])) Log.err("'@' isn't in custom blacklist", arg[2]);
            else if (list.contains(arg[2])) Log.err("You can't remove a name from the default list");
            else {
              bannedNames.remove(arg[2]);
              saveSettings();
              Log.info("'@' has been removed from the blacklist", arg[2]);
            }

          } else if (arg[1].equals("ip")) {
            if (arg[2].split("\\.").length != 4 || !Strings.canParseByteList(arg[2].split("\\."))) Log.err("Incorrect format for IPv4");
            else {
              bannedIps.remove(arg[2]);
              saveSettings();
              Log.info("'@' has been removed from the blacklist", arg[2]);
            }

          } else Log.err("Invalid argument. possible arguments: name, ip");
        } else Log.err("Please enter a value");
        break;

      case "clear":
        if (arg[1].equals("name")) {
          bannedNames.clear();
          saveSettings();
          Log.info("Name blacklist emptied!");

        } else if (arg[1].equals("ip")) {
          bannedIps.clear();
          saveSettings();
          Log.info("IP blacklist emptied!");

        } else Log.err("Invalid argument. possible arguments: name, ip");
        break;

      default: Log.err("Invalid argument. possible arguments: list, add, remove");
    }
  }

  public static boolean checkName(mindustry.gen.Player player, String name) {
    boolean kicked = true;
    String message = "";

    if (name.startsWith(FilterType.prefix) ||
        name.startsWith("~"))
      message = "[scarlet]Your nickname must not start by [orange]" + FilterType.prefix + "[scarlet] or [orange] ~";
    else if (name.length() < 3) message = "[scarlet]Your nickname must be at least 3 characters long";
    else if (bannedNames.contains(name) ||
        name.toLowerCase().equals("[server]") ||
        name.toLowerCase().equals("server"))
      message = "[scarlet]This nickname is banned!";
    else if (!player.admin &&
        netServer.admins.getAdmins().contains(p -> {
          String adminName = Strings.stripGlyphs(Strings.stripColors(p.lastName)).strip().toLowerCase();
          return adminName.contains(name.toLowerCase()) || name.toLowerCase().contains(adminName);
        }))
      message = "[scarlet]Spoofing an admin name is prohibited! [lightgray](even if not entirely)";
    else if (bannedClients.contains(name)) message = "Ingenuine copy of Mindustry.\n\nMindustry is free on: [royal]https://anuke.itch.io/mindustry[]\n";
    else if (bannedIps.contains(player.con.address)) message = "[scarlet]Your IP is blacklisted. [lightgray](ip: " + player.ip() + ")";
    else kicked = false;

    if (kicked) {
      player.kick(message);
      util.ALog.write("Check", "Connection refused of @ [@] for reason: @", player.name, player.uuid(), message);
    }

    return kicked;
  }

  @SuppressWarnings("unchecked")
  public static void load() {
    try {
      if (Core.settings.has("bannedNamesList")) bannedNames = Core.settings.getJson("bannedNamesList", Seq.class, Seq::new);
      else saveSettings();

      if (Core.settings.has("bannedIpsList")) bannedIps = Core.settings.getJson("bannedIpsList", Seq.class, Seq::new);
      else saveSettings();

    } catch (Exception e) { saveSettings(); }
  }

  public static void saveSettings() {
    Core.settings.putJson("bannedNamesList", bannedNames);
    Core.settings.putJson("bannedIpsList", bannedIps);
  }
}
