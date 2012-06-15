package SegEngine.seg;

/**
* @file NTTree.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief
*  
**/

import java.util.HashSet;
import java.util.ArrayList;

public class NTTree {
	NTTreeNode root;           //!< the root node of the tree
	NTTreeNode terminal;       //!< the node of current new word
	HashSet<NTerm> newTerms;   //!< the set of new words
	int nodeNum;               //!< the number of nodes in this ternary tree
	
	/**
     * @brief constructor of this class, initial all variables
     */
	public NTTree() {
		this.root = null;
		this.newTerms = new HashSet<NTerm>();
		this.terminal = null;
		nodeNum = 0;
	}
	
	/**
     * @brief insert a word in the phrase (word sequence) to the dictionary tree
     * @param fa the father node of the insert position
     * @param node the node where the word is inserted
     * @param words the word sequence to be inserted
     * @param poses the pos tags of the word sequence
     * @param index indicate which word in the word sequence will be inserted in current process
     * @return the node where the current word is inserted
     */
	public NTTreeNode insertWord(NTTreeNode fa, NTTreeNode node, 
			ArrayList<String> words, ArrayList<String> poses, int index) {
		if (words == null || poses.get(index).compareTo("w") == 0 || words.size() <= 1) return null;
//		if (words == null || words.size() <= 1) return null;
		if (node == null) {
			node = new NTTreeNode(words.get(index), poses.get(index));
			this.nodeNum++;
			index++;
			if (this.root == null) this.root = node;
			node.father = fa;
			if (index >= words.size()) {
				this.terminal = node;
			}
			else {
				node.middle = insertWord(node, node.middle, words, poses, index);
			}
		}
		else {
			if (node.s.compareTo(words.get(index)) > 0) {
				node.left = insertWord(fa, node.left, words, poses, index);
			}
			else if (node.s.compareTo(words.get(index)) < 0) {
				node.right = insertWord(fa, node.right, words, poses, index);
			}
			else {
				node.fre++;
				if (++index >= words.size()) {
					this.terminal = node;
				}
				else {
					node.middle = insertWord(node, node.middle, words, poses, index);
				}
			}
		}
		return node;
	}
		
	/**
     * @brief find all words in the dictionary tree
     * @param n the root node of subtree to be searched
     * @param pre the prefix(word with pos) of the subtree
     * @param pre1 the prefix(word without pos) of the subtree
     * @param preLen the length of prefix of the subtree
     */
	public void findAllWords(NTTreeNode n, String pre, String pre1, int preLen) {
		if (n == null) return;
		this.findAllWords(n.left, pre, pre1, preLen);
		this.findAllWords(n.right, pre, pre1, preLen);
		if (n.pos.compareTo("w") == 0) return;
		pre += n.s+"/"+n.pos+" ";
		pre1 += n.s;
//		preLen += n.s.length();
		preLen ++;
		if (preLen > 1) {
    		NTerm t = new NTerm(pre, pre1, n.fre, preLen);
    		this.newTerms.add(t);
		}
		this.findAllWords(n.middle, pre, pre1, preLen);
	}
	
	
	
	/**
     * @brief get the number of nodes in the tree
     * @return the number of nodes in the tree
     */
	public int getNodeNum() {
		return nodeNum;
	}

	/**
     * @brief clear all words whose frequency is below threshold
     * @param threshold the predefined threshold
     */
	public void clearResult(int threshold) {
		int c1= 0;
		for (NTerm t: this.newTerms) {
			if (t.fre < threshold ) {
				t.selected = false;
			}
		}
		for (NTerm t1: this.newTerms) {
			c1++;
//			System.out.println(c1);
			if (!t1.selected) {
				continue;
			}
			for (NTerm t2: this.newTerms) {
				if (t2.selected == false || t2 == t1) continue;
				if (t2.word.contains(t1.word) && t2.fre == t1.fre) {
					t1.selected = false;
					break;
				}
			}
		}
	}
	
}

class NTTreeNode {
	String s;
	String pos;
//	long flag;
	int fre;
	NTTreeNode left;
	NTTreeNode right;
	NTTreeNode middle;
	NTTreeNode father;
//	boolean isTerminal;
	
	public NTTreeNode(String arg, String arg1) {
		s = arg;
		pos = arg1;
//		flag = 0;
		fre = 1;
		left = null;
		right = null;
		middle = null;
		father = null;
//		isTerminal = false;
	}
	
}

class NTerm {
	String word;
	String word1;
	int len;
	int fre;
	boolean selected;
	
	public NTerm (String w, String w1, int f, int len) {
		this.word = w;
		this.word1 = w1;
		this.fre = f;
		this.selected = true;
		this.len = len;
		
	}
}