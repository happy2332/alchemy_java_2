set term postscript eps enhanced color 'Helvetica' 26
set output "ps-size-time.eps"
set auto y
set logscale y
set xlabel "Domain Size" font "Helvetica,32"
set ylabel "Time (in seconds)" font "Helvetica,32"
#set yrange [0:800]
#set yrange [1:10000]
#set xrange [0:300]
set key left top
plot "ps-size-time.data" using 1:2 with linespoints ls 1 ps 2 pt 5 lw 3 title 'SetInEq',\
 "ps-size-time.data" using 1:3 with linespoints ls 3 ps 2 pt 7 lw 3 title 'Normal'
