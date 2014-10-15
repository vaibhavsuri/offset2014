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
dsweep="5 9 14 20"

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
  if [ $((${offset}%2)) -eq 0 ] ; then
    firstp=`expr ${offset} / 2 - 1`
    firstq=`expr ${firstp} + 2 `
  else
    firstp=`expr ${offset} / 2`
    firstq=`expr ${firstp} + 1`
  fi
  secp=${2}
  secq=`expr ${offset} - ${secp}`
  if [ ${1} -eq 1 ] ; then
    myp=${firstp}
    myq=${firstq}
    herp=${secp}
    herq=${secq}
  else
    herp=${firstp}
    herq=${firstq}
    myp=${secp}
    myq=${secq}
  fi
}

ForkAndWait() {
  for i in ${psweep} ; do
    SetPQ ${1} ${i}
    if [ ! ${myp} -eq ${herp} -a ! ${myp} -eq ${herq} ] ; then
      output="result_${i}"
      echo "${myp}" "${myq}" "${herp}" "${herq}"
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

FixPlayerFile() {
  cp -r "${1}" "${1}${3}"
  newpackage="package offset.${2}${3};"
  for file in `ls "${1}${3}"` ; do 
    if [ -f "${1}${3}/${file}" ] ; then
      sed -i "1 s/^.*$/${newpackage}/" "${1}${3}/${file}"
    fi
  done

}

RunSim() {
  echo "Offset: $offset" >> ${sim_output}
  echo -e "Strategy: $strategy\n" >> ${sim_output}
  echo -e "Opponent: $opponent\n" >> ${sim_output}
  for i in ${psweep} ; do
    FixPlayerFile "${strategy_dir}" "${strategy}" "${i}"
    FixPlayerFile "${opponent_dir}" "${opponent}" "${i}"
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
    offset=0
  else
    sim_output="sim_runs/${strategy}_vs_${opponent}_${offset}"
    > ${sim_output}
    RunSim
  fi
fi

nextnum(){
  expr "${1}" : '\([0-9]\+\)'
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
  echo "betas = \" 5 9 14 20 \"" >> ${gnuplot_file}
  echo "lookup_beta(i) = word(betas,i)" >> ${gnuplot_file}
  echo "set title \"The Dependence of Player 1's Score on Player 2's Score\"" >> ${gnuplot_file}
  echo "set xlabel \"Player 2 (${opponent}) Score\"" >> ${gnuplot_file}
  echo "set ylabel \"Player 1 (${strategy}) Score\"" >> ${gnuplot_file}
  echo "set nox2tics" >> ${gnuplot_file}
  echo "set border 3" >> ${gnuplot_file}
  echo "point=1.5" >> ${gnuplot_file}
  echo "plot  for [i=1:4] '${strategy}_vs_${opponent}_'.lookup_beta(i).'.txt' using 2:1 title \"offset= \".lookup_beta(i) with points ,\\" >> ${gnuplot_file}
  echo "x w lines lc 'black'" >> ${gnuplot_file}

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
  echo "plot '${strategy}_vs_${opponent}_${offset}.txt' u 2:1 w points title 'Results'  " >> ${gnuplot_file}
fi

exit
