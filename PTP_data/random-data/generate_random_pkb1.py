import sys
numPreds = 5
numClauses = 5
predsPerClause = 3
numConstants = int(sys.argv[1])
#numConstants = 3
numEvidence = numPreds*numConstants
numVariables = predsPerClause+1
from random import randint
import random
f0 = open('random_pkb_mln_'+str(numConstants)+'.txt','w')

f0.write('#domains\ndom={')
for i in range(numConstants):
	f0.write(str(i))
	if(i != numConstants-1):
		f0.write(',')
f0.write('}\n')

f0.write('#predicates\n')	
#Generate pred names
preds = []
for i in range(numPreds):
	preds.append("P"+str(i))
	f0.write("P"+str(i)+"(dom)\n")
	
#Generate variables
var = []
for i in range(numVariables):
	var.append("x"+str(i))
	
wt = 0.0001		
#Generate clauses
#clauses = []
f0.write('#formulas\n')
for i in range(numClauses):
	clause = "("
	pair_list = []
	predList = random.sample(preds,predsPerClause)
	for j in range(predsPerClause):
		sign = ""
		if randint(0,1) < 0.5:
			sign = "!"
		while(True):
			pair = [preds[randint(0,numPreds-1)],var[randint(0,numVariables-1)]]
			if(pair not in pair_list):
				clause += sign + pair[0] + "(" + pair[1] + ")"
				pair_list.append(pair)
				break
		if(j != predsPerClause-1):
			clause += " | "
	clause += ")::"+str(wt)+"\n"
	f0.write(clause)
	
f0.close()

pair_list = []
evidence_list = []
for i in range(numEvidence):
		sign = ""
		if(randint(0,1) < 0.5):
			sign = "!"
		while(True):
			pair = [preds[randint(0,numPreds-1)],str(randint(0,numConstants-1))]
			if(pair not in pair_list):
				evidence = sign + pair[0] + "(" + pair[1] + ")" + "\n"
				pair_list.append(pair)
				evidence_list.append(evidence)
				break;
							
for i in range(1):
	evid = (numEvidence*i)/10
	f1 = open('random_evidence_'+str(numConstants)+'_'+str(i)+'.txt','w')
	for evidence in evidence_list[:evid]:
		f1.write(evidence)
	f1.close()

