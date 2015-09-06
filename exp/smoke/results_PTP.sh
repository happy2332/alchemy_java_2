f="res"
for VARIABLE in 10 20 30 40 50
do
	#python generate_random_pkb1.py $VARIABLE
	fn1="$VARIABLE""PTP_smoke""$f"
	#fn2="$VARIABLE""NCL""$f"
	#java -jar NCL.jar $VARIABLE > $fn2
	java -jar PTP_smoke.jar $VARIABLE > $fn1
done
