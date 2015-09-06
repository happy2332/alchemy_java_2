package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Decomposer {
	public ArrayList<Term> decomposer_terms = new ArrayList<Term>();
	//key is predicatesymbol id, value is position the term occupies
	public Map<Integer,Integer> predicate_positions = new HashMap<Integer,Integer>();
	Map<Integer,Integer> norm_predicate_positions = new HashMap<Integer,Integer>();
	//key is predicatesymbol id, value is number of predicates sharing a common position
	Map<Integer,Integer> atom_counter = new HashMap<Integer,Integer>();
	boolean deletionMarker;
	
	public Decomposer(){
		
	}
	
	public Decomposer(ArrayList<Term> decomposer_terms_, Map<Integer,Integer> predicate_positions_,
			Map<Integer,Integer> atomCounter, Map<Integer,Integer> norm_predicate_positions_){
		decomposer_terms = new ArrayList<Term>(decomposer_terms_);
		predicate_positions = new HashMap<Integer,Integer>(predicate_positions_);
		atom_counter = new HashMap<Integer,Integer>(atomCounter);
		deletionMarker = false;
		norm_predicate_positions = new HashMap<Integer,Integer>(norm_predicate_positions_);
	}
	
	public void print()
	{
		System.out.print("[");
		for(int j=0;j<decomposer_terms.size();j++)
		{
			System.out.print(decomposer_terms.get(j));
			if(j!=decomposer_terms.size()-1)
				System.out.print(" , ");
		}
		System.out.println("]");
	}

}
