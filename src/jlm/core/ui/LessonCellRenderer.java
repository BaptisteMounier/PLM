package jlm.core.ui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jlm.core.model.Game;
import jlm.core.model.lesson.Lesson;

public class LessonCellRenderer extends JLabel implements ListCellRenderer {

	private static final long serialVersionUID = 2633159591634625763L;
    //final static ImageIcon longIcon = new ImageIcon("long.gif");
    //final static ImageIcon shortIcon = new ImageIcon("short.gif");

	public LessonCellRenderer() {
		//setOpaque(true);
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		if (value instanceof Lesson) {
			Lesson lesson = (Lesson) value;

			setText(lesson.getName());
			
			boolean accessible = Game.getInstance().isAccessible(lesson);			
			setEnabled(accessible);
			if (accessible) {
				if (lesson.isSuccessfullyCompleted()) {
					setIcon(ResourcesCache.getIcon("resources/star.png"));
				} else {
					setIcon(ResourcesCache.getIcon("resources/star_white.png"));
				}
			}

			
		} else {
			setText(value!=null?value.toString():"");
		}
     
       
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        
               
        setFont(list.getFont());
        setOpaque(true);
        return this;
	}

}