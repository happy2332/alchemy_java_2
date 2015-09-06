import random

def rand_not(threshold):
	r = random.random()
	if r > threshold:
		return '!'
	else:
		return ''


start_dom_size = 550
end_dom_size = 1000
dom_step_size = 50
start_evid_per = 0
end_evid_per = 100
evid_per_step_size = 10
not_smokes_prob = 0.5
cancer_prob = 0.5


for i in range(start_dom_size, end_dom_size+dom_step_size, dom_step_size):
	people_list= [str(x) for x in range(i)]
	random.shuffle(people_list)
	for j in range(start_evid_per, end_evid_per + evid_per_step_size, evid_per_step_size):
		f = open('ptp_smoke/evidence_'+str(i)+'_'+str(j)+'.txt','w')
		f.write('//Smokes evidence, domain size : '+str(i)+', '+str(j)+'% evidence\n')
		for k in range(len(people_list)*j/100):
			person = people_list[k]
			smokes_sign = rand_not(not_smokes_prob);
			cancer_sign = rand_not(cancer_prob);
			f.write(smokes_sign+'S('+person+')\n')
			f.write(cancer_sign+'C('+person+')\n')
		f.close()

