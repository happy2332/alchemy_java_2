f="res"
for VARIABLE in 10 20 30
do
	#python generate_random_pkb1.py $VARIABLE
	fn2="$VARIABLE""NCL""$f"
	java -jar NCL.jar $VARIABLE > $fn2
done
