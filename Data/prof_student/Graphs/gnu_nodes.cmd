set term postscript eps enhanced color 'Helvetica' 20
set output "link-pred-dom-nodes.eps"
set auto y
#set logscale y
set xlabel "Domain Size"
set ylabel "No. of nodes"
#set yrange [-10:1200]
#set yrange [1:100000000]
#set xrange [0:300]
#set key right bottom
plot "link_prediction_evid_50_nodes.data" using 1:2 with linespoints pt 5 title 'setinq',\
 "link_prediction_evid_50_nodes.data" using 1:3 with linespoints lt 7 title 'normal'
