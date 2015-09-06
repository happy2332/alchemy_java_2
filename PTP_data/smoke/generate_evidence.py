evidList = []
dom=80
from random import *
for i in range(dom):
	sign=""
	if randint(0,1) < 0.5:
			sign = "!"
	evidList.append(sign+'S('+str(i)+')\n')
	evidList.append(sign+'C('+str(i)+')\n')

for i in range(dom):
	for j in range(dom):
		sign=""
		if randint(0,1) < 0.5:
			sign = "!"
		evidList.append(sign+'F('+str(i)+','+str(j)+')\n')

shuffle(evidList)
numEvidence = len(evidList)
for i in range(10):
	evid = (numEvidence*i)/10
	f1 = open('smoke_evidence_'+str(dom)+"_"+str(i)+'.txt','w')
	for evidence in evidList[:evid]:
		f1.write(evidence)
	f1.close()

