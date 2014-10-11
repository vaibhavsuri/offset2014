set term wxt size 700, 450
unset label          #remove any previous labels
set xrange [0:1000]
set yrange [0:1000]
set xtic nomirror auto       #set xtics automatically
set ytic nomirror auto       #set ytics automatically
betas = " 8 10 15 19 "
lookup_beta(i) = word(betas,i)
set title "The Dependence of Player 1's Score on Player 2's Score"
set xlabel "Player 2 Score"
set ylabel "Player 1 Score"
set nox2tics
set border 3
point=1.5
plot  for [i=1:4] 'movesdelta_vs_dumb_'.lookup_beta(i).'.txt' using 1:2 title "offset= ".lookup_beta(i) with points 
