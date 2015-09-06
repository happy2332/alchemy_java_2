import sys
import re
import numpy
classes=[]
p=re.compile('SameBib|Class_\d+')
f = open(sys.argv[1], 'r+')
for line in f:
	t=p.findall(line)
	if(len(t)>0):
	 if(t[0]=='SameBib'):
		i=int(t[1].split("_")[1])
		j=int(t[2].split("_")[1])
		if i not in classes:
			classes.append(i)
		if j not in classes:
			classes.append(j)
		
f.close()
print classes
MAX=len(classes)
a=numpy.zeros(shape=(MAX,MAX))

f = open(sys.argv[1], 'r+')
for line in f:
	t=p.findall(line)
	if(len(t)>0):
	 if(t[0]=='SameBib'):
		i=int(t[1].split("_")[1])
		j=int(t[2].split("_")[1])
		a[classes.index(i)][classes.index(j)]=1
		a[classes.index(j)][classes.index(i)]=1
f.close()
#FWA
for k in range(0,MAX):
	for i in range(0,MAX):
		for j in range(0,MAX):
			a[i][j]=max(a[i][j],a[i][k]*a[k][j])

print "FWA done"
#form EC
files=[]
ctof=numpy.zeros(MAX)
for i in range(0,MAX):
	j=0
	k=0
	foundfirst=False
	while j<i+1:
		if a[i][j]>0 :
			if (not foundfirst):
				foundfirst=True
				k=j
				
				if(j==i):
				 files.append(k)
			a[i][j]=k
			ctof[i]=k				
			ctof[j]=k
		j=j+1
print"File allocation done"
#generate evidence
fh=[]
p1=re.compile('Class_\d+')
for i in files:
	fh.append(open('%d' %(i),'w'))
f = open(sys.argv[1], 'r+')
for line in f:
	t=p1.findall(line)
	if(len(t)>0):
		i=int(t[0].split("_")[1])
		if i in classes: 
		 ind=classes.index(i)
		 fh[files.index(ctof[ind])].write(line)
		else:
		  for h in fh:
			h.write(line)
	else:
		for h in fh:
			h.write(line)
for h in fh:
	h.close()
f.close()
print "Evidence files generated"		
			

