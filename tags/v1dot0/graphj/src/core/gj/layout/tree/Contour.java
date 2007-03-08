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
package gj.layout.tree;

/**
 * A Contour describes an area in a latitude/longitude space
 * which extends from a north to south with a west and east
 * extension. An example is
 * <pre>
 *     lon*gi*tude
 *   l     +-+
 *   a     | |          North
 *   t   +-+ +---+        A
 *   i   |       |  West <+> East
 *   t +-+       |        V
 *   u |         |      South
 *   d +---------+
 *   e
 * </pre>
 * A data representation for this example is
 * <pre>
 *     lon*gi*tude
 *   l .....0.....
 *   a    -1 +1         North
 *   t .....1.....        A
 *   i  -2      +3  West <+> East
 *   t .....2.....        V
 *   u-3        +3      South
 *   d .....3.....
 *   e
 * </pre>
 */
/*package*/ class Contour {
  
  protected final static int 
    WEST = 0,
    EAST = 1;
        
  /*package*/ int 
    north,
    south,
    west ,
    east ;
    
  private int
    dlat = 0,
    dlon = 0;    
    
  private int[][] data = null;
  private int  [] size = null;

  /**
   * Constructor
   */    
  /*package*/ Contour(double n, double w, double e, double s) {
    // keep n,w,e,s
    north = (int)Math.floor(n);
    west  = (int)Math.floor(w);
    east  = (int)Math.ceil (e);
    south = (int)Math.ceil (s);
    // done
  }
  
  /**
   * Merge
   */    
  /*package*/ static Contour merge(Contour[] contours) {
    
    // validity check?
    if (contours.length==0)
      throw new IllegalArgumentException("zero-length list n/a");
      
    // performance improvement ? 
    // - one-size list is trivial
    // - two-size list with [0]==[1] is trivial
    if (contours.length==1||(contours.length==2&&contours[0]==contours[1]))
      return contours[0];
      
    // create a new result
    Contour result = new Contour(0, Integer.MAX_VALUE, Integer.MIN_VALUE, 0);
  
    // prepare some data
    int demand = 0;
    for (int c=0;c<contours.length;c++) {
      if (contours[c].size==null) demand+=2;
      else demand+=Math.max(contours[c].size[EAST],contours[c].size[WEST]);
    }
    result.data = new int[2][demand];
    result.size = new int[2];
    
    // take east
    east: for (int c=contours.length-1;c>=0;c--) {
      Iterator it = contours[c].getIterator(EAST);
      if (c==contours.length-1) result.north = it.north;
      else while (it.south<=result.south) if (!it.next()) continue east;
      do {
        result.east = Math.max(result.east,it.longitude);
        result.data[EAST][result.size[EAST]++] = it.longitude;
        result.data[EAST][result.size[EAST]++] = it.south;
        result.south = it.south;
      } while (it.next());
    }
    result.size[EAST]--;
    
    // take west
    west: for (int c=0;c<contours.length;c++) {
      Iterator it = contours[c].getIterator(WEST);
      if (c==0) result.north = it.north;
      else while (it.south<=result.south) if (!it.next()) continue west;
      do {
        result.west = Math.min(result.west,it.longitude);
        result.data[WEST][result.size[WEST]++] = it.longitude;
        result.data[WEST][result.size[WEST]++] = it.south;
        result.south = it.south;
      } while (it.next());
    }
    result.size[WEST]--;
    
    // done
    return result;
  }
  
  /**
   * Pad
   */
  /*package*/ void pad(int[] pad) {
    north -= pad[0];
    west  -= pad[1];
    east  += pad[2];
    south += pad[3];
  }
  
  /**
   * Translate
   */
  /*package*/ void translate(int dlat, int dlon) {
    north += dlat;
    south += dlat;
    west  += dlon;
    east  += dlon;
    this.dlat += dlat;
    this.dlon += dlon;
  }

  /**
   * Helper to get an iterator
   */
  /*package*/ Iterator getIterator(int side) {
    return new Iterator(this,side);
  }
    
  /**
   * ContourIterator
   */
  /*package*/ static class Iterator {
    
    private Contour contour;
    private int i,side;
    
    /*package*/ int
      north,
      longitude,
      south;
    
    /**
     * Constructor
     */
    private Iterator(Contour c, int s) {
      
      contour = c;
      side = s;
      
      if (contour.size==null) {
        // simplest case - no data!
        north = contour.north;
        longitude = s==WEST ? contour.west : contour.east;
        south = contour.south;
      } else {
        // grab first from data
        south = contour.north;
        i = 0;
        next();
      }
    }

    /**
     * Set to next
     */
    /*package*/ boolean next() {
      
      // out of data?
      if ((contour.size==null)||(i>=contour.size[side]))
        return false;
        
      // move forward
      this.north = this.south;
      this.longitude = contour.dlon + contour.data[side][i++];
      if (i==contour.size[side])
        this.south = contour.south;
      else
        this.south = contour.dlat + contour.data[side][i++];

      // done
      return true;      
    }
    
  } //ContourIterator

} //Contour
