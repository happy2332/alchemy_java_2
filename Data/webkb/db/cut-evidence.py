import random
evid_per = 40
'''
personNum = 271
person_list = [str(i) for i in range(personNum)]
random.shuffle(person_list)
person_list = person_list[:len(person_list)*evid_per/100]
'''
f0 = open('webkb.texas.5class.dom.20.evid.100.eclipse.txt','r')
# Find unique pages
pages_set = set()
for line in f0:
	line = line.strip()
	page = line.split("(")[1].split(")")[0]
	pages_set.add(page)
#print pages_set
#print len(pages_set)
pages_list = list(pages_set)
random.shuffle(pages_list)
pages_list = pages_list[:len(pages_list)*evid_per/100]
f0.close()

f0 = open('webkb.texas.5class.dom.20.evid.100.eclipse.txt','r')
f1 = open('webkb.texas.5class.dom.20.evid.'+str(evid_per)+'.eclipse.txt','w')
for line in f0:
	line = line.strip()
	line1 = line.strip('!')
	page = line.split('(')[1].split(')')[0]
	if page in pages_list:
		f1.write(line+'\n')

f0.close()
f1.close()

