import random

def rand_not():
	r = random.random()
	if r < 0.5:
		return '!'
	else:
		return ''


dom_size = 100
num_groups = 1
group_card = dom_size/num_groups
f = open('evidence.txt','w')
for grp_num in range(num_groups):
	evid_list= []
	start = grp_num*group_card
	end = start + group_card
	for i in range(start,end):
		evid_list.append(rand_not()+'S('+str(i)+')')
		evid_list.append(rand_not()+'C('+str(i)+')')
		for j in range(start+i,end):
			evid_list.append(rand_not()+'F('+str(i)+','+str(j)+')')

	random.shuffle(evid_list)
	evid_percentage= 50
	for i in range(len(evid_list)*evid_percentage/100):
		evid = evid_list[i]
		sign = ''
		if 'F(' in evid:
			[p1,p2] = evid.split(',')
			p1 = p1.split('(')[1]
			p2 = p2.split(')')[0]
			if '!' in evid:
				sign = '!'
			if p1!=p2:
				f.write(sign+'F('+p2+','+p1+')'+'\n')
		f.write(evid_list[i]+'\n')
f.close()

