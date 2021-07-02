package functions;

import arc.struct.ObjectMap;
import mindustry.gen.Player;


public class TempData {
	private static ObjectMap<Player, TempData> tempData = new ObjectMap<>(); // uuid, data
	public final String realName;
	public final String normalizedName;
	public Integer hue;
	
	private TempData(int hue, Player p){
        this.hue = hue;
        this.realName = p.name;
        this.normalizedName = arc.util.Strings.stripColors(p.name);
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
