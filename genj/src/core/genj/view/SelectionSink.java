package genj.view;

import genj.gedcom.Context;
import genj.util.swing.DialogHelper;
import genj.util.swing.DialogHelper.ComponentVisitor;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.RootPaneContainer;

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
    
    public static SelectionSink getSink(AWTEvent event) {
      return getSink((Component)event.getSource());
    }
    
    public static SelectionSink getSink(Component source) {
      SelectionSink sink = (SelectionSink)DialogHelper.visitOwners(source, new ComponentVisitor() {
        public Component visit(Component parent, Component child) {
          if (parent instanceof RootPaneContainer) {
            Container contentPane = ((RootPaneContainer)parent).getContentPane();
            if (contentPane.getComponentCount()>0 && contentPane.getComponent(0) instanceof SelectionSink)
              return contentPane.getComponent(0);
          }
          return parent instanceof SelectionSink ? parent : null;
        }
      });
      return sink;
    }

    public static void fireSelection(AWTEvent event, Context context) {
      boolean isActionPerformed = false;
      if (event instanceof ActionEvent)
        isActionPerformed |= (((ActionEvent)event).getModifiers()&ActionEvent.CTRL_MASK)!=0;
      if (event instanceof MouseEvent)
        isActionPerformed |= (((MouseEvent)event).getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0;
      fireSelection((Component)event.getSource(), context, isActionPerformed);
    }

    public static void fireSelection(Component source, Context context, boolean isActionPerformed) {

      SelectionSink sink = getSink(source);
      if (sink!=null)
        sink.fireSelection(context, isActionPerformed);
    }
  }
}
