set term wxt size 700, 450
unset label          #remove any previous labels
set xrange [0:1000]
set yrange [0:1000]
set xtic auto       #set xtics automatically
set ytic auto       #set ytics automatically
set title "The Dependence of Player 1's Score on Player 2's Score"
set xlabel "Player 2 Score"
set ylabel "Player 1 Score"
point=1.5
plot 'movesdelta_vs_dumb_15.txt' u 1:2 w points title 'Results'  
