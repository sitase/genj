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

import genj.util.ReferenceSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gedcom Property : simple value with choices
 */
public class PropertyChoiceValue extends PropertySimpleValue {

  /**
   * need tag-argument constructor for all properties
   */
  public PropertyChoiceValue(String tag) {
    super(tag);
  }
  
  /**
   * Remember a value
   */
  protected boolean remember(String oldValue, String newValue) {
    // transient or no access to containing gedcom? 
    Gedcom gedcom = getGedcom();
    if (isTransient||gedcom==null)
      return false;
    ReferenceSet<String, Property> refSet = gedcom.getReferenceSet(getTag());
    // intern newValue - we expect the remembered values to be shared so we share the string instances for an upfront cost
    newValue = newValue.intern();
    // forget old
    if (oldValue.length()>0) refSet.remove(oldValue, this);
    // remember new
    if (newValue.length()>0) refSet.add(newValue, this);
    // done
    return true;
  }
  
  /**
   * Returns all choices in same gedcom file as this
   */
  public List<String> getChoices(boolean sort) {
    // got access to a reference set?
    Gedcom gedcom = getGedcom();
    if (gedcom==null)
      return Collections.EMPTY_LIST;
    return getChoices(gedcom, getTag(), sort);
  }
  
  public List<String> getDefaults() {
    return getDefaults(getTag());
  }
  
  /**
   * Returns all choices for given property tag
   */
  public static List<String> getChoices( Gedcom gedcom, String tag, boolean sort) {
	return new ArrayList<String>(gedcom.getReferenceSet(tag).getKeys(sort ? gedcom.getCollator() : null));
  }
  
  /**
   * Returns the default vals for the given tag
   */
  public static List<String> getDefaults(String tag) {
    List<String> result = new ArrayList<String>(10);
    String defaults = Gedcom.resources.getString(tag + ".vals", false);
    if (defaults != null) {
      for (String token : defaults.split(",")) {
        result.add(token.trim());
      }
    }
    return result;
  }

  /**
   * Returns all properties with given tag, optionally sorted by the value of the tag
   */
  public static Property[] getSameChoices(Gedcom gedcom, String tag, boolean sort) {
    
    // lookup choices
    ReferenceSet<String, Property> references = gedcom.getReferenceSet(tag);
    List<String> choices = references.getKeys(sort ? gedcom.getCollator() : null);

    // grab 'em all
    List<Property> result = new ArrayList<Property>(choices.size());
    for (String choice : choices) 
      result.addAll(references.getReferences(choice));
    
    // done
    return Property.toArray(result);
    
  }
  
  /**
   * Returns all Properties that contain the same value
   */
  public Property[] getSameChoices() {
    // got access to a reference set?
    Gedcom gedcom = getGedcom();
    if (gedcom==null)
      return new Property[0];
    ReferenceSet<String, Property> refSet = gedcom.getReferenceSet(getTag());
    // convert
    return toArray(refSet.getReferences(super.getValue()));
  }
  
  /**
   * @see genj.gedcom.PropertySimpleValue#setValue(java.lang.String)
   */
  public void setValue(String value) {
    // TUNING: for choices we expect a lot of repeating values so
    // we build the intern representation of value here - this makes
    // us share string instances for an upfront cost
    setValueInternal(value.intern());
  }
  
  /**
   * A special value that allows global substitution
   */
  public void setValue(String value, boolean global) {
    
    // more?
    if (global) {
      // change value of all with value
      Property[] others = getSameChoices();
      for (int i=0;i<others.length;i++) {
        Property other = others[i];
        if (other instanceof PropertyChoiceValue&&other!=this) 
          ((PropertyChoiceValue)other).setValue(value);
      }
    }    
      
    // change me
    setValue(value);
    
    // done
  }

  private void setValueInternal(String value) {
    // remember
    remember(super.getValue(), value);
    // delegate
    super.setValue(value);
  }

  /**
   * @see genj.gedcom.Property#addNotify(genj.gedcom.Property)
   */
  /*package*/ void afterAddNotify() {
    // delegate
    super.afterAddNotify();
    // a remember wouldn't have worked until now
    remember("", super.getValue());
    // done
  }

  /**
   * Removing us from the reference set (our value is not used anymore)
   * @see genj.gedcom.PropertyRelationship#delNotify()
   */
  /*package*/ void beforeDelNotify() {
    // forget value
    remember(super.getValue(), "");
    // continue
    super.beforeDelNotify();
  }

} //PropertyChoiceValue
