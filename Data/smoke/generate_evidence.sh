for i in `seq 10 10 200`
do
echo $i
python generate_evidence_nofriends.py $i
done
