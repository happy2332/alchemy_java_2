normal=false
approx=false
startDom=300
domStepSize=100
endDom=300
startEvid=0
evidStepSize=20
endEvid=100
outFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/prof_student/results_04_06/link_prediction_mln_dom_"$startDom"_"$domStepSize"_"$endDom"_evid_"$startEvid"_"$evidStepSize"_"$endEvid"_normal_"$normal"_approx_"$approx".out"
printf "startDom = %d, domStepSize = %d, endDom = %d, startEvid = %d, evidStepSize = %d, endEvid = %d\n" $startDom $domStepSize $endDom $startEvid $evidStepSize $endEvid > $outFile
for domSize in `seq $startDom $domStepSize $endDom`
do
	for evid_per in `seq $startEvid $evidStepSize $endEvid`
	do
		inputFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/prof_student/mln/link_prediction_mln_"$domSize".txt"
		evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/prof_student/db/evidence_"$domSize"_"$evid_per".txt"		
		java -jar /home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/ptp_first_order_disjoint.jar -i $inputFile -e $evidFile -o $outFile -q GoodProf,GoodStudent,FutureProf,Advises,CoAuthor
	done
done
