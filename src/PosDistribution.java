package SegEngine.seg;

/**
* @file PosDistribution.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2009
* @version 1.0.0 
* @brief 
*  
**/

import java.util.ArrayList;

class PosDistribution {
	ArrayList<Integer> posId; //!< the POS list of the word
	ArrayList<Integer> count; //!< the count of each POS
	ArrayList<Float> prob;    //!< the probability of each POS
	int maxIndex;             //!< the index of the maximum POS of the word
	int maxCount;             //!< the maximum count of all the POSes
	float sum;                //!< the sum of all POS count; it is also the frequency of the word
	boolean normalized;       //!< whether the probabilities have been normalized

	/**
     * @brief constructor of this class, initial all variables
     */
	public PosDistribution() {
		this.posId = new ArrayList<Integer>();
		this.count = new ArrayList<Integer>();
		this.prob = new ArrayList<Float>();
		this.sum = 0;
		this.normalized = false;
		this.maxIndex = -1;
		this.maxCount = -1;
	}

	/**
     * @brief add one POS and corresponding frequency
     * @param id the ID of POS
     * @param count the frequency of count
     */
	public void add(int id, int count) {
		int i = this.posId.indexOf(id);
		if (i < 0) {
			i = this.posId.size();
			this.posId.add(id);
			this.count.add(count);
			this.prob.add((float)0);
		}
		else {
			int c = this.count.get(i);
			this.count.set(i, c+count);
		}
		this.sum += count;
		if (count > this.maxCount) {
			this.maxCount = count;
			this.maxIndex = i;
		}
		this.normalized = false;
	}

	/**
     * @brief normalize the distribution of all POSes
     * @return whether the process is successful
     */
	public boolean normalize() {
		if (this.posId.size() == 0) return false;
		for (int i = 0; i < this.posId.size(); i++) {
			this.prob.set(i, (float) this.count.get(i) / sum);
		}
		this.normalized = true;
		return 1 == this.posId.size();
	}

	/**
     * @brief get the frequency of the word
     * @return the frequency of the word
     */
	public int getSum() {
		return (int) this.sum;
	}
	
	/**
     * @brief check whether the word has only one POS
     * @return true for one POS and false for multiple POSes
     */
	public boolean isSinglePos() {
		if (this.prob.size() > 0 && 
				Math.abs(this.prob.get(0)-1.0) < Const.EPSILON) 
			return true;
		return false;
	}
	
	/**
     * @brief get the 1st POS
     * @return the POS id of the 1st POS in the distribution
     */
	public int get1stPos () {
		if (this.posId.size() > 0)
			return this.posId.get(0);
		else return -1;
	}
	
	/**
     * @brief get the POS with the maximum probability or frequency
     * @return the POS id with the maximum probability or frequency
     */
	public int getMaxPos () {
		if (this.maxIndex >= 0)
			return this.posId.get(this.maxIndex);
		else return -1;
	}
}
