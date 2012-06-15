package SegEngine.seg;

/**
* @file test.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2012
* @version 1.0.0 
* @brief 
*  
**/

import java.util.ArrayList;
import java.util.HashSet;

public class test {
	
	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub
		
		Dic myDic = new Dic("");
		myDic.Load("I:/resources/config.ini");
		if (!myDic.IsReady()) return;//!< Step 1;
		Seg seg = new Seg(myDic);
		myDic.Remove("国", Const.TTYPE_CH1);
		
		String chs0[] = {"铁道部副部长王志国在武汉", 
				"天水围国际大厦",
				"身份证",
				" 三十日在此间表示  铁道部副部长王志国毳媭 ", 
				"据湖北利川市政府介绍", 
				"据湖北武汉市政府介绍天水围国际大厦", 
				}; 
		int name = 1, org = 1, addr = 1;
		HashSet<String> eTypeSet = new HashSet<String>();
		eTypeSet.add("Location");
		eTypeSet.add("Organization");
		eTypeSet.add("Person");
		
		ArrayList<Entity> eResult = seg.EntityExtract(chs0, "zh", eTypeSet, 0);
		for (Entity e: eResult) {
			String line = "";
			line += "title: " + e.title;
			line += "\tid: " + e.id;
			line += "\ttype: " + e.type;
			line += "\tfrequency: " + e.frequency;
			line += "\tscore: " + e.score;
			System.out.println(line);
			for (int i = 0; i < e.frequency; i++) {
				line = "---\t";
				line += "title: " + e.entityOccurrences.get(i).title;
				line += "\ttype: " + e.entityOccurrences.get(i).type;
				line += "\tscore: " + e.entityOccurrences.get(i).score;
				line += "\tdocID: " + e.entityOccurrences.get(i).docID;
				line += "\tbegPos: " + e.entityOccurrences.get(i).textStartLocation;
				line += "\tendPos: " + e.entityOccurrences.get(i).textEndLocation;
				System.out.println(line);
			}
		}
		
		String chs[] = {"铁道部副部长王志国", 
				"和平和服务",
				"身份证",
				"铁道部副部长王志国毳媭  三十日在此间表示", 
				"据湖北利川市政府介绍", 
				"2007年年底", 
				"现行电脑输入《这个字》不难。\n难的是个人无法改变现行身份证办理软件\n\r和所有身份证检测终端的集成软件。这些软件如不接受《它所处》系统环境   支持的字符集以外字符，神仙也没办法",
				}; 
		ArrayList<String> sList = new ArrayList<String>();
		name = -1;
		for (int i = 0; i < chs.length; i++) {
			String sen = chs[i];
//			String sen = chs[i]+"THIS IS TEST " + i;
			sList = seg.DoSeg(sen, name, org, addr);
            for (String s: sList) {
            	System.out.print(s+" ");
            }
            System.out.println();
//        	System.out.println("\t"+seg.GetClassName());
		}
		
		termEntry te;
		te = myDic.Get("铁道部", Const.TTYPE_CORE_DIC);
		if (te != null) {
			System.out.println(te.type);
			System.out.println(te.term);
			System.out.println(te.property);
		}
		te = myDic.Get("永鼎股份", Const.TTYPE_ADD_DIC);
		if (te != null) {
			System.out.println(te.type);
			System.out.println(te.term);
			System.out.println(te.property);
		}
		te = myDic.Get("黄土路|路上", Const.TTYPE_SIMPLE_CONF);
		if (te != null) {
			System.out.println(te.type);
			System.out.println(te.term);
			System.out.println(te.property);
		}
		te = myDic.Get("市委", Const.TTYPE_PRE_CONF);
		if (te != null) {
			System.out.println(te.type);
			System.out.println(te.term);
			System.out.println(te.property);
		}
		te = myDic.Get("应有", Const.TTYPE_NEXT_CONF);
		if (te != null) {
			System.out.println(te.type);
			System.out.println(te.term);
			System.out.println(te.property);
		}
		te = myDic.Get("卫", Const.TTYPE_CH1);
		if (te != null) {
			System.out.println(te.type);
			System.out.println(te.term);
			System.out.println(te.property);
		}
		te = myDic.Get("宫崎", Const.TTYPE_J1);
		if (te != null) {
			System.out.println(te.type);
			System.out.println(te.term);
			System.out.println(te.property);
		}
//		myDic.remove("铁道部", Const.TTYPE_CORE_DIC);
//		myDic.remove("王", Const.TTYPE_CH1);
//		myDic.remove("志", Const.TTYPE_CH2);
		myDic.Remove("国", Const.TTYPE_CH3);
//		name = 1;
		sList = seg.DoSeg(chs[0], name, org, addr);
        for (String s: sList) {
        	System.out.print(s+" ");
        }
        System.out.println();
        
        te.term = "年年|年底";
        te.type = Const.TTYPE_SIMPLE_CONF;
        te.property = "1";
        
		myDic.Add(te);
		sList = seg.DoSeg(chs[5], name, org, addr);
        for (String s: sList) {
        	System.out.print(s+" ");
        }
        System.out.println("\n");

        ArrayList<Occurrence> oResult = seg.BatchSeg(chs, false, name, org, addr);
		for (Occurrence occu: oResult) {
			String rWord = chs[occu.documentID].substring(occu.startPos, occu.endPos);
			boolean state = occu.phase.compareTo(rWord) == 0;
			System.out.println(state + "\tword:"+occu.phase
					+ "\treal word: "+rWord
					+ "\tPOS: "+occu.POS
					+ "\tdocumentID "+occu.documentID
					+ "\tparagraphID "+occu.paragraphID
					+ "\tsentenceID "+occu.sentenceID
					+ "\tstartPos "+occu.startPos
					+ "\tendPos "+occu.endPos
					);
		}
	}

}