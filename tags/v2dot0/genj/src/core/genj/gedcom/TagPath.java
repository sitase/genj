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
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Class for encapsulating a path of tags that describe the way throug
 * a tree of properties. An example for a path is TagPath("INDI:BIRT:DATE")
 * which denotes the <em>date</em> of property <em>birth</em> of an
 * individual.
 * @author  Nils Meier
 * @version 0.1 04/21/98
 */
public class TagPath {

  /** the list of tags that describe the path */
  private String tags[];
  
  /** the hash of this path (immutable) */
  private int hash = 0;

  /**
   * Constructor for TagPath
   * @param path path as colon separated string value a:b:c
   * @exception IllegalArgumentException in case format isn't o.k.
   */
  public TagPath(String path) throws IllegalArgumentException {

    // Parse path
    StringTokenizer tokens = new StringTokenizer(path,":",false);
    int length = tokens.countTokens();
    if (length==0)
      throw new IllegalArgumentException("No valid path :"+path);

    // ... setup data
    tags = new String[length];
    for (int i=0;i<length;i++) {
      tags[i] = tokens.nextToken();
      hash += tags[i].hashCode();
    }

    // Done
  }
  
  /**
   * Constructor for TagPath
   */
  public TagPath(Property[] props) {
    
    // decompose property path
    tags = new String[props.length];

    // grab prop's tags and hash
    for (int i=0; i<tags.length; i++) {
      tags[i] = props[i].getTag();
      hash += tags[i].hashCode();
    }
    
    // done
  }
    
  /**
   * Constructor for TagPath
   */
  public TagPath(TagPath other, String tag) {
    tags = new String[other.tags.length+1];
    System.arraycopy(other.tags, 0, tags, 0, other.tags.length);
    tags[tags.length-1] = tag;
    hash = other.hash+tag.hashCode();
  }

  /**
   * Constructor for TagPath
   */
  public TagPath(TagPath other, int len) {
    // copyup to len and rehash
    tags = new String[len];
    for (int i=0; i<tags.length; i++) {
      tags[i] = other.tags[i];
      hash += tags[i].hashCode();
    }
    // done
  }

  /**
   * Constructor for TagPath
   * @param path path as colon separated string value a:b:c
   * @exception IllegalArgumentException in case format isn't o.k.
   */
  public TagPath(Stack path) throws IllegalArgumentException {
    // grab stack elements
    tags = new String[path.size()];
    for (int i=0; i<tags.length; i++) {
      tags[i] = path.get(i).toString();
      hash += tags[i].hashCode();
    }
    // done
  }
  
  /**
   * Wether this path starts with prefix
   */
  public boolean startsWith(TagPath prefix) {
    // not if longer
    if (prefix.tags.length>tags.length) 
      return false;
    // check
    for (int i=0;i<prefix.tags.length;i++) {
      if (!tags[i].equals(prefix.tags[i])) return false;
    }
    // yes
    return true;
  }

  /**
   * Returns comparison between two TagPaths
   */
  public boolean equals(Object obj) {

    // Me ?
    if (obj==this) {
      return true;
    }

    // TagPath ?
    if (!(obj instanceof TagPath)) {
      return false;
    }

    // Size ?
    TagPath other = (TagPath)obj;
    if (other.tags.length!=tags.length) {
      return false;
    }

    // Elements ?
    for (int i=0;i<tags.length;i++) {
      if (!tags[i].equals(other.tags[i])) {
        return false;
      }
    }

    // Equal
    return true;
  }

  /**
   * Returns the n-th tag of this path
   * @param which 1-based number
   * @return tag as <code>String</code>
   */
  public String get(int which) {
    return tags[which];
  }

  /**
   * Returns the last tag of this path
   * @return last tag as <code>String</code>
   */
  public String getLast() {
    return tags[tags.length-1];
  }

  /**
   * Returns the length of this path
   * @return length of this path
   */
  public int length() {
    return tags.length;
  }
  
  /**
   * Returns the path as a string
   */
  public String toString() {
    String result = tags[0];
    for (int i=1;i<tags.length;i++)
      result = result + ":" + tags[i];
    return result;
  }
  
  /**
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return hash;
  }

  /**
   * Resolve a path from given property
   */
  public static TagPath get(Property prop) {
    
    String p = prop.getTag();
    while (!(prop instanceof Entity)) {
      prop = prop.getParent();
      p = prop.getTag() + ":" + p;
    }
    
    // done
    return new TagPath(p);
  }

  /**
   * Simple test for path : contains ':'
   */
  public static boolean isPath(String path) {
    return path.indexOf(':')>0;
  }

  /**
   * Get an array out of collection
   */
  public static TagPath[] toArray(Collection c) {
    return (TagPath[])c.toArray(new TagPath[c.size()]);
  }

  /**
   * Filter TagPath by entities
   */
  public static TagPath[] filter(TagPath[] paths, int entity) {
    List result = new ArrayList(paths.length);
    String tag = Gedcom.getTagFor(entity);
    for (int i=0; i<paths.length; i++) {
    	if (paths[i].get(0).equals(tag)) 
        result.add(paths[i]);
    }
    return toArray(result);
  }

} //TagPath