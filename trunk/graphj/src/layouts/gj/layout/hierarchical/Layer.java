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
package gj.layout.hierarchical;

import gj.model.Edge;
import gj.model.Vertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * an ordered layer of nodes
 */
/*package*/ class Layer implements Iterable<Layer.Assignment> {
  
  /*package*/ static Vertex DUMMY = new Dummy();
  
  private List<Assignment> assignments = new ArrayList<Assignment>();

  /**
   * Add a vertex to layer at given position
   */
  /*package*/ void add(Assignment assignment) {
    
    int pos = 0;
    while (pos<assignments.size()){
      if (assignment.originalx < assignments.get(pos).originalx) 
        break;
      pos ++;
    }

    add(assignment, pos);
  }
  
  /*package*/ void add(Assignment assignment, int pos) {
    
    assignment.position = pos;
    assignments.add(pos, assignment);
    
    while (++pos<assignments.size())
      assignments.get(pos).position++;
    
  }
  
  /*package*/ void swap(int u, int v) {
    Assignment vu = assignments.get(u);
    vu.position = v;
    
    Assignment vv = assignments.get(v);
    vv.position = u;
    
    assignments.set(u, vv);
    assignments.set(v, vu);
    
  }
  
  /*package*/ int size() {
    return assignments.size();
  }
  
  /*package*/ Assignment get(int pos) {
    return assignments.get(pos);
  }
  
  @Override
  public String toString() {
    return assignments.toString();
  }
  
  /**
   * A vertex assigned to a layer
   */
  /*package*/ static class Assignment {

    private double originalx;
    private int layer = -1;
    private Vertex vertex;
    private int position = -1;
    private List<Assignment> outgoing = new ArrayList<Assignment>();
    private List<Assignment> incoming = new ArrayList<Assignment>();
    
    /**
     * A new vertex/layer assignment 
     */
   /*package*/ Assignment(Vertex vertex, int layer, double originalx) {
      this.originalx = originalx;
      this.vertex = vertex;
      this.layer = layer;
    }
   
    /**
     * A dummy vertex/layer assignment between two given assignments
     */
    /*package*/ Assignment(Assignment source, Assignment sink, int layer, double originalx) {
      this(DUMMY, layer, originalx);
      addOutgoing(sink);
      addIncoming(source);
     
      sink.incoming.remove(source);
      sink.incoming.add(this);
     
      source.outgoing.remove(sink);
      source.outgoing.add(this);
    }
    
    /*package*/ void addOutgoing(Assignment adjacent) {
      this.outgoing.add(adjacent);
    }
    
    /*package*/ void addIncoming(Assignment adjacent) {
      this.incoming.add(adjacent);
    }
    
    /*package*/ Vertex vertex() {
      return vertex;
    }
    
    /*package*/ boolean push(int layer) {
      if (this.layer>=layer)
        return false;
      this.layer = layer;
      return true;
    }
    
    /*package*/ List<Assignment> incoming() {
      return incoming;
    }
    
    /*package*/ List<Assignment> outgoing() {
      return outgoing;
    }
    
    /*package*/ List<Assignment> adjacents(int direction) {
      return direction<0 ? outgoing : incoming;
    }

    /*package*/ int pos() {
      return position;
    }
    
    /*package*/ int layer() {
      return layer;
    }
   
    @Override
    public String toString() {
      StringBuffer result = new StringBuffer();
      result.append("{");
      for (int i=0;i<incoming.size();i++) {
        if (i>0) result.append(",");
        result.append(incoming.get(i).vertex);
      }
      result.append("}");
      result.append(vertex);
      result.append("{");
      for (int i=0;i<outgoing.size();i++) {
        if (i>0) result.append(",");
        result.append(outgoing.get(i).vertex);
      }
      result.append("}");
      return result.toString();
    }
  } // Assignment

  public Iterator<Assignment> iterator() {
    return assignments.iterator();
  }
  
  private static class Dummy implements Vertex {
    @Override
    public String toString() {
      return "Dummy";
    }
    public Collection<? extends Edge> getEdges() {
      throw new IllegalArgumentException("n/a");
    }
  }
  
}
