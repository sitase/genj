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
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

/**
 * Abstract base type for WindowManagers
 */
public abstract class WindowManager {

  private final static Object WINDOW_MANAGER_KEY = WindowManager.class;
  
  /** message types*/
  public static final int  
    ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE,
    INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE,
    WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE,
    QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE,
    PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;
  
  /** registry */
  protected Registry registry;

  /** a counter for temporary keys */
  private int temporaryKeyCounter = 0;  

  /** a mapping between key to framedlg */
  private Map key2framedlg = new HashMap();
  
  /** broadcast listeners */
  private List listeners = new ArrayList();
  
  /** a log */
  /*package*/ final static Logger LOG = Logger.getLogger("genj.window");
  
  /** 
   * Constructor
   */
  protected WindowManager(Registry regiStry) {
    registry = regiStry;
  }
  
  /**
   * Add listener
   */
  public void addBroadcastListener(WindowBroadcastListener listener) {
    listeners.add(listener);
  }
  
  /**
   * Remove listener
   */
  public void removeBroadcastListener(WindowBroadcastListener listener) {
    listeners.remove(listener);
  }
  
  /**
   * Broadcast an event to all components that implement BroadcastListener 
   */
  public void broadcast(WindowBroadcastEvent event) {
    
    // initialize it
    event.setWindowManager(this);
    
    // tell listeners
    for (Iterator ls = listeners.iterator(); ls.hasNext();) {
      WindowBroadcastListener l = (WindowBroadcastListener) ls.next();
      try {
        l.handleBroadcastEvent(event);
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "broadcast listener threw throwable", t);
      }
    }
    
    // tell components
    String[] keys = recallKeys();
    for (int i = 0; i < keys.length; i++) {
      broadcast(event, getContent(keys[i]));
    }
    
    // done
  }
  
  private void broadcast(WindowBroadcastEvent event, Component component) {
    
    // .. to component
    if (component instanceof WindowBroadcastListener) {
      try {
        if (!((WindowBroadcastListener)component).handleBroadcastEvent(event))
          return;
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "broadcast listener threw throwable", t);
      }
    }
    
    // and to children
    if (component instanceof Container) {
      Component[] cs = (((Container)component).getComponents());
      for (int j = 0; j < cs.length; j++) {
        broadcast(event, cs[j]);
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
   * Returns an appropriate WindowManager instance for given component
   * @return manager or null if no appropriate manager could be found
   */
  public static WindowManager getInstance(JComponent component) {
    Component cursor = component;
    while (cursor!=null) {
      if (cursor instanceof JComponent) {
        Object object = ((JComponent)cursor).getClientProperty(WINDOW_MANAGER_KEY);
        if (object instanceof WindowManager)
          return (WindowManager)object;
      }
      cursor = cursor.getParent();
    }
    LOG.warning("Failed to find window manager for "+component);
    return null;
  }
  
  /**
   * Setup a new independant window
   */
  public final String openWindow(String key, String title, ImageIcon image, JComponent content, JMenuBar menu, Action close) {
    // set us up
    content.putClientProperty(WINDOW_MANAGER_KEY, this);
    // create a key?
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    boolean maximized = registry.get(key+".maximized", false);
    // deal with it in impl
    Object frame = openWindowImpl(key, title, image, content, menu, bounds, maximized, close);
    // remember it
    key2framedlg.put(key, frame);
    // done
    return key;
  }
  
  /**
   * Implementation for handling an independant window
   */
  protected abstract Object openWindowImpl(String key, String title, ImageIcon image, JComponent content, JMenuBar menu, Rectangle bounds, boolean maximized, Action onClosing);
  
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
    // set us up
    content.putClientProperty(WINDOW_MANAGER_KEY, this);
    // check options - default to OK
    if (actions==null) 
      actions = Action2.okOnly();
    // key is necessary
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    // do it
    Object result = openDialogImpl(key, title, messageType, content, actions, owner, bounds, true);
    // analyze - check which action was responsible for close
    for (int a=0; a<actions.length; a++) 
      if (result==actions[a]) return a;
    return -1;
  }
  
  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, javax.swing.JComponent, javax.swing.JComponent)
   */
  public final String openNonModalDialog(String key, String title,  int messageType, JComponent content, Action[] actions, Component owner) {
    // set us up
    content.putClientProperty(WINDOW_MANAGER_KEY, this);
    // check options - none ok
    if (actions==null) actions = new Action[0];
    // key is necessary
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    // do it
    Object dialog = openDialogImpl(key, title, messageType, content, actions, owner, bounds, false);
    // remember it
    key2framedlg.put(key, dialog);
    // done
    return key;
  }

  /**
   * Implementation for core frame handling
   */
  protected abstract Object openDialogImpl(String key, String title,  int messageType, JComponent content, Action[] actions, Component owner, Rectangle bounds, boolean modal);

  /**
   * Create a temporary key
   */
  protected String getTemporaryKey() {
    return "_"+temporaryKeyCounter++;
  }

  /**
   * Recall Keys
   */
  protected String[] recallKeys() {
    return (String[])key2framedlg.keySet().toArray(new String[0]);
  }

  /**
   * Recall frame/dialog
   */
  protected Object recall(String key) {
    // no key - no result
    if (key==null) 
      return null;
    // look it up
    return key2framedlg.get(key);
  }

  /**
   * Forget about frame/dialog, stash away bounds
   */
  protected void closeNotify(String key, Rectangle bounds, boolean maximized) {
    // no key - no action
    if (key==null) 
      return;
    // forget frame/dialog
    key2framedlg.remove(key);
    // temporary key? nothing to stash away
    if (key.startsWith("_")) 
      return;
    // keep bounds
    if (bounds!=null&&!maximized)
      registry.put(key, bounds);
    registry.put(key+".maximized", maximized);
    // done
  }
  
  /**
   * @see genj.window.WindowManager#closeAll()
   */
  public void closeAll() {

    // loop through keys    
    String[] keys = recallKeys();
    for (int k=0; k<keys.length; k++) {
      close(keys[k]);
    }
    
    // done
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
