package plm.test.simple;

import java.io.BufferedWriter;

import plm.universe.Entity;

public class SimpleExerciseEntity extends Entity {

	@Override
	/* BEGIN TEMPLATE */
	public void run() throws Exception {
		/* BEGIN SOLUTION */
		System.out.println("Je fonctionne !");
		/* END SOLUTION */
	}
	/* END TEMPLATE */

	@Override
	public void command(String command, BufferedWriter out) {
	}
}
