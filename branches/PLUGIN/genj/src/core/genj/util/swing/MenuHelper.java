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
package genj.util.swing;

import genj.util.MnemonicAndText;

import java.awt.Component;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Class which provides some static helpers for menu-handling
 */
public class MenuHelper  {
  
  public final static Action2 ACTION_SEPARATOR = new Action2();
  
  private Stack menus            = new Stack();  // JMenu or JPopupMenu or JMenuBar
  private Component target       = null;

  /** Setters */    
  public MenuHelper popMenu() { 
    // pop it of the stack
    JMenu menu = (JMenu)menus.pop(); 
    // remove trailing separator
    int count = menu.getMenuComponentCount();
    if (count>0 && menu.getMenuComponent(count-1).getClass() == JPopupMenu.Separator.class)
      menu.remove(count-1);
    // remove it if empty
    if (menu.getMenuComponentCount()==0)
      menu.getParent().remove(menu);
    // done
    return this; 
  }
  public MenuHelper pushMenu(JPopupMenu popup) { menus.push(popup); return this; }
  public MenuHelper pushMenu(JMenuBar bar) { menus.push(bar); return this; }
  public MenuHelper setTarget(Component set) { target=set; return this; }

  /**
   * Creates a menubar
   */
  public JMenuBar createBar() {
    JMenuBar result = new JMenuBar();
    menus.push(result);
    return result;
  }
  
  /**
   * Creates a menu
   */
  public JMenu createMenu(String text) {
    return createMenu(text, null);
  }

  /**
   * Creates a menu
   */
  public JMenu createMenu(String text, Icon img) {
    JMenu result = new JMenu();
    
    // no text?
    if (text!=null&&text.length()>0) {
      MnemonicAndText mat = new MnemonicAndText(text);
      result.setText(mat.getText());
      result.setMnemonic(mat.getMnemonic());
    }
    if (img!=null) 
      result.setIcon(img);
    Object menu = peekMenu();
    if (menu instanceof JMenu)
      ((JMenu)menu).add(result);
    if (menu instanceof JPopupMenu)
      ((JPopupMenu)menu).add(result);
    if (menu instanceof JMenuBar)
      ((JMenuBar)menu).add(result);

    menus.push(result);
    return result;
  }
  
  /**
   * Creates a PopupMenu
   */
  public JPopupMenu createPopup() {
    // create one
    JPopupMenu result = new JPopupMenu();
    // that's the menu now
    pushMenu(result);
    // done
    return result;
  }

//  private void createItem(Component item) {
//
//    Object menu = peekMenu();
//    if (menu instanceof JMenu)
//      ((JMenu)menu).add(item);
//    if (menu instanceof JPopupMenu)
//      ((JPopupMenu)menu).add(item);
//    if (menu instanceof JMenuBar)
//      ((JMenuBar)menu).add(item);
//    
//  }

  /**
   * Creates items from list of ActionDelegates
   * @param actions either ActionDelegates or lists of ActionDelegates that
   * will be separated visually by createSeparator
   */
  public void createItems(List actions) {
    // Loop through list
    for (Iterator it  = actions.iterator(); it.hasNext(); ) 
      createItem((Action2)it.next());
    // done
  }

  /**
   * Creates an item
   */
  public void createItem(Action2 action) {
    
    // a separator?
    if (action==ACTION_SEPARATOR) {
      createSeparator();
      return;
    }
    
    // create a menu item
    JMenuItem result = new JMenuItem();
    result.setAction(action);

    // patch it up
    if (action.getAccelerator()!=null)
      result.setAccelerator(action.getAccelerator());
  
    // add it to current menu on stack  
    Object menu = peekMenu();
    if (menu instanceof JMenu)
      ((JMenu)menu).add(result);
    if (menu instanceof JPopupMenu)
      ((JPopupMenu)menu).add(result);
    if (menu instanceof JMenuBar)
      ((JMenuBar)menu).add(result);
      
    // propagate target
    if (target!=null) action.setTarget(target);
    
    // done
 }

  /**
   * Creates an separator
   */
  public MenuHelper createSeparator() {
    // try to create one
    Object menu = peekMenu();
    if (menu instanceof JMenu) {
      JMenu jmenu = (JMenu)menu;
      int count = jmenu.getMenuComponentCount();
      if (count>0 && jmenu.getMenuComponent(count-1).getClass() != JPopupMenu.Separator.class)
        jmenu.addSeparator();
    }
    if (menu instanceof JPopupMenu) {
      JPopupMenu pmenu = (JPopupMenu)menu;
      int count = pmenu.getComponentCount();
      if (count>0 && pmenu.getComponent(count-1).getClass() != JPopupMenu.Separator.class)
        pmenu.addSeparator();
    }
    // done      
    return this;
  }

  /**
   * Helper getting the top Menu from the stack
   */  
  private Object peekMenu() {
    if (menus.size()==0) return null;
    return menus.peek();
  }
  
} //MenuHelper

