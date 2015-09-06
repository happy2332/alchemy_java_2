package org.utd.cs.mln.lmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Decomposer;
import org.utd.cs.mln.alchemy.core.LDecomposer;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.comb;

public class PtpExactInference {

	/*
	public MLN mln;
	public LDecomposer decomposer;
	public LHeuristics heuristics;
	*/
	/*
	public PtpExactInference(MLN mln_){
		mln = mln_;
		decomposer = new LDecomposer(mln);
		heuristics = new LHeuristics(decomposer,mln);
	}*/
	
	public LogDouble doLvExactPartitionDisjointSeg(MLN mln){
		LogDouble totalVal = new LogDouble(1.0);
		ArrayList<MLN> dCNFs = new ArrayList<MLN>();
		//DisjointCNFCreator.seperateDisjointCNF(mln,dCNFs);
		DisjointCNFCreator.seperateDisjointSegmentsCNF(mln,dCNFs);
		mln.clearData();
		for(int t=0;t<dCNFs.size();t++)
		{
			MLN CNF = dCNFs.get(t);
			LogDouble mcnt = doLvExactPartition(CNF);
			totalVal = totalVal.multiply(mcnt);
		}
		return totalVal;
	}
	
	public LogDouble doLvExactPartition(MLN mln){
		LogDouble totalVal = new LogDouble(1.0);
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
			LogDouble weight = PTPUtil.CNFWeight(mln);
			mln.clearData();
			return weight;
		}
		ArrayList<MLN> dCNFs = new ArrayList<MLN>();
		DisjointCNFCreator.seperateDisjointCNF(mln,dCNFs);
		//DisjointCNFCreator.seperateDisjointSegmentsCNF(mln,dCNFs);
		mln.clearData();
		for(int t=0;t<dCNFs.size();t++)
		{
			MLN CNF = dCNFs.get(t);
			if(CNF.isPropositionalSegments()){
				LogDouble mcnt = PTPExactPropositional.doLvExactPropositional(CNF);
				totalVal = totalVal.multiply(mcnt);
				continue;
			}
			// Skip decomposer for now
			
			ArrayList<Integer> powerFactors = new ArrayList<Integer>();
			ArrayList<MLN> decomposedMlns = PTPUtil.decomposeCNF(CNF, powerFactors);
			if(powerFactors.size() > 1 || powerFactors.get(0) > 1)
			{
				for(int mlnId = 0 ; mlnId < decomposedMlns.size() ; mlnId++){
					LogDouble mcnt = doLvExactPartition(decomposedMlns.get(mlnId));
					LogDouble val = new LogDouble(1.0);
					val = mcnt.power(powerFactors.get(mlnId));
					totalVal = totalVal.multiply(val);
				}
				continue;
			}
			Atom tmpAtom = LHeuristics.getAtomToSplitActiveTerms(CNF);
			System.out.println(tmpAtom.symbol.symbol);
			if(tmpAtom==null)
			{	
				//Atom tmpAtom1 = heuristics.getAtomToSplit(CNF);
				//System.out.println(tmpAtom1);
				LogDouble wt1 = PTPUtil.CNFWeight(CNF);
				CNF.clearData();
			    totalVal = totalVal.multiply(wt1); // Doubt here
				continue;
			}
			if(tmpAtom.activeTermsIndices.size() >= 1){
				// do partial grounding.
				// For now : do on first index of activeTermIndices
				PartialGround pg = new PartialGround();
				MLN gCNF = pg.partiallyGroundMln(CNF, tmpAtom.symbol.id, tmpAtom.activeTermsIndices.get(0));
				totalVal = totalVal.multiply(doLvExactPartition(gCNF));
				continue;
			}
			if(tmpAtom.activeTermsIndices.size() == 0){
				MLN nMln = PTPUtil.splitPropositionalPred(CNF, tmpAtom.symbol.id, 0);
				LiftedPTP.numMLNs++;
				MLN pMln = PTPUtil.splitPropositionalPred(CNF, tmpAtom.symbol.id, 1);
				LiftedPTP.numMLNs++;
				LogDouble pVal = doLvExactPartition(pMln);
				LogDouble nVal = doLvExactPartition(nMln);
				LogDouble temp = new LogDouble(nVal.value - pVal.value, true);
				//double val = Math.exp(doLvExactPropositional(pmln).value) + Math.exp(doLvExactPropositional(nmln).value);
				//LogDouble mcnt = new LogDouble(val,false);
				LogDouble mcnt = new LogDouble(1+Math.exp(temp.value),false);
				
				//double val = Math.exp(doLvExactPartition(nMln).value) + Math.exp(doLvExactPartition(pMln).value);
				//LogDouble mcnt = new LogDouble(val,false);
				mcnt = mcnt.multiply(pVal);
				totalVal = totalVal.multiply(mcnt);
				continue;
			}
			Atom atom = MLN.create_new_atom(tmpAtom);
			int splitTermPos = atom.activeTermsIndices.get(0);
			if(LiftedPTP.lazyDisjoint == true){
				ArrayList<ArrayList<TreeSet<Integer>>> allTermsDjSegments = createDisjointSegments(CNF, atom.symbol.id);
				// Create map : firstelement > segmentId
				ArrayList<HashMap<Integer, Integer>> allTermsFirstElemToSegmentMap = new ArrayList<HashMap<Integer,Integer>>();
				for(int tId = 0 ; tId < allTermsDjSegments.size() ; tId++){
					HashMap<Integer,Integer> firstElemToSegmentMap = new HashMap<Integer,Integer>();
					for(int i = 0 ; i < allTermsDjSegments.get(tId).size() ; i++){
						firstElemToSegmentMap.put(allTermsDjSegments.get(tId).get(i).iterator().next(), i);
					}
					allTermsFirstElemToSegmentMap.add(firstElemToSegmentMap);
				}
				
				updateClauseSegments(CNF, allTermsFirstElemToSegmentMap, allTermsDjSegments, atom.symbol.id);
			}
			// Collect all disjoint segments of atom
			// NOTE : we assume that atom selected is singleton
			Set<TreeSet<Integer>> djSegmentsSet = new HashSet<TreeSet<Integer>>();
			for(WClause clause : CNF.clauses){
				for(Atom a : clause.atoms){
					if(atom.symbol.id != a.symbol.id)
						continue;
					Term term = a.terms.get(splitTermPos);
					for(TreeSet<Integer> s : term.domain){
						djSegmentsSet.add(new TreeSet<Integer>(s));
					}
				}
			}
			ArrayList<TreeSet<Integer>> djSegments = new ArrayList<TreeSet<Integer>>(djSegmentsSet);
			ArrayList<Integer> binCoeffList = new ArrayList<Integer>();
			for(int i = 0 ; i < djSegments.size() ; i++){
				binCoeffList.add(0);
			}
			LogDouble val = new LogDouble(1.0);
			val = applyBinomial(CNF, djSegments, -1, binCoeffList, atom.symbol.id, splitTermPos);
			totalVal = totalVal.multiply(val);
		}
		return totalVal;
	}
	
	private LogDouble applyBinomial(MLN CNF, ArrayList<TreeSet<Integer>> djSegments, int indexProcessed, ArrayList<Integer> binCoeffList, int predId, int splitTermPos) {
		
		// Base case : when all disjoint segments have been processed
		if(indexProcessed == djSegments.size()-1){
			MLN nCNF = new MLN();
			LiftedPTP.numMLNs++;
			for(WClause clause : CNF.clauses){
				nCNF.clauses.add(MLN.create_new_clause(clause));
				//nCNF.symbols = (List<PredicateSymbol>) DeepCopyUtil.copy(CNF.symbols);
				//nCNF.max_predicate_id = CNF.max_predicate_id;
			}
			nCNF.setMLNPoperties();
			ArrayList<Set<Pair>> predEquivalenceClasses = new ArrayList<Set<Pair>>();
			NonSameEquivConverter.findEquivalenceClasses(nCNF, predEquivalenceClasses);
			Map<Integer,Integer> firstElemSegmentsToBinCoeff = new HashMap<Integer,Integer>();
			for(int segId = 0 ; segId < djSegments.size() ; segId++){
				int firstElem = djSegments.get(segId).iterator().next();
				firstElemSegmentsToBinCoeff.put(firstElem, binCoeffList.get(segId));
			}
			splitSegments(nCNF, firstElemSegmentsToBinCoeff, predId, predEquivalenceClasses, splitTermPos);
			// Remove atom with id PredId
			for(WClause clause : nCNF.clauses){
				Set<Term> clause_terms_set = new HashSet<Term>();
				for(Atom atom : clause.atoms){
					clause_terms_set.addAll(atom.terms);
				}
				ArrayList<Term> clause_terms = new ArrayList<Term>(clause_terms_set);
				Set<Term> termsNotToRemove = new HashSet<Term>();
				for(Atom atom1 : clause.atoms){
					if(predId == atom1.symbol.id){
						for(Term term1 : atom1.terms){
							for(Atom atom2 : clause.atoms){
								if(predId == atom2.symbol.id)
									continue;
								for(Term term2 : atom2.terms){
									if(term1 == term2)
										termsNotToRemove.add(term1);
								}
							}
						}
					}
					else{
						for(Term term1 : atom1.terms)
							termsNotToRemove.add(term1);
					}
				}
				clause_terms.removeAll(termsNotToRemove);
				int numSegments = clause.hcNumCopies.size();
				for(Term term : clause_terms){
					for(int s = 0 ; s < numSegments ; s++){
						clause.hcNumCopies.set(s, clause.hcNumCopies.get(s)*term.domain.get(s).size());
					}
				}
				for(int atomId = clause.atoms.size() - 1 ; atomId >= 0 ; atomId--){
					if(clause.atoms.get(atomId).symbol.id == predId){
						clause.atoms.remove(atomId);
						clause.sign.remove(atomId);
					}
				}
			}
			//System.out.println("Binomial done for predId : " + predId);
			return doLvExactPartition(nCNF);
		}
		double val = 0.0;
		// Process indexProcessed+1 th segment
		for(int k = 0 ; k <= djSegments.get(indexProcessed+1).size() ; k++){
			//MLN nMln = applyBinomialOnSegment(CNF, djSegments.get(indexProcessed+1), k, predId);
			//System.out.println("applying binomial on predId : " + predId + " and k : " + k);
			//System.out.println("MLN : ");
			/*
			for(WClause clause : nMln.clauses){
				System.out.println("clause : ");
				clause.print();
				for(Atom atom : clause.atoms){
					System.out.println("atom : " + atom.symbol.symbol + ", domain : " + atom.terms.get(0).domain);
				}
			}*/
			binCoeffList.set(indexProcessed+1,k);
			LogDouble result = new LogDouble(1.0);
			result = applyBinomial(CNF, djSegments, indexProcessed+1, binCoeffList, predId, splitTermPos);
			val = val + comb.findComb(djSegments.get(indexProcessed+1).size(), k)*Math.exp(result.value);
		}
		return new LogDouble(val,false);
	}

	public void splitSegments(MLN CNF, Map<Integer, Integer> firstElemSegmentsToBinCoeff, int predId, ArrayList<Set<Pair>> predEquivalenceClasses, int splitTermPos){
		for(WClause clause : CNF.clauses){
			Set<Term> termsSplitted = new HashSet<Term>();
			// Find which pred pos pairs to split
			Pair predIdPair = new Pair(predId,splitTermPos);
			Set<Pair> pairsToSplit = new HashSet<Pair>();
			Set<Term> clause_terms_set = new HashSet<Term>();
			for(Atom atom : clause.atoms){
				clause_terms_set.addAll(atom.terms);
			}
			ArrayList<Term> clause_terms = new ArrayList<Term>(clause_terms_set);
			for(Set<Pair> pairs : predEquivalenceClasses){
				if(pairs.contains(predIdPair)){
					pairsToSplit = pairs;
					break;
				}
			}
			for(Atom atom : clause.atoms){
				if(atom.symbol.id != predId)
					continue;
				for(int tId = 0 ; tId < atom.terms.size() ; tId++){
					Pair p = new Pair(atom.symbol.id,tId);
					if(!pairsToSplit.contains(p))
						continue;
					Term t = atom.terms.get(tId);
					if(termsSplitted.contains(t))
						continue;
					termsSplitted.add(t);
					int numSegments = t.domain.size();
					for(int segNum = 0 ; segNum < numSegments ; segNum++){
						int firstElem = t.domain.get(segNum).iterator().next();
						if(!firstElemSegmentsToBinCoeff.containsKey(firstElem))
							continue;
						int k = firstElemSegmentsToBinCoeff.get(firstElem);
						if(k == 0 || k == t.domain.get(segNum).size()){
							if(atom.symbol.id == predId){
								boolean temp = clause.satHyperCubes.get(segNum);
								temp |= (k == t.domain.get(segNum).size()) ^ clause.sign.get(clause.atoms.indexOf(atom));
								clause.satHyperCubes.set(segNum, temp);
							}
						}
						else{
							TreeSet<Integer> set = new TreeSet<Integer>(); // contains k elements
							int count = 0;
							for(Integer elem : t.domain.get(segNum)){
								set.add(elem);
								count++;
								if(count == k)
									break;
							}
							t.domain.get(segNum).removeAll(set);
							t.domain.add(set);
							for(Term term : clause_terms){
								if(term == t)
									continue;
								term.domain.add(new TreeSet<Integer>(term.domain.get(segNum)));
							}
							clause.hcNumCopies.add(clause.hcNumCopies.get(segNum));
							
							boolean oldSat = clause.satHyperCubes.get(segNum);
							if(clause.sign.get(clause.atoms.indexOf(atom)) == true){
								clause.satHyperCubes.set(segNum, true);
							}
							if(clause.sign.get(clause.atoms.indexOf(atom)) == false){
								clause.satHyperCubes.add(true);
							}
							else{
								clause.satHyperCubes.add(false | oldSat);
							}
							
						}
					}
				}
			}
			
			for(Atom atom : clause.atoms){
				if(atom.symbol.id == predId)
					continue;
				for(int tId = 0 ; tId < atom.terms.size() ; tId++){
					Pair p = new Pair(atom.symbol.id,tId);
					if(!pairsToSplit.contains(p))
						continue;
					Term t = atom.terms.get(tId);
					if(termsSplitted.contains(t))
						continue;
					termsSplitted.add(t);
					int numSegments = t.domain.size();
					for(int segNum = 0 ; segNum < numSegments ; segNum++){
						int firstElem = t.domain.get(segNum).iterator().next();
						if(!firstElemSegmentsToBinCoeff.containsKey(firstElem))
							continue;
						int k = firstElemSegmentsToBinCoeff.get(firstElem);
						if(k == 0 || k == t.domain.get(segNum).size()){
							if(atom.symbol.id == predId){
								boolean temp = clause.satHyperCubes.get(segNum);
								temp |= (k == t.domain.get(segNum).size()) ^ clause.sign.get(clause.atoms.indexOf(atom));
								clause.satHyperCubes.set(segNum, temp);
							}
						}
						else{
							TreeSet<Integer> set = new TreeSet<Integer>(); // contains k elements
							int count = 0;
							for(Integer elem : t.domain.get(segNum)){
								set.add(elem);
								count++;
								if(count == k)
									break;
							}
							t.domain.get(segNum).removeAll(set);
							t.domain.add(set);
							for(Term term : clause_terms){
								if(term == t)
									continue;
								term.domain.add(new TreeSet<Integer>(term.domain.get(segNum)));
							}
							clause.hcNumCopies.add(clause.hcNumCopies.get(segNum));
							clause.satHyperCubes.add(clause.satHyperCubes.get(segNum));
						}
					}
				}
			}
		}
	}

	private MLN applyBinomialOnSegment(MLN CNF, TreeSet<Integer> djSegment, int k, int predId) {
		
		MLN nMln = new MLN();
		int djSegFirstElement = djSegment.iterator().next();
		int n = djSegment.size();
		for(WClause clause : CNF.clauses){
			WClause newClause = MLN.create_new_clause(clause);
			Set<Term> clause_terms_set = new HashSet<Term>();
			for(Atom atom : newClause.atoms){
				clause_terms_set.addAll(atom.terms);
			}
			ArrayList<Term> clause_terms = new ArrayList<Term>(clause_terms_set);
			for(Atom atom : newClause.atoms){
				if(atom.symbol.id == predId){
					Term term = atom.terms.get(0);
					int numSegments = term.domain.size();  
					for(int i = numSegments-1 ; i >= 0 ; i--){
						// check if it is same segment as param disjoint segment
						if(term.domain.get(i).iterator().next() != djSegFirstElement)
							continue;
						if(k == n || k == 0){
							boolean temp = newClause.satHyperCubes.get(i);
							temp |= (k == n) ^ newClause.sign.get(newClause.atoms.indexOf(atom));
							newClause.satHyperCubes.set(i, temp);
						}
						else{
							TreeSet<Integer> set = new TreeSet<Integer>(); // contains k elements
							int count = 0;
							for(Integer elem : term.domain.get(i)){
								set.add(elem);
								count++;
								if(count == k)
									break;
							}
							term.domain.get(i).removeAll(set);
							boolean oldSat = newClause.satHyperCubes.get(i);
							if(newClause.sign.get(newClause.atoms.indexOf(atom)) == true){
								newClause.satHyperCubes.set(i, true);
							}
							
							term.domain.add(set);
							if(newClause.sign.get(newClause.atoms.indexOf(atom)) == false){
								newClause.satHyperCubes.add(true);
							}
							else{
								newClause.satHyperCubes.add(false | oldSat);
							}
							newClause.hcNumCopies.add(newClause.hcNumCopies.get(i));
							for(Term t : clause_terms){
								if(term == t)
									continue;
								t.domain.add(new TreeSet<Integer>(t.domain.get(i)));
							}
						}
					}
				}
			}
			nMln.clauses.add(newClause);
		} // end of clause loop
		// Check for inconsistency : empty segment of a term
		/*
		Scanner sc = new Scanner(System.in);
		for(WClause clause : nMln.clauses){
			for(Atom atom : clause.atoms){
				for(Term term : atom.terms){
					for(TreeSet<Integer> s : term.domain){
						if(s.isEmpty()){
							System.out.println("INCONSISTENCY..................................................................");
							System.out.println("atom : " + atom.symbol.symbol);
							System.out.println("predId : " + predId);
							System.out.println("term domain : " + term.domain);
							System.out.println("k : " + k);
							System.out.println("clause number : " + nMln.clauses.indexOf(clause));
							System.out.println("waiting for input.....");
							sc.next();
						}
					}
				}
			}
		}*/
		return nMln;
	}

	private void updateClauseSegments(MLN CNF,
			ArrayList<HashMap<Integer, Integer>> allTermsFirstElemToSegmentMap,
			ArrayList<ArrayList<TreeSet<Integer>>> allTermsDjSegments, int predId) {
		
		for(WClause clause : CNF.clauses){
			Set<Term> termSeen = new HashSet<Term>();
			Set<Term> clause_terms = new HashSet<Term>();
			for(Atom atom : clause.atoms){
				for(Term term : atom.terms)
					clause_terms.add(term);
			}
			
			for(Atom atom : clause.atoms){
				if(atom.symbol.id != predId)
					continue;
				for(int tId = 0 ; tId < atom.terms.size() ; tId++){
					Term term = atom.terms.get(tId);
					if(termSeen.contains(term))
						continue;
					termSeen.add(term);
					int numSegments = term.domain.size();
					for(int segId = 0 ; segId < numSegments ; segId++){
						while(true){
							int firstElem = term.domain.get(segId).iterator().next();
							int djSegId = allTermsFirstElemToSegmentMap.get(tId).get(firstElem);
							if(allTermsDjSegments.get(tId).get(djSegId).size() == term.domain.get(segId).size())
								break;
							term.domain.get(segId).removeAll(allTermsDjSegments.get(tId).get(djSegId));
							term.domain.add(new TreeSet<Integer>(allTermsDjSegments.get(tId).get(djSegId)));
							clause.hcNumCopies.add(clause.hcNumCopies.get(segId));
							clause.satHyperCubes.add(clause.satHyperCubes.get(segId));
							for(Term clauseTerm : clause_terms){
								if(clauseTerm == term)
									continue;
								clauseTerm.domain.add(new TreeSet<Integer>(clauseTerm.domain.get(segId)));
							}
						}
					}
				}
			}
		}
		
	}
	private ArrayList<TreeSet<Integer>> createDisjointSegments(MLN CNF, Decomposer decomposer) {
		Set<TreeSet<Integer>> djSegmentsSets = new HashSet<TreeSet<Integer>>();
		for(WClause clause : CNF.clauses){
			for(Atom atom : clause.atoms){
				if(!decomposer.predicate_positions.containsKey(atom.symbol.id))
					break;
				Term term = atom.terms.get(decomposer.predicate_positions.get(atom.symbol.id));
				djSegmentsSets.addAll((ArrayList<TreeSet<Integer>>)DeepCopyUtil.copy(term.domain));
			}
		}
		ArrayList<TreeSet<Integer>> djSegmentsList = new ArrayList<TreeSet<Integer>>(djSegmentsSets);
		createDisjointIdenticalSegments(djSegmentsList);
		return djSegmentsList;
	}
	
	private ArrayList<ArrayList<TreeSet<Integer>>> createDisjointSegments(MLN CNF, int predId) {
		
		// For each term, store sets of segments
		ArrayList<Set<TreeSet<Integer>>> allTermsDjSegmentsSets = new ArrayList<Set<TreeSet<Integer>>>();
		boolean firstOccurence = false;
		for(WClause clause : CNF.clauses){
			for(Atom atom : clause.atoms){
				if(atom.symbol.id == predId){
					if(firstOccurence == false){
						firstOccurence = true;
						for(int i = 0 ; i < atom.terms.size() ; i++){
							allTermsDjSegmentsSets.add(new HashSet<TreeSet<Integer>>());
						}
					}
					for(int termPos = 0 ; termPos < atom.terms.size() ; termPos++){
						allTermsDjSegmentsSets.get(termPos).addAll((ArrayList<TreeSet<Integer>>)DeepCopyUtil.copy(atom.terms.get(termPos).domain));
					}
				}
			}
		}
		
		// create lists from set
		ArrayList<ArrayList<TreeSet<Integer>>> allTermsDjSegmentsList = new ArrayList<ArrayList<TreeSet<Integer>>>();
		for(Set<TreeSet<Integer>> segments : allTermsDjSegmentsSets){
			allTermsDjSegmentsList.add(new ArrayList<TreeSet<Integer>>(segments));
		}
		
		// create disjoint segments for each term
		for(ArrayList<TreeSet<Integer>> segments : allTermsDjSegmentsList){
			createDisjointIdenticalSegments(segments);
		}
		//System.out.println("disjoint created...");
		return allTermsDjSegmentsList;
	}
	
	public static void createDisjointIdenticalSegments(ArrayList<TreeSet<Integer>>segments){
		if(segments.size() < 2){
			return;
		}
		//HyperCube hyperCube = segments.get(0);
		ArrayList<ArrayList<Integer>> hyperCubesIntersectionList = new ArrayList<ArrayList<Integer>>();
		//ArrayList<Boolean> doneList = new ArrayList<Boolean>();
		for(int hcId = 0 ; hcId < segments.size() ; hcId++){
			hyperCubesIntersectionList.add(new ArrayList<Integer>());
			//doneList.add(false);
		}
		for(int hc1Id = 0 ; hc1Id < segments.size() ; hc1Id++){
			for(int hc2Id = 0 ; hc2Id < hc1Id ; hc2Id++){
				/*
				Set<Integer>hc1VarConstants = hyperCubes.get(hc1Id).varConstants.get(varId);
				Set<Integer>hc2VarConstants = hyperCubes.get(hc2Id).varConstants.get(varId);
				Set<Integer>intersectionHcVarConstants = new HashSet<Integer>(hc1VarConstants);
				intersectionHcVarConstants.retainAll(hc2VarConstants);
				*//*
				if(varId == 1 && hc1Id == 1 && hc2Id == 0){
					System.out.println("printing varConstants");
					System.out.println(hyperCubes.get(hc1Id).varConstants.get(varId));
					System.out.println(hyperCubes.get(hc2Id).varConstants.get(varId));
				}*/
				if(!areDisjointOrIdentical(segments.get(hc1Id),segments.get(hc2Id))){
					hyperCubesIntersectionList.get(hc1Id).add(hc2Id);
					hyperCubesIntersectionList.get(hc2Id).add(hc1Id);
					/*
					if(varId == 1 && hc1Id == 1 && hc2Id == 0){
						System.out.println("hyperCube added");
					}*/
				}
			}
		}
		//System.out.println("Initial hyperCubeIntersectionList created and list size is " + hyperCubesIntersectionList.size());
		//System.out.println("first hypercube's intersection list size : " + hyperCubesIntersectionList.get(0).size());
		/*
		for(int i = 0 ; i < hyperCubesIntersectionList.size() ; i++){
			System.out.println(hyperCubesIntersectionList.get(i));
		}*/
		// Repetitively create disjoint(or same) hypercubes on variable varId
		//System.out.println(segmentsIndexList);
		while(true){
			boolean done = true;
			int hcListId = 0;
			for(hcListId = 0 ; hcListId < hyperCubesIntersectionList.size() ; hcListId++){
				ArrayList<Integer>hyperCubeList = hyperCubesIntersectionList.get(hcListId);
				if(hyperCubeList.size() > 0){
					done = false;
					break;
				}
			}
			// If there is no non empty list, we are done 
			if(done == true)
				break;
			/*for(int hcListId = hyperCubesIntersectionList.size() - 1 ; hcListId >= 0 ; hcListId--){
				if(hyperCubesIntersectionList.get(hcListId).size() > 0){
					int hc2Id = hyperCubes.indexOf(hyperCubesIntersectionList.get(hcListId).get(0));
					createDisjointHyperCubesVar(varId,hcListId,hc2Id,hyperCubesIntersectionList,hyperCubes);
				}
			}*/
			int hc2Id = hyperCubesIntersectionList.get(hcListId).get(0);
			//System.out.println("Now calling disjoint on var function with hcListId = "+hcListId+" and hc2Id = "+hc2Id);
			//System.out.println(hyperCubes);
			createDisjoint(hcListId,hc2Id,hyperCubesIntersectionList,segments);
			//System.out.println(hyperCubes);
			//System.out.println(hyperCubesIntersectionList);
		}
		//System.out.println("Disjoint on variable "+ varId + "done, now number of hyperCubes = "+hyperCubes.size());
		// Remove duplicates
		Set<TreeSet<Integer>> segmentsSet = new HashSet<TreeSet<Integer>>(segments);
		segments.clear();
		segments.addAll(segmentsSet);
	}
	
	public static void createDisjoint(int h1Id, int h2Id, ArrayList<ArrayList<Integer>> hyperCubesIntersectionList, ArrayList<TreeSet<Integer>>segments){
		TreeSet<Integer> hc1VarConstants = segments.get(h1Id);
		TreeSet<Integer> hc2VarConstants = segments.get(h2Id);
		//System.out.println("Inside createDisjointHyperCubesVar function with input hyperCubes : "+h1.toString()+" and "+h2.toString());
		// create 3 sets : intSectHc = h1^h2, h1-intSectHc, h2-intSectHc, all on variable varId
		//Set<Integer>hc1VarConstants = new HashSet<Integer>(h1.varConstants.get(0));
		//Set<Integer>hc2VarConstants = new HashSet<Integer>(h2.varConstants.get(0));
		
		TreeSet<Integer>intSectVarConstants = new TreeSet<Integer>(hc1VarConstants);
		intSectVarConstants.retainAll(hc2VarConstants);
		
		TreeSet<Integer>onlyHc1VarConstants = new TreeSet<Integer>(hc1VarConstants);
		onlyHc1VarConstants.removeAll(intSectVarConstants);
		
		TreeSet<Integer>onlyHc2VarConstants = new TreeSet<Integer>(hc2VarConstants);
		onlyHc2VarConstants.removeAll(intSectVarConstants);
		
		
		//ArrayList<HyperCube>mergedIntSectHyperCubes = new ArrayList<HyperCube>();
		//HyperCube onlyHc1HyperCube = null, onlyHc2HyperCube = null;
		//System.out.println("Now creating onlyh1 hyperCube");
		if(onlyHc1VarConstants.size() > 0){
			//onlyHc1HyperCube = new HyperCube(h1);
			//onlyHc1HyperCube.setVarConstants(onlyHc1VarConstants, 0);
			segments.add(onlyHc1VarConstants); // Add this new hyperCube into hyperCubes List
			ArrayList<Integer>onlyHc1HyperCubesList = new ArrayList<Integer>(); // Intersection list of this new hyperCube, we will fill this now
			
			// Fill intersection list of new hypercube and also update rest's lists with index of this new hyperCube
			for(Integer hcId : hyperCubesIntersectionList.get(h1Id)){
				if(!areDisjointOrIdentical(onlyHc1VarConstants, segments.get(hcId))){
					onlyHc1HyperCubesList.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(segments.size()-1);
				}
			}
			hyperCubesIntersectionList.add(onlyHc1HyperCubesList);
			///System.out.println(hyperCubes);
			///System.out.println(hyperCubesIntersectionList);
			///System.out.println(onlyHc1ClauseList);
		}
		//System.out.println("Onlyh1 hyperCube created");
		//System.out.println("Now creating onlyh2 hyperCube");
		if(onlyHc2VarConstants.size() > 0){
			//onlyHc2HyperCube = new HyperCube(h2);
			//onlyHc2HyperCube.setVarConstants(onlyHc2VarConstants, 0);
			segments.add(onlyHc2VarConstants);
			ArrayList<Integer>onlyHc2HyperCubesList = new ArrayList<Integer>();
			for(Integer hcId : hyperCubesIntersectionList.get(h2Id)){
				if(!areDisjointOrIdentical(onlyHc2VarConstants, segments.get(hcId))){
					onlyHc2HyperCubesList.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(segments.size()-1);
				}
			}
			hyperCubesIntersectionList.add(onlyHc2HyperCubesList);
			///System.out.println(hyperCubes);
			///System.out.println(hyperCubesIntersectionList);
			//System.out.println(onlyHc2ClauseList);
		}
		//System.out.println("onlyh2 hyperCube created");
		//System.out.println("Now hyperCubeIntersectionList is : ");
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println("Now removing h1Id = "+h1Id+" from all places");
		if(intSectVarConstants.size() > 0){
			//HyperCube intSectHyperCube1 = new HyperCube(h1);
			//intSectHyperCube1.setVarConstants(intSectVarConstants, 0);
			TreeSet<Integer> intSectVarConstants1 = new TreeSet<Integer>(intSectVarConstants); 
			segments.set(h1Id,intSectVarConstants1);
			TreeSet<Integer> intSectVarConstants2 = new TreeSet<Integer>(intSectVarConstants);
			segments.set(h2Id,intSectVarConstants1);
		}
			
		for(int i = hyperCubesIntersectionList.get(h1Id).size() - 1 ; i >= 0 ; i--){
			int hcId = hyperCubesIntersectionList.get(h1Id).get(i);
			///System.out.println("hcId = "+hcId);
			int h1Index = hyperCubesIntersectionList.get(hcId).indexOf(h1Id);
			if(areDisjointOrIdentical(segments.get(hcId), intSectVarConstants))
			{
			///System.out.println("h1Index = "+h1Index);
				hyperCubesIntersectionList.get(hcId).remove(h1Index);
				hyperCubesIntersectionList.get(h1Id).remove(i);
			}
			///System.out.println("hyperCubeIntersectionList = ");
			///System.out.println(hyperCubesIntersectionList);
		}
		//System.out.println("h1Id removed from all places and now hyperCubeIntersectionList is : ");
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println(hyperCubes);
		//System.out.println(hyperCubesIntersectionList);
		//System.out.println("Now removing h2Id = "+h2Id+" from all places");
		for(int i = hyperCubesIntersectionList.get(h2Id).size() - 1 ; i >= 0 ; i--){
			int hcId = hyperCubesIntersectionList.get(h2Id).get(i);
			int h2Index = hyperCubesIntersectionList.get(hcId).indexOf(h2Id);
			///System.out.println("h2Index = "+h2Index);
			if(areDisjointOrIdentical(segments.get(hcId), intSectVarConstants)){
				hyperCubesIntersectionList.get(hcId).remove(h2Index);
				hyperCubesIntersectionList.get(h2Id).remove(i);
			}
				
		}
		///System.out.println("h2Id removed from all places and now hyperCubeIntersectionList is : ");
		///System.out.println(hyperCubesIntersectionList);
		/*
		if(intSectVarConstants.size() > 0){
			//HyperCube intSectHyperCube1 = new HyperCube(h1);
			//intSectHyperCube1.setVarConstants(intSectVarConstants, 0);
			//TreeSet<Integer> intSectVarConstants1 = new TreeSet<Integer>(intSectVarConstants); 
			//segments.set(h1Id,intSectVarConstants1);
			ArrayList<Integer>intSectHyperCubesList1 = new ArrayList<Integer>();
			for(Integer hcId : hyperCubesIntersectionList.get(h1Id)){
				if(hcId == h2Id)
					continue;
				if(!areDisjointOrIdentical(segments.get(h1Id),segments.get(hcId))){
					intSectHyperCubesList1.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(h1Id);
				}
			}
			hyperCubesIntersectionList.get(h1Id).clear();
			hyperCubesIntersectionList.get(h1Id).addAll(intSectHyperCubesList1);
			
			//HyperCube intSectHyperCube2 = new HyperCube(h2); 
			//intSectHyperCube2.setVarConstants(intSectVarConstants, 0);
			//TreeSet<Integer> intSectVarConstants2 = new TreeSet<Integer>(intSectVarConstants);
			//segments.set(h2Id,intSectVarConstants2);
			ArrayList<Integer>intSectHyperCubesList2 = new ArrayList<Integer>();
		
			for(Integer hcId : hyperCubesIntersectionList.get(h2Id)){
				if(hcId == h1Id)
					continue;
				if(!areDisjointOrIdentical(segments.get(h2Id),segments.get(hcId))){
					intSectHyperCubesList2.add(hcId);
					hyperCubesIntersectionList.get(hcId).add(h2Id);
				}
			}
			hyperCubesIntersectionList.get(h2Id).clear();
			hyperCubesIntersectionList.get(h2Id).addAll(intSectHyperCubesList2);
		}*/
	}
	
	private static boolean areDisjointOrIdentical(TreeSet<Integer> set1,
			TreeSet<Integer> set2) {
		
		if(set1.equals(set2)){
			return true;
		}
		TreeSet<Integer>intersection = new TreeSet<Integer>(set1);
		intersection.retainAll(set2);
		if(intersection.size() > 0){
			return false;
		}
		return true;
	}

	/*
	public ArrayList<MLN> decomposeCNF(MLN CNF, ArrayList<Integer> powerFactors) {
		ArrayList<Decomposer> decomposer_list = new ArrayList<Decomposer>();
		LDecomposer lDecomposer = new LDecomposer();
		lDecomposer.find_decomposer(CNF, decomposer_list);
		
		//int powerFactor = 0;
		// Print*/
		/*
		System.out.print("Decomposer={ ");
		for(int i=0;i<decomposer_list.size();i++)
		{
			decomposer_list.get(i).print();
		}
		System.out.println("}");
		*//*
		ArrayList<MLN> decomposedMlns = new ArrayList<MLN>();
		if(decomposer_list.size()==0){
			powerFactors.add(1);
			return decomposedMlns;
		}
		else
		{
			MLN remainingClausesMln = new MLN(); // This MLN stores those clauses in which decomposer doesn't exist
			remainingClausesMln.max_predicate_id = CNF.max_predicate_id;
			remainingClausesMln.symbols = (List<PredicateSymbol>) DeepCopyUtil.copy(CNF.symbols);
			//for(int i=0;i<decomposer_list.size();i++)
			for(int i=0;i<1;i++) // currently deal with only 1 decomposer at a time
			{
				Decomposer decomposer = decomposer_list.get(i);
				if(LiftedPTP.lazyDisjoint == true){
					ArrayList<TreeSet<Integer>> djSegments = createDisjointSegments(CNF, decomposer);
					Map<Integer, Integer> firstElemToSegmentMap = new HashMap<Integer,Integer>();
					for(int e = 0 ; e < djSegments.size() ; e++){
						firstElemToSegmentMap.put(djSegments.get(e).iterator().next(), e);
					}
					updateClauseSegments(CNF, firstElemToSegmentMap, djSegments, decomposer);
				}
				// set of first elements of disjoint segments. Also provides indices for new MLNs. For example, if this list is [2,1,5], it means
				// first MLN contains segments which start from 2, second MLN contains segments which start from 1, and so on.
				ArrayList<Integer> firstElements = new ArrayList<Integer>();
				for(WClause clause : CNF.clauses){
					// stores those mln Ids (indices) in which this clause has been added 
					Set<Integer> MlnIds = new HashSet<Integer>();
					Term decomposerTerm = null;
					for(Atom atom : clause.atoms){
						for(Term term : atom.terms){
							if(decomposer.decomposer_terms.contains(term)){
								decomposerTerm = term;
								break;
							}
						}
						break;
					}
					// Add this clause as it is into remainingClausesMln
					if(decomposerTerm == null){
						remainingClausesMln.clauses.add(MLN.create_new_clause(clause));
					}
					else{
						// for each segment of decomposer term, find which mln it should belong and put it into that
						for(int segIndex = 0 ; segIndex < decomposerTerm.domain.size() ; segIndex++){
							TreeSet<Integer> segment = decomposerTerm.domain.get(segIndex);
							int firstElem = segment.iterator().next();
							int MlnIndex = firstElements.indexOf(firstElem);
							MLN mln = null;
							// If MLN doesn't exist yet, create it
							if(MlnIndex == -1){
								firstElements.add(firstElem);
								// Also add powerFactor into powerFactors list
								powerFactors.add(segment.size());
								mln = new MLN();
								mln.symbols = (List<PredicateSymbol>) DeepCopyUtil.copy(CNF.symbols);
								mln.max_predicate_id = CNF.max_predicate_id;
								MlnIndex = firstElements.indexOf(firstElem);
								decomposedMlns.add(mln);
							}
							else{
								mln = decomposedMlns.get(MlnIndex);
							}
							WClause nClause = null;
							// If this clause doesn't exist in mln, create it and add this MLN id into MlnIds set
							if(!MlnIds.contains(MlnIndex)){
								nClause = MLN.create_new_clause(clause);
								mln.clauses.add(nClause);
								// Remove all segments, satHyperCubes and numCopies
								for(Atom nAtom : nClause.atoms){
									for(Term nTerm : nAtom.terms){
										nTerm.domain.clear();
									}
								}
								nClause.satHyperCubes.clear();
								nClause.hcNumCopies.clear();
								MlnIds.add(MlnIndex);
							}
							//Add this segment (along with other terms' segments corresponding to this row) to this mln. Also add satHyperCubes
							// and numCopies
							else{
								// clauseId corresponding to this clause in the decomposed mln will be last one
								nClause = mln.clauses.get(mln.clauses.size()-1);
							}
							nClause.satHyperCubes.add(clause.satHyperCubes.get(segIndex));
							nClause.hcNumCopies.add(clause.hcNumCopies.get(segIndex));
							// terms whose segment has been added into new Clause
							Set<Term> termsAdded = new HashSet<Term>();
							for(int atomIndex = 0 ; atomIndex < clause.atoms.size() ; atomIndex++){
								for(int termIndex = 0 ; termIndex < clause.atoms.get(atomIndex).terms.size() ; termIndex++){
									if(termsAdded.contains(clause.atoms.get(atomIndex).terms.get(termIndex)))
										continue;
									termsAdded.add(clause.atoms.get(atomIndex).terms.get(termIndex));
									TreeSet<Integer> nSeg = new TreeSet<Integer>(clause.atoms.get(atomIndex).terms.get(termIndex).domain.get(segIndex)); 
									// If this term is decomposer term, reduce its domain to size 1 and add this segment
									if(decomposerTerm == clause.atoms.get(atomIndex).terms.get(termIndex)){
										nSeg.clear();
										nSeg.add(firstElem);
										nClause.atoms.get(atomIndex).terms.get(termIndex).domain.add(nSeg);
									}
									else{
										nClause.atoms.get(atomIndex).terms.get(termIndex).domain.add(nSeg);
									}
								}
							}
						}
					}
				}
				// Happy skipping this part, may not be required
				//store pre-decomp domain
				//decomposer_list[i]->decomposer_terms[j]->origDomain.clear();
				//for(unsigned int jj=0;jj<decomposer_list[i]->decomposer_terms[j]->domain.size();jj++)
					//decomposer_list[i]->decomposer_terms[j]->origDomain.push_back(decomposer_list[i]->decomposer_terms[j]->domain[jj]);
			 
			}
			// If remainingClauseMlns contains any clause, add it into MLN list
			if(remainingClausesMln.clauses.size() > 0){
				decomposedMlns.add(remainingClausesMln);
				powerFactors.add(1);
			}
			
		}
		// we don't need original CNF now
		CNF.clauses.clear();
		return decomposedMlns;
	}
	*/
	private void updateClauseSegments(MLN CNF,
			Map<Integer, Integer> firstElemToSegmentMap,
			ArrayList<TreeSet<Integer>> djSegments, Decomposer decomposer) {
		
		for(WClause clause : CNF.clauses){
			Set<Term> clause_terms = new HashSet<Term>();
			for(Atom atom : clause.atoms){
				for(Term term : atom.terms)
					clause_terms.add(term);
			}
			for(Atom atom : clause.atoms){
				if(!decomposer.predicate_positions.containsKey(atom.symbol.id))
					break;
				Term term = atom.terms.get(decomposer.predicate_positions.get(atom.symbol.id));
				int numSegments = term.domain.size();
				for(int segId = 0 ; segId < numSegments ; segId++){
					while(true){
						int firstElem = term.domain.get(segId).iterator().next();
						int djSegId = firstElemToSegmentMap.get(firstElem);
						if(djSegments.get(djSegId).size() == term.domain.get(segId).size())
							break;
						term.domain.get(segId).removeAll(djSegments.get(djSegId));
						term.domain.add(new TreeSet<Integer>(djSegments.get(djSegId)));
						clause.hcNumCopies.add(clause.hcNumCopies.get(segId));
						clause.satHyperCubes.add(clause.satHyperCubes.get(segId));
						for(Term clauseTerm : clause_terms){
							if(clauseTerm == term)
								continue;
							clauseTerm.domain.add(new TreeSet<Integer>(clauseTerm.domain.get(segId)));
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
