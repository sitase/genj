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
package genj.renderer;

/**
 * Encapsulating name and html for rendering an entity */
public class Blueprint {
  
  /** the name of this scheme */
  private String name;
  
  /** the html of this scheme */
  private String html;
  
  /** read-only */
  private boolean isReadOnly = false;

  /**
   * Constructor - temporary blueprint w/o name
   */
  public Blueprint(String hTml) {
    html = hTml;
  }
    
  /**
   * Constructor - name, html and editable
   */
  /*package*/ Blueprint(String nAme, String hTml, boolean readOnly) {
    // remember
    name = nAme;
    html = hTml;
    isReadOnly = readOnly;
    // done
  }

  /**
   * Accessor - html
   */
  public void setHTML(String hTml) {
    // o.k.?
    if (isReadOnly()) 
      throw new IllegalArgumentException("Can't change read-only Blueprint");
    // remember
    html = hTml;
    // done
  }
  
  /**
   * Accessor - html
   */
  public String getHTML() {
    return html;
  }

  /**
   * Accessor - name
   */
  public String getName() {
    return name;
  }
  
  /**
   * Accessor - readonly
   */
  public boolean isReadOnly() {
    return isReadOnly;
  }
  
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    // type and null check
    if (o==null||(!(o instanceof Blueprint))) return false;
    // check
    Blueprint other = (Blueprint)o;
    return other.getName().equals(getName())&&other.getHTML().equals(getHTML());
  }

} //RenderingScheme
