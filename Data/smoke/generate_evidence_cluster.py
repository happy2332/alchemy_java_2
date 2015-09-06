import random

def rand_not(threshold):
	r = random.random()
	if r < threshold:
		return '!'
	else:
		return ''


dom_size = 500
num_clusters = 25

f = open('evidence.txt','w')
S_set = set()
C_set = set()
F_set = set()
for cluster_num in range(num_clusters):
	cluster_size = int(random.gauss(10,2))
	domain_list = [i for i in range(dom_size)]
	random.shuffle(domain_list)
	cluster = domain_list[:cluster_size]
	for i in range(len(cluster)):
		person1 = cluster[i]
		S_set.add(str(person1))
		C_set.add(str(person1))
		for j in range(len(cluster)):
			person2 = cluster[j]
			F_set.add((str(person1),str(person2)))


for person in S_set:
	f.write(rand_not(0.5)+'S('+person+')\n')

for person in C_set:
	f.write(rand_not(0.5)+'C('+person+')\n')
	
for (person1,person2) in F_set:
	f.write('F('+person1+','+person2+')\n')


f.close()

