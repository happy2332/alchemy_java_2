import random

stud_start_dom_size = 10
stud_end_dom_size = 90
stud_dom_step_size = 10
prof_start_dom_size = 100
prof_end_dom_size = 100
prof_dom_step_size = 100

start_evid_per = 40
end_evid_per = 40
evid_per_step_size = 20


for i in range(stud_start_dom_size, stud_end_dom_size+stud_dom_step_size, stud_dom_step_size):
	prof_list= [str(x) for x in range(prof_start_dom_size)]
	random.shuffle(prof_list)
	student_list = [str(x) for x in range(i)]
	random.shuffle(student_list)
	for j in range(start_evid_per, end_evid_per+evid_per_step_size, evid_per_step_size):
		f = open('db/evidence_'+str(i)+'_'+str(j)+'.txt','w')
		for k in range(len(student_list)*j/100):
			student = student_list[k]
			isGoodStudent = random.choice(range(2))
			if isGoodStudent == 0:
				f.write('!GoodStudent('+student+')\n')
			else:
				f.write('GoodStudent('+student+')\n')
		for k in range(len(prof_list)*50/100):
			prof = prof_list[k]
			isGoodProf = random.choice(range(2))
			if isGoodProf == 0:
				f.write('!GoodProf('+prof+')\n')
			else:
				f.write('GoodProf('+prof+')\n')
		f.close()

