package org.utd.cs.mln.lmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.print.attribute.Size2DSyntax;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.Evidence;
import org.utd.cs.mln.alchemy.core.HyperCube;
import org.utd.cs.mln.alchemy.core.InCorrectParamsException;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateNotFound;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;
import org.utd.cs.mln.alchemy.util.Parser;

import java.util.Scanner;

public class LiftedPTP {
	public static long  numGroundings = 0;
	public static long numMLNs = 0;
	public static long totalSegments = 0;
	public static double logComb = 0;
	public static boolean queryEvidence = false;
	public static boolean toPrint = false;
	public static boolean lazyDisjoint = false;
	public static boolean closedWorld = true;
	public static Stack<Double> resultStack = new Stack<Double>();
	public static double getPartition(MLN mln, boolean approximate){
		// base case : If every hyperCube of every clause
		if(isBaseCaseReached(mln) == true){
			// resultStack.add(findBaseCaseZ(mln, approximate));
			// return eval_stack(resultStack);
			return findBaseCaseZ(mln,approximate);
		}
		/*
		// Decomposition step
		ArrayList<Set<Pair>> predEquivalenceClasses = new ArrayList<Set<Pair>>();
		Decomposer d = new Decomposer();
		d.findEquivalenceClasses(mln, predEquivalenceClasses);
		ArrayList<MLN> decomposedMLNs = null;
		ArrayList<Integer> sizeOfSegments = new ArrayList<Integer>();
		for(int eqClassId = 0 ; eqClassId < predEquivalenceClasses.size() ; eqClassId++){
			if(d.isDecomposer(predEquivalenceClasses.get(eqClassId), mln)){
				//System.out.println("Doing decomposition...");
				decomposedMLNs = d.initDecomposer(mln, predEquivalenceClasses.get(eqClassId), sizeOfSegments);
				if(decomposedMLNs != null){
					break;
				}
			}
		}
		 
		if(decomposedMLNs != null && decomposedMLNs.size() > 0){
			//System.out.println("Decomposition Done...");
			double totalZ = 1;
			///System.out.println("Printing decomposed MLNs : ");
			for(int mlnId = 0 ; mlnId < decomposedMLNs.size() ; mlnId++){
				//System.out.println("MLN no. : " + mlnId+1);
				MLN decomposedMln = decomposedMLNs.get(mlnId);*/
				/*//
				for(WClause clause : decomposedMln.clauses){
					clause.print();
					System.out.println("HyperCubes : ");
					for(HyperCube hc : clause.hyperCubes){
						System.out.println(hc);
					}
				}*///
		/*
				totalZ *= Math.pow(getPartition(decomposedMln, approximate),sizeOfSegments.get(mlnId));
			}
			return totalZ;
		}*/
		///System.out.println("Doing Splitting...");
		return LSplitIterative.liftedSplit(mln, approximate);
	}
	
	private static double eval_stack(Stack<Double> stack) {
		while(true){
			if(stack.size() == 1){
				return stack.pop();
			}
			double op1 = stack.pop();
			double op = stack.pop();
			if(op == -1.0){
				stack.push(stack.pop()+op1);
			}
			else{
				stack.push(stack.pop()*op1);
			}
		}
	}

	// return (2^#groundings)*exp(sum(for each hypercube in each clause) #hyperCubegroundings*wt*num_copies
	public static double findBaseCaseZ(MLN mln, boolean approximate) {
		double weightedSumClauses = 0.0;
		HashMap<Integer,Set<ArrayList<Integer>>> predsGroundings = new HashMap<Integer,Set<ArrayList<Integer>>>();  
		for(WClause clause : mln.clauses){
			int numSatisfiedHyperCubes = 0;
			for(HyperCube hc : clause.hyperCubes){
				if(hc.satisfied == false){
					continue;
				}
				ArrayList<ArrayList<TreeSet<Integer>>> hyperCubeTuples = LiftedSplit.cartesianProd(hc.varConstants);
				for(Atom atom : clause.atoms){
					Set<ArrayList<Integer>> predGroundings = new HashSet<ArrayList<Integer>>();
					for(ArrayList<TreeSet<Integer>> hyperCubeTuple : hyperCubeTuples){
						ArrayList<Integer> predGrounding = new ArrayList<Integer>();
						for(Term term : atom.terms){
							predGrounding.add(hyperCubeTuple.get(clause.terms.indexOf(term)).iterator().next());
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
				numSatisfiedHyperCubes += hyperCubeTuples.size()*hc.num_copies;
				numGroundings += hyperCubeTuples.size();
			}// end of hyperCube loop
			weightedSumClauses += numSatisfiedHyperCubes * clause.weight.getValue();
		} // end of clause loop
		int totalNumPredGroundings = 0;
		for(Integer predId : predsGroundings.keySet()){
			totalNumPredGroundings += predsGroundings.get(predId).size();
		}
		if(approximate == true){
			return totalNumPredGroundings*Math.log(2) + weightedSumClauses;
		}
		return Math.pow(2, totalNumPredGroundings)*Math.exp(weightedSumClauses);
	}
	
	// Base case is reached when every hyperCube of every clause is either satisfied or empty
	public static boolean isBaseCaseReached(MLN mln) {
		for(WClause clause : mln.clauses){
			for(HyperCube hc : clause.hyperCubes){
				if(hc.satisfied == false && !hc.isEmpty()){
					return false;
				}
			}
		}
		return true;
	}
	/*
	public static ArrayList<MLN> baseSolver(MLN mln){
		
	}*/
	
	public static void parseParams(String[] args, LiftedPTPParams params) throws InCorrectParamsException{
		int argNum = 0;
		while(argNum < args.length){
			if(args[argNum].equalsIgnoreCase("-normal")){
				params.isNormal = true;
			}
			else if(args[argNum].equalsIgnoreCase("-approx")){
				params.approximate = true;
			}
			else if(args[argNum].equalsIgnoreCase("-queryEvidence")){
				params.queryEvidence = true;
			}
			else if(args[argNum].equalsIgnoreCase("-i")){
				argNum++;
				if(argNum >= args.length)
					throw new InCorrectParamsException("No filename after switch -i");
				else
					params.inputFile = args[argNum];
			}
			else if(args[argNum].equalsIgnoreCase("-e")){
				argNum++;
				if(argNum >= args.length)
					throw new InCorrectParamsException("No filename after switch -e");
				else
					params.evidenceFile = args[argNum];
			}	
			else if(args[argNum].equalsIgnoreCase("-o")){
				argNum++;
				if(argNum >= args.length)
					throw new InCorrectParamsException("No filename after switch -o");
				else
					params.resultFile = args[argNum];
			}
			else if(args[argNum].equalsIgnoreCase("-numIter")){
				argNum++;
				if(argNum >= args.length)
					throw new InCorrectParamsException("No value after switch -numIter");
				else
					params.numIter = Integer.parseInt(args[argNum]);
			}
			else if(args[argNum].equalsIgnoreCase("-q")){
				argNum++;
				if(argNum >= args.length)
					throw new InCorrectParamsException("No query after switch -q");
				else{
					params.queryString = args[argNum].replaceAll("\\s", "");
				}
			}
			argNum++;
		}
		if(params.inputFile.equals("")){
			throw new InCorrectParamsException("No input file provided");
		}
		if(params.evidenceFile.equals("")){
			throw new InCorrectParamsException("No evidence file provided");
		}
	}
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws PredicateNotFound 
	 * @throws IncorrectParams 
	 */
	public static void main(String[] args) throws FileNotFoundException, PredicateNotFound {
		LiftedPTPParams params = new LiftedPTPParams();
		try{
			parseParams(args, params);
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println(params);
		PrintWriter writer;
		if(!params.resultFile.equals("")){
			File file = new File (params.resultFile);
			writer = new PrintWriter (new FileOutputStream(file,true),true);
		}
		else{
			writer = new PrintWriter(System.out);
		}
		
		for(int i = 0 ; i >= 0 ; i--){
			for(int j = 0 ; j <= 0 ; j++){
				LiftedPTP.numMLNs = 0;
				double total_time = 0.0;
				long time = System.currentTimeMillis();
				MLN mln = new MLN();
				Parser parser = new Parser(mln);
				//String fn="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/smoke/smoke_mln_99.txt";
				//String filename = new String(fn);
				parser.parseInputMLNFile(params.inputFile);
				String queries[] = params.queryString.split(",");
				ArrayList<String> query_preds = new ArrayList<String>(Arrays.asList(queries));
				
				/*
				// If no query_predicate is specified, then default is all predicates are queries
				if(query_preds.size() == 0){
					for(PredicateSymbol symbol : mln.symbols){
						query_preds.add(symbol.symbol);
					}
				}*/
				//String fn2="//home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/smoke/ptp_smoke/evidence_"+domSize+"_"+evid_per+".txt";
				ArrayList<Evidence> evidList = parser.parseInputEvidenceFile(params.evidenceFile);
				MlnToHyperCube mlnToHyperCube = new MlnToHyperCube();
				
				HashMap<PredicateSymbol,ArrayList<ArrayList<HyperCube>>> predsHyperCubeHashMap = mlnToHyperCube.createPredsHyperCube(evidList, mln, params.isNormal, query_preds, params.queryEvidence);
				int origNumClauses = mln.clauses.size();
				for(int clauseId = 0 ; clauseId < origNumClauses ; clauseId++){
					//System.out.println("creating new clause for clauseId : "+ clauseId);
					mln.clauses.addAll(mlnToHyperCube.createClauseHyperCube(mln.clauses.get(clauseId), predsHyperCubeHashMap, params.isNormal));
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
				// Empty out the predsHyperCube
				for(PredicateSymbol s : predsHyperCubeHashMap.keySet()){
					for(ArrayList<HyperCube> a : predsHyperCubeHashMap.get(s)){
						a.clear();
					}
				}
				
				System.out.println("Time taken to create clauses in hyperCube form : " + (long)(System.currentTimeMillis() - time) + " ms");
				total_time += (double)(System.currentTimeMillis() - time)/1000;
				time = System.currentTimeMillis();
				
				// printing domains
				//for(WClause clause : mln.clauses){
					//clause.print();
				//}
				if(lazyDisjoint == false || params.isNormal == true){
					ArrayList<Set<Pair>> predEquivalenceClasses = new ArrayList<Set<Pair>>();
					NonSameEquivConverter.findEquivalenceClasses(mln, predEquivalenceClasses);
					normalizeMLN(mln, predEquivalenceClasses);
				}

				// manual creation of hyperCubes for testing
				/*
				mln.clauses.get(0).atoms.get(0).terms.get(0).domain.clear();
				mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(0)));
				mln.clauses.get(0).satHyperCubes.add(false);
				mln.clauses.get(0).hcNumCopies.add(1);
				mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(1)));
				mln.clauses.get(0).satHyperCubes.add(false);
				mln.clauses.get(0).hcNumCopies.add(1);
				mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(2)));
				mln.clauses.get(0).satHyperCubes.add(false);
				mln.clauses.get(0).hcNumCopies.add(1);
				
				mln.clauses.get(1).atoms.get(0).terms.get(0).domain.clear();
				mln.clauses.get(1).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(3)));
				mln.clauses.get(1).satHyperCubes.add(false);
				mln.clauses.get(1).hcNumCopies.add(1);
				mln.clauses.get(1).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(4)));
				mln.clauses.get(1).satHyperCubes.add(false);
				mln.clauses.get(1).hcNumCopies.add(1);
				mln.clauses.get(1).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(5)));
				mln.clauses.get(1).satHyperCubes.add(false);
				mln.clauses.get(1).hcNumCopies.add(1);
				*/
				// printing domains
				//for(WClause clause : mln.clauses){
					//clause.print();
				//}
				System.out.println("Time taken to disjoint all hyperCubes : " + (long)(System.currentTimeMillis() - time) + " ms");
				
				total_time += (double)(System.currentTimeMillis() - time)/1000;
				System.out.println("Total number of clauses : " + mln.clauses.size());
				double Z = 0.0;
				time = System.currentTimeMillis();
				if(params.approximate == true){
					ArrayList<Double> zArray = new ArrayList<Double>();
					int num_iter = params.numIter;
					for(int iter = 0 ; iter < num_iter ; iter++){
						System.out.println("iteration no. : " + iter);
						MLN tempMln = new MLN();
						for(WClause clause : mln.clauses){
							WClause newClause = MLN.create_new_clause(clause);
							tempMln.clauses.add(newClause);
						}
						LisApproxInference lisApproxInference = new LisApproxInference();
						LogDouble curZ = lisApproxInference.doLvApproxPartitionBinomial(tempMln);
						zArray.add(curZ.getValue());	
					}
					System.out.println("Time taken for " + num_iter + " iterations : " + (double)(System.currentTimeMillis()-time)/1000.0 + " sec");
					total_time += (double)(System.currentTimeMillis() - time)/1000;
					System.out.println("zArray : " + zArray);
					double avgZ = findAverageZ(zArray);
					double stdZ = findSDZ(zArray);
					System.out.println("Average Z : " + avgZ);
					System.out.println("Standard dev Z : " + stdZ);
					writer.println(avgZ+","+stdZ+","+total_time);
				}
				else{
					PtpExactInference ptpExactInference = new PtpExactInference();
					Z = ptpExactInference.doLvExactPartition(mln).value;
					//Z = ptpExactInference.doLvExactPartitionDisjointSeg(mln).value;
					System.out.println("Time taken for exact inference : " + (double)(System.currentTimeMillis()-time)/1000.0 + " sec");
					total_time += (double)(System.currentTimeMillis() - time)/1000;	
					System.out.println("No. of mlns formed : " + numMLNs);
					System.out.println("Z = " + Z);
					writer.println(Z+","+total_time+","+numMLNs);
				}
				System.out.println("Total Time taken : " + total_time + " sec");
			}
		}
		writer.close();
	}

	private static Double findSDZ(ArrayList<Double> zArg) {
		ArrayList<Double> z = new ArrayList<Double>(zArg);
		
		double sum = 0.0;
		for(int i = 0 ; i < z.size() ; i++){
			sum += z.get(i);
		}
		double ave = sum/z.size();
		double sumSqDev = 0.0;
		for(int i = 0 ; i < z.size() ; i++){
			sumSqDev += (z.get(i)-ave)*(z.get(i)-ave);
		}
		double std = Math.sqrt(sumSqDev/z.size());
		return std;
	}

	private static double findAverageZ(ArrayList<Double> zArg) {
		ArrayList<Double> z = new ArrayList<Double>(zArg);
		//System.out.println("Calculating average...z = " + z);
		double maxZ = z.get(0);
		for(int i = 1 ; i < z.size() ; i++){
			if(z.get(i) > maxZ){
				maxZ = z.get(i);
			}
		}
		double result = 0.0;
		for(int i = 0 ; i < z.size() ; i++){
			z.set(i, z.get(i) - maxZ);
		}
		//System.out.println("z = " + z);
		for(int i = 0 ; i < z.size() ; i++){
			result += Math.exp(z.get(i));
		}
		return Math.log((result)/z.size()) + maxZ;
	}

	private static void normalizeMLN(MLN mln,
			ArrayList<Set<Pair>> predEquivalenceClasses) {
	
		// Create equivalence classes of terms
		Map<Term,Integer> termsToEquivalenceClassesIndices = new HashMap<Term,Integer>();
		// Create classes of segments of each equivalence class
		ArrayList<Set<TreeSet<Integer>>> allEqClassesDjSegments = new ArrayList<Set<TreeSet<Integer>>>();
		for(int i = 0 ; i < predEquivalenceClasses.size() ; i++){
			allEqClassesDjSegments.add(new HashSet<TreeSet<Integer>>());
		}
		
		
		for(WClause clause : mln.clauses){
			for(Atom atom : clause.atoms){
				for(int termId = 0 ; termId < atom.terms.size() ; termId++){
					Pair pair = new Pair(atom.symbol.id,termId);
					for(int j = 0 ; j < predEquivalenceClasses.size() ; j++){
						if(predEquivalenceClasses.get(j).contains(pair)){
							if(!termsToEquivalenceClassesIndices.containsKey(atom.terms.get(termId))){
								termsToEquivalenceClassesIndices.put(atom.terms.get(termId), j);
								allEqClassesDjSegments.get(j).addAll((ArrayList<TreeSet<Integer>>)DeepCopyUtil.copy(atom.terms.get(termId).domain));
							}
							break;
						}
					}
				}
			}
		}
		//System.out.println(termsToEquivalenceClassesIndices);
		// create lists from set
		ArrayList<ArrayList<TreeSet<Integer>>> allEqClassesDjSegmentsList = new ArrayList<ArrayList<TreeSet<Integer>>>();
		for(Set<TreeSet<Integer>> segments : allEqClassesDjSegments){
			allEqClassesDjSegmentsList.add(new ArrayList<TreeSet<Integer>>(segments));
		}
		
		// create disjoint segments for each term
		for(ArrayList<TreeSet<Integer>> segments : allEqClassesDjSegmentsList){
			LisApproxInference.createDisjointIdenticalSegments(segments);
		}
		
		// Create map : firstelement > segmentId
		ArrayList<HashMap<Integer, Integer>> allEqClassesFirstElemToSegmentMap = new ArrayList<HashMap<Integer,Integer>>();
		for(int tId = 0 ; tId < allEqClassesDjSegmentsList.size() ; tId++){
			HashMap<Integer,Integer> firstElemToSegmentMap = new HashMap<Integer,Integer>();
			for(int i = 0 ; i < allEqClassesDjSegmentsList.get(tId).size() ; i++){
				firstElemToSegmentMap.put(allEqClassesDjSegmentsList.get(tId).get(i).iterator().next(), i);
			}
			allEqClassesFirstElemToSegmentMap.add(firstElemToSegmentMap);
		}
		updateClauseSegments(mln, allEqClassesFirstElemToSegmentMap, allEqClassesDjSegmentsList, termsToEquivalenceClassesIndices);
	}

	private static void updateClauseSegments(
			MLN mln,
			ArrayList<HashMap<Integer, Integer>> allEqClassesFirstElemToSegmentMap,
			ArrayList<ArrayList<TreeSet<Integer>>> allEqClassesDjSegmentsList,
			Map<Term, Integer> termsToEquivalenceClassesIndices) {
		
		for(WClause clause : mln.clauses){
			Set<Term> clause_terms = new HashSet<Term>();
			for(Atom atom : clause.atoms){
				for(Term term : atom.terms)
					clause_terms.add(term);
			}
			for(Term term : clause_terms){
				int eqClassIndex =  termsToEquivalenceClassesIndices.get(term);
				int numSegments = term.domain.size();
				for(int segId = 0 ; segId < numSegments ; segId++){
					while(true){
						int firstElem = term.domain.get(segId).iterator().next();
						int djSegId = allEqClassesFirstElemToSegmentMap.get(eqClassIndex).get(firstElem);
						if(allEqClassesDjSegmentsList.get(eqClassIndex).get(djSegId).size() == term.domain.get(segId).size())
							break;
						term.domain.get(segId).removeAll(allEqClassesDjSegmentsList.get(eqClassIndex).get(djSegId));
						term.domain.add(new TreeSet<Integer>(allEqClassesDjSegmentsList.get(eqClassIndex).get(djSegId)));
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
