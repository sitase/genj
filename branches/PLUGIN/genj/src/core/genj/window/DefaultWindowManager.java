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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JToolBar;
import javax.swing.RootPaneContainer;

/**
 * The default 'heavyweight' window manager
 */
public class DefaultWindowManager extends WindowManager {

  /** screen we're dealing with */
  private Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
  
  /** a hidden default frame */
  private JFrame defaultFrame = new JFrame();
  
  /** map window/dialog to key */
  private Map window2key = new HashMap();
  protected Map key2window = new HashMap();
  
  /** 
   * Constructor
   */
  public DefaultWindowManager(Registry registry, ImageIcon defaultDialogImage) {
    super(registry);
    if (defaultDialogImage!=null) defaultFrame.setIconImage(defaultDialogImage.getImage());
  }
  
  /**
   * Frame implementation
   */
  protected void openWindowImpl(final String key, String title, ImageIcon image, final JComponent content) {
    
    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    boolean maximized = registry.get(key+".maximized", false);
    
    // Create a frame
    final JFrame frame = new JFrame() {
      /**
       * dispose is our onClose hook because
       * WindowListener.windowClosed is too 
       * late (one frame) after dispose()
       */
      public void dispose() {
        // forget about key but keep bounds
        closeNotify(key, content, getBounds(), getExtendedState()==MAXIMIZED_BOTH);
        // continue
        super.dispose();
      }
    };
    // keep key around (before show!)
    key2window.put(key, frame);
    window2key.put(frame, key);


    // setup looks
    if (title!=null) frame.setTitle(title);
    if (image!=null) frame.setIconImage(image.getImage());

    // add content
    frame.getContentPane().add(content);

    // hook up our own closing code
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {

        // this goes inbound only
        WindowClosingEvent event = new WindowClosingEvent(key, frame);
        broadcastInbound(event, content, Collections.EMPTY_SET);        
        
        if (!event.isCancelled()) {
          frame.dispose();
          
          // tell all
          broadcast(new WindowClosedEvent(key, content));
        }
      }
    });

    // place
    if (bounds==null) {
      frame.pack();
      Dimension dim = frame.getSize();
      bounds = new Rectangle(screen.width/2-dim.width/2, screen.height/2-dim.height/2,dim.width,dim.height);
      LOG.log(Level.FINE, "Sizing window "+key+" to "+bounds+" after pack()");
    }
    frame.setBounds(bounds.intersection(screen));
    
    if (maximized)
      frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    
    // show
    frame.setVisible(true);
    
    // done
  }
  
  /**
   * Dialog implementation
   */
  protected void openNonModalDialogImpl(final String key, String title,  int messageType, final JComponent content, Action[] actions, Component owner) {

    // create an option pane
    JOptionPane optionPane = new Content(messageType, content, actions);
    
    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    
    // let it create the dialog
    final JDialog dlg = optionPane.createDialog(owner != null ? owner : defaultFrame, title);
    dlg.setResizable(true);
    dlg.setModal(false);
    if (bounds==null) {
      dlg.pack();
      if (owner!=null)
        dlg.setLocationRelativeTo(owner.getParent());
    } else {
      if (owner==null) {
        dlg.setBounds(bounds.intersection(screen));
      } else {
        dlg.setBounds(new Rectangle(bounds.getSize()).intersection(screen));
        dlg.setLocationRelativeTo(owner.getParent());
      }
    }

    // hook up to the dialog being hidden by the optionpane - that's what is being called after the user selected a button (setValue())
    dlg.addComponentListener(new ComponentAdapter() {
      public void componentHidden(ComponentEvent e) {
        closeNotify(key, content, dlg.getBounds(), false);
        dlg.dispose();
      }
    });
    
    // remember
    key2window.put(key, dlg);
    window2key.put(dlg, key);
    
    // show it
    dlg.setVisible(true);
    
    // return result
  }
  
  /**
   * Dialog implementation
   */
  protected Object openDialogImpl(final String key, String title,  int messageType, final JComponent content, Action[] actions, Component owner) {

    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    
    // create an option pane
    JOptionPane optionPane = new Content(messageType, content, actions);
    
    // let it create the dialog
    final JDialog dlg = optionPane.createDialog(owner != null ? owner : defaultFrame, title);
    dlg.setResizable(true);
    dlg.setModal(true);
    if (bounds==null) {
      dlg.pack();
      if (owner!=null)
        dlg.setLocationRelativeTo(owner.getParent());
    } else {
      if (owner==null) {
        dlg.setBounds(bounds.intersection(screen));
      } else {
        dlg.setBounds(new Rectangle(bounds.getSize()).intersection(screen));
        dlg.setLocationRelativeTo(owner.getParent());
      }
    }

    // hook up to the dialog being hidden by the optionpane - that's what is being called after the user selected a button (setValue())
    dlg.addComponentListener(new ComponentAdapter() {
      public void componentHidden(ComponentEvent e) {
        closeNotify(key, content, dlg.getBounds(), false);
        dlg.dispose();
      }
    });
    
    // remember
    key2window.put(key, dlg);
    window2key.put(dlg, key);
    
    // show it
    dlg.setVisible(true);
    
    // return result
    return optionPane.getValue();
  }
  
  /**
   * Intercept close notification
   */
  protected void closeNotify(String key, Component content, Rectangle bounds, boolean maximized) {
    
    // no key - no action
    if (key==null) 
      return;
    
    // temporary key? nothing to stash away
    if (!key.startsWith("_"))  {
      
      // keep bounds
      if (bounds!=null&&!maximized)
        registry.put(key, bounds);
      registry.put(key+".maximized", maximized);
      
      // keep toolbar orientation
      Container contentPane = getRootPane(content).getContentPane();
      Component[] cs = contentPane.getComponents();
      for (int c = 0; c < cs.length; c++) {
        if (cs[c] instanceof JToolBar) {
          registry.put(key+".toolbar", (String)((BorderLayout)contentPane.getLayout()).getConstraints(cs[c]));
          break;
        }
      }
      
    }
    
    // forget
    window2key.remove(key2window.remove(key));

    // done
  }
  
  /**
   * Find a JRootPane
   */
  protected JRootPane getRootPane(Component component) {
    // look for a rootpane
    for (Component cursor = component; cursor!=null ;) {
      if (cursor instanceof RootPaneContainer) 
        return ((RootPaneContainer)cursor).getRootPane();
      cursor = cursor.getParent();
    }
    throw new IllegalStateException("can't find rootpane of "+component);
  }
  
  /**
   * Set the menubar for a component's root pane
   */
  protected void setMenubarImpl(Component component, JMenuBar menubar) {
   getRootPane(component).setJMenuBar(menubar);
  }
  
  /**
   * Impl for setting window's toolbar
   */
  protected void setToolbarImpl(Component component, JToolBar toolbar) {
    
    // remove any previous toolbars
    Container content = getRootPane(component).getContentPane();
    Component[] cs = content.getComponents();
    for (int i=0; i<cs.length; i++) {
      if (cs[i] instanceof JToolBar) 
        content.remove(i);
    }
    
    // done?
    if (toolbar==null) 
      return;
    
    String key = (String)window2key.get(getWindowForComponent(component));
    if (key==null)
      throw new IllegalArgumentException("Unknown component to set toolbar on");
    
    // lookup orientation
    String dir = registry.get(key+".toolbar", BorderLayout.NORTH);
    int orientation;
    if (dir.equals(BorderLayout.WEST))
      orientation = JToolBar.VERTICAL;
    else if (dir.equals(BorderLayout.NORTH))
      orientation = JToolBar.HORIZONTAL;
    else if (dir.equals(BorderLayout.EAST))
      orientation = JToolBar.VERTICAL;
    else if (dir.equals(BorderLayout.SOUTH))
      orientation = JToolBar.HORIZONTAL;
    else {
      dir = BorderLayout.NORTH;
      orientation = JToolBar.HORIZONTAL;
    }
    toolbar.setOrientation(orientation);
    
    // add it
    content.add(toolbar, dir);
    
    // done
  }
  
  public JToolBar getToolbarImpl(Component component) {
    Container content = getRootPane(component).getContentPane();
    Component[] cs = content.getComponents();
    for (int i=0; i<cs.length; i++) {
      if (cs[i] instanceof JToolBar) 
        return (JToolBar)cs[i];
    }
    return null;
  }

  /**
   * @see genj.window.WindowManager#show(java.lang.String)
   */
  public boolean show(String key) {

    Object framedlg = key2window.get(key);
    
    if (framedlg instanceof JFrame) {
      ((JFrame)framedlg).toFront(); 
      return true;
    }

    if (framedlg instanceof JDialog) {
      ((JDialog)framedlg).toFront();
      return true;
    }

    return false;
  }
  
  /**
   * @see genj.window.WindowManager#closeFrame(java.lang.String)
   */
  public void close(String key) {

    Object framedlg = key2window.get(key);
    
    if (framedlg instanceof JFrame) {
      JFrame frame = (JFrame)framedlg;
      frame.dispose(); 
      return;
    }

    if (framedlg instanceof JDialog) {
      JDialog dlg = (JDialog)framedlg;
      dlg.setVisible(false); // we're using the optionpane signal for a closing dialog: hide it
      return;
    }

    // done
  }
  
  /**
   * Close all
   */
  public void closeAll() {

    // loop through keys    
    
    for (Iterator keys = new ArrayList(key2window.keySet()).iterator(); keys.hasNext(); )
      close((String)keys.next());
    
    // done
  }
  
  
  /**
   * @see genj.window.WindowManager#getRootComponents()
   */
  public List getRootComponents() {

    List result = new ArrayList();
    
    // loop through keys    
    
    for (Iterator keys = key2window.keySet().iterator(); keys.hasNext(); ) {
      
      String key = (String)keys.next();
      Object framedlg = key2window.get(key);

      if (framedlg instanceof JFrame)      
        result.add(((JFrame)framedlg).getRootPane());

      if (framedlg instanceof JDialog)      
        result.add(((JDialog)framedlg).getRootPane());
    }
    
    // done
    return result;
  }
  
  /**
   * @see genj.window.WindowManager#getContent(java.lang.String)
   */
  public JComponent getContent(String key) {
    
    Object framedlg = key2window.get(key);
    
    if (framedlg instanceof JFrame)
      return (JComponent)((JFrame)framedlg).getContentPane().getComponent(0); 

    if (framedlg instanceof JDialog)
      return (JComponent)((JDialog)framedlg).getContentPane().getComponent(0);

    return null;
  }

  /**
   * Get the window for given owner component
   */  
  private Window getWindowForComponent(Component c) {
    if (c instanceof Frame || c instanceof Dialog || c==null)
      return (Window)c;
    return getWindowForComponent(c.getParent());
  }
  
} //DefaultWindowManager