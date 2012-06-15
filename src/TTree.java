package SegEngine.seg;

/**
* @file TTree.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

import java.util.ArrayList;

public class TTree {
	TTreeNode root;    //!< the root node of the tree
	TTreeNode terminal;//!< the node corresponding to the recent added word

	public TTree() {
		root = null;
	}

	/**
     * @brief insert a word (or part of it) into the tree
     * @param fa the father node of the current node where substring of the word will insert
     * @param node the current node where substring of the word will insert
     * @param word the entire string that should be inserted
     * @param index the start position of the substring of the word
     * @param flag the property of the word
     * @return the node storing the ith (indicated by index) character of the word
     * @note when the inserting process finish, terminal will record the node corresponding the word 
     */
	public TTreeNode insertWord(TTreeNode fa, TTreeNode node, String word,
			int index, long flag) {
		if (node == null) {
			node = new TTreeNode(word.charAt(index++));
			if (this.root == null)
				this.root = node;
			node.father = fa;
			if (index >= word.length()) {
				node.isTerminal = true;
				node.flag = flag;
				this.terminal = node;
			} else {
				node.middle = insertWord(node, node.middle, word, index, flag);
			}
		} else {
			if (node.c > word.charAt(index)) {
				node.left = insertWord(fa, node.left, word, index, flag);
			} else if (node.c < word.charAt(index)) {
				node.right = insertWord(fa, node.right, word, index, flag);
			} else {
				if (++index >= word.length()) {
					node.isTerminal = true;
					node.flag = flag;
					this.terminal = node;
				} else {
					node.middle = insertWord(node, node.middle, word, index,
							flag);
				}
			}
		}
		return node;
	}

	/**
     * @brief find all substrings of sentence that start from the begin and are stored in the tree
     * @param sentence the source string
     * @param begin the start position in the sentence
     * @return all qualified substrings (words)
     */
	public ArrayList<WordInSen> findMatchedWords(String sentence, int begin) {
		ArrayList<WordInSen> result = new ArrayList<WordInSen>();
		String sen = sentence.substring(begin);
		TTreeNode curNode = root;
		int curIndex = 0;
		while (curNode != null && curIndex < sen.length()) {
			if (curNode.c > sen.charAt(curIndex)) {
				curNode = curNode.left;
			} else if (curNode.c < sen.charAt(curIndex)) {
				curNode = curNode.right;
			} else {
				if (curNode.isTerminal) {
					WordInSen word = new WordInSen(begin, curIndex + 1, curNode);
					result.add(word);
					if (begin > 0 && sentence.charAt(begin-1) < 255 && sentence.charAt(begin) < 255)
						return result;
				}
				curNode = curNode.middle;
				curIndex++;
			}
		}
		return result;
	}

	/**
     * @brief find all substrings of sentence that start from the begin and are stored in the tree
     * @param sentence the source string
     * @param begin the start position in the sentence
     * @param result all qualified substrings (words)
     * @return whether the process is successful
     */
	public boolean findMatchedWords(ArrayList<WordInSen> result, String sentence, int begin) {
		TTreeNode curNode = root;
		int curIndex = begin;
		while (curNode != null && curIndex < sentence.length()) {
			if (curNode.c > sentence.charAt(curIndex)) {
				curNode = curNode.left;
			} else if (curNode.c < sentence.charAt(curIndex)) {
				curNode = curNode.right;
			} else {
				if (curNode.isTerminal) {
					WordInSen word = new WordInSen(begin, curIndex + 1, curNode);
					result.add(word);
					if (begin > 0 && sentence.charAt(begin-1) < 255 && sentence.charAt(begin) < 255)
						return true;
				}
				curNode = curNode.middle;
				curIndex++;
			}
		}
		return true;
	}

	/**
     * @brief delete word from the tree 
     * @param cur the start node of the search process
     * @param word the string (or substring) to be search
     * @param index the start position of the word
     */
	public void DeleteWord(TTreeNode cur, String word, int index) {
		if (cur == null || word.length() <= index) return;
		char c = word.charAt(index);
		if (cur.c < c) this.DeleteWord(cur.right, word, index);
		else if (cur.c > c) this.DeleteWord(cur.left, word, index);
		else {
			if (index == word.length()-1) {
				cur.isTerminal = false;
				return;
			}
			else this.DeleteWord(cur.middle, word, index+1);
		}
	}
	
	/**
     * @brief find the corresponding node of a word in the tree  
     * @param str the word
     * @return the node corresponding to the word or null (not found)
     */
	public TTreeNode string2Node(String str) {
		if (null == str || 0 == str.length()) return null;
		TTreeNode node = this.root;
		int index = 0;
		while (node != null) {
			if (node.c == str.charAt(index)) {
				if (index == str.length() - 1) {
					if (node.isTerminal)
						return node;
					else
						return null;
				} else {
					index++;
					node = node.middle;
				}
			} else if (node.c < str.charAt(index))
				node = node.right;
			else
				node = node.left;
		}
		return null;
	}

	//!<get the string of given node
	/**
     * @brief get the string of given node 
     * @param node the given node
     * @return the corresponding string
     */
	public String node2String(TTreeNode node) {
		if (node == null || !node.isTerminal)
			return "";
		String result = "";
		while (node != null) {
			result = node.c + result;
			node = node.father;
		}
		return result;
	}

	/**
     * @brief show all the words stored in the tree (or subtree) 
     * @param node the start node of the show process
     * @param pre the substring that has been explored before the current node is visited
     * @note This function is only used for debug
     */
	public void ShowTree(TTreeNode node, String pre) {
		if (node == null)
			return;
		String str = pre + node.c;
		if (node.isTerminal)
			System.out.println(str);
		ShowTree(node.left, pre);
		ShowTree(node.middle, str);
		ShowTree(node.right, pre);
	}
}

/*
 * node of ternary tree
 */
class TTreeNode {
	char c;//!<character
	long flag;//!<property of the word
	int fre;//!<word frequency
	PosDistribution pd;//!<POS distribution
	String weight;//!<weight of word
	String hier_str;//!<hierarchical segmentation result
	float[] classScore;//!<scores for each class
	String[] code;//!<code of the word
	TTreeNode left;
	TTreeNode right;
	TTreeNode middle;
	TTreeNode father;
	boolean isTerminal;//!<whether the node is the end of a word
	boolean isSinglePOS;//!<whether the word has only one POS

	public TTreeNode(char arg) {
		this.c = arg;
		this.flag = 0;
		this.fre = 0;
		this.pd = null;
		this.weight = null;
		this.hier_str = null;
		this.classScore = null;
		this.code = null;
		this.left = null;
		this.right = null;
		this.middle = null;
		this.father = null;
		this.isTerminal = false;
		this.isSinglePOS = false;
	}

	public boolean isHierarchical() {
		return ((this.flag & Const.IS_HIERARCHICAL) != 0);
	}

	public boolean hasCode() {
		return ((this.flag & Const.HAS_CODE) != 0);
	}

	public boolean isNegative() {
		return ((this.flag & Const.IS_NEGATIVE) != 0);
	}

	public boolean isCHLastName() {
		return ((this.flag & Const.IS_CH_LASTNAME) != 0);
	}

	public boolean isCH2ndName() {
		return ((this.flag & Const.IS_CH_2_NAME) != 0);
	}

	public boolean isCH3rddName() {
		return ((this.flag & Const.IS_CH_3_NAME) != 0);
	}

	public boolean isJLastName() {
		return ((this.flag & Const.IS_J_LASTNAME) != 0);
	}

	public boolean isJ2ndName() {
		return ((this.flag & Const.IS_J_2_NAME) != 0);
	}

	public boolean isJ3rdName() {
		return ((this.flag & Const.IS_J_3_NAME) != 0);
	}

	public boolean isJ4thName() {
		return ((this.flag & Const.IS_J_4_NAME) != 0);
	}

	public boolean isWest1stName() {
		return ((this.flag & Const.IS_WEST_1_NAME) != 0);
	}

	public boolean isWestFollowName() {
		return ((this.flag & Const.IS_WEST_F_NAME) != 0);
	}

	public boolean isLongWord() {
		return ((this.flag & Const.IS_LONG_WORD) != 0);
	}

	public boolean isTimeEnd() {
		return ((this.flag & Const.IS_TIME_END) != 0);
	}

	public boolean isPlaceEnd() {
		return ((this.flag & Const.IS_PLACE_END) != 0);
	}

	public boolean isNamePrefix() {
		return ((this.flag & Const.IS_NAME_PREFIX) != 0);
	}

	public boolean isBetweenName() {
		return ((this.flag & Const.IS_BETWEEN_NAME) != 0);
	}

	public boolean isStrongWord() {
		return ((this.flag & Const.IS_STRONG_WORD) != 0);
	}

	public boolean isStrongPrefix() {
		return ((this.flag & Const.IS_STRONG_PREFIX) != 0);
	}

	public boolean isVerbOrTime() {
		return ((this.flag & Const.IS_VERBORTIME) != 0);
	}

	public boolean isNameSuffix() {
		return ((this.flag & Const.IS_NAME_SUFFIX) != 0);
	}

	public boolean isName() {
		return ((this.flag & Const.IS_NAME) != 0 || (this.flag & Const.IS_ORIGIN_NAME) != 0);
	}

	public boolean notName() {
		return ((this.flag & Const.IS_NOT_NAME) != 0);
	}
	
	public boolean isM() {
		return ((this.flag & Const.IS_M) != 0);
	}

	public boolean isQ() {
		return ((this.flag & Const.IS_Q) != 0);
	}

	public boolean isAWord() {
		return ((this.flag & Const.IS_A_WORD) != 0);
	}

	public boolean isNX() {
		return ((this.flag & Const.IS_NX) != 0);
	}

	public boolean isSingleName() {
		return ((this.flag & Const.IS_SINGLENAME) != 0);
	}
	
	public boolean isOrgName() {
		return ((this.flag & Const.IS_ORG_NAME) != 0);
	}

	public boolean containChLastName() {
		return ((this.flag & Const.CONTAIN_CH_LASTNAME) != 0);
	}
	
	public boolean isJ() {
		return ((this.flag & Const.IS_J) != 0);
	}
	
	public boolean isNSend() {
		return ((this.flag & Const.IS_NS_END) != 0);
	}
	
	public boolean isStrongSuffix() {
		return ((this.flag & Const.IS_STRONG_SUFFIX) != 0);
	}
	
	public boolean isPositive() {
		return ((this.flag & Const.IS_POSITIVE) != 0);
	}
	
	public boolean isFinal() {
		return ((this.flag & Const.IS_FINAL) != 0);
	}

	public boolean isMQL() {
		return ((this.flag & Const.IS_MQL) != 0);
	}

	public boolean isOrgEnd() {
		return ((this.flag & Const.IS_ORG_END) != 0);
	}

	public boolean isOrgExclude() {
		return ((this.flag & Const.IS_ORG_EXCLUDE) != 0);
	}

	public boolean isAfterName() {
		return ((this.flag & Const.IS_AFTER_NAME) != 0);
	}

	public boolean isPreName() {
		return ((this.flag & Const.IS_PRE_NAME) != 0);
	}

	public boolean isOriginName() {
		return ((this.flag & Const.IS_ORIGIN_NAME) != 0);
	}

	public long getFlag() {
		return flag;
	}

	public void setFlag(long flag) {
		this.flag = flag;
	}
}