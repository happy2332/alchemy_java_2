package org.utd.cs.mln.lmap;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.util.Parser;

public class LisApproxPropositional {

	
	public static LogDouble doLvApproxPropositional(MLN mln) {
		PTPUtil.simplifyCNFPropositional(mln);
		LogDouble totalVal = new LogDouble(1.0,false);
		if(mln.clauses.size() == 0){
			return totalVal;
		}
		int completedCount=0;
		for(int i=0;i<mln.clauses.size();i++)
		{
			if(mln.clauses.get(i).atoms.size()==0 || mln.clauses.get(i).isSatisfied())
			{
				completedCount++;
			}
		}
		// Base case : If all clauses are satisfied or empty
		if(completedCount == mln.clauses.size())
		{
			LogDouble weight = PTPUtil.CNFWeightPropositional(mln);
			mln.clearData();
			return weight;
		}
		ArrayList<MLN> dCNFs = new ArrayList<MLN>();
		DisjointCNFCreator.seperateDisjointCNF(mln,dCNFs);
		mln.clearData();
		for(int t=0;t<dCNFs.size();t++)
		{
			MLN CNF = dCNFs.get(t);
			Atom tmpAtom = LHeuristics.getAtomToSplit(CNF);
			if(tmpAtom==null)
			{	
				//Atom tmpAtom1 = heuristics.getAtomToSplit(CNF);
				//System.out.println(tmpAtom1);
				LogDouble wt1 = PTPUtil.CNFWeightPropositional(CNF);
				CNF.clearData();
			    totalVal = totalVal.multiply(wt1);
				continue;
			}
			LogDouble sampleWeight = new LogDouble(2.0,false);
			splitPred(CNF, tmpAtom.symbol.id);
			LogDouble mcnt = doLvApproxPropositional(CNF);
			sampleWeight = sampleWeight.multiply(mcnt);
			totalVal = totalVal.multiply(sampleWeight);
		}
		return totalVal;
	}

	private static void splitPred(MLN CNF, int predId) {
		int k = PTPUtil.sampleBinomial(1);
		for(WClause clause : CNF.clauses){
			boolean satisfied = false;
			for(int atomId = clause.atoms.size() - 1 ; atomId >= 0 ; atomId--){
				if(clause.atoms.get(atomId).symbol.id != predId)
					continue;
				if(k == 0 && clause.sign.get(atomId)==true){
					satisfied = true;
				}
				if(k == 1 && clause.sign.get(atomId)==false){
					satisfied = true;
				}
				clause.atoms.remove(atomId);
				clause.sign.remove(atomId);
			}
			if(satisfied){
				clause.hcNumCopies.set(0, clause.hcNumCopies.get(0) + clause.hcNumCopies.get(1));
				clause.hcNumCopies.set(1, 0);
			}
		}
	}
	
	/**
	 * @param args
	 * @throws PredicateNotFound 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, PredicateNotFound {
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		String fn="/home/happy/Dropbox/experiments/approx_code/LiftedMAP/LiftedMAP/smoke/smoke_mln.txt";
		String filename = new String(fn);
		parser.parseInputMLNFile(filename);
		String queries[] = new String[]{"S","C"};
		ArrayList<String> query_preds = new ArrayList<String>(Arrays.asList(queries));
		// If no query_predicate is specified, then default is all predicates are queries
		if(query_preds.size() == 0){
			for(PredicateSymbol symbol : mln.symbols){
				query_preds.add(symbol.symbol);
			}
		}
		boolean queryEvidence = false;
		String fn2="/home/happy/Dropbox/experiments/approx_code/LiftedMAP/LiftedMAP/smoke/evidence.txt";
		ArrayList<Evidence> evidList = parser.parseInputEvidenceFile(fn2);
		MlnToHyperCube mlnToHyperCube = new MlnToHyperCube();
		boolean isNormal = true;
		HashMap<PredicateSymbol,ArrayList<ArrayList<HyperCube>>> predsHyperCubeHashMap = mlnToHyperCube.createPredsHyperCube(evidList, mln, isNormal, query_preds);
		int origNumClauses = mln.clauses.size();
		for(int clauseId = 0 ; clauseId < origNumClauses ; clauseId++){
			mln.clauses.addAll(mlnToHyperCube.createClauseHyperCube(mln.clauses.get(clauseId), predsHyperCubeHashMap, isNormal));
		}
		for(int clauseId = origNumClauses-1 ; clauseId >= 0 ; clauseId--){
			mln.clauses.remove(clauseId);
		}
		// Empty out the atom hyperCubes
		for(WClause clause : mln.clauses){
			for(Atom atom : clause.atoms){
				atom.hyperCubes.clear();
			}
		}
		PartialGround pg = new PartialGround();
		MLN gMln = pg.partiallyGroundMln(mln, 0, 0);
		System.out.println(".partial grounding done..,");
		LogDouble Z = doLvApproxPropositional(gMln);
		System.out.println(Math.exp(Z.value));


	}

}
