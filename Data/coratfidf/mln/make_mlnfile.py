f0 = open('coratfidf-eclipse.mln','r')
f1 = open('coratfidf-eclipse_mln.txt','w')
formula_started = False
for line in f0:
	if not formula_started:
		f1.write(line)
	else:
		if line.startswith('//'):
			continue
		formula = line.split()
		print formula
		if len(formula) > 1:
			f1.write('(')
			print len(formula)
			for i in range(1,len(formula)+1,2):
				f1.write(formula[i])
				if(i != len(formula)-1):
					f1.write(' | ')
			f1.write(')::2'+'\n')
		
	if line.startswith('#formulas'):
		formula_started = True
	
