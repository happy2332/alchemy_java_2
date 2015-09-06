package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LDecomposer {
	//public MLN mln;
	/*
	public LDecomposer(MLN mln_){
		mln = mln_;
	}*/
	
	public boolean decomposeCNF(MLN CNF,Integer powerFactor){
		ArrayList<Decomposer> decomposer_list = new ArrayList<Decomposer>();
		find_decomposer(CNF,decomposer_list);
		return true;
	}
	
	
	public void find_decomposer(MLN CNF, ArrayList<Decomposer> decomposer_list) {
		
		Set<Integer> nonUnifiedList = new HashSet<Integer>();

		//vector<int> predicateCounter(mln.getMaxPredicateId());
		Map<Integer,Integer> predicateCounter = new HashMap<Integer,Integer>();
		for(int i=0;i<CNF.clauses.size();i++)
		{
			WClause clause = CNF.clauses.get(i);
			if(clause.atoms.size()==0)
				continue;
			if(clause.isPropositionalActiveTerms())
				continue;
			ArrayList<Term> terms = new ArrayList<Term>();
			ArrayList<ArrayList<Integer>> positions = new ArrayList<ArrayList<Integer>>();
			ArrayList<Integer> atom_list = new ArrayList<Integer>();
			ArrayList<Integer> norm_atom_list = new ArrayList<Integer>();
			//vector<bool> completedAtoms(mln.getMaxPredicateId());
			Set<Integer> completedAtoms = new HashSet<Integer>();
			for(int k=0;k<clause.atoms.size();k++)
			{
				Atom atom = clause.atoms.get(k);
				int id = atom.symbol.id;
				atom_list.add(id);
				norm_atom_list.add(atom.symbol.parentId);
				if(!completedAtoms.contains(id))
				{
					completedAtoms.add(id);
					if(predicateCounter.containsKey(id)){
						predicateCounter.put((Integer)id, (Integer)(predicateCounter.get(id)+1));
					}
					else{
						predicateCounter.put((Integer)id, 1);
					}
					
				}

			}
			//check if any atom occurs in failed list
			boolean nonunified = false;
			for(int k=0;k<atom_list.size();k++)
			{
				if(nonUnifiedList.contains(atom_list.get(k)))
				{
					nonunified = true;
					break;
				}
			}
			if(!nonunified)
				clause.find_decomposer(terms,positions);
			
			if(terms.size()==0)
			{
				removeRowsFromDecomposer(clause,decomposer_list);
				//add all atoms to non unified list
				for(int k = 0 ; k < atom_list.size() ; k++)
					nonUnifiedList.add(atom_list.get(k));
				continue;
			}
			boolean unified = append_to_decomposer(atom_list,terms,positions,decomposer_list,norm_atom_list);
			if(!unified)
			{
				removeRowsFromDecomposer(CNF.clauses.get(i),decomposer_list);
				//add all atoms to non unified list
				for(int k=0;k<atom_list.size();k++)
					nonUnifiedList.add(atom_list.get(k));
			}
			
			// Print
			/*
			for(int x=0;x<decomposer_list.size();x++)
			{
				System.out.print("[");
				for(int y=0;y<decomposer_list.get(x).decomposer_terms.size();y++)
					System.out.print(decomposer_list.get(x).decomposer_terms.get(y)+" ");
				System.out.println("]");
				System.out.print("{");
				for(Integer it1 : decomposer_list.get(x).predicate_positions.keySet())
				{
					System.out.print(it1+"::"+decomposer_list.get(x).predicate_positions.get(it1)+",");
				}
				System.out.println("}");
				System.out.print("(");
				for(Integer it1 : decomposer_list.get(x).atom_counter.keySet())
				{
					System.out.print(it1+"::"+decomposer_list.get(x).atom_counter.get(it1)+",");
				}
				System.out.println(")");
			}
			System.out.println("--------------------------------");
			*/
		}
		
		for(int x=0;x<decomposer_list.size();x++)
		{
			if(decomposer_list.size()==0)
				break;
			boolean isDecomposer = true;
			for(Integer it1 : decomposer_list.get(x).atom_counter.keySet()){
				int predicateCount = predicateCounter.get(it1);
				int atomCount = decomposer_list.get(x).atom_counter.get(it1);
				if(predicateCount != atomCount)
				{
					isDecomposer = false;
					break;
				}
			}
			if(!isDecomposer)
			{
				decomposer_list.remove(x);
				x--;
			}
		}
	}
	
	private boolean append_to_decomposer(ArrayList<Integer> atom_list,
			ArrayList<Term> terms, ArrayList<ArrayList<Integer>> positions,
			ArrayList<Decomposer> decomposer_list,
			ArrayList<Integer> norm_atom_list) {
	
		boolean unified = true;
		ArrayList<Decomposer> potentialDecomposers = new ArrayList<Decomposer>();
		//Make new decomposer and Append to potential decomposers list
		for(int t1=0;t1<positions.size();t1++)
		{
			ArrayList<Integer> position_list = new ArrayList<Integer>(positions.get(t1));
			Term term = terms.get(t1);
			potentialDecomposers.add(create_new_decomposer(term,atom_list,position_list,norm_atom_list));
		}
		if(decomposer_list.size()==0)//decomposer is empty
		{
			//return the potential decomposer list
			decomposer_list.clear();
			decomposer_list.addAll(potentialDecomposers);
			return unified;
		}
		//match every potential decomposer with existing list of decomposers
		ArrayList<Decomposer> decomposer_add = new ArrayList<Decomposer>();
		int nonUnifiedTermCount = 0;
		for(int i = 0 ; i < potentialDecomposers.size() ; i++)
		{
			int notFoundCount=0;
			ArrayList<Integer> unifiedIndex = new ArrayList<Integer>();
			int numNonUnify = 0;
			for(int j=0 ; j < decomposer_list.size() ; j++)
			{
				//Unify new potential decomposer with existing decomposers
				int status = unify(potentialDecomposers.get(i),decomposer_list.get(j));
				if(status==0)
				{
					numNonUnify++;
					decomposer_list.get(j).deletionMarker = true;
				}
				else if(status == 1)
				{
					//merge the decomposers into 1
					unifiedIndex.add(j);
				}
				else
					notFoundCount++;
			}
			if(numNonUnify == decomposer_list.size())
			{
				//did not unify with anything
				nonUnifiedTermCount++;
			}
			if(notFoundCount == decomposer_list.size())
			{
				//disjoint with all existing sets,append
				decomposer_add.add(potentialDecomposers.get(i));
			}
			else if(unifiedIndex.size() > 0)
			{
				//merge all unified rows into 1
				Decomposer tempD = potentialDecomposers.get(i);
				for(int k = 0 ; k < unifiedIndex.size() ; k++)
				{
					Decomposer newD = merge(tempD,decomposer_list.get(unifiedIndex.get(k)));
					tempD = newD;
				}

				decomposer_add.add(tempD);
			}
		}
		
		if(nonUnifiedTermCount == potentialDecomposers.size())
		{
			//no terms unified with any decomposer row
			unified = false;
		}
		//add the new decomposer rows
		for(int i=0;i<decomposer_add.size();i++)
		{
			decomposer_list.add(decomposer_add.get(i));
		}

		for(int i=0;i<decomposer_list.size();i++)
		{
			if(decomposer_list.size() == 0)
				break;
			if(decomposer_list.get(i).deletionMarker)
			{
				decomposer_list.remove(i);
				i--;
			}
		}
		return unified;
	}
	
	private Decomposer merge(Decomposer d1, Decomposer d2) {
		
		ArrayList<Term> decomposerTerms = new ArrayList<Term>();
		Map<Integer,Integer> predicate_positions = new HashMap<Integer,Integer>();
		Map<Integer,Integer> norm_predicate_positions = new HashMap<Integer,Integer>();
		//key is predicatesymbol id, value is number of predicates sharing a common position
		Map<Integer,Integer> atomCounter = new HashMap<Integer,Integer>();
		for(int i=0;i<d1.decomposer_terms.size();i++)
		{
			decomposerTerms.add(d1.decomposer_terms.get(i));
		}
		for(int i=0;i<d2.decomposer_terms.size();i++)
		{
			decomposerTerms.add(d2.decomposer_terms.get(i));
		}
		for(Integer it1 : d1.predicate_positions.keySet())
		{
			predicate_positions.put(it1,d1.predicate_positions.get(it1));
		}
		for(Integer it1 : d2.predicate_positions.keySet())
		{
			predicate_positions.put(it1,d2.predicate_positions.get(it1));
		}
		
		for(Integer it1 : d1.atom_counter.keySet())
		{
			atomCounter.put(it1,d1.atom_counter.get(it1));
		}
		for(Integer it1 : d2.atom_counter.keySet())
		{
			if(atomCounter.containsKey(it1))
			{
				atomCounter.put(it1, atomCounter.get(it1)+d2.atom_counter.get(it1));
			}
			else
				atomCounter.put(it1,d2.atom_counter.get(it1));
		}
		for(Integer it1 : d1.norm_predicate_positions.keySet())
		{
			norm_predicate_positions.put(it1,d1.norm_predicate_positions.get(it1));
		}
		for(Integer it1 : d2.norm_predicate_positions.keySet())
		{
			norm_predicate_positions.put(it1,d2.norm_predicate_positions.get(it1));
		}

		d1.deletionMarker = true;
		d2.deletionMarker = true;
		Decomposer newDecomp = new Decomposer(decomposerTerms,predicate_positions,atomCounter,norm_predicate_positions);
		return newDecomp;
	}
	
	private int unify(Decomposer d1, Decomposer d2) {

		boolean found=false;
		for(Integer it1 : d1.predicate_positions.keySet())
		{	
			if(d2.predicate_positions.containsKey(it1))
			{
				found=true;
				//found d1's predicate in d2, match the positions
				int d2_pred_pos = d2.predicate_positions.get(it1);
				int d1_pred_pos = d1.predicate_positions.get(it1);
				if(d2_pred_pos != d1_pred_pos)
					return 0;
			}
		}
		if(found)
			return 1;
		else
			return -1;
	}
	
	private Decomposer create_new_decomposer(Term term,
			ArrayList<Integer> atom_list, ArrayList<Integer> position_list,
			ArrayList<Integer> norm_atom_list) {

		ArrayList<Term> decomposer_terms = new ArrayList<Term>();
		Map<Integer,Integer> predicate_positions = new HashMap<Integer,Integer>();
		Map<Integer,Integer> norm_predicate_positions = new HashMap<Integer,Integer>();
		//key is predicatesymbol id, value is number of predicates sharing a common position
		Map<Integer,Integer> atomCounter = new HashMap<Integer,Integer>();
		decomposer_terms.add(term);
		for(int k = 0 ; k < atom_list.size() ; k++)
		{
			predicate_positions.put(atom_list.get(k),position_list.get(k));
			norm_predicate_positions.put(norm_atom_list.get(k),position_list.get(k));
			atomCounter.put(atom_list.get(k),1);
		}
		Decomposer d = new Decomposer(decomposer_terms,predicate_positions,atomCounter,norm_predicate_positions);
		return d;
	}
	
	private void removeRowsFromDecomposer(WClause clause,
			ArrayList<Decomposer> decomposer_list) {
		
		//remove from the potential decomposers list all predicates intersecting with clause i's predicates
		ArrayList<Integer> rowsToDelete = new ArrayList<Integer>();
		for(int j = 0 ; j < clause.atoms.size() ; j++)
		{
			for(int k = 0 ; k < decomposer_list.size() ; k++)
			{
				if(decomposer_list.get(k).predicate_positions.containsKey(clause.atoms.get(j).symbol.id))
				{
					//remove entire decomposer row
					rowsToDelete.add(k);
				}
			}
		}
		int removedRows=0;
		for(int k = 0 ; k < rowsToDelete.size() ; k++)
		{
			if(decomposer_list.isEmpty())
				break;
			int updatedRow = rowsToDelete.get(k)-removedRows-1;
			if(updatedRow < 0)
				updatedRow = 0;
			if(updatedRow > decomposer_list.size()-1)
				updatedRow = decomposer_list.size()-1;
			decomposer_list.remove(updatedRow);
			removedRows++;
		}

		
	}
}
