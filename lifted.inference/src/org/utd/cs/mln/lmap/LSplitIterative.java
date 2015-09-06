package org.utd.cs.mln.lmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.comb;

public class LSplitIterative {
	// Task : Given an MLN, it splits (in lifted manner) an atom and returns Z
		// Input : MLN mln
		// Output : A Double number indicating Z of input MLN
		static int count = 0;
		public static double liftedSplit(MLN mln, boolean approximate){
			
			// Find list of predicates : 
			Set<Integer> predIdMap = new HashSet<Integer>();
			for(WClause clause : mln.clauses){
				for(Atom atom : clause.atoms){
					predIdMap.add(atom.symbol.id);
				}
			}
			while(true){
				if(LiftedPTP.isBaseCaseReached(mln)){
					return LiftedPTP.findBaseCaseZ(mln, approximate);
				}
				Atom atomToSplitOn = choosePredToSplitOn(mln);
				int predId = atomToSplitOn.symbol.id;
				// See if predId is singleton or not, if it is singleton, then apply binomial, else split on all groundings
				boolean applyBinomial = false;
				if(isSingletonAtom(atomToSplitOn)){
					doLSplitting(mln, predId);
				}

			}
		}
		
		private static void doLSplitting(MLN mln, int predId) {
			
			// Given an MLN and predId, split on all segments of predId. First create disjoint or identical
			// segments, and then split.
			
			// Store info about atoms and terms in clauses which are pred (predId)
			HashMap<Integer,ArrayList<Integer>> clausesBinomialTermIndices = new HashMap<Integer,ArrayList<Integer>>(); // For each clauseId, it stores list of terms which are arguments of predicate predId
			HashMap<Integer,ArrayList<Boolean>> clausesBinomialPredSigns = new HashMap<Integer,ArrayList<Boolean>>(); // For each clauseId, it stores list of signs for each occurrence of predId. Sign = True means predId came with negative sign, else positive.
			ArrayList<HashMap<Integer,Integer>> clausesTermFreqCountList = new ArrayList<HashMap<Integer,Integer>>(); // For each clause, it stores frequency of each term in that clause. HashMap key : termId, value : frequency
			ArrayList<HyperCube> segments = new ArrayList<HyperCube>();
			for(int clauseId = 0 ; clauseId < mln.clauses.size() ; clauseId++){
				WClause clause = mln.clauses.get(clauseId);
				///System.out.println("Clause HyperCube : " + clause.hyperCubes);
				ArrayList<Integer> binomialClauseTermIndices = new ArrayList<Integer>();
				ArrayList<Boolean> binomialClausePredSigns = new ArrayList<Boolean>();
				HashMap<Integer,Integer> termFreqCount = new HashMap<Integer,Integer>();
				for(Atom atom : clause.atoms){
					for(Term term : atom.terms){
						if(termFreqCount.containsKey(clause.terms.indexOf(term))){
							termFreqCount.put(clause.terms.indexOf(term), termFreqCount.get(clause.terms.indexOf(term))+1);
						}
						else{
							termFreqCount.put(clause.terms.indexOf(term), 1);
						}
					}
					if(predId == atom.symbol.id){
						binomialClauseTermIndices.add(clause.terms.indexOf(atom.terms.get(0))); // We need to get only first term because binomial atom is unary
						binomialClausePredSigns.add(clause.sign.get(clause.atoms.indexOf(atom)));
					}
				}
				clausesTermFreqCountList.add(termFreqCount);
				
				// If there is no binomial atom in this clause, then no need to go further
				if(binomialClauseTermIndices.size() == 0)
					continue;
				
				clausesBinomialTermIndices.put(clauseId, binomialClauseTermIndices);
				clausesBinomialPredSigns.put(clauseId, binomialClausePredSigns);
				///System.out.println("termFreqCount : " + termFreqCount.toString());
				///System.out.println("binomialClauseTermIndices : " + binomialClauseTermIndices.toString());
				///System.out.println("binomialClausePredSigns : " + binomialClausePredSigns.toString());
				for(HyperCube hyperCube : clause.hyperCubes){
					for(Integer binomialTermIndex : binomialClauseTermIndices){
						HyperCube projectedHyperCube = new HyperCube(hyperCube.varConstants.get(binomialTermIndex));
						/*
						if(hyperCubeClauseListHashMap.containsKey(projectedHyperCube)){
							hyperCubeClauseListHashMap.get(projectedHyperCube).add(clauseId);
						}
						else{
							hyperCubeClauseListHashMap.put(projectedHyperCube, new HashSet<Integer>(Arrays.asList(clauseId)));
						}*/
						segments.add(projectedHyperCube);
					}
				}
			}
			Decomposer.createDisjointHyperCubes(segments);
			
			// Create hashmap : key -> first element of segment, value -> segmentindex
			HashMap<Integer,Integer> firstElementToSegmentMap = new HashMap<Integer,Integer>();
			for(int i = 0 ; i < segments.size() ; i++){
				firstElementToSegmentMap.put(segments.get(i).varConstants.get(0).iterator().next(), i);
			}
			// Now we have disjoint segments (original hyperCubes are not yet broken). 
			// Now we break original hyperCubes according to disjoint segments and then split on predId
			// Find total number of groundings of pred, which is just sum of size of all segments (since they are disjoint)
			int numPredGroundings = 0;
			for(HyperCube hyperCube : segments){
				//System.out.println(hyperCube);
				numPredGroundings += hyperCube.varConstants.get(0).size();
			}
			//System.out.println("Num pred Groundings : " + numPredGroundings);
			binomialSplitter(mln, predId, clausesTermFreqCountList, clausesBinomialTermIndices, clausesBinomialPredSigns, segments, firstElementToSegmentMap);
		}
		
		private static void binomialSplitter(
				MLN mln,
				int predId,
				ArrayList<HashMap<Integer, Integer>> clausesTermFreqCountList,
				HashMap<Integer, ArrayList<Integer>> clausesBinomialTermIndices,
				HashMap<Integer, ArrayList<Boolean>> clausesBinomialPredSigns,
				ArrayList<HyperCube> segments,
				HashMap<Integer, Integer> firstElementToSegmentMap) {

			// TODO : implement set difference
			for(Integer clauseId : clausesBinomialPredSigns.keySet()){
				WClause clause = mln.clauses.get(clauseId);
				for(Integer termId : clausesBinomialTermIndices.get(clauseId)){
					int origNumHc = clause.hyperCubes.size();
					for(int hcId = origNumHc - 1 ; hcId >= 0 ; hcId--){
						HyperCube hyperCube = clause.hyperCubes.get(hcId);
						Set<Integer> origSegment = hyperCube.varConstants.get(termId);
						ArrayList<Integer> djSegIndices = new ArrayList<Integer>();
						while(origSegment.size() > 0){
							int segIndex = firstElementToSegmentMap.get(origSegment.iterator().next());
							djSegIndices.add(segIndex);
							origSegment.removeAll(segments.get(segIndex).varConstants.get(0));
						}
						hyperCube.varConstants.get(termId).addAll(segments.get(djSegIndices.get(0)).varConstants.get(0));
						for(int i = 1 ; i < djSegIndices.size() ; i++){
							HyperCube newHyperCube = new HyperCube(hyperCube);
							newHyperCube.varConstants.get(termId).clear();
							newHyperCube.varConstants.get(termId).addAll(segments.get(djSegIndices.get(i)).varConstants.get(0));
							clause.hyperCubes.add(newHyperCube);
						}
					}
				}
			}
			
			// Now split on each segment
			ArrayList<Integer> segmentCases = new ArrayList<Integer>();
			// 0th is for false, 1st is true
			ArrayList<ArrayList<TreeSet<Integer>>> segmentsPartitions = new ArrayList<ArrayList<TreeSet<Integer>>>();
			segmentsPartitions.add(new ArrayList<TreeSet<Integer>>());
			segmentsPartitions.add(new ArrayList<TreeSet<Integer>>());
			
			double branchFactor = 0.0;
			for(HyperCube segment : segments){
				double rand_num = Math.random();
				int segmentSize = segment.varConstants.get(0).size();
				Set<Integer> segmentSet = new HashSet<Integer>(segment.varConstants.get(0));
				// Now create cumulative intervals of prob like [nc0/2^n, (nc0+nc1)/2^n,...(nc0+nc1+..ncn)/2^n]. We need not create all intervals, the
				// moment our cumulative sum exceeds rand_num, we can stop 
				double totalCases = Math.pow(2, segmentSize);
				double cumulativeSum = 0.0;
				int caseSelected = 0;
				for(int k = 0 ; k <= segmentSize ; k++){
					cumulativeSum += comb.findComb(segmentSize, k);
					if(cumulativeSum/totalCases > rand_num){
						caseSelected = k;
						branchFactor += comb.findCombLog(segmentSize, caseSelected);
						break;
					}
				}
				segmentCases.add(caseSelected);
				TreeSet<Integer> falsePartiton = new TreeSet<Integer>();
				TreeSet<Integer> truePartiton = new TreeSet<Integer>();
				for(int i = 0 ; i < segmentSize ; i++){
					if(i <= caseSelected){
						truePartiton.add(segmentSet.iterator().next());
					}
					else{
						falsePartiton.add(segmentSet.iterator().next());
					}
				}
				segmentsPartitions.get(0).add(falsePartiton);
				segmentsPartitions.get(1).add(truePartiton);
			}

			for(Integer clauseId : clausesBinomialPredSigns.keySet()){
				WClause clause = mln.clauses.get(clauseId);
				// For current clause, extract indices of terms which are arguments of pred
				ArrayList<Integer> binomialTermIndices = new ArrayList<Integer>(clausesBinomialTermIndices.get(clauseId));
				ArrayList<Integer> termsToRemove = new ArrayList<Integer>();
				
				Collections.sort(binomialTermIndices,Collections.reverseOrder());
				for(Integer termId : binomialTermIndices){
					boolean termToRemove = true;
					outerloop:
					for(Atom atom : clause.atoms){
						if(atom.symbol.id != predId){
							for(Term term  : atom.terms){
								if(termId == clause.terms.indexOf(term)){
									termToRemove = false;
									break outerloop;
								}
							}
						}
					}
					if(termToRemove == true){
						termsToRemove.add(termId);
					}
				}
				// For current clause, extract signs of all occurrences of pred in this clause. Note that size pf
				// binomialTermIndices and binomialPredSigns is same because for each occurrence of pred in a clause
				// different term appears i.e. no S(x) V S(x) type is allowed
				ArrayList<Boolean> binomialPredSigns = new ArrayList<Boolean>(clausesBinomialPredSigns.get(clauseId));
				int numNewHyperCubes = (int)Math.pow(2.0, binomialPredSigns.size());
				// Go over each hyperCube of this clause and apply binomial. We are going from last hyperCube, because some hyperCube may get deleted
				for(int hcId = clause.hyperCubes.size() - 1 ; hcId >= 0 ; hcId--){
					HyperCube curHyperCube = clause.hyperCubes.get(hcId);
					int numBits = binomialPredSigns.size();
					
					for(int c = 0 ; c < numNewHyperCubes ; c++){
						int temp = c;
						HyperCube h = new HyperCube(curHyperCube);
						boolean sat = false;
						boolean toAdd = true;
						//int sameSegmentNumTuples = 1;
						for(int bitNum = 0 ; bitNum < numBits ; bitNum++){
							if(temp%2 == 1 && binomialPredSigns.get(bitNum) == false){
								sat = true;
							}
							if(temp%2 == 0 && binomialPredSigns.get(bitNum) == true){
								sat = true;
							}
							int termId = binomialTermIndices.get(bitNum);
							if(segmentsPartitions.get(temp%2).get(bitNum).size() == 0){
								toAdd = false;
								break;
							}
							if(termsToRemove.contains(termId)){
								h.num_copies *= segmentsPartitions.get(temp%2).get(bitNum).size();
								h.varConstants.remove(termId);
							}
							else{
								h.varConstants.set(termId, segmentsPartitions.get(temp%2).get(bitNum));
							}
							temp = temp/2;
						}
						if(sat == true){
							h.satisfied = true;
						}
						if(toAdd == true){
							clause.hyperCubes.add(h);
						}
					} // end of c loop

				} // end of hyperCube loop
				
				// Remove terms and atoms
				for(Integer termId : termsToRemove){
					clause.terms.remove((int)termId);
				}
				for(int atomId = clause.atoms.size() - 1 ; atomId >= 0 ; atomId--){
					if(clause.atoms.get(atomId).symbol.id == predId){
						clause.atoms.remove(atomId);
					}
				}

			} // end of clause loop 

		}

		private static Atom choosePredToSplitOn(MLN mln) {
			// Return first singleton pred found, otherwise return first pred found
			Atom atomOfSelectedPred = null;
			for(WClause clause : mln.clauses){
				for(Atom atom : clause.atoms){
					if(atomOfSelectedPred == null){
						atomOfSelectedPred = atom;
					}
					if(isSingletonAtom(atom) == true){
						atomOfSelectedPred = atom;
						return atomOfSelectedPred;
					}
				}
			}
			return atomOfSelectedPred;
		}
		private static boolean isSingletonAtom(Atom atom) {
			if(atom.terms.size() == 1){
				return true;
			}
			return false;
		}

}
