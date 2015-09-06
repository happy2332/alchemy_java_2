package org.utd.cs.mln.lmap;

public class AtomLocation {

	public int clauseIndex;
	public int atomIndex;
	public int termIndex;
	public AtomLocation(int clauseIndex_,int atomIndex_,int termIndex_){
		clauseIndex = (clauseIndex_);
		atomIndex = (atomIndex_);
		termIndex = (termIndex_);
	}
	boolean isEqual(AtomLocation at)
	{
		if(at.atomIndex == atomIndex && at.clauseIndex == clauseIndex && at.termIndex == termIndex)
			return true;
		else
			return false;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
