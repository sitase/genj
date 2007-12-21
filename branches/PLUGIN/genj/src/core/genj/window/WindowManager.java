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
package genj.window;

import genj.util.Registry;
import genj.util.swing.Action2;
import genj.util.swing.TextAreaWidget;
import genj.util.swing.TextFieldWidget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

/**
 * Abstract base type for WindowManagers
 */
public abstract class WindowManager {

  /** message types*/
  public static final int  
    ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE,
    INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE,
    WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE,
    QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE,
    PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;
  
  /** a counter for temporary keys */
  private int temporaryKeyCounter = 0;  

  /** broadcast listeners */
  private static List listeners = new ArrayList();
  
  /** whether we're muting broadcasts atm */
  private boolean muteBroadcasts = false;
  
  /** registry */
  protected Registry registry;
  
  /** the vm instance */
  private static WindowManager instance = null;
  
  /** a log */
  /*package*/ final static Logger LOG = Logger.getLogger("genj.window");
  
  /** 
   * Constructor
   */
  protected WindowManager(Registry registry) {
    if (instance!=null)
      throw new IllegalArgumentException("Only one window manager per VM allowed");
    instance = this;
    this.registry = registry;
  }
  
  /**
   * Add listener
   */
  public static void addBroadcastListener(WindowBroadcastListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }
  
  /**
   * Remove listener
   */
  public static void removeBroadcastListener(WindowBroadcastListener listener) {
    synchronized (listeners) { 
      listeners.remove(listener);
    }
  }
  
  /**
   * Set the menubar for a component's root pane
   */
  public static void setMenubar(Component component, JMenuBar menubar) {
    getInstance(component).setMenubarImpl(component, menubar);
  }

  /**
   * Set the menubar for a component's root pane
   */
  protected abstract void setMenubarImpl(Component component, JMenuBar menubar);
  
  /**
   * Set the toolbar to the provided actions for a component's root pane
   */
  public static void setToolbar(Component component, JToolBar toolbar) {
    
    // patch buttons for toolbar
    for (int i = 0, j = toolbar.getComponentCount(); i < j; i++) {
      Component comp = toolbar.getComponent(i);
      if (comp instanceof AbstractButton)
        ((AbstractButton)comp).setMargin(new Insets(2,2,2,2));
    }
    
    // handle in impl
    getInstance(component).setToolbarImpl(component, toolbar);
    
  }
  
  /**
   * impl
   */
  protected abstract void setToolbarImpl(Component component, JToolBar toolbar);

  /**
   * get the toolbar for a component
   */
  public JToolBar getToolbar(Component component) {
    // handle in impl
    return getToolbarImpl(component);
  }
  
  /**
   * impl
   */
  protected abstract JToolBar getToolbarImpl(Component component);

  /**
   * Broadcast an event to all components that implement BroadcastListener 
   */
  protected void broadcast(WindowBroadcastEvent event) {
    
    // are we muted atm?
    if (muteBroadcasts)
      return;
    try {
      muteBroadcasts = true;
     
      // send outbound
      Set visitedOutbound = new HashSet();
      if (!broadcastOutbound(event, visitedOutbound))
        return;
      
      // switch to inbound
      event.setInbound();
      
      // tell listeners of managers
      Iterator ls;
      synchronized (listeners) {
        ls = new ArrayList(listeners).iterator();
      }
      while (ls.hasNext()) {
        WindowBroadcastListener l = (WindowBroadcastListener) ls.next();
        try {
          l.handleBroadcastEvent(event);
        } catch (Throwable t) {
          LOG.log(Level.WARNING, "broadcast listener threw throwable - continuing broadcast", t);
        }
      }
      
      // tell components (but not the originating one)
      for (Iterator roots = getRootComponents().iterator(); roots.hasNext(); ) {
        Component root = (Component)roots.next();
        broadcastInbound(event, (Component)root, visitedOutbound);
      }
      
    } finally {
      muteBroadcasts = false;
    }
    
    // done
  }
  
  protected boolean broadcastOutbound(WindowBroadcastEvent event, Set visited) {
    
    // bubble up "outbound"
    Component cursor = event.getSource();
    while (cursor!=null) {
      
      // a listener that cares? chance to stop this from bubbling outbound even more
      if (cursor instanceof WindowBroadcastListener) {
        visited.add(cursor);
        try {
          if (!((WindowBroadcastListener)cursor).handleBroadcastEvent(event))
            return false;
        } catch (Throwable t) {
          LOG.log(Level.WARNING, "broadcast listener threw throwable - continuing broadcast", t);
        }
      }
      
      // move up 
      cursor = cursor.getParent();
    }

    // done
    return true;
      
  }
  
  protected void broadcastInbound(WindowBroadcastEvent event, Component component, Set dontRevisit) {
    
    // a stop?
    if (dontRevisit.contains(component))
      return;
    
    // .. to component
    if (component instanceof WindowBroadcastListener) {
      try {
        if (!((WindowBroadcastListener)component).handleBroadcastEvent(event))
          return;
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "broadcast listener threw throwable - not recursing broadcast", t);
        return;
      }
    }
    
    // and to children
    if (component instanceof Container) {
      Component[] cs = (((Container)component).getComponents());
      for (int j = 0; j < cs.length; j++) {
        broadcastInbound(event, cs[j], dontRevisit);
      }
    }
    
    // done
  }
    
  /**
   * Close dialog/frame 
   * @param key the dialog/frame's key
   */
  public abstract void close(String key);
  
  /**
   * Return root components of all heavyweight dialogs/frames
   */
  public abstract List getRootComponents();
  
  /**
   * Return the content of a dialog/frame 
   * @param key the dialog/frame's key 
   */
  public abstract JComponent getContent(String key);
  
  /**
   * Makes sure the dialog/frame is visible
   * @param key the dialog/frame's key 
   * @return success or no valid key supplied
   */
  public abstract boolean show(String key);
  
  /**
   * Setup a new independant window
   */
  public final String openWindow(String key, String title, ImageIcon image, JComponent content) {
    // create a key?
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // deal with it in impl
    openWindowImpl(key, title, image, content);
    // done
    return key;
  }
  
  /**
   * Implementation for handling an independant window
   */
  protected abstract void openWindowImpl(String key, String title, ImageIcon image, JComponent content);
  
  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, java.lang.String, String[], javax.swing.JComponent)
   */
  public final int openDialog(String key, String title,  int messageType, String txt, Action[] actions, Component owner) {
    
    // analyze the text
    int maxLine = 40;
    int cols = 40, rows = 1;
    StringTokenizer lines = new StringTokenizer(txt, "\n\r");
    while (lines.hasMoreTokens()) {
      String line = lines.nextToken();
      if (line.length()>maxLine) {
        cols = maxLine;
        rows += line.length()/maxLine;
      } else {
        cols = Math.max(cols, line.length());
        rows++;
      }
    }
    rows = Math.min(10, rows);
    
    // create a textpane for the txt
    TextAreaWidget text = new TextAreaWidget("", rows, cols);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setText(txt);
    text.setEditable(false);    
    text.setCaretPosition(0);
    text.setRequestFocusEnabled(false);

    // wrap in reasonable sized scroll
    JScrollPane content = new JScrollPane(text);
      
    // delegate
    return openDialog(key, title, messageType, content, actions, owner);
  }
  
  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, java.awt.Dimension, javax.swing.JComponent[], java.lang.String[], javax.swing.JComponent)
   */
  public final int openDialog(String key, String title,  int messageType, JComponent[] content, Action[] actions, Component owner) {
    // assemble content into Box (don't use Box here because
    // Box extends Container in pre JDK 1.4)
    JPanel box = new JPanel();
    box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
    for (int i = 0; i < content.length; i++) {
      if (content[i]==null) continue;
      box.add(content[i]);
      content[i].setAlignmentX(0F);
    }
    // delegate
    return openDialog(key, title, messageType, box, actions, owner);
  }

  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, java.lang.String, java.lang.String, javax.swing.JComponent)
   */
  public final String openDialog(String key, String title,  int messageType, String txt, String value, Component owner) {

    // prepare text field and label
    TextFieldWidget tf = new TextFieldWidget(value, 24);
    JLabel lb = new JLabel(txt);
    
    // delegate
    int rc = openDialog(key, title, messageType, new JComponent[]{ lb, tf}, Action2.okCancel(), owner);
    
    // analyze
    return rc==0?tf.getText().trim():null;
  }

  /**
   * dialog core routine
   */
  public final int openDialog(String key, String title,  int messageType, JComponent content, Action[] actions, Component owner) {
    // check options - default to OK
    if (actions==null) 
      actions = Action2.okOnly();
    // key is necessary
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // do it
    Object rc = openDialogImpl(key, title, messageType, content, actions, owner);
    // analyze - check which action was responsible for close
    for (int a=0; a<actions.length; a++) 
      if (rc==actions[a]) return a;
    return -1;
  }
  
  /**
   * Implementation for core frame handling
   */
  protected abstract Object openDialogImpl(String key, String title,  int messageType, JComponent content, Action[] actions, Component owner);

  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, javax.swing.JComponent, javax.swing.JComponent)
   */
  public final String openNonModalDialog(String key, String title,  int messageType, JComponent content, Action[] actions, Component owner) {
    // check options - none ok
    if (actions==null) actions = new Action[0];
    // key is necessary
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // do it
    openNonModalDialogImpl(key, title, messageType, content, actions, owner);
    // done
    return key;
  }

  /**
   * Implementation for core frame handling
   */
  protected abstract void openNonModalDialogImpl(String key, String title,  int messageType, JComponent content, Action[] actions, Component owner);

  /**
   * Create a temporary key
   */
  protected String getTemporaryKey() {
    return "_"+temporaryKeyCounter++;
  }

  /**
   * @see genj.window.WindowManager#closeAll()
   */
  public abstract void closeAll();
  
  /**
   * Returns an appropriate WindowManager instance for given component
   * @return manager or null if no appropriate manager could be found
   */
  public static WindowManager getInstance(Component component) {
    return instance;
  }

  private static WindowManager getInstanceImpl(Component cursor) {
    while (cursor!=null) {
      if (cursor instanceof JComponent) {
        WindowManager result = (WindowManager) ((JComponent)cursor).getClientProperty(WindowManager.class);
        if (result!=null)
          return result;
      }
      cursor = cursor.getParent();
    }
    return null;
  }

  /**
   * A patched up JOptionPane
   */
  protected class Content extends JOptionPane {
    
    /** constructor */
    protected Content(int messageType, JComponent content, Action[] actions) {
      super(new JLabel(),messageType, JOptionPane.DEFAULT_OPTION, null, new String[0] );
      
      // wrap content in a JPanel - the OptionPaneUI has some code that
      // depends on this to stretch it :(
      JPanel wrapper = new JPanel(new BorderLayout());
      wrapper.add(BorderLayout.CENTER, content);
      setMessage(wrapper);

      // create our action buttons
      Option[] options = new Option[actions.length];
      for (int i=0;i<actions.length;i++)
        options[i] = new Option(actions[i]);
      setOptions(options);
      
      // set defalut?
      if (options.length>0) 
        setInitialValue(options[0]);
      
      // done
    }

    /** patch up layout - don't allow too small */
    public void doLayout() {
      // let super do its thing
      super.doLayout();
      // check minimum size
      Container container = getTopLevelAncestor();
      Dimension minimumSize = container.getMinimumSize();
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      minimumSize.width = Math.min(screen.width/2, minimumSize.width);
      minimumSize.height = Math.min(screen.height/2, minimumSize.height);
      Dimension size        = container.getSize();
      if (size.width < minimumSize.width || size.height < minimumSize.height) {
        Dimension newSize = new Dimension(Math.max(minimumSize.width,  size.width),
                                          Math.max(minimumSize.height, size.height));
        container.setSize(newSize);
      }
      // checked
    }
    
    /** an option in our option-pane */
    private class Option extends JButton implements ActionListener {
      
      /** constructor */
      private Option(Action action) {
        super(action);
        addActionListener(this);
      }
      
      /** trigger */
      public void actionPerformed(ActionEvent e) {
        // this will actually force the dialog to hide - JOptionPane listens to property changes
        setValue(getAction());
      }
      
    } //Action2Button
    
  } // Content 
  
} //AbstractWindowManager
