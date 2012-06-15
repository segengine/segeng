package SegEngine.seg;

/**
* @file Occurrence.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

public class Occurrence {
	String phase;
	String POS;
	int startPos;
	int endPos;
	int documentID;
	int paragraphID;
	int sentenceID;
	
	public Occurrence() {
		this.phase = "";
		this.POS = "";
		this.startPos = -1;
		this.endPos = -1;
		this.documentID = -1;
		this.paragraphID = -1;
		this.sentenceID = -1;
	}
}
