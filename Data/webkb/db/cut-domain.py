import random
domain_per = 20
'''
personNum = 271
person_list = [str(i) for i in range(personNum)]
random.shuffle(person_list)
person_list = person_list[:len(person_list)*evid_per/100]
'''
f0 = open('webkb.texas.5class.dom.100.evid.100.eclipse.txt','r')
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
pages_list = pages_list[:len(pages_list)*domain_per/100]
pages_dic = {}
page_count = 0
for page in pages_list:
	if not page in pages_dic:
		pages_dic[page] = page_count
		page_count += 1

f0.close()

print len(pages_dic)

f0 = open('webkb.texas.5class.dom.100.evid.100.eclipse.txt','r')
f1 = open('webkb.texas.5class.dom.'+str(domain_per)+'.evid.100.eclipse.txt','w')
for line in f0:
	line = line.strip()
	line1 = line.strip('!')
	page = line.split('(')[1].split(')')[0]
	pred = line.split('(')[0]
	if page in pages_list:
		f1.write(pred+'('+str(pages_dic[page])+')\n')

f0.close()
f1.close()

