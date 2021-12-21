package util;


public class Strings extends arc.util.Strings {
	public static String rJust(String str, int newLenght) { return rJust(str, newLenght, " "); }
	public static String rJust(String str, int newLenght, String filler) {
		if (filler.length() >= str.length() + newLenght) return str;
	
		while (str.length() < newLenght) str = filler + str;
		return str;
	}
	
	public static String lJust(String str, int newLenght) { return lJust(str, newLenght, " "); }
	public static String lJust(String str, int newLenght, String filler) {
		if (filler.length() >= str.length() + newLenght) return str;
		
		while (str.length() < newLenght) str += filler;
		return str;
	}
	
	public static String mJust(String left, String right, int newLenght) { return mJust(left, right, newLenght, " "); }
	public static String mJust(String left, String right, int newLenght, String filler) {
		if (filler.length() >= left.length() + right.length() + newLenght) return left + right;

		while (left.length() + right.length() < newLenght) left += filler;
		return left + right;
	}
	
	public static String createSpaces(int length) {
    	String spaces = "";
    	for (int i=0; i<length; i++) spaces+=" ";
    	return spaces;
    }
	
	public static int bestLength(Iterable<? extends String> list) {
		int best = 0;

    	for (String i : list) {
    		if (i.length() > best) best = i.length();
    	}

    	return best;
    }
	
	public static String RGBString(String str, int hue) {
    	String out = "";
    	
    	for (char c : str.toCharArray()) {
    		if (hue < 360) hue+=10;
    		else hue = 0;
    		
    		out += (c == '[' ? "[[#" : "[#") + Integer.toHexString(java.awt.Color.getHSBColor(hue / 360f, 1f, 1f).getRGB()).substring(2) + "]" + c;
    	}
    	
        return out;
    }
	
	public static boolean canParseByteList(String[] list) {
		for (String str : list) 
			if (!canParseInt(str) || Integer.parseInt(str) > 255) 
				return false;
		
		return true;
	}
	
	public static String stripGlyphs(CharSequence str){
        StringBuilder out = new StringBuilder();
        int c;

        for(int i=0; i<str.length(); i++){
            c = str.charAt(i);
            if(c >= 0xE000 && c <= 0xF8FF) continue;
            out.append((char) c);
        }

        return out.toString();
	}
	
	public static boolean choiseOn(String str) {
		switch (str) {
			case "on": case "true": case "1": return true;
			default: return false;
		}
	}
	
	public static boolean choiseOff(String str) {
		switch (str) {
			case "off": case "false": case "0": return true;
			default: return false;
		}
	}
	
}
