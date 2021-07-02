package functions;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.Seq;


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
	
	public static void update() {
		commands.forEach(command -> {
			if (command.value != temp.get(command.key)) {
				arc.util.Log.warn("Changes have been made, the server will shut down in 10 seconds for them to be applied.");
				try { Thread.sleep(10000); } 
				catch (InterruptedException e) { Core.app.exit(); }
				Core.app.exit();
				return;
			}
		});
	}

	public static void load(arc.util.CommandHandler handler, boolean isServer) {
		while (!canLoad) {}
		
		handler.getCommandList().forEach(command -> {
			if (!commands.containsKey((isServer ? "" : "/") + command.text)) 
				commands.put((isServer ? "" : "/") + command.text, true);
		});
		save();
		
		commands.forEach(command -> { 
			if (!command.value) {
				if (command.key.startsWith("/")) handler.removeCommand(command.key.substring(1));
				else handler.removeCommand(command.key); 
			}	
		});
		temp.putAll(commands);
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
}
