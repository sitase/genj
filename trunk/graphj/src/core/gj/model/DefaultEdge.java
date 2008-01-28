/**
 * This file is part of GraphJ
 * 
 * Copyright (C) 2002-2004 Nils Meier
 * 
 * GraphJ is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GraphJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GraphJ; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package gj.model;

import java.util.Collection;
import java.util.Iterator;

/**
 * A default implementation of an edge
 */
public class DefaultEdge implements Edge {
  
  private Vertex from,to;
  
  public DefaultEdge(Vertex from, Vertex to) {
    this.from = from;
    this.to = to;
  }
  
  public DefaultEdge(Collection<Vertex> verticies) {
    if (verticies.size()!=2) 
      throw new IllegalArgumentException("Edge requires exactly two verticies");
    Iterator<Vertex> it = verticies.iterator();
    this.from = it.next();
    this.to = it.next();
  }

  public Vertex getEnd() {
    return from;
  }

  public Vertex getStart() {
    return to;
  }

  @Override
  public int hashCode() {
    return from.hashCode() + to.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Edge))
      return false;
    Edge that = (Edge)obj;
    return (this.from.equals(that.getStart()) && this.to.equals(that.getEnd()));
  }
  
  @Override
  public String toString() {
    return from+">"+to;
  }
  
}
