import random

start_dom_size = 10
end_dom_size = 50
dom_step_size = 10
start_evid_per = 0
end_evid_per = 100
evid_per_step_size = 10


for i in range(start_dom_size, end_dom_size+dom_step_size, dom_step_size):
	person_list = [str(x) for x in range(i)]
	random.shuffle(person_list)
	for j in range(start_evid_per, end_evid_per+evid_per_step_size, evid_per_step_size):
		f = open('db/evidence_'+str(i)+'_'+str(j)+'.txt','w')
		for k in range(len(person_list)*j/100):
			person = person_list[k]
			person_area = random.choice(range(3))
			for area in range(3):
				if area == person_area:
					f.write('WorksInArea'+str(area)+'('+person+')\n')
				else:
					f.write('!WorksInArea'+str(area)+'('+person+')\n')
		f.close()

