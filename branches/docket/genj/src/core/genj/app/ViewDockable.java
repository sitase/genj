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
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.TagPath;
import genj.util.Registry;
import genj.util.swing.Action2;
import genj.util.swing.MenuHelper;
import genj.view.ActionProvider;
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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
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
  public ViewDockable(Workbench workbench, ViewFactory factory, Context context) {

    this.workbench = workbench;

    // title
    String title = factory.getTitle();

    // get a registry
    Registry registry = new Registry(Registry.lookup(context.getGedcom().getOrigin().getFileName(), context.getGedcom().getOrigin()), factory.getClass().getName() + ".1");

    // create new View
    // FIXME docket pass in current selection context to new view
    view = factory.createView(title, registry, context);
    
    // backlink
    view.putClientProperty(ViewDockable.class, this);

    // create the view
    setContent(view);
    setTitle(title);
    setIcon(factory.getImage());
  }
  
  public View getView() {
    return (View)getContent();
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
   * WorkbenchListener callback - workbench signals request for commit of in-flight changes
   */
  public void commitRequested() {
    view.commit();
  }
  
  /**
   * WorkbenchListener callback - workbench signals closing 
   */
  public boolean workbenchClosing() {
    return view.closing();
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
    
    /**
     * Find workbench for given component
     * @return workbench or null
     */
    private static Workbench getWorkbench(Component component) {
      do {
        if (component instanceof View) {
          ViewDockable dockable = (ViewDockable) ((View)component).getClientProperty(ViewDockable.class);
          return dockable!=null ? dockable.workbench : null;
        }
        component = component.getParent();
      }
      while (component!=null);
      return null;
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
    public void actionPerformed(ActionEvent event) {
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

    /**
     * Create a popup menu for given context
     */
    private JPopupMenu getContextMenu(ViewContext context, Component target) {
      
      // make sure context is valid 
      if (context==null)
        return null;
      
      Property[] properties = context.getProperties();
      Entity[] entities = context.getEntities();
      Gedcom gedcom = context.getGedcom();

      // make sure any existing popup is cleared
      MenuSelectionManager.defaultManager().clearSelectedPath();
      
      // find all action providers
      List<ActionProvider> providers = getWorkbench(target).getActionProviders();
      
      // hook up context menu to toplevel component - child components are more likely to have been 
      // removed already by the time any of the associated actions are run
      while (target.getParent()!=null) target = target.getParent();

      // create a popup
      MenuHelper mh = new MenuHelper().setTarget(target);
      JPopupMenu popup = mh.createPopup();

      // popup local actions?
      mh.createItems(context.getActions());
      mh.createSeparator(); // it's lazy
      
      // items for set or single property?
      if (properties.length>1) {
        mh.createMenu("'"+Property.getPropertyNames(properties, 5)+"' ("+properties.length+")");
        for (ActionProvider provider : providers) try {
          mh.createSeparator();
          mh.createItems(provider.createActions(properties));
        } catch (Throwable t) {
          LOG.log(Level.WARNING, "Action Provider exception on createActions(Property[])", t);
        }
        mh.popMenu();
      }
      if (properties.length==1) {
        Property property = properties[0];
        while (property!=null&&!(property instanceof Entity)&&!property.isTransient()) {
          // a sub-menu with appropriate actions
          mh.createMenu(Property.LABEL+" '"+TagPath.get(property).getName() + '\'' , property.getImage(false));
          for (ActionProvider provider : providers) try {
            mh.createItems(provider.createActions(property));
          } catch (Throwable t) {
            LOG.log(Level.WARNING, "Action Provider exception on createActions(Property)", t);
          }
          mh.popMenu();
          // recursively for parents
          property = property.getParent();
        }
      }
          
      // items for set or single entity
      if (entities.length>1) {
        mh.createMenu("'"+Property.getPropertyNames(entities,5)+"' ("+entities.length+")");
        for (ActionProvider provider : providers) try {
          mh.createSeparator();
          mh.createItems(provider.createActions(entities));
        } catch (Throwable t) {
          LOG.log(Level.WARNING, "Action Provider exception on createActions(Entity[])", t);
        }
        mh.popMenu();
      }
      if (entities.length==1) {
        Entity entity = entities[0];
        String title = Gedcom.getName(entity.getTag(),false)+" '"+entity.getId()+'\'';
        mh.createMenu(title, entity.getImage(false));
        for (ActionProvider provider : providers) try {
          mh.createItems(provider.createActions(entity));
        } catch (Throwable t) {
          LOG.log(Level.WARNING, "Action Provider exception on createActions(Entity)", t);
        }
        mh.popMenu();
      }
          
      // items for gedcom
      String title = "Gedcom '"+gedcom.getName()+'\'';
      mh.createMenu(title, Gedcom.getImage());
      for (ActionProvider provider : providers) try {
        mh.createItems(provider.createActions(gedcom));
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "Action Provider exception on createActions(Gedcom", t);
      }
      mh.popMenu();

      // done
      return popup;
    }
    
  } //ContextHook

  public void gedcomClosed(Gedcom gedcom) {
  }

  public void gedcomOpened(Gedcom gedcom) {
  }
  
} //ViewDockable