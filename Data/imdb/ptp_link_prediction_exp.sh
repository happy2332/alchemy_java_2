normal=false
approx=true
startEvid=0
evidStepSize=20
endEvid=100
for fileNum in {2..5}
do
outFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/imdb/results_04_06/imdb."$fileNum"_evid_"$startEvid"_"$evidStepSize"_"$endEvid"_normal_"$normal"_approx_"$approx"_1000_iter.out"
logFile=$outFile".log"
printf "startEvid = %d, evidStepSize = %d, endEvid = %d\n" $startDom $domStepSize $endDom $startEvid $evidStepSize $endEvid > $outFile
for evid_per in `seq $startEvid $evidStepSize $endEvid`
do
	inputFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/imdb/mln/imdb."$fileNum"-eclipse-mln.txt"
	evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/imdb/db/imdb."$fileNum".evid."$evid_per".eclipse.txt"		
	java -jar /home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/ptp_first_order_disjoint.jar -i $inputFile -e $evidFile -o $outFile -approx -numIter 240 -q actor,director,movie,workedUnder -numIter 1000| tee --append $logFile
done
done
