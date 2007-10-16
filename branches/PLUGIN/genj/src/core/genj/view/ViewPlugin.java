/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2002 Nils Meier <nils@meiers.net>
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
package genj.view;

import genj.app.ExtendMenubar;
import genj.app.ExtendToolbar;
import genj.gedcom.Gedcom;
import genj.plugin.ExtensionPoint;
import genj.plugin.Plugin;
import genj.plugin.PluginManager;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.window.WindowManager;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.FocusManager;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

/**
 * A plugin that provides [a] view(s) onto gedcom data
 */
public abstract class ViewPlugin implements Plugin {
  
  private final static ContextHook CONTEXT_HOOK = new ContextHook();
  private final static Resources RESOURCES = Resources.get(ViewPlugin.class);
  private final static Logger LOG = Logger.getLogger("genj.view");
  
  protected PluginManager manager = null;

  /**
   * @see genj.plugin.Plugin#initPlugin(genj.plugin.PluginManage)
   */
  public void initPlugin(PluginManager manager) {
    this.manager = manager;
  }
  
  /** 
   * Provide an image
   */
  protected abstract ImageIcon getImage();
  
  /** 
   * Provide a text
   */
  protected abstract String getTitle();
  
  /**
   * Provide a component
   */
  protected abstract JComponent createView(Gedcom gedcom, Registry registry);
  
  /**
   * @see genj.plugin.Plugin#extend(ExtensionPoint)
   */
  public void extend(ExtensionPoint ep) {
    
    // menubar we can hook into?
    if (ep instanceof ExtendMenubar) 
      extend((ExtendMenubar)ep);
    
    // toolbar we can hook into?
    if (ep instanceof ExtendToolbar) 
      extend((ExtendToolbar)ep);
    
    // done
  }

  /** extend toolbar */
  private void extend(ExtendToolbar toolbar) {
    toolbar.addAction(new Open(toolbar.getGedcom()));
  }
  
  /** extend menubar */
  private void extend(ExtendMenubar menu) {
    
    Gedcom gedcom = menu.getGedcom();
    if (gedcom!=null)
      menu.addAction(RESOURCES.getString("views"), new Open(gedcom));
  }
  
  /**
   * Get the package name of this view plugin
   */
  private String getPackage() {
    
    Matcher m = Pattern.compile(".*\\.(.*)\\..*").matcher(getClass().getName());
    if (!m.find())
      throw new IllegalArgumentException("can't resolve package for "+this);
    return m.group(1);
    
  }

  /** 
   * helper for creating a context menu popupmenu
   */
  public static JPopupMenu createContextMenu(ViewContext context, Component target) {
    
    // make sure context is valid
    if (context==null)
      throw new IllegalArgumentException("context can't be null");
    
    // send it around
    PluginManager.get().extend(new ExtendContextMenu(context));
    
    // make sure any existing popup is cleared
    MenuSelectionManager.defaultManager().clearSelectedPath();
    
    // hook up context menu to toplevel component - child components are more likely to have been 
    // removed already by the time any of the associated actions are run
    while (target.getParent()!=null) target = target.getParent();

    // create the popup
    return context.getPopupMenu(target);
  }
  
  /**
   * Action - Open a view
   */
  private class Open extends Action2 {
    
    private Gedcom gedcom;

    /** constructor */
    private Open(Gedcom gedcom) {
      this.gedcom = gedcom;
      String txt = RESOURCES.getString("view.open", ViewPlugin.this.getTitle());
      setText(txt);
      setTip(txt);
      setImage(ViewPlugin.this.getImage());
      setEnabled(gedcom!=null);
    }
    
    private boolean isShiftModifier() {
      try {
        AWTEvent event = EventQueue.getCurrentEvent();
        return event instanceof InputEvent&&((InputEvent)event).isShiftDown();
      } catch (Throwable t) {
        return false;
      }
    }
    
    /** execute */
    protected void execute() {
      
      if (isShiftModifier())
        System.out.println("SHIFT");
      
      int index = 1;
      String key = getPackage()+"."+index;
    
      // get a registry 
      Registry registry = new Registry( Registry.lookup(gedcom), key) ;

      // title 
      String title = gedcom.getName()+" - "+getTitle()+" ("+registry.getViewSuffix()+")";

      // create the view
      JComponent view = createView(gedcom, registry);
      
      // add context hook for keyboard shortcuts
      InputMap inputs = view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
      inputs.put(KeyStroke.getKeyStroke("shift F10"), CONTEXT_HOOK);
      inputs.put(KeyStroke.getKeyStroke("CONTEXT_MENU"), CONTEXT_HOOK); // this only works in Tiger 1.5 on Windows
      view.getActionMap().put(CONTEXT_HOOK, CONTEXT_HOOK);

      // open frame
      WindowManager.getInstance(getTarget()).openWindow(key, title, ViewPlugin.this.getImage(), view);
      
      // extend toolbar
//      WindowManager.addToolbarAction(view, new Close(key));
          
      // done
    }
    
  } //Open

  /**
   * Action - Close view
   */
  private class Close extends Action2 {
    private String key;
    /** constructor */
    protected Close(String key) {
      this.key = key;
      setImage(Images.imgClose);
      setTip(RESOURCES, "view.close.tip");
    }
    /** run */
    protected void execute() {
      WindowManager.getInstance(getTarget()).close(key);
    }
  } //ActionClose
  
  /**
   * Our hook into keyboard and mouse operated context changes / menues
   */
  private static class ContextHook extends Action2 implements AWTEventListener {
    
    /** constructor */
    private ContextHook() {
      try {
        AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            Toolkit.getDefaultToolkit().addAWTEventListener(ContextHook.this, AWTEvent.MOUSE_EVENT_MASK);
            return null;
          }
        });
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "Cannot install ContextHook ("+t.getMessage()+")");
      }
    }
    
    /**
     * Resolve context for given component
     */
    private ViewContext getContext(Component component) {
      ViewContext context;
      // find context provider in component hierarchy
      while (component!=null) {
        // component can provide context?
        if (component instanceof ContextProvider) {
          ContextProvider provider = (ContextProvider)component;
          context = provider.getContext();
          if (context!=null)
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
      if (context!=null) {
        JPopupMenu popup = createContextMenu(context, focus);
        if (popup!=null)
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
      MouseEvent me = (MouseEvent) event;
      if (!(me.isPopupTrigger()||me.getID()==MouseEvent.MOUSE_CLICKED))
        return;
      
      // find deepest component (since components without attached listeners
      // won't be the source for this event)
      Component component  = SwingUtilities.getDeepestComponentAt(me.getComponent(), me.getX(), me.getY());
      if (!(component instanceof JComponent))
        return;
      Point point = SwingUtilities.convertPoint(me.getComponent(), me.getX(), me.getY(), component );
      
      // try to identify context
      ViewContext context = getContext(component);
      if (context==null) 
        return;

      // a popup?
      if(me.isPopupTrigger())  {
        
        // cancel any menu
        MenuSelectionManager.defaultManager().clearSelectedPath();
        
        // show context menu
        JPopupMenu popup = createContextMenu(context, (JComponent)component);
        if (popup!=null)
          popup.show((JComponent)component, point.x, point.y);
        
      }
      
      // a double-click on provider?
      if (me.getButton()==MouseEvent.BUTTON1&&me.getID()==MouseEvent.MOUSE_CLICKED&&me.getClickCount()>1) {
        WindowManager.broadcast(new ContextSelectionEvent(context, component, true));
      }
        
      // done
    }
    
  } //ContextMenuHook
  
  
} //ViewPlugin
