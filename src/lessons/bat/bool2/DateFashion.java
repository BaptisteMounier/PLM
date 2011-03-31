/* automatically converted from the Nick Parlante's excellent exercising site http://javabat.com/ */

package lessons.bat.bool2;
import jlm.core.model.lesson.Lesson;
import jlm.universe.World;
import jlm.universe.bat.BatExercise;
import jlm.universe.bat.BatWorld;

public class DateFashion extends BatExercise {
  public DateFashion(Lesson lesson) {
    super(lesson);
    
    World[] myWorlds = new BatWorld[12];
    myWorlds[0] = new BatWorld(VISIBLE, 5, 10) ;
    myWorlds[1] = new BatWorld(VISIBLE, 5, 2) ;
    myWorlds[2] = new BatWorld(VISIBLE, 5, 5) ;
    myWorlds[3] = new BatWorld(INVISIBLE, 3, 3) ;
    myWorlds[4] = new BatWorld(INVISIBLE, 10, 2) ;
    myWorlds[5] = new BatWorld(INVISIBLE, 2, 9) ;
    myWorlds[6] = new BatWorld(INVISIBLE, 9, 9) ;
    myWorlds[7] = new BatWorld(INVISIBLE, 10, 5) ;
    myWorlds[8] = new BatWorld(INVISIBLE, 2, 2) ;
    myWorlds[9] = new BatWorld(INVISIBLE, 3, 7) ;
    myWorlds[10] = new BatWorld(INVISIBLE, 2, 7) ;
    myWorlds[11] = new BatWorld(INVISIBLE, 6, 2) ;

    setup(myWorlds,"dateFashion");
  }

  /* BEGIN SKEL */
  public void run(World w) {
    BatWorld bw = (BatWorld) w;
    bw.result = dateFashion((Integer)w.getParameter(0), (Integer)w.getParameter(1));
  }
  /* END SKEL */

  /* BEGIN TEMPLATE */
int dateFashion(int you, int date) {
  /* BEGIN SOLUTION */
    if (you <= 2 || date <= 2)
       return 0;
    else if (you >= 8 || date >= 8)
	   return 2;
    else
	   return 1;
  /* END SOLUTION */
}
  /* END TEMPLATE */
}
