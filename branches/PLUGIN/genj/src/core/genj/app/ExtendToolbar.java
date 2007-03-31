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
import genj.util.swing.Action2;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JToolBar;

/**
 * An extension point that allows to add toolbar items
 */
public class ExtendToolbar extends ExtensionPoint {
  
  private final static Action2 SEPARATOR = new Action2();
  
  private Gedcom gedcom;
  private JComponent source;
  
  private List actions = new ArrayList();
  
  /** 
   * Constructor 
   */
  protected ExtendToolbar(Gedcom gedcom, JComponent source) {
    this.gedcom = gedcom;
    this.source = source;
  }
  
  /** 
   * currently selected gedcom that will be reflected in the menu shown 
   * @return current gedcom or null if none selected
   */
  public Gedcom getGedcom() {
    return gedcom;
  }

  /** 
   * add a toolbar action 
   */
  public void addAction(Action2 action) {
    if (action.getImage()==null)
      throw new IllegalArgumentException("Extend Toolbar actions need to provide image");
    action.setText(null);
    actions.add(action);
  }
  
  /**
   * add a separator
   */
  public void addSeparator() {
    actions.add(SEPARATOR);
  }
  
  /** resolve toolbar */
  /*package*/ JToolBar getToolbar() {
    
    // create toolbar and setup helper
    JToolBar toolbar = new JToolBar();
    for (int i = 0; i < actions.size(); i++) {
      Action2 action = (Action2)actions.get(i);
      action.setTarget(source);
      if (action==SEPARATOR)
        toolbar.addSeparator();
      else
        toolbar.add(action);
    }
    
    // donre
    return toolbar;
  }
  
}
