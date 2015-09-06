package org.utd.cs.mln.lmap;

public class LiftedPTPParams {

	public String inputFile="", evidenceFile="";
	public String resultFile = "", queryString="";
	public boolean isNormal = false, approximate = false, queryEvidence = false;
	public int numIter = 100;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public String toString() {
		return "LiftedPTPParams [\ninputFile=" + inputFile + "\nevidenceFile="
				+ evidenceFile + "\nresultFile=" + resultFile
				+ "\nqueryString=" + queryString + "\nisNormal=" + isNormal
				+ "\napproximate=" + approximate + "\nqueryEvidence="
				+ queryEvidence + "\nnumIter=" + numIter + "\n]";
	}
	
	
}
