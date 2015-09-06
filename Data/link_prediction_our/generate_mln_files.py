start_dom_size = 20
end_dom_size = 100
dom_step_size = 10

#start_evid_per = 50
#end_evid_per = 50
#evid_per_step_size = 10
for i in range(start_dom_size, end_dom_size + dom_step_size, dom_step_size):
	f0 = open('ptp_link_prediction/link_prediction_mln_'+str(i)+'.txt','w')
	f0.write('#domains\n\
prof={0,..., '+str(i-10-1)+'}\n\
student={0, ..., '+str(i-1)+'}\n\
\n\
#predicates\n')
	for areaNum in range(3):
		f0.write('WorksInArea'+str(areaNum)+'(prof)\n')
	for areaNum in range(3):
		f0.write('InterestedArea'+str(areaNum)+'(student)\n')	
	f0.write('Advises(prof,student)\n\
CoAuthor(prof,student)\n\
\n\
#formulas\n')
	for areaNum in range(3):
		f0.write('(WorksInArea'+str(areaNum)+'(x))::1\n')
		f0.write('(InterestedArea'+str(areaNum)+'(x))::2\n')
	f0.write('(Advises(x,y))::2\n\
(CoAuthor(x,y))::3\n')
	for areaNum in range(3):
		f0.write('(!WorksInArea'+str(areaNum)+'(x) | !InterestedArea'+str(areaNum)+'(y) | Advises(x,y))::2\n')
	f0.write('(!Advises(x,y) | CoAuthor(x,y))::3')
	f0.close();
