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
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;

/**
 * A view context is a gedcom context enriched with UI actions
 * @see Context
 */  
public class ViewContext extends Context implements Comparable {
  
  private ViewManager manager;
  private List actions = new ArrayList();
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
   * Add an action
   */
  public ViewContext addAction(Action2 action) {
    actions.add(action);
    return this;
  }
  
  /**
   * Add actions
   */
  public ViewContext addActions(Action2.Group group) {
    actions.add(group);
    return this;
  }
  
  /**
   * Access to actions
   */
  public List getActions() {
    return Collections.unmodifiableList(actions);
  }
  
  /**
   * Connect to manager
   */
  /*package*/ void setManager(ViewManager set) {
    manager = set;
  }
  
  /**
   * Accessor
   */
  public ViewManager getManager() {
    return manager;
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
