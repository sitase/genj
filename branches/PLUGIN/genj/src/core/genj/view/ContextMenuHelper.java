/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2007 Nils Meier <nils@meiers.net>
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

import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.TagPath;
import genj.plugin.PluginManager;
import genj.util.swing.MenuHelper;

import java.awt.Component;
import java.util.Iterator;

import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;

/**
 * A helper for constructing a context menu for a given gedcom context
 */
public class ContextMenuHelper {
  
  public static JPopupMenu createContextMenu(ViewContext context, Component target, PluginManager plugins) {
    
    // make sure context is valid
    if (context==null)
      throw new IllegalArgumentException("context can't be null");
    
    // decipher content
    Property[] properties = context.getProperties();
    Entity[] entities = context.getEntities();
    Gedcom gedcom = context.getGedcom();

    // send it around
    plugins.extend(new ExtendContextMenu(context));
    
    // make sure any existing popup is cleared
    MenuSelectionManager.defaultManager().clearSelectedPath();
    
    // hook up context menu to toplevel component - child components are more likely to have been 
    // removed already by the time any of the associated actions are run
    while (target.getParent()!=null) target = target.getParent();

    // create a popup
    MenuHelper mh = new MenuHelper().setTarget(target);
    JPopupMenu popup = mh.createPopup();
    
    // items for local actions?
    mh.createItems(context.getActions());
    
    // popup for string keyed actions?
    if (!context.getActionGroups().isEmpty()) {
      for (Iterator keys = context.getActionGroups().iterator(); keys.hasNext();) {
        Object key = keys.next();
        if (key instanceof String) {
          mh.createMenu((String)key);
          mh.createItems(context.getActions(key));
          mh.popMenu();
        }
      }
    }
    
    // dive into gedcom structure 
    mh.createSeparator(); // it's lazy
    
    // items for set or single property?
    if (properties.length>1) {
      mh.createMenu("'"+Property.getPropertyNames(properties, 5)+"' ("+properties.length+")");
      mh.createItems(context.getActions(properties));
      mh.popMenu();
    } else if (properties.length==1) {
      Property property = properties[0];
      while (property!=null&&!(property instanceof Entity)&&!property.isTransient()) {
        // a sub-menu with appropriate actions
        mh.createMenu(Property.LABEL+" '"+TagPath.get(property).getName() + '\'' , property.getImage(false));
        mh.createItems(context.getActions(property));
        mh.popMenu();
        // recursively for parents
        property = property.getParent();
      }
    }
        
    // items for set or single entity
    if (entities.length>1) {
      mh.createMenu("'"+Property.getPropertyNames(entities,5)+"' ("+entities.length+")");
      mh.createItems(context.getActions(entities));
      mh.popMenu();
    } else if (entities.length==1) {
      Entity entity = entities[0];
      String title = Gedcom.getName(entity.getTag(),false)+" '"+entity.getId()+'\'';
      mh.createMenu(title, entity.getImage(false));
      mh.createItems(context.getActions(entity));
      mh.popMenu();
    }
        
    // items for gedcom
    String title = "Gedcom '"+gedcom.getName()+'\'';
    mh.createMenu(title, Gedcom.getImage());
    mh.createItems(context.getActions(gedcom));
    mh.popMenu();

    // done
    return popup;
  }
  
}
