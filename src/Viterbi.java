package SegEngine.seg;

/**
* @file Viterbi.java
* @author SegEngine
* @date Sunday April 15 19:00:00 CST 2012
* @version 1.0.0 
* @brief 
*  
**/

public class Viterbi {
	float[][] a;        //!< transfer matrix, size: N*N
	float[][] b;        //!< observation matrix, size: N*M
	int N;				//!< number of states
	int M;           	//!< number of observations
	int[] pos;          //!< POS sequence of segmentation result
	int[][] back;       //!< back trace matrix, size:N*M
	float[][] prob;     //!< probability matrix, the maximum P(state|observation)
	int beginID;		//!< ID(s) of start POS
	int endID;			//!< ID(s) of end POS
	boolean ready;      //!< whether the initial work has been done

	/**
     * @brief constructor of Viterbi class
     * @param n the number of states
     * @param tran the transfer matrix
     */
	public Viterbi(int n, float[][] tran) {
		this.a = tran;
		this.N = n;
		ready = false;
	}

	/**
     * @brief reset the value of variables in Viterbi module
     * @param bid the POS id of the start state
     * @param eid the POS id of the end state
     * @param o the observation matrix
     * @param m the number of the observations
     */
	public void resetPara(int bid, int eid, float[][] o, int m) {
		this.beginID = bid;
		this.endID = eid;
		this.b = o;
		this.M = m;
		this.pos = new int[m];
		this.back = new int[N][m];
		this.prob = new float[N][m];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < N; j++) {
				this.back[j][i] = 0;
				this.prob[j][i] = 0;
			}
		}
		this.ready = true;
	}

	/**
     * @brief get the POS tagging result
     * @param out the array to save POS result
     * @param len the length of the output array
     * @return whether successfully obtain the POS tagging result
     */
	public boolean getPos(int[] out, int len) {
		if (len != pos.length)
			return false;
		for (int i = 0; i < len; i++)
			out[i] = pos[i];
		return true;
	}

	/**
     * @brief perform Viterbi algorithm
     * @return whether the algorithm is successful
     */
	public boolean viterbiRun() {
		if (!ready)
			return false;
		for (int i = 0; i < this.M; i++) {//!< each word
			if (i == 0) {//!< first word
				boolean ok = false;
				float maxP = 0;
				int index = -1;
				for (int j = 0; j < this.N; j++) {//!< each POS
					if (maxP < this.b[j][0]) {
						maxP = this.b[j][0];
						index = j;
					}
					if (this.a[this.beginID][j] == 0 || this.b[j][0] == 0)
						continue;
					else {
						this.prob[j][0] = this.a[this.beginID][j]
								* this.b[j][0];
						ok = true;
					}
				}
				if (!ok) {
					this.prob[index][0] = maxP;
				}
			} else {//!< following words
				boolean ok = false;
				float maxP = 0;//!< maximum observation probability
				int index = -1;//!< POS id with the maximum probability
				float maxP1 = 0;//!< maximum probability of preceding word
				int index1 = -1;//!< POS id of the preceding word with maximum probability
				for (int j = 0; j < this.N; j++) {//!< each POS
					if (this.b[j][i] == 0)
						continue;
					if (maxP < this.b[j][i]) {
						maxP = this.b[j][i];
						index = j;
					}
					float maxProb = Float.MIN_VALUE;
					for (int k = 0; k < this.N; k++) {//!< each POS preceding word
						if (this.prob[k][i - 1] == 0)
							continue;
						if (maxP1 < this.prob[k][i - 1]) {
							maxP1 = this.prob[k][i - 1];
							index1 = k;
						}
						if (this.a[k][j] == 0)
							continue;
						else {
							float tmp = this.prob[k][i - 1] * this.a[k][j];
							if (tmp > maxProb) {
								maxProb = tmp;
								this.prob[j][i] = tmp * this.b[j][i];
								this.back[j][i] = k;
								ok = true;
							}
						}
					}
				}
				if (!ok) {
					this.prob[index][i] = this.b[index][i];
					this.back[index][i] = index1;
				}
			}
		}
		//!< last word
		float maxProb = Float.MIN_VALUE;
		this.pos[this.M - 1] = -1;
		boolean ok = false;
		float maxP = 0;//!< maximum observation probability
		int index = -1;//!< POS id with the maximum probability
		for (int i = 0; i < this.N; i++) {
			if (this.prob[i][this.M - 1] == 0)
				continue;
			if (maxP < this.prob[i][this.M - 1]) {
				maxP = this.prob[i][this.M - 1];
				index = i;
			}
			if (this.a[i][this.endID] == 0)
				continue;
			else {
				float tmp = this.prob[i][this.M - 1] * this.a[i][this.endID];
				if (tmp > maxProb) {
					maxProb = tmp;
					this.pos[this.M - 1] = i;
					ok = true;
				}
			}
		}
		if (!ok) {
			this.pos[this.M - 1] = index;
		}

		//!< back trace
		if (this.pos[this.M - 1] < 0) {
			System.out.println("BackTrace Error 1...");
			return false;
		}
		for (int i = this.M - 2; i >= 0; i--) {
			this.pos[i] = this.back[this.pos[i + 1]][i + 1];
			if (this.pos[i] < 0) {
				System.out.println("BackTrace Error 2...");
				return false;
			}
		}
		return true;
	}
}
