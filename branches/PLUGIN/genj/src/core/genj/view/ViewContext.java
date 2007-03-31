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

import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.TagPath;
import genj.util.swing.Action2;
import genj.util.swing.MenuHelper;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

/**
 * A view context is a gedcom context enriched with UI actions
 * @see Context
 */  
public class ViewContext extends Context implements Comparable {
  
  private Map key2actions = new HashMap();
  private ImageIcon  img = null;
  private String txt = null;
  
  /**
   * Constructor
   */
  public ViewContext(Context context) {
    super(context);
  }
  
  /**
   * Constructor
   */
  public ViewContext(Gedcom ged) {
    super(ged);
  }
  
  /**
   * Constructor
   */
  public ViewContext(Entity entity) {
    super(entity);
  }
  
  /**
   * Constructor
   */
  public ViewContext(Property prop) {
    super(prop);
  }
  
  /**
   * Constructor
   */
  public ViewContext(Gedcom ged, Property[] props) {
    super(ged, props);
  }
  
  /** 
   * returns actions for given sub-context
   */
  private List getActions(Object group) {
    // we patch an array up to a list so the hash's equals method leads to the required result since
    //  !new String[]{ "foo", "bar" }.equals(new String[]{ "foo", "bar" })
    // but
    //  new ArrayList(new String[]{ "foo", "bar" }).equals(new ArrayList(new String[]{ "foo", "bar" }))
    if (group.getClass().isArray())
      group = new ArrayList(Arrays.asList((Object[])group));
    List actions = (List)key2actions.get(group);
    if (actions==null) {
      actions = new ArrayList();
      key2actions.put(group, actions);
    }
    return actions;
  }
  
  /**
   * Add a top-level action
   */
  public void addAction(Action2 action) {
    getActions(this).add(action);
  }
  
  /**
   * Add a top-level separator
   */
  public void addSeparator() {
    addSeparator(this);
  }
 
  /**
   * Adds a separator to top-level and each group
   */
  public void addSeparator(boolean global) {

    addSeparator(this);
    
    if (global) for (Iterator groups = key2actions.keySet().iterator(); groups.hasNext();) 
      addSeparator(groups.next());
    
  }
  
  /**
   * Add an action to an action group in this context 
   * In context menus supported groups are String, Property, Property[], Entity, Entity[], Gedcom 
   */
  public void addAction(Object group, Action2 action) {
    getActions(group).add(action);
  }
  
  /**
   * Add an action group separator
   */
  public void addSeparator(Object group) {
    List actions  = getActions(group);
    if (actions.size()>0&&actions.get(actions.size()-1)!=MenuHelper.ACTION_SEPARATOR)
      actions.add(MenuHelper.ACTION_SEPARATOR);
   }
  
  /** 
  * Accessor 
  */
  public String getText() {
   
   if (txt!=null)
     return txt;
   
   if (getNumProperties()==1) {
     Property prop = getProperty();
     txt = Gedcom.getName(prop.getTag()) + "/" + prop.getEntity();
   } else if (getNumProperties()>1)
     txt = Property.getPropertyNames(getProperties(), 5);
   else  if (getNumEntities()==1) 
     txt = getEntity().toString();
   else if (getNumEntities()>1)
     txt = Entity.getPropertyNames(getEntities(), 5);
   else txt = getGedcom().getName();
   
   return txt;
  }
  
  /** 
  * Accessor
  */
  public ViewContext setText(String text) {
   txt = text;
   return this;
  }
  
  /** 
  * Accessor
  */
  public ImageIcon getImage() {
   // an override?
   if (img!=null)
     return img;
   // check prop/entity/gedcom
   if (getNumProperties()==1)
     img = getProperty().getImage(false);
   else if (getNumEntities()==1)
     img = getEntity().getImage(false);
   else img = Gedcom.getImage();
   return img;
  }
  
  /** 
  * Accessor
  */
  public ViewContext setImage(ImageIcon set) {
   img = set;
   return this;
  }
  
  /** comparison  */
  public int compareTo(Object o) {
    ViewContext that = (ViewContext)o;
    if (this.txt==null)
      return -1;
    if (that.txt==null)
      return 1;
    return this.txt.compareTo(that.txt);
  }
  
  /** 
   * Create a context menu for this context
   */
  /*package*/ JPopupMenu getPopupMenu(Component target) {
    
    MenuHelper mh = new MenuHelper().setTarget(target);
    JPopupMenu popup = mh.createPopup();
    
    // decipher content
    Property[] properties = getProperties();
    Entity[] entities = getEntities();
    Gedcom gedcom = getGedcom();

    // items for local actions?
    mh.createItems(getActions(this));
    
    // popup for string keyed actions?
    if (!key2actions.isEmpty()) {
      for (Iterator keys = key2actions.keySet().iterator(); keys.hasNext();) {
        Object key = keys.next();
        if (key instanceof String) {
          mh.createMenu((String)key);
          mh.createItems(getActions(key));
          mh.popMenu();
        }
      }
    }
    
    // dive into gedcom structure 
    mh.createSeparator(); // it's lazy
    
    // items for set or single property?
    if (properties.length>1) {
      mh.createMenu("'"+Property.getPropertyNames(properties, 5)+"' ("+properties.length+")");
      mh.createItems(getActions(properties));
      mh.popMenu();
    } else if (properties.length==1) {
      Property property = properties[0];
      while (property!=null&&!(property instanceof Entity)&&!property.isTransient()) {
        // a sub-menu with appropriate actions
        mh.createMenu(Property.LABEL+" '"+TagPath.get(property).getName() + '\'' , property.getImage(false));
        mh.createItems(getActions(property));
        mh.popMenu();
        // recursively for parents
        property = property.getParent();
      }
    }
        
    // items for set or single entity
    if (entities.length>1) {
      mh.createMenu("'"+Property.getPropertyNames(entities,5)+"' ("+entities.length+")");
      mh.createItems(getActions(entities));
      mh.popMenu();
    } else if (entities.length==1) {
      Entity entity = entities[0];
      String title = Gedcom.getName(entity.getTag(),false)+" '"+entity.getId()+'\'';
      mh.createMenu(title, entity.getImage(false));
      mh.createItems(getActions(entity));
      mh.popMenu();
    }
        
    // items for gedcom
    String title = "Gedcom '"+gedcom.getName()+'\'';
    mh.createMenu(title, Gedcom.getImage());
    mh.createItems(getActions(gedcom));
    mh.popMenu();

    // done
    return popup;
  }

} //ViewContext
