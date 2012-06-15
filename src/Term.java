package SegEngine.seg;

/**
* @file Term.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

public class Term {
	public String word;
	public String pos;
	public String ID;
	public String posOrNeg;
	public String hier;
	public TTreeNode n;
	public String weight;
	public float confidence;
	
	public Term(String s1, String s2, String s3, String s4, String s5,
			TTreeNode n, String w, float c) {
		this.word = s1;
		this.pos = s2;
		this.ID = s3;
		this.posOrNeg = s4;
		this.hier = s5;
		this.n = n;
		this.weight = w;
		this.confidence = c;
	}
}
