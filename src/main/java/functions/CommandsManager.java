package functions;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.CommandHandler;


public class CommandsManager {
	private static ObjectMap<String, Boolean> commands = new ObjectMap<>(), temp = new ObjectMap<>();
	private static volatile boolean canLoad = false;
	public final String name;
	public final boolean isActivate;

	private CommandsManager(String name, boolean isActivate) {
		this.name = name;
		this.isActivate = isActivate;
	}
	
	public static Boolean get(String name) {
		return commands.get(name);
	}

	public static boolean set(String name, boolean value) {
		return commands.put(name, value);
	}
	
	public static Seq<CommandsManager> copy() {
		Seq<CommandsManager> copy = new Seq<>();
		commands.forEach(command -> copy.add(new CommandsManager(command.key, command.value)));
		return copy;
	}
	
	public static void save() {
		StringBuilder builder = new StringBuilder();

		if (!commands.isEmpty())
			commands.forEach(command -> {
				builder.append(command.key + " - ");
				if (command.value) builder.append(1 + " | ");
				else builder.append(0 + " | ");
			});
		else builder.append("");
		
		Core.settings.put("handlerManager", builder.toString());
		Core.settings.forceSave();
	}
	
	public static void update(CommandHandler handler) {
		commands.forEach(command -> {
			if (command.value != temp.get(command.key)) {
				handler.removeCommand("host");
				
				handler.register("host", "[mapname] [mode]", "Open the server. Will default to survival and a random map if not specified.", arg -> {
					arc.util.Log.warn("Changes have been made. Please restart the server for them to take effect. (tip: write 'exit' to shut down the server)");
				});
				return;
			}
		});
	}

	public static void load(CommandHandler handler, boolean isClient) {
		while (!canLoad) {}
		
		handler.getCommandList().forEach(command -> {
			if (!commands.containsKey(handler.getPrefix() + command.text)) 
				commands.put(handler.getPrefix() + command.text, true);
		});
		
		Seq<String> list = (isClient ? commands.keys().toSeq().filter(c -> c.startsWith(handler.getPrefix())) : commands.keys().toSeq().filter(c -> !c.startsWith(mindustry.Vars.netServer.clientCommands.getPrefix()))),
				comparator = handler.getCommandList().map(command -> command.text);
		
		commands.forEach(command -> { 
			if (!command.value) handler.removeCommand(command.key.substring(handler.getPrefix().length()));
		});
		
		comparator.each(c -> list.remove(handler.getPrefix() + c));
		list.each(c -> commands.remove(c));
		
		temp.putAll(commands);
		save();
	}
	
	public static void init() {
		String content = Core.settings.has("handlerManager") ? Core.settings.getString("handlerManager") : "";

		if (!content.equals("")) {
			String[] temp;
			
			for (String line : content.split(" \\| ")) {
				temp = line.split(" \\- ");
				
				if (temp.length == 2) {
					if (temp[1].equals("1")) commands.put(temp[0], true);
					else commands.put(temp[0], false);
				}
			}
		} else save();
		
		canLoad = true;
	}
	
	public static void clearSave() {
		Core.settings.put("handlerManager", "");
		Core.settings.forceSave();
	}
}
