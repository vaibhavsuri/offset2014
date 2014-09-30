package offset.Greedy;

import java.util.*;

import offset.sim.Pair;
import offset.sim.Point;
import offset.sim.movePair;

public class Player extends offset.sim.Player {

	int size = 32;

	public Player(Pair prin, int idin) {
		super(prin, idin);
	}

	public void init() {
	}

	public movePair move(Point[] grid, Pair pr, Pair pr0, ArrayList<ArrayList> history) {
		movePair movepr = new movePair();
		movePair bestMovePr = new movePair();
		bestMovePr.move = false;
		int bestScore = 0;

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				for (int i_pr=0; i_pr<size; i_pr++) {
					for (int j_pr=0; j_pr <size; j_pr++) {
						movepr.x = grid[i*size+j];
						movepr.y = grid[size*i_pr+j_pr];
						int score = validateMove(movepr, pr);
						if (score > bestScore) {
							bestScore = score;
							bestMovePr.x = movepr.x; // deep copy
							bestMovePr.y = movepr.y;
						}
					}
				}
			}
		}

		if (bestScore > 0)
			bestMovePr.move = true;
		return bestMovePr;
	}


	int validateMove(movePair movepr, Pair pr) {
		Point src = movepr.x;
		Point target = movepr.y;
		boolean rightposition = false;

		if (Math.abs(target.x-src.x)==Math.abs(pr.x) && Math.abs(target.y-src.y)==Math.abs(pr.y)) {
			rightposition = true;
		}
		if (Math.abs(target.x-src.x)==Math.abs(pr.y) && Math.abs(target.y-src.y)==Math.abs(pr.x)) {
			rightposition = true;
		}
		if (rightposition && src.value == target.value && src.value >0) {
			return src.value;
		}
		else {
			return 0;
		}
	}
}
