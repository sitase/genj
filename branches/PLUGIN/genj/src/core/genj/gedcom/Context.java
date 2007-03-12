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
package genj.gedcom;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * A context represents a 'current context in Gedcom terms', a gedcom
 * an entity and a property
 */  
public class Context {
  
  private Gedcom gedcom;
  private List entities = new ArrayList();
  private List properties = new ArrayList();
  private Class entityType = null;
  private Class propertyType = null;
  
  /**
   * Constructor
   */
  public Context(Context context) {
    this.gedcom = context.gedcom;
    this.entities.addAll(context.entities);
    this.properties.addAll(context.properties);
    this.entityType = context.entityType;
    this.propertyType = context.propertyType;
  }
  
  /**
   * Constructor
   */
  public Context(Context[] contexts) {
    if (contexts.length==0)
      throw new IllegalArgumentException("can't construct context from empty list of contexts");
    
    Context context = contexts[0];
    gedcom = context.gedcom;
    entities.addAll(context.entities);
    properties.addAll(context.properties);
    entityType = context.entityType;
    propertyType = context.propertyType;
    
    for (int i = 1; i < contexts.length; i++) {
      context = contexts[i];
      addProperties(context.getProperties());
      addEntities(context.getEntities());
    }
  }
  
  /**
   * Constructor
   */
  public Context(Gedcom ged) {
    if (ged==null)
      throw new IllegalArgumentException("gedcom for context can't be null");
    gedcom = ged;
  }
  
  /**
   * Constructor
   */
  public Context(Entity entity) {
    this(entity.getGedcom());
    addEntity(entity);
  }
  
  /**
   * Constructor
   */
  public Context(Property prop) {
    this(prop.getGedcom());
    addProperty(prop);
  }
  
  /**
   * Constructor
   */
  public Context(Gedcom ged, Property[] props) {
    this(ged);
    addProperties(props);
  }
  
  /**
   * A context less a given property
   */
  public Context less(Property prop) {
    if (prop instanceof Entity)
      return less((Entity)prop);
    if (!properties.contains(prop))
      return this;
    Context clone = new Context(this);
    clone.properties.remove(prop);
    return clone;
  }
  
  /**
   * A context less a given entity
   */
  public Context less(Entity entity) {
    if (!entities.contains(entity))
      return this;
    Context clone = new Context(this);
    clone.entities.remove(entity);
    return clone;
  }
  
  /**
   * containment check
   */
  public boolean contains(Property prop) {
    if (prop instanceof Entity)
      return contains((Entity)prop);
    return properties.contains(prop);
  }
  
  /**
   * containment check
   */
  public boolean contains(Entity entity) {
    return entities.contains(entity);
  }
  
  /**
   * Add an entity
   */
  private void addEntity(Entity e) {
    // check gedcom
    if (e.getGedcom()!=gedcom)
      throw new IllegalArgumentException("entity's gedcom can't be different");
    // keep track of entity/types we contain
    entities.remove(e);
    if (entityType!=null&&entityType!=e.getClass())
      entityType = Entity.class;
    else 
      entityType = e.getClass();
    entities.add(e);
  }
  
  /**
   * Add entities
   */
  private void addEntities(Entity[] es) {
    for (int i = 0; i < es.length; i++) 
      addEntity(es[i]);
  }
  
  /**
   * Remove entities
   */
  private void removeEntities(Collection rem) {
    
    // easy for entities
    entities.removeAll(rem);
    
    // do properties to
    for (ListIterator iterator = properties.listIterator(); iterator.hasNext();) {
      Property prop = (Property) iterator.next();
      if (rem.contains(prop.getEntity()))
        iterator.remove();
    }
  }
  
  /**
   * Add a property
   */
  private void addProperty(Property p) {
    // keep entity
    addEntity(p.getEntity());
    if (p instanceof Entity)
      return;
    // check gedcom
    if (p.getGedcom()!=gedcom)
      throw new IllegalArgumentException("property's gedcom can't be different");
    // keep track of property types we contain
    properties.remove(p);
    if (propertyType!=null&&propertyType!=p.getClass())
      propertyType = Property.class;
    else 
      propertyType = p.getClass();
    // keep it
    properties.add(p);
  }
  
  /**
   * Add properties
   */
  private void addProperties(Property[] ps) {
    for (int i = 0; i < ps.length; i++) 
      addProperty(ps[i]);
  }
  
  /**
   * Remove properties
   */
  private void removeProperties(Collection rem) {
    properties.removeAll(rem);
  }
  
  /**
   * Accessor
   */
  public Gedcom getGedcom() {
    return gedcom;
  }
  
  /**
   * Accessor - last entity selected
   */
  public Entity getEntity() {
    return entities.isEmpty() ? null : (Entity)entities.get(0);
  }
  
  /**
   * number of entities
   */
  public int getNumEntities() {
    return entities.size();
  }

  /**
   * Accessor - last property selected
   */
  public Property getProperty() {
    return properties.isEmpty() ? null : (Property)properties.get(0);
  }
  
  /**
   * Accessor - all entities
   */
  public Entity[] getEntities() {
    if (entityType==null)
      return new Entity[0];
    return (Entity[])entities.toArray((Entity[])Array.newInstance(entityType, entities.size()));
  }

  /**
   * Accessor - properties
   */
  public Property[] getProperties() {
    if (propertyType==null)
      return new Property[0];
    return (Property[])properties.toArray((Property[])Array.newInstance(propertyType, properties.size()));
  }
  
  /**
   * number of properties
   */
  public int getNumProperties() {
    return properties.size();
  }

} //Context
