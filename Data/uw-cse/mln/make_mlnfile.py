f0 = open('uw-cse-eclipse.mln','r')
f1 = open('uw-cse-eclipse-mln.txt','w')
PRED = 0
FORMULA = 1
state = PRED
import re
constants = set()
preds = {} # key : pred_name, value : list of arg types
pred_pos_dic = {} # key : pred_name, value : lists of arg indices which are constants in MLN
formulas = []

def parsePredStringMLN(line):
	pred_name = line.split('(')[0]
	args = re.search(r'\((.*)\)',line).group(1).split(',')
	args = [arg.strip() for arg in args]
	preds[pred_name] = args
	pred_pos_dic[pred_name] = []

def parseClauseStringMLN(line):
	final_formula = '('
	line_parts = line.split()
	wt = line_parts[0]
	# go on odd numbered parts, because on even positions, there is 'v' (or) sign
	for i in range(1,len(line_parts)+1,2):
		pred_name = line_parts[i].split('(')[0]
		arg_types = preds[pred_name.strip('!')]
		args = re.search(r'\((.*)\)',line_parts[i]).group(1).split(',')
		args = [arg.strip() for arg in args]
		new_pred_name = pred_name
		retain_args = []
		remove_args_indices = []
		retain_args_indices = []
		for j in range(len(args)):
			if args[j][0].isupper():
				new_pred_name += '_'+str(j)+'_'+args[j]
				constants.add(args[j])
				remove_args_indices.append(j)
			else:
				retain_args.append(args[j])
				retain_args_indices.append(j)
		
		# If new pred found, add it to predsList
		if pred_name != new_pred_name:
			preds[new_pred_name.strip('!')] = []
			if not remove_args_indices in pred_pos_dic[pred_name.strip('!')]:
				pred_pos_dic[pred_name.strip('!')].append(remove_args_indices)
			for arg_index in retain_args_indices:
				preds[new_pred_name.strip('!')].append(arg_types[arg_index])
		
		# write atom into formula
		final_formula += new_pred_name+'('
		for arg in retain_args[:-1]:
			final_formula += arg + ','
		final_formula += retain_args[-1]+')'
		if(i != len(line_parts)-1):
			final_formula += ' | '
	final_formula += ')::'+wt
	formulas.append(final_formula)
		
for line in f0:
	if line in ['\n','\r\n'] or line.startswith('//'):
		f1.write(line)
		continue
	if line.startswith('#predicates'):
		state = PRED
		continue
	if line.startswith('#formulas'):
		state = FORMULA
		continue
	if state == PRED:
		parsePredStringMLN(line)
	elif state == FORMULA:
		parseClauseStringMLN(line)
	else:
		break


domain_dic = {} # key : arg_type, value : string constants
for pred in preds:
	for arg_type in preds[pred]:
		domain_dic[arg_type] = []
#print domain_dic
		
f2 = open('ai.db','r')
f3 = open('evidence.db','w')

for line in f2:
	#f1.write(line)
	if line in ['\n','\r\n'] or line.startswith('//'):
		continue
	pred_name = line.split('(')[0]
	f3.write(pred_name+'(')
	args = re.search(r'\((.*)\)',line).group(1).split(',')
	args = [arg.strip() for arg in args]
	for i in range(len(args)):
		arg_type = preds[pred_name][i]
		if not args[i] in domain_dic[arg_type]:
			domain_dic[arg_type].append(args[i])
		f3.write(str(domain_dic[arg_type].index(args[i])))
		if i!=len(args)-1:
			f3.write(',') 
	f3.write(')\n')
	# make new preds if needed
	for remove_arg_indices in pred_pos_dic[pred_name.strip('!')]:
		new_pred_name = pred_name
		retain_arg_indices = list(set(range(len(args))).difference(set(remove_arg_indices)))
		retain_arg_indices.sort()
		for arg_index in remove_arg_indices:
			new_pred_name += '_'+str(arg_index)+'_'+args[arg_index]
		#print new_pred_name
		if new_pred_name.strip('!') in preds:
			f3.write(new_pred_name+'(')
			#print retain_arg_indices
			for arg_index in retain_arg_indices[:-1]:
				arg_type = preds[pred_name][arg_index]
				f3.write(str(domain_dic[arg_type].index(args[arg_index]))+',')
			arg_type = preds[pred_name][retain_arg_indices[-1]]
			f3.write(str(domain_dic[arg_type].index(args[retain_arg_indices[-1]]))+')\n')

f1.write('#domains\n')
for arg_type in domain_dic:
	f1.write(arg_type+'={0, ... ,'+str(len(domain_dic[arg_type])-1)+'}\n')
f1.write('\n')
f1.write('#predicates\n')
for pred_name in preds:
	f1.write(pred_name+'(')
	for arg in preds[pred_name][:-1]:
		f1.write(arg+',')
	f1.write(preds[pred_name][-1]+')\n')
f1.write('\n')
f1.write('#formulas\n')
for formula in formulas:
	f1.write(formula+'\n')
