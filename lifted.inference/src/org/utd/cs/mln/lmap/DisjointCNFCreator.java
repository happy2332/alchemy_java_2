package org.utd.cs.mln.lmap;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.utd.cs.gm.core.LogDouble;
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

import com.sun.org.apache.xpath.internal.FoundIndex;

public class DisjointCNFCreator {

	/**
	 * @param args
	 */
	
	public static void seperateDisjointSegmentsCNF(MLN mln, ArrayList<MLN> disjointCNFList) {
		
		//System.out.println("Segment level disjointing...");
		// Partitions of <clauseId,segId> which should come together in 1 mln. If clauseIndexes size is 5, then there will be 5 disjoint MLNs
		ArrayList<ArrayList<Pair>> clauseSegIndexes = new ArrayList<ArrayList<Pair>>();
		// Partitions of <Atom,segments> list which should come together in 1 mln. Each entry of this arraylist corresponds to different disjoint MLN, and 
		// contains set of <atomId,segmentFirstElementForEachTermList> in that partition.
		
		ArrayList<HashSet<ArrayList<Integer>>> atomSegIds = new ArrayList<HashSet<ArrayList<Integer>>>();
		ArrayList<Integer> emptyClausesIndices = new ArrayList<Integer>(); // We create a separate partition for each empty clause
		//ArrayList<LogDouble> satWts = new ArrayList<LogDouble>();
		//ArrayList<Boolean> lvsat = new ArrayList<Boolean>();
		for(int i=0;i<mln.clauses.size();i++)
		{
			WClause clause = mln.clauses.get(i);
			if(clause.atoms.size()==0)
			{
				//numEmptyClauses++;
				//satWts.add(clause.weight);
				//lvsat.add(clause.isSatisfied());
				emptyClausesIndices.add(i);
				continue;
			}
			
			// for each atom in this clause, search if it already exists in any partition, if exists, find which partition, and break there only
			int numSegments = clause.atoms.get(0).terms.get(0).domain.size();
			for(int s = 0 ; s < numSegments ; s++)
			{
				// Stores, for each <atomId,TermSegmentList> in this clause, which partition it belongs to
				ArrayList<Integer> foundPos = new ArrayList<Integer>();
				for(Atom atom : clause.atoms){
					ArrayList<Integer> atomSegList = new ArrayList<Integer>();
					atomSegList.add(atom.symbol.id);
					for(Term term : atom.terms){
						atomSegList.add(term.domain.get(s).iterator().next());
					}
					// Go over each partition
					for(int k=0;k<atomSegIds.size();k++)
					{
						// If atom already exists in kth partition
						if(atomSegIds.get(k).contains(atomSegList))
						{
							if(!foundPos.contains(k))
								foundPos.add(k);
							break;
						}
					}
				}
				if(foundPos.size() == 0){
					// Create new partition
					HashSet<ArrayList<Integer>> tmpAtomSegIds = new HashSet<ArrayList<Integer>>();
					// Add all <atom ID,segment> of this clause in this partition
					for(Atom atom : clause.atoms){
						ArrayList<Integer> atomSegList = new ArrayList<Integer>();
						atomSegList.add(atom.symbol.id);
						for(Term term : atom.terms){
							atomSegList.add(term.domain.get(s).iterator().next());
						}
						tmpAtomSegIds.add(atomSegList);
					}
					// Add this partition to partitions list
					atomSegIds.add(tmpAtomSegIds);
					// Similarly do for clauses
					ArrayList<Pair> tmpCSegInd = new ArrayList<Pair>();
					tmpCSegInd.add(new Pair(i,s));
					clauseSegIndexes.add(tmpCSegInd);
				}
				// If 1 or more atoms exist in current partitions
				// Now different atoms of this clause might have been found in different partitions, but now we have to merge all those partitions.
				// So we create a new partition which contains merge of all partitions which contain atoms of this clause
				// Finally delete those old partitions which were merged 
				else
				{
					// We have to delete partitions specified in foundPos, so we have to sort them in decreasing order to maintain correct indices
					Collections.sort(foundPos, Collections.reverseOrder());
					
					// New partitions for atoms and clauses
					HashSet<ArrayList<Integer>> tmpAtomSegIds = new HashSet<ArrayList<Integer>>();
					ArrayList<Pair> tmpCSegInds = new ArrayList<Pair>();
					
					// Add current clause in this new clause partition
					tmpCSegInds.add(new Pair(i,s));
					// Add all atoms of this clause in this new partition
					for(Atom atom : clause.atoms)
					{
						ArrayList<Integer> atomSegList = new ArrayList<Integer>();
						atomSegList.add(atom.symbol.id);
						for(Term term : atom.terms){
							atomSegList.add(term.domain.get(s).iterator().next());
						}
						tmpAtomSegIds.add(atomSegList);
					}
					
					// Now loop over partitions to be merged and add their entries to new partition 
					for(Integer it : foundPos)
					{
						for(ArrayList<Integer> a : atomSegIds.get(it))
							tmpAtomSegIds.add(new ArrayList<Integer>(a));
						for(Pair p : clauseSegIndexes.get(it))
							tmpCSegInds.add(new Pair(p));
						atomSegIds.remove((int)it);
						clauseSegIndexes.remove((int)it);
					}
					atomSegIds.add(tmpAtomSegIds);
					clauseSegIndexes.add(tmpCSegInds);
				}
			}
			
		}// End of clause loop
		
		// Now make new MLN for each partition
		for(int i=0;i<clauseSegIndexes.size();i++)
		{
			Set<Integer> clauseIdsSeen = new HashSet<Integer>();
			ArrayList<WClause> tmpClauses = new ArrayList<WClause>();
			Map<Integer,Integer> clauseIdToNewClauseMap = new HashMap<Integer,Integer>(); 
			for(Pair p : clauseSegIndexes.get(i))
			{
				if(!clauseIdsSeen.contains(p.first)){
					clauseIdsSeen.add(p.first);
					WClause newClause = MLN.create_new_clause(mln.clauses.get(p.first));
					Set<Term> termsSeen = new HashSet<Term>();
					for(int atomId = 0 ; atomId < newClause.atoms.size() ; atomId++){
						for(int termId = 0 ; termId < newClause.atoms.get(atomId).terms.size() ; termId++){
							if(!termsSeen.contains(newClause.atoms.get(atomId).terms.get(termId))){
								termsSeen.add(newClause.atoms.get(atomId).terms.get(termId));
								newClause.atoms.get(atomId).terms.get(termId).domain.clear();
								newClause.atoms.get(atomId).terms.get(termId).domain.add(new TreeSet<Integer>(mln.clauses.get(p.first).atoms.get(atomId).terms.get(termId).domain.get(p.second)));
							}
						}
					}
					newClause.hcNumCopies.clear();
					newClause.hcNumCopies.add(mln.clauses.get(p.first).hcNumCopies.get(p.second));
					newClause.satHyperCubes.clear();
					newClause.satHyperCubes.add(mln.clauses.get(p.first).satHyperCubes.get(p.second));
					tmpClauses.add(newClause);
					clauseIdToNewClauseMap.put(p.first, tmpClauses.size()-1);
				}
				else{
					WClause newClause = tmpClauses.get(clauseIdToNewClauseMap.get(p.first));
					Set<Term> termsSeen = new HashSet<Term>();
					for(int atomId = 0 ; atomId < newClause.atoms.size() ; atomId++){
						for(int termId = 0 ; termId < newClause.atoms.get(atomId).terms.size() ; termId++){
							if(!termsSeen.contains(newClause.atoms.get(atomId).terms.get(termId))){
								termsSeen.add(newClause.atoms.get(atomId).terms.get(termId));
								newClause.atoms.get(atomId).terms.get(termId).domain.add(new TreeSet<Integer>(mln.clauses.get(p.first).atoms.get(atomId).terms.get(termId).domain.get(p.second)));
							}
						}
					}
					newClause.hcNumCopies.add(mln.clauses.get(p.first).hcNumCopies.get(p.second));
					newClause.satHyperCubes.add(mln.clauses.get(p.first).satHyperCubes.get(p.second));
				}
			}
			MLN dMLN = new MLN();
			dMLN.clauses.addAll(tmpClauses);
			dMLN.setMLNPoperties();
			disjointCNFList.add(dMLN);
		}
		
		// Make single MLN for empty clauses
		for(Integer emptyClauseIndex : emptyClausesIndices)
		{
			ArrayList<WClause> tmpClauses = new ArrayList<WClause>();
			MLN dMLN = new MLN();
			WClause nClause= new WClause();
			nClause.hcNumCopies.addAll(new ArrayList<Integer>(mln.clauses.get(emptyClauseIndex).hcNumCopies));
			nClause.satHyperCubes.addAll(new ArrayList<Boolean>(mln.clauses.get(emptyClauseIndex).satHyperCubes));
			nClause.weight = new LogDouble(mln.clauses.get(emptyClauseIndex).weight);
			//nClause.satisfied = lvsat.get(i);
			//nClause.weight = satWts.get(i);
			tmpClauses.add(nClause);
			dMLN.clauses.addAll(tmpClauses);
			dMLN.setMLNPoperties();
			disjointCNFList.add(dMLN);
		}
		mln.clearData();
		//System.out.println("Segment level disjointing done...");
	}

	public static void seperateDisjointCNF(MLN mln, ArrayList<MLN> disjointCNFList) {
		
		// Partitions of indices of clauses which should come together in 1 mln. If clauseIndexes size is 5, then there will be 5 disjoint MLNs
		ArrayList<ArrayList<Integer>> clauseIndexes = new ArrayList<ArrayList<Integer>>();
		// Partitions of Atoms which should come together in 1 mln. Each entry of this arraylist corresponds to different disjoint MLN, and 
		// contains list of atomIds in that partition. So idea is :
		// If any of the atom ID in the clause (which we want to decide for) appears in a partition, then that clause must go into corresponding 
		// clause Indexes partition. So there is 1-1 mapping between partitions of atoms and partitions of clauses.
		// If no atom matches with any existing partition, then create new partition (for new disjoint mln), and also create new parttion fo clause.
		
		ArrayList<ArrayList<Integer>> atomIds = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> emptyClausesIndices = new ArrayList<Integer>(); // We create a separate partition for each empty clause
		//ArrayList<LogDouble> satWts = new ArrayList<LogDouble>();
		//ArrayList<Boolean> lvsat = new ArrayList<Boolean>();
		for(int i=0;i<mln.clauses.size();i++)
		{
			WClause clause = mln.clauses.get(i);
			if(clause.atoms.size()==0)
			{
				//numEmptyClauses++;
				//satWts.add(clause.weight);
				//lvsat.add(clause.isSatisfied());
				emptyClausesIndices.add(i);
				continue;
			}
			
			// Stores, for each atom in this clause, which partition it belongs to
			ArrayList<Integer> foundPos = new ArrayList<Integer>();
			
			// for each atom in this clause, search if it already exists in any partition, if exists, find which partition, and break there only
			for(int j=0;j<clause.atoms.size();j++)
			{
				// Go over each partition
				for(int k=0;k<atomIds.size();k++)
				{
					// If atom already exists in kth partition
					if(atomIds.get(k).contains(clause.atoms.get(j).symbol.id))
					{
						if(!foundPos.contains(k))
							foundPos.add(k);
						break;
					}
				}
			}
			
			// If no atom exists in current partitions, create new partition
			if(foundPos.size()==0)
			{
				// Create new partition
				ArrayList<Integer> tmpIds = new ArrayList<Integer>();
				
				// Add all atom IDs of this clause in this partition
				for(int j=0;j<clause.atoms.size();j++)
					tmpIds.add(clause.atoms.get(j).symbol.id);
				
				// Add this partition to partitions list
				atomIds.add(tmpIds);
				
				// Similarly do for clauses
				ArrayList<Integer> tmpCind = new ArrayList<Integer>();
				tmpCind.add(i);
				clauseIndexes.add(tmpCind);
			}
			// If 1 or more atoms exist in current partitions
			// Now different atoms of this clause might have been found in different partitions, but now we have to merge all those partitions.
			// So we create a new partition which contains merge of all partitions which contain atoms of this clause
			// Finally delete those old partitions which were merged 
			else
			{
				// We have to delete partitions specified in foundPos, so we have to sort them in decreasing order to maintain correct indices
				Collections.sort(foundPos, Collections.reverseOrder());
				
				// New partitions for atoms and clauses
				ArrayList<Integer> tmpAtomIds = new ArrayList<Integer>();
				ArrayList<Integer> tmpCInds = new ArrayList<Integer>();
				
				// Add current clause in this new clause partition
				tmpCInds.add(i);
				// Add all atoms of this clause in this new partition
				for(int j=0;j<clause.atoms.size();j++)
				{
					tmpAtomIds.add(clause.atoms.get(j).symbol.id);
				}
				
				// Now loop over partitions to be merged and add their entries to new partition 
				for(Integer it : foundPos)
				{
					for(int j=0;j<atomIds.get(it).size();j++)
						tmpAtomIds.add(atomIds.get(it).get(j));
					for(int j=0;j<clauseIndexes.get(it).size();j++)
						tmpCInds.add(clauseIndexes.get(it).get(j));
					atomIds.remove((int)it);
					clauseIndexes.remove((int)it);
				}
				atomIds.add(tmpAtomIds);
				clauseIndexes.add(tmpCInds);
			}
		}// End of clause loop
		
		// Now make new MLN for each partition
		for(int i=0;i<clauseIndexes.size();i++)
		{
			ArrayList<WClause> tmpClauses = new ArrayList<WClause>();
			for(int j=0;j<clauseIndexes.get(i).size();j++)
			{
				tmpClauses.add(MLN.create_new_clause(mln.clauses.get(clauseIndexes.get(i).get(j))));
			}
			MLN dMLN = new MLN();
			dMLN.clauses.addAll(tmpClauses);
			dMLN.setMLNPoperties();
			disjointCNFList.add(dMLN);
		}
		
		// Make single MLN for empty clauses
		for(Integer emptyClauseIndex : emptyClausesIndices)
		{
			ArrayList<WClause> tmpClauses = new ArrayList<WClause>();
			MLN dMLN = new MLN();
			WClause nClause= new WClause();
			nClause.hcNumCopies.addAll(new ArrayList<Integer>(mln.clauses.get(emptyClauseIndex).hcNumCopies));
			nClause.satHyperCubes.addAll(new ArrayList<Boolean>(mln.clauses.get(emptyClauseIndex).satHyperCubes));
			nClause.weight = new LogDouble(mln.clauses.get(emptyClauseIndex).weight);
			//nClause.satisfied = lvsat.get(i);
			//nClause.weight = satWts.get(i);
			tmpClauses.add(nClause);
			dMLN.clauses.addAll(tmpClauses);
			dMLN.setMLNPoperties();
			disjointCNFList.add(dMLN);
			
		}
	}

	public static void main(String[] args) throws FileNotFoundException, PredicateNotFound {
		LiftedPTPParams params = new LiftedPTPParams();
		try{
			LiftedPTP.parseParams(args, params);
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println(params);
		MLN mln = new MLN();
		Parser parser = new Parser(mln);
		parser.parseInputMLNFile(params.inputFile);
		String queries[] = params.queryString.split(",");
		ArrayList<String> query_preds = new ArrayList<String>(Arrays.asList(queries));
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
		
		// manual creation of hyperCubes for testing
		
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.clear();
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(0)));
		mln.clauses.get(0).satHyperCubes.add(false);
		mln.clauses.get(0).hcNumCopies.add(1);
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(1)));
		mln.clauses.get(0).satHyperCubes.add(false);
		mln.clauses.get(0).hcNumCopies.add(1);
		mln.clauses.get(0).atoms.get(0).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(0)));
		mln.clauses.get(0).satHyperCubes.add(false);
		mln.clauses.get(0).hcNumCopies.add(1);
		
		mln.clauses.get(0).atoms.get(1).terms.get(0).domain.clear();
		mln.clauses.get(0).atoms.get(1).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(3)));
		//mln.clauses.get(0).satHyperCubes.add(false);
		//mln.clauses.get(0).hcNumCopies.add(1);
		mln.clauses.get(0).atoms.get(1).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(3)));
		//mln.clauses.get(1).satHyperCubes.add(false);
		//mln.clauses.get(1).hcNumCopies.add(1);
		mln.clauses.get(0).atoms.get(1).terms.get(0).domain.add(new TreeSet<Integer>(Arrays.asList(5)));
		//mln.clauses.get(1).satHyperCubes.add(false);
		//mln.clauses.get(1).hcNumCopies.add(1);
		ArrayList<MLN> dCNFs = new ArrayList<MLN>();
		seperateDisjointSegmentsCNF(mln, dCNFs);
		System.out.println("disjointing done...");
	}

}
