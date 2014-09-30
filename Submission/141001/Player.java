package offset.group5;

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
		int[] bestScore = {0, 0};

		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < size; ++j) {
				for (int i_pr = 0; i_pr < size; ++i_pr) {
					for (int j_pr = 0; j_pr < size; ++j_pr) {
						movepr.src = grid[i*size+j];
						movepr.target = grid[i_pr*size+j_pr];
						int[] score = validateMove(movepr, pr);
						if (score[0] > bestScore[0] || (score[0] == bestScore[0] && score[1] > bestScore[1])) {
							bestScore[0] = score[0];
							bestScore[1] = score[1];
							bestMovePr.src = movepr.src; // deep copy
							bestMovePr.target = movepr.target;
						}
					}
				}
			}
		}

		if (bestScore[0] > 0)
			bestMovePr.move = true;
		return bestMovePr;
	}


	int[] validateMove(movePair movepr, Pair pr) {
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
			int[] score = new int[2];
			score[0] = src.value;
			score[1] = src.value + target.value;
			if (src.owner == id)
				score[1] -= src.value;
			if (target.owner == id)
				score[1] -= target.value;
			return score;
		}
		else {
			int[] score = {0, 0};
			return score;
		}
	}
}
