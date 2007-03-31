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
package genj.app;

import genj.gedcom.Gedcom;
import genj.plugin.ExtensionPoint;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.MenuHelper;
import genj.window.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JMenuBar;

/**
 * An extension point that allows to add menu items to the application menu
 */
public class ExtendMenubar extends ExtensionPoint {
  
  private final static Resources RESOURCES = Resources.get(ExtendMenubar.class);

  private static final String
    TXT_FILE = RESOURCES.getString("cc.menu.file"),
    TXT_TOOLS = RESOURCES.getString("cc.menu.tools"),
    TXT_HELP = RESOURCES.getString("cc.menu.help");
  
  public static final String
    FILE_MENU= "FILE",
    TOOLS_MENU = "TOOLS",
    HELP_MENU = "HELP";
  
  private Gedcom gedcom;
  private JComponent source;
  
  private List menus = new ArrayList();
  private Map group2actions = new HashMap();
  
  /** 
   * Constructor 
   */
  protected ExtendMenubar(Gedcom gedcom, JComponent source) {
    this.gedcom = gedcom;
    this.source = source;
  }
  
  /**
   * WindowManager
   */
  public WindowManager getWindowManager() {
    return WindowManager.getInstance(source); 
  }
  
  /**
   * Source of event
   */
  public JComponent getSource() {
    return source; 
  }
  
  /** 
   * currently selected gedcom that will be reflected in the menu shown 
   * @return current gedcom or null if none selected
   */
  public Gedcom getGedcom() {
    return gedcom;
  }

  /** 
   * add a menu  action 
   */
  public void addAction(String menu, Action2 action) {
    if (!menus.contains(menu))
      menus.add(menu);
    getActions(menu).add(action);
  }
  
  /** 
   * add a menu separator
   */
  public void addSeparator(String menu) {
    if (!menus.contains(menu))
      menus.add(menu);
    getActions(menu).add(MenuHelper.ACTION_SEPARATOR);
  }
  
  /** resolve menus */
  public List getMenus() {
    return Collections.unmodifiableList(menus);
  }
  
  /** resolve actions */
  private List getActions(String menu) {
    List result = (List)group2actions.get(menu);
    if (result==null) {
      result = new ArrayList();
      group2actions.put(menu, result);
    }
    return result;
  }
  
  /** calculate the menubar */
  /*package*/ JMenuBar getMenuBar() {
    
    // new menu
    MenuHelper mh = new MenuHelper().setTarget(source);
    JMenuBar menubar = new JMenuBar();
    mh.pushMenu(menubar);
    
    for (Iterator groups = getMenus().iterator(); groups.hasNext();) {
      
      String group = (String) groups.next();
      List actions = getActions(group);
      
      // TOOD we should parse groups for sub-groups (e.g. Plugin|Foo|Action)
      
      // TODO this is a hardcoded list of menu defaults
      if (group==ExtendMenubar.FILE_MENU)
        group = TXT_FILE;
      else if (group==ExtendMenubar.TOOLS_MENU)
        group = TXT_TOOLS;
      else if (group==ExtendMenubar.HELP_MENU)
        group = TXT_HELP;

      mh.createMenu(group);
      mh.createItems(actions);
      mh.popMenu();
      
      // next group
    }
    
    // done
    return menubar;
  }
  
}
