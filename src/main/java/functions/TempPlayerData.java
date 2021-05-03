package functions;

import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("serial")
public class TempPlayerData implements Serializable {
	public static HashMap<String, TempPlayerData> tempPlayerDatas = new HashMap<>(); // uuid, data
	public Integer hue;
	public String realName;
	public String nameNotSpace;
	public String nameNotColor;
	public int id;

	public TempPlayerData(Integer hue, String name, int id){
        this.hue = hue;
        this.realName = name;
        this.nameNotSpace = name.replaceAll("\\s+", "_");
        this.nameNotColor = name.replaceAll("\\[", "[[");
        this.id = id;
    }
    public void setHue(int i) {
    	this.hue = i; 
    }
}

