package org.utd.cs.mln.lmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.utd.cs.gm.utility.DeepCopyUtil;
import org.utd.cs.mln.alchemy.core.CreateHyperCubeBasic;
import org.utd.cs.mln.alchemy.core.HyperCube;
public class testPTP {

	/**
	 * @param args
	 */
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
	public static void test(Integer i){
		
	}
	
	public static void main(String[] args) {
		Stack<Double>stack = new Stack<Double>();
		stack.push(1.0);
		stack.push(-1.0);
		stack.push(2.0);
		stack.push(-2.0);
		stack.push(3.0);
		System.out.println(eval_stack(stack));
		ArrayList<HyperCube> hcs = new ArrayList<HyperCube>();
		HyperCube h = new HyperCube();
		h.varConstants.add(new TreeSet<Integer>(Arrays.asList(0,1)));
		h.varConstants.add(new TreeSet<Integer>(Arrays.asList(0,1)));
		hcs.add(h);
		h = new HyperCube();
		h.varConstants.add(new TreeSet<Integer>(Arrays.asList(2)));
		h.varConstants.add(new TreeSet<Integer>(Arrays.asList(0,1)));
		hcs.add(h);
		h = new HyperCube();
		h.varConstants.add(new TreeSet<Integer>(Arrays.asList(0,1)));
		h.varConstants.add(new TreeSet<Integer>(Arrays.asList(2)));
		hcs.add(h);
		CreateHyperCubeBasic chcb = new CreateHyperCubeBasic();
		ArrayList<HyperCube> mhcs = chcb.mergeHyperCubes(hcs);
		System.out.println(mhcs);
		System.out.println(LisApproxInference.sampleBinomial(1));
		Integer j = new Integer(6);
		System.out.println(j);
		test(j);
		System.out.println(j);
	}

}
