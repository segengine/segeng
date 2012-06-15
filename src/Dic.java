package SegEngine.seg;

/**
* @file Dic.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

import java.io.BufferedReader; //import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader; //import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class Dic {

	// private static Log log = LogFactory.getLog(Seg.class);

	TTree word_dic;//!< core dictionary
	TTree additional_word_dic;//!< additional dictionary
	float[][] transMatrix; //!< transfer matrix, size: N*N
	HashMap<String, Integer> pos2Id;//!< POS and corresponding id
	HashMap<Integer, String> id2Pos;//!< id and corresponding POS
	boolean ready; //!< dictionary is ready
	String sep;    //!< separation character for segmentation result

	String timeStamp;
	
	TTree conf_dic;//!< dictionary for simple conflict
	HashMap<TTreeNode, ArrayList<TTreeNode>> conf_pre_list;//!< previous word for solve complex conflict
	HashMap<TTreeNode, ArrayList<TTreeNode>> conf_next_list;//!< next word for solve complex conflict
	HashMap<TTreeNode, HashSet<TTreeNode>> not_match_name_list;//!< words not matching last name
	HashMap<String, TTreeNode> id2node;//!< from word code to word node
//	private float[] classScore;//!< the class score of each word
	String[] className = {"電子類", "體育類", "金融投資類", "汽車類", "教育類",
			"旅遊類", "醫療衛生", "時尚", "无法判断类别"};
	String[] unitend = {"街", "道", "路", "楼", "层", "号", "铺", "地下", "商场", 
			"大厦", "园", "村", "乡", "广场", "中心", "酒店", "阁", "湾", "岛", 
			"街市", "城", "山", "涌", "期", "轩", "坊", "院", "大学", "邨", 
			"墟", "里", "座", "馆", "中", "东", "西", "室", "码头", "咀", "台", 
			"山庄", "站", "围", "苑", "廊", "径", "机场", "市集",
			"地库", "巷", "百货", "市场", "商场", "总部", "舫", "段"};//!<, "行", "会", "堂"
	String[] HKdistincts = {"何文田", "杏花村", "太古", "天水围", "罗湖", "铜锣湾", 
			"元朗", "坪洲", "西环", "西湾河", "薄扶林", "半山", "石硖尾", "鲤鱼门", 
			"东涌", "火炭", "柴湾", "流浮山", "葵芳", "长沙湾", "荃湾", "北角", "屯门", 
			"大澳", "彩虹", "落马洲", "慈云山", "湾仔", "黄大仙", "赤鱲角", "将军澳", 
			"新蒲岗", "山顶", "九龙城", "大角咀", "旺角", "九龙塘", "乐富", "油麻地", 
			"筲箕湾", "观塘", "葵涌", "上水", "九龙湾", "马鞍山", "荔枝角", "大屿山", 
			"香港仔", "佐敦", "美孚", "南丫岛", "深井", "蓝田", "深水埗", "浅水湾", "大坑", 
			"油塘", "深水湾", "大埔", "粉岭", "石澳", "上环", "中环", "太和", "跑马地", 
			"蒲苔岛", "愉景湾", "鸭利洲", "长洲", "西贡", "太子", "马湾", "金钟", "青衣", 
			"沙田", "牛头角", "钻石山", "红磡", "鲗鱼涌", "赤柱", "土瓜湾", "尖沙咀", 
			"大围", "天后", "苏豪", "海滩", "黄埔新天地", "九龙", "新界"};//!<, "香港"
	String[] notNSU = {"("};
	HashSet<String> uEnd;
	HashSet<String> uStart;
	HashSet<String> uExcept;
	
	String[] notname = {"更"};
	HashSet<String> enPos;

	/**
     * @brief constructor of dictionary class
     * @param path the path of configuration file
     */
	public Dic(String path) {
		this.ready = false;
		if(path != null && path.length() > 0) this.Load(path);
	}
	

	public boolean IsReady() {
		return this.ready;
	}
	
	/**
     * @brief load all the dictionaries
     * @param path the path of configuration file
     */
	public void Load(String path) {
		this.reset();
		this.uEnd = new HashSet<String>();
		for (String s: this.unitend) uEnd.add(s);
		this.uStart = new HashSet<String>();
		for (String s: this.HKdistincts) uStart.add(s);
		this.uExcept = new HashSet<String>();
		for (String s: this.notNSU) uExcept.add(s);
		FileInputStream in;
		BufferedReader br = null;
		try {
			in = new FileInputStream(path);// 
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			if (null == br) return;
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				String[] items = StringUtils.split(line, "=");
				if (items.length != 2) continue;
				if (items[0].compareTo("AFTER_NAME") == 0) 
					this.loadAfterName(items[1]);
				else if (items[0].compareTo("ADD_DIC") == 0) 
					this.loadWordDic(items[1], this.additional_word_dic);
				else if (items[0].compareTo("CH_NAME_2") == 0) 
					this.loadNameElement(Const.CH2, items[1]);
				else if (items[0].compareTo("CH_NAME_3") == 0) 
					this.loadNameElement(Const.CH3, items[1]);
				else if (items[0].compareTo("CH_NAME_L") == 0) 
					this.loadNameElement(Const.CH1, items[1]);
				else if (items[0].compareTo("COM_CONF_NEXT") == 0) 
					this.loadConfNext(items[1]);
				else if (items[0].compareTo("COM_CONF_PER") == 0) 
					this.loadConfPre(items[1]);
				else if (items[0].compareTo("J_NAME_2") == 0) 
					this.loadNameElement(Const.J2, items[1]);
				else if (items[0].compareTo("J_NAME_3") == 0) 
					this.loadNameElement(Const.J3, items[1]);
				else if (items[0].compareTo("J_NAME_4") == 0) 
					this.loadNameElement(Const.J4, items[1]);
				else if (items[0].compareTo("J_NAME_L") == 0) 
					this.loadNameElement(Const.J1, items[1]);
				else if (items[0].compareTo("ORG_NAME") == 0) 
					this.loadOrgName(items[1]);
				else if (items[0].compareTo("POS_FRE") == 0) 
					this.loadPOSfre(items[1]);
				else if (items[0].compareTo("SIMPLE_CONF") == 0) 
					this.loadSimpleConf(items[1]);
				else if (items[0].compareTo("SPE_LAST_NAME") == 0) 
					this.loadSLastName(items[1]);
				else if (items[0].compareTo("TRANS_MATRIX") == 0) 
					this.loadTransMatrix(items[1]);
				else if (items[0].compareTo("MAIN_DIC") == 0) 
					this.loadWordDic(items[1], this.word_dic);
				else if (items[0].compareTo("W_NAME_L") == 0) 
					this.loadNameElement(Const.W1, items[1]);
				else if (items[0].compareTo("W_NAME_F") == 0) 
					this.loadNameElement(Const.WF, items[1]);
			}
			this.addSpecialWord();
			this.ready = true;			
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Load dictionaries error!");
			System.exit(0);
		}
		System.out.println("All Dictionaries Done.");
	}

	/**
     * @brief initiate all variables in dictionary class
     */
	private void reset() {
		this.timeStamp = "";
		this.word_dic = new TTree();
		this.additional_word_dic = new TTree();
		this.conf_dic = new TTree();
		this.conf_pre_list = new HashMap<TTreeNode, ArrayList<TTreeNode>>();
		this.conf_next_list = new HashMap<TTreeNode, ArrayList<TTreeNode>>();
		this.not_match_name_list = new HashMap<TTreeNode, HashSet<TTreeNode>>();
		this.id2node = new HashMap<String, TTreeNode>();
		this.enPos = new HashSet<String>();
		this.enPos.add("nx");
		this.pos2Id = new HashMap<String, Integer>();
		this.id2Pos = new HashMap<Integer, String>();
		this.transMatrix = new float[60][60];
		this.sep = "/";
	}

	/**
     * @brief add special words to dictionary
     */
	private void addSpecialWord() {
		// some special words
		long flag;
		String[] times = { "世纪", "年", "季度", "月", "月份", "星期", "日", "小时", 
				"时", "月底", "月初", "年期", "年度", "年底", "年中", 
				"分", "分钟", "秒", "秒钟", "点钟", "年前", "年后", "年间", "年来",
				"年多来", "年内", "周年", "週年"};
		String[] places = { "市", "区", "县", "乡", "镇", "村", "州", "邦", "街",
				"街区", "大街", "路", "道", "屯", "岗", "关", "岭", "江", "河", "湖", 
				"海", "洋", "岛", "堡", "城", "洲" , "寺", "巷", "坡", "池", "塘", 
				"港", "直街", "园"};//"省", 
		String[] betweenNames = { "-", "·" };
		String[] namePrefixes = { "老", "大", "小", "姓" };
		String[] nameSuffixes = { "父", "兄", "母", "妻", "姨", "嫂", "氏", "某", 
				"天王", "特派员", "警司", "市长", "主任", "宗亲", 
//				"姓",
				"师傅", "师奶", "师母", "同志", "老师", "连长", "大姐", "大妈", "大爷",
				"婶", "妈", "爷", "爷爷", "奶奶", "先生", "太太", "夫人", "小姐", "公子", "女士",
//				"儿子", "女儿", "丈夫", "孙子", "孙女"
				};
		String[] strongWord = { "新", "时", "敬", "亦" };
		String[] strongPrefix = { "最", "和", "拟", "原", "不", "正", "有", "谁",
				"可", "所", "中", "着", "等", "更", "得", "还", "来", "会", "地", 
				"这", "相", "末", "并", "很", "是", "应", "要", "处", "号"};
		String[] strongSuffix = { "上", "和", "下", "中", "了", "前", "后", "将",
				"向", "的", "是", "为", "成", "时", "有", "指", "主席", "主任", "者",
				"地", "里", "不", "会"};
		String[] containCHLN = {"和黄", "家童", "永和"};
		String[] notName = {"家", "府"};
//		String[] orgEnd = {"公司", "厂"};
//		String[] afterName = {"父母", "一行", "身上", "儿子", "女儿", "父亲", "母亲", "夫妇", "记者", "老人",
//				"书记", "主席", "第一", "教授", "院士", "书画", "晚报", "律师", "博士", "校长", "家族", 
//				"委员", "一家", "电话", "多年", "书法", "受访", "油田", "议员", "个人", "故居", "年龄", 
//				"图片", "经理", "部长"};
		String[] preName = {"英雄", "关心"};
//		String[] finalWord = {"家", "府"};
		flag = Const.IS_STRONG_SUFFIX;
		for (String s : strongSuffix) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}
		flag = Const.IS_TIME_END;
		for (String s : times) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}
		flag = Const.CONTAIN_CH_LASTNAME;
		for (String s : containCHLN) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}
		flag = Const.IS_PLACE_END;
		for (String s : places) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}
		flag = Const.IS_BETWEEN_NAME;
		for (String s : betweenNames) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}
		flag = Const.IS_NAME_PREFIX;
		for (String s : namePrefixes) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}

		flag = Const.IS_STRONG_WORD;
		for (String s : strongWord) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}

		flag = Const.IS_STRONG_PREFIX;
		for (String s : strongPrefix) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}

		flag = Const.IS_NAME_SUFFIX;
		for (String s : nameSuffixes) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}

		flag = Const.IS_NOT_NAME;
		for (String s : notName) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}

		flag = Const.IS_PRE_NAME;
		for (String s : preName) {
			TTreeNode n = this.word_dic.string2Node(s);
			if (n == null) {
				this.word_dic.insertWord(null, this.word_dic.root, s, 0, flag);
			} else {
				n.flag |= flag;
			}
		}
	}
	

	/**
     * @brief show words in main dictionary, simple configuration and complex configuration
     */
	private void showDics() {
		this.word_dic.ShowTree(this.word_dic.root, "");
		System.out.println("-----------------------------------");
		this.conf_dic.ShowTree(this.conf_dic.root, "");
		System.out.println("-----------------------------------");
		for (TTreeNode n : this.conf_pre_list.keySet()) {
			System.out.println(this.word_dic.node2String(n));
			for (TTreeNode n1 : this.conf_pre_list.get(n)) {
				System.out.println("\t" + this.word_dic.node2String(n1));
			}
		}
		System.out.println("-----------------------------------");
		for (TTreeNode n : this.conf_next_list.keySet()) {
			System.out.println(this.word_dic.node2String(n));
			for (TTreeNode n1 : this.conf_next_list.get(n)) {
				System.out.println("\t" + this.word_dic.node2String(n1));
			}
		}
	}

	/**
     * @brief delete word in main dictionary or additional dictionary
     * @param word the word to be deleted
     * @param tree where the word is located
     */
	private void delete(String word, TTree tree) {
		TTreeNode n = tree.string2Node(word);
		if (n != null) {
			n.isTerminal = false;
		}
	}
	
	/**
     * @brief add word to assigned dictionary
     * @param word the word to be added
     * @param dic where the word is going to be added
     */
	private void addWord(String word, TTree dic) {
		String line = word+"\t1\t(n,1)";
		this.addLine(line, dic);
	}
		
	/**
     * @brief update word in given dictionary
     * @param line the word and related information to be updated
     * @param tree the dictionary to be updated
     */
	private void updata(String line, TTree tree) {
		if (line.length() == 0 || line.charAt(0) == '#') return;
		String[] words = StringUtils.split(line, "\t");
		this.delete(words[0], tree);
		this.addLine(line, tree);
	}
	
	/**
     * @brief show word in main dictionary or additional dictionary
     * @param word the word or its id(word code) to be shown
     * @return the information of the word in the dictionary
     */
	private String showWord(String word, TTree dic) {
		TTreeNode n = null;
		String result = "";
		if (this.id2node.containsKey(word)) {
			n = this.id2node.get(word);
			result = this.word_dic.node2String(n)+"\t";
		}
		else {
			n = dic.string2Node(word);
			result = word+"\t";
		} 
		if (n == null) return "";
		if (n.pd != null) {
			for (int i = 0; i < n.pd.posId.size(); i ++) {
				result += "("+this.id2Pos.get(n.pd.posId.get(i))+","+n.pd.count.get(i)+")\t";
			}
		}
		if (n.isHierarchical())//hier
			result += n.hier_str+"\t";
		if (n.hasCode()){//code
			result += "<";
			result += n.code[0];
			for (int i = 1; i < n.code.length; i++) result += "-"+n.code[i];
			result += ">\t";
		}
		if (n.isPositive())//positive
			result += "*\t";
		if (n.isNegative())//negative
			result += "~\t";
		return result;
	}
	
	/**
     * @brief add property to given word in dictionary
     * @param word the word to which the property is added 
     * @param flag the property to be added to the word
     */
	private void addFlag(String word, long flag) {
		TTreeNode n = this.word_dic.string2Node(word);
		if (null == n) {
			n = this.additional_word_dic.string2Node(word);
			if (null == n) 
				this.word_dic.insertWord(null, this.word_dic.root, word, 0, 0);
		}
		n.flag |= flag;
	}
	
	/**
     * @brief add a word and its related information to given dictionary, if the word has
     * 	already existed in the dictionary this function will update its information
     * @param line the word and its information (format is same as dictionary)
     * @param dic the dictionary where the word should be added
     * @return the ternary tree node of the word in the dictionary
     */
	private TTreeNode addLine(String line, TTree dic) {
		if (null == line || null == dic) return null;
		long flag = 0;
		if (this.additional_word_dic == dic) flag = Const.IS_A_WORD;
		String[] items = StringUtils.split(line, "\t");
		if (items.length < 2) return null;
//		if (items[0].compareTo("长王") == 0)
//			flag = 0;
		if (items[1].charAt(0) == '2') flag |= Const.IS_SINGLENAME;
		dic.insertWord(null, dic.root, items[0], 0, flag);
		TTreeNode n = dic.terminal;
		for (int i = 2; i < items.length; i++) {
			if (items[i].charAt(0) == '(') {//!<POS
				this.addPOSDis(n, items[i], items[0].length());
			}
			else if (items[i].charAt(0) == '[') {//!<hierarchical segment result
				n.flag |= Const.IS_HIERARCHICAL;
				n.hier_str = new String(items[i].substring(1,items[i].length()-1));
			}
			else if (items[i].charAt(0) == '*') {//!<positive
				n.flag |= Const.IS_POSITIVE;
			}
			else if (items[i].charAt(0) == '~') {//!<negative
				n.flag |= Const.IS_NEGATIVE;
			}
			else if (items[i].charAt(0) == '<') {//!<word code
				n.flag |= Const.HAS_CODE;
				String[] codes = StringUtils.split(items[i], "<->");
				this.setCode(n, codes);
			}
			else if (items[i].charAt(0) == '{') {//!<class score
				String[] scores = StringUtils.split(items[i], "{;}");
				this.setScore(n, scores);
			}
			else if (items[i].charAt(0) == '!') {//!<weight
				n.weight = new String(items[i]);
			}
		}
		if (n.pd != null) {
			n.isSinglePOS = n.pd.normalize();
			n.fre = n.pd.getSum();
		}
		return n;
	}
	
	/**
     * @brief load dictionary from given path
     * @param path where the dictionary file is located
     * @param dic which dictionary in the class will receive data
     */
	private void loadWordDic(String path, TTree dic) {
		FileInputStream in;
		BufferedReader br = null;
		try {
			in = new FileInputStream(path);//
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				if (line.startsWith("**")) {
					this.timeStamp = line.substring(2);
					continue;
				}
				this.addLine(line, dic);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" fail.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}
	
	/**
     * @brief set the class scores of given word
     * @param n the ternary tree node of the word
     * @param scores the class scores of the word
     */
	private void setScore(TTreeNode n, String[] scores) {
		if (null == n || null == scores || scores.length > Const.CLASS_NUM) return;
		n.classScore = new float[Const.CLASS_NUM];
		for (int i = 0; i < scores.length; i++) {
			if (scores[i].startsWith("!")) break;
			n.classScore[i] = Float.parseFloat(scores[i]);
		}
	}
	
	/**
     * @brief set the word code list to given word
     * @param n the ternary tree node of the word
     * @param src the code list of the word
     */
	private void setCode(TTreeNode n, String[] src) {
		if (null == n || null == src) return;
		n.code = new String[src.length];
		for (int i = 0; i < src.length; i++){
			n.code[i] = new String(src[i]);
			this.id2node.put(src[i], n);
		}
	}

	/**
     * @brief add POS distribution of given word to dictionary
     * @param n the ternary tree node of the word
     * @param info the POS of the word and related POS count
     * @param len the length of the word
     */
	private void addPOSDis(TTreeNode n, String info, int len) {
		String[] tmp = StringUtils.split(info, "(,)");
		if (tmp.length != 2) return ;
		if (null == n.pd) n.pd = new PosDistribution();
		String pos = tmp[0];
		if (pos.compareTo("nrf") == 0)
			n.flag |= Const.IS_WEST_1_NAME;
		if (pos.startsWith("nr") && pos.length() > 2) {
			pos = "nr";
			n.flag |= Const.IS_NAME;
		}
		if (!this.pos2Id.containsKey(pos)) {//非法词性
			if (n.pd.posId.size() == 0) 
				n.pd = null;
			return ;
		}
		
		if (pos.compareTo("np") == 0 || pos.compareTo("nd") == 0
				|| pos.compareTo("nt") == 0 || pos.compareTo("nz") == 0
				|| pos.compareTo("ns") == 0 || len >= 4)
			// if (len >= 5)
			n.flag |= Const.IS_LONG_WORD;
		if (len == 1 && (pos.compareTo("p") == 0 || pos.compareTo("c") == 0 
						|| pos.compareTo("u") == 0))
			n.flag |= Const.IS_STRONG_WORD;
		if (pos.compareTo("v") == 0 || pos.compareTo("t") == 0) {
			n.flag |= Const.IS_VERBORTIME;
			n.flag |= Const.IS_AFTER_NAME;
		}
		if (pos.compareTo("np") == 0)
			n.flag |= Const.IS_PRE_NAME;
		if (pos.compareTo("m") == 0)
			n.flag |= Const.IS_M;
		if (pos.compareTo("q") == 0)
			n.flag |= Const.IS_Q;
		if (pos.compareTo("nx") == 0)
			n.flag |= Const.IS_NX;
		if (pos.compareTo("j") == 0)
			n.flag |= Const.IS_J;
		if (pos.compareTo("ns") == 0)
			n.flag |= Const.IS_AFTER_NAME;
		if (pos.compareTo("p") == 0)
			n.flag |= Const.IS_AFTER_NAME;
		if (pos.compareTo("d") == 0)
			n.flag |= Const.IS_AFTER_NAME;
		if (pos.compareTo("f") == 0)
			n.flag |= Const.IS_AFTER_NAME;
		if (pos.compareTo("a") == 0)
			n.flag |= Const.IS_AFTER_NAME;
		if (pos.compareTo("r") == 0)
			n.flag |= Const.IS_AFTER_NAME;
		if (pos.compareTo("c") == 0)
			n.flag |= Const.IS_AFTER_NAME;
		if (pos.compareTo("np") == 0 || pos.compareTo("nd") == 0
				|| pos.compareTo("nt") == 0 || pos.compareTo("nz") == 0
				|| pos.compareTo("ns") == 0)// len >= 4
			n.flag |= Const.IS_LONG_WORD;
		int posID = this.pos2Id.get(pos);
		int fre = Integer.parseInt(tmp[1]);
		n.pd.add(posID, fre);
	}

	/**
     * @brief load word lists that do not match last name into dictionary
     * @param path where the resource file is located
     */
	private void loadSLastName(String path) {
		FileInputStream in;
		BufferedReader br = null;
		try {
			in = new FileInputStream(path);//
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			TTreeNode cNode = null;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				if (line.charAt(0) != '\t') 
					cNode = this.word_dic .string2Node(line);
				else if (cNode != null) {
					for (int i = 1; i < line.length(); i++) {
						TTreeNode n = this.word_dic.string2Node(line.substring(i,i+1));
						if (n != null) {
							if (this.not_match_name_list.containsKey(n)) {
								this.not_match_name_list.get(n).add(cNode);
							} else {
								HashSet<TTreeNode> tmp = new HashSet<TTreeNode>();
								tmp.add(cNode);
								this.not_match_name_list.put(n, tmp);
							}
						}
					}
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}

	/**
     * @brief load word lists that often appear after name into dictionary
     * @param path where the resource file is located
     */
	private void loadAfterName(String path) {
		FileInputStream in;
		BufferedReader br = null;
		try {
			in = new FileInputStream(path);//!< 
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			long flag = Const.IS_AFTER_NAME;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				TTreeNode n = this.word_dic.string2Node(line);
				if (n == null) {
//					this.word_dic.insertWord(null, this.word_dic.root, line, 0, flag);
				} else {
					n.flag |= flag;
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}

	/**
     * @brief load simple conflict and corresponding solution into dictionary
     * @param path where the resource file is located
     */
	private void loadSimpleConf(String path) {
//		if(true) return;
		FileInputStream in;
		BufferedReader br = null;
		try {
			in = new FileInputStream(path);//
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				String[] words = StringUtils.split(line, "\t"); 
				if (words.length != 2) return;
				this.conf_dic.insertWord(null, this.conf_dic.root, words[0], 0, 0); 
				TTreeNode node = this.conf_dic.terminal; 
				node.weight = new String(words[1]); 
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}

	/**
     * @brief load preceded word lists of complex conflict into dictionary
     * @param path where the resource file is located
     */
	private void loadConfPre(String path) {
//		if(true) return;
		FileInputStream in;
		BufferedReader br = null;
		try {
			TTreeNode cnode = null;
			in = new FileInputStream(path);//!< 
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				if (line.charAt(0) != '\t') {//!< 中心词
					cnode = this.word_dic.string2Node(line);
					if (null == cnode) {
						this.addWord(line, this.word_dic);
						cnode = this.word_dic.terminal;
					}
					ArrayList<TTreeNode> tmpList = new ArrayList<TTreeNode>();
					this.conf_pre_list.put(cnode, tmpList);
				} else {//!< 前导词
					line = line.substring(1);
					TTreeNode n = this.word_dic.string2Node(line);
					if (null == n) {
						this.addWord(line, this.word_dic);
						n = this.word_dic.terminal;
					}
					if (cnode == null) {
						System.out.println("Error");
						break;
					}
					this.conf_pre_list.get(cnode).add(n);
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}

	/**
     * @brief load next word lists of complex conflict into dictionary
     * @param path where the resource file is located
     */
	private void loadConfNext(String path) {
//		if(true) return;
		FileInputStream in;
		BufferedReader br = null;
		try {
			TTreeNode cnode = null;
			in = new FileInputStream(path);//!< 
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				if (line.charAt(0) != '\t') {//!< 中心词
					cnode = this.word_dic.string2Node(line);
					if (null == cnode) {
						this.addWord(line, this.word_dic);
						cnode = this.word_dic.terminal;
					}
					ArrayList<TTreeNode> tmpList = new ArrayList<TTreeNode>();
					this.conf_next_list.put(cnode, tmpList);
				} else {//!< 后续词
					line = line.substring(1);
					TTreeNode n = this.word_dic.string2Node(line);
					if (null == n) {
						this.addWord(line, this.word_dic);
						n = this.word_dic.terminal;
					}
					if (cnode == null) {
						System.out.println("Error");
						break;
					}
					this.conf_next_list.get(cnode).add(n);
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}

	/**
     * @brief load name elements into dictionary
     * @param id indicate which element will be loaded
     * @param path where the resource file is located
     */
	private void loadNameElement(int id, String path) {
		long flag = 0;
		String code = "gbk";
		String pos = "nr";
		switch (id) {
		case Const.CH1: {
			flag = Const.IS_CH_LASTNAME;
			code = "utf-8";
			break;
		}
		case Const.CH2: {
			flag = Const.IS_CH_2_NAME;
			code = "utf-8";
			break;
		}
		case Const.CH3: {
			flag = Const.IS_CH_3_NAME;
			break;
		}
		case Const.J1: {
			flag = Const.IS_J_LASTNAME;
			pos = "nrj";
			break;
		}
		case Const.J2: {
			flag = Const.IS_J_2_NAME;
			pos = "nrj";
			break;
		}
		case Const.J3: {
			flag = Const.IS_J_3_NAME;
			pos = "nrj";
			break;
		}
		case Const.J4: {
			flag = Const.IS_J_4_NAME;
			pos = "nrj";
			break;
		}
		case Const.W1: {
			flag = Const.IS_WEST_1_NAME;
			pos = "nrf";
			break;
		}
		case Const.WF: {
			flag = Const.IS_WEST_F_NAME;
			code = "utf-8";
			pos = "nrf";
			break;
		}
		default:
			return;
		}
		FileInputStream in;
		BufferedReader br = null;
		try {
			TTreeNode n = null;
			in = new FileInputStream(path);//!<
			br = new BufferedReader(new InputStreamReader(in, code));
			String line = null;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				n = this.word_dic.string2Node(line);
				if (null == n) {
					this.word_dic.insertWord(null, this.word_dic.root, line, 0, flag);
					this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", line.length());
				} else
					n.flag |= flag;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}

	/**
     * @brief load suffix of organization name into dictionary
     * @param path where the resource file is located
     */
	private void loadOrgName(String path) {
		FileInputStream in;
		BufferedReader br = null;
		try {
			TTreeNode n = null;
			long flag = 0;
			in = new FileInputStream(path);//!< 
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				if (line.charAt(0) == '(') {
					line = line.substring(1);
					flag = Const.IS_NS_END;
				}
				else if (line.charAt(0) == ')') {
					line = line.substring(1);
					flag = Const.IS_ORG_EXCLUDE;
				}
				else {
					flag = Const.IS_ORG_NAME;
					if (line.endsWith("公司") || line.endsWith("厂"))
						flag |= Const.IS_ORG_END;
				}
				n = this.word_dic.string2Node(line);
				if (n == null) {
					this.word_dic.insertWord(null, this.word_dic.root, line, 0, flag);
					this.addPOSDis(this.word_dic.terminal, "(n,1)", line.length());
				} else
					n.flag |= flag;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}
		
	/**
     * @brief load POS and corresponding frequency into dictionary
     * @param path where the resource file is located
     */
	private void loadPOSfre(String path) {
		FileInputStream in;
		BufferedReader br = null;
		try {
			in = new FileInputStream(path);//!< 
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				String pos = line.substring(0, line.indexOf('('));
				int id = Integer.parseInt(line.substring(line.indexOf(')') + 1,
						line.indexOf('\t')));
				this.pos2Id.put(pos, id);
				this.id2Pos.put(id, pos);
			}
			this.pos2Id.put("?", -1);
			this.id2Pos.put(-1, "?");
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}
	
	/**
     * @brief load the POS transfer matrix into dictionary
     * @param path where the resource file is located
     */
	private void loadTransMatrix(String path) {
		FileInputStream in;
		BufferedReader br = null;
		try {
			in = new FileInputStream(path);//!< 
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line = null;
			int id = 0;
			System.out.print(path+"...");
			while ((line = br.readLine()) != null) {
				String[] fres = StringUtils.split(line, "\t");
				float sum = 0;
				for (int j = 0; j < 60; j++) {
					if (j < pos2Id.size() - 1) {
						this.transMatrix[id][j] = Float.parseFloat(fres[j]);
						this.transMatrix[id][j] = Float.parseFloat(fres[j]);
						sum += this.transMatrix[id][j];
					} else {
						this.transMatrix[id][j] = 0;
						this.transMatrix[id][j] = 0;
					}
				}
				for (int j = 0; j < 60; j++) {
					this.transMatrix[id][j] /= sum;
				}
				id++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(" failed.");
			System.exit(0);
		}
		System.out.println(" OK.");
	}
	
	/**
     * @brief get the time stamp of the dictionary
     * @return the time stamp of the dictionary
     */
	private String getTimeStamp() {
		return this.timeStamp;
	}
	
	/**
     * @brief get the code to word node map
     * @return the code to word node map
     */
	private HashMap<String, TTreeNode> getId2Node() {
		return this.id2node;
	}
		
	/**
     * @brief show all words in a string that have the same character to that of the string
     * @param start whether start from root of ternary tree
     * @param n ternary tree node to check the current character
     * @param s the string to be searched
     * @param result to save the found words
     */
	private void showWord(boolean start, TTreeNode n, String s, ArrayList<String> result) {
		if (start) n = this.word_dic.root;
		if (n == null) return;
		if (n.isTerminal && s.length() == 1) {
//!<			System.out.println(s+n.c);
			result.add(s+n.c);
		}
		this.showWord(false, n.left, s, result);
		this.showWord(false, n.right, s, result);
		this.showWord(false, n.middle, s+n.c, result);
	}

	/**
     * @brief show all words in a string that have the same character to that of the string
     * @param start whether start from root of ternary tree
     * @param n ternary tree node to check the current character
     * @param s the string to be searched
     * @param result to save the found words
     */
	private void showWord1(boolean start, TTreeNode n, String s, ArrayList<String> result) {
		if (start) n = this.word_dic.root;
		if (n == null) return;
		if (n.isTerminal && s.length() == 1 ) {
			String tmp = ""+n.c;
			TTreeNode tmpN = this.word_dic.string2Node(tmp);
			if (tmpN != null && tmpN.isCH3rddName()) {
//!<				System.out.println(s+n.c);
				result.add(s+n.c);
			}
		}
		if (s.length() == 0 && n.isCH2ndName())this.showWord1(false, n.middle, s+n.c, result);
		this.showWord1(false, n.left, s, result);
		this.showWord1(false, n.right, s, result);
	}
	
	private void addName(boolean start, TTreeNode n, String s) {
		if (start) n = this.word_dic.root;
		if (n == null) return;
		if (n.isTerminal && s.length() == 1 ) {
			String tmp = ""+n.c;
			TTreeNode tmpN = this.word_dic.string2Node(tmp);
			if (tmpN != null && tmpN.isCH3rddName()) {
				n.flag |= Const.IS_NAME;
			}
		}
		if (s.length() == 0 && n.isCH2ndName())this.addName(false, n.middle, s+n.c);
		this.addName(false, n.left, s);
		this.addName(false, n.right, s);
	}
	
	private void addOriginName(boolean start, TTreeNode n, String s) {
		if (start) n = this.word_dic.root;
		if (n == null) return;
		if (n.isTerminal && s.length() == 1 && n.isName() && !n.isPreName()) {
			String tmp = ""+n.c;
			TTreeNode tmpN = this.word_dic.string2Node(tmp);
			if (tmpN != null && tmpN.isCH2ndName()) {
				n.flag |= Const.IS_ORIGIN_NAME;
			}
		}
		if (s.length() == 0 && n.isCHLastName())this.addOriginName(false, n.middle, s+n.c);
		this.addOriginName(false, n.left, s);
		this.addOriginName(false, n.right, s); 
	}
	
	/**
     * @brief get term information of given term string and term type
     * @param termStr the term string to be inquired
     * @param type the term type to be inquired
     * @return the found term information
     */
	public termEntry Get(String termStr, int type) {
		if (null == termStr) return null;
		termEntry te = new termEntry(termStr);
		te.type = type;
		switch (type) {
		case Const.TTYPE_CORE_DIC: {
			String s = this.showWord(termStr, this.word_dic);
			if (s.length() == 0) {
				te = null;
				break;
			}
			int k = s.indexOf('\t');
			te.property = s.substring(k);
			break;
		}
		case Const.TTYPE_ADD_DIC: {
			String s = this.showWord(termStr, this.additional_word_dic);
			if (s.length() == 0) {
				te = null;
				break;
			}
			int k = s.indexOf('\t');
			te.property = s.substring(k);
			break;
		}
		case Const.TTYPE_SIMPLE_CONF: {
			TTreeNode n = this.conf_dic.string2Node(termStr);
			if (n == null) {
				te = null;
				break;
			}
			te.property = n.weight;
			break;
		}
		case Const.TTYPE_PRE_CONF: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !this.conf_pre_list.containsKey(n)) {
				te = null;
				break;
			}
			for (TTreeNode n1: this.conf_pre_list.get(n)) {
				te.property += "\t"+this.word_dic.node2String(n1)+"\n";
			}
			break;
		}
		case Const.TTYPE_NEXT_CONF: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !this.conf_next_list.containsKey(n)) {
				te = null;
				break;
			}
			for (TTreeNode n1: this.conf_next_list.get(n)) {
				te.property += "\t"+this.word_dic.node2String(n1)+"\n";
			}
			break;
		}
		case Const.TTYPE_CH1: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isCHLastName()) {
				te = null;
			}
			break;
		}
		case Const.TTYPE_CH2: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isCH2ndName()) {
				te = null;
			}
			break;
		}
		case Const.TTYPE_CH3: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isCH3rddName()) {
				te = null;
			}
			break;
		}
		case Const.TTYPE_J1: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isJLastName()) {
				te = null;
			}
			break;
		}
		case Const.TTYPE_J2: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isJ2ndName()) {
				te = null;
			}
			break;
		}
		case Const.TTYPE_J3: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isJ3rdName()) {
				te = null;
			}
			break;
		}
		case Const.TTYPE_J4: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isJ4thName()) {
				te = null;
			}
			break;
		}
		case Const.TTYPE_W1: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isWest1stName()) {
				te = null;
			}
			break;
		}
		case Const.TTYPE_WF: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n == null || !n.isWestFollowName()) {
				te = null;
			}
			break;
		}
		default: te = null;
		}
		return te;
	}
	
	/**
     * @brief add term entry to proper dictionary, if the entry has already existed in the dictionary
     * 	the function will update its information
     * @param entry the term entry to be added
     * @return whether the entry is added successfully
     */
	public boolean Add(termEntry entry) {
		boolean result = false;
		if (null == entry) return result;
		switch (entry.type) {
		case Const.TTYPE_CORE_DIC: {
			String s = entry.term + entry.property;
			TTreeNode n = this.addLine(s, this.word_dic);
			if (n != null) {
				result = true;
			}
			break;
		}
		case Const.TTYPE_ADD_DIC: {
			String s = entry.term + entry.property;
			TTreeNode n = this.addLine(s, this.additional_word_dic);
			if (n != null) {
				result = true;
			}
			break;
		}
		case Const.TTYPE_SIMPLE_CONF: {
			TTreeNode n = this.conf_dic.string2Node(entry.term);
			if (null == n) {
				this.conf_dic.insertWord(null, this.conf_dic.root, entry.term, 0, 0); 
				n = this.conf_dic.terminal; 
			}
			n.weight = entry.property; 
			result = true;
			break;
		}
		case Const.TTYPE_PRE_CONF: {
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.addWord(entry.term, this.word_dic);
				n = this.word_dic.terminal;
			}
			ArrayList<TTreeNode> tmpList = new ArrayList<TTreeNode>();
			this.conf_pre_list.put(n, tmpList);
			String[] items = StringUtils.split(entry.property, "\t\n");
			for (String item: items) {
				TTreeNode n1 = this.word_dic.string2Node(item);
				if (null == n1) {
					this.addWord(item, this.word_dic);
					n1 = this.word_dic.terminal;
				}
				this.conf_pre_list.get(n).add(n1);				
			}
			result = true;
			break;
		}
		case Const.TTYPE_NEXT_CONF: {
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.addWord(entry.term, this.word_dic);
				n = this.word_dic.terminal;
			}
			ArrayList<TTreeNode> tmpList = new ArrayList<TTreeNode>();
			this.conf_next_list.put(n, tmpList);
			String[] items = StringUtils.split(entry.property, "\t\n");
			for (String item: items) {
				TTreeNode n1 = this.word_dic.string2Node(item);
				if (null == n1) {
					this.addWord(item, this.word_dic);
					n1 = this.word_dic.terminal;
				}
				this.conf_next_list.get(n).add(n1);				
			}
			result = true;
			break;
		}
		case Const.TTYPE_CH1: {
			long flag = Const.IS_CH_LASTNAME;
			String pos = "nr";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		case Const.TTYPE_CH2: {
			long flag = Const.IS_CH_2_NAME;
			String pos = "nr";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		case Const.TTYPE_CH3: {
			long flag = Const.IS_CH_3_NAME;
			String pos = "nr";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		case Const.TTYPE_J1: {
			long flag = Const.IS_J_LASTNAME;
			String pos = "nrj";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		case Const.TTYPE_J2: {
			long flag = Const.IS_J_2_NAME;
			String pos = "nrj";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		case Const.TTYPE_J3: {
			long flag = Const.IS_J_3_NAME;
			String pos = "nrj";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		case Const.TTYPE_J4: {
			long flag = Const.IS_J_4_NAME;
			String pos = "nrj";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		case Const.TTYPE_W1: {
			long flag = Const.IS_WEST_1_NAME;
			String pos = "nrf";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		case Const.TTYPE_WF: {
			long flag = Const.IS_WEST_F_NAME;
			String pos = "nrf";
			TTreeNode n = this.word_dic.string2Node(entry.term);
			if (null == n) {
				this.word_dic.insertWord(null, this.word_dic.root, entry.term, 0, flag);
				this.addPOSDis(this.word_dic.terminal, "("+pos+",1)", entry.term.length());
			} else
				n.flag |= flag;
			result = true;
			break;
		}
		default:
		}
		return result;
	}
	
	/**
     * @brief set term property of proper dictionary, if the entry does not exist in the dictionary
     * 	the function will create a new term
     * @param entry the term entry to be set
     * @return whether the entry is added successfully
     */
	public boolean Set(termEntry entry) {
		return this.Add(entry);
	}
	
	/**
     * @brief remove word from given dictionary
     * @param termStr the word to be remove
     * @param type indicate which dictionary the word might exist
     * @return whether the entry is removed successfully
     */
	public boolean Remove(String termStr, int type) {
		boolean result = false;
		if (null == termStr) return result;
		switch (type) {
		case Const.TTYPE_CORE_DIC: {
			this.delete(termStr, this.word_dic);
			result = true;
			break;
		}
		case Const.TTYPE_ADD_DIC: {
			this.delete(termStr, this.additional_word_dic);
			result = true;
			break;
		}
		case Const.TTYPE_SIMPLE_CONF: {
			this.delete(termStr, this.conf_dic);
			result = true;
			break;
		}
		case Const.TTYPE_PRE_CONF: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && this.conf_pre_list.containsKey(n)) {
				this.conf_pre_list.remove(n);
			}
			result = true;
			break;
		}
		case Const.TTYPE_NEXT_CONF: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && this.conf_next_list.containsKey(n)) {
				this.conf_next_list.remove(n);
			}
			result = true;
			break;
		}
		case Const.TTYPE_CH1: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isCHLastName()) {
				n.flag -= Const.IS_CH_LASTNAME;
			}
			result = true;
			break;
		}
		case Const.TTYPE_CH2: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isCH2ndName()) {
				n.flag -= Const.IS_CH_2_NAME;
			}
			result = true;
			break;
		}
		case Const.TTYPE_CH3: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isCH3rddName()) {
				n.flag -= Const.IS_CH_3_NAME;
			}
			result = true;
			break;
		}
		case Const.TTYPE_J1: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isJLastName()) {
				n.flag -= Const.IS_J_LASTNAME;
			}
			result = true;
			break;
		}
		case Const.TTYPE_J2: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isJ2ndName()) {
				n.flag -= Const.IS_J_2_NAME;
			}
			result = true;
			break;
		}
		case Const.TTYPE_J3: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isJ3rdName()) {
				n.flag -= Const.IS_J_3_NAME;
			}
			result = true;
			break;
		}
		case Const.TTYPE_J4: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isJ4thName()) {
				n.flag -= Const.IS_J_4_NAME;
			}
			result = true;
			break;
		}
		case Const.TTYPE_W1: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isWest1stName()) {
				n.flag -= Const.IS_WEST_1_NAME;
			}
			result = true;
			break;
		}
		case Const.TTYPE_WF: {
			TTreeNode n = this.word_dic.string2Node(termStr);
			if (n != null && n.isWestFollowName()) {
				n.flag -= Const.IS_WEST_F_NAME;
			}
			result = true;
			break;
		}
		default: result = false;
		}
		return result;
	}
}
