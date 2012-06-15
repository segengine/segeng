package SegEngine.seg;

/**
* @file Seg.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;

import org.apache.commons.lang.StringUtils;

import englishseg.Seg_English;

public class Seg{

	/**
	 * @param args
	 */


	// private static Log log = LogFactory.getLog(Seg.class);
	private Dic myDic; //!<dictionary
	private Viterbi myViterbi;//!<viterbi for pos tagging
	private Seg_English mySegEn;//!<for English tagging
	private String sen;//!<sentence to be segmented

	boolean findOrg;
	boolean findNsUnit;
	boolean verifyName;
	boolean findNewWord;
	boolean seg_English;

	private HashMap<TTreeNode, ArrayList<Float>> word2score;//!< score of each class
	private HashMap<TTreeNode, String> word2weight;//!< TFIDF weight
	private HashMap<String, Integer> newWord2fre;
	private ArrayList<WordInSen> words;//!< all words in current sentence
	private ArrayList<comp> compList;//!< all competition pairs in current sentence
	private HashMap<Integer, Integer> compCount;//!< competition number of each word
	private HashMap<String, nameInfo> name2fre;//!< new person name
	private float[] classScore;
	private String[] className = {"電子類", "體育類", "金融投資類", "汽車類", "教育類",
			"旅遊類", "醫療衛生", "時尚", "无法判断类别"};

	private String[] notname = {"更"};
	HashSet<String> enPos;

	private ArrayList<Term> segResult;
	private float[][] observeMatrix;//!< pos distribution matrix for current sentence

	/**
     * @brief constructor of Seg class
     * @param dic the dictionary used for segmentation 
     */
	public Seg(Dic dic) {
		this.seg_English = true;
		if (this.seg_English) {
			System.out.print("Loading English Segmentation Resources......");
			this.mySegEn = new Seg_English();
			System.out.println("Done");
		}
		this.myDic = dic;
		this.myViterbi = new Viterbi(60, myDic.transMatrix);
	}

	/**
     * @brief reset all parameters used for segmenting new sentence s
     * @param s the sentence to be segmented
     */
	private void reset(String s) {
		this.sen = new String(s);
		this.segResult = new ArrayList<Term>();
		this.name2fre = new HashMap<String, nameInfo>(); 
		this.newWord2fre = new HashMap<String, Integer>();
		this.words = new ArrayList<WordInSen>();
		this.compList = new ArrayList<comp>();
		this.compCount = new HashMap<Integer, Integer>();
		this.enPos = new HashSet<String>();
		this.enPos.add("nx");
		this.word2score = new HashMap<TTreeNode, ArrayList<Float>>();
		this.word2weight = new HashMap<TTreeNode, String>();
		this.word2weight.put(null, "0");
		this.classScore = new float[8];	
	}

	/**
     * @brief find all legal words in the sentence and store them in the word set
     */
	private void findAllWords(int findName,	int findOrg, int findPlace) {
		this.words.clear();
		int curIndex = 0;
		while (curIndex < this.sen.length()) {
			int k = this.words.size();
			ArrayList<WordInSen> ws = null;
			// additional dictionary first
			ws = myDic.additional_word_dic.findMatchedWords(this.sen, curIndex);
			if (ws.size() == 0) {
				ws = myDic.word_dic.findMatchedWords(this.sen, curIndex);
				if (ws.size() == 0) {
					WordInSen word = new WordInSen(curIndex, 1, null);
					word.setPos("?");
					this.words.add(word);
				}
				else {
					for (int i = 0; i < ws.size(); i++) {
						this.words.add(ws.get(i));
					}
				}
			} else {
				for (int i = 0; i < ws.size(); i++) {
					this.words.add(ws.get(i));
				}
			}
			if (-1 == findName) {//find name
				int len = 0;
				len = this.findCName(curIndex, this.sen.length());
				if (len > 0) {
					int i = k;
					while (i < this.words.size() && this.words.get(i).getLen() != len) i++;
					if (i == this.words.size()) {
						WordInSen word = new WordInSen(curIndex, len, null);
						word.setPos("nrc");
						this.words.add(word);
					}
//					else {
//						this.words.get(i).setPos("nrc");
//					}
				}
				len = this.findJName(curIndex, this.sen.length());
				if (len > 0) {
					int i = k;
					while (i < this.words.size() && this.words.get(i).getLen() != len) i++;
					if (i == this.words.size()) {
						WordInSen word = new WordInSen(curIndex, len, null);
						word.setPos("nrj");
						this.words.add(word);
					}
//					else {
//						this.words.get(i).setPos("nrj");
//					}
				}
				len = this.findWName(curIndex, this.sen.length());
				if (len > 0) {
					int i = k;
					while (i < this.words.size() && this.words.get(i).getLen() != len) i++;
					if (i == this.words.size()) {
						WordInSen word = new WordInSen(curIndex, len, null);
						word.setPos("nrf");
						this.words.add(word);
					}
//					else {
//						this.words.get(i).setPos("nrf");
//					}
				}
			}
			if (-1 == findOrg) {//find organization name
				
			}
			if (-1 == findPlace) {//find place name
				
			}
			curIndex++;
		}
	}
	
	/**
     * @brief find all possible Chinese names in current sentence
     * @param index the start position to find possible Chinese name
     * @param endIndex the end position of current sentence
     * @return the length of Chinese name, 0 for find nothing
     */
	private int findCName(int index, int endIndex) {
		int result = 0;
		if (index+2 > endIndex) return result;
		String s1 = sen.substring(index, index+1);
		TTreeNode n1 = this.myDic.word_dic.string2Node(s1);
		if (null != n1 && n1.isCHLastName()) {
			String s2 = sen.substring(index+1, index+2);
			TTreeNode n2 = this.myDic.word_dic.string2Node(s2);
			if (null != n2 && n2.isCH2ndName()) {
				result = 2;
				if (index+3 <= endIndex) {
					String s3 = sen.substring(index+2, index+3);
					TTreeNode n3 = this.myDic.word_dic.string2Node(s3);
					if (null != n3 && n3.isCH3rddName()) {
						result = 3;
					}					
				}
			}
			else if (null != n2 && n2.isCHLastName() && index+4 <= endIndex) {//double last name
				String s3 = sen.substring(index+2, index+3);
				TTreeNode n3 = this.myDic.word_dic.string2Node(s3);
				String s4 = sen.substring(index+3, index+4);
				TTreeNode n4 = this.myDic.word_dic.string2Node(s4);
				if (null != n3 && n3.isCH2ndName() && null != n4 && n4.isCH3rddName())
					result = 4;
			}
		}
		else if (index+3 <= endIndex){
			s1 = sen.substring(index, index+2);
			n1 = this.myDic.word_dic.string2Node(s1);
			if (null != n1 && n1.isCHLastName()) {
				String s2 = sen.substring(index+2, index+3);
				TTreeNode n2 = this.myDic.word_dic.string2Node(s2);
				if (null != n2 && n2.isCH2ndName()) {
					result = 3;
					if (index+4 <= endIndex) {
						String s3 = sen.substring(index+3, index+4);
						TTreeNode n3 = this.myDic.word_dic.string2Node(s3);
						if (null != n3 && n3.isCH3rddName()) {
							result = 4;
						}					
					}
				}
			}
		}
		return result;
	}
	
	/**
     * @brief find all possible Japanese names in current sentence
     * @param index the start position to find possible Japanese name
     * @param endIndex the end position of current sentence
     * @return the length of Japanese name, 0 for find nothing
     */
	private int findJName(int index, int endIndex) {
		int result = 0;
		if (index+3 > endIndex) return result;
		String s1 = sen.substring(index, index+2);
		TTreeNode n1 = this.myDic.word_dic.string2Node(s1);
		if (null != n1 && n1.isJLastName()) {
			String s2 = sen.substring(index+2, index+3);
			TTreeNode n2 = this.myDic.word_dic.string2Node(s2);
			if (null != n2 && n2.isJ2ndName()) {
				result = 3;
				if (index+4 <= endIndex) {
					String s3 = sen.substring(index+3, index+4);
					TTreeNode n3 = this.myDic.word_dic.string2Node(s3);
					if (null != n3 && n3.isJ3rdName()) {
						result = 4;
						if (index+5 <= endIndex) {
							String s4 = sen.substring(index+4, index+5);
							TTreeNode n4 = this.myDic.word_dic.string2Node(s4);
							if (null != n4 && n4.isJ4thName()) {
								result = 5;
							}							
						}
					}					
				}
			}
		}
		return result;
	}
	
	/**
     * @brief find all possible west names in current sentence
     * @param index the start position to find possible west name
     * @param endIndex the end position of current sentence
     * @return the length of west name, 0 for find nothing
     */
	private int findWName(int index, int endIndex) {
		int result = 0;
		if (index+2 > endIndex) return result;
		String s1 = sen.substring(index, index+1);
		TTreeNode n1 = this.myDic.word_dic.string2Node(s1);
		if (null != n1 && n1.isWest1stName()) {
			result++;
			s1 = sen.substring(index+result, index+result+1);
			n1 = this.myDic.word_dic.string2Node(s1);
			while(null != n1 && n1.isWestFollowName()) {
				result++;
				if (index+result == endIndex) break;
				s1 = sen.substring(index+result, index+result+1);
				n1 = this.myDic.word_dic.string2Node(s1);
			}
		}
		if (result > 1) return result;
		result = 0;
		if (index+3 > endIndex) return result;
		s1 = sen.substring(index, index+2);
		n1 = this.myDic.word_dic.string2Node(s1);
		if (null != n1 && n1.isWest1stName()) {
			result += 2;
			s1 = sen.substring(index+result, index+result+1);
			n1 = this.myDic.word_dic.string2Node(s1);
			while(null != n1 && n1.isWestFollowName()) {
				result++;
				if (index+result == endIndex) break;
				s1 = sen.substring(index+result, index+result+1);
				n1 = this.myDic.word_dic.string2Node(s1);
			}
		}
		if (result > 2) return result;
		return 0;
	}

	/**
     * @brief show all words found by "findAllWords", this function is only used for debugging
     */
	private void showAllWords() {
		System.out.println(this.sen);
		for (WordInSen word : this.words) {
			System.out.println(this.sen.substring(word.getBegin(), word.getBegin() + word.getLen()));
		}
	}

	/**
     * @brief find all word pair that conflict with each other
     */
	private void findCompetition() {
		this.compList.clear();
		this.compCount.clear();
		for (int i = 0; i < this.words.size(); i++) {
			WordInSen w1 = this.words.get(i);
			TTreeNode n1 = w1.getNode();
//			if (n1 != null && n1.isCH3rddName()) continue;
			int end1 = w1.getBegin() + w1.getLen();
//			String s1 = this.sen.substring(w1.getBegin(), end1);//!<for debug
			if (!w1.isSelected())
				continue;
			for (int j = i + 1; j < this.words.size(); j++) {
				WordInSen w2 = this.words.get(j);
				TTreeNode n2 = w2.getNode();
//				if (n2 != null && n2.isCHLastName()) continue;
				int end2 = w2.getBegin() + w2.getLen();
//				String s2 = this.sen.substring(w2.getBegin(), end2);
				if (!w2.isSelected())
					continue;
				if (end1 <= w2.getBegin())
					break;
				//!< later word cover former word that has only one or two characters
				if (w1.getBegin() == w2.getBegin() 
//						&& w1.getLen() == 1
						&& (w1.getLen() == 1 || w1.getLen() == 2)
//						&& !n1.isStrongWord()
//						&& !n1.isStrongPrefix()
//						&& !n1.isTimeEnd()
				) {
					w1.setSelected(false);
					break;
				}
				//!< former word cover later word that has only one character
//				else if (w1.getLen() != 1 && w2.getLen() == 1 ) {
//					w2.setSelected(false);
//					continue;
//				} 
				else if (w1.getBegin() < w2.getBegin() && end1 >= end2 && w2.getLen() <= 2) {
					w2.setSelected(false);
					continue;
				}
				else if (end1 > w2.getBegin()) {
					if (end1 >= end2 && n1 != null
							&& (n1.isHierarchical() || //!< has hierarchical segmentation
							n1.hasCode() || //!< has code
							1 == w2.getLen() ||		//!<has only one character
							n1.isLongWord())) {//!< long word
						w2.setSelected(false);
					} 
					else if (n1 != null && !n1.isStrongWord() &&
							(w1.getBegin() == w2.getBegin()
							&& w1.getLen() < w2.getLen() && n2 != null
							&& (n2.isHierarchical() || //!< has hierarchical segmentation
									n2.hasCode() || //!< has code
							n2.isLongWord()))) {//!< long word
						w1.setSelected(false);
						break;
					} else {
						this.compList.add(new comp(i, j));
						int count;
						if (this.compCount.containsKey(i)) {
							count = this.compCount.get(i) + 1;
							this.compCount.put(i, count);
						} else
							this.compCount.put(i, 1);
						if (this.compCount.containsKey(j)) {
							count = this.compCount.get(j) + 1;
							this.compCount.put(j, count);
						} else
							this.compCount.put(j, 1);
					}
				}
			}
		}
	}

	/**
     * @brief show all competitions that are found by "findCompetition", this function is used
     * 		only for debugging
     */
	private void showAllComps() {
		System.out.println(this.sen);
		for (comp c : this.compList) {
			WordInSen w = this.words.get(c.i);
			String s1 = this.sen.substring(w.getBegin(), w.getBegin()
					+ w.getLen());
			w = this.words.get(c.j);
			String s2 = this.sen.substring(w.getBegin(), w.getBegin()
					+ w.getLen());
			System.out.println(s1 + "|" + s2);
		}
	}

	/**
     * @brief find the winner of each conflicting pair
     */
	private void solveComp() {
		for (comp c : this.compList) {
			WordInSen w1 = this.words.get(c.i);
			int end1 = w1.getBegin() + w1.getLen();
			String s1 = this.sen.substring(w1.getBegin(), end1);
			WordInSen w2 = this.words.get(c.j);
			int end2 = w2.getBegin() + w2.getLen();
			String s2 = this.sen.substring(w2.getBegin(), end2);
			TTreeNode n1, n2;
			n1 = w1.getNode();
			n2 = w2.getNode();
			if (!w1.isSelected() && !w2.isSelected())
				continue;
			else if (!w1.isSelected() && w2.isSelected()) {
				int count = this.compCount.get(c.j) - 1;
				this.compCount.put(c.j, count);
				continue;
			}
			else if (w1.isSelected() && !w2.isSelected()) {
				int count = this.compCount.get(c.i) - 1;
				this.compCount.put(c.i, count);
				continue;
			}
			if (n1 != null && n1.isAWord()) {
				w2.setSelected(false);
				int count = this.compCount.get(c.j) + 1;
				this.compCount.put(c.j, count);
				count = this.compCount.get(c.i) - 1;
				this.compCount.put(c.i, count);
				continue;
			}
			else if (n2 != null && n2.isAWord()) {
				w1.setSelected(false);
				int count = this.compCount.get(c.i) + 1;
				this.compCount.put(c.i, count);
				count = this.compCount.get(c.j) - 1;
				this.compCount.put(c.j, count);
				continue;
			}
			
			//!< simple conflict examination
			String conf = s1 + "|" + s2;
			boolean b1 = false, b2 = false;
			TTreeNode confN = myDic.conf_dic.string2Node(conf);
			if (confN != null) {//
				String winner = confN.weight;
				if (winner.compareTo("0") == 0) {
					w2.setSelected(false);
					w1.setSelected(true);
				} else {
					w1.setSelected(false);
					w2.setSelected(true);
				}
				continue;
			}
			
			//
			// conflict number examination
			if (this.compCount.get(c.i) > this.compCount.get(c.j)) {
				w1.setSelected(false);
				int count = this.compCount.get(c.i) + 1;
				this.compCount.put(c.i, count);
				count = this.compCount.get(c.j) - 1;
				this.compCount.put(c.j, count);
				continue;
			} else if (this.compCount.get(c.i) < this.compCount.get(c.j)) {
				w2.setSelected(false);
				int count = this.compCount.get(c.j) + 1;
				this.compCount.put(c.j, count);
				count = this.compCount.get(c.i) - 1;
				this.compCount.put(c.i, count);
				continue;
			} else if (n1 != null && n1.isOrgName() && n2 != null && !n2.isOrgName()) {
				w2.setSelected(false);
				int count = this.compCount.get(c.j) + 1;
				this.compCount.put(c.j, count);
				count = this.compCount.get(c.i) - 1;
				this.compCount.put(c.i, count);
				continue;
			} else if (n1 != null && !n1.isOrgName() && n2 != null && n2.isOrgName()) {
				w1.setSelected(false);
				int count = this.compCount.get(c.i) + 1;
				this.compCount.put(c.i, count);
				count = this.compCount.get(c.j) - 1;
				this.compCount.put(c.j, count);
				continue;
			} else if (w1.getBegin() < w2.getBegin() && end1 < end2) {//!<交叉型
				String s11 = this.sen
						.substring(w1.getBegin(), w2.getBegin());
				String s21 = this.sen.substring(end1, end2);
				TTreeNode n11 = myDic.word_dic.string2Node(s11);
				TTreeNode n21 = myDic.word_dic.string2Node(s21);
				if (n11 == null && n21 != null) {
					w2.setSelected(false);
					continue;   
				} else if (n11 != null && n21 == null) {
					w1.setSelected(false);
					continue;
				} else if (n11 != null && n21 != null) {
					if (n21.isStrongSuffix()) {
						w2.setSelected(false);
						continue;
					}
					if (n11.isStrongPrefix()) {
						w1.setSelected(false);
						continue;
					}
					if (n21.isStrongWord()) {
						w2.setSelected(false);
						continue;
					}
					if (n11.isStrongWord()) {
						w1.setSelected(false);
						continue;
					}

					if (n21.isM()) {
						w2.setSelected(false);
						continue;
					}
					if (n11.isM()) {
						w1.setSelected(false);
						continue;
					}

//					if (w1.getLen() == 3 && w2.getLen() == 2 &&
//							end1-w2.getBegin() == 1) {
//						w1.setSelected(false);
//						continue;
//					}
					
					if (w1.getLen() * s21.length() < w2.getLen() * s11.length()) {
						w1.setSelected(false);
						continue;
					}
					
					int f1 = n11.fre;
					int f2 = n21.fre;
					if (f1 > f2) {
						w1.setSelected(false);
						continue;
					} else if (f1 < f2) {
						w2.setSelected(false);
						continue;
					}	
					
					int score1 = 0;
					int score2 = 0;
					if (n11.isStrongWord()) score1 += 2;
					if (n11.isVerbOrTime()) score1 += 1;
					if (n21.isStrongWord()) score2 += 2;
					if (n21.isVerbOrTime()) score2 += 1;
					if (score1 > score2) {
						w1.setSelected(false);
						continue;
					}
					if (score1 < score2) {
						w2.setSelected(false);
						continue;
					}
					if (s11.length() > s21.length()) {
						w1.setSelected(false);
						continue;
					} else if (s11.length() < s21.length()) {
						w2.setSelected(false);
						continue;
					} 
//					else if (f1 > f2) {
//						w1.setSelected(false);
//						continue;
//					} else if (f1 < f2) {
//						w2.setSelected(false);
//						continue;
//					}
				}
			}
			//!< organization examination
			if (n1 != null
					&& ((n1.flag & Const.IS_HIERARCHICAL) != 0 || (n1.flag & Const.HAS_CODE) != 0)
					&& (n2 == null || (n2.flag & Const.IS_HIERARCHICAL) == 0
							&& (n2.flag & Const.HAS_CODE) == 0)) {
				w2.setSelected(false);
				continue;
			} else if (n2 != null
					&& ((n2.flag & Const.IS_HIERARCHICAL) != 0 || (n2.flag & Const.HAS_CODE) != 0)
					&& (n1 == null || (n1.flag & Const.IS_HIERARCHICAL) == 0
							&& (n1.flag & Const.HAS_CODE) == 0)) {
				w1.setSelected(false);
				continue;
			}
			//!< recent word examination
//			if (this.isRecentWord(n1) && !this.isRecentWord(n2)) {
//				w2.setSelected(false);
//				continue;
//			} else if (!this.isRecentWord(n1) && this.isRecentWord(n2)) {
//				w1.setSelected(false);
//				continue;
//			}
			//!< inclusion examination
			if (w1.getBegin() <= w2.getBegin()
					&& w1.getBegin() + w1.getLen() >= w2.getBegin()
							+ w2.getLen()) {
				w2.setSelected(false);
				continue;
			} else if (w1.getBegin() >= w2.getBegin()
					&& w1.getBegin() + w1.getLen() <= w2.getBegin()
							+ w2.getLen()) {
				w1.setSelected(false);
				continue;
			}
			//!< simple/complex conflict examination
			confN = myDic.conf_dic.string2Node(conf);
			if (confN != null) {//!< simple conflict
				String winner = confN.weight;
				if (winner.compareTo("0") == 0) {
					w2.setSelected(false);
				} else {
					w1.setSelected(false);
				}
			} else {//!< complex conflict
				//!< w1
				if (!myDic.conf_next_list.keySet().contains(n1))
					w1.setSelected(false);
				else {
					b1 = true;
					int begin = w1.getBegin() + w1.getLen();
					boolean found = false;
					ArrayList<TTreeNode> nextList = myDic.conf_next_list.get(n1);
					//!< compare and find common
					for (WordInSen word : this.words) {//!< all next words
						if (begin != word.getBegin())
							continue;
						TTreeNode node = word.getNode();
						for (TTreeNode n : nextList) {
							if (node == n)
								found = true;
						}
					}
					if (!found)
						w1.setSelected(false);
				}
				//!< w2
				if (!myDic.conf_pre_list.keySet().contains(n2))
					w2.setSelected(false);
				else {
					b2 = true;
					boolean found = false;
					ArrayList<TTreeNode> preList = myDic.conf_pre_list.get(n2);
					//!< compare and find common
					for (WordInSen word : this.words) {//!< all next words
						int end = word.getBegin() + word.getLen();
						if (end != w2.getBegin())
							continue;
						TTreeNode node = word.getNode();
						for (TTreeNode n : preList) {
							if (node == n)
								found = true;
						}
					}
					if (!found)
						w2.setSelected(false);
				}
			}
		}
	}

	/**
     * @brief show all the words survived after competition, this function is only used for debugging
     */
	private void showAllLeftWords() {
		System.out.println(this.sen);
		for (WordInSen word : this.words) {
			if (word.isSelected()) {
				System.out.print(this.sen.substring(word.getBegin(), word
						.getBegin()
						+ word.getLen())
						+ " ");
			}
		}
		System.out.println();
	}

	/**
     * @brief reorganize word after competition and form final segmentation result
     */
	private void getResult() {
		int begin = 0;
		int preWordIndex = -1;
		boolean needNewWord = false;
		//!< for (WordInSen word: this.words) {
		for (int j = 0; j < this.words.size(); j++) {
			WordInSen word = this.words.get(j);
			if (!word.isSelected())
				continue;//!< loser, skip
			if (word.getBegin() == begin) {//!< winner, select
				begin += word.getLen();
				preWordIndex = j;
				continue;
			} else if (word.getBegin() < begin) {
				word.setSelected(false);
				continue;
			} else {// word.getBegin() > begin
				needNewWord = true;
				if (preWordIndex != -1) {
					int k = preWordIndex + 1;
					for (; k < j; k++) {
						if (this.words.get(k).getBegin() > this.words.get(
								preWordIndex).getBegin())
							break;
						if (this.words.get(k).getBegin() == this.words.get(
								preWordIndex).getBegin()
								&& (this.words.get(k).getBegin() + this.words
										.get(k).getLen()) == word.getBegin()) {
							this.words.get(k).setSelected(true);
							this.words.get(preWordIndex).setSelected(false);
							needNewWord = false;
						}
					}
				}
			}
			//!< empty position that need to be filled
			// System.out.println("++++++++++++"+begin);
			// word.getBegin()>begin
			if (needNewWord) {
				int b = begin;
				int dis = word.getBegin() - b;
				int smallIndex;
//				if (preWordIndex == -1)
//					smallIndex = -1;
//				else
					smallIndex = preWordIndex;
				while (b < word.getBegin()) {
					boolean found = false;
					for (int i = j - 1; i > smallIndex; i--) {
						if (this.words.get(i).getBegin() == b
								&& this.words.get(i).getLen() <= dis) {
							this.words.get(i).setSelected(true);
							b += this.words.get(i).getLen();
							dis -= this.words.get(i).getLen();
							smallIndex = i;
							found = true;
							break;
						}
					}
					if (!found) {
						this.words.get(smallIndex+1).setBegin(b);
						this.words.get(smallIndex+1).setLen(1);
						this.words.get(smallIndex+1).setNode(null);
						this.words.get(smallIndex+1).setPos("?");
						this.words.get(smallIndex+1).setSelected(true);
						b++;
						dis--;
						smallIndex++;
					}
				}
				needNewWord = false;
			}
			begin = word.getBegin() + word.getLen();
		}
		// System.out.println("_________"+begin);
		while (begin < this.sen.length()) {
			for (int i = this.words.size() - 1; i >= 0; i--) {
				if (this.words.get(i).getBegin() == begin) {
					this.words.get(i).setSelected(true);
					// System.out.println("<<<"+begin);
					// System.out.println(this.sen.substring(begin,
					// begin+this.words.get(i).getLen()));
					// System.out.println(">>>");
					begin += this.words.get(i).getLen();
					break;
				}
			}
		}
		for (int j = 0; j < this.words.size(); j++) {
			WordInSen word = this.words.get(j);
			TTreeNode n = word.getNode();
			if (!word.isSelected() || n == null)
				continue;//
			if (this.word2score.containsKey(n)) {
				int i = 0;
				for (float d: this.word2score.get(n)) {
					this.classScore[i++] += d;
				}
			}
		}
	}

	/**
     * @brief combine digital, English letters and time end
     */
	private void comb1() {
		String pos = null;
		int len = 0;
		ArrayList<Integer> saves = new ArrayList<Integer>();
		for (int i = 0; i < this.words.size(); i++) {
			WordInSen w = this.words.get(i);
			if (!w.isSelected()) continue;
			TTreeNode n = w.getNode();
			if (pos == null) {
				if (n != null && n.isM()) {
					pos = "m";
					len = w.getLen();
					saves.add(i);
				} else if (n != null && n.isNX()) {
					pos = "nx";
					len = w.getLen();
					saves.add(i);					
				} 
			} else if (pos.compareTo("m") == 0) {
				if (n != null && n.isM()) {
					len += w.getLen();
					saves.add(i);
				} else if (n != null && n.isNX()) {
					pos = "nx";
					len += w.getLen();
					saves.add(i);					
				} else {
					if (n != null && n.isTimeEnd()) {
						pos = "t";
						len += w.getLen();
						w.setSelected(false);
						this.words.get(saves.get(0)).setNode(null);
					}
					else if (n != null && n.isQ() && !(len == 1 && 
							(this.sen.charAt(this.words.get(saves.get(0)).getBegin()) == '/' ||
							this.sen.charAt(this.words.get(saves.get(0)).getBegin()) == '许' ||
							this.sen.charAt(this.words.get(saves.get(0)).getBegin()) == '之'))) {
						pos = "mq";
						len += w.getLen();
						w.setSelected(false);
						this.words.get(saves.get(0)).setNode(null);
					}
					WordInSen w1 = this.words.get(saves.get(0));
					if (saves.size() > 1 && this.sen.charAt(this.words.get(saves.get(saves.size()-1)).getBegin()) == '又') {
						saves.remove(saves.size()-1);
						len--;
					}
					w1.setLen(len);
					if (len > 1) w1.setPos(pos);
					if (saves.size() > 1) this.words.get(saves.get(0)).setNode(null);
					for (int j = 1; j < saves.size(); j++) {
						this.words.get(saves.get(j)).setSelected(false);
					}
					saves.clear();
					pos = null;
					len = 0;
				}
			} else {//!<"nx"
				if (n != null && (n.isM() || n.isNX() || w.getLen() == 1 && this.sen.charAt(w.getBegin()) == '-')) {
					len += w.getLen();
					saves.add(i);
				} else {
					WordInSen w1 = this.words.get(saves.get(0));
					w1.setLen(len);
					w1.setPos(pos);
					for (int j = 1; j < saves.size(); j++) {
						this.words.get(saves.get(j)).setSelected(false);
					}
					saves.clear();
					pos = null;
					len = 0;
				}
			}
			
		}
		if (pos != null) {
			WordInSen w1 = this.words.get(saves.get(0));
			w1.setLen(len);
			if (len > 1) w1.setPos(pos);
			for (int j = 1; j < saves.size(); j++) {
				this.words.get(saves.get(j)).setSelected(false);
			}
		}
	}
	
	/**
     * @brief check whether the last name and its following character match
     */
	private boolean checkMatch(TTreeNode n1, TTreeNode n2) {
		if (n1 == null || n2 == null) return true;
		if (myDic.not_match_name_list.containsKey(n2) 
				&& myDic.not_match_name_list.get(n2).contains(n1)) return false;
		return true;
	}
	
	/**
     * @brief name recognition module
     * @param result segmentation result
     */
	private void findName(ArrayList<WordInSen> result) {
		if (result == null)
			return;
		result.clear();
		ArrayList<WordInSen> saves = new ArrayList<WordInSen>();
		int nameLen = 0;
		int nameBegin = 0;
		long flag = 0;
		int leftLen = 0;
		WordInSen name = null;//!<possible three-letter Chinese name
		WordInSen containName = null;//!<word that starts with Chinese last name
		boolean floatLast = false;
		TTreeNode c1Node = null;
		WordInSen w;
//		for (WordInSen w : this.words) {
		for (int wIndex = 0; wIndex < this.words.size(); wIndex++) {
			w = this.words.get(wIndex);
			if (!w.isSelected())
				continue;
			TTreeNode n = w.getNode();
			if (flag == 0) {
				if (n == null) {
					result.add(w);
					continue;
				}
				if (n.isNamePrefix()) {
					// for (WordInSen w1: saves) result.add(w1);
					// saves.clear();
					flag = Const.IS_NAME_PREFIX;
					saves.add(w);
				} else if (n.isCHLastName()) {
					// for (WordInSen w1: saves) result.add(w1);
					// saves.clear();
					flag = Const.IS_CH_LASTNAME;
					c1Node = n;
					saves.add(w);
					leftLen = 2;
				} else if (n.isJLastName()) {
					// for (WordInSen w1: saves) result.add(w1);
					// saves.clear();
					flag = Const.IS_J_LASTNAME;
					saves.add(w);
					leftLen = 3;
					c1Node = n;
				} else if (n.isWest1stName() || (n.isName()&&w.getLen()>3)) {
					nameBegin = w.getBegin();
					nameLen = w.getLen();
					flag = Const.IS_WEST_1_NAME;
					c1Node = n;
					name = w;//!<start of west name
				} else if (n.isSingleName()) {
//				} else if (this.isSingleName(w)) {
					for (WordInSen w1 : saves)
						result.add(w1);
					saves.clear();
					flag = Const.IS_CH_LASTNAME;
					saves.add(w);
					c1Node = n;
					leftLen = 1;
				} else {
					//!<organization recognition
					int lIndex = result.size()-1;
					if (lIndex < 0 || n == null) result.add(w);
					else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
							&&((result.get(lIndex).getPos() != null
							&&result.get(lIndex).getPos().startsWith("nr"))
							||(result.get(lIndex).getNode() != null
							&&(result.get(lIndex).getNode().isName()
							||result.get(lIndex).getNode().isWest1stName())))) {
						int len = result.get(lIndex).getLen();
						len += w.getLen();
						WordInSen newWord = new WordInSen(
								result.get(lIndex).getBegin(), len, null);
						if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
						result.set(lIndex, newWord);
					}
					else if (n.containChLastName()) {
						result.add(this.words.get(wIndex-1));
						flag = Const.IS_CH_LASTNAME;
						c1Node = this.words.get(wIndex+1).getNode();
						saves.add(this.words.get(wIndex+1));
						leftLen = 2;
						containName = w;
					}
					else  result.add(w);
					//
				}
			} else if (flag == Const.IS_WEST_1_NAME) {
				if (
						n == null && w.getPos() == null 
						|| n != null && n.isWestFollowName() 
						|| n != null && n.isWest1stName()
						|| n != null && (n.isWest1stName() && w.getLen() > 1)
//						|| n != null && (n.isWest1stName() && w.getLen() > 1)
						) {
					if (c1Node == null) {//!<not close to the start word, no matching examination required
    					nameLen += w.getLen();
    					name = null;
					}
					else {
						if (this.checkMatch(c1Node, n)) {//!<closely following the start word, matching examination is required
	    					nameLen += w.getLen();
	    					name = null;
						}
						else {
							result.add(name);
							name = null;
							flag = 0;
							nameLen = 0;
							//!<organization recognition
							int lIndex = result.size()-1;
							if (lIndex < 0 || n == null) result.add(w);
							else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
									&&((result.get(lIndex).getPos() != null
									&&result.get(lIndex).getPos().startsWith("nr"))
									||(result.get(lIndex).getNode() != null
									&&(result.get(lIndex).getNode().isName()
									||result.get(lIndex).getNode().isWest1stName())))) {
								int len = result.get(lIndex).getLen();
								len += w.getLen();
								WordInSen newWord = new WordInSen(
										result.get(lIndex).getBegin(), len, null);
								if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
								result.set(lIndex, newWord);
							}
							else if (n.containChLastName()) {
								result.add(this.words.get(wIndex-1));
								flag = Const.IS_CH_LASTNAME;
								c1Node = this.words.get(wIndex+1).getNode();
								saves.add(this.words.get(wIndex+1));
								leftLen = 2;
								containName = w;
							}
							else  result.add(w);
							//
						}
						c1Node = null;
					}
				} else {
					if (name == null) {//!<word sequence existing
						WordInSen newWord = new WordInSen(nameBegin, nameLen,
								null);
						newWord.setPos("nrf");
						result.add(newWord);
					} else {//!<only start word
						result.add(name);
						name = null;
					}
					flag = 0;
					nameBegin = 0;
					nameLen = 0;
					//!<organization recognition
					int lIndex = result.size()-1;
					if (lIndex < 0 || n == null) result.add(w);
					else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
							&&((result.get(lIndex).getPos() != null
							&&result.get(lIndex).getPos().startsWith("nr"))
							||(result.get(lIndex).getNode() != null
							&&(result.get(lIndex).getNode().isName()
							||result.get(lIndex).getNode().isWest1stName())))) {
						int len = result.get(lIndex).getLen();
						len += w.getLen();
						WordInSen newWord = new WordInSen(
								result.get(lIndex).getBegin(), len, null);
						if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
						result.set(lIndex, newWord);
					}
					else if (n.containChLastName()) {
						result.add(this.words.get(wIndex-1));
						flag = Const.IS_CH_LASTNAME;
						c1Node = this.words.get(wIndex+1).getNode();
						saves.add(this.words.get(wIndex+1));
						leftLen = 2;
						containName = w;
					}
					else  result.add(w);
					//!<
				}
			} else if (flag == Const.IS_NAME_PREFIX) {//!< pre+ch_l
				if (n != null && n.isCHLastName()) {
					WordInSen newWord = new WordInSen(saves.get(0).getBegin(),
							saves.get(0).getLen() + w.getLen(), null);
					newWord.setPos("nrcl");
					result.add(newWord);
					flag = 0;
					saves.clear();
				} else {
					result.add(saves.get(0));
					saves.clear();
					flag = 0;
					//!<organization recognition
					int lIndex = result.size()-1;
					if (lIndex < 0 || n == null) result.add(w);
					else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
							&&((result.get(lIndex).getPos() != null
							&&result.get(lIndex).getPos().startsWith("nr"))
							||(result.get(lIndex).getNode() != null
							&&(result.get(lIndex).getNode().isName()
							||result.get(lIndex).getNode().isWest1stName())))) {
						int len = result.get(lIndex).getLen();
						len += w.getLen();
						WordInSen newWord = new WordInSen(
								result.get(lIndex).getBegin(), len, null);
						if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
						result.set(lIndex, newWord);
					}
					else if (n.containChLastName()) {
						result.add(this.words.get(wIndex-1));
						flag = Const.IS_CH_LASTNAME;
						c1Node = this.words.get(wIndex+1).getNode();
						saves.add(this.words.get(wIndex+1));
						leftLen = 2;
						containName = w;
					}
					else  result.add(w);
					//!<
				}
			} else if (flag == Const.IS_CH_LASTNAME) {
				if (n == null) {//!<word not in dictionary, end the recognizing process
					if (containName != null) {
						int index = result.size() - 1;
						result.set(index, containName);
						containName = null;
					} else if (saves.size() == 1) {//!<only one word in saves
						if (name == null) {//!<no optional name
							result.add(saves.get(0));
						} else {//!<optional name exist (last name in saves is wrong)
							int index = result.size() - 1;
							result.set(index, name);
							name = null;
						}
					} else {//!<multiple elements in saves
						int len = 0;
						for (WordInSen ww : saves)
							len += ww.getLen();
						WordInSen newWord = new WordInSen(saves.get(0).getBegin(),
								len, null);
						if (saves.size() == 2 && saves.get(1).getNode().notName())//!<e.g. “刘家”
							newWord.setPos("n");
						else
							newWord.setPos("nrc");
						result.add(newWord);
					}
					saves.clear();
					flag = 0;
					//!<organization recognition
					int lIndex = result.size()-1;
					if (lIndex < 0 || n == null) result.add(w);
					else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
							&&((result.get(lIndex).getPos() != null
							&&result.get(lIndex).getPos().startsWith("nr"))
							||(result.get(lIndex).getNode() != null
							&&(result.get(lIndex).getNode().isName()
							||result.get(lIndex).getNode().isWest1stName())))) {
						int len = result.get(lIndex).getLen();
						len += w.getLen();
						WordInSen newWord = new WordInSen(
								result.get(lIndex).getBegin(), len, null);
						if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
						result.set(lIndex, newWord);
					}
					else if (n.containChLastName()) {
						result.add(this.words.get(wIndex-1));
						flag = Const.IS_CH_LASTNAME;
						c1Node = this.words.get(wIndex+1).getNode();
						saves.add(this.words.get(wIndex+1));
						leftLen = 2;
						containName = w;
					}
					else  result.add(w);
					//!<
				} else if ((!n.isCH2ndName()&&!n.isCH3rddName()&&!n.isName())
						&&n.isJLastName()) {//!<Japanese last name
					if (containName != null) {//!<e.g. "和黄"
						int index = result.size() - 1;
						result.set(index, containName);
						containName = null;
					} else if (saves.size() == 1) {//!<only one element in saves
						if (name == null) {//!<no optional names
							result.add(saves.get(0));
						} else {//!<optional name exist, e.g. “王国林”，王国is a word，林is a last name，
							//!<“王国” in saves，“王国林”exist in name as optional names
							int index = result.size() - 1;
							result.set(index, name);
							name = null;
						}
					} else {//!<multiple elements in saves
						int len = 0;
						for (WordInSen ww : saves)
							len += ww.getLen();
						WordInSen newWord = new WordInSen(saves.get(0).getBegin(),
								len, null);
						if (saves.size() == 2 && saves.get(1).getNode().notName())
							newWord.setPos("n");//!<e.g. 张家、李家 etc.
						else
							newWord.setPos("nrc");
						result.add(newWord);
					}
					saves.clear();
					flag = Const.IS_J_LASTNAME;
					saves.add(w);
					leftLen = 3;
				}else if ((!n.isCH2ndName()&&!n.isCH3rddName()&&!n.isName())
						&&(n.isWest1stName() || (n.isName()&&w.getLen()>2))) {
					//!<see explanation in Japanese name processing 
					if (containName != null) {
						int index = result.size() - 1;
						result.set(index, containName);
						containName = null;
					} else if (saves.size() == 1) {//!<
						if (name == null) {
							result.add(saves.get(0));
						} else {
							int index = result.size() - 1;
							result.set(index, name);
							name = null;
						}
					} else {
						int len = 0;
						for (WordInSen ww : saves)
							len += ww.getLen();
						WordInSen newWord = new WordInSen(saves.get(0).getBegin(),
								len, null);
						if (saves.size() == 2 && saves.get(1).getNode().notName())
							newWord.setPos("n");
						else
							newWord.setPos("nrc");
						result.add(newWord);
					}
					saves.clear();
					nameBegin = w.getBegin();
					nameLen = w.getLen();
					flag = Const.IS_WEST_1_NAME;
					c1Node = n;
					name = w;
				} else if (w.getLen() != 1) {//!< double letter word
					c1Node = null;
					if (w.getLen() == leftLen) {//!< double letter word with single last name: ch_1+ch_23
//						String str1 = this.sen.substring(w.getBegin(), w
//								.getBegin() + 1);
//						String str2 = this.sen.substring(w.getBegin() + 1, w
//								.getBegin() + 2);
//						TTreeNode node1 = this.word_dic.string2Node(str1);
//						TTreeNode node2 = this.word_dic.string2Node(str2);
						if (n != null && n.isNameSuffix()) {//!< name suffix
							if (name == null) {//!<e.g. 王某
								WordInSen newWord = new WordInSen(saves.get(0)
										.getBegin(), saves.get(0).getLen()
										+ leftLen, null);
								newWord.setPos("nrcr");
								result.add(newWord);
								floatLast = false;
								saves.clear();
								flag = 0;
								leftLen = 0;
								containName = null;
							} else {//!<e.g. 王茂林大爷
								saves.clear();
								int index = result.size() - 1;
								result.set(index, name);
								name = null;
								flag = 0;
								//!<organization recognition
								int lIndex = result.size()-1;
								if (lIndex < 0 || n == null) result.add(w);
								else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
										&&((result.get(lIndex).getPos() != null
										&&result.get(lIndex).getPos().startsWith("nr"))
										||(result.get(lIndex).getNode() != null
										&&(result.get(lIndex).getNode().isName()
										||result.get(lIndex).getNode().isWest1stName())))) {
									int len = result.get(lIndex).getLen();
									len += w.getLen();
									WordInSen newWord = new WordInSen(
											result.get(lIndex).getBegin(), len, null);
									if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
									result.set(lIndex, newWord);
								}
								else if (n.containChLastName()) {
									result.add(this.words.get(wIndex-1));
									flag = Const.IS_CH_LASTNAME;
									c1Node = this.words.get(wIndex+1).getNode();
									saves.add(this.words.get(wIndex+1));
									leftLen = 2;
									containName = w;
								}
								else  result.add(w);
								//!<
							}
						}
//						else if (n != null && n.isName() && !(n.isAfterName())) {//!<name with two letters, e.g. 桂花
						else if (n != null && n.isName() && !(n.isAfterName() && (name != null ||
								(result.size() > 0 && result.get(result.size()-1).getLen() == 2 &&
								(result.get(result.size()-1).getPos() != null &&
								result.get(result.size()-1).getPos().startsWith("nr") ||
								result.get(result.size()-1).getNode() != null &&
								result.get(result.size()-1).getNode().isOriginName()) || 
								saves.size() > 0 && saves.get(saves.size()-1).getLen() == 2 
								&& saves.get(saves.size()-1).getPos() != null &&
								saves.get(saves.size()-1).getPos().startsWith("nrc"))))) {//!<name with two letters, e.g. 桂花
							WordInSen newWord = new WordInSen(saves.get(0)
									.getBegin(), saves.get(0).getLen()
									+ leftLen, null);
							newWord.setPos("nrc");
							result.add(newWord);
							floatLast = false;
							saves.clear();
							flag = 0;
							leftLen = 0;
							name = null;
							containName = null;
						} else {//!< not name
							if (containName != null) {
								int index = result.size() - 1;
								result.set(index, containName);
								containName = null;
							} else if (name != null) {
								int index = result.size() - 1;
								result.set(index, name);
								name = null;
							} else
								result.add(saves.get(0));
							saves.clear();
							flag = 0;
							leftLen = 0;
							//!<organization recognition
							int lIndex = result.size()-1;
							if (lIndex < 0 || n == null) result.add(w);
							else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
									&&((result.get(lIndex).getPos() != null
									&&result.get(lIndex).getPos().startsWith("nr"))
									||(result.get(lIndex).getNode() != null
									&&(result.get(lIndex).getNode().isName()
									||result.get(lIndex).getNode().isWest1stName())))) {
								int len = result.get(lIndex).getLen();
								len += w.getLen();
								WordInSen newWord = new WordInSen(
										result.get(lIndex).getBegin(), len, null);
								if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
								result.set(lIndex, newWord);
							}
							else if (n.containChLastName()) {
								result.add(this.words.get(wIndex-1));
								flag = Const.IS_CH_LASTNAME;
								c1Node = this.words.get(wIndex+1).getNode();
								saves.add(this.words.get(wIndex+1));
								leftLen = 2;
								containName = w;
							}
							else  result.add(w);
							//!<
						}
					} else {//!< word with more then 3 letters or leftLen == 1
						if (containName != null) {
							int index = result.size() - 1;
							result.set(index, containName);
							containName = null;
						} else if (name != null) {
							int index = result.size() - 1;
							result.set(index, name);
							saves.clear();
							name = null;
						} else if (saves.size() == 1) {//!< single name
							result.add(saves.get(0));
							saves.clear();
						} else {//!< ch_l+ch_2
							WordInSen newWord = new WordInSen(saves.get(0)
									.getBegin(), saves.get(0).getLen()
									+ saves.get(1).getLen(), null);
							if (saves.size() == 2 && saves.get(1).getNode().notName())
								newWord.setPos("n");
							else
								newWord.setPos("nrc");
							result.add(newWord);
							saves.clear();
						}
						floatLast = false;
						flag = 0;
						name = null;
						//!<organization recognition
						int lIndex = result.size()-1;
						if (lIndex < 0 || n == null) result.add(w);
						else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
								&&((result.get(lIndex).getPos() != null
								&&result.get(lIndex).getPos().startsWith("nr"))
								||(result.get(lIndex).getNode() != null
								&&(result.get(lIndex).getNode().isName()
								||result.get(lIndex).getNode().isWest1stName())))) {
							int len = result.get(lIndex).getLen();
							len += w.getLen();
							WordInSen newWord = new WordInSen(
									result.get(lIndex).getBegin(), len, null);
							if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
							result.set(lIndex, newWord);
						}
						else if (n.containChLastName()) {
							result.add(this.words.get(wIndex-1));
							flag = Const.IS_CH_LASTNAME;
							c1Node = this.words.get(wIndex+1).getNode();
							saves.add(this.words.get(wIndex+1));
							leftLen = 2;
							containName = w;
						}
						else  result.add(w);
						//!<
					}
				}
				//!< single letter word
				else if (leftLen == 2) {//!< only ch_l
					if (n.isNameSuffix() && name == null) {//!<name suffix
						WordInSen newWord = new WordInSen(saves.get(0)
								.getBegin(),
								saves.get(0).getLen() + w.getLen(), null);
						newWord.setPos("nrcr");
						result.add(newWord);
						floatLast = false;
						saves.clear();
						flag = 0;
						leftLen = 0;
						name = null;
						containName = null;
					} else if (n.isCHLastName() && floatLast) {
						WordInSen newWord = new WordInSen(saves.get(0)
								.getBegin(),
								saves.get(0).getLen(), null);
						newWord.setPos("nrc");
						result.add(newWord);
						floatLast = false;
						saves.clear();
						c1Node = n;
						saves.add(w);
						leftLen = 2;
						name = null;
					} else if (n.isCHLastName() && !floatLast && this.checkMatch(c1Node, n)
							|| (n.isCH2ndName() && this.checkMatch(c1Node, n))
							|| (floatLast && n.isCH3rddName())) {//!<second letter matching
						c1Node = n;
						if (n.isCHLastName() && !floatLast) {//!<second letter is last name
							//!< previous last name wrong
							if (saves.get(0).getNode().isStrongWord()
//									|| saves.get(0).getNode().isVerbOrTime()
									) {//!<e.g. 于陈水扁不利
								if (containName != null) {
									int index = result.size() - 1;
									result.set(index, containName);
									containName = null;
								} else if (name != null) {
    								 int index = result.size()-1;
    								 result.set(index, name);
    								 name = null;
								 }
								 else
									 result.add(saves.get(0));
								 saves.clear();
								 saves.add(w);
							}
							else {
								WordInSen newWord = new WordInSen(saves.get(0)
										.getBegin(), saves.get(0).getLen()
										+ w.getLen(), null);
								saves.clear();
								if (saves.size() == 2 && saves.get(1).getNode().notName())
									newWord.setPos("n");
								else
									newWord.setPos("nrc");
								saves.add(newWord);
								name = null;
								containName = null;
							}
							leftLen = 2;
							floatLast = true;
						} else if (n.isStrongWord() && containName != null) {
							int index = result.size() - 1;
							result.set(index, containName);
							containName = null;
						} else if (n.isStrongWord() && name != null) {
							int index = result.size() - 1;
							result.set(index, name);
							name = null;
							saves.clear();
							flag = 0;
							//!<organization recognition
							int lIndex = result.size()-1;
							if (lIndex < 0 || n == null) result.add(w);
							else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
									&&((result.get(lIndex).getPos() != null
									&&result.get(lIndex).getPos().startsWith("nr"))
									||(result.get(lIndex).getNode() != null
									&&(result.get(lIndex).getNode().isName()
									||result.get(lIndex).getNode().isWest1stName())))) {
								int len = result.get(lIndex).getLen();
								len += w.getLen();
								WordInSen newWord = new WordInSen(
										result.get(lIndex).getBegin(), len, null);
								if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
								result.set(lIndex, newWord);
							}
							else if (n.containChLastName()) {
								result.add(this.words.get(wIndex-1));
								flag = Const.IS_CH_LASTNAME;
								c1Node = this.words.get(wIndex+1).getNode();
								saves.add(this.words.get(wIndex+1));
								leftLen = 2;
								containName = w;
							}
							else  result.add(w);
							//!<
						} else {
							saves.add(w);
							leftLen = 1;
							name = null;
							containName = null;
						}
					} else {//!< previous last name wrong
						if (containName != null) {
							int index = result.size() - 1;
							result.set(index, containName);
							containName = null;
						} else if (name != null) {
							int index = result.size() - 1;
							result.set(index, name);
							name = null;
						} else
							result.add(saves.get(0));
						saves.clear();
						flag = 0;
						//!<organization recognition
						int lIndex = result.size()-1;
						if (lIndex < 0 || n == null) result.add(w);
						else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
								&&((result.get(lIndex).getPos() != null
								&&result.get(lIndex).getPos().startsWith("nr"))
								||(result.get(lIndex).getNode() != null
								&&(result.get(lIndex).getNode().isName()
								||result.get(lIndex).getNode().isWest1stName())))) {
							int len = result.get(lIndex).getLen();
							len += w.getLen();
							WordInSen newWord = new WordInSen(
									result.get(lIndex).getBegin(), len, null);
							if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
							result.set(lIndex, newWord);
						}
						else if (n.containChLastName()) {
							result.add(this.words.get(wIndex-1));
							flag = Const.IS_CH_LASTNAME;
							c1Node = this.words.get(wIndex+1).getNode();
							saves.add(this.words.get(wIndex+1));
							leftLen = 2;
							containName = w;
						}
						else  result.add(w);
						//!<
					}
				} else {//!< ch_l+ch_2, leftLen == 1
					if (n.isCHLastName()) {
						if (saves.size() == 1) {//!< single name
							result.add(saves.get(0));
						} else {//!< ch_l+ch_2
							WordInSen newWord = new WordInSen(saves.get(0)
									.getBegin(), saves.get(0).getLen()
									+ saves.get(1).getLen(), null);
							if (saves.size() == 2 && saves.get(1).getNode().notName())
								newWord.setPos("n");
							else
								newWord.setPos("nrc");
							result.add(newWord);
							floatLast = false;
						}
						if (n.isCH3rddName() && !n.isStrongWord()) {
							int len = 0;
							for (WordInSen ww : saves)
								len += ww.getLen();
							name = new WordInSen(saves.get(0).getBegin(),
									len + 1, null);
							name.setPos("nrc");
						}
						saves.clear();
						saves.add(w);
						leftLen = 2;
						c1Node = n;
					} else if (n.isNameSuffix()) {
						if (saves.size() == 1) {//!< single name
							result.add(saves.get(0));
						} else {//!< ch_l+ch_2
							WordInSen newWord = new WordInSen(saves.get(0)
									.getBegin(), saves.get(0).getLen()
									+ saves.get(1).getLen(), null);
							if (saves.size() == 2 && saves.get(1).getNode().notName())
								newWord.setPos("n");
							else
								newWord.setPos("nrc");
							result.add(newWord);
						}
						saves.clear();
						flag = 0;
						floatLast = false;
						//!<organization recognition
						int lIndex = result.size()-1;
						if (lIndex < 0 || n == null) result.add(w);
						else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
								&&((result.get(lIndex).getPos() != null
								&&result.get(lIndex).getPos().startsWith("nr"))
								||(result.get(lIndex).getNode() != null
								&&(result.get(lIndex).getNode().isName()
								||result.get(lIndex).getNode().isWest1stName())))) {
							int len = result.get(lIndex).getLen();
							len += w.getLen();
							WordInSen newWord = new WordInSen(
									result.get(lIndex).getBegin(), len, null);
							if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
							result.set(lIndex, newWord);
						}
						else if (n.containChLastName()) {
							result.add(this.words.get(wIndex-1));
							flag = Const.IS_CH_LASTNAME;
							c1Node = this.words.get(wIndex+1).getNode();
							saves.add(this.words.get(wIndex+1));
							leftLen = 2;
							containName = w;
						}
						else  result.add(w);
						//!<
					} else {
						if (n.isCH3rddName()) {//!< ch_l+ch_2+ch_3
							if (saves.size() == 1
									&& (n.isStrongWord()
									||!this.checkMatch(c1Node, n))) {
								result.add(saves.get(0));
								saves.clear();
								flag = 0;
								c1Node = null;
								//!<organization recognition
								int lIndex = result.size()-1;
								if (lIndex < 0 || n == null) result.add(w);
								else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
										&&((result.get(lIndex).getPos() != null
										&&result.get(lIndex).getPos().startsWith("nr"))
										||(result.get(lIndex).getNode() != null
										&&(result.get(lIndex).getNode().isName()
										||result.get(lIndex).getNode().isWest1stName())))) {
									int len = result.get(lIndex).getLen();
									len += w.getLen();
									WordInSen newWord = new WordInSen(
											result.get(lIndex).getBegin(), len, null);
									if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
									result.set(lIndex, newWord);
								}
								else if (n.containChLastName()) {
									result.add(this.words.get(wIndex-1));
									flag = Const.IS_CH_LASTNAME;
									c1Node = this.words.get(wIndex+1).getNode();
									saves.add(this.words.get(wIndex+1));
									leftLen = 2;
									containName = w;
								}
								else  result.add(w);
								//!<
							} else {
								int len = 0;
								for (WordInSen ww : saves)
									len += ww.getLen();
								WordInSen newWord = new WordInSen(saves.get(0)
										.getBegin(), len + w.getLen(), null);
								newWord.setPos("nrc");
								result.add(newWord);
								floatLast = false;
								saves.clear();
								flag = 0;
								leftLen = 0;
							}
						} else {
							if (saves.size() == 1) {
								result.add(saves.get(0));
								saves.clear();
							} else {//!< ch_l+ch_2
								int len = 0;
								for (WordInSen ww : saves)
									len += ww.getLen();
								WordInSen newWord = new WordInSen(saves.get(0)
										.getBegin(), len, null);
								if (saves.size() == 2 && saves.get(1).getNode().notName())
									newWord.setPos("n");
								else
									newWord.setPos("nrc");
								result.add(newWord);
								floatLast = false;
								saves.clear();
							}
							leftLen = 0;
							flag = 0;
							//!<organization recognition
							int lIndex = result.size()-1;
							if (lIndex < 0 || n == null) result.add(w);
							else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
									&&((result.get(lIndex).getPos() != null
									&&result.get(lIndex).getPos().startsWith("nr"))
									||(result.get(lIndex).getNode() != null
									&&(result.get(lIndex).getNode().isName()
									||result.get(lIndex).getNode().isWest1stName())))) {
								int len = result.get(lIndex).getLen();
								len += w.getLen();
								WordInSen newWord = new WordInSen(
										result.get(lIndex).getBegin(), len, null);
								if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
								result.set(lIndex, newWord);
							}
							else if (n.containChLastName()) {
								result.add(this.words.get(wIndex-1));
								flag = Const.IS_CH_LASTNAME;
								c1Node = this.words.get(wIndex+1).getNode();
								saves.add(this.words.get(wIndex+1));
								leftLen = 2;
								containName = w;
							}
							else  result.add(w);
							//!<
						}
					}
				}
			} else if (flag == Const.IS_J_LASTNAME) {
				if (n == null || w.getLen() > 3) {
					if (saves.size() == 1) {
						result.add(saves.get(0));
						saves.clear();
					} else {
						int len = 0;
						for (WordInSen ww : saves)
							len += ww.getLen();
						WordInSen newWord = new WordInSen(saves.get(0)
								.getBegin(), len, null);
						newWord.setPos("nrj");
						result.add(newWord);
						saves.clear();
					}
					flag = 0;
					//!<organization recognition
					int lIndex = result.size()-1;
					if (lIndex < 0 || n == null) result.add(w);
					else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
							&&((result.get(lIndex).getPos() != null
							&&result.get(lIndex).getPos().startsWith("nr"))
							||(result.get(lIndex).getNode() != null
							&&(result.get(lIndex).getNode().isName()
							||result.get(lIndex).getNode().isWest1stName())))) {
						int len = result.get(lIndex).getLen();
						len += w.getLen();
						WordInSen newWord = new WordInSen(
								result.get(lIndex).getBegin(), len, null);
						if (n.isOrgName()) newWord.setPos("nt"); else newWord.setPos("ns");
						result.set(lIndex, newWord);
					}
					else if (n.containChLastName()) {
						result.add(this.words.get(wIndex-1));
						flag = Const.IS_CH_LASTNAME;
						c1Node = this.words.get(wIndex+1).getNode();
						saves.add(this.words.get(wIndex+1));
						leftLen = 2;
						containName = w;
					}
					else  result.add(w);
					//!<
				} else if (leftLen == 1) {//!< J_l+J_2+J_3
					if (w.getLen() == 1 && w.getNode().isJ4thName()) {//!< J_l+J_2+J_3+J_4
						int len = 0;
						for (WordInSen ww : saves)
							len += ww.getLen();
						WordInSen newWord = new WordInSen(saves.get(0)
								.getBegin(), len + w.getLen(), null);
						newWord.setPos("nrj");
						result.add(newWord);
						saves.clear();
						flag = 0;
					} else {//!< J_l+J_2+J_3
						int len = 0;
						for (WordInSen ww : saves)
							len += ww.getLen();
						WordInSen newWord = new WordInSen(saves.get(0)
								.getBegin(), len, null);
						newWord.setPos("nrj");
						result.add(newWord);
						saves.clear();
						flag = 0;
						//!<organization recognition
						int lIndex = result.size()-1;
						if (lIndex < 0 || n == null) result.add(w);
						else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
								&&((result.get(lIndex).getPos() != null
								&&result.get(lIndex).getPos().startsWith("nr"))
								||(result.get(lIndex).getNode() != null
								&&(result.get(lIndex).getNode().isName()
								||result.get(lIndex).getNode().isWest1stName())))) {
							int nl = result.get(lIndex).getLen();
							nl += w.getLen();
							WordInSen nw = new WordInSen(
									result.get(lIndex).getBegin(), nl, null);
							if (n.isOrgName()) nw.setPos("nt"); else nw.setPos("ns");
							result.set(lIndex, nw);
						}
						else if (n.containChLastName()) {
							result.add(this.words.get(wIndex-1));
							flag = Const.IS_CH_LASTNAME;
							c1Node = this.words.get(wIndex+1).getNode();
							saves.add(this.words.get(wIndex+1));
							leftLen = 2;
							containName = w;
						}
						else  result.add(w);
						//!<
					}
				} else if (leftLen == 2) {
					if (w.getLen() == 1 && w.getNode().isJ3rdName()) {
						saves.add(w);
						leftLen--;
					} else {//!< J_l+J_2
						int len = 0;
						for (WordInSen ww : saves)
							len += ww.getLen();
						WordInSen newWord = new WordInSen(saves.get(0)
								.getBegin(), len, null);
						newWord.setPos("nrj");
						result.add(newWord);
						saves.clear();
						flag = 0;
						//!<organization recognition
						int lIndex = result.size()-1;
						if (lIndex < 0 || n == null) result.add(w);
						else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
								&&((result.get(lIndex).getPos() != null
								&&result.get(lIndex).getPos().startsWith("nr"))
								||(result.get(lIndex).getNode() != null
								&&(result.get(lIndex).getNode().isName()
								||result.get(lIndex).getNode().isWest1stName())))) {
							int nl = result.get(lIndex).getLen();
							nl += w.getLen();
							WordInSen nw = new WordInSen(
									result.get(lIndex).getBegin(), nl, null);
							if (n.isOrgName()) nw.setPos("nt"); else nw.setPos("ns");
							result.set(lIndex, nw);
						}
						else if (n.containChLastName()) {
							result.add(this.words.get(wIndex-1));
							flag = Const.IS_CH_LASTNAME;
							c1Node = this.words.get(wIndex+1).getNode();
							saves.add(this.words.get(wIndex+1));
							leftLen = 2;
							containName = w;
						}
						else  result.add(w);
						//!<
					}
				} else {//!< J_l
					if (w.getLen() == 1 && w.getNode().isJ2ndName() && this.checkMatch(c1Node, n)) {
						saves.add(w);
						leftLen--;
					} else if(w.getNode().isName()){
						int len = saves.get(0).getLen();
						len += w.getLen();
						WordInSen newWord = new WordInSen(saves.get(0)
								.getBegin(), len, null);
						newWord.setPos("nrj");
						result.add(newWord);
						saves.clear();
						flag = 0;
					} else {
						result.add(saves.get(0));
						saves.clear();
						flag = 0;
						//!<organization recognition
						int lIndex = result.size()-1;
						if (lIndex < 0 || n == null) result.add(w);
						else if ((n.isOrgName() || n.isNSend())
							&&result.get(lIndex).getLen()>1 
								&&((result.get(lIndex).getPos() != null
								&&result.get(lIndex).getPos().startsWith("nr"))
								||(result.get(lIndex).getNode() != null
								&&(result.get(lIndex).getNode().isName()
								||result.get(lIndex).getNode().isWest1stName())))) {
							int nl = result.get(lIndex).getLen();
							nl += w.getLen();
							WordInSen nw = new WordInSen(
									result.get(lIndex).getBegin(), nl, null);
							if (n.isOrgName()) nw.setPos("nt"); else nw.setPos("ns");
							result.set(lIndex, nw);
						}
						else if (n.containChLastName()) {
							result.add(this.words.get(wIndex-1));
							flag = Const.IS_CH_LASTNAME;
							c1Node = this.words.get(wIndex+1).getNode();
							saves.add(this.words.get(wIndex+1));
							leftLen = 2;
							containName = w;
						}
						else  result.add(w);
						//!<
					}
				}
			}
		}
		//!< clean up work
		if (saves.size() > 0 || nameLen > 0) {
			if (flag == Const.IS_CH_LASTNAME) {
				if (saves.size() == 1) {
					if (name == null) {
						//!< if(saves.get(0).getLen() == 1)
						//!< saves.get(0).setPos("");
						result.add(saves.get(0));
					} else {
						int index = result.size() - 1;
						result.set(index, name);
					}
				} else {
					int len = 0;
					for (WordInSen ww : saves)
						len += ww.getLen();
					WordInSen newWord = new WordInSen(saves.get(0).getBegin(),
							len, null);
					if (saves.size() == 2 && saves.get(1).getNode().notName())
						newWord.setPos("n");
					else
						newWord.setPos("nrc");
					result.add(newWord);
				}
			} else if (flag == Const.IS_J_LASTNAME) {
				if (saves.size() == 1) {
					result.add(saves.get(0));
				} else {
					int len = 0;
					for (WordInSen ww : saves)
						len += ww.getLen();
					WordInSen newWord = new WordInSen(saves.get(0).getBegin(),
							len, null);
					newWord.setPos("nrj");
					result.add(newWord);
				}
			} else if (flag == Const.IS_WEST_1_NAME) {
				if (name == null) {
//					this.word_dic.insertWord(null, this.word_dic.root,
//							this.sen.substring(nameBegin, nameBegin
//									+ nameLen), 0, flag);
//					String[] words = { "", "", "(nr,1)\t(ns,1)" };
//					this.myMarkpos.addWord2Dis(this.word_dic.terminal, words);
					WordInSen newWord = new WordInSen(nameBegin, nameLen,
							null);
					newWord.setPos("nrf");
					result.add(newWord);
				} else {
					result.add(name);
					name = null;
				}
			} else {
				result.add(saves.get(0));
			}
		}
		return;
	}

	/**
     * @brief segment a short sentence
     * @param sen the sentence to be segmented
     * @param sResult the segmentation result
     * @param debug whether output internal variables
     * @param findName name recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findOrg organization recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findPlace address recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     */
	private void segSen(String sen, ArrayList<String> sResult, boolean debug, int findName,
			int findOrg, int findPlace) {
		if (sen == null || sResult == null || sen.length() == 0)
			return;
		sResult.clear();
		ArrayList<String> result = new ArrayList<String>();
		String s1 = "";
		for (int i = 0; i < sen.length(); i++) {
			if (sen.charAt(i) >= 'A' && sen.charAt(i) <= 'Z') 
				s1 += (char)(sen.charAt(i)+'a'-'A');
			else s1 += sen.charAt(i);
		}
		this.sen = s1;
		this.findAllWords(findName, findOrg, findPlace);//!< step1
		if (debug) {
		System.out
				.println("-----------------------------------AllLeftWords after find all words");
		this.showAllLeftWords();
		System.out
				.println("-----------------------------------AllLeftWords end");
		}
		
		this.findCompetition();//!< step2
		if (debug) {
		System.out
				.println("-----------------------------------AllLeftWords after find competition");
		this.showAllLeftWords();
		System.out
				.println("-----------------------------------AllLeftWords end");
		}
		
		this.solveComp();//!< step3
		if (debug) {
		System.out
				.println("-----------------------------------AllLeftWords after solve competition");
		this.showAllLeftWords();
		System.out
				.println("-----------------------------------AllLeftWords end");
		}
		
		this.getResult();//!< step4
		if (debug) {
		System.out
				.println("-----------------------------------AllLeftWords after getResult");
		this.showAllLeftWords();
		System.out
				.println("-----------------------------------AllLeftWords end");
		}
		
		this.comb1();//!< step5
		if (debug) {
		System.out
				.println("-----------------------------------AllLeftWords after comb1");
		this.showAllLeftWords();
		System.out
				.println("-----------------------------------AllLeftWords end");
		}
		
		if (1 == findPlace) {
			this.findNSU();//!< step6
			if (debug) {
			System.out
					.println("-----------------------------------AllLeftWords after findNSU");
			this.showAllLeftWords();
			System.out
					.println("-----------------------------------AllLeftWords end");
			}			
		}
		//!< this.confRecord();
		ArrayList<WordInSen> wordList = new ArrayList<WordInSen>();//!< for pos tagging
		ArrayList<String> posList = new ArrayList<String>();

		if (1 == findName) this.findName(wordList);//!< step7
		else {
			for (WordInSen w: this.words) {
				if (w.isSelected()) wordList.add(w);
			}
		}

		//second combination
		WordInSen preword = null;
		for (WordInSen word : wordList) {
			if (word.getPos() != null 
					&& word.getLen() == 2
					&& word.getPos().compareTo("n") == 0 
					&& this.sen.charAt(word.getBegin()+1) == '家'
					&& preword != null 
					&& preword.getPos() != null
					&& preword.getLen() == 2
					&& preword.getPos().compareTo("nrc") == 0
					) {
				preword.setLen(3);
				word.setBegin(word.getBegin()+1);
				word.setLen(1);
				result.set(result.size()-1, this.sen.substring(preword.getBegin(), 
						preword.getBegin() + 3));
			}
			result.add(this.sen.substring(word.getBegin(), word
					.getBegin() + word.getLen()));//!< segmentation result without pos tagging
			preword = word;
		}

		markSen(wordList, posList);//!< step8 pos tagging
		// for debug
//		System.out.println("---------before combination-----------");
		
		//third combination
		ArrayList<Term> tmpResult = new ArrayList<Term>();
		String preWord = "";
		for (int i = 0; i < wordList.size(); i++) {
			String tmp = "";
			Term tmpT;
			TTreeNode n = wordList.get(i).getNode();
			if (i != 0 && n != null && n.isCH3rddName()
					&& preWord.length() == 2
					&& posList.get(i-1).startsWith("nr")) {//!<composing name
				tmp = preWord+result.get(i)+"/nrc ";
				result.set(i-1, "");
				tmpT = new Term(preWord+result.get(i), "nrc", "", "", "", null, "!0!", 0);//OK
				tmpResult.set(tmpResult.size()-1, tmpT);
			}
			else if (i != 0 && n != null && n.isPlaceEnd()
					&& posList.get(i-1).startsWith("n")) {//!<composing address name
				tmp = preWord+result.get(i)+"/ns ";
				result.set(i-1, "");
				tmpT = new Term(preWord+result.get(i), "ns", "", "", "", null, "!0!", 0);//OK
				tmpResult.set(tmpResult.size()-1, tmpT);
			}
			else if (posList.get(i).charAt(0) == '[') {//!<hierarchical segmentation
				tmp = posList.get(i) + " ";
				int k = posList.get(i).indexOf(']');
				String hpos = posList.get(i).substring(k+2);
				tmpT = new Term(result.get(i), hpos, "", "", 
						posList.get(i).substring(1, k), wordList.get(i).getNode(),
						this.word2weight.get(wordList.get(i).getNode()), 1);//OK
				tmpResult.add(tmpT);
			}
			else if ((result.get(i).endsWith(".") || result.get(i).endsWith("_")
					|| result.get(i).endsWith("·") || result.get(i).endsWith("-"))
					&& posList.get(i).startsWith("nr")) {//!<decompose west name
				tmp = result.get(i).substring(0, result.get(i).length()-1);
				String ss = result.get(i).substring(result.get(i).length()-1);
				tmpT = new Term(tmp, posList.get(i), "", "", "", null, "!0!", 0);//OK
				tmpResult.add(tmpT);
				tmp += "/" + posList.get(i) + "  " + ss +"/w ";
				tmpT = new Term(ss, "w", "", "", "", null, "!0!", 0);//OK
				tmpResult.add(tmpT);				
			}
			else {
				boolean isName = false;
				for (String s: this.notname) {
					if (result.get(i).endsWith(s)&& posList.get(i).startsWith("nr")) {
						tmp = result.get(i).substring(0, result.get(i).length()-1);
						String ss = result.get(i).substring(result.get(i).length()-1);
						tmpT = new Term(tmp, posList.get(i), "", "", "", null, "!0!", 0);//OK
						tmpResult.add(tmpT);
						tmp += "/" + posList.get(i) + "  " + ss +"/ad ";
						tmpT = new Term(ss, "ad", "", "", "", null, "!0!", 0);//OK
						tmpResult.add(tmpT);
						isName = true;
						break;
					}
				}
				if (!isName) {
					tmp = result.get(i) + "/" + posList.get(i) + " ";
					int k = posList.get(i).indexOf(myDic.sep.charAt(0));
					if (k > 0) {
						if (posList.get(i).charAt(k+1) >= '0' &&
								posList.get(i).charAt(k+1) <= '9')//!<ID
							tmpT = new Term(result.get(i), posList.get(i).substring(0,k), posList.get(i).substring(k+1), 
									"", "", wordList.get(i).getNode(),
									"!"+this.word2weight.get(wordList.get(i).getNode())+"!", 0);//OK
						else//!<positive or negative
							tmpT = new Term(result.get(i), posList.get(i).substring(0,k), "", posList.get(i).substring(k+1), 
									"", wordList.get(i).getNode(),
									"!"+this.word2weight.get(wordList.get(i).getNode())+"!", 0);//OK
							
					}
					else
						tmpT = new Term(result.get(i), posList.get(i), "", "", "", wordList.get(i).getNode(),
								"!"+this.word2weight.get(wordList.get(i).getNode())+"!", 0);//OK
					tmpResult.add(tmpT);
				}
			}
			preWord = result.get(i);
			result.set(i, tmp);
		}
		for (String s: result) 
			if (s.length() > 0) sResult.add(s);
		
		//find organization
		if (1 == findOrg)
			sResult = this.findNT(tmpResult, sResult);
		else
			this.segResult.addAll(tmpResult);
//		System.out.println();
	}

	/**
     * @brief segment text
     * @param text the text to be segmented
     * @param result the String type segmentation result
     * @param debug whether output internal variables
     * @param findName name recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findOrg organization recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findPlace address recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @return the Term type segmentation result
     */
	private ArrayList<Term> segText(String text, ArrayList<String> result, boolean debug, 
			int findName, int findOrg, int findPlace) {
		if (text == null || result == null || text.length() == 0)
			return null;
		this.segResult.clear();
		result.clear();
		String seps = "　 \t\n\r\"!?;﹕“”‘’；，﹐。、`《》？！|［］「」『』〖〗【】{}";
		int begin = 0, i;
		String pre = "", cur;
		for (i = 0; i < text.length(); i++) {
			cur = text.substring(i, i + 1);
			if (seps.contains(cur)) {//!< separation
				if (begin < i) {
					if (pre.compareTo("《") == 0 && cur.compareTo("》") == 0 
							|| pre.compareTo("“") == 0 && cur.compareTo("”") == 0
							&& i - begin <= 5) {
						result.add(text.substring(begin, i) + "/nz");
						Term t = new Term(text.substring(begin, i), "nz", "", "", "", null, "!0!", 1);//OK
						this.segResult.add(t);
					} else {
						ArrayList<String> tmp = new ArrayList<String>();
						this.segSen(text.substring(begin, i), tmp, debug, findName, findOrg,
								findPlace);
						for (String s : tmp)
							result.add(s);
					}
					//!< System.out.println("-----------------------------------showAllComps");
					//!< this.showAllComps();
					//!< System.out.println("-----------------------------------showAllLeftWords");
					//!< this.showAllLeftWords();
				}
				if (cur.compareTo(" ") != 0 && cur.compareTo("　") != 0) {
					result.add(cur + "/w ");
					Term t = new Term(cur, "w", "", "", "", null, "!0!", 1);//OK
					this.segResult.add(t);
				}
				begin = i + 1;
				pre = cur;
			}
		}
		if (begin < i) {
			ArrayList<String> tmp = new ArrayList<String>();
			this.segSen(text.substring(begin, i), tmp, debug, findName, findOrg, findPlace);
			for (String s : tmp)
				result.add(s);
			// System.out.println("-----------------------------------showAllComps");
			// this.showAllComps();
			// System.out.println("-----------------------------------showAllLeftWords");
			// this.showAllLeftWords();
		}
		if(this.verifyName) this.verifyName(result);
		if(this.findNewWord) this.findNewWord(true);
		if(this.seg_English) this.tagEnglish(result);
		
		result.clear();
		for (int k = 0; k < this.segResult.size(); k++) {
			String s = this.segResult.get(k).word+"/"+this.segResult.get(k).pos+";";
//			if (this.segResult.get(k).ID.length() > 0)
//				s += this.segResult.get(k).ID+";";
//			if (this.segResult.get(k).posOrNeg.length() > 0)
//				s += this.segResult.get(k).posOrNeg+";";
//			s += this.segResult.get(k).weight+" ";
			result.add(s);
		}
		
//		if (result.size() == this.segResult.size())
//			for (int k = 0; k < result.size(); k++) {
//				String weight = ";"+this.segResult.get(k).weight;
//				result.set(k, result.get(k)+weight);
//			}
//		else
//			for (int k = 0; k < result.size(); k++) {
//				int end = result.get(k).indexOf('/');
//				String word = result.get(k).substring(0, end);
//				TTreeNode n = this.word_dic.string2Node(word);
//				String weight = ";!"+this.word2weight.get(n)+"! ";
//				result.set(k, result.get(k)+weight);
//			}
		
		return this.segResult;
	}

	/**
     * @brief find new word in current sentence
     * @param clearResult whether clear previous new words
     */
	private void findNewWord(boolean clearResult) {
		if(clearResult) this.newWord2fre.clear();
		NTTree newWordTree = new NTTree();
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<String> poses = new ArrayList<String>();
		for (Term t: this.segResult) {
			words.add(t.word);
			poses.add(t.pos);
		}
		if (words.size() > 1) {
			for (int i = 0; i < words.size()-1; i++) {
				newWordTree.insertWord(null, newWordTree.root, words, poses, i);
			}
		}
		newWordTree.findAllWords(newWordTree.root, "", "", 0);
//		System.out.println(newWordTree.newTerms.size());
		newWordTree.clearResult(Const.MIN_NEW_WORD_FRE);
		for (NTerm t: newWordTree.newTerms) {
			if (t.selected == true) {
				String s = t.word1+"\t"+t.word+"\t"+t.len+"\t";
				if (this.newWord2fre.containsKey(s)) {
					int fre = this.newWord2fre.get(s) + t.fre;
					this.newWord2fre.put(s, fre);
				}
				else {
					this.newWord2fre.put(s, t.fre);
				}
			}
		}
//		System.out.println("nodeNum:"+newWordTree.getNodeNum()+"\tnew word number:"+this.newWord2fre.size());
//		for (String s: this.newWord2fre.keySet()) {
//			System.out.println(s+"\t"+this.newWord2fre.get(s));
//		}
	}
	
	/**
     * @brief get the new words with corresponding frequencies
     * @return word and frequency map
     */
	private HashMap<String, Integer> getNewWord2fre() {
		return this.newWord2fre;
	}
	
	/**
     * @brief clear name frequencies map
     */
	private void clearName2Fre() {
		this.name2fre.clear();
	}
	
	/**
     * @brief verify Chinese name. some Chinese name might be wrongly recognized. this function
     * 		will change wrong long name to correct short name. sometimes this function might also
     * 		change correct long name to wrong short name
     * @param result the current segmentation result including POS 
     * @note this function will modify the segmentation result, meanwhile, it will modify the other
     * 		form of segmentation result
     */
	private void verifyName(ArrayList<String> result) {
//		this.name2fre.clear();
		for (Term t: this.segResult) {
			if (t.pos.compareTo("nrc") == 0) {
				if (this.name2fre.containsKey(t.word)) {
					this.name2fre.get(t.word).fre++;
				}
				else {
					nameInfo ni = new nameInfo (1,-1);
					this.name2fre.put(t.word, ni);
				}
			}
		}
		if (name2fre.size() > 0) {
			boolean modify = false;
			for (String s: name2fre.keySet()) {//!<long name
				if (name2fre.get(s).newEnd > 0) continue;
				if (s.length() <= 2) continue;
				for (String s1: name2fre.keySet()) {//!<short name
					if (s == s1) continue;
					if (name2fre.get(s1).newEnd > 0) continue;
					if (s.startsWith(s1) && name2fre.get(s).fre < name2fre.get(s1).fre) {
						name2fre.get(s).newEnd = s1.length();
						modify = true;
						break;
					}
				}
			}
			if (modify) {
				ArrayList<Term> tmp = new ArrayList<Term>();
				HashMap<String, String> oName2nName = new HashMap<String, String>();
				for (Term t: this.segResult) {
					if (this.name2fre.containsKey(t.word) &&
							this.name2fre.get(t.word).newEnd > 0) {
						String s1 = t.word.substring(0, this.name2fre.get(t.word).newEnd);
						String s2 = t.word.substring(this.name2fre.get(t.word).newEnd);
						Term t1 = new Term(s1, "nrc", "", "", "", null, "!0!", 0);//OK
						tmp.add(t1);
						TTreeNode n = myDic.word_dic.string2Node(s2);
						String pos = "";
						if (null == n) pos = "?";
						else if (null != n.pd) {
							pos = this.myDic.id2Pos.get(n.pd.getMaxPos());
							if (pos == null) pos = "?";
						}
						Term t2 = new Term(s2, pos, "", "", "", n, this.word2weight.get(n), 0);//OK
						tmp.add(t2);
						String oName = t.word+"/nrc ";
						String nName = s1+"/nrc  "+s2+"/"+pos+" ";
						oName2nName.put(oName, nName);
					}
					else tmp.add(t);
				}
				for (int k = 0; k < result.size(); k++) {
					if (oName2nName.containsKey(result.get(k))) 
						result.set(k, oName2nName.get(result.get(k)));
				}
				this.segResult.clear();
				this.segResult.addAll(tmp);
			}
		}
	}
	
	/**
     * @brief tag POS of English words in current sentence
     */
	private void tagEnglish(ArrayList<String> result) {
//		this.name2fre.clear();
		ArrayList<String> newResult = new ArrayList<String>();
		ArrayList<Term> newSegResult = new ArrayList<Term>();
		String curPhrase1 = "";
		String curPhrase2 = "";
		for (int i = 0, j = 0; i < this.segResult.size() && j < result.size(); i++, j++) {
			while (i < this.segResult.size() && this.segResult.get(i).pos.compareTo("nx") != 0) {//!<find begin point
				newSegResult.add(this.segResult.get(i));
				i++;
			}
			if (i == this.segResult.size()) {//!<no English word
				while (j < result.size()) {
					newResult.add(result.get(j));
					j++;
				}
				break;
			}
			curPhrase1 = this.segResult.get(i).word;
			i++;
			while (i < this.segResult.size() && this.enPos.contains(this.segResult.get(i).pos)) {//!<find end point
				curPhrase1 += " "+this.segResult.get(i).word;
				i++;
			}
			
			while (j < result.size() && result.get(j).substring(result.get(j).lastIndexOf('/')+1, result.get(j).length()-1).compareTo("nx") != 0) {//!<find begin point
//				String pos = result.get(j).substring(result.get(j).lastIndexOf('/')+1, result.get(j).length()-1);
				newResult.add(result.get(j));
				j++;
			}
			if (j < result.size()) {
				curPhrase2 = result.get(j).substring(0, result.get(j).lastIndexOf('/'));
				j++;
				while (j < result.size() && this.enPos.contains(result.get(j).substring(result.get(j).lastIndexOf('/')+1, result.get(j).length()-1))) {
					curPhrase2 += " "+result.get(j).substring(0, result.get(j).lastIndexOf('/'));
					j++;
				}
			}
			
			if (curPhrase1.length() > 0 && curPhrase1.compareTo(curPhrase2) == 0) {
			    try{
					List<List<String>> lines = this.mySegEn.tokenizeToListWithPOS(curPhrase1+" ");
					for (List<String> line : lines) {
						for (String s: line) {
							String word = s.substring(0, s.lastIndexOf('/'));
							String pos = s.substring(s.lastIndexOf('/')+1);
							if (pos.startsWith("CD")) pos = "m";
							Term t = new Term(word, pos, "", "", "", null, "!0!", 1);//OK
							newSegResult.add(t);
							newResult.add(word+"/"+pos);
						}
				    }
				}catch(IOException e){e.printStackTrace();}				
			}
			else {
				if (curPhrase1.length() > 0) {
				    try{
						List<List<String>> lines = this.mySegEn.tokenizeToListWithPOS(curPhrase1+" ");
						for (List<String> line : lines) {
							for (String s: line) {
								String word = s.substring(0, s.lastIndexOf('/'));
								String pos = s.substring(s.lastIndexOf('/')+1);
								if (pos.startsWith("CD")) pos = "m";
								Term t = new Term(word, pos, "", "", "", null, "!0!", 1);//OK
								newSegResult.add(t);
							}
					    }
					}catch(IOException e){e.printStackTrace();}				
				}
				if (curPhrase2.length() > 0) {
				    try{
						List<List<String>> lines = this.mySegEn.tokenizeToListWithPOS(curPhrase2+" ");
						for (List<String> line : lines) {
							for (String s: line) {
								String word = s.substring(0, s.lastIndexOf('/'));
								String pos = s.substring(s.lastIndexOf('/')+1);
								if (pos.startsWith("CD")) pos = "m";
								newResult.add(word+"/"+pos);
							}
					    }
					}catch(IOException e){e.printStackTrace();}				
				}				
			}
			curPhrase1 = "";
			curPhrase2 = ""; 
			if (i < this.segResult.size())
				newSegResult.add(this.segResult.get(i));
			if (j < result.size())
				newResult.add(result.get(j));
		}
		if (curPhrase1.length() > 0 && curPhrase1.compareTo(curPhrase2) == 0) {
		    try{
				List<List<String>> lines = this.mySegEn.tokenizeToListWithPOS(curPhrase1);
				for (List<String> line : lines) {
					for (String s: line) {
						String word = s.substring(0, s.lastIndexOf('/'));
						String pos = s.substring(s.lastIndexOf('/')+1);
						if (pos.startsWith("CD")) pos = "m";
						Term t = new Term(word, pos, "", "", "", null, "!0!", 1);//OK
						newSegResult.add(t);
						newResult.add(word+"/"+pos);
					}
			    }
			}catch(IOException e){e.printStackTrace();}				
		}
		else {
			if (curPhrase1.length() > 0) {
			    try{
					List<List<String>> lines = this.mySegEn.tokenizeToListWithPOS(curPhrase1);
					for (List<String> line : lines) {
						for (String s: line) {
							String word = s.substring(0, s.lastIndexOf('/'));
							String pos = s.substring(s.lastIndexOf('/')+1);
							if (pos.startsWith("CD")) pos = "m";
							Term t = new Term(word, pos, "", "", "", null, "!0!", 1);//OK
							newSegResult.add(t);
						}
				    }
				}catch(IOException e){e.printStackTrace();}				
			}
			if (curPhrase2.length() > 0) {
			    try{
					List<List<String>> lines = this.mySegEn.tokenizeToListWithPOS(curPhrase2);
					for (List<String> line : lines) {
						for (String s: line) {
							String word = s.substring(0, s.lastIndexOf('/'));
							String pos = s.substring(s.lastIndexOf('/')+1);
							if (pos.startsWith("CD")) pos = "m";
							newResult.add(word+"/"+pos);
						}
				    }
				}catch(IOException e){e.printStackTrace();}				
			}				
		}
		this.segResult.clear();
		this.segResult.addAll(newSegResult);
		result.clear();
		result.addAll(newResult);
	}
	
	/**
     * @brief clear all class score of current sentence
     */
	private void clearScore() {
		this.classScore = new float[8];
	}
	
	/**
     * @brief get the class name of current sentence
     * @return the most possible class name
     */
	private String getClassName() {
		int cId = 8;
		float maxScore = 0;
		for (int i = 0; i < 8; i++) {
			if (this.classScore[i] > maxScore) {
				maxScore = this.classScore[i];
				cId = i;
			}
		}
		return this.className[cId];
	}
	
	/**
     * @brief find organization name in current sentence
     * @param tList Term segmentation result
     * @param sList String segmentation result
     * @return the String segmentation result with recognized organization names
     */
	private ArrayList<String> findNT(ArrayList<Term> tList, ArrayList<String> sList) {
		ArrayList<String> sResult = new ArrayList<String>();
		if (tList.size() != sList.size()) {
			this.segResult.addAll(tList);
			return sList;
		}
		int state = 0;
		int begin = -1;
		int subBegin = -1;
		boolean combine = false;
		for (int i = 0; i < tList.size(); i++) {
			switch (state) {
			case 0: {//!<initial state
				if (tList.get(i).pos.startsWith("ns")) {
					state = 1;
					begin = i;
				}
				else if (tList.get(i).pos.startsWith("nr")) {
					state = 2;
					begin = i;
				}
				else {
					this.segResult.add(tList.get(i));
					sResult.add(sList.get(i));
				}
				break;
			}
			case 1: {//!<start of ns
				if (tList.get(i).pos.startsWith("ns"))
					continue;
				else if (tList.get(i).pos.startsWith("nr")) {
					state = 2;
				}
				else if (tList.get(i).n != null && tList.get(i).n.isOrgExclude()
						|| tList.get(i).pos.compareTo("w") == 0) {//!<excluding word
					for (int j = begin; j <= i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					state = 0;
					begin = -1;
				}
				else if (tList.get(i).n != null && tList.get(i).n.isOrgName()) {
					if (i-begin < Const.MAX_ORG_LEN && i-begin > 1) {//!<new NT
						String word = "", sWord ="";
						for (int j = begin; j <= i; j++){
							word += tList.get(j).word;
							sWord += sList.get(j)+"  ";
						}
						Term t = new Term(word, "nt_", "", "", sWord, null, "!0!", 0);//OK
						this.segResult.add(t);
						sWord = "["+sWord+"]/nt_";
						sResult.add(sWord);
						combine = true;
					}
					else {
						for (int j = begin; j <= i; j++) {
							this.segResult.add(tList.get(j));
							sResult.add(sList.get(j));
						}
					}
					begin = -1;
					if (tList.get(i).n.isOrgEnd() || !combine) {
						state = 0;
					}
					else {
						state = 4;
						subBegin = i+1;
					}
					combine = false;
				}
				else state = 3;
				break;
			}
			case 2: {//!<ns...nr
				if (tList.get(i).pos.startsWith("nr"))
					continue;
				else if (tList.get(i).pos.startsWith("ns")) {//!<restart
					state = 1;
					for (int j = begin; j < i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					begin = i;
				}
				else if (tList.get(i).n != null && tList.get(i).n.isOrgExclude()
						|| tList.get(i).pos.compareTo("w") == 0 ) {//!<excluding word
					for (int j = begin; j <= i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					state = 0;
					begin = -1;
				}
				else if (tList.get(i).n != null && tList.get(i).n.isOrgName()) {//!<end of organization name
					if (i-begin < Const.MAX_ORG_LEN && i-begin > 1) {//!<new NT
						String word = "", sWord ="";
						for (int j = begin; j <= i; j++){
							word += tList.get(j).word;
							sWord += sList.get(j)+"  ";
						}
						Term t = new Term(word, "nt_", "", "", sWord, null, "!0!", 0);//OK
						this.segResult.add(t);
						sWord = "["+sWord+"]/nt_";
						sResult.add(sWord);
						combine = true;
					}
					else {
						for (int j = begin; j <= i; j++) {
							this.segResult.add(tList.get(j));
							sResult.add(sList.get(j));
						}
					}
					begin = -1;
					if (tList.get(i).n.isOrgEnd() || !combine) {
						state = 0;
					}
					else {
						state = 4;
						subBegin = i+1;
					}
					combine = false;
				}
				else state = 3;
				break;
			}
			case 3: {
				if (tList.get(i).pos.startsWith("nr")) {
					state = 2;
					for (int j = begin; j < i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					begin = i;
				}
				else if (tList.get(i).pos.startsWith("ns")) {
					state = 1;
					for (int j = begin; j < i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					begin = i;
				}
				else if (tList.get(i).n != null && tList.get(i).n.isOrgExclude()
						|| tList.get(i).pos.compareTo("w") == 0) {//!< excluding word
					for (int j = begin; j <= i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					state = 0;
					begin = -1;
				}
				else if (tList.get(i).n != null && tList.get(i).n.isOrgName()) {
					if (i-begin < Const.MAX_ORG_LEN && i-begin > 1) {//!<new NT
						String word = "", sWord ="";
						for (int j = begin; j <= i; j++){
							word += tList.get(j).word;
							sWord += sList.get(j)+"  ";
						}
						Term t = new Term(word, "nt_", "", "", sWord, null, "!0!", 0);//OK
						this.segResult.add(t);
						sWord = "["+sWord+"]/nt_";
						sResult.add(sWord);
						combine = true;
					}
					else {
						for (int j = begin; j <= i; j++) {
							this.segResult.add(tList.get(j));
							sResult.add(sList.get(j));
						}
					}
					begin = -1;
					if (tList.get(i).n.isOrgEnd() || !combine) {
						state = 0;
					}
					else {
						state = 4;
						subBegin = i+1;
					}
					combine = false;
				}
				break;
			}
			case 4: {//!<part of organization name detected
				if (tList.get(i).pos.startsWith("nr")) {
					state = 2;
					for (int j = subBegin; j < i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					begin = i;
					subBegin = -1;
				}
				else if (tList.get(i).pos.startsWith("ns")) {
					state = 1;
					for (int j = subBegin; j < i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					begin = i;
					subBegin = -1;
				}
				else if (tList.get(i).n != null && tList.get(i).n.isOrgExclude()
						|| tList.get(i).pos.compareTo("w") == 0) {//!<excluding name
					for (int j = subBegin; j <= i; j++) {
						this.segResult.add(tList.get(j));
						sResult.add(sList.get(j));
					}
					state = 0;
					subBegin = -1;
				}
				else if (tList.get(i).n != null && tList.get(i).n.isOrgEnd()) {
					if (i-subBegin < Const.MAX_ORG_LEN && i-subBegin > 1) {//!<new NT
						int index1 = segResult.size()-1;
						int index2 = sResult.size()-1;
						int index3 = sResult.get(index2).length()-5;
						String word = segResult.get(index1).word;
						if (index3 <= 1)
							System.out.println();
						String sWord = sResult.get(index2).substring(1, index3);
						for (int j = subBegin; j <= i; j++){
							word += tList.get(j).word;
							sWord += sList.get(j)+"  ";
						}
						Term t = new Term(word, "nt_", "", "", sWord, null, "!0!", 0);//OK
						this.segResult.set(index1, t);
						sWord = "["+sWord+"]/nt_";
						sResult.set(index2, sWord);
					}
					else {
						for (int j = subBegin; j <= i; j++) {
							this.segResult.add(tList.get(j));
							sResult.add(sList.get(j));
						}
					}
					subBegin = -1;
					state = 0;
				}
				break;
			}
			}
		}
		if (begin >= 0) {
			for (int j = begin; j < tList.size(); j++) {
				this.segResult.add(tList.get(j));
				sResult.add(sList.get(j));
			}
		}
		else if (subBegin >= 0) {
			for (int j = subBegin; j < tList.size(); j++) {
				this.segResult.add(tList.get(j));
				sResult.add(sList.get(j));
			}
		}
		sList.clear();
		sList.addAll(sResult);
		return sResult;
	}
	
	/**
     * @brief find HongKong address name in current sentence
     */
	private void findNSU() {
		boolean inNSU = false;
		ArrayList<Integer> positions = new ArrayList<Integer>();//!<for all units
		ArrayList<Integer> elements = new ArrayList<Integer>();//!<for all elements in one unit
		int begin = -1;//!<head of a unit
		int len = 0;//!<length of a unit
		for (int wIndex = 0; wIndex < this.words.size(); wIndex++) {
			WordInSen w = this.words.get(wIndex);
			if (!w.isSelected()) continue;
			String s = this.sen.substring(w.getBegin(), w.getBegin() + w.getLen());
			if (inNSU) {//!<start of ns has been detected
				if (myDic.uExcept.contains(s)) {//!<ns units end
					inNSU = false;
					if (positions.size() > 1) {
						for (int i: positions) this.words.get(i).setPos("nsu");
					}
					positions.clear();
					begin = -1;
					len = 0;
				}
				else if (myDic.uEnd.contains(s.substring(s.length()-1)) || 
						s.length() > 1 && myDic.uEnd.contains(s.substring(s.length()-2))) {//!<end of a unit
					positions.add(wIndex);
					if (begin != -1) {
						for (int i: elements) this.words.get(i).setSelected(false);
						this.words.get(wIndex).setBegin(begin);
						len += this.words.get(wIndex).getLen();
						this.words.get(wIndex).setLen(len);
						this.words.get(wIndex).setNode(null);
						elements.clear();
						begin = -1;
						len = 0;
					}
				}
				else {//!<possible elements of a unit
					elements.add(wIndex);
					if (begin == -1) {
						begin = this.words.get(wIndex).getBegin();
					}
					len += this.words.get(wIndex).getLen();
					if (elements.size() > Const.MAX_NSU_LEN) {//!<ns units end
						inNSU = false;
						if (positions.size() > 1) {
							for (int i: positions) this.words.get(i).setPos("nsu");
						}
						positions.clear();
						begin = -1;
						len = 0;
					}
				}
			}
			else {
				if (myDic.uStart.contains(s)) {
					inNSU = true;
					positions.add(wIndex);
				}
			}
		}
		if (positions.size() > 1) {
			for (int i: positions) this.words.get(i).setPos("nsu");
		}
	}
	
	/**
     * @brief tag POS of each word in the sentence
     * @param wordList the words to be tagged
     * @param posList the result POS list
     */
	private void markSen(ArrayList<WordInSen> wordList, ArrayList<String> posList) {
		if (wordList.size() == 0 || posList == null) {
			System.out.println("Mark pos error.");
			return;
		}
		int len = wordList.size();
		int[] posIds = new int[len];
		ArrayList<TTreeNode> viterbiWords = new ArrayList<TTreeNode>();//!< words in current phrase
		int bid = myDic.pos2Id.get("w");//!<start posing is a punctuation
		int eid;
		for (int i = 0; i < len; i++) {//!< word by word
			TTreeNode n = wordList.get(i).getNode();
			if (wordList.get(i).getPos() != null
					&& wordList.get(i).getPos().length() > 0) {//!<having pre-tagged POS
				if (wordList.get(i).getPos().startsWith("nr"))
					posIds[i] = myDic.pos2Id.get("nr");
				else if (wordList.get(i).getPos().startsWith("m"))
					posIds[i] = myDic.pos2Id.get("m");
				else if (wordList.get(i).getPos().startsWith("t"))
					posIds[i] = myDic.pos2Id.get("t");
				else if (wordList.get(i).getPos().startsWith("nt"))
					posIds[i] = myDic.pos2Id.get("nt");
				else if (wordList.get(i).getPos().startsWith("ns"))
					posIds[i] = myDic.pos2Id.get("ns");
				else posIds[i] = myDic.pos2Id.get("w");
				int m = viterbiWords.size();//!< number of words to be tagged
				if (m > 0) {
					this.observeMatrix = new float[60][m];
					eid = posIds[i];
					for (int j = 0; j < m; j++) {//!< word by word
						for (int k = 0; k < 60; k++)
							//!< POS by POS
							this.observeMatrix[k][j] = 0;//!< no POS distribution
						PosDistribution tmp = viterbiWords.get(j).pd;//!< get POS distribution
						for (int k = 0; k < tmp.posId.size(); k++) {
							this.observeMatrix[tmp.posId.get(k)][j] = tmp.prob
									.get(k);
						}
					}
					myViterbi.resetPara(bid, eid, this.observeMatrix, m);//!< set parameters for Viterbi
					myViterbi.viterbiRun();//!< tagging POS
					int[] posResult = new int[m];
					myViterbi.getPos(posResult, m);//!< get POS result
					for (int k = 0; k < m; k++)
						posIds[k + i - m] = posResult[k];//!< save POS result
					//!< end viterbi
					viterbiWords.clear();//!< clear tagging result
				} else
					bid = posIds[i];
			} 
			else if (n.pd != null && n.isSinglePOS) {//!< word with only one POS
				posIds[i] = n.pd.get1stPos();//!< tag POS
				int m = viterbiWords.size();//!< number of words to be tagged
				if (m > 0) {
					this.observeMatrix = new float[60][m];
					eid = posIds[i];
					for (int j = 0; j < m; j++) {//!< word by word
						for (int k = 0; k < 60; k++)
							//!< POS by POS
							this.observeMatrix[k][j] = 0;//!< no POS distribution
						PosDistribution tmp = viterbiWords.get(j).pd;//!< get POS distribution
						for (int k = 0; k < tmp.posId.size(); k++) {
							this.observeMatrix[tmp.posId.get(k)][j] = tmp.prob
									.get(k);
						}
					}
					myViterbi.resetPara(bid, eid, this.observeMatrix, m);//!< set parameters for Viterbi
					myViterbi.viterbiRun();//!< tag POS
					int[] posResult = new int[m];
					myViterbi.getPos(posResult, m);//!< get POS result
					for (int k = 0; k < m; k++)
						posIds[k + i - m] = posResult[k];//!< save POS result
					//!< end viterbi
					viterbiWords.clear();//!< clear tagging result
				} else
					bid = posIds[i];
			} 
			else if (n.pd != null) {//!< word with multiple POSes
				viterbiWords.add(n);
			} else {//!< not in dictionary
				posIds[i] = -1;//!< no POS information
				int m = viterbiWords.size();//!< number of words to be tagged
				if (m > 0) {
					this.observeMatrix = new float[60][m];
					eid = this.myDic.pos2Id.get("w");
					for (int j = 0; j < m; j++) {//!< word by word
						for (int k = 0; k < 60; k++)
							//!< POS by POS
							this.observeMatrix[k][j] = 0;//!< no POS distribution
						PosDistribution tmp = viterbiWords.get(j).pd;//!< get POS distribution
						for (int k = 0; k < tmp.posId.size(); k++) {
							this.observeMatrix[tmp.posId.get(k)][j] = tmp.prob
									.get(k);
						}
					}
					myViterbi.resetPara(bid, eid, this.observeMatrix, m);//!< set parameters for Viterbi
					myViterbi.viterbiRun();//!< tag POS
					int[] posResult = new int[m];
					myViterbi.getPos(posResult, m);//!< get POS result
					for (int k = 0; k < m; k++)
						posIds[k + i - m] = posResult[k];//!< save POS result
					//!< end viterbi
					viterbiWords.clear();//!< clear tagging result
				} else
					bid = this.myDic.pos2Id.get("w");
			}
		}
		//!< clean up
		int m = viterbiWords.size();
		if (m > 0) {
			this.observeMatrix = new float[60][m];
			eid = this.myDic.pos2Id.get("w");
			for (int j = 0; j < m; j++) {//!< word by word
				for (int k = 0; k < 60; k++)
					//!< POS by POS
					this.observeMatrix[k][j] = 0;//!< no POS distribution
				PosDistribution tmp = viterbiWords.get(j).pd;//!< get POS distribution
				for (int k = 0; k < tmp.posId.size(); k++) {
					this.observeMatrix[tmp.posId.get(k)][j] = tmp.prob.get(k);
				}
			}
			myViterbi.resetPara(bid, eid, this.observeMatrix, m);//!< set parameters for Viterbi
			myViterbi.viterbiRun();//!< tag POS
			int[] posResult = new int[m];
			myViterbi.getPos(posResult, m);//!< get POS result
			for (int k = 0; k < m; k++)
				posIds[k + len - m] = posResult[k];//!< save POS result
			//!< end viterbi
			viterbiWords.clear();//!< clear tagging result
		}
		for (int i = 0; i < len; i++) {
			String pos;
			if (wordList.get(i).getPos() != null) {
				pos = wordList.get(i).getPos();
			} else
				pos = this.myDic.id2Pos.get(posIds[i]);
			TTreeNode n = wordList.get(i).getNode();
			if (n != null && n.hasCode() && 
					(pos.compareTo("nt") == 0 
					|| pos.compareTo("nr") == 0
					|| pos.compareTo("nz") == 0)) {
				pos += wordList.get(i).getNode().code;
			}
			else if (n != null && n.isHierarchical()) {
				pos = wordList.get(i).getNode().hier_str + "/" + pos;
			}
			if (n != null && n.isNegative()) {
//				if (this.sepWithSemicolon)
//					pos += ";~";
//				else
//					pos += "/~";
				pos += this.myDic.sep+"~";
			}
			if (n != null && n.isPositive()) {
//				if (this.sepWithSemicolon)
//					pos += ";*";
//				else
//					pos += "/*";
				pos += this.myDic.sep+"*";
			}
			posList.add(pos);
		}
	}
	
	/**
     * @brief entrance of segmentation engine
     * @param sen the sentence to be segmented
     * @param name name recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param org organization recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param addr address recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @return String segmentation result
     */
	public ArrayList<String> DoSeg(String sen, int name, int org, int addr) {
		ArrayList<String> sList = new ArrayList<String>();
		this.reset(sen);
		this.segText(this.sen, sList, false, name, org, addr);//!< Step 2
//		this.segText(this.sen, sList, true);//!< Step 2
		return sList;
	}
	
	/**
     * @brief Entrance of structured segmentation engine. This method separates a given text into 
     * 	segments. It stores the results into a list of Occurrence objects. Also required are: the 
     * 	exact document, paragraph and the sentence the segment appears in.
     * @param docs the documents to be segmented
     * @param debug whether debug information will be output
     * @param findName name recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findOrg organization recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findPlace address recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @return Segmentation result -- A list of occurrence for a given text.
     */
	public ArrayList<Occurrence> BatchSeg(String []docs, boolean debug, 
			int findName, int findOrg, int findPlace) {
		if (null == docs) return null;
		ArrayList<Occurrence> result = new ArrayList<Occurrence>();
		for (int i = 0; i < docs.length; i++) {
			ArrayList<Occurrence> tmp = this.segDoc(docs[i], debug, i, findName, findOrg, findPlace);
			if (tmp != null) result.addAll(tmp);
		}
		return result;
	}

	/**
     * @brief Separate one document into segments
     * @param doc the document to be segmented
     * @param debug whether debug information will be output
     * @param docID the id of current document
     * @param findName name recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findOrg organization recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findPlace address recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @return Segmentation result -- A list of occurrence for a given document.
     */
	private ArrayList<Occurrence> segDoc(String doc, boolean debug, int docID,
			int findName, int findOrg, int findPlace) {
		if (null == doc || doc.length() == 0) return null;
		ArrayList<Occurrence> result = new ArrayList<Occurrence>();
		String seps = "\n\r";
		String[] paras = StringUtils.split(doc, seps);
		int startPos = 0;
		for (int i = 0; i < paras.length; i++) {
			int pStartPos = startPos + doc.substring(startPos).indexOf(paras[i].charAt(0));
			ArrayList<Occurrence> pResult = this.segPara(paras[i], debug, docID, i, 
					pStartPos, findName, findOrg, findPlace);
			if (pResult != null) result.addAll(pResult);
			startPos = pStartPos+paras[i].length();
		}
		return result;
	}
	
	/**
     * @brief Separate one paragraph into segments
     * @param para the paragraph to be segmented
     * @param debug whether debug information will be output
     * @param docID the id of current document
     * @param paraID the id of current paragraph
     * @param paraStartPos the start position of this paragraph in current document
     * @param findName name recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findOrg organization recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findPlace address recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @return Segmentation result -- A list of occurrence for a given paragraph.
     */
	private ArrayList<Occurrence> segPara(String para, boolean debug, int docID, int paraID,
			int paraStartPos, int findName, int findOrg, int findPlace) {
		if (null == para || para.length() == 0) return null;
		ArrayList<Occurrence> result = new ArrayList<Occurrence>();
		String seps = "　 \t\"!?;﹕“”‘’；，﹐。、`《》？！|［］「」『』〖〗【】{}";
		String[] sens = StringUtils.split(para, seps);
		int startPos = 0;
		for (int i = 0; i < sens.length; i++) {
			char pre = ' ', next = ' ';
			int pStartPos = startPos + para.substring(startPos).indexOf(sens[i].charAt(0));
			if (pStartPos != startPos) pre = para.charAt(pStartPos-1);
			startPos = pStartPos+sens[i].length();
			if (startPos != para.length()) next = para.charAt(startPos);
			if ('《' == pre && '》' == next) {
				Occurrence occu = new Occurrence();
				occu.documentID = docID;
				occu.paragraphID = paraID;
				occu.sentenceID = i;
				occu.phase = sens[i];
				occu.startPos = paraStartPos + pStartPos;
				occu.endPos = paraStartPos + startPos;
//				occu.startPos = 0;
//				occu.endPos = sens[i].length();
				occu.POS = "nz";
				result.add(occu);
			}
			else {
				ArrayList<Occurrence> senResult = this.segSentence(sens[i], debug, docID, paraID, i,
						paraStartPos, pStartPos, findName, findOrg, findPlace);
				if (senResult != null) result.addAll(senResult);				
			}
		}
		return result;
	}

	/**
     * @brief Separate one sentence into segments
     * @param sen the sentence to be segmented
     * @param debug whether debug information will be output
     * @param docID the id of current document
     * @param paraID the id of current paragraph
     * @param senID the id of the current sentence
     * @param pStartPos the start position of this paragraph in current document
     * @param startPos the start position of this sentence in current paragraph
     * @param findName name recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findOrg organization recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @param findPlace address recognition: 0 for none, -1 for before segmentation, 
     * 	1 for after segmentation
     * @return Segmentation result -- A list of occurrence for a given sentence.
     */
	private ArrayList<Occurrence> segSentence(String sen, boolean debug, int docID, int paraID,
			int senID, int pStartPos, int startPos, int findName, int findOrg, int findPlace) {
		if (sen == null || sen.length() == 0) return null;
		ArrayList<Occurrence> result = new ArrayList<Occurrence>();
		this.reset(sen);
		ArrayList<String> tmp = new ArrayList<String>();
		this.segSen(sen, tmp, debug, findName, findOrg, findPlace);

		if(this.verifyName) this.verifyName(tmp);
		if(this.findNewWord) this.findNewWord(true);
		if(this.seg_English) this.tagEnglish(tmp);

//		int sStartPos = startPos;
		int sStartPos = 0;
		for (int k = 0; k < this.segResult.size(); k++) {
			Occurrence occu = new Occurrence();
			occu.documentID = docID;
			occu.paragraphID = paraID;
			occu.sentenceID = senID;
			occu.phase = this.segResult.get(k).word;
			occu.startPos = pStartPos + startPos + sStartPos;
			occu.endPos = occu.startPos + occu.phase.length();
			occu.POS = this.segResult.get(k).pos;
			result.add(occu);
//			sStartPos = occu.endPos;
			sStartPos += occu.phase.length();
		}
		return result;
	}
	
	/**
     * @brief This method finds entities of several possible types in the supplied texts, i.e. 
     * 	Location, Address Organization and Person. The method accepts multiple texts as input 
     * 	and outputs a list of entities for each text. It also accepts parameters such as the 
     * 	language and types of entities to extract.
     * @param docs the documents where entities will be found
     * @param languageCode the language code of the input documents. E.g. “zh” for Chinese
     * 	(this will always by “zh” now)
     * @param entitiesToExtract Type of entities to extract. (e.g. “Location”, “Address”, 
     * 	“Organization”, “Person”
     * @param extractKeywords Number of keywords to extract. Now no keyword extraction model 
     * 	available. So no keywords in the entity result
     * @return A list of entities from each of the documents. Each entity represents individual 
     * 	address, location, organization, people found within the text.
     * @note currently no keywords extraction is performed
     */
	public ArrayList<Entity> EntityExtract(String []docs, String languageCode, 
			HashSet<String> entitiesToExtract, int extractKeywords) {
		if (null == docs || null == entitiesToExtract || languageCode.compareTo("zh") != 0
				|| extractKeywords < 0)
			return null;
		ArrayList<Entity> result = new ArrayList<Entity>();
		HashMap<String, String> POS2Type = new HashMap<String, String>();
		int name = 0;
		int org = 0;
		int addr = 0;
		for (String s: entitiesToExtract) {
			if (s.compareTo("Location") == 0 || s.compareTo("Address") == 0) {
				POS2Type.put("ns", "Location or Address");
				addr = 1;
			}
			else if (s.compareTo("Organization") == 0) {
				POS2Type.put("nt", "Organization");
				org = 1;
			}
			else if (s.compareTo("Person") == 0) {
				POS2Type.put("nr", "Person");
				name = 1;
			}
		}
		ArrayList<Occurrence> oResult = this.BatchSeg(docs, false, name, org, addr);
		HashMap<String, ArrayList<EntityOccurrence>> key2list = 
			new HashMap<String, ArrayList<EntityOccurrence>>();
		for (Occurrence occu: oResult) {
			if (occu.POS.length() < 2) continue;
			String pos = occu.POS.substring(0, 2);
			if (!POS2Type.containsKey(pos)) continue;
			String key = pos + occu.phase;
			if (!key2list.containsKey(key)) {
				key2list.put(key, new ArrayList<EntityOccurrence>());
			}
			EntityOccurrence eo = new EntityOccurrence();
			eo.title = occu.phase;
			eo.type = POS2Type.get(pos);
			eo.score = occu.POS.length() > 2 ? 0.0 : 1.0;
			eo.docID = occu.documentID;
			eo.textStartLocation = occu.startPos;
			eo.textEndLocation = occu.endPos;
			key2list.get(key).add(eo);
		}
		for (String key: key2list.keySet()) {
			Entity e = new Entity();
			e.title = key.substring(2);
			e.type = POS2Type.get(key.substring(0, 2));
			e.frequency = key2list.get(key).size();
			e.entityOccurrences = key2list.get(key);
			e.score = 0.0;
			for (EntityOccurrence eo: e.entityOccurrences) {
				e.score += eo.score;
			}
			e.score /= e.frequency;
			result.add(e);
		}
		return result;
	}
}
