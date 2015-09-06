package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.utd.cs.gm.utility.Pair;
import org.utd.cs.mln.lmap.LiftedSplit;

public class Atom {
	public PredicateSymbol symbol;
	// terms may be shared across atoms
	public List<Term> terms;
	public ArrayList<Integer> activeTermsIndices = new ArrayList<Integer>();
	public ArrayList<HyperCube> hyperCubes = new ArrayList<HyperCube>();

	// Conditions: terms.size()=symbol.variables.size() and
	// terms[i].type=symbol.variables[i]
	public Atom() {
		terms = new ArrayList<Term>();
	}

	public Atom(PredicateSymbol symbol_, List<Term> terms_) {
		symbol = (symbol_);
		terms = (terms_);
		activeTermsIndices.clear();
		for(int i = 0 ; i < terms.size() ; i++){
			activeTermsIndices.add(i);
		}
	}

	public Atom(Atom atom) {
		symbol = atom.symbol;
		terms = new ArrayList<Term>();
		for (int i = 0; i < atom.terms.size(); i++) {
			Term tm = new Term(atom.terms.get(i));
			terms.add(tm);
		}
		activeTermsIndices = new ArrayList<Integer>(atom.activeTermsIndices);
	}

	public boolean isConstant() {
		boolean constantDomain = true;
		for(int i=0;i<terms.size();i++)
		{
			if(!terms.get(i).isConstant())
			{
				constantDomain = false;
				break;
			}
		}
		return constantDomain;

	}

	public int isSingletonAtom() {

		if(terms.size() == 1)
		{
			return 0;
		}
		int variableIndex = -1;
		int numVariables = 0;
		for(int i=0;i<terms.size();i++)
		{
			if(!terms.get(i).isConstant())
			{
				//variable
				numVariables++;
				if(numVariables > 1)
					break;
				//store index of the first variable
				if(variableIndex == -1)
					variableIndex = i;
			}
		}
		//singleton if no variables or 1 variable
		if(numVariables == 1)
			return variableIndex;
		else
			return -1;

	}
	
	// Pair's firstvalue tells if singleton or not. second value tell position of variable which is varying
	public Pair isSingletonPartial(){
		if(terms.size() == 1)
		{
			new Pair(0,0);
		}
		int variableIndex = -1;
		int numVariables = 0;
		for(int i=0;i<terms.size();i++)
		{
			if(!terms.get(i).isConstant())
			{
				//variable
				numVariables++;
				if(numVariables > 1)
					break;
				//store index of the first variable
				if(variableIndex == -1)
					variableIndex = i;
			}
		}
		//singleton if no variables or 1 variable
		if(numVariables == 1)
			return new Pair(0,variableIndex);
		else
			return new Pair(-1, variableIndex);

	}

	
	public int getNumberOfGroundings() {

		int numHyperCubes = terms.get(0).domain.size();
		Set<ArrayList<Integer>> predGroundings = new HashSet<ArrayList<Integer>>();
		for(int hcId = 0 ; hcId < numHyperCubes ; hcId++){
			ArrayList<TreeSet<Integer>> varConstants = new ArrayList<TreeSet<Integer>>();
			for(Term t : terms){
				varConstants.add(new TreeSet<Integer>(t.domain.get(hcId)));
			}
			ArrayList<ArrayList<TreeSet<Integer>>> hyperCubeTuples = LiftedSplit.cartesianProd(varConstants);
				
			for(ArrayList<TreeSet<Integer>> hyperCubeTuple : hyperCubeTuples){
				ArrayList<Integer> predGrounding = new ArrayList<Integer>();
				for(int i = 0 ; i < hyperCubeTuple.size() ; i++){
					predGrounding.add(hyperCubeTuple.get(i).iterator().next());
				}
				predGroundings.add(predGrounding);
			}
		}
		return predGroundings.size();
	}
	
	
	public void print() {

		System.out.print(symbol.symbol + "[ID::" +  symbol.id + "wt::" + symbol.pweight + "," + symbol.nweight + "]" + " ( ");
		for(int j=0;j<terms.size();j++){
			if((int)terms.get(j).domain.size()==1){
				System.out.print(terms.get(j).domain.get(0));
			}
			else{
				System.out.print(terms.get(j) + "[#" + terms.get(j).domain.size() + "]");
			}
			if(j!=terms.size()-1)
				System.out.print(", ");
		}
		System.out.print(")");

	}

	/*
	public int hasSingletonSegment(WClause clause) {
		ArrayList<Integer> clauseTermIds = new ArrayList<Integer>();
		for(Term term : terms){
			clauseTermIds.add(clause.terms.indexOf(term));
		}
		int predPosition = -1;
		for(HyperCube hyperCube : clause.hyperCubes){
			int nonSingletonSegmentTermId = -1;
			int countNonSingletonSegments = 0;
			for(Integer termId : clauseTermIds){
				if(hyperCube.varConstants.get(termId).size() > 1){
					nonSingletonSegmentTermId = termId;
					countNonSingletonSegments++;
				}
			}
			if(countNonSingletonSegments <= 1){
				predPosition = terms.indexOf(clause.terms.get(nonSingletonSegmentTermId));
				return predPosition;
			}
		}
		return predPosition;	
	}*/
	
} // Class ends here
