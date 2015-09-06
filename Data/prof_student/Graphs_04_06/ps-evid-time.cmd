set term postscript eps enhanced color 'Helvetica' 26
set output "ps-evid-time.eps"
set auto y
set logscale y
set xlabel "Evidence %" font "Helvetica,32"
set ylabel "Time (in seconds)" font "Helvetica,32"
#set yrange [-10:1200]
set yrange [1:10000]
#set xrange [0:300]
#set key right bottom
plot "ps-evid-time.data" using 1:2 with linespoints ls 1 pt 5 lw 3 ps 2 title 'SetInEq',\
 "ps-evid-time.data" using 1:3 with linespoints ls 3 pt 7 lw 3 ps 2 title 'Normal'
