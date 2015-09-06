stud_start_dom_size = 10
stud_end_dom_size = 90
stud_dom_step_size = 10

prof_start_dom_size = 100
prof_end_dom_size = 100
prof_dom_step_size = 100

for i in range(stud_start_dom_size, stud_end_dom_size + stud_dom_step_size, stud_dom_step_size):
	f0 = open('mln/link_prediction_mln_'+str(i)+'.txt','w')
	f0.write('#domains\n\
prof={0,..., '+str(prof_start_dom_size-1)+'}\n\
student={0, ..., '+str(i-1)+'}\n\
\n\
#predicates\n\
GoodProf(prof)\n\
GoodStudent(student)\n\
FutureProf(student)\n\
Advises(prof,student)\n\
CoAuthor(prof,student)\n\
\n\
#formulas\n\
(GoodProf(x))::2\n\
(GoodStudent(x))::1\n\
(FutureProf(x))::3\n\
(Advises(x,y))::4\n\
(CoAuthor(x,y))::5\n\
(!GoodProf(y) | !GoodStudent(x) | !Advises(y,x) | FutureProf(x))::1\n\
(!CoAuthor(x,y) | Advises(x,y))::2')
	f0.close();
