package SegEngine.seg;

/**
* @file EntityOccurrence.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
* 
**/

public class EntityOccurrence {
	String title;
	String type;
	double score;
	int docID;
	int textStartLocation;
	int textEndLocation;
	
	public EntityOccurrence() {
		this.title = "";
		this.type = "";
		this.score = 0;
		this.docID = -1;
		this.textStartLocation = -1;
		this.textEndLocation = -1;
	}
}
