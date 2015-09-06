package org.utd.cs.mln.lmap;

import org.utd.cs.mln.alchemy.core.MLN;

public class LiftedAlgsHandler {

	
	/**
	 * @param args
	 */
	public void WMPTPZExact(MLN mln){
		System.out.println("Pre-processing..Converting to Weighted Model Counting...");
		//convert to WM PTP
		LWMConvertor lw = new LWMConvertor(mln);
		lw.convertMLN();		
		System.out.println("WM Converted MLN");
		for(int i=0;i<mln.clauses.size();i++)
			mln.clauses.get(i).print();
		//LFileDump::dumpMLNToFile(&mln);
		//cout<<"done writing dump files ("<<MLNDUMPFILENAME<<","<<SYMBOLSDUMPFILENAME<<")"<<endl;
		System.out.println("Exact Z = ");
		//ptpSearch->startExactWeightedModelCounting(params).printValue();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
