normal=false
approx=false
startDom=10
domStepSize=50
endDom=10
startEvid=0
evidStepSize=10
endEvid=0
outFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/collaborate/results/collaborate_mln_dom_"$startDom"_"$domStepSize"_"$endDom"_evid_"$startEvid"_"$evidStepSize"_"$endEvid"_normal_"$normal"_approx_"$approx".out"
printf "startDom = %d, domStepSize = %d, endDom = %d, startEvid = %d, evidStepSize = %d, endEvid = %d\n" $startDom $domStepSize $endDom $startEvid $evidStepSize $endEvid > $outFile
for domSize in `seq $startDom $domStepSize $endDom`
do
	for evid_per in `seq $startEvid $evidStepSize $endEvid`
	do
		inputFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/collaborate/mln/collaborate_mln_"$domSize".txt"
		evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/collaborate/db/evidence_"$domSize"_"$evid_per".txt"		
		java -jar /home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/ptp_updated.jar -i $inputFile -e $evidFile -o $outFile -q WorksInArea0,WorksInArea1,WorksInArea2,Collaborate,CoAuthor
	done
done
