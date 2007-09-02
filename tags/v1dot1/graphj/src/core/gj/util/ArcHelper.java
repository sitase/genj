/**
 * GraphJ
 * 
 * Copyright (C) 2002 Nils Meier
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package gj.util;

import gj.awt.geom.Geometry;
import gj.awt.geom.Path;
import gj.model.Arc;
import gj.model.Node;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Iterator;

/**
 * A simplified layout for arcs
public class ArcHelper {

  /**
   * Updates a path of an arc
   */
  public static Path update(Arc arc) {
    return update(arc.getPath(), arc.getStart(), arc.getEnd());
  }
  
  /**
   * Updates a set of arcs
   */
  public static void updateArcs(Collection arcs) {
    Iterator it = arcs.iterator();
    while (it.hasNext()) update((Arc)it.next());
  }

  /**
   * Updates a simple path between two nodes
   */
  public static Path update(Path path, Node start, Node end) {
    return update(path,start.getPosition(),start.getShape(),end.getPosition(),end.getShape());
  }

  /**
   * Updates given path with a line going through points between two shapes
   * @param path the path to update
   * @param points a sequence of points describing the path
   * @param s1 shape positioned at the first point
   * @param s2 shape positioned at the last point
   */  
  public static Path update(Path path, Point2D[] points, Shape s1, Shape s2) {
    
    // clean things up initially
    path.reset();
    
    // intersect the first segment with s1
    Point2D
      a = Geometry.getIntersection(points[1], points[0], points[0], s1),
      b = Geometry.getIntersection(points[points.length-2], points[points.length-1], points[points.length-1], s2);
    
    // add the points to this path
    path.moveTo(a);
    for (int i=1;i<points.length-1;i++) {
      path.lineTo(points[i]);
    }
    path.lineTo(b);
    
    // done
    return path;
  }

  /**
   * Updates given path with a line between given points between two shapes
   * @param p1 the starting point
   * @param s1 the shape sitting at p1
   * @param p2 the ending point
   * @param s2 the shape sitting at p2
   */
  public static Path update(Path path, Point2D p1, Shape s1, Point2D p2, Shape s2) {
    
    // clean things up initially
    path.reset();
    
    // A loop for p1==p2
    if (p1.equals(p2)) {
      
      Rectangle2D bounds = s1.getBounds2D();

      double 
        w = bounds.getMaxX()+bounds.getWidth()/4,
        h = bounds.getMaxY()+bounds.getHeight()/4;

      Point2D
        a = p1,
        b = new Point2D.Double(a.getX()+w, a.getY()  ),
        c = new Point2D.Double(a.getX()+w, a.getY()+h),
        d = new Point2D.Double(a.getX()  , a.getY()+h);
        
      update(path,new Point2D[]{a,b,c,d,a}, s1, s1);
      
      return path;
    }

    // A simple line
    path.moveTo(Geometry.getIntersection(p2, p1, p1, s1));
    path.lineTo(Geometry.getIntersection(p1, p2, p2, s2));
   
    // done
    return path; 
  }

} //ArcLayout