tDom = 5
sDom = 10
cDom = 10
jDom = 5

import random 


evid_percentage = 50
f = open('student_evidence.txt','w')
student_list = [str(i) for i in range(sDom)]
random.shuffle(student_list)
student_list = student_list[:evid_percentage*sDom/100]
for s in student_list:
	joboffered = [str(i) for i in range(jDom)]
	random.shuffle(joboffered)
	f.write('JobOffered('+s+','+joboffered[0]+')\n')

f.close()
'''

# generate teaches : for each teacher, randomly choose one course which she teaches. Also don't alot one course to
# multiple teachers
courses = [str(i) for i in range(cDom)]
random.shuffle(courses)
for i in range(tDom):
	f.write('Teaches('+str(i)+','+courses[i]+')\n')
'''
'''	
# generate takes : for each student, randomly choose some courses which he takes. No. of courses come from normal distribution
# with mean as 5 and std 2
for s in range(sDom):
	numCourses = int(random.gauss(5,2))
	courses = [str(i) for i in range(cDom)]
	random.shuffle(courses)
	courses = courses[:numCourses]
	for course in courses:
		f.write('Takes('+str(s)+','+course+')\n')

f.close()

'''
