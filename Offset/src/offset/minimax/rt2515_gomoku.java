/*

	 Gomoku Agent Program
	 Richard Townsend
	 UNI: rt2515
	 Due 11-21-13
	 Artificial Intelligence, Columbia

 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashMap;
import java.awt.Point;
import java.util.Scanner;
import java.lang.Integer;

public class rt2515_gomoku{


	//Board Implementation
	public static class GBoard{
		//Constant for number of "lines" on which a chain can exist
		//There are four lines on a board: left to right, top to bottom, and the two diagonals
		private final static int LINE = 4;

		//Mapping of directions to indices in array
		private final static int U = 0;
		private final static int UR = 1;
		private final static int R = 2;
		private final static int DR = 3;
		private final static int D = 4;
		private final static int DL = 5;
		private final static int L = 6;
		private final static int UL = 7;


		//List of empty coordinates, coordinates with x's on them, those with o's on them, and
		//the current players list (either set to xlist or olist)
		private ArrayList<Point> empty;
		private ArrayList<Point> xlist;
		private ArrayList<Point> olist;
		private ArrayList<Point> plist;

		//Keep track of the longest chain starting from a coordinate in one of the 4 possible
		//orientations, where the nth orientation is the nth index in the value of a key-value pair.
		private HashMap<Point,int[]> xchain;
		private HashMap<Point,int[]> ochain;
		private HashMap<Point,int[]> pchain;

		//Game board dimension, winning chain length, time limit for a move, respectively
		private int dimension;
		private int winchain;
		private int timelim;

		//Empty Constructor
		public GBoard(){
			empty = new ArrayList<Point>();
			xlist = new ArrayList<Point>();
			olist = new ArrayList<Point>();
			xchain = new HashMap<Point,int[]>();
			ochain = new HashMap<Point,int[]>();
			winchain = timelim = 0;
		}

		//Constructor for start of game
		public GBoard(int dim, int chain, int time){
			empty = buildBoard(dim);
			xlist = new ArrayList<Point>();
			olist = new ArrayList<Point>();
			xchain = new HashMap<Point,int[]>();
			ochain = new HashMap<Point,int[]>();
			dimension = dim;
			winchain = chain;
			timelim = time;
		}

		//Create a board that depicts a snapshot of a game
		public GBoard(int d, int c, int t, ArrayList<Point> e, ArrayList<Point> xl, ArrayList<Point> ol,
				HashMap<Point, int[]> xc, HashMap<Point, int[]> oc){
			dimension = d;
			winchain = c;
			timelim = t;
			empty = e;
			xlist = xl;
			olist = ol;
			xchain = xc;
			ochain = oc;
		}

		//Copy this board such that changes made to copy don't change this board
		public GBoard copy(){
			ArrayList<Point> e = new ArrayList<Point>(empty);
			ArrayList<Point> xl = new ArrayList<Point>(xlist);
			ArrayList<Point> ol = new ArrayList<Point>(olist);
			HashMap<Point, int[]> xc = new HashMap<Point, int[]>();
			xc.putAll(xchain);
			Iterator<Point> iter = xc.keySet().iterator();
			while (iter.hasNext()){
				Point spot = iter.next();
				xc.put(spot, Arrays.copyOf(xc.get(spot), xc.get(spot).length));
			}
			HashMap<Point, int[]> oc = new HashMap<Point, int[]>();
			oc.putAll(ochain);
			iter = oc.keySet().iterator();
			while (iter.hasNext()){
				Point spot = iter.next();
				oc.put(spot, Arrays.copyOf(oc.get(spot), oc.get(spot).length));
			}
			return new GBoard(dimension, winchain, timelim, e, xl, ol, xc, oc);
		}

		//Construct empty dim x dim game board
		private ArrayList<Point> buildBoard(int dim){
			ArrayList<Point> board = new ArrayList<Point>(dim*dim);
			for (int x = 0; x < dim; x++){
				for (int y = 0; y < dim; y++){
					board.add(new Point(x, y));
				}
			}
			return board;
		}

		//Set the current player (who gets to move)
		public void setPlayer(char player){
			if (player == 'x'){
				plist = xlist;
				pchain = xchain;
			}
			else{
				plist = olist;
				pchain = ochain;
			}
		}

		public char getPlayer(){
			if (plist == xlist)
				return 'x';
			return 'o';
		}

		//Returns true if this is a legal move
		public boolean legalMove(Point move){
			return empty.contains(move);
		}

		//Get a list of all possible moves on this board 
		public Point[] getMoves(){
			Point[] emptyspots = new Point[0];
			return empty.toArray(emptyspots);
		}

		//Get locations of current player's game pieces
		public Point[] getPieces(){
			Point[] playerspots = new Point[0];
			return plist.toArray(playerspots);
		}

		//Test if this board is a winner for current player
		public boolean isGoal(){
			int[] value = new int[8];
			Iterator<Point> iter = pchain.keySet().iterator();
			while(iter.hasNext()){
				Point t = iter.next();
				value = pchain.get(t);
				for(int j = 0; j < value.length; j++){
					if (value[j] == (winchain-1))
						return true;
				}
			}
			return false;
		}

		//Update the board with current player's move, returning True if update was successful
		public boolean makeMove(Point spot){
			if (empty.remove(spot) == false || xlist.contains(spot) || olist.contains(spot))
				return false;
			plist.add(spot);
			updateChains(spot);
			return true;
		}

		//Get current player's score in terms of an evaluation function
		public Double getScore(){
			if (pchain == xchain){
				return calcScore(xchain) - calcScore(ochain);
			}
			return calcScore(ochain) - calcScore(xchain);
		}

		//Calculate score for a chain list
		public Double calcScore(HashMap<Point, int[]> chain){
			double score = 0.0;
			double tscore = 0.0;
			int[] value = new int[8];
			int open = 0;
			HashMap<Point, int[]> temp = new HashMap<Point, int[]>();
			Iterator<Point> iter = chain.keySet().iterator();
			while (iter.hasNext()){
				Point spot = iter.next();
				temp.put(spot, Arrays.copyOf(chain.get(spot), chain.get(spot).length));
			}
			iter = temp.keySet().iterator();
			while(iter.hasNext()){
				Point t = iter.next();
				value = temp.get(t);
				//Assign scores to each chain that end at this point
				for (int j = 0; j < value.length; j++){
					if (value[j] ==  winchain-1)
						tscore = winchain * 100000.0;
					else if (value[j] == winchain-2)
						tscore = (winchain-1) * 1000.0;
					else if (value[j] == winchain-3)
						tscore = (winchain-2) * 100.0;
					else if (value[j] ==  winchain-4)
						tscore = (winchain-3) * 3.0;
					else if (value[j] < winchain-4 && value[j] >= 1)
						tscore = value[j] * 2.0;

					if (value[j] > 0){
						//Only add to score if chain is fully open or half-open
						open = isOpen(t, j, value[j]);
						if (open == 1 || value[j] == winchain-1)
							score += tscore;
						else if (open == 0)
							score += tscore/2.0;
						//Remove other endpoint from temporary chain list so we don't double count
						Point endpt = getEndPoint(t, value[j], j);
						if (endpt.getX() >= 0 && endpt.getY() >=0){
							int[] endpttemp = java.util.Arrays.copyOf(chain.get(endpt), chain.get(endpt).length);
							endpttemp[(j+LINE)%8] = 0;
							temp.put(endpt, endpttemp);
						}
					}

					//reset for next count
					tscore = 0.0;
				}
			}
			return score;
		}

		//Print out graphical representation of board
		public String toString(){
			Point spot = new Point();
			String result = "";
			char c = ' ';
			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
					spot.setLocation(i,j);
					if (empty.contains(spot))
						c = '.';
					else if (xlist.contains(spot))
						c = 'x';
					else
						c = 'o';
					result = result + c + " ";
				}
				result = result + "\n";
			}
			return result;
		}

		//Print out all chains for current player
		public String chainToString(){
			int[] value = new int[8];
			Iterator<Point> iter = pchain.keySet().iterator();
			String result = "Current player's chains: ";
			while(iter.hasNext()){
				Point t = iter.next();
				result += t +" ";
				value = pchain.get(t);
				for(int j = 0; j < value.length; j++){
					result += value[j] + ", ";
				}
				result += "\n";
			}
			return result;
		}

		/*-------------------------
			-	Private board methods.  -
			-------------------------*/

		//Update a player's chain map after a move has been made
		private void updateChains(Point spot){

			int[] newlist = new int[8];

			//initialize new list
			for (int i = 0; i < newlist.length; i++)
				newlist[i] = 0;

			//Update each "line"
			for (int i = 0; i < LINE; i++){
				newlist = updateLine(newlist, i, spot);
			}
			if (!allZeros(newlist))
				pchain.put(spot, newlist);
		}

		//Update current player's chainlist on line in direction curline, where
		//latest move occurred on point spot
		private int[] updateLine(int[] newlist, int curline, Point spot){

			Point one, two;
			one = new Point(spot);
			two = new Point(spot);

			switch(curline){
				case U:one.translate(-1,0);
							 two.translate(1,0);
							 break;
				case UR:	one.translate(-1,1);
									two.translate(1,-1);
									break;
				case R:	one.translate(0,1);
								two.translate(0,-1);
								break;
				default: one.translate(1,1);
								 two.translate(-1,-1);
								 break;
			}
			if (plist.contains(one) && plist.contains(two)){
				doubleMod(one, curline+LINE, two, curline);
			}
			else if (plist.contains(one)){
				//Point one is in direction curline with respect to spot
				return singleMod(newlist, one, spot, curline, curline+LINE);
			}
			else if (plist.contains(two)){
				//Point two is in direction curline+LINE with respect to spot
				return singleMod(newlist, two, spot, curline+LINE, curline);
			}
			else{
				newlist[curline] = 0;
				newlist[(curline+LINE) % 8] = 0;
			}
			return newlist;
		}


		//Modify the chain lists for two points, as the latest move connected two
		//chains into one large chain.
		//di points from pi to pj, i != j
		private void doubleMod(Point p1, int d1, Point p2, int d2){
			int[] chain1, chain2;
			Point endpt1, endpt2;

			endpt1 = new Point(-1,-1);
			endpt2 = new Point(-1,-1);

			//See if pchain has p1 or p2
			chain1 = pchain.get(p1);
			chain2 = pchain.get(p2);

			//If p1 in pchain, get endpoint other endpoint of its chain in direction d2, if it exists
			if (chain1 != null && chain1[d2] > 0)
				endpt1 = getEndPoint(p1, chain1[d2], d2);
			//Same for p2 in direction d1
			if (chain2 != null && chain2[d1] > 0)
				endpt2 = getEndPoint(p2, chain2[d1], d1);

			//Both p1 and p2 are in pchain and each pi has chain in direction dj, j != i
			if (!endpt1.equals(new Point(-1,-1)) && !endpt2.equals(new Point(-1,-1))){
				int x1, x2;
				x1 = chain1[d2];
				x2 = chain2[d1];
				pchain.get(endpt1)[d1] = x1 + x2 + 2;
				pchain.get(endpt2)[d2] = x1 + x2 + 2;
				chain1[d2] = 0;
				if (allZeros(chain1))
					pchain.remove(p1);
				chain2[d1] = 0;
				if (allZeros(chain2))
					pchain.remove(p2);
			}
			//Check if p1 in pchain and it has a chain in direction d2
			else if(!endpt1.equals(new Point(-1,-1))){
				if (chain2 == null){
					chain2 = new int[8];
				}
				chain2[d2] = chain1[d2] + 2;
				pchain.get(endpt1)[d1] = chain2[d2];
				pchain.put(p2, chain2);
				chain1[d2] = 0;
				if (allZeros(chain1))
					pchain.remove(p1);
			}
			//Check same for p2 (probably put p1 stuff in method and call that)
			else if(!endpt2.equals(new Point(-1,-1))){
				if (chain1 == null){
					chain1 = new int[8];
				}
				chain1[d1] = chain2[d1] + 2;
				pchain.get(endpt2)[d2] = chain1[d1];
				pchain.put(p1, chain1);
				chain2[d1] = 0;
				if (allZeros(chain2))
					pchain.remove(p2);
			}
			//p1 doesn't have a chain in direction d2 and p2 doesn't have one in direction d1
			else{
				if (chain1 == null)
					chain1 = new int[8];
				if (chain2 == null)
					chain2 = new int[8];
				chain1[d1] = 2;
				chain2[d2] = 2;
				pchain.put(p1, chain1);
				pchain.put(p2, chain2);
			}

		}

		//Modify chainlist as adding latest move either created a chain or extended one
		private int[] singleMod(int[] newlist, Point other, Point current, int linedir, int neglinedir){

			int[] temp = new int[8];
			int count = 0;
			Point farEndPoint = new Point();

			if (pchain.containsKey(other)){
				temp = pchain.get(other);
				if (pchain.get(other)[linedir] > 0){
					count = temp[linedir] + 1;//extend length of chain by one

					//Update other's chain count
					temp[linedir] = 0;//This coord is now inside of linedir-directed chain, instead of being an endpoint
					if (allZeros(temp))
						pchain.remove(other);//Remove if coord is not an endpoint for any chain

					//Update other endpoint of other's chain
					farEndPoint = getEndPoint(current, count, linedir);
					temp = pchain.get(farEndPoint);
					temp[neglinedir] = count;

					//update current's chain count
					newlist[linedir] = count;
					pchain.put(current, newlist);
					return newlist;
				}
			}
			//otherwise other wasn't an endpoint for a chain in direction linedir
			newlist[linedir] = 1;
			temp[neglinedir] = 1;
			pchain.put(current, newlist);
			pchain.put(other, temp);

			return newlist;
		}

		//Current is an endpoint of a chain of length count leaving current
		//in direction specified by linedir.
		//Returns: other endpoint of this chain.
		private Point getEndPoint(Point current, int count, int linedir){
			Point endpoint = new Point(current);
			switch(linedir){
				case U: endpoint.translate(count*-1,0);
								break;
				case UR: endpoint.translate(count*-1,count);
								 break;
				case R: endpoint.translate(0, count);
								break;
				case DR: endpoint.translate(count,count);
								 break;
				case D: endpoint.translate(count,0);
								break;
				case DL:  endpoint.translate(count, count*-1);
									break;
				case L: endpoint.translate(0,count*-1);
								break;
				default: endpoint.translate(count*-1, count*-1);
								 break;
			}
			return endpoint;
		}

		//Check if a chain list for a point is all zeros
		private boolean allZeros(int[] list){
			for (int i = 0; i < list.length; i++){
				if (list[i] != 0){
					return false;
				}
			}
			return true;
		}

		//Check if a chain along a line is "open" on both ends
		//Returns: 1 if chain is fully open
		//				 0 if chain is half open
		//			  -1 if chain is closed
		private int isOpen(Point t, int linedir, int length){
			Point p1, p2;//points on board that we'll check
			p1 = getEndPoint(t, 1, (linedir + LINE) % 8);
			p2 = getEndPoint(t, length+1, linedir);
			if (empty.contains(p1) && empty.contains(p2))
				return 1;
			if (empty.contains(p1) || empty.contains(p2))
				return 0;
			return -1;
		}

	}


	//++++++++++++++++++++++++++++//
	//Actual Gomoku Agent is Below//
	//****************************//

	//Game parameters
	private static int dim, chain, time;

	//Timelimit
	private static double timelimit, timestart;
	private static int pruned;

	//Mini-class to represent a move in minimax
	public static class Move{

		private double score;
		private Point move;

		public Move(){
			score = 0.0;
			move = new Point();
		}
		public Move(double s, Point m){
			score = s;
			move = m;
		}

		public double getScore(){
			return score;
		}

		public Point getPoint(){
			return move;
		}

		public Move getMove(){
			return this;
		}

	}

	//Implementation of alpha-beta minimax
	public static Move bestMove(GBoard board, int depth, double mybest, double herbest){
		Point[] movelist;
		GBoard tempboard = new GBoard();
		Point lastMove;
		double bestscore = 0.0;
		Point bestm = new Point();
		Move tempmove = new Move();
		double tempscore = 0.0;
		double divtime = 0.0;

		if (depth == 0){
			return new Move(board.getScore(), null);
		}

		movelist = Arrays.copyOf(board.getMoves(), board.getMoves().length);


		bestscore = mybest;
		bestm = null;

		if (movelist.length == 0)
			bestscore = board.getScore();


		for (int i = 0; i < movelist.length; i++){
			//Timing is enforced here
			if (depth == 6){
				timestart = (double)System.currentTimeMillis();
			}
			else if (depth < 6){
				if ((double)System.currentTimeMillis() - timestart > timelimit){
					return new Move(board.getScore(), null);
				}
			}

			//Make copy of board for this recursive call
			tempboard = board.copy();
			lastMove = new Point(movelist[i]);

			//Need to set which player is moving
			tempboard.setPlayer(board.getPlayer());
			tempboard.makeMove(lastMove);
			if (board.getPlayer() == 'x')
				tempboard.setPlayer('o');
			else
				tempboard.setPlayer('x');
			tempmove = bestMove(tempboard, depth-1, -1*herbest, -1*bestscore);//recursive call!
			tempboard.setPlayer(board.getPlayer());

			tempscore = -1 * tempmove.getScore();

			if (tempscore > bestscore){
				bestscore = tempscore;
				bestm = lastMove;
			}

			if (bestscore > herbest){
				return new Move(bestscore, bestm);
			}
		}

		return new Move(bestscore, bestm);
	}


	//Given a board, CPU either uses alpha-beta or randomness to select a move
	public static GBoard cpuMove(GBoard board, char pl, int mode){
		pruned = 0;
		board.setPlayer(pl);
		Point cpuMove = new Point();
		if (mode == 0){
			//get timelimit for subtrees
			timelimit = ((time*1000))/(double)(board.getMoves().length);
			cpuMove = bestMove(board, 6,  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).getPoint();//Trying 4 ply?
		}
		else{
			Point[] moves = board.getMoves();
			int moveindex =(int)(Math.random() * moves.length);
			cpuMove = moves[moveindex];
		}
		if (null == cpuMove){
			System.out.println("No more moves available!");
			return null;
		}
		board.makeMove(cpuMove);
		System.out.println(board);
		System.out.println("The computer placed a piece at point ["+(int)cpuMove.getX()+" "+(int)cpuMove.getY()+"] ");
		if (board.isGoal()){
			System.out.println("Player " + board.getPlayer() + " wins after a total of "+board.getPieces().length+" moves!");
			return null;
		}
		return board;
	}

	//Carry out a human player's move 
	public static GBoard playerMove(GBoard board, char pl){
		Scanner input = new Scanner(System.in);

		board.setPlayer(pl);
		String playerMove;
		System.out.print("Player move: ");
		playerMove = input.next() + " " + input.next();
		Point attempt = new Point(new Integer(playerMove.split(" ")[0]), new Integer(playerMove.split(" ")[1]));
		while (!board.legalMove(attempt)){
			System.out.print("Invalid move, please select another: ");
			playerMove = input.next() + " " + input.next();
			attempt = new Point(new Integer(playerMove.split(" ")[0]), new Integer(playerMove.split(" ")[1]));
		}
		board.makeMove(new Point(new Integer(playerMove.split(" ")[0]), new Integer(playerMove.split(" ")[1])));
		System.out.println(board);
		if (board.isGoal()){
			System.out.println("Player " + board.getPlayer() + " wins after a total of "+board.getPieces().length+" moves!");
			return null;
		}
		return board;
	}

	public static void main(String[] args){

		//Ensure correct number of args
		if (args.length != 3){
			System.out.println("Usage: java Gomoku DIMENSION CHAIN TIME");
			System.out.println("DIMENSION: game board size");
			System.out.println("CHAIN: winning chain lenght");
			System.out.println("TIME: move selection time limit	(in seconds)");
			return;
		}

		//Parse command-line arguments
		for (int i = 0; i < args.length; i++){
			try{
				int x = Integer.parseInt(args[i]);
				switch(i){
					case 0: dim = x;
									break;
					case 1: chain = x;
									break;
					case 2: time = x;
									break;
					default: System.out.println("Invalid number of arguments");
									 break;
				}
			}
			catch(NumberFormatException e){
				System.err.println("Argument " + (i+1) + " must be an integer");
				return;
			}
		}

		//Get game mode from user 
		Scanner input = new Scanner(System.in);
		int mode, player;
		mode = player = 4;
		while (mode < 1 || mode > 3){
			System.out.println("Which mode would you like?");
			System.out.println("1. Interactive play");
			System.out.println("2. Optimal agent vs Random agent");
			System.out.println("3. Optimal agent vs Optimal agent");
			System.out.print("Selection (1-3): ");
			mode = input.nextInt();
		}


		//Set who plays first
		if (mode != 3){
			while (player < 0 || player > 1){
				System.out.println("Should the  optimal agent play as x or o?");
				System.out.print("Selection (1 for x, 0 for o): ");
				player = input.nextInt();
			}
		}

		//Create empty game board with given parameters
		GBoard board = new GBoard(dim, chain, time);

		//Play game!
		switch(mode){
			case 1: if (player == 1){
								while (!((board = cpuMove(board, 'x', 0)) == null)){
									if ((board = playerMove(board, 'o')) == null)
										break;
								}
							}
							else{
								while (!((board = playerMove(board, 'x')) == null)){
									if ((board = cpuMove(board, 'o', 0)) == null)
										break;
								}
							}
							break;
			case 2: if (player == 1){
								while (!((board = cpuMove(board, 'x', 0)) == null)){
									if ((board = cpuMove(board, 'o', 1)) == null)
										break;
								}
							}
							else{
								while (!((board = cpuMove(board, 'x', 1)) == null)){
									if ((board = cpuMove(board, 'o', 0)) == null)
										break;
								}
							}
							break;
			case 3:	while (!((board = cpuMove(board, 'x', 0)) == null)){
								if ((board = cpuMove(board, 'o', 0)) == null)
									break;
							}
							break;
		}

	}

}
