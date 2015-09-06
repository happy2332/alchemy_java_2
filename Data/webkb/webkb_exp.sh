normal=false
approx=true
for domPer in `seq 20 20 40`
do
evidPer=40
outFile="results_04_06/webkb_"$domPer"_"$evidPer"_"$normal"_approx_"$approx"_1000iter.out"
logFile=$outFile".log"
inputFile="mln/webkb.dom."$domPer".eclipse.mln.txt"
evidFile="db/webkb.texas.5class.dom."$domPer".evid."$evidPer".eclipse.txt"		
#evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/webkb/mln/webkb.texas-eclipse.txt"
java -jar ../../ptp_first_order_disjoint.jar -i $inputFile -e $evidFile -o $outFile -q PageClass_0_Course,LinkTo,PageClass_0_Person,PageClass_0_Faculty,PageClass_0_Student,PageClass_0_Department -approx  -numIter 1000 | tee $logFile
done
#java -jar ../../ptp_02_06.jar -i $inputFile -e $evidFile -o $outFile -q LinkTo -approx -normal
