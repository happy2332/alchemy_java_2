n = 5 # numpreds
m = 5 #numclause
s = 3 # predperclause
c = 10 #domain size
e = int(n*c) # evidence
v = s+1 # vars
from random import randint
f0 = open('random_pkb_mln.txt','w')

f0.write('#domains\ndom={')
for i in range(c):
	f0.write(str(i))
	if(i != c-1):
		f0.write(',')
f0.write('}\n')

f0.write('#predicates\n')	
#Generate pred names
preds = []
for i in range(n):
	preds.append("P"+str(i))
	f0.write("P"+str(i)+"(dom)\n")
	
#Generate variables
var = []
for i in range(v):
	var.append("x"+str(i))
	
wt = 0.0001		
#Generate clauses
#clauses = []
f0.write('#formulas\n')
for i in range(m):
	clause = "("
	pair_list = []
	for j in range(s):
		sign = ""
		if randint(0,1) < 0.5:
			sign = "!"
		while(True):
			pair = [preds[randint(0,n-1)],var[randint(0,v-1)]]
			if(pair not in pair_list):
				clause += sign + pair[0] + "(" + pair[1] + ")"
				pair_list.append(pair)
				break
				
		if(j != s-1):
			clause += " | "
	clause += ")::"+str(wt)+"\n"
	f0.write(clause)
	
f0.close()

pair_list = []
evidence_list = []
for i in range(c):
		sign = ""
		if(randint(0,1) < 0.5):
			sign = "!"
		while(True):
			pair = [preds[randint(0,0)],str(randint(0,c-1))]
			if(pair not in pair_list):
				evidence = sign + pair[0] + "(" + pair[1] + ")" + "\n"
				pair_list.append(pair)
				evidence_list.append(evidence)
				break;
							
for i in range(10):
	evid = (e*i)/10
	f1 = open('random_evidence'+str(i)+'.txt','w')
	for evidence in evidence_list[:evid]:
		f1.write(evidence)
	f1.close()
