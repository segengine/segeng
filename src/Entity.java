package SegEngine.seg;

/**
* @file Entity.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

import java.util.ArrayList;

public class Entity {
	String title;
	String id;
	String type;
	int frequency;
	double score;
	ArrayList<EntityOccurrence> entityOccurrences;
	
	public Entity() {
		this.title = "";
		this.id = "";
		this.type = "";
		this.frequency = -1;
		this.score = -1;
		this.entityOccurrences = null;
	}
}
