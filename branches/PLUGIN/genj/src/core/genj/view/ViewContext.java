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
import genj.util.swing.Action2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

/**
 * A view context is a gedcom context enriched with UI actions
 * @see Context
 */  
public class ViewContext extends Context implements Comparable {
  
  private Map sub2actions = new HashMap();
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
   * Access to top-level actions
   */
  public List getActions() {
    return Collections.unmodifiableList(getActions(this));
  }
  
  /** 
   * returns actions for given sub-context
   */
  public List getActions(Object group) {
    // we patch an array up to a list so the hash's equals method leads to the required result since
    //  !new String[]{ "foo", "bar" }.equals(new String[]{ "foo", "bar" })
    // but
    //  new ArrayList(new String[]{ "foo", "bar" }).equals(new ArrayList(new String[]{ "foo", "bar" }))
    if (group.getClass().isArray())
      group = new ArrayList(Arrays.asList((Object[])group));
    List actions = (List)sub2actions.get(group);
    if (actions==null) {
      actions = new ArrayList();
      sub2actions.put(group, actions);
    }
    return actions;
  }
  
  /**
   * Returns list of action groups (basically corresponding to sub-menus later)
   */
  public Collection getActionGroups() {
    return sub2actions.keySet();
  }
  
  /**
   * Add a top-level action
   */
  public void addAction(Action2 action) {
    getActions(this).add(action);
  }
  
  /**
   * Add an action to an action group in this context 
   * In context menus supported groups are String, Property, Property[], Entity, Entity[], Gedcom 
   */
  public void addAction(Object group, Action2 action) {
    getActions(group).add(action);
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

} //ViewContext
