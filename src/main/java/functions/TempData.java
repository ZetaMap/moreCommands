package functions;

import arc.struct.ObjectMap;
import arc.util.Strings;

import mindustry.gen.Player;

public class TempData {
	private static ObjectMap<Player, TempData> tempData = new ObjectMap<>(); // uuid, data
	public Integer hue;
	public String realName;
	public String normalizedName;

	private TempData(int hue, Player p){
        this.hue = hue;
        this.realName = p.name;
        this.normalizedName = Strings.stripColors(p.name);
    }
	
	public void setHue(int i) {
    	this.hue = i; 
    }
	
	public static TempData get(Player p) {
		return tempData.get(p);
	}
	
    public static TempData put(Player p) {
    	return tempData.put(p, new TempData(0, p));
    }
    
    public static void remove(Player p) {
    	tempData.remove(p);
    }

    public static boolean contains(Player p) {
    	return tempData.containsKey(p);
    }
    
    @SuppressWarnings("unlikely-arg-type")
	public static boolean equals(Player p) {
    	return tempData.equals(p);
    }
    
    public static boolean isEmpty() {
    	return tempData.isEmpty();
    }
}
