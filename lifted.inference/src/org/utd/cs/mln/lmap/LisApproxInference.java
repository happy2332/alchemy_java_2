package org.utd.cs.mln.lmap;

import java.io.FileNotFoundException;
import java.sql.NClob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.LDecomposer;
import org.utd.cs.mln.alchemy.core.Decomposer;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;
import org.utd.cs.mln.alchemy.util.comb;

public class LisApproxInference {
	
	//public MLN mln;
	//public LDecomposer decomposer;
	//public LHeuristics heuristics;
	//public LvrNormPropagation lvrNormPropagate;
	//not owned
	//public LProposalDistribution distribution;
	/*
	public LisApproxInference(MLN mln_){
		mln = mln_;
		decomposer = new LDecomposer(mln);
		heuristics = new LHeuristics(decomposer,mln);
		lvrNormPropagate = new LvrNormPropagation(mln);
	}*/
		

	public LogDouble doLvApproxPartitionBinomial(MLN mln){
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
			LogDouble weight = PTPUtil.CNFWeight(mln);
			mln.clearData();
			return weight;
		}
		
		ArrayList<MLN> dCNFs = new ArrayList<MLN>();
		DisjointCNFCreator.seperateDisjointCNF(mln, dCNFs);
		//DisjointCNFCreator.seperateDisjointSegmentsCNF(mln,dCNFs);
		mln.clearData();
		for(int t=0;t<dCNFs.size();t++)
		{
			MLN CNF = dCNFs.get(t);
			if(CNF.isPropositionalSegments()){
				//LogDouble mcnt = LisApproxPropositional.doLvApproxPropositional(CNF);
				//System.out.println("exact propostional inference...");
				LogDouble mcnt = PTPExactPropositional.doLvExactPropositional(CNF);
				totalVal = totalVal.multiply(mcnt);
				continue;
			}
			// Do not apply decomposers
			ArrayList<Integer> powerFactors = new ArrayList<Integer>();
			ArrayList<MLN> decomposedMlns = PTPUtil.decomposeCNF(CNF, powerFactors);
			if(powerFactors.size() > 1 || powerFactors.get(0) > 1)
			{
				for(int mlnId = 0 ; mlnId < decomposedMlns.size() ; mlnId++){
					LogDouble mcnt = doLvApproxPartitionBinomial(decomposedMlns.get(mlnId));
					LogDouble val = new LogDouble(1.0,false);
					val = mcnt.power((double)powerFactors.get(mlnId));
					totalVal = totalVal.multiply(val);
				}
				continue;
			}
			Atom tmpAtom = LHeuristics.getAtomToSplitActiveTerms(CNF);
			if(tmpAtom==null)
			{	
				//Atom tmpAtom1 = heuristics.getAtomToSplit(CNF);
				//System.out.println(tmpAtom1);
				LogDouble wt1 = PTPUtil.CNFWeight(CNF);
				CNF.clearData();
			    totalVal = totalVal.multiply(wt1); // Doubt here
				continue;
			}
			if(tmpAtom.activeTermsIndices.size() > 1){
				// do partial grounding.
				// For now : do on first index of activeTermIndices
				PartialGround pg = new PartialGround();
				MLN gCNF = pg.partiallyGroundMln(CNF, tmpAtom.symbol.id, tmpAtom.activeTermsIndices.get(0));
				totalVal = totalVal.multiply(doLvApproxPartitionBinomial(gCNF));
				continue;
			}
			if(tmpAtom.activeTermsIndices.size() == 0){
				int k = PTPUtil.sampleBinomial(1);
				LogDouble sampleWeight = new LogDouble(2.0,false);
				splitPropositionalPred(CNF, tmpAtom.symbol.id);
				LogDouble mcnt = doLvApproxPartitionBinomial(CNF);
				sampleWeight = sampleWeight.multiply(mcnt);
				totalVal = totalVal.multiply(sampleWeight);
				continue;
			}
			Atom atom = MLN.create_new_atom(tmpAtom);
			LogDouble sampleWeight = new LogDouble(1.0,false);
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
			//sampleWeight = applyBinomial(CNF, atom.symbol.id);
			//sampleWeight = applyBinomialUpdated(CNF, atom.symbol.id, atom.activeTermsIndices.get(0));
			//System.out.println("Applying binomial on pred : " + atom.symbol.symbol + ", id : " + atom.symbol.id);
			sampleWeight = applyBinomialFinal(CNF, atom.symbol.id, atom.activeTermsIndices.get(0));
			LogDouble mcnt = doLvApproxPartitionBinomial(CNF);
			sampleWeight = sampleWeight.multiply(mcnt);
			totalVal = totalVal.multiply(sampleWeight);
		}
		return totalVal ;
	}

	private void splitPropositionalPred(MLN CNF, int predId) {
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
				int numSegments = clause.satHyperCubes.size();
				for(int i = 0 ; i < numSegments ; i++){
					clause.satHyperCubes.set(i, true);
				}
			}
			
		}		
	}

	private LogDouble applyBinomial(MLN CNF, int predId) {
		
		int totalGroundings = 0;
		// key : list of first elements of terms segments, value : first : binomialK, second : termPosition (on which binomial to apply)
		Map<ArrayList<Integer>,ArrayList<Integer>> firstElemSegmentsToBinCoeff = new HashMap<ArrayList<Integer>,ArrayList<Integer>>();
		for(WClause clause : CNF.clauses){
			Set<Term> clause_terms_set = new HashSet<Term>();
			for(Atom atom : clause.atoms){
				clause_terms_set.addAll(atom.terms);
			}
			ArrayList<Term> clause_terms = new ArrayList<Term>(clause_terms_set);
			for(Atom atom : clause.atoms){
				if(atom.symbol.id == predId){
					int numSegments = atom.terms.get(0).domain.size();  
					for(int i = numSegments-1 ; i >= 0 ; i--){
						ArrayList<Integer> firstElemList = new ArrayList<Integer>();
						int maxDomain = 0;
						int maxDomainPos = 0;
						int segGroundings = 1;
						for(int tId = 0 ; tId < atom.terms.size() ; tId++){
							Term term = atom.terms.get(tId);
							segGroundings *= term.domain.get(i).size();
							if(term.domain.get(i).size() > maxDomain){
								maxDomain = term.domain.get(i).size();
								maxDomainPos = tId;
							}
							firstElemList.add(term.domain.get(i).iterator().next());
						}
						if(!firstElemSegmentsToBinCoeff.containsKey(firstElemList)){
							int k = PTPUtil.sampleBinomial(maxDomain);
							//k = 1;// hardCoded k (should delete)
							ArrayList<Integer> val = new ArrayList<Integer>();
							val.add(k);
							val.add(maxDomainPos);
							totalGroundings += segGroundings;
							firstElemSegmentsToBinCoeff.put(firstElemList, val);
						}
						ArrayList<Integer> val = firstElemSegmentsToBinCoeff.get(firstElemList);
						int k = val.get(0);
						Term maxDomTerm = atom.terms.get(val.get(1));
						if(k == maxDomain || k == 0){
							boolean temp = clause.satHyperCubes.get(i);
							temp |= (k == maxDomain) ^ clause.sign.get(clause.atoms.indexOf(atom));
							clause.satHyperCubes.set(i, temp);
						}
						else{
							TreeSet<Integer> set = new TreeSet<Integer>(); // contains k elements
							int count = 0;
							for(Integer elem : maxDomTerm.domain.get(i)){
								set.add(elem);
								count++;
								if(count == k)
									break;
							}
							maxDomTerm.domain.get(i).removeAll(set);
							boolean oldSat = clause.satHyperCubes.get(i);
							if(clause.sign.get(clause.atoms.indexOf(atom)) == true){
								clause.satHyperCubes.set(i, true);
							}
							
							maxDomTerm.domain.add(set);
							if(clause.sign.get(clause.atoms.indexOf(atom)) == false){
								clause.satHyperCubes.add(true);
							}
							else{
								clause.satHyperCubes.add(false | oldSat);
							}
							clause.hcNumCopies.add(clause.hcNumCopies.get(i));
							for(Term term : clause_terms){
								if(term == maxDomTerm)
									continue;
								term.domain.add(new TreeSet<Integer>(term.domain.get(i)));
							}
						}
						
					}
				}
			}
			
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
				}
			}
		}// end of clause loop
		return new LogDouble(totalGroundings*Math.log(2), true);
	}

	// This is correct version. In this, all terms in an equivalence get updated i.e. splitted based on k for each segment
	private LogDouble applyBinomialUpdated(MLN CNF, int predId) {
		
		int totalGroundings = 0;
		// key : first element of term segment, value : first : binomialK, second : termPosition (on which binomial to apply)
		Map<Integer,ArrayList<Integer>> firstElemSegmentsToBinCoeff = new HashMap<Integer,ArrayList<Integer>>();
		for(WClause clause : CNF.clauses){
			
			for(Atom atom : clause.atoms){
				if(atom.symbol.id == predId){
					int numSegments = atom.terms.get(0).domain.size();  
					for(int i = numSegments-1 ; i >= 0 ; i--){
						ArrayList<Integer> firstElemList = new ArrayList<Integer>();
						int maxDomain = 0;
						int maxDomainPos = 0;
						int segGroundings = 1;
						for(int tId = 0 ; tId < atom.terms.size() ; tId++){
							Term term = atom.terms.get(tId);
							segGroundings *= term.domain.get(i).size();
							if(term.domain.get(i).size() > maxDomain){
								maxDomain = term.domain.get(i).size();
								maxDomainPos = tId;
							}
							firstElemList.add(term.domain.get(i).iterator().next());
						}
						if(!firstElemSegmentsToBinCoeff.containsKey(firstElemList.get(0))){
							int k = PTPUtil.sampleBinomial(maxDomain);
							//k = 1;// hardCoded k (should delete)
							ArrayList<Integer> val = new ArrayList<Integer>();
							val.add(k);
							maxDomainPos = 0; // hardcoded
							val.add(maxDomainPos);
							totalGroundings += segGroundings;
							firstElemSegmentsToBinCoeff.put(firstElemList.get(0), val);
						}
					}
				}
			}
		}
		ArrayList<Set<Pair>> predEquivalenceClasses = new ArrayList<Set<Pair>>();
		NonSameEquivConverter.findEquivalenceClasses(CNF, predEquivalenceClasses);
		splitSegments(CNF, firstElemSegmentsToBinCoeff, predId, predEquivalenceClasses);
						
		Set<Term> termsNotToRemove = new HashSet<Term>();
		for(WClause clause : CNF.clauses){
			Set<Term> clause_terms_set = new HashSet<Term>();
			for(Atom atom : clause.atoms){
				clause_terms_set.addAll(atom.terms);
			}
			ArrayList<Term> clause_terms = new ArrayList<Term>(clause_terms_set);
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
				}
			}
		}// end of clause loop
		return new LogDouble(totalGroundings*Math.log(2), true);
	}
	
	// This is correct version. In this, all terms in an equivalence get updated i.e. splitted based on k for each segment
		private LogDouble applyBinomialFinal(MLN CNF, int predId, Integer splitTermIndex) {
			
			int totalGroundings = 0;
			// key : first element of term segment, value : first : binomialK, second : termPosition (on which binomial to apply)
			Map<Integer,ArrayList<Integer>> firstElemSegmentsToBinCoeff = new HashMap<Integer,ArrayList<Integer>>();
			for(WClause clause : CNF.clauses){
				
				for(Atom atom : clause.atoms){
					if(atom.symbol.id == predId){
						int numSegments = atom.terms.get(0).domain.size();  
						for(int i = numSegments-1 ; i >= 0 ; i--){
							//ArrayList<Integer> firstElemList = new ArrayList<Integer>();
							//int maxDomain = 0;
							//int maxDomainPos = 0;
							int segGroundings = 1;
							for(int tId = 0 ; tId < atom.terms.size() ; tId++){
								Term term = atom.terms.get(tId);
								segGroundings *= term.domain.get(i).size();
								/*
								if(term.domain.get(i).size() > maxDomain){
									maxDomain = term.domain.get(i).size();
									maxDomainPos = tId;
								}*/
								//firstElemList.add(term.domain.get(i).iterator().next());
							}
							int firstElem = atom.terms.get(splitTermIndex).domain.get(i).iterator().next();
							if(!firstElemSegmentsToBinCoeff.containsKey(firstElem)){
								int k = PTPUtil.sampleBinomial(atom.terms.get(splitTermIndex).domain.get(i).size());
								//k = 1;// hardCoded k (should delete)
								ArrayList<Integer> val = new ArrayList<Integer>();
								val.add(k);
								//maxDomainPos = 0; // hardcoded
								val.add(splitTermIndex);
								totalGroundings += segGroundings;
								firstElemSegmentsToBinCoeff.put(firstElem, val);
							}
						}
					}
				}
			}
			ArrayList<Set<Pair>> predEquivalenceClasses = new ArrayList<Set<Pair>>();
			NonSameEquivConverter.findEquivalenceClasses(CNF, predEquivalenceClasses);
			splitSegments(CNF, firstElemSegmentsToBinCoeff, predId, predEquivalenceClasses);
							
			Set<Term> termsNotToRemove = new HashSet<Term>();
			for(WClause clause : CNF.clauses){
				Set<Term> clause_terms_set = new HashSet<Term>();
				for(Atom atom : clause.atoms){
					clause_terms_set.addAll(atom.terms);
				}
				ArrayList<Term> clause_terms = new ArrayList<Term>(clause_terms_set);
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
					}
				}
			}// end of clause loop
			return new LogDouble(totalGroundings*Math.log(2), true);
		}
	
	public void splitSegments(MLN CNF, Map<Integer, ArrayList<Integer>> firstElemSegmentsToBinCoeff, int predId, ArrayList<Set<Pair>> predEquivalenceClasses){
		int splitTermIndex = 0;
		for(Integer i : firstElemSegmentsToBinCoeff.keySet()){
			splitTermIndex = firstElemSegmentsToBinCoeff.get(i).get(1);
			break;
		}
		for(WClause clause : CNF.clauses){
			Set<Term> termsSplitted = new HashSet<Term>();
			// Find which pred pos pairs to split
			Pair predIdPair = new Pair(predId,splitTermIndex);
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
						int k = firstElemSegmentsToBinCoeff.get(firstElem).get(0);
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
						int k = firstElemSegmentsToBinCoeff.get(firstElem).get(0);
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


	

	
			
	
	
	public static void main(String args[]) throws FileNotFoundException, PredicateNotFound{
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
		
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.clear();
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(0,1)));
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(2)));
		mln.clauses.get(0).atoms.get(0).terms.get(1).domain.clear();
		mln.clauses.get(0).atoms.get(0).terms.get(1).domain.add(new TreeSet<Integer>(Arrays.asList(1)));
		mln.clauses.get(0).atoms.get(0).terms.get(1).domain.add(new TreeSet<Integer>(Arrays.asList(2)));
		mln.clauses.get(0).satHyperCubes.clear();
		mln.clauses.get(0).satHyperCubes.add(true);
		mln.clauses.get(0).satHyperCubes.add(true);
		mln.clauses.get(0).hcNumCopies.clear();
		mln.clauses.get(0).hcNumCopies.add(1);
		mln.clauses.get(0).hcNumCopies.add(1);
		mln.clauses.get(1).atoms.get(0).terms.get(0).domain.clear();
		mln.clauses.get(1).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(0,2)));
		mln.clauses.get(1).atoms.get(0).terms.get(1).domain.clear();
		mln.clauses.get(1).atoms.get(0).terms.get(1).domain.add(new TreeSet<Integer>(Arrays.asList(1)));
		mln.clauses.get(1).satHyperCubes.clear();
		mln.clauses.get(1).satHyperCubes.add(true);
		mln.clauses.get(1).hcNumCopies.clear();
		mln.clauses.get(1).hcNumCopies.add(1);
		//System.out.println(findDontCare(mln));
	}
}
