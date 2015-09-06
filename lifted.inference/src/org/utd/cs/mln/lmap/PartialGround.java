package org.utd.cs.mln.lmap;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;

public class PartialGround {

	/**
	 * @param args
	 */
	private Map<String, PredicateSymbol> symbolTable = new HashMap<String, PredicateSymbol>();
	private int max_predicate_id;
	private MLN groundMLN;
	public PartialGround(){
		symbolTable = new HashMap<String, PredicateSymbol>();
		max_predicate_id = 0;
		groundMLN = new MLN();
	}
	
	public MLN partiallyGroundMln(MLN mln, int predId, int pos){
		//System.out.println("doing partial grounding on pred : " + predId);
		mln.setMLNPoperties();
		max_predicate_id = mln.max_predicate_id;
		ArrayList<Set<Pair>> predEquivalenceClasses = new ArrayList<Set<Pair>>();
		NonSameEquivConverter.findEquivalenceClasses(mln, predEquivalenceClasses);
		Set<Pair> eqClassToGround = null;
		Pair inPair = new Pair(predId,pos);
		for(Set<Pair> p : predEquivalenceClasses){
			if(p.contains(inPair)){
				eqClassToGround = new HashSet<Pair>(p);
				break;
			}
		}
		//System.out.println("Total number of clauses : " + mln.clauses.size());
		for(WClause clause : mln.clauses){
			Set<Term> clauseWiseTermsToGround = new HashSet<Term>();
			for(Atom atom : clause.atoms){
				for(int termId = 0 ; termId < atom.terms.size() ; termId++){
					if(eqClassToGround.contains(new Pair(atom.symbol.id,termId))){
						clauseWiseTermsToGround.add(atom.terms.get(termId));
					}
				}
			}
			if(clauseWiseTermsToGround.size() > 0)
				partiallyGroundClause(clause, new ArrayList<Term>(clauseWiseTermsToGround));
			else
				groundMLN.clauses.add(MLN.create_new_clause(clause));
			//System.out.println("Clause No. " + mln.clauses.indexOf(clause) + " done...");
		}
		groundMLN.setMLNPoperties();
		//System.out.println("After partial grounding...number of clauses : " + groundMLN.clauses.size());
		mln.clearData();
		return groundMLN;
	}
	private void partiallyGroundClause(WClause clause,
			ArrayList<Term> termsToGround) {
		
		List<WClause> groundClauses = new ArrayList<WClause>();
		int numSegments = termsToGround.get(0).domain.size();
		
		// Key : permutation, value : clauseId in groundClauses corresponding to key permutation
		Map<ArrayList<Integer>,Integer> permToClauseMap = new HashMap<ArrayList<Integer>,Integer>();
		for(int segNum = 0 ; segNum < numSegments ; segNum++){
			ArrayList<ArrayList<Integer>> permutations = permute(termsToGround, segNum);
			for (int i = 0; i < permutations.size(); i++) {
				ArrayList<Integer> perm = new ArrayList<Integer>(permutations.get(i));
				WClause new_clause = null;
				boolean newClauseAdded = false;
				// If clause corresponding to this permutation doesn't exist already, create new clause
				if(!permToClauseMap.containsKey(perm)){
					// Create new clause. This create_new_clause method is different from MLN.create_new_clause() in that it doesn't copy domain of
					// terms, satHyperCubes, hcNumCopies. It only copies atoms' structure.
					new_clause = create_new_clause(clause);
					groundClauses.add(new_clause);
					permToClauseMap.put(perm, groundClauses.size()-1);
					newClauseAdded = true;
				}
				else{
					new_clause = groundClauses.get(permToClauseMap.get(perm));
				}
				
				new_clause.satHyperCubes.add(clause.satHyperCubes.get(segNum));
				new_clause.hcNumCopies.add(clause.hcNumCopies.get(segNum));
				// Now go over each term of each atom and if it is to be grounded, ground it. Segment of term which is not to be grounded is added
				// as it is. Also create new symbols for atoms which are grounded 
				Set<Term> termsSeen = new HashSet<Term>();
				for (int j = 0; j < clause.atoms.size(); j++) {
					Atom atom = clause.atoms.get(j);
					Atom newAtom = new_clause.atoms.get(j);
					ArrayList<Integer> termIdsGrounded = new ArrayList<Integer>();
					String symbolTableKey = atom.symbol.symbol;
					for (int k = 0; k < atom.terms.size(); k++) {
						Term term = atom.terms.get(k);
						Term newTerm = newAtom.terms.get(k);
						//Find the term in the term list, and replace the newClause's
						//term domain by permutation.
						int termIndex = termsToGround.indexOf(term);
						if(termIndex == -1) {
							//The term is not grounded
							symbolTableKey += "_-1";
							
							// Populate domain according to old term
							if(!termsSeen.contains(newTerm))
							{
								newTerm.domain.add(new TreeSet<Integer>(term.domain.get(segNum)));
								termsSeen.add(newTerm);
							}
							//System.out.println("newTerm domain = "+newTerm.domain);
							continue;
						}
						if(!termsSeen.contains(newTerm)){
							//newTerm.domain.clear();
							termsSeen.add(newTerm);
							TreeSet<Integer> newSegment = new TreeSet<Integer>();
							newSegment.add(perm.get(termIndex));
							newTerm.domain.add(newSegment);
						}
						termIdsGrounded.add(k);
						symbolTableKey += "_" + perm.get(termIndex);
					}
					Collections.sort(termIdsGrounded,Collections.reverseOrder());
					for(Integer tId : termIdsGrounded){
						newAtom.activeTermsIndices.remove(tId);
					}
					// If this was a new Clause added, create new symbol for newAtom
					if(newClauseAdded == true){
						//Look up symbol table for symbol
						if(symbolTable.containsKey(symbolTableKey)) {
							newAtom.symbol = symbolTable.get(symbolTableKey);
						} else {
							PredicateSymbol symbol = create_new_symbol(newAtom.symbol);
							symbolTable.put(symbolTableKey, symbol);
							newAtom.symbol = symbol;
							
							String[] strings = symbolTableKey.split("_");
							symbol.printString = strings[0] + "(";
							for (int i1 = 1; i1 < strings.length - 1; i1++) {
								if(strings[i1].equals("-1"))
									symbol.printString += "_, ";
								else 
									symbol.printString += strings[i1] + ", ";
							}
							if(strings[strings.length - 1].equals("-1"))
								symbol.printString += "_) ";
							else 
								symbol.printString += strings[strings.length - 1] + ") ";
						}

					}
				}
			}		
		}
		groundMLN.clauses.addAll(groundClauses);
	}
	
	// Create new symbol. NOTE : This symbol's id will be 1 more than highest id till now i.e. this symbol will get new id
	private PredicateSymbol create_new_symbol(PredicateSymbol symbol) {
		List<Integer> var_types = new ArrayList<Integer>();
		for(int i=0;i<symbol.variable_types.size();i++)
			var_types.add(symbol.variable_types.get(i));
		PredicateSymbol newSymbol = new PredicateSymbol(max_predicate_id,symbol.symbol,var_types,symbol.pweight,symbol.nweight);
		
		max_predicate_id++;
		groundMLN.symbols.add(newSymbol);
		
		return newSymbol;
	}
	

	private WClause create_new_clause(WClause clause) {
		WClause new_clause = new WClause();
		new_clause.sign = new ArrayList<Boolean>(clause.sign);
		new_clause.satisfied = clause.satisfied;
		new_clause.weight = clause.weight;

		//if atoms have common terms their relationship must be maintained when new clause is created
		List<Term> newTerms = new ArrayList<Term>();
		List<Term> oldTerms = new ArrayList<Term>();

		for (int i = 0; i < clause.atoms.size(); i++) 
		{
			for (int j = 0; j < clause.atoms.get(i).terms.size(); j++)
			{
				int termPosition=-1;
				for(int m=0;m<oldTerms.size();m++)
				{
					if(oldTerms.get(m)==clause.atoms.get(i).terms.get(j))
					{
						termPosition = m;
					}
				}
				if(termPosition==-1)
				{
					Term term = new Term();
					term.type = clause.atoms.get(i).terms.get(j).type;
					// Do NOT populate term domain
					newTerms.add(term);
					oldTerms.add(clause.atoms.get(i).terms.get(j));
				}
				else
				{
					newTerms.add(newTerms.get(termPosition));
					oldTerms.add(clause.atoms.get(i).terms.get(j));
				}
			}
		}
		int ind=0;
		new_clause.atoms = new ArrayList<Atom>(clause.atoms.size());
		for (int i = 0; i < clause.atoms.size(); i++) {
			new_clause.atoms.add(new Atom());
			new_clause.atoms.get(i).symbol = MLN.create_new_symbol(clause.atoms.get(i).symbol);
			new_clause.atoms.get(i).activeTermsIndices = new ArrayList<Integer>(clause.atoms.get(i).activeTermsIndices);
			new_clause.atoms.get(i).terms = new ArrayList<Term>(clause.atoms.get(i).terms.size());
			for (int j = 0; j < clause.atoms.get(i).terms.size(); j++) {
				new_clause.atoms.get(i).terms.add(newTerms.get(ind));
				ind++;
			}
		}
		return new_clause;
	}


	private ArrayList<ArrayList<Integer>> permute(ArrayList<Term> termsToGround, int segNum) {

		int permutationSize = 1;
		for (Term term : termsToGround) {
			permutationSize *= term.domain.get(segNum).size();
		}
		
		//int[][] permutations = new int[permutationSize][termsToGround.size()];
		ArrayList<ArrayList<Integer>> permutations = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> segments = new ArrayList<ArrayList<Integer>>();
		for(int i = 0 ; i < termsToGround.size() ; i++){
			segments.add(new ArrayList<Integer>(termsToGround.get(i).domain.get(segNum)));
		}
		
		for (int i = 0; i < permutationSize; i++) {
			ArrayList<Integer> singlePerm = new ArrayList<Integer>();
			int residue = i;
			for (int j = 0; j < termsToGround.size(); j++) {
				int index = residue % segments.get(j).size();
				residue = residue / segments.get(j).size();
				singlePerm.add(segments.get(j).get(index));
			}
			permutations.add(singlePerm);
		}
		return permutations;
	}

	public static void main(String[] args) throws FileNotFoundException, PredicateNotFound {
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		String fn="/home/happy/Dropbox/experiments/approx_code/LiftedMAP/LiftedMAP/smoke/smoke_mln.txt";
		String filename = new String(fn);
		parser.parseInputMLNFile(filename);
		String queries[] = new String[]{"F"};
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
		// manual creation of hyperCubes for testing
		/*
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.clear();
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(0,1)));
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(0)));
		mln.clauses.get(0).atoms.get(0).terms.get(1).domain.clear();
		mln.clauses.get(0).atoms.get(0).terms.get(1).domain.add(new TreeSet<Integer>(Arrays.asList(1)));
		mln.clauses.get(0).atoms.get(0).terms.get(1).domain.add(new TreeSet<Integer>(Arrays.asList(2)));
		mln.clauses.get(0).satHyperCubes.clear();
		mln.clauses.get(0).satHyperCubes.add(true);
		mln.clauses.get(0).satHyperCubes.add(false);
		mln.clauses.get(0).hcNumCopies.clear();
		mln.clauses.get(0).hcNumCopies.add(1);
		mln.clauses.get(0).hcNumCopies.add(1);
		*/
		PartialGround pg = new PartialGround();
		MLN pgMln = pg.partiallyGroundMln(mln, 0, 0);
		System.out.println("partial grounding done...");
		System.out.println("Now total number of clauses : " + pgMln.clauses.size());
		System.out.println("Now total number of predicates : " + pgMln.max_predicate_id);
	}

}
