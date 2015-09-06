f = open('er-test_temp.db','r')
f1 = open('er-test-our.db','w')
pred_list = ['Author','Title','Venue','HasWordAuthor','HasWordTitle','HasWordVenue']
auth_dic = {}
venue_dic = {}
title_dic = {}
bib_dic={}
word_dic = {}
pred_index = 0
for line in f:
	if line in ['\n', '\r\n']:
		pred_index+=1
		f1.write('\n')
		continue
	line = line.rstrip()
	consts = line.split(',')
	line = pred_list[pred_index]+"("
	for const in consts:
		const = const.strip()
		if const.startswith('Author_'):
			if const not in auth_dic:
				auth_dic[const] = len(auth_dic)
			line += str(auth_dic[const])
		if const.startswith('Title_'):
			if const not in title_dic:
				title_dic[const] = len(title_dic)	
			line += str(title_dic[const])
		if const.startswith('Venue_'):
			if const not in venue_dic:
				venue_dic[const] = len(venue_dic)
			line += str(venue_dic[const])
		if const.startswith('Class_'):
			if const not in bib_dic:
				bib_dic[const] = len(bib_dic)
			line += str(bib_dic[const])
		if const.startswith('Word_'):
			if const not in word_dic:
				word_dic[const] = len(word_dic)
			line += str(word_dic[const])
		line += ','
	line += ')\n'
	f1.write(line)
print len(auth_dic)-1
print len(venue_dic)-1
print len(title_dic)-1
print len(bib_dic)-1
print len(word_dic)-1
	
