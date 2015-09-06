# Create evidence by first creating full domain and then subtracting given evidence file from it.
personNum = 271
domain = set()
for i in range(personNum):
	domain.add('PageClass_0_Department('+str(i)+')')

#for i in range(personNum):
#	domain.add('PageClass_0_Staff('+str(i)+')')

for i in range(personNum):
	domain.add('PageClass_0_Course('+str(i)+')')

for i in range(personNum):
	domain.add('PageClass_0_Faculty('+str(i)+')')

for i in range(personNum):
	domain.add('PageClass_0_Person('+str(i)+')')

#for i in range(personNum):
#	domain.add('PageClass_0_ResearchProject('+str(i)+')')

for i in range(personNum):
	domain.add('PageClass_0_Student('+str(i)+')')

f0 = open('webkb.texas.eclipse.txt','r')
f1 = open('webkb.texas.5class.evid.100.eclipse.txt','w')
evidence = set()
for line in f0:
	if "Research" in line or "Staff" in line:
		continue
	f1.write(line)
	line = line.strip()
	evidence.add(line)

rem_evidence = domain.difference(evidence)
for i in rem_evidence:
	f1.write('!'+i+'\n')

f0.close()
f1.close()
