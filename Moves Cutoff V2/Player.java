package offset.dumb1;

import java.util.*;

import offset.sim.Pair;
import offset.sim.Point;
import offset.sim.movePair;

public class Player extends offset.sim.Player {
	
	int size = 32;
	List<movePair> my_legal_moves = new ArrayList<>();
	List<movePair> opponent_legal_moves = new ArrayList<>();
	
	public void init() {
	}

	public Player(Pair prin, int idin) {
		super(prin, idin);
	}
	
	public void find_legal_moves(Point[] grid, Pair pr, Pair pr0)
	{
		movePair movepr = new movePair();
		my_legal_moves.clear();
		opponent_legal_moves.clear();
		
		for (int i = 0; i < size; ++i) 
		{
			for (int j = 0; j < size; ++j) 
			{
				for (int i_pr = 0; i_pr < size; ++i_pr) 
				{
					for (int j_pr = 0; j_pr < size; ++j_pr)
					{
						movepr.src = grid[i*size+j];
						movepr.target = grid[i_pr*size+j_pr];
						if(validateMove(movepr, pr))
							my_legal_moves.add(new movePair(true, movepr.src, movepr.target));
						if(validateMove(movepr, pr0))
							opponent_legal_moves.add(new movePair(true, movepr.src, movepr.target));
					}
				}
			}
		}
		
	}
	
	
	public boolean validateMove(movePair movepr, Pair pr) {
		Point src = movepr.src;
		Point target = movepr.target;
		boolean rightposition = false;

		if (Math.abs(target.x-src.x)==Math.abs(pr.p) && Math.abs(target.y-src.y)==Math.abs(pr.q)) {
			rightposition = true;
		}
		if (Math.abs(target.x-src.x)==Math.abs(pr.q) && Math.abs(target.y-src.y)==Math.abs(pr.p)) {
			rightposition = true;
		}
		if (rightposition && src.value == target.value && src.value > 0) 
			return true;
		
		return false;	
	}
	
	//Creating an imaginary grid, where we make a move and then return the imaginary grid
	public Point[] create_imag_grid(Point[] grid, movePair move)
	{
		Point[] imag_grid = new Point[size*size];
		//replicating the current grid to the imaginary grid
		for (int i = 0; i < size*size; i++)
		{
			Point temp = new Point();
			temp.x = grid[i].x;
			temp.y = grid[i].y;
			temp.change = grid[i].change;
			temp.owner = grid[i].owner;
			temp.value = grid[i].value;
			imag_grid[i] = temp;
		}
		//making the move in the imaginary grid
		Point src = imag_grid[move.src.x * size + move.src.y];
		Point target = imag_grid[move.target.x * size + move.target.y];
		src.value = 0;
		src.owner = -1;
		target.value = 2 * target.value;
		target.owner = id;
		target.change = true;
		
		return imag_grid;
	}
	

	//Counting the number of opponent moves you can cut off by making the move 'move'
	public int moves_cut_off(Point []grid, Pair pr, Pair pr0, movePair move)
	{
		opponent_legal_moves.clear();
		
		int moves_before = 0, moves_after = 0;
		find_legal_moves(grid, pr, pr0);
		moves_before = opponent_legal_moves.size();
		
		Point[] imag_grid = create_imag_grid(grid, move);
		opponent_legal_moves.clear();
		find_legal_moves(imag_grid, pr, pr0);
		moves_after = opponent_legal_moves.size();
		
		return (moves_before - moves_after);
	}
	
	//just a wrapper class to make life easier in the filter_cut_off method
	public class MovesWrapper
	{
		int index;
		int moves_cut_off;
	}
	
	//Given a list of moves 'all_moves', this returns the top N moves which cut off the most number of opponent moves
	public List<movePair> filter_cut_off(Point []grid, Pair pr, Pair pr0, List<movePair> all_moves)
	{
		int limit = 5; //sets the N for the top N
		int moves_cut_off = 0;
		List<movePair> top_moves = new ArrayList<>();
		List<MovesWrapper> temp_moves = new ArrayList<>();
		
		for (int i = 0; i < all_moves.size(); i++)
		{			
			//populating the temporary top N moves list with the first N moves
			if (temp_moves.size() < limit)
			{
				MovesWrapper x = new MovesWrapper();
				x.index = i;
				x.moves_cut_off = moves_cut_off(grid, pr, pr0, all_moves.get(i));
				temp_moves.add(x);
				continue;
			}
			
			//sorting the temporary top N moves
			Collections.sort(temp_moves, new Comparator<MovesWrapper>() {
				  public int compare(MovesWrapper c1, MovesWrapper c2) {
				    if (c1.moves_cut_off > c2.moves_cut_off) return -1;
				    if (c1.moves_cut_off < c2.moves_cut_off) return 1;
				    return 0;
				  }});
			
			moves_cut_off = moves_cut_off(grid, pr, pr0, all_moves.get(i));
			
			//evaluating this move against the worst move in the top N moves
			if (moves_cut_off > temp_moves.get(temp_moves.size()-1).moves_cut_off)
			{
				temp_moves.remove(temp_moves.size()-1);
				MovesWrapper x = new MovesWrapper();
				x.index = i;
				x.moves_cut_off = moves_cut_off;
				temp_moves.add(x);
			}
		}
		
		//copying the moves from the temporary list to the actual list which is to be returned
		for (int i = 0; i < temp_moves.size(); i++)
		{
			top_moves.add(all_moves.get(temp_moves.get(i).index));
			System.out.println("Moves cut off = "+temp_moves.get(i).moves_cut_off);
		}
		return top_moves;
	}

	public movePair move(Point[] grid, Pair pr, Pair pr0, ArrayList<ArrayList> history) 
	{
			//find all legal moves for both players
			find_legal_moves(grid,pr,pr0);
			System.out.println("Searching...be patient:)");
			//Generating the list of the best moves
			List<movePair> best_moves = filter_cut_off(grid,pr,pr0,my_legal_moves);
			movePair movepr = new movePair(true, best_moves.get(0).src, best_moves.get(0).target);							
			System.out.println("Chose move: "+movepr.src.x+", "+movepr.src.y+" to "+movepr.target.x+", "+movepr.target.y);
			return movepr;

	}
}