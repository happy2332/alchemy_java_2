import random
evid_per = 80
personNum = 59
person_list = [str(i) for i in range(personNum)]
random.shuffle(person_list)
person_list = person_list[:len(person_list)*evid_per/100]

f0 = open('/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/imdb/db/imdb.2.evid.100.eclipse.txt','r')
f1 = open('/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/imdb/db/imdb.2.evid.'+str(evid_per)+'.eclipse.txt','w')
for line in f0:
	line = line.strip()
	line1 = line.strip('!')
	if not line1.startswith('movie'):
		person = line.split('(')[1].split(')')[0]
		if person in person_list:
			f1.write(line+'\n')
	else:
		person = line.split(',')[1].split(')')[0]
		if person in person_list:
			f1.write(line+'\n')

f0.close()
f1.close()
