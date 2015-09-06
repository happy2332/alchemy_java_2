start_dom_size = 10
end_dom_size = 50
dom_step_size = 10

#start_evid_per = 50
#end_evid_per = 50
#evid_per_step_size = 10
for i in range(start_dom_size, end_dom_size + dom_step_size, dom_step_size):
	f0 = open('mln/collaborate_mln_'+str(i)+'.txt','w')
	f0.write('#domains\n\
person={0, ..., '+str(i-1)+'}\n\
\n\
#predicates\n')
	for areaNum in range(3):
		f0.write('WorksInArea'+str(areaNum)+'(person)\n')
	f0.write('Collaborate(person,person)\n\
CoAuthor(person,person)\n\
\n\
#formulas\n')
	for areaNum in range(3):
		f0.write('(WorksInArea'+str(areaNum)+'(x))::1\n')
	f0.write('(Collaborate(x,y))::2\n\
(CoAuthor(x,y))::3\n')
	for areaNum in range(3):
		f0.write('(!WorksInArea'+str(areaNum)+'(x) | !WorksInArea'+str(areaNum)+'(y) | Collaborate(x,y))::2\n')
	f0.write('(!Collaborate(x,y) | CoAuthor(x,y))::3')
	f0.close();
