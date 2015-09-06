from math import exp
from math import log

def findAve(zValues):
	maxVal = max(zValues)
	zValues = [exp(x-maxVal) for x in zValues]
	avg = log(sum(zValues)/len(zValues)) + maxVal
	return avg
	
inFile = 'imdb.5_evid_0_20_100_normal_false_approx_true_1000_iter.out.log'
outFile = 'minZ.'+inFile
f0 = open(inFile,'r')
f1 = open(outFile,'w')
numPartitions = 7
for line in f0:
	if line.startswith('zArray'):
		avgZPartitions = []
		allZStr = line.split('[')[1].split(']')[0]
		allZVals = allZStr.split(',')
		allZVals = [float(x) for x in allZVals]
		valsPerPartition = len(allZVals)/numPartitions
		for i in range(numPartitions):
			zValues = []
			zValues = allZVals[i*valsPerPartition:i*valsPerPartition+valsPerPartition]
			avgZPartitions.append(findAve(zValues))
		f1.write(str(min(avgZPartitions))+'\n')
		
f0.close()
f1.close()
