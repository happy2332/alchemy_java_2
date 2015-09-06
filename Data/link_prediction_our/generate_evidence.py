import random


start_dom_size = 20
end_dom_size = 100
dom_step_size = 10
start_evid_per = 50
end_evid_per = 50
evid_per_step_size = 10


for i in range(start_dom_size, end_dom_size+dom_step_size, dom_step_size):
	prof_list= [str(x) for x in range(i-10)]
	random.shuffle(prof_list)
	student_list = [str(x) for x in range(i)]
	random.shuffle(student_list)
	for j in range(start_evid_per, end_evid_per+evid_per_step_size, evid_per_step_size):
		f = open('ptp_link_prediction/evidence_'+str(i)+'_'+str(j)+'.txt','w')
		for k in range(len(prof_list)*50/100):
			prof = prof_list[k]
			prof_area = random.choice(range(3))
			for area in range(3):
				if area == prof_area:
					f.write('WorksInArea'+str(area)+'('+prof+')\n')
				else:
					f.write('!WorksInArea'+str(area)+'('+prof+')\n')
		for k in range(len(student_list)*j/100):
			student = student_list[k]
			stud_area = random.choice(range(3))
			for area in range(3):
				if area == stud_area:
					f.write('InterestedArea'+str(area)+'('+student+')\n')
				else:
					f.write('!InterestedArea'+str(area)+'('+student+')\n')
		f.close()

