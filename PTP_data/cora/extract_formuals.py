f = open('temp.mln')
f1 = open('temp1.mln','w')
for line in f:
	if line in ['\n', '\r\n']:
		f1.write('\n')
		continue
	toWrite = "("
	if line.startswith('//'):
		continue;
	line = line.strip()
	print line
	elem = line.split()
	print len(elem)
	wt = elem[0]
	rest = "".join(elem[1])
	toWrite+=rest+')::'+wt+'\n'
	f1.write(toWrite)
