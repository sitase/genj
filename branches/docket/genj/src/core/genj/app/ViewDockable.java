/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2009 Nils Meier <nils@meiers.net>
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package genj.app;

import genj.gedcom.Context;
import genj.gedcom.Gedcom;
import genj.util.Registry;
import genj.util.swing.Action2;
import genj.view.ContextProvider;
import genj.view.SelectionListener;
import genj.view.ToolBar;
import genj.view.View;
import genj.view.ViewContext;
import genj.view.ViewFactory;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import swingx.docking.DefaultDockable;
import swingx.docking.Docked;

/**
 * A dockable for views
 */
/* package */class ViewDockable extends DefaultDockable implements SelectionListener, WorkbenchListener {
  
  private final static Logger LOG = Logger.getLogger("genj.app");
  private final static ContextHook HOOK = new ContextHook();

  private View view;
  private Workbench workbench;
  private boolean ignoreSelectionChanged = false;

  /**
   * Constructor
   */
  public ViewDockable(Workbench workbench, ViewFactory factory, Gedcom gedcom) {

    this.workbench = workbench;

    // title
    String title = factory.getTitle();

    // get a registry
    Registry registry = new Registry(Registry.lookup(gedcom.getOrigin().getFileName(), gedcom.getOrigin()), factory.getClass().getName() + ".1");

    // create new View
    view = factory.createView(title, gedcom, registry);

    // create the view
    setContent(view);
    setTitle(title);
  }

  @Override
  public void docked(final Docked docked) {
    super.docked(docked);

    // listen to view
    view.addSelectionListener(this);

    // listen to workbench
    workbench.addWorkbenchListener(this);

    // only if ToolBarSupport and no bar installed
    view.populate(new ToolBar() {
      public void add(Action action) {
        docked.addTool(action);
      }

      public void add(JComponent component) {
        docked.addTool(component);
      }

      public void addSeparator() {
        docked.addToolSeparator();
      }
    });

    // .. a button for editing the View's settings
    // FIXME docket show settings button
    // if (SettingsWidget.hasSettings(view))
    // bh.create(new ActionOpenSettings());

    // .. a button for printing View
    // FIXME docket show print button
    // try {
    // Printer printer =
    // (Printer)Class.forName(view.getClass().getName()+"Printer").newInstance();
    // try {
    // printer.setView(view);
    // PrintTask print = new PrintTask(printer, viewHandle.getTitle(), view, new
    // PrintRegistry(viewHandle.getRegistry(), "print"));
    // print.setTip(ViewManager.RESOURCES, "view.print.tip");
    // bh.create(print);
    // } catch (Throwable t) {
    // ViewManager.LOG.log(Level.WARNING,
    // "can't setup printing for printer "+printer.getClass().getName());
    // ViewManager.LOG.log(Level.FINE,
    // "throwable while setting up "+printer.getClass().getName(), t);
    // }
    // } catch (Throwable t) {
    // }

    // done
  }

  @Override
  public void undocked() {

    // don't listen to view
    view.removeSelectionListener(this);

    // don't listen to workbench
    workbench.removeWorkbenchListener(this);

    // continue
    super.undocked();
  }

  /**
   * SelectionListener callback - view fired selection change
   */
  public void select(Context context, boolean isActionPerformed) {
    ignoreSelectionChanged = true;
    try {
      workbench.fireSelection(context, isActionPerformed);
    } finally {
      ignoreSelectionChanged = false;
    }
  }

  /**
   * WorkbenchListener callback - workbench signals selection change
   */
  public void selectionChanged(Context context, boolean isActionPerformed) {
    if (!ignoreSelectionChanged)
      view.select(context, isActionPerformed);
  }

  /**
   * Our hook into keyboard and mouse operated context changes / menu
   */
  private static class ContextHook extends Action2 implements AWTEventListener {

    /** constructor */
    private ContextHook() {
      try {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
          public Void run() {
            Toolkit.getDefaultToolkit().addAWTEventListener(ContextHook.this, AWTEvent.MOUSE_EVENT_MASK);
            return null;
          }
        });
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "Cannot install ContextHook", t);
      }
    }
    
    private JPopupMenu getContextMenu(Context context, Component target) {
      // FIXME docket need context menu assembly
      JPopupMenu result = new JPopupMenu("");
      result.add("Hello World");
      return result;
    }

    /**
     * Resolve context for given component
     */
    private ViewContext getContext(Component component) {
      ViewContext context;
      // find context provider in component hierarchy
      while (component != null) {
        // component can provide context?
        if (component instanceof ContextProvider) {
          ContextProvider provider = (ContextProvider) component;
          context = provider.getContext();
          if (context != null)
            return context;
        }
        // try parent
        component = component.getParent();
      }
      // not found
      return null;
    }

    /**
     * A Key press initiation of the context menu
     */
    protected void execute() {
      // only for jcomponents with focus
      Component focus = FocusManager.getCurrentManager().getFocusOwner();
      if (!(focus instanceof JComponent))
        return;
      // look for ContextProvider and show menu if appropriate
      ViewContext context = getContext(focus);
      if (context != null) {
        JPopupMenu popup = getContextMenu(context, focus);
        if (popup != null)
          popup.show(focus, 0, 0);
      }
      // done
    }

    /**
     * A mouse click initiation of the context menu
     */
    public void eventDispatched(AWTEvent event) {

      // a mouse popup/click event?
      if (!(event instanceof MouseEvent))
        return;
      final MouseEvent me = (MouseEvent) event;
      if (!(me.isPopupTrigger() || me.getID() == MouseEvent.MOUSE_CLICKED))
        return;

      // NM 20080130 do the component/context calculation in another event to
      // allow everyone to catch up
      // Peter reported that the context menu is the wrong one as
      // PropertyTreeWidget
      // changes the selection on mouse clicks (following right-clicks).
      // It might be that eventDispatched() is called before the mouse click is
      // propagated to the
      // component thus calculates the menu before the selection changes.
      // So I'm trying now to show the popup this in a later event to make sure
      // everyone caught up to the event

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          // find deepest component (since components without attached listeners
          // won't be the source for this event)
          Component component = SwingUtilities.getDeepestComponentAt(me.getComponent(), me.getX(), me.getY());
          if (!(component instanceof JComponent))
            return;
          Point point = SwingUtilities.convertPoint(me.getComponent(), me.getX(), me.getY(), component);

          // try to identify context
          ViewContext context = getContext(component);
          if (context == null)
            return;

          // a double-click on provider?
          if (me.getButton() == MouseEvent.BUTTON1 && me.getID() == MouseEvent.MOUSE_CLICKED && me.getClickCount() == 2) {
            View.fireSelection(component, context, true);
            return;
          }

          // a popup?
          if (me.isPopupTrigger()) {

            // cancel any menu
            MenuSelectionManager.defaultManager().clearSelectedPath();

            // show context menu
            JPopupMenu popup = getContextMenu(context, (JComponent) component);
            if (popup != null)
              popup.show((JComponent) component, point.x, point.y);

          }
        }
      });

      // done
    }

  } //ContextHook
  
} //ViewDockable