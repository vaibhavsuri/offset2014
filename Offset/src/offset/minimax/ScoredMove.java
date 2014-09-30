
package offset.minimax;

import java.util.*;

import offset.sim.Pair;
import offset.sim.Point;
import offset.sim.movePair;

//Object to represent a move and the resulting "score" of the board
//after making that move
public class ScoredMove{

  private double score;
  private movePair move;

  public ScoredMove(){
    score = 0.0;
    move = new movePair();
  }
  public ScoredMove(double s, movePair m){
    score = s;
    move = m;
  }

  public double getScore(){
    return score;
  }

  public movePair getMovePair(){
    return move;
  }

  public ScoredMove getScoredMove(){
    return this;
  }

}
