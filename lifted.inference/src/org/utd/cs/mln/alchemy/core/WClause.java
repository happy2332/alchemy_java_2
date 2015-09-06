package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.utd.cs.gm.core.LogDouble;
import org.utd.cs.gm.utility.Pair;

public class WClause {

	public List<Atom> atoms = new ArrayList<Atom>();
	//public List<Term> terms = new ArrayList<Term>();
	// Whenever terms' num of hyperCubes change, update below two data structures appropriately 
	public List<Integer> hcNumCopies = new ArrayList<Integer>(); // stores numCopies for each hyperCube of clause
	public List<Boolean> satHyperCubes = new ArrayList<Boolean>();
	//public List<HyperCube> hyperCubes = new ArrayList<HyperCube>();
	//public ArrayList<ArrayList<Integer>> tuples = new ArrayList<ArrayList<Integer>>();
	public List<Boolean> sign = new ArrayList<Boolean>(); // sign = true : negative, false : positive
	public LogDouble weight;
	public boolean satisfied;
	
	public WClause() {
		satisfied = (false);
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}

	public boolean valid(){
		for(int i=0;i<atoms.size();i++){
			for(int j=0;j<atoms.get(i).terms.size();j++){
							if(atoms.get(i).terms.get(j).domain.isEmpty()) return false;
			}
		}
		
		return true;
	}
	
	public void print() {

		if(satisfied)
			System.out.print("Satisfied V ");
		if(atoms.size()==0)
			System.out.print("{ }");
		for(int i=0;i<atoms.size();i++)
		{
			if(sign.get(i))
				System.out.print("!");
			atoms.get(i).print();
			if(i!=atoms.size()-1)
				System.out.print(" V ");
		}
		System.out.println();
	}

	public void removeAtom(int index) {

		//terms are shared in the same clause, check if the atom has shared terms
		for(int i=0;i<atoms.get(index).terms.size();i++)
		{
			boolean shared = false;
			for(int j=0;j<atoms.size();j++)
			{
				if(j==index)
					continue;
				for(int k=0;k<atoms.get(j).terms.size();k++)
				{
					if(atoms.get(index).terms.get(i) == atoms.get(j).terms.get(k))
					{
						shared = true;
						break;
					}
				}
				if(shared)
					break;
			}
			if(!shared)
			{
				atoms.get(index).terms.remove(i);
				i--;
			}
		}
		atoms.remove(index);
		sign.remove(index);
	}
	
	public void findSelfJoins(Map<Integer, ArrayList<Integer>> selfJoinedAtoms) {
		for(int i=0;i<atoms.size();i++)
		{
			int id = atoms.get(i).symbol.id;
			if(!selfJoinedAtoms.containsKey(id))
			{
				ArrayList<Integer> tempPos = new ArrayList<Integer>();
				tempPos.add(i);
				for(int j=i+1;j<atoms.size();j++)
				{
					if(atoms.get(i).symbol.id == atoms.get(j).symbol.id)
					{
						//self joined
						tempPos.add(j);
					}
				}
				if(tempPos.size()>1)
				{
					//self joined
					selfJoinedAtoms.put(id, tempPos);
				}
			}
		}
		
	}
	
	public boolean isSelfJoinedOnAtom(Atom atom) {
		int count=0;
		for(int i=0;i<atoms.size();i++)
		{
			if(atoms.get(i).symbol.id==atom.symbol.id)
				count++;
		}
		if(count > 1)
			return true;
		else
			return false;
	}
	
	public boolean isPropositionalActiveTerms(){
		for(int i=0;i<atoms.size();i++)
		{
			if(atoms.get(i).activeTermsIndices.size() > 0)
				return false;
		}
		return true;
	}
	
	public boolean isPropositional() {

		for(int i=0;i<atoms.size();i++)
		{
			if(!atoms.get(i).isConstant())
				return false;
		}
		return true;
		
	}
	
	public int getNumberOfGroundings() {

		int numberOfGroundings=1;
		Set<Term> terms = new HashSet<Term>();
		
		for(int i=0;i<atoms.size();i++)
		{
			for (int j = 0; j < atoms.get(i).terms.size(); j++)
			{
				terms.add(atoms.get(i).terms.get(j));
			}
			
		}
		int numHyperCubes = terms.iterator().next().domain.size();
		for(int i = 0 ; i < numHyperCubes ; i++){
			int numTuples = 1;
			for (Term term : terms) {
				numTuples *= term.domain.get(i).size();
			}
			numberOfGroundings += numTuples;
		}
		return numberOfGroundings;
	}

	// Added By Happy
	public int findEquivClass(ArrayList<Set<Pair>> equi_class,int cum_index,int cur_index, ArrayList<Integer> varIndexToClauseIndex, int  clause_index, List<List<Integer>> finalTermsToGround, ArrayList<Integer> varIndexToDomainSize){
		ArrayList<Term> termList = new ArrayList<Term>();
		/*
		for(Atom atom : atoms)
		{
			System.out.println("terms size : "+atom.terms.size());
			for(Term term : atom.terms)
			{
				System.out.print("domain size : "+term.domain.size()+", ");
			}
			System.out.println();
		}*/
		for(int i = 0 ; i < atoms.size() ; i++){
			for(int j = 0 ; j < atoms.get(i).terms.size() ; j++){
				Set<Pair> single_equi_class = new HashSet<Pair>();
				Term term = atoms.get(i).terms.get(j);
				int term_id = termList.indexOf(term);
				System.out.println("termList = "+termList);
				System.out.println("term = "+term);
				//System.out.println("term id = "+term_id);
				//System.out.println("cur_index = "+cur_index);
				if(finalTermsToGround.get(atoms.get(i).symbol.id).contains(j))
				{
					//System.out.println("empty equi class of " + atoms.get(i).symbol.symbol + " and pos = " + j);
					if(term_id == -1)
					{
						cur_index++;
						termList.add(term);
						equi_class.add(single_equi_class);
						varIndexToClauseIndex.add(clause_index);
						varIndexToDomainSize.add(term.domain.size());
					}
					continue;
				}
				if(term_id == -1) // term not present
				{
					term_id = termList.size();
					termList.add(term);
				}
				Pair equiv_pair = new Pair(atoms.get(i).symbol.id,j);
				if(term_id + cum_index < cur_index)
				{
					equi_class.get(term_id + cum_index).add(equiv_pair);
				}
				else
				{
					single_equi_class.add(equiv_pair);
					equi_class.add(single_equi_class);
					varIndexToClauseIndex.add(clause_index);
					varIndexToDomainSize.add(term.domain.size());
					//varIndexToDomainSize.add(atoms.get(i).terms.get(j).domain.size());
					cur_index++;
				}
				//System.out.println("equi_class : "+equi_class);
			}
		}
		return cur_index;
	}
	
	public int findEquivClass(ArrayList<Set<Pair>> equi_class,int cum_index,int cur_index){
		ArrayList<Term> termList = new ArrayList<Term>();
		/*
		for(Atom atom : atoms)
		{
			System.out.println("terms size : "+atom.terms.size());
			for(Term term : atom.terms)
			{
				System.out.print("domain size : "+term.domain.size()+", ");
			}
			System.out.println();
		}*/
		for(int i = 0 ; i < atoms.size() ; i++){
			for(int j = 0 ; j < atoms.get(i).terms.size() ; j++){
				Term term = atoms.get(i).terms.get(j);
				int term_id = termList.indexOf(term);
				//System.out.println("term id = "+term_id);
				//System.out.println("cur_index = "+cur_index);
				if(term_id == -1)
				{
					cur_index++;
					termList.add(term);
					Set<Pair> termEquivalenceClass = new HashSet<Pair>();
					termEquivalenceClass.add(new Pair(atoms.get(i).symbol.id,j));
					equi_class.add(termEquivalenceClass);
				}
				else{
					equi_class.get(term_id+cum_index).add(new Pair(atoms.get(i).symbol.id,j));
				}
				///System.out.println("equi_class becomes : ");
				///System.out.println(equi_class);
			}
		}
		return cur_index;
	}
	
	public boolean isSatisfied(){
		for(Boolean hcSat : satHyperCubes){
			if(hcSat == false){
				return false;
			}
		}
		return true;
	}

	public void find_decomposer(ArrayList<Term> terms,
			ArrayList<ArrayList<Integer>> positions) {
		
		Map<Integer,ArrayList<Integer>> selfJoinedAtoms = new HashMap<Integer,ArrayList<Integer>>();
		findSelfJoins(selfJoinedAtoms);
		Set<Term> clause_terms_set = new HashSet<Term>();

		for(int i=0;i<atoms.size();i++){
			Atom atom = atoms.get(i);
			//if any constant in clause, can never have a decomposer
			/* Happy comment
			if(atom.isConstant() || atom.activeTermsIndices.size() == 0)
				return;
			*/
			for(int j=0;j<atom.terms.size();j++){
				clause_terms_set.add(atom.terms.get(j));
			}
		}
		
		ArrayList<ArrayList<Integer>> clause_positions = new ArrayList<ArrayList<Integer>>();
		for(int i = 0 ; i < clause_terms_set.size() ; i++){
			clause_positions.add(new ArrayList<Integer>());
		}
		ArrayList<Term> clause_terms = new ArrayList<Term>(clause_terms_set);
		for(int i=0;i<atoms.size();i++){
			Atom atom = atoms.get(i);
			/* Happy Comment
			if(atom.isConstant() || atom.activeTermsIndices.size() == 0)
				continue;
			*/
			for(int j=0;j<atom.terms.size();j++){
				int k=clause_terms.indexOf(atom.terms.get(j));
				clause_positions.get(k).add(j);
			}
		}
		int k=0;
		for(Term i : clause_terms){
			//check if term is a variable and not a constant
			//if(!i.isConstant()) Happy Comment
			{
				// Check if the term participates in all atoms and unifies with self joined atoms. If it does, it is decomposer
				if (clause_positions.get(k).size()==atoms.size()){
					//check for self joins
					boolean unified = true;
					for(Integer it : selfJoinedAtoms.keySet())
					{
						//check for unification with all self joined atoms
						
						int pos = clause_positions.get(k).get(selfJoinedAtoms.get(it).get(0));
						for(int m = 1 ; m < selfJoinedAtoms.get(it).size() ; m++)
						{
							if(clause_positions.get(k).get(m)!=pos)
							{
								unified = false;
								break;
							}
						}
						if(!unified)
							break;
					}
					if(unified)
					{
						terms.add(i);
						positions.add(clause_positions.get(k));
					}
				}
			}
			++k;
		}
	}

}
