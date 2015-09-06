evid_percentage = 10
f0 = open('/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/webkb/mln/webkb.texas-eclipse.txt')
f1 = open('/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/webkb/db/webkb.'+str(evid_percentage)+'.texas-eclipse.txt','w')
f1.write('// Webkb evidence file, pageclass evidence % : '+str(evid_percentage)+'\n')
pageclass_list = []
for line in f0:
	if not line.startswith('Page'):
		f1.write(line)
	else:
		pageclass_list.append(line)
		
f0.close()
import random
random.shuffle(pageclass_list)
for i in range(len(pageclass_list)*evid_percentage/100):
	f1.write(pageclass_list[i])
	
f1.close()
