package SegEngine.seg;

/**
* @file termEntry.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

public class termEntry {
	int type;
	String term;
	String property;
	
	public termEntry(String t) {
		this.type = -1;
		this.term = t;
		this.property = "";
	}
}
