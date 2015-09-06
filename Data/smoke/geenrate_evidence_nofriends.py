import random

def rand_not(threshold):
	r = random.random()
	if r > threshold:
		return '!'
	else:
		return ''


dom_size = 50
people_list= [str(i) for i in range(dom_size)]
evid_percentage= 50
random.shuffle(people_list)
not_smokes_prob = 0.5
cancer_prob = 0.5
f = open('evidence.txt','w')
for i in range(len(people_list)*evid_percentage/100):
	person = people_list[i]
	smokes_sign = rand_not(not_smokes_prob);
	cancer_sign = rand_not(cancer_prob);
	f.write(smokes_sign+'S('+person+')\n')
	f.write(cancer_sign+'C('+person+')\n')
f.close()

