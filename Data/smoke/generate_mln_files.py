start_dom_size = 550
end_dom_size = 1000
dom_step_size = 50
#start_evid_per = 50
#end_evid_per = 50
#evid_per_step_size = 10
for i in range(start_dom_size, end_dom_size + dom_step_size, dom_step_size):
	f0 = open('ptp_smoke/smoke_mln_'+str(i)+'.txt','w')
	f0.write('#domains\n\
dom1={0, ..., '+str(i-1)+'}\n\n\
#predicates\n\
C(dom1)\n\
S(dom1)\n\
F(dom1,dom1)\n\n\
#formulas\n\
(!S(x)) :: 1\n\
(!C(x)) :: 2\n\
(!F(x,y)) :: 3\n\
( !S(x) | C(x)) :: 2\n\
( !S(x) | !F(x,y) | S(y)) :: 3')
	f0.close();
