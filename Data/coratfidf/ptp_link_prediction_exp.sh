normal=false
approx=true
outFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/coratfidf/mln/cora_"$normal"_approx_"$approx".out"
inputFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/coratfidf/mln/coratfidf-eclipse_mln.txt"
evidFile="/home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/Data/coratfidf/db/test/coratfidf-eclipse.1.db"		
java -jar /home/happy/experiments/approx_code/LiftedMAP/LiftedMAP/ptp.jar -i $inputFile -e $evidFile -o $outFile -q SameBib,SameAuthor,SameVenue,SameTitle -approx -queryEvidence
