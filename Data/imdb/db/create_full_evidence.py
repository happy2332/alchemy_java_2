# Create evidence by first creating full domain and then subtracting given evidence file from it.
personNum = 59
movieNum = 4
domain = set()
for i in range(personNum):
	domain.add('actor('+str(i)+')')

for i in range(personNum):
	domain.add('director('+str(i)+')')

for i in range(movieNum):
	for j in range(personNum):
		domain.add('movie('+str(i)+','+str(j)+')')

f0 = open('imdb.2.eclipse.txt','r')
f1 = open('imdb.2.evid.100.eclipse.txt','w')
evidence = set()
for line in f0:
	if not line.startswith('worked'):
		f1.write(line)
	line = line.strip()
	evidence.add(line)

rem_evidence = domain.difference(evidence)
for i in rem_evidence:
	f1.write('!'+i+'\n')

f0.close()
f1.close()
