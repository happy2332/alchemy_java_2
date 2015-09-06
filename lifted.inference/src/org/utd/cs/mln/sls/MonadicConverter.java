package org.utd.cs.mln.sls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sat4j.specs.ContradictionException;
import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.PredicateSymbol;
import org.utd.cs.mln.alchemy.core.Term;
import org.utd.cs.mln.alchemy.core.WClause;

public class MonadicConverter {
	
	private MaxSatEncoder encoder;
	
	private Map<String, PredicateSymbol> symbolTable = new HashMap<String, PredicateSymbol>();
	private int predicateId = 0;
	
	private MLN monadicMln;
	private MLN mln;
	
	public MonadicConverter(MLN _mln) {
		mln = _mln;
		monadicMln = new MLN();
		encoder = new MaxSatEncoder(mln);
	}
	
	private void partiallyGround(WClause clause, List<Term> terms) {
		
		List<WClause> groundClauses = new ArrayList<WClause>();
		
		int[][] permutations = permute(terms);
		for (int i = 0; i < permutations.length; i++) {
			//Create a clause for each permutation
			WClause newClause = create_new_clause(clause);
			for (int j = 0; j < clause.atoms.size(); j++) {
				Atom atom = clause.atoms.get(j);
				Atom newAtom = newClause.atoms.get(j);
				
				String symbolTableKey = atom.symbol.symbol;
				for (int k = 0; k < atom.terms.size(); k++) {
					Term term = atom.terms.get(k);
					Term newTerm = newAtom.terms.get(k);
					
					//Find the term in the term list, and replace the newClause's
					//term domain by permutation.
					int termIndex = terms.indexOf(term);
					if(termIndex == -1) {
						//The term is not grounded
						symbolTableKey += "_-1";
						
						// Populate domain according to old term
						for(int l=0; l < term.domain.size(); l++)
							newTerm.domain.add(term.domain.get(l));
						continue;
					}
					
					newTerm.domain.clear();
					newTerm.domain.add(permutations[i][termIndex]);
					symbolTableKey += "_" + permutations[i][termIndex];
				}
				//Look up symbol table for symbol
				if(symbolTable.containsKey(symbolTableKey)) {
					newAtom.symbol = symbolTable.get(symbolTableKey);
				} else {
					PredicateSymbol symbol = create_new_symbol(newAtom.symbol);
					symbolTable.put(symbolTableKey, symbol);
					newAtom.symbol = symbol;
					
					String[] strings = symbolTableKey.split("_");
					symbol.printString = strings[0] + "(";
					for (int i1 = 1; i1 < strings.length - 1; i1++) {
						if(strings[i1].equals("-1"))
							symbol.printString += "_, ";
						else 
							symbol.printString += strings[i1] + ", ";
					}
					if(strings[strings.length - 1].equals("-1"))
						symbol.printString += "_) ";
					else 
						symbol.printString += strings[strings.length - 1] + ") ";
				}
				
			}
			groundClauses.add(newClause);
		}
		
		monadicMln.clauses.addAll(groundClauses);
	}
	
	/**
	 * Create a copy of the predicate symbol <code>symbol</code>. The new symbol
	 * will NOT be dependent on the previous symbol, and will have a separate id.
	 * 
	 * @param symbol
	 * @return
	 */
	private PredicateSymbol create_new_symbol(PredicateSymbol symbol) {
		List<Integer> var_types = new ArrayList<Integer>();
		for(int i=0;i<symbol.variable_types.size();i++)
			var_types.add(symbol.variable_types.get(i));
		PredicateSymbol newSymbol = new PredicateSymbol(predicateId,symbol.symbol,var_types,symbol.pweight,symbol.nweight);
		
		predicateId++;
		monadicMln.symbols.add(newSymbol);
		
		return newSymbol;
	}

	private WClause create_new_clause(WClause clause) {

		WClause new_clause = new WClause();
		new_clause.sign = new ArrayList<Boolean>(clause.sign);
		new_clause.satisfied = clause.satisfied;
		new_clause.weight = clause.weight;

		//if atoms have common terms their relationship must be maintained when new clause is created
		List<Term> newTerms = new ArrayList<Term>();
		List<Term> oldTerms = new ArrayList<Term>();

		for (int i = 0; i < clause.atoms.size(); i++) 
		{
			for (int j = 0; j < clause.atoms.get(i).terms.size(); j++)
			{
				int termPosition=-1;
				for(int m=0;m<oldTerms.size();m++)
				{
					if(oldTerms.get(m)==clause.atoms.get(i).terms.get(j))
					{
						termPosition = m;
					}
				}
				if(termPosition==-1)
				{
					Term term = new Term();
					term.type = clause.atoms.get(i).terms.get(j).type;
					// Do NOT populate term domain
					newTerms.add(term);
					oldTerms.add(clause.atoms.get(i).terms.get(j));
				}
				else
				{
					newTerms.add(newTerms.get(termPosition));
					oldTerms.add(clause.atoms.get(i).terms.get(j));
				}
			}
		}
		int ind=0;
		new_clause.atoms = new ArrayList<Atom>(clause.atoms.size());
		for (int i = 0; i < clause.atoms.size(); i++) {
			new_clause.atoms.add(new Atom());
			new_clause.atoms.get(i).symbol = MLN.create_new_symbol(clause.atoms.get(i).symbol);
			new_clause.atoms.get(i).terms = new ArrayList<Term>(clause.atoms.get(i).terms.size());
			for (int j = 0; j < clause.atoms.get(i).terms.size(); j++) {
				new_clause.atoms.get(i).terms.add(newTerms.get(ind));
				ind++;
			}
		}
		return new_clause;
	}
	
	/**
	 * Create all possible permutation of a the domains of the terms
	 * @param terms
	 * @return 
	 */
	private int[][] permute(List<Term> terms) {
		
		int permutaionSize = 1;
		for (Term term : terms) {
			permutaionSize *= term.domain.size();
		}
		
		int[][] permuations = new int[permutaionSize][terms.size()];
		
		for (int i = 0; i < permuations.length; i++) {
			int residue = i;
			for (int j = 0; j < terms.size(); j++) {
				int index = residue % terms.get(j).domain.size();
				residue = residue / terms.get(j).domain.size();
				permuations[i][j] = terms.get(j).domain.get(index);
			}
		}
		
		return permuations;
		
	}
	
	public MLN convert() throws ContradictionException {
		long time = System.currentTimeMillis();
		encoder.encode();
		encoder.solve();
		System.out.println("Time taken by Sat4j = " + (System.currentTimeMillis() - time) + " ms");
		
		List<List<Integer>> model = encoder.model();
		
		for (WClause clause : mln.clauses) {
			Set<Term> termsToGround = new HashSet<Term>();
			for (Atom atom : clause.atoms) {
				for (int i = 0; i < atom.terms.size(); i++) {
					if(model.get(atom.symbol.id).get(i) > 0) {
						// The corresponding SAT variable is positive
						// Hence we need to ground this term
						termsToGround.add(atom.terms.get(i));
					}
				}
			}
			this.partiallyGround(clause, new ArrayList<Term>(termsToGround));
		}
		
		return monadicMln;
	}

}
