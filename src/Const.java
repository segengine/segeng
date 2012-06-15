package SegEngine.seg;

/**
* @file Const.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

public class Const {
	public static final int RECENTSIZE = 20;

	public static final int DIGITAL = 1;
	public static final int ENGLISH = 2;
	public static final int NAME = 4;
	public static final int UNKNOWN = 8;
	
	public static final String[] className = {"電子類", "體育類", "金融投資類", "汽車類", "教育類",
			"旅遊類", "醫療衛生", "時尚", "无法判断类别"};
	
	public static final double EPSILON = 1 >> 5;

	public static final int CLASS_NUM = 9;
	
	// word properties
	public static final long IS_HIERARCHICAL = 1;//!< has hierarchical segmentation
	public static final long HAS_CODE = 1 << 1;//!< has code
	public static final long IS_NEGATIVE = 1 << 2;//!< is negative word
	public static final long IS_CH_LASTNAME = 1 << 3;//!< 1st character of Chinese name
	public static final long IS_CH_2_NAME = 1 << 4;//!< 2nd character of Chinese name
	public static final long IS_CH_3_NAME = 1 << 5;//!< 3rd character of Chinese name
	public static final long IS_J_LASTNAME = 1 << 6;//!< 1st character(s) of Japanese name
	public static final long IS_J_2_NAME = 1 << 7;//!< 2nd character of Japanese name
	public static final long IS_J_3_NAME = 1 << 8;//!< 3rd character of Japanese name
	public static final long IS_J_4_NAME = 1 << 9;//!< 4th character of Japanese name
	public static final long IS_WEST_1_NAME = 1 << 10;//!< 1st character(s) of west name
	public static final long IS_WEST_F_NAME = 1 << 11;//!< following character(s) of west name
	public static final long IS_BETWEEN_NAME = 1 << 12;//!< link of name
	public static final long IS_LONG_WORD = 1 << 13;//!< long word
	public static final long IS_TIME_END = 1 << 14;//!< time end
	public static final long IS_PLACE_END = 1 << 15;//!< place end
	public static final long IS_NAME_PREFIX = 1 << 16;//!< name prefix
	public static final long IS_STRONG_WORD = 1 << 17;//!< strong word (often exist alone)
	public static final long IS_STRONG_PREFIX = 1 << 18;//!< prefix of strong word
	public static final long IS_VERBORTIME = 1 << 19;//!< verb or time
	public static final long IS_NAME_SUFFIX = 1 << 20;//!< name suffix
	public static final long IS_NAME = 1 << 21;//!< is name
	public static final long IS_NOT_NAME = 1 << 22;//!< is not name
	public static final long IS_M = 1 << 23;//!< number
	public static final long IS_NX = 1 << 24;//!< English word
	public static final long IS_SINGLENAME = 1 << 25;//!< 1st two characters of Chinese name
	public static final long IS_Q = 1 << 26;//!< quantity word
	public static final long IS_A_WORD = 1 << 27;//!< word in additional dictionary
	public static final long IS_ORG_NAME = 1 << 28;//!< organization suffix
	public static final long CONTAIN_CH_LASTNAME = 1 << 29;//!< contain Chinese last name
	public static final long IS_J = 1 << 30;//!< abbreviation
	public static final long IS_NS_END = (long)1 << 31;//!< end of place name
	public static final long IS_POSITIVE = (long)1 << 32;//!< positive word
	public static final long IS_STRONG_SUFFIX = (long)1 << 33;//!< suffix of strong word
	public static final long IS_FINAL = (long)1 << 34;//!< must be the final result
	public static final long IS_MQL = (long)1 << 34;//!< MQL word, also in final result
	public static final long IS_ORG_END = (long)1 << 35; //!< end of organization
	public static final long IS_ORG_EXCLUDE = (long)1 << 36; //!> not component of organization word
	public static final long IS_AFTER_NAME = (long)1 << 37;//!< appear just after person name
	public static final long IS_ORIGIN_NAME = (long)1 << 38;//!< is person name
	public static final long IS_PRE_NAME = (long)1 << 39;//!< appear preceding to name

	public static final int CHNAME = 1;
	public static final int JNAME = 2;
	public static final int WESTNAME = 4;

	public static final int POS_THRESHOLD = 20;
	public static final int MAX_ORG_LEN = 7;
	public static final int MAX_ORG_SUB_LEN = 3;
	
	public static final int CH1 = 1;
	public static final int CH2 = 2;
	public static final int CH3 = 3;
	public static final int J1 = 4;
	public static final int J2 = 5;
	public static final int J3 = 6;
	public static final int J4 = 7;
	public static final int W1 = 8;
	public static final int WF = 9;
	
	public static final int MIN_NEW_WORD_FRE = 3;
	
	public static final int MAX_NSU_LEN = 4;
	public static final int MIN_NSU_COUNT = 3;
	
	public static final int MAX_THREAD_NUM = 2;
	public static final boolean MULTI_THREAD = true;
	
	public static final int TTYPE_CORE_DIC = 1;
	public static final int TTYPE_ADD_DIC = 2;
	public static final int TTYPE_SIMPLE_CONF = 3;
	public static final int TTYPE_PRE_CONF = 4;
	public static final int TTYPE_NEXT_CONF = 5;
	public static final int TTYPE_CH1 = 6;
	public static final int TTYPE_CH2 = 7;
	public static final int TTYPE_CH3 = 8;
	public static final int TTYPE_J1 = 9;
	public static final int TTYPE_J2 = 10;
	public static final int TTYPE_J3 = 11;
	public static final int TTYPE_J4 = 12;
	public static final int TTYPE_W1 = 13;
	public static final int TTYPE_WF = 14;
	public static final int TTYPE_S_L_NAME = 15;
}
