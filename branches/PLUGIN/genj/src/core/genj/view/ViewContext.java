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
import java.lang.reflect.Array;
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
  
  private List keys = new ArrayList();
  private Map key2actions = new HashMap();
  
  private ImageIcon  img = null;
  private String txt = null;
  private Object originalContext = null;
  
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
    originalContext = ged;
  }
  
  /**
   * Constructor
   */
  public ViewContext(Entity entity) {
    super(entity);
    
    originalContext = entity;
  }
  
  /**
   * Constructor
   */
  public ViewContext(Property prop) {
    super(prop);
    
    originalContext = prop;
  }
  
  /**
   * Constructor
   */
  public ViewContext(Gedcom ged, Property[] props) {
    super(ged, props);

    originalContext = props.length==1 ? props[0] : keyify(props);
  }
  
  private Object keyify(Object key) {
    // we patch an array up to a list so the hash's equals method leads to the required result since
    //  !new String[]{ "foo", "bar" }.equals(new String[]{ "foo", "bar" })
    // but
    //  new ArrayList(new String[]{ "foo", "bar" }).equals(new ArrayList(new String[]{ "foo", "bar" }))
    if (key!=null && key.getClass().isArray()) {
      if (Array.getLength(key)==1)
        key = Array.get(key, 0);
      else
        key = new ArrayList(Arrays.asList((Object[])key));
    }
    return key;
  }
  
  /** 
   * returns actions for given sub-context
   */
  private List getActions(Object key) {
    
    key = keyify(key);
    if (key==null || originalContext.equals(key))
      return keys;
    
    List actions = (List)key2actions.get(key);
    if (actions==null) {
      
      // create a new bucket
      actions = new ArrayList();
      key2actions.put(key, actions);
      
      // keep gedcom as last item if the key is not a string
      int pos = keys.size();
      if (!(key instanceof String))
        for (pos= 0; pos < keys.size(); pos++) {
          if (keys.get(pos) instanceof Gedcom) 
            break;
        }
        keys.add(pos, key);
    }
    
    return actions;
  }
  
  /**
   * Add a top-level action
   */
  public void addAction(Action2 action) {
    addAction(null, action);
  }
  
  /**
   * Add an action to a subcontext of this context 
   * @param key a group key - either String, Property, Property[], Entity, Entity[], Gedcom
   */
  public void addAction(Object key, Action2 action) {
    getActions(key).add(action);
  }
  
  /**
   * Add a top-level separator
   */
  public void addSeparator() {
    addSeparator(null);
  }
 
  /**
   * Add an action to a subcontext
   */
  public void addSeparator(Object key) {
    List actions  = getActions(key);
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

  private Object[] toList(Object list, Class type) {
    try {
      Object[] result = (Object[])Array.newInstance(type, ((List)list).size());
      for (int i=0, j= ((List)list).size(); i<j ; i++ ) 
        result[i] = ((List)list).get(i);
      return result;
    } catch (ArrayStoreException e) {
      return null;
    } catch (ClassCastException e) {
      return null;
    }
  }
  
  /** 
   * Create a context menu for this context
   */
  /*package*/ JPopupMenu getPopupMenu(Component target) {
    
    MenuHelper mh = new MenuHelper().setTarget(target);
    JPopupMenu popup = mh.createPopup();
    
    // loop over all sub-contexts
    for (Iterator it = keys.iterator(); it.hasNext(); ) {
      Object key = it.next();
      
      // action?
      if (key instanceof Action2) {
        mh.createItem((Action2)key);
        continue;
      }
      
      // items for set of entities?
      Entity[] entities = (Entity[])toList(key, Entity.class);
      if (entities!=null) {
        mh.createMenu("'"+Property.getPropertyNames(entities,5)+"' ("+entities.length+")", Property.getImage(entities));
        mh.createItems(getActions(key));
        mh.popMenu();
        continue;
      }
      
      // items for single entity?
      if (key instanceof Entity) {
        Entity entity = (Entity)key;
        String title = Gedcom.getName(entity.getTag(),false)+" '"+entity.getId()+'\'';
        mh.createMenu(title, entity.getImage(false));
        mh.createItems(getActions(entity));
        mh.popMenu();
        continue;
      }
      
      // items for set of properties?
      Property[] properties = (Property[])toList(key, Property.class);
      if (properties!=null) {
        mh.createMenu("'"+Property.getPropertyNames(properties, 5)+"' ("+properties.length+")", Property.getImage(properties));
        mh.createItems(getActions(key));
        mh.popMenu();
        continue;
      }
      
      // items for a single property?
      if (key instanceof Property) {
        Property property = (Property)key;
        // a sub-menu with appropriate actions
        mh.createMenu(Property.LABEL+" '"+TagPath.get(property).getName() + '\'' , property.getImage(false));
        mh.createItems(getActions(property));
        mh.popMenu();
        continue;
      }
      
      // items for gedcom?
      if (key instanceof Gedcom) {
        Gedcom gedcom = (Gedcom)key;
        String title = "Gedcom '"+gedcom.getName()+'\'';
        mh.createMenu(title, Gedcom.getImage());
        mh.createItems(getActions(gedcom));
        mh.popMenu();
        continue;
      }

      // fallthrough: named subcontext
      mh.createMenu((String)key.toString());
      mh.createItems(getActions(key));
      mh.popMenu();

      // next
    }
    
    // done
    return popup;
  }
    
} //ViewContext


