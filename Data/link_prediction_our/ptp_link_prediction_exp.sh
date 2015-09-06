normal=false
approx=false
outFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/link_prediction/ptp_link_prediction/link_prediction_mln_evid_50_normal_"$normal"_approx_"$approx".out"
for domSize in `seq 30 10 30`
do
	for evid_per in `seq 50 10 50`
	do
		inputFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/link_prediction/ptp_link_prediction/link_prediction_mln_"$domSize".txt"
		evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/link_prediction/ptp_link_prediction/evidence_"$domSize"_"$evid_per".txt"		
		java -jar /home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/ptp_updated.jar -i $inputFile -e $evidFile -o $outFile -q WorksInArea0,WorksInArea1,WorksInArea2,InterestedArea0,InterestedArea1,InterestedArea2,Advises,CoAuthor
	done
done
