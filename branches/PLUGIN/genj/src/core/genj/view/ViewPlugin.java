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
import genj.util.Origin;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;

/**
 * A plugin that provides [a] view(s) onto gedcom data
 */
public abstract class ViewPlugin implements Plugin {
  
  private static Resources RESOURCES = Resources.get(ViewPlugin.class);
  
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
   * Helper that returns registry for gedcom
   */
  private Registry getRegistry(Gedcom gedcom) {
    Origin origin = gedcom.getOrigin();
    String name = origin.getFileName();
    return Registry.lookup(name, origin);
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
    
    /** execute */
    protected void execute() {
      
      int index = 1;
      String key = getPackage()+"."+index;
    
      // get a registry 
      Registry registry = new Registry( getRegistry(gedcom), key) ;

      // title 
      String title = gedcom.getName()+" - "+getTitle()+" ("+registry.getViewSuffix()+")";

      // create the view
      JComponent view = createView(gedcom, registry);
      
// FIXME context keyboard shortcut      
//      // add context hook for keyboard shortcuts
//      InputMap inputs = view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
//      inputs.put(KeyStroke.getKeyStroke("shift F10"), contextHook);
//      inputs.put(KeyStroke.getKeyStroke("CONTEXT_MENU"), contextHook); // this only works in Tiger 1.5 on Windows
//      view.getActionMap().put(contextHook, contextHook);

      // open frame
      manager.getWindowManager().openWindow(key, title, ViewPlugin.this.getImage(), view);
      
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
      setTip(ViewManager.RESOURCES, "view.close.tip");
    }
    /** run */
    protected void execute() {
      manager.getWindowManager().close(key);
    }
  } //ActionClose
  
} //ViewPlugin
