f="res"
for VARIABLE in 10 20 30 40 50
do
	#python generate_random_pkb1.py $VARIABLE
	fn2="$VARIABLE""NCL_smoke""$f"
	java -jar NCL_smoke.jar $VARIABLE > $fn2
done
