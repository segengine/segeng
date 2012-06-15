package SegEngine.seg;

/**
* @file WordInSen.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2012
* @version 1.0.0 
* @brief 
*  
**/

public class WordInSen {
	private int begin;          //!< the beginning position of a word in the sentence
	private int len;            //!< the length of the word
	private TTreeNode node;     //!< the ternary tree node corresponding to the word
	private boolean selected;   //!< whether the word is selected as the final segmentation result
	private int compNum;        //!< the number of words that conflict with this word
	private String pos;         //!< the final POS of the word in the sentence

	public WordInSen(int b, int l, TTreeNode n) {
		this.begin = b;
		this.len = l;
		this.node = n;
		this.selected = true;
		this.compNum = 0;
		this.pos = null;
	}

	public void setSelected(boolean s) {
		this.selected = s;
	}

	public int getBegin() {
		return this.begin;
	}

	public void setBegin(int b) {
		this.begin = b;
	}

	public int getLen() {
		return this.len;
	}

	public void setLen(int l) {
		this.len = l;
	}

	public void setNode(TTreeNode n) {
		this.node = n;
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void incCompnum() {
		this.compNum++;
	}

	public int getCompnum() {
		return this.compNum;
	}

	public TTreeNode getNode() {
		return this.node;
	}

	public String getPos() {
		return this.pos;
	}

	public void setPos(String s) {
		this.pos = new String(s);
	}
}
