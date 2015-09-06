normal=false
approx=true
evidPer=100
outFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/webkb/results/webkb_"$evidPer"_"$normal"_approx_"$approx".out"
inputFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/webkb/mln/webkb-eclipse-mln.txt"
#evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/webkb/db/webkb."$evidPer".texas-eclipse.txt"		
evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/webkb/mln/webkb.texas-eclipse.txt"
#java -jar /home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/ptp.jar -i $inputFile -e $evidFile -o $outFile -q PageClass_0_Staff,PageClass_0_Course,LinkTo,PageClass_0_Person,PageClass_0_Faculty,PageClass_0_ResearchProject,PageClass,PageClass_0_Student,PageClass_0_Department -approx
java -jar /home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/ptp.jar -i $inputFile -e $evidFile -o $outFile -q LinkTo -approx
