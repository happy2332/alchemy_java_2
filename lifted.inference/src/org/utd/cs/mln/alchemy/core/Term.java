package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Term {

	// There is a one to one mapping between constants and the domain
	public int type;
	public List<TreeSet<Integer>> domain = new ArrayList<TreeSet<Integer>>();
	public List<Integer> origDomain = new ArrayList<Integer>();
	public List<ArrayList<Integer>> segmentDomain = new ArrayList<ArrayList<Integer>>(); 

	public Term() {
	}

	public Term(int type_, List<Set<Integer>> domain_, List<Integer> origDomain_) {
		type = type_;
		domain = new ArrayList<TreeSet<Integer>>();
		for(Set<Integer> hc : domain_){
			domain.add(new TreeSet<Integer>(hc));
		}
		origDomain = new ArrayList<Integer>(origDomain_);
	}
	/*
	public Term(int type_, List<Set<Integer>> segment){
		type = type_;
		segmentDomain = new ArrayList<>(hyperCube_);
	}
	*/
	public Term(int type_, int value) {
		type = type_;
		domain = new ArrayList<TreeSet<Integer>>();
		domain.add(new TreeSet<Integer>());
		domain.get(0).add(value);
	}
	
	public Term(int type_, Set<Integer> hyperCube) {
		type = type_;
		domain = new ArrayList<TreeSet<Integer>>();
		domain.add(new TreeSet<Integer>(hyperCube));
		origDomain = new ArrayList<Integer>(hyperCube);
	}

	boolean isConstant(){
		boolean constantDomain = true;
		Set<Integer> domainSet = new HashSet<Integer>();
		for(Set<Integer> s : domain){
			domainSet.addAll(s);
		}
		if(domainSet.size() > 1)
			constantDomain = false;
		return constantDomain;
	}
	
	Term(Term term) {
		type = term.type;
		for (int k = 0; k < term.domain.size(); k++)
			domain.add(term.domain.get(k));
		origDomain = term.origDomain;
	}

	/*
	@Override
	public String toString() {
		return "Term [type=" + type + ", domain=" + domain + ", segmentDomain="
				+ segmentDomain + "]";
	}
	*/
}
