package org.utd.cs.mln.lmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Decomposer;
import org.utd.cs.mln.alchemy.core.LDecomposer;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.comb;

public class PTPUtil {

	/**
	 * @param args
	 */
	public static LogDouble CNFWeight(MLN mln)
	{
		/*for(unsigned int i=0;i<CNF.size();i++)
		{
			if(CNF[i]->satisfied)
				cout<<"Satisfied v";
			CNF[i]->print();
		}
		*/
		//ArrayList<Atom> removedAtoms = new ArrayList<Atom>();
		//Set<Integer> completedIds = new HashSet<Integer>();
		LogDouble totalWeight = new LogDouble(1.0,false);
		MLN remainingCNF = new MLN();
		for(int i=0;i<mln.clauses.size();i++)
		{
			WClause nClause = MLN.create_new_clause(mln.clauses.get(i));
			int numHyperCubes = mln.clauses.get(i).satHyperCubes.size();
			Set<Term> nClause_terms = new HashSet<Term>();
			for(Atom atom : nClause.atoms){
				nClause_terms.addAll(atom.terms);
			}
			int sz = 0;
			for(int n = numHyperCubes-1 ; n >= 0 ; n--){
				if(mln.clauses.get(i).satHyperCubes.get(n) == false){
					continue;
				}
				int numTuples = 1;
				for(Term t : nClause_terms){
					numTuples *= t.domain.get(n).size();
					t.domain.remove(n);
				}
				sz += (numTuples*mln.clauses.get(i).hcNumCopies.get(n));
				nClause.satHyperCubes.remove(n);
				nClause.hcNumCopies.remove(n);
			}
			remainingCNF.clauses.add(nClause);
			/*
			for(HyperCube hc : mln.clauses.get(i).hyperCubes)
			{
				
				sz += hc.getNumTuples();
			}*/
			//totalWeight += CNF[i]->weight*LogDouble(sz,false);
			//if(!CNF[i]->weight.is_zero)
			{
				//System.out.println(mln.clauses.get(i).weight);
				LogDouble tmpL = mln.clauses.get(i).weight.power((double)sz);
				//double tmp = Math.exp(tmpL.getValue());
				//LogDouble newWt = new LogDouble(tmp,true);
				totalWeight = totalWeight.multiply(tmpL);
			}
		}
		int totalDontcare = findDontCare(mln);
		//LogDouble d = new LogDouble((double)totalDontcare,false);
		LogDouble out = new LogDouble((double)1,false);
		LogDouble c = new LogDouble((double)2,false);
		out = c.power(totalDontcare);
		LogDouble tVal = totalWeight.multiply(out);
		mln.clearData();
		mln = remainingCNF;

		return tVal;
	}
	
	public static int findDontCare(MLN mln) {
		int numGroundings = 0;
		double weightedSumClauses = 0.0;
		HashMap<Integer,Set<ArrayList<Integer>>> predsGroundings = new HashMap<Integer,Set<ArrayList<Integer>>>();
		int numSatisfiedHyperCubes = 0;
		for(WClause clause : mln.clauses){
			if(clause.isSatisfied() == false || clause.atoms.size() == 0){
				continue;
			}
			Set<Term> clause_terms_set = new HashSet<Term>();
			for(Atom atom : clause.atoms){
				clause_terms_set.addAll(atom.terms);
			}
			ArrayList<Term> clause_terms = new ArrayList<Term>(clause_terms_set);
			int numHyperCubes = clause.satHyperCubes.size();
			for(int hcId = 0 ; hcId < numHyperCubes ; hcId++){
				if(clause.satHyperCubes.get(hcId) == false){
					continue;
				}
				ArrayList<TreeSet<Integer>> varConstants = new ArrayList<TreeSet<Integer>>();
				for(Term t : clause_terms){
					varConstants.add(new TreeSet<Integer>(t.domain.get(hcId)));
				}
				ArrayList<ArrayList<TreeSet<Integer>>> hyperCubeTuples = LiftedSplit.cartesianProd(varConstants);
				for(Atom atom : clause.atoms){
					Set<ArrayList<Integer>> predGroundings = new HashSet<ArrayList<Integer>>();
					for(ArrayList<TreeSet<Integer>> hyperCubeTuple : hyperCubeTuples){
						ArrayList<Integer> predGrounding = new ArrayList<Integer>();
						for(Term term : atom.terms){
							predGrounding.add(hyperCubeTuple.get(clause_terms.indexOf(term)).iterator().next());
						}
						predGroundings.add(predGrounding);
					}
					if(predsGroundings.containsKey(atom.symbol.id)){
						predsGroundings.get(atom.symbol.id).addAll(predGroundings);
					}
					else{
						predsGroundings.put(atom.symbol.id, predGroundings);
					}
				} // end of atom loop
				//numSatisfiedHyperCubes += hyperCubeTuples.size();
			}// end of hyperCube loop
		} // end of clause loop
		int totalDontCarePreds = 0;
		for(Integer pId : predsGroundings.keySet()){
			totalDontCarePreds += predsGroundings.get(pId).size();
		}
		return totalDontCarePreds;
	}
	
	public static LogDouble CNFWeightPropositional(MLN mln) {
		LogDouble totalWeight = new LogDouble(1.0,false);
		for(WClause clause : mln.clauses){
			LogDouble tmpL = clause.weight.power(clause.hcNumCopies.get(0)); // first entry of clause hcNumCopies is for true segments
			totalWeight = totalWeight.multiply(tmpL);
		}
		int totalDontcare = findDontCarePropositional(mln);
		LogDouble out = new LogDouble((double)1,false);
		LogDouble c = new LogDouble((double)2,false);
		out = c.power(totalDontcare);
		LogDouble tVal = totalWeight.multiply(out);
		mln.clearData();
		return tVal;
	}

	public static int findDontCarePropositional(MLN mln) {
		Set<Integer> dontCarePredIds = new HashSet<Integer>();
		for(WClause clause : mln.clauses){
			if(clause.hcNumCopies.get(1) == 0)
				continue;
			for(Atom atom : clause.atoms){
				dontCarePredIds.add(atom.symbol.id);
			}
		}
		return dontCarePredIds.size();
	}
	
	public static void simplifyCNFPropositional(MLN CNF){
		// clear domain of each term in each clause. Count number of true and false segments and keep only two entries in satHyperCubes and numCopies,
		// one for true segments and one for false segments
		for(WClause clause : CNF.clauses){
			for(Atom atom : clause.atoms){
				for(Term term : atom.terms){
					term.domain.clear();
				}
			}
			int numSegments = clause.satHyperCubes.size();
			int trueCount = 0;
			int falseCount = 0; 
			for(int i = 0 ; i < numSegments ; i++){
				if(clause.satHyperCubes.get(i) == true){
					trueCount += clause.hcNumCopies.get(i);
				}
				else{
					falseCount += clause.hcNumCopies.get(i);
				}
			}
			clause.satHyperCubes.clear();
			clause.hcNumCopies.clear();
			clause.satHyperCubes.add(true);
			clause.satHyperCubes.add(false);
			clause.hcNumCopies.add(trueCount);
			clause.hcNumCopies.add(falseCount);
		}
	}


	public static int sampleBinomial(int n) {
		double rand_num = Math.random();
		double totalCases = Math.pow(2, n);
		double cumulativeSum = 0.0;
		int caseSelected = 0;
		for(int k = 0 ; k <= n ; k++){
			cumulativeSum += comb.findComb(n, k);
			if(cumulativeSum/totalCases > rand_num){
				caseSelected = k;
				break;
			}
		}
		return caseSelected;
	}

	public static void main(String[] args) {
		int onecount = 0, zerocount = 0;
		for(int i = 0 ; i < 100 ; i++){
			if(sampleBinomial(1) == 1)
				onecount++;
			else
				zerocount++;
		}
		System.out.println(onecount);
		System.out.println(zerocount);
	}

	public static MLN splitPropositionalPred(MLN CNF, int predId, int k) {
		
		MLN newMln = new MLN();
		
		for(WClause clause : CNF.clauses){
			WClause new_clause = MLN.create_new_clause(clause);
			boolean satisfied = false;
			for(int atomId = new_clause.atoms.size() - 1 ; atomId >= 0 ; atomId--){
				if(new_clause.atoms.get(atomId).symbol.id != predId)
					continue;
				if(k == 0 && new_clause.sign.get(atomId)==true){
					satisfied = true;
				}
				if(k == 1 && new_clause.sign.get(atomId)==false){
					satisfied = true;
				}
				new_clause.atoms.remove(atomId);
				new_clause.sign.remove(atomId);
			}
			if(satisfied){
				int numSegments = new_clause.satHyperCubes.size();
				for(int i = 0 ; i < numSegments ; i++){
					new_clause.satHyperCubes.set(i, true);
				}
			}
			newMln.clauses.add(new_clause);
		}
		newMln.max_predicate_id = CNF.max_predicate_id;
		newMln.symbols = (List<PredicateSymbol>) DeepCopyUtil.copy(CNF.symbols);
		newMln.setMLNPoperties();
		return newMln;
	}
	
	public static ArrayList<MLN> decomposeCNF(MLN CNF, ArrayList<Integer> powerFactors) {
		ArrayList<Decomposer> decomposer_list = new ArrayList<Decomposer>();
		LDecomposer lDecomposer = new LDecomposer();
		lDecomposer.find_decomposer(CNF, decomposer_list);
		
		//int powerFactor = 0;
		// Print
		/*
		System.out.print("Decomposer={ ");
		for(int i=0;i<decomposer_list.size();i++)
		{
			decomposer_list.get(i).print();
		}
		System.out.println("}");
		*/
		ArrayList<MLN> decomposedMlns = new ArrayList<MLN>();
		if(decomposer_list.size()==0){
			powerFactors.add(1);
			return decomposedMlns;
		}
		else
		{
			MLN remainingClausesMln = null; 

			//for(int i=0;i<decomposer_list.size();i++)
			for(int i=0;i<decomposer_list.size();i++) // currently deal with only 1 decomposer at a time
			{
				remainingClausesMln = new MLN(); // This MLN stores those clauses in which decomposer doesn't exist
				//remainingClausesMln.max_predicate_id = CNF.max_predicate_id;
				//remainingClausesMln.symbols = (List<PredicateSymbol>) DeepCopyUtil.copy(CNF.symbols);
				powerFactors.clear();
				decomposedMlns.clear();
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
				if(remainingClausesMln.clauses.size() > 0 || powerFactors.size() > 1 || powerFactors.get(0) > 1)
					break;
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
		if(powerFactors.size() > 1 || powerFactors.get(0) > 1)
			CNF.clauses.clear();
		// set MLN prop for each new MLN
		for(MLN dMln : decomposedMlns){
			dMln.setMLNPoperties();
		}
		return decomposedMlns;
	}
	
	private static void updateClauseSegments(MLN CNF,
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
	
	private static ArrayList<TreeSet<Integer>> createDisjointSegments(MLN CNF, Decomposer decomposer) {
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

	public static boolean isPropositionalSatisfied(WClause clause) {
		if(clause.hcNumCopies.get(1) == 0)
			return true;
		else
			return false;
	}

}
