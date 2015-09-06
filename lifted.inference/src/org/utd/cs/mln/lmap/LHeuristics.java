package org.utd.cs.mln.lmap;

import java.util.HashMap;
import java.util.Map;

import org.utd.cs.mln.alchemy.core.Atom;
import org.utd.cs.mln.alchemy.core.LDecomposer;
import org.utd.cs.mln.alchemy.core.MLN;
import org.utd.cs.mln.alchemy.core.WClause;

public class LHeuristics {

	//public LDecomposer decomposer;
	//public MLN mln;
	/*
	public LHeuristics(LDecomposer decomposer_, MLN mln_) {
		decomposer = decomposer_;
		mln = mln_;
	}*/
	
	public static Atom getAtomToSplitActiveTerms(MLN CNF){
		int maxDegreeSingleton = 0;
		int maxDegreeNST = 0;
		int maxDegreeConstant = 0;
		int minDomainSingleton = Integer.MAX_VALUE;
		Atom atomMaxDegree_S = null;
		Atom atomMaxDegree_NS = null;
		Atom atomMaxDegree_Const = null;
		Atom atomMinDomain_S = null;
		//compute the number of unsatisfied clauses that each predicate participates in
		Map<Integer,Integer> degree = new HashMap<Integer,Integer>();
		// stores origDomainSize of each pred. Stores min if pred is not singleton
		Map<Integer,Integer> origDomainSizePred = new HashMap<Integer,Integer>();
		for(int i=0;i<CNF.clauses.size();i++){
			WClause clause = CNF.clauses.get(i);
			boolean checkforsatclause=true;
			if(!clause.isSatisfied() || checkforsatclause){
				for(Atom atom : clause.atoms){
					if(degree.containsKey(atom.symbol.id)){
						degree.put(atom.symbol.id, degree.get(atom.symbol.id)+1);
					}
					else{
						degree.put(atom.symbol.id, 1);
					}
					if(!origDomainSizePred.containsKey(atom.symbol.id)){
						if(atom.activeTermsIndices.size() == 1){
							origDomainSizePred.put(atom.symbol.id, atom.terms.get(atom.activeTermsIndices.get(0)).origDomain.size());
						}
					}
				}
			}
		}
		Atom movieAtom = null;
		for(int i=0;i<CNF.clauses.size();i++){
			WClause clause = CNF.clauses.get(i);
			//boolean checkforsatclause=false;
			boolean checkforsatclause=true;
			//do not choose atoms in satisfied clauses
			if(clause.isSatisfied() && !checkforsatclause)
				continue;
			for(Atom atom : clause.atoms){
				int atomDeg = 0;
				int atomDomainSize = 0;
				if(degree.containsKey(atom.symbol.id)){
					atomDeg = degree.get(atom.symbol.id);
				}
				if(origDomainSizePred.containsKey(atom.symbol.id)){
					atomDomainSize = origDomainSizePred.get(atom.symbol.id);
				}
				// Singleton atom
				if(atom.activeTermsIndices.size() == 1){
					if(atomDeg > maxDegreeSingleton)
					{
						maxDegreeSingleton = atomDeg;
						atomMaxDegree_S = atom;
					}
					if(atomDomainSize < minDomainSingleton){
						minDomainSingleton = atomDomainSize;
						atomMinDomain_S = atom;
					}
				}
				else if(atom.activeTermsIndices.size()>1)
				{
					if(atomDeg > maxDegreeNST)
					{
						maxDegreeNST = atomDeg;
						atomMaxDegree_NS = atom;
					}
					if(atom.symbol.symbol.startsWith("movie")){
						movieAtom = atom;
					}	
				}
				else if(atom.activeTermsIndices.size() == 0)
				{
					if(atomDeg > maxDegreeConstant)
					{
						maxDegreeConstant= atomDeg;
						atomMaxDegree_Const = atom;
					}
				}
			
			}
		}
		if(atomMinDomain_S != null){
			return atomMinDomain_S;
		}
		else if(atomMaxDegree_S != null){
			return atomMaxDegree_S;
		}
		else if(atomMaxDegree_Const != null)
			return atomMaxDegree_Const;
		else
		{
			//return atomMaxDegree_NS;
			return movieAtom;
		}
		
	}
	public static Atom getAtomToSplit(MLN CNF){
		//get the atom which is singleton and has minimum domain
		Atom minDomainSingleton = null;
		int minNumDomainsST = 100000;
		Atom minDomainNonSingleton = null;
		int minNumDomainsNST = 100000;
		Atom minDomainUCAtom = null;
		int minNumDomainsUCAtom = 100000;
		int maxDegreeSingleton = 0;
		int maxDegreeNST = 0;
		int maxDegreeUC = 0;
		int maxDegreeConstant = 0;
		Atom atomMaxDegree_S = null;
		Atom atomMaxDegree_NS = null;
		Atom atomMaxDegree_UC = null;
		Atom atomMaxDegree_Const = null;
		
		//compute the number of unsatisfied clauses that each predicate participates in
		Map<Integer,Integer> degree = new HashMap<Integer,Integer>();
		for(int i=0;i<CNF.clauses.size();i++){
			WClause clause = CNF.clauses.get(i);
			boolean checkforsatclause=false;
			if(!clause.isSatisfied() || checkforsatclause){
				for(Atom atom : clause.atoms){
					if(degree.containsKey(atom.symbol.id)){
						degree.put(atom.symbol.id, degree.get(atom.symbol.id)+1);
					}
					else{
						degree.put(atom.symbol.id, 1);
					}
				}
			}
		}
		
		for(int i=0;i<CNF.clauses.size();i++){
			WClause clause = CNF.clauses.get(i);
			boolean checkforsatclause=false;
			//do not choose atoms in satisfied clauses
			if(clause.isSatisfied() && !checkforsatclause)
				continue;
			for(Atom atom : clause.atoms){
				int atomDeg = 0;
				if(degree.containsKey(atom.symbol.id)){
					atomDeg = degree.get(atom.symbol.id);
				}
				int index = atom.isSingletonAtom();
				if(index != -1){
					// For now, not using below heuristic
					//if(atom.terms.get(index)domain.size() < minNumDomainsST)
					//{
						//minDomainSingleton=CNF[i]->atoms[k];
						//minNumDomainsST=CNF[i]->atoms[k]->terms[index]->domain.size();
						
					//}
					if(atomDeg > maxDegreeSingleton)
					{
						maxDegreeSingleton = atomDeg;
						atomMaxDegree_S = atom;
					}		
				}
				else if(atom.terms.size()>1)
				{
					// For now, not using below heuristic (min domain)
					/*
					int totalSize=1;
					for(unsigned int m=0;m<CNF[i]->atoms[k]->terms.size();m++)
					{
						totalSize*=CNF[i]->atoms[k]->terms[m]->domain.size();
					}
					if(totalSize < minNumDomainsNST )
					{
						minDomainNonSingleton=CNF[i]->atoms[k];
						minNumDomainsNST=totalSize;
					}*/
					if(atomDeg > maxDegreeNST)
					{
						maxDegreeNST = atomDeg;
						atomMaxDegree_NS = atom;
					}

					if(clause.atoms.size()==1)
					{
						
						// For now, not using below heuristic
						/*
						//unit clause
						if(totalSize < minNumDomainsUCAtom)
						{
							minNumDomainsUCAtom = totalSize;
							minDomainUCAtom = CNF[i]->atoms[k];

						}*/
						if(atomDeg > maxDegreeUC)
						{
							maxDegreeUC= atomDeg;
							atomMaxDegree_UC = atom;
						}

					}
				}
				else if(atom.isConstant())
				{
					if(atomDeg > maxDegreeConstant)
					{
						maxDegreeConstant= atomDeg;
						atomMaxDegree_Const = atom;
					}
				}
			
			}
		}
		if(atomMaxDegree_S != null){
			return atomMaxDegree_S;
		}
		else if(atomMaxDegree_Const != null)
			return atomMaxDegree_Const;
		// For now, skiiping below heuristic
		/*
		if(minDomainSingleton)
			return minDomainSingleton;
		*/
		else
		{
			//find best non singleton using lookahead
			/*Atom* atom = decomposition_lookahead(CNF);
			if(atom!=NULL)
				return atom;
			else
			*/
			//return minDomainNonSingleton; // Commented by Happy
			return atomMaxDegree_NS;
		}
	}
	
}
