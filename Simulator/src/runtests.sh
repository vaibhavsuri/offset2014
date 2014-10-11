#!/bin/bash


constant="" 
sim_output=""
result=""
nosim=0
pairRegEx='(\([0-9]+,[0-9]+\))'
myp=0
myq=0
herp=0
herq=0


#Parameters for simulator
offset=0
strategy="dumb1"
opponent="dumb"
strategy_dir="offset/${strategy}"
opponent_dir="offset/${opponent}"

#Parameter Sweeps (arbitrarily chosen by Richard)
psweep="1 2 3" 
dsweep="8 10 15 19"

Usage() {
    echo "Usage: runtests.sh [options]"
    echo "-g          Generate graphs; don't run simulator"
    echo "-d number   Specify constant d for testruns"	
    echo "-s strategy Strategy to use for testruns"
    echo "-h          Print this help"
    exit 1
}

# Set the p,q pairs for both players based on the offset
# and the loop in ForkAndWait
SetPQ() {
  if [ ${1} -eq 1 ] ; then
    if [ $((${offset}%2)) -eq 0 ] ; then
      myp=`expr ${offset} / 2 - 1`
      myq=`expr ${myp} + 2 `
    else
      myp=`expr ${offset} / 2`
      myq=`expr ${myp} + 1`
    fi
    herp=${2}
    herq=`expr ${offset} - ${herp}`
  else
    if [ $((${offset}%2)) -eq 0 ] ; then
      herp=`expr ${offset} / 2 - 1`
      herq=`expr ${herp} + 2 `
    else
      herp=`expr ${offset} / 2`
      herq=`expr ${herp} + 1`
    fi
    myp=${2}
    myq=`expr ${offset} - ${myp}`
  fi
}

ForkAndWait() {
  for i in ${psweep} ; do
    SetPQ ${1} ${i}
    if [ ! ${myp} -eq ${herp} -a ! ${myp} -eq ${herq} ] ; then
      output="result_${i}"
      ( java "offset.sim.Offset" "${offset}" "${strategy}${i}" "${opponent}${i}" "${output}" "False" "${myp}" "${myq}" "${herp}" "${herq}" &> /dev/null ) &
    fi
  done 
  FAIL=0
  for job in `jobs -p` ; do
    wait $job || let "FAIL+=1"
  done
  if [ ! ${FAIL} -eq 0 ] ; then
    echo "We had ${FAIL} failures!"
    exit 1
  fi
}

RunSim() {
  echo "Offset: $offset" >> ${sim_output}
  echo -e "Strategy: $strategy\n" >> ${sim_output}
  echo -e "Opponent: $opponent\n" >> ${sim_output}
  for i in ${psweep} ; do
    cp -r "${strategy_dir}" "${strategy_dir}${i}"
    newpackage="package offset.${strategy}${i};"
    for file in `ls "${strategy_dir}${i}"` ; do 
      if [ -f "${strategy_dir}${i}/${file}" ] ; then
        sed -i "1 s/^.*$/${newpackage}/" "${strategy_dir}${i}/${file}"
      fi
    done
    cp -r "${opponent_dir}" "${opponent_dir}${i}"
    newpackage="package offset.${opponent}${i};"
    for file in `ls "${opponent_dir}${i}"` ; do 
      if [ -f "${opponent_dir}${i}/${file}" ] ; then
        sed -i "1 s/^.*$/${newpackage}/" "${opponent_dir}${i}/${file}"
      fi
    done
  done 

  for k in `seq 1 1 2` ; do
    ForkAndWait ${k}

    #Collect results

    echo "Finished ${k} batch with offset = ${offset}"
    for j in ${psweep} ; do
      if [ -f "result_${j}" ] ; then
        output="result_${j}"
        pairs=`tail ${output} | grep 'Pair' | sed -r "s/^(.*)${pairRegEx}(.*)${pairRegEx}/\2 \4/" | xargs echo`
        scores=`tail ${output} | grep 'GAME' | sed -r 's/^.*\s([0-9]+)\s.*\s([0-9]+)/\1 \2/' | xargs echo`
        echo "Pairs: ${pairs}" >> ${sim_output}
        echo -e "Scores: ${scores}\n" >> ${sim_output}
        rm ${output}
     fi
    done 
  done
  for i in ${psweep} ; do
    rm -r "${strategy_dir}${i}"
    rm -r "${opponent_dir}${i}"
  done
}

GetStat(){
  value=`cat ${1} | grep -o "${2}:.*" | grep -o "[0-9]\{1,\}"`
  echo ${value}
}

GetPairs(){
  value=`cat ${sim_output} | grep -o "Pairs:.*" | grep -o "([0-9]\+,[0-9]\+) ([0-9]\+,[0-9]\+)"`
  echo ${value}
}

if [ "$#" -lt 2 ] ; then
  Usage
  exit 1
fi


while getopts go:d:s:h c; do
    case $c in
        g) # Just generate graphs
            nosim=1
            ;;
        s) # Set strategy
            strategy=$OPTARG
            strategy_dir="offset/${strategy}"
            ;;
        o) # Set opponent strategy
            opponent=$OPTARG
            opponent_dir="offset/${opponent}"
            ;;
	d) # Set offset size
            offset=$OPTARG
	    ;;
	h) # Help
	    Usage
	    ;;
        *)
            Usage
            ;;
    esac
done





if [ ${nosim} -eq 0 ] ; then

  # Make clean files where we'll store test results

  for i in `seq 1 1 10` ; do
    if [ -d ${strategy_dir}${i} ] ; then
      rm -r ${strategy_dir}${i}
    fi
    if [ -d ${opponent_dir}${i} ] ; then
      rm -r ${opponent_dir}${i}
    fi
  done

  # Main test loop
  if [ ${offset} -eq 0 ] ; then
    for d in ${dsweep} ; do
      echo "Running tests for d=${d}"
      offset=${d}
      sim_output="sim_runs/${strategy}_vs_${opponent}_${d}"
      > ${sim_output}
      RunSim 
    done
  else
    sim_output="sim_runs/${strategy}_vs_${opponent}_${offset}"
    > ${sim_output}
    RunSim
  fi
fi

nextnum(){
  expr match "${1}" '\([0-9]\+\)'
}

# Collect statistics
if [ ${offset} -eq 0 ] ; then
  output="data_points/${strategy}_vs_${opponent}"
  gnuplot_file="${output}.p"
  for d in ${dsweep} ; do
    sim_output="sim_runs/${strategy}_vs_${opponent}_${d}"
    stat_output="${output}_${d}.txt"
    > ${stat_output}
    echo "#Strategy: ${strategy}" > ${stat_output}
    echo "#Opponent: ${opponent}" >> ${stat_output}
    echo -e "#Player 1 Score   Player 2 Score\n" >> ${stat_output}

    pairs=`GetStat "${sim_output}" "Scores"`
    numpairs=( $pairs )
    numpairs=`expr ${#numpairs[@]} / 2 + 1` #Count number of score pairs
    for i in `seq 2 1 ${numpairs}` ; do 
      first=`nextnum ${pairs}`
      pairs=`echo ${pairs#${first}}`
      sec=`nextnum ${pairs}`
      pairs=`echo ${pairs#${sec}}`
      echo ${first} ${sec} >> ${stat_output}
    done

  done

  #Set up GNUPLOT file
  echo "set term wxt size 700, 450" > ${gnuplot_file}
  echo "unset label          #remove any previous labels" >> ${gnuplot_file}
  echo "set xrange [0:1000]" >> ${gnuplot_file}
  echo "set yrange [0:1000]" >> ${gnuplot_file}
  echo "set xtic nomirror auto       #set xtics automatically" >> ${gnuplot_file}
  echo "set ytic nomirror auto       #set ytics automatically" >> ${gnuplot_file}
  echo "betas = \" 8 10 15 19 \"" >> ${gnuplot_file}
  echo "lookup_beta(i) = word(betas,i)" >> ${gnuplot_file}
  echo "set multiplot" >> ${gnuplot_file}
  echo "set size 0.4,1" >> ${gnuplot_file}
  echo "set origin 0,0" >> ${gnuplot_file}
  echo "set title \"The Dependence of Player 1's Score on Player 2's Score\"" >> ${gnuplot_file}
  echo "set xlabel \"Player 2 Score\"" >> ${gnuplot_file}
  echo "set ylabel \"Player 1 Score\"" >> ${gnuplot_file}
  echo "set nox2tics" >> ${gnuplot_file}
  echo "set border 3" >> ${gnuplot_file}
  echo "point=1.5" >> ${gnuplot_file}
  echo "plot  for [i=1:4] '${strategy}_vs_${opponent}_'.lookup_beta(i).'.txt' using 1:2 title \"offset= \".lookup_beta(i) with points " >> ${gnuplot_file}

else
  sim_output="sim_runs/${strategy}_vs_${opponent}_${offset}"
  stat_output="data_points/${strategy}_vs_${opponent}_${offset}"
  gnuplot_file="${stat_output}.p"
  stat_output="${stat_output}.txt"
  > ${stat_output}

  echo "#Strategy: ${strategy}" > ${stat_output}
  echo "#Opponent: ${opponent}" >> ${stat_output}
  echo -e "#Player 1 Score   Player 2 Score\n" >> ${stat_output}

  pairs=`GetStat "${sim_output}" "Scores"`
  numpairs=( $pairs )
  numpairs=`expr ${#numpairs[@]} / 2 + 1` #Count number of score pairs
  for i in `seq 2 1 ${numpairs}` ; do 
    first=`nextnum ${pairs}`
    pairs=`echo ${pairs#${first}}`
    sec=`nextnum ${pairs}`
    pairs=`echo ${pairs#${sec}}`
    echo ${first} ${sec} >> ${stat_output}
  done
  echo "set term wxt size 700, 450" > ${gnuplot_file}
  echo "unset label          #remove any previous labels" >> ${gnuplot_file}
  echo "set xrange [0:1000]" >> ${gnuplot_file}
  echo "set yrange [0:1000]" >> ${gnuplot_file}
  echo "set xtic nomirror auto       #set xtics automatically" >> ${gnuplot_file}
  echo "set ytic nomirror auto       #set ytics automatically" >> ${gnuplot_file}
  echo "set title \"The Dependence of Player 1's Score on Player 2's Score\"" >> ${gnuplot_file}
  echo "set xlabel \"Player 2 Score\"" >> ${gnuplot_file}
  echo "set ylabel \"Player 1 Score\"" >> ${gnuplot_file}
  echo "point=1.5" >> ${gnuplot_file}
  echo "plot '${strategy}_vs_${opponent}_${offset}.txt' u 1:2 w points title 'Results'  " >> ${gnuplot_file}
fi

exit



#totaltitle=""
#for title in ${series} ; do
#  thistitle=`echo -e "\"${nseries} ${title}\" "`
#  totaltitle="${totaltitle}${thistitle}"
#done
#echo "${totaltitle}" >> ${stat_output}
#
#i=0
#j=0
#for value in ${x} ; do
#  stat_array[${i}]=${value}
#  i=`expr ${i} + 1`
#done
#for title in ${series} ; do
#  i=0
#  j=`expr ${j} + 1`
#  for value in ${x} ; do
#    avg_ticks=`nextnum ${ticks}`
#    ticks=`echo ${ticks#${avg_ticks}}`
#    stat_array[${i}]="${stat_array[${i}]} ${avg_ticks}"
#    i=`expr ${i} + 1`
#  done
#done
#for k in `seq 0 1 ${i}` ; do
#  echo ${stat_array[${k}]} >> ${stat_output}
#done
#
## Generate Gnuplot script for this data
#
##Index into gnuplot input file correctly
#j=`expr ${j} + 1`
#
