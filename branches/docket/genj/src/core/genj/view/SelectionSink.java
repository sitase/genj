package genj.view;

import java.awt.AWTEvent;
import java.awt.Component;

import genj.gedcom.Context;
import genj.window.WindowManager;

/**
 * A sink for selection events
 */
public interface SelectionSink {

  /**
   * fire selection event
   * @param context
   * @param isActionPerformed
   */
  public void fireSelection(Context context, boolean isActionPerformed);
  
  public class Dispatcher {
	  public static void fireSelection(AWTEvent event, Context context, boolean isActionPerformed) {
		  fireSelection(WindowManager.getComponent(event), context, isActionPerformed);
	  }
	  public static void fireSelection(Component source, Context context, boolean isActionPerformed) {
		  Component c = WindowManager.getComponent(source);
		  while (c!=null) {
			  if (c instanceof SelectionSink) {
				  ((SelectionSink)c).fireSelection(context, isActionPerformed);
				  return;
			  }
			  c = c.getParent();
		  }
		  throw new IllegalArgumentException("No sink for source "+source);
	  }
  }
}
