package offset.minimax;

import java.util.*;

import offset.sim.Pair;
import offset.sim.Point;
import offset.sim.movePair;

public class Player extends offset.sim.Player {

  int size = 32;
  int myid;
  int minimaxID;
  ArrayList<Point> minimaxGrid;

  final int SEARCH_DEPTH = 2;//Depth of alpha-beta search
  final int TIME_OUT = 1;//Vary this number to set time limit for subtree searching
  static boolean ISLATER = false;


  static double timelimit, timestart;

  public Player(Pair prin, int idin) {
    super(prin, idin);
    myid = idin;
  }

  public void init() {
  }

  public movePair move(Point[] grid, Pair pr, Pair pr0, ArrayList<ArrayList> history) {
    movePair movepr = new movePair();
    movePair bestMovePr = new movePair();
    bestMovePr.move = false;
    int bestScore = 0;

    //BROUGHT OVER FROM GOMOKU
    //get timelimit for subtrees
    minimaxID = myid;
    timelimit = ((TIME_OUT*1000))/(double)(getValidMoves(pr,grid).size());
    
    if (getValidMoves(pr,grid).size() < 5000){
      ISLATER = true;
      bestMovePr = bestMove(copyGrid(grid), SEARCH_DEPTH, pr, pr0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).getMovePair();
      if (bestMovePr != null){
        bestMovePr.move = true;
        return bestMovePr;
      }
    }
    else{

      for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
          for (int i_pr=0; i_pr<size; i_pr++) {
            for (int j_pr=0; j_pr <size; j_pr++) {
              movepr.src = grid[i*size+j];
              movepr.target = grid[size*i_pr+j_pr];
              if (validateMove(movepr, pr)) {
                      movepr.move = true;
                      return movepr;
              }
            }
          }
        }
      }

    }
    return bestMovePr;
  }

  boolean validateMove(movePair movepr, Pair pr) {
    	
    	Point src = movepr.src;
    	Point target = movepr.target;
    	boolean rightposition = false;
    	if (Math.abs(target.x-src.x)==Math.abs(pr.p) && Math.abs(target.y-src.y)==Math.abs(pr.q)) {
    		rightposition = true;
    	}
    	if (Math.abs(target.x-src.x)==Math.abs(pr.p) && Math.abs(target.y-src.y)==Math.abs(pr.q)) {
    		rightposition = true;
    	}
        if (rightposition && src.value == target.value && src.value >0) {
        	return true;
        }
        else {
        	return false;
        }
    }

  //Implementation of alpha-beta minimax
  protected ScoredMove bestMove(Point[] grid, int depth, Pair myPair, Pair herPair, double mybest, double herbest){
    ArrayList<movePair> moveList = new ArrayList<movePair>();
    Point[] tempGrid = new Point[grid.length];
    movePair lastMove;
    double bestscore = 0.0;
    movePair bestMove = new movePair();
    ScoredMove tempmove = new ScoredMove();
    double tempscore = 0.0;
    double divtime = 0.0;

    double timebefore, timeafter;

    //Score for this player on current board
    int thisBoardScore = calculateScore(minimaxID,grid);

    //The id of this player
    int playerID = minimaxID;

    //Make copy of board for this recursive call
    tempGrid = copyGrid(grid);

    //End recursion at bottom of search space.
    //Just return the score since we don't choose a move at this level.
    if (depth == 0){
      return new ScoredMove(thisBoardScore, null);
    }

    //Get set of valid moves for this player
    moveList = getValidMoves(myPair,tempGrid);

    //Set alpha-beta params
    bestscore = mybest;
    bestMove = null;

    //Iterate over movelist until we find the best one
    timebefore = (double)System.currentTimeMillis();
    for (int i = 0; i < moveList.size(); i++){

      //Timing is enforced here so we don't spend to long searching a subtree
      if (depth == SEARCH_DEPTH){
        timestart = (double)System.currentTimeMillis();
      }
      else if (depth < SEARCH_DEPTH){
        if ((double)System.currentTimeMillis() - timestart > timelimit){
          return new ScoredMove(thisBoardScore, null);
        }
      }


      //Choose the next move to try
      movePair chosenMove = moveList.get(i);

      //Make the chosen move on our copied grid
      Point[] newTempGrid = makeHypMove(minimaxID,tempGrid,chosenMove);

      //Need to set which player is moving next
      if (minimaxID == 1)
        minimaxID = 0;
      else
        minimaxID = 1;

      //Recursively call bestMove to determine what opponent will do, negating the alpha-beta params
      //so they now pertain to the opponent
      tempmove = bestMove(newTempGrid, depth-1, herPair, myPair, -1*herbest, -1*bestscore);

      //Set minimaxID back to the current player
      minimaxID = playerID;

      //Negate opponent's best score
      tempscore = -1 * tempmove.getScore();


      //check if opponent's best score is lower than scores we found
      //previously (we do > because we negated their score)
      if (tempscore > bestscore){
        bestscore = tempscore;
        bestMove = chosenMove;
      }

      //Here's where the "pruning" happens -- return a move if the alpha-beta window shuts
      if (bestscore > herbest){
        return new ScoredMove(bestscore, bestMove);
      }
    }
    timeafter= (double)System.currentTimeMillis();


    //We checked every possible move, so return the best one
    return new ScoredMove(bestscore, bestMove);
  }

  //Given a board and a player id, calculate the score of that board for that player
  int calculateScore(int id, Point[] grid) {
    int score = 0;
    for (int i=0; i<size; i++) {
      for (int j =0; j<size; j++) {
        if (grid[i*size+j].owner == id) {
          score = score+grid[i*size+j].value;
        }
      }
    }
    return score;
  }

  //Given a (p,q) pair and a board, return all allowable moves for that
  //pair on that board.
  ArrayList<movePair> getValidMoves(Pair myPair, Point[] grid){
    movePair movepr = new movePair();
    movepr.move = false;
    ArrayList<movePair> moveList = new ArrayList<movePair>();
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < size; j++) {
        for (int i_pr=0; i_pr<size; i_pr++) {
          for (int j_pr=0; j_pr <size; j_pr++) {
            movepr.src = grid[i*size+j];
            movepr.target = grid[size*i_pr+j_pr];
            if (validateMove(movepr,myPair)) {
              moveList.add(movepr);
            }
          }
        }
      }
    }

    return moveList;
  }

  //Given a board and a hypothetical move, update the grid to take that move into account.
  Point[] makeHypMove(int id, Point[] grid, movePair movepr){
    Point[] newGrid = copyGrid(grid);
    Point src = new Point();
    Point target = new Point();
    int srcSpot, targetSpot;
    srcSpot = -1;
    targetSpot = -1;
    for (int i = 0; i < grid.length; i ++){
      if (newGrid[i].x == movepr.src.x && newGrid[i].y == movepr.src.y){
       src = new Point(movepr.src.x,movepr.src.y,movepr.src.value,movepr.src.owner); 
       srcSpot = i;
      }
      if (newGrid[i].x == movepr.target.x && newGrid[i].y == movepr.target.y){
       target = new Point(movepr.target.x,movepr.target.y,movepr.target.value,movepr.target.owner); 
       targetSpot = i;
      } 
    }
    target.value = target.value+src.value;
    src.value = 0;
    target.owner = id;
    src.owner = -1;
    src.change = true;
    target.change = true;
    newGrid[srcSpot] = src;
    newGrid[targetSpot] = target;
    return newGrid;
  }

  //Make copy of grid contents
  Point[] copyGrid (Point[] grid){
    Point[] newGrid = new Point[grid.length];
    for (int i = 0; i < grid.length; i++){
      newGrid[i] = new Point(grid[i].x,grid[i].y,grid[i].value,grid[i].owner);
    }
    return newGrid;
  }
}
