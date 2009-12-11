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

import java.util.ArrayList;
import java.util.List;

/**
 * A context represents a 'current context in Gedcom terms', a gedcom
 * an entity and a property
 */
public class Context {

  private Gedcom gedcom;
  private List<Entity> entities = new ArrayList<Entity>();
  private List<Property> properties = new ArrayList<Property>();

  @Override
  public boolean equals(Object obj) {
    Context that = (Context)obj;
    return this.gedcom==that.gedcom && this.entities.equals(that.entities) 
      && this.properties.equals(that.properties);
  }
  
  /**
   * Constructor
   */
  public Context(Context context) {
    this.gedcom = context.gedcom;
    this.entities.addAll(context.entities);
    this.properties.addAll(context.properties);
  }

  /**
   * Constructor
   */
  public Context(Gedcom gedcom, List<? extends Entity> entities, List<? extends Property> properties) {
    this.gedcom = gedcom;

    // grab ents
    for (Entity e : entities) {
      if (e.getGedcom()!=gedcom)
        throw new IllegalArgumentException("gedcom must be same");
      if (!entities.contains(e))
        this.entities.add(e);
    }

    // grab props
    for (Property p : properties) {
      if (!this.properties.contains(p)) {
        Entity e = p.getEntity();
        if (e.getGedcom()!=gedcom)
          throw new IllegalArgumentException("gedcom must be same");
        this.properties.add(p);
        if (!entities.contains(e))
          this.entities.add(e);
      }
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
  public Context(Property prop) {
    this(prop.getGedcom());
    properties.add(prop);
  }

  /**
   * Constructor
   */
  public Context(Entity entity) {
    this(entity.getGedcom());
    entities.add(entity);
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
   * Accessor - last property selected
   */
  public Property getProperty() {
    return properties.isEmpty() ? null : (Property)properties.get(0);
  }

  /**
   * Accessor - all entities
   */
  public List<? extends Entity> getEntities() {
    return entities;
  }

  /**
   * Accessor - properties
   */
  public List<? extends Property> getProperties() {
    return properties;
  }

  private List<Property> getProperties(Entity entity) {
    if (entity.getGedcom()!=gedcom)
      throw new IllegalArgumentException("entity.gedcom!=gedcom");
    List<Property> result = new ArrayList<Property>();
    for (Property prop : properties) {
      if (prop.getEntity()==entity)
        result.add(prop);
    }
    return result;
  }
  
  /** storage */
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append(gedcom.getName());
    for (Entity entity : entities) {
      result.append("[");
      result.append(entity.getId());
      
      for (Property prop : getProperties(entity)) {
        result.append("[");
        result.append(prop.getPath());
        result.append("]");
      }
      
      result.append("]");
    }
    return result.toString();
  }
  

} //Context
