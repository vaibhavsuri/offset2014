package offset.hierarchy;

import java.util.*;

import offset.sim.Pair;
import offset.sim.Point;
import offset.sim.movePair;

public class Player extends offset.sim.Player {

  int size = 32;

  //Constants for representing steal scores
  final int STEAL_BOTH = 3;
  final int STEAL_ONE  = 2;
  final int STEAL_NONE = 1;

  public Player(Pair prin, int idin) {
    super(prin, idin);
  }

  public void init() {
  }

  public movePair move(Point[] grid, Pair pr, Pair pr0, ArrayList<ArrayList> history) {
    movePair movepr = new movePair();
    movePair bestMovePr = new movePair();
    bestMovePr.move = false;
    int[] bestScore = {0, 0, 0};

    movePair[] moves = getBestStealMoves(grid, pr); //get best moves in terms of stealing from opponent
    for (int i = 0; i < moves.length; i++) {
      int[] score = scoreMove(moves[i]);
      if (validateMove(moves[i],pr)){
        if (score[0] > bestScore[0] || (score[0] == bestScore[0] && score[1] > bestScore[1])) {
          bestScore[0] = score[0];
          bestScore[1] = score[1];
          bestMovePr.src = moves[i].src; // deep copy
          bestMovePr.target = moves[i].target;
        }
      }
    }

    if (bestScore[0] > 0)
      bestMovePr.move = true;
    return bestMovePr;
  }


  //Check if a move is legal for a given player
  boolean validateMove(movePair movepr, Pair pr) {
    Point src = movepr.src;
    Point target = movepr.target;
    boolean rightposition = false;

    if (Math.abs(target.x-src.x)==Math.abs(pr.p) && Math.abs(target.y-src.y)==Math.abs(pr.q)) {
      rightposition = true;
    }
    if (Math.abs(target.x-src.x)==Math.abs(pr.q) && Math.abs(target.y-src.y)==Math.abs(pr.p)) {
      rightposition = true;
    }
    if (rightposition && src.value == target.value && src.value > 0) {
      return true;
    }
    return false;
  }

  //Return the score of a move based on the value we would get
  //from making that move.
  int[] scoreMove(movePair movepr) {
    Point src = movepr.src;
    Point target = movepr.target;
    int[] score = new int[2];
    score[0] = src.value;
    score[1] = src.value + target.value;
    if (src.owner == id)
      score[1] -= src.value;
    if (target.owner == id)
      score[1] -= target.value;
    return score;
  }

  //Given a move, return it's score based on how many tiles it steals
  int getStealScore(movePair movepr) {
    Point src = movepr.src;
    Point target = movepr.target;
    if(src.value == 1) //TODO: How should we score the 1's case?
      return STEAL_NONE;
    if(src.owner != id && target.owner != id)
      return STEAL_BOTH;
    if(src.owner != id || target.owner != id)
      return STEAL_ONE;
    else
      return STEAL_NONE;


  }

  /*
  //Return the set of moves with the highest steal score.
  //We store all the moves with the highest score in an arraylist
  //until a move with a higher score is found, in which case we empty
  //the list and proceed to only collect moves with that higher score.
  */
  movePair[] getBestStealMoves(Point[] grid, Pair pr){
    movePair movepr = new movePair();
    ArrayList<movePair> bestMoves = new ArrayList<movePair>();
    int highscore = 0;
    int tempscore = 0;
    for (int i = size-1; i >= 0; i--) {
      for (int j = size-1; j >= 0; j--) {
        for (int i_pr = size-1; i_pr >= 0; i_pr--) {
          for (int j_pr = size-1; j_pr >= 0; j_pr--) {
            movepr.src = grid[i*size+j];
            movepr.target = grid[i_pr*size+j_pr];
            if (validateMove(movepr, pr)){
              tempscore = getStealScore(movepr);
              //If we find a higher scoring move, flush arraylist and only collect
              //moves with that score
              if (tempscore > highscore) {
                bestMoves.clear();
                highscore = tempscore;
              }
              if (tempscore == highscore){
                bestMoves.add(movepr);
              }
              movepr = new movePair();
            }
          }
        }
      }
    }
    return bestMoves.toArray(new movePair[0]);
  }
}
