package org.utd.cs.mln.lmap;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.Formula;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;

public class LWMConvertor {

	public MLN mln;
	public WClause clause;
	public LogDouble conversionFactor;
	public LWMConvertor(MLN mln_)
	{
		mln = mln_;
		conversionFactor = new LogDouble(1.0,false);
	}
	public void convertMLN(){
		int originalMLNSize = mln.clauses.size();
		ArrayList<WClause> newClausestoAdd = new ArrayList<WClause>();
		mln.formulas.clear();
		for(int i=0;i<mln.clauses.size();i++)
		{
			Formula f = new Formula(i,i+1,mln.clauses.get(i).weight);
			mln.formulas.add(f);
		}
		for(WClause clause : mln.clauses)
		{
			Set<Term> terms = new HashSet<Term>();
			double v = Math.exp(clause.weight.value);
			double v1 = Math.exp(-1*v);
			LogDouble wt = new LogDouble(v1,false);
			for(Atom atom : clause.atoms)
			{
				for(Term term : atom.terms)
				{
					terms.add(term);
				}
			}
			//create a new predicate symbol for each formula
			//int curr_num_symbols = mln.symbols.size();
			int curr_num_symbols = mln.getMaxPredicateId();
			ArrayList<Integer> var_type = new ArrayList<Integer>();
			for(int i = 0 ; i < terms.size() ; i++){
				var_type.add(0);
			}
			//new symbol has weight(NSM)=1 and weight(!NSM)=weight of formula
			String newString = new String("NSM" + curr_num_symbols);
			PredicateSymbol p = new PredicateSymbol(curr_num_symbols, newString, var_type,new LogDouble(1.0,false),wt);
			mln.symbols.add(p);
			mln.setMaxPredicateId(mln.getMaxPredicateId()+1);
			updateMLN(mln.clauses.indexOf(clause),curr_num_symbols,newClausestoAdd);
		}
		//erase all LvrMLN clauses and formulas
		mln.clauses.clear();
		mln.formulas.clear();
		//update the mln with augmented clauses
		mln.clauses = newClausestoAdd;
		for(int jj=0;jj<mln.clauses.size();jj++)
		{
			if(mln.clauses.get(jj).isSatisfied())
			{
				for(int kk=0;kk<mln.clauses.get(jj).atoms.size();kk++)
				{
					if(mln.clauses.get(jj).atoms.get(kk).symbol.symbol.contains("NSM"))
					{
						mln.clauses.get(jj).removeAtom(kk);
						break;
					}
				}
			}
		}
		
		System.out.println("*************WMCONVERTOR Output****************");
		for(int i=0;i<mln.clauses.size();i++)
			mln.clauses.get(i).print();
		System.out.println("********************************************");
		mln.conversionFactor = conversionFactor;
	}
	
	private void updateMLN(int formulaIndex, int symId,
			ArrayList<WClause> newClausestoAdd) {
		
		ArrayList<Term> newAtomTerms = new ArrayList<Term>();
		ArrayList<ArrayList<AtomLocation>> newAtomTermLocs = new ArrayList<ArrayList<AtomLocation>>();
		//gather the terms for new predicate and their locations in the original LvrMLN
		getNewAtomTerms(formulaIndex,newAtomTerms,newAtomTermLocs);
		int newAtomSize = getAtomSize(newAtomTerms);
		double v = Math.exp(mln.clauses.get(formulaIndex).weight.value);
		double v1 = newAtomSize*v;
		conversionFactor = conversionFactor.multiply(new LogDouble(v1,true));
		ArrayList<ArrayList<AtomLocation>> atomLocs = new ArrayList<ArrayList<AtomLocation>>(); 
		int formulaEndIndex = mln.formulas.get(formulaIndex).MLNClauseEndIndex-1;
		int formulaStartIndex = mln.formulas.get(formulaIndex).MLNClauseStartIndex;
		//create new clauses which represents (C1 ^ C2...)->NSM [ !(C1 ^ C2...)v NSM ]
		ArrayList<WClause> tempClauses = generateAtomCombinations(formulaEndIndex,formulaStartIndex,atomLocs);
		int symbolIndex = toSymbolIndex(symId);
		appendNewPredicate(newAtomTerms, newAtomTermLocs, atomLocs,tempClauses,mln.symbols.get(symbolIndex));
		
		//create n new clauses: NSM->(C1 ^ C2...) [!NSM V C1, !NSM V C2,...]
		ArrayList<WClause> tempClauses1 = new ArrayList<WClause>();
		for(int i = 0 ; i < formulaEndIndex - formulaStartIndex + 1 ; i++){
			tempClauses1.add(new WClause());
		}
		int iter = 0;
		for(int i=formulaStartIndex;i<=formulaEndIndex;i++)
		{
			tempClauses1.set(iter++,MLN.create_new_clause(mln.clauses.get(i)));
		}
		appendNewPredicate1(newAtomTerms, newAtomTermLocs,tempClauses1,mln.symbols.get(symbolIndex));
		
		for(int i=0;i<tempClauses.size();i++)
		{
			newClausestoAdd.add(tempClauses.get(i));
		}

		for(int i=0;i<tempClauses1.size();i++)
		{
			newClausestoAdd.add(tempClauses1.get(i));
		}
	}
	
	private void appendNewPredicate1(ArrayList<Term> newTerms,
			ArrayList<ArrayList<AtomLocation>> termLocs,
			ArrayList<WClause> clauses, PredicateSymbol ps) {
		
		for(int i=0;i<clauses.size();i++)
		{
			ArrayList<Term> newSymbolTerms = new ArrayList<Term>();
			for(int t = 0 ; t < newTerms.size() ; t++){
				newSymbolTerms.add(new Term());
			}
			//for each new term, match its location in clause i's atom locations
			for(int j=0;j<termLocs.size();j++)
			{
				//check if term occurs in clause i
				AtomLocation foundLocation = null;
				for(int k=0;k<termLocs.get(j).size();k++)
				{				
					//check which element matches current location
					for(int m=0;m<clauses.get(i).atoms.size();m++)
					{
						for(int n=0;n<clauses.get(i).atoms.get(m).terms.size();n++)
						{
							AtomLocation newLocation = new AtomLocation(i,m,n);
							if(termLocs.get(j).get(k).isEqual(newLocation))
							{
								//found the correct term location
								foundLocation = termLocs.get(j).get(k);
								break;
							}
						}
						if(foundLocation!=null)
							break;
					}
				}
				if(foundLocation == null)
				{
					//use a new copy of the term j, no reuse required for new predicate symbol
					newSymbolTerms.set(j, MLN.create_new_term(newTerms.get(j)));
				}
				else
				{
					//new predicate symbol needs to reuse term from found atom location
					newSymbolTerms.set(j, clauses.get(i).atoms.get(foundLocation.atomIndex).terms.get(foundLocation.termIndex));
				}
			}
			Atom newSymAtom = new Atom(MLN.create_new_symbol(ps),newSymbolTerms);
			clauses.get(i).atoms.add(newSymAtom);
			clauses.get(i).sign.add(true);
		}
	}
	private void appendNewPredicate(ArrayList<Term> newTerms,
			ArrayList<ArrayList<AtomLocation>> termLocs,
			ArrayList<ArrayList<AtomLocation>> atomLocs,
			ArrayList<WClause> clauses, PredicateSymbol ps) {
		
		for(int i=0;i<clauses.size();i++)
		{
			ArrayList<Term> newSymbolTerms = new ArrayList<Term>();
			for(int t = 0 ; t < newTerms.size(); t++){
				newSymbolTerms.add(new Term());
			}
			//for each new term, match its location in clause i's atom locations
			for(int j=0;j<termLocs.size();j++)
			{
				AtomLocation match = getMatchedLocation(atomLocs.get(i),termLocs.get(j),clauses.get(i));
				if(match == null)
				{
					//use a new copy of the term j, no reuse required for new predicate symbol
					newSymbolTerms.set(j, MLN.create_new_term(newTerms.get(j)));
				}
				else
				{
					newSymbolTerms.set(j, clauses.get(i).atoms.get(match.atomIndex).terms.get(match.termIndex));				
				}
			}
			
			//Atom* newSymAtom = new Atom(ps,newSymbolTerms);
			Atom newSymAtom = new Atom(MLN.create_new_symbol(ps),newSymbolTerms);
			clauses.get(i).atoms.add(newSymAtom);
			clauses.get(i).sign.add(false);
		}
		
	}
	
	private AtomLocation getMatchedLocation(ArrayList<AtomLocation> atomLocs,
			ArrayList<AtomLocation> termLocs, WClause clause) {
		

		for(int n=0;n<termLocs.size();n++)
		{
			int iter =0;
			for(int k=0;k<clause.atoms.size();k++)
			{
				for(int m=0;m<clause.atoms.get(k).terms.size();m++)
				{
					if(termLocs.get(n).isEqual(atomLocs.get(iter++)))
					{
						//clause index is not really needed, can return -1 in its place
						return (new AtomLocation(-1,k,m));
					}
				}
			}
		}
		return null;
	}
	
	private int toSymbolIndex(int predId) {
		for(int i=0;i<mln.symbols.size();i++)
		{
			if(mln.symbols.get(i).id == predId)
				return i;
		}
		return 0;	
	}
	
	private ArrayList<WClause> generateAtomCombinations(int index,
			int startIndex, ArrayList<ArrayList<AtomLocation>> locations) {

		if(index == startIndex)
		{
			ArrayList<WClause> tmpClauses = new ArrayList<WClause>();
			for(int i=0;i<mln.clauses.get(index).atoms.size();i++)
			{
				Atom newAtom = MLN.create_new_atom(mln.clauses.get(index).atoms.get(i));
				ArrayList<AtomLocation> aLocVec = new ArrayList<AtomLocation>();
				for(int k=0;k<newAtom.terms.size();k++)
				{
					AtomLocation aLoc = new AtomLocation(index-startIndex,i,k);
					aLocVec.add(aLoc);
				}
				locations.add(aLocVec);
				WClause newClause = new WClause();
				newClause.atoms.add(newAtom);
				newClause.sign.add(!(mln.clauses.get(index).sign.get(i)));
				newClause.satisfied = mln.clauses.get(index).satisfied;
				newClause.hcNumCopies = (List<Integer>) DeepCopyUtil.copy((mln.clauses.get(index).hcNumCopies));
				newClause.satHyperCubes = (List<Boolean>) DeepCopyUtil.copy((mln.clauses.get(index).satHyperCubes));
				tmpClauses.add(newClause);
			}
			return tmpClauses;
		}
		ArrayList<WClause> allCombClauses = generateAtomCombinations(index-1,startIndex,locations);
		ArrayList<WClause> augmentedClauses = new ArrayList<WClause>();
		
		for(int i=0;i<mln.clauses.get(index).atoms.size();i++)
		{
			for(int j=0;j<allCombClauses.size();j++)
			{
				WClause newClause = MLN.create_new_clause(allCombClauses.get(j));
				//make a new copy of location vector associated with j
				ArrayList<AtomLocation> newLocVec = new ArrayList<AtomLocation>();
				for(int k=0;k<locations.get(j).size();k++)
					newLocVec.add(locations.get(j).get(k));
				//augment the combination
				Atom nAtom = MLN.create_new_atom(mln.clauses.get(index).atoms.get(i));
				boolean sign = !(mln.clauses.get(index).sign.get(i));
				newClause.atoms.add(nAtom);
				newClause.sign.add(sign);
				augmentedClauses.add(newClause);

				//augment the location vector
				for(int k=0;k<nAtom.terms.size();k++)
				{
					AtomLocation aLoc = new AtomLocation(index-startIndex,i,k);
					newLocVec.add(aLoc);
				}
				locations.add(newLocVec);
			}
		}
		for(int i = allCombClauses.size() - 1 ; i >= 0 ; i--){
			locations.remove(i);
		}
		return augmentedClauses;
	}
	
	private int getAtomSize(ArrayList<Term> terms) {
		int numHyperCubes = terms.get(0).domain.size();
		Set<ArrayList<Integer>> predGroundings = new HashSet<ArrayList<Integer>>();
		for(int hcId = 0 ; hcId < numHyperCubes ; hcId++){
			ArrayList<TreeSet<Integer>> varConstants = new ArrayList<TreeSet<Integer>>();
			for(Term t : terms){
				varConstants.add(new TreeSet<Integer>(t.domain.get(hcId)));
			}
			ArrayList<ArrayList<TreeSet<Integer>>> hyperCubeTuples = LiftedSplit.cartesianProd(varConstants);
				
			for(ArrayList<TreeSet<Integer>> hyperCubeTuple : hyperCubeTuples){
				ArrayList<Integer> predGrounding = new ArrayList<Integer>();
				for(int i = 0 ; i < hyperCubeTuple.size() ; i++){
					predGrounding.add(hyperCubeTuple.get(i).iterator().next());
				}
				predGroundings.add(predGrounding);
			}
		}
		return predGroundings.size();
	}
	private void getNewAtomTerms(int formulaIndex,
			ArrayList<Term> terms,
			ArrayList<ArrayList<AtomLocation>> locationIndex) {
		
		int start = mln.formulas.get(formulaIndex).MLNClauseStartIndex;
		int end = mln.formulas.get(formulaIndex).MLNClauseEndIndex;
		for(int i=start;i<end;i++)
		{	
			int relLocation = i - start;
			ArrayList<Term> completedTerms = new ArrayList<Term>();
			int currIndex = locationIndex.size();
			for(int j=0;j<mln.clauses.get(i).atoms.size();j++)
			{
				for(int k=0;k<mln.clauses.get(i).atoms.get(j).terms.size();k++)
				{
					Term term = mln.clauses.get(i).atoms.get(j).terms.get(k);
					boolean found =false;
					for(int m=0;m<completedTerms.size();m++)
					{
						if(completedTerms.get(m)==term)
						{
							found=true;
							locationIndex.get(currIndex+m).add(new AtomLocation(relLocation,j,k));
							break;
						}
					}
					if(!found)
					{
						completedTerms.add(term);
						//AtomLocation* atLoc = new AtomLocation(i,j);
						AtomLocation atLoc = new AtomLocation(relLocation,j,k);
						ArrayList<AtomLocation> tempLoc = new ArrayList<AtomLocation>();
						tempLoc.add(atLoc);
						locationIndex.add(tempLoc);
					}
				}
			}
			for(int k=0;k<completedTerms.size();k++)
				terms.add(completedTerms.get(k));		
			
		}
	}
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws PredicateNotFound 
	 */
	public static void main(String[] args) throws FileNotFoundException, PredicateNotFound {
	
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		String fn="smoke/smoke_mln_499.txt";
		LiftedPTP.queryEvidence = false;
		String filename = new String(fn);
		parser.parseInputMLNFile(filename);
		String fn2="smoke/evidence.txt";
		ArrayList<Evidence> evidList = parser.parseInputEvidenceFile(fn2);
		MlnToHyperCube mlnToHyperCube = new MlnToHyperCube();
		HashMap<PredicateSymbol,ArrayList<ArrayList<HyperCube>>> predsHyperCubeHashMap = mlnToHyperCube.createPredsHyperCube(evidList, mln);
		int origNumClauses = mln.clauses.size();
		boolean isNormal = false;
		for(int clauseId = 0 ; clauseId < origNumClauses ; clauseId++){
			mln.clauses.addAll(mlnToHyperCube.createClauseHyperCube(mln.clauses.get(clauseId), predsHyperCubeHashMap, isNormal));
		}
		for(int clauseId = origNumClauses-1 ; clauseId >= 0 ; clauseId--){
			mln.clauses.remove(clauseId);
		}
		/*
		for(WClause clause : mln.clauses)
			clause.print();
		*/
		LWMConvertor lwmConvertor = new LWMConvertor(mln);
		lwmConvertor.convertMLN();
	}

}
