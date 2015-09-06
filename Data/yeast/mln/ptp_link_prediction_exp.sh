normal=false
approx=true
outFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/yeast/yeast_normal_"$normal"_approx_"$approx".out"
inputFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/yeast/mln/yeast-eclipse-mln.txt"
evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/yeast/mln/yeast.1-eclipse.txt"		
java -jar /home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/ptp.jar -i $inputFile -e $evidFile -o $outFile -q interaction,function -approx
