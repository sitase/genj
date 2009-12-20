package genj.view;

import genj.gedcom.Context;
import genj.window.WindowManager;
import genj.window.WindowManager.ContainerVisitor;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

/**
 * A sink for selection events
 */
public interface SelectionSink {

  /**
   * fire selection event
   * 
   * @param context
   * @param isActionPerformed
   */
  public void fireSelection(Context context, boolean isActionPerformed);

  public class Dispatcher {
    public static void fireSelection(AWTEvent event, Context context) {
      boolean isActionPerformed = false;
      if (event instanceof ActionEvent)
        isActionPerformed |= (((ActionEvent)event).getModifiers()&ActionEvent.CTRL_MASK)!=0;
      if (event instanceof MouseEvent)
        isActionPerformed |= (((MouseEvent)event).getModifiers()&MouseEvent.CTRL_DOWN_MASK)!=0;
      fireSelection((Component)event.getSource(), context, isActionPerformed);
    }

    public static void fireSelection(Component source, Context context, boolean isActionPerformed) {

      SelectionSink sink = (SelectionSink)WindowManager.visitContainers(source, new ContainerVisitor() {
        public Component visit(Component parent, Component child) {
          return parent instanceof SelectionSink ? parent : null;
        }
      });
      
      if (sink==null)
        throw new IllegalArgumentException("Can't find sink for "+source);

      sink.fireSelection(context, isActionPerformed);
    }
  }
}
