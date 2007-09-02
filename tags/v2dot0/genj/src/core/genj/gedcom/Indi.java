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
import java.util.Calendar;
import java.util.List;

import genj.util.WordBuffer;
import genj.util.swing.ImageIcon;

/**
 * Class for encapsulating a person
 */
public class Indi extends Entity {

  private final static ImageIcon
    IMG_MALE   = MetaProperty.get(new TagPath("INDI")).getImage("male"),
    IMG_FEMALE = MetaProperty.get(new TagPath("INDI")).getImage("female"),
    IMG_UNKNOWN = MetaProperty.get(new TagPath("INDI")).getImage();
    
  /**
   * Adds a family in which the individual is a partner
   */
  /*package*/ Fam addFam() throws GedcomException {
    return addFam((Fam)getGedcom().createEntity(Gedcom.FAMILIES, null));
  }

  /**
   * Adds a family in which the individual is a partner
   */
  /*package*/ Fam addFam(Fam fam) throws GedcomException {

    // Remember Fam where this is spouse in
    PropertyFamilySpouse pfs = new PropertyFamilySpouse(fam.getId());
    addProperty(pfs);

    // Link !
    try {
      pfs.link();
    } catch (GedcomException ex) {
      delProperty(pfs);
    }

    return fam;
  }

  /**
   * Deletes a family in which the person was a partner
   */
  /*package*/ Indi delFam(int which ) {
    Property[] fams = getProperties(new TagPath("INDI:FAMS"),true);
    if (which > fams.length)
      throw new IllegalArgumentException("Individual isn't spouse in "+which+" families");
    delProperty(fams[which-1]);
    return this;
  }

  /**
   * Deletes the family in which the Individual was child
   */
  /*package*/ Indi delFamc() {
    Property prop = getProperty(new TagPath("INDI:FAMC"),true);
    if (prop==null) {
      return this;
    }
    delProperty(prop);
    return this;
  }

  /**
   * Calculate indi's birth date
   */
  public PropertyDate getBirthDate() {

    // Calculate BIRT|DATE
    PropertyDate p = (PropertyDate)getProperty(new TagPath("INDI:BIRT:DATE"),true);
    if (p==null) {
      return null;
    }

    // Return string value
    return p;
  }

  /**
   * Calculate indi's death date
   */
  public PropertyDate getDeathDate() {

    // Calculate DEAT|DATE
    PropertyDate p = (PropertyDate)getProperty(new TagPath("INDI:DEAT:DATE"),true);
    if (p==null) {
      return null;
    }

    // Return string value
    return p;
  }
  
  /**
   * Calculate the 'younger' sibling
   */
  public Indi getOlderSibling() {
    
    // this is a child in a family?
    Fam f = getFamc();
    if (f==null) return null;
    
    // what are the children of that one
    Indi[] cs = f.getChildren();
    for (int c=0;c<cs.length;c++) {
      if (cs[c]==this) return (c>0) ? cs[c-1] : null;
    }
    
    // there's no previous one
    return null;
  }
  
  /**
   * Calculate the 'older' sibling
   */
  public Indi getYoungerSibling() {
    
    // this is a child in a family?
    Fam f = getFamc();
    if (f==null) return null;
    
    // what are the children of that one
    Indi[] cs = f.getChildren();
    for (int c=cs.length-1;c>=0;c--) {
      if (cs[c]==this) return (c<cs.length-1) ? cs[c+1] : null;
    }
    
    // there's no previous one
    return null;
  }
  
  /** 
   * Calculate indi's partners. The number of partners can be
   * smaller than the number of families this individual is
   * part of because spouses in families don't have to be defined.
   */
  public Indi[] getPartners() {
    // Look at all families and remember spouses
    Fam[] fs = getFamilies();
    List l = new ArrayList(fs.length);
    for (int f=0; f<fs.length; f++) {
      Indi p = fs[f].getOtherSpouse(this);
      if (p!=null) l.add(p);
    }
    // Return result
    Indi[] result = new Indi[l.size()];
    l.toArray(result);
    return result;
  }
  
  /**
   * Calculate indi's children
   */
  public Indi[] getChildren() {
    // Look at all families and remember children
    Fam[] fs = getFamilies();
    List l = new ArrayList(fs.length);
    for (int f=0; f<fs.length; f++) {
      Indi[]cs = fs[f].getChildren();
      for (int c=0;c<cs.length;c++) l.add(cs[c]);
    }
    // Return result
    Indi[] result = new Indi[l.size()];
    l.toArray(result);
    return result;
  }
  
  /** 
   * Calculate indi's father
   */
  public Indi getFather() {
    // have we been child in family?
    Fam f = getFamc();
    if (f==null) return null;
    // ask fam
    return f.getHusband();
  }

  /** 
   * Calculate indi's mother
   */
  public Indi getMother() {
    // have we been child in family?
    Fam f = getFamc();
    if (f==null) return null;
    // ask fam
    return f.getWife();
  }

  /**
   * Calculate indi's birth date
   */
  public String getBirthAsString() {

    PropertyDate p = getBirthDate();
    if (p==null) {
      return "";
    }

    // Return string value
    return p.toString();
  }

  /**
   * Calculate indi's death date
   */
  public String getDeathAsString() {

    PropertyDate p = getDeathDate();
    if (p==null) {
      return "";
    }

    // Return string value
    return p.toString();
  }

  /**
   * Returns the selected family in which the individual is a partner
   */
  public Fam getFam(int which) {
    Property[] props = getProperties("FAMS", true);
    if (which>=props.length) {
      return null;
    }
    return ((PropertyFamilySpouse)props[which]).getFamily();
  }
  
  /**
   * Get Family with option to create
   */
  /*package*/ Fam getFam(boolean create) throws GedcomException {
    Fam fam = getFam(0);
    if (fam!=null||!create) return fam;
    fam = (Fam)getGedcom().createEntity(Gedcom.FAMILIES, null);
    if (getSex()==PropertySex.FEMALE) fam.setWife(this);
    else fam.setHusband(this);
    return fam;    
  }

  /**
   * Returns the family in which the person is child
   */
  public Fam getFamc( ) {
    Property prop = getProperty("FAMC",true);
    if (prop==null) {
      return null;
    }
    return ((PropertyFamilyChild)prop).getFamily();
  }

  /**
   * Get Family with option to create
   */
  public Fam getFamc(boolean create) throws GedcomException {
    Fam fam = getFamc();
    if (fam!=null||!create) return fam;
    fam = (Fam)getGedcom().createEntity(Gedcom.FAMILIES, null);
    fam.addChild(this);
    return fam;    
  }

  /**
   * Returns indi's first name
   */
  public String getFirstName() {

    // Calculate NAME
    PropertyName p = (PropertyName)getProperty("NAME",true);
    if (p==null) {
      return "";
    }

    // Return string value
    return p.getFirstName();
  }

  /**
   * Calculate indi's last name
   */
  public String getLastName() {

    // Calculate NAME
    PropertyName p = (PropertyName)getProperty("NAME",true);
    if (p==null) {
      return "";
    }

    // Return string value
    return p.getLastName();
  }

  /**
   * Returns indi's name
   */
  public String getName() {

    // Calculate NAME
    Property name = getProperty("NAME",true);
    if (name instanceof PropertyName)
      return ((PropertyName)name).getName();  
    return "";
  }
  
  /** 
   * Returns the number of parents this individual has
   */
  public int getNoOfParents() {
    Fam fam = getFamc();
    return fam==null?0:fam.getNoOfSpouses();
  }

  /**
   * Returns the number of families in which the individual is a partner
   */
  public int getNoOfFams() {
    return getProperties("FAMS",true).length;
  }
  
  /**
   * Returns the families in which this individual is a partner
   */
  public Fam[] getFamilies() {
    Property[] props = getProperties("FAMS",true);
    Fam[] result = new Fam[props.length];
    for (int f=0; f<result.length; f++) {
      result[f] = ((PropertyFamilySpouse)props[f]).getFamily();
    }    
    return result;
  }

  /**
   * Returns indi's sex
   */
  public int getSex() {
    PropertySex p = (PropertySex)getProperty("SEX",true);
    return p!=null ? p.getSex() : PropertySex.UNKNOWN;
  }
  
  /**
   * Set indi's sex
   */
  public void setSex(int sex) {
    PropertySex p = (PropertySex)getProperty("SEX",true);
    if (p==null) p = (PropertySex)addProperty(new PropertySex());
    p.setSex(sex);
  }

  /**
   * Checks wether this individual is descendant of individual
   */
  /*package*/ boolean isDescendantOf(Indi indi) {

    // Me ?
    if (this==indi) {
      return true;
    }

    // Childhood ?
    Fam fam = getFamc();
    if (fam==null) {
      return false;
    }

    // Recursive call
    return fam.isDescendantOf(indi);
  }


  /**
   * Checks wether this individual is descendant of family 
   */
  /*package*/ boolean isDescendantOf(Fam fam) {
    // fam's children
    Indi[] children = fam.getChildren();
    for (int c=0; c<children.length; c++) {
      if (isDescendantOf(children[c])) return true;
    }
    return false;
  }

  /**
   * Sets the family in which the person is child
   */
  /*package*/ Indi setFamc(Fam fam) throws GedcomException {

    // Remove old
    Property p = getProperty("FAMC",true);
    if (p!=null) {
      delProperty(p);
    }

    // Remember new Fam where this is child in
    PropertyFamilyChild pfc = new PropertyFamilyChild(fam.getId());
    addProperty(fam);

    // Link !
    try {
      pfc.link();
    } catch (GedcomException ex) {
      delProperty(pfc);
    }

    return this;
  }

  /**
   * Returns this entity as String description
   */
  public String toString() {
    String name = getName();
    return name.length()>0 ? name : super.toString();
  }

  /**
   * Calculate indi's age at given date
   * @param pEnd the date at which to calc age (or null for *now*)
   * @return age as a string description or null
   */
  public String toAgeString(PropertyDate pEnd) {

    // try to get birth    
    PropertyDate birth = getBirthDate();
    if (birth==null)
      return null;
    PropertyDate.PointInTime pit = birth.getStart();
    if (!pit.isValid())
      return null;
      
    int 
      ythen = pit.getYear(),
      mthen = Math.max(0,pit.getMonth()),
      dthen = Math.max(0,pit.getDay  ());
   
    // age at what point in time?
    int ynow, mnow, dnow;
    if (pEnd==null) {
      Calendar now = Calendar.getInstance(); // default to current time
      ynow = now.get(Calendar.YEAR);      
      mnow = now.get(Calendar.MONTH);      
      dnow = now.get(Calendar.DATE)-1;      
    } else {
      PropertyDate.PointInTime at = pEnd.getStart();
      if (!at.isValid()) return null;
      ynow = at.getYear();
      mnow = Math.max(0, at.getMonth());
      dnow = Math.max(0, at.getDay  ());
    }
    
    // calculate deltas
    int ydelta = ynow - ythen;
    int mdelta = mnow - mthen;
    int ddelta = dnow - dthen;
    
    // check day
    if (ddelta<0) {
      // decrease months
      mdelta -=1;
      // increase days with days in previous month
      //ddelta +=30;


      Calendar c = Calendar.getInstance();
      c.set(ythen, mthen, 1);
      int days = c.getActualMaximum(Calendar.DATE);
      ddelta = dnow + (days-dthen); 

    }

    // check month now<then
    if (mdelta<0) {
      // decrease years
      ydelta -=1;
      // increase months
      mdelta +=12;
    } 

    // calculate output
    WordBuffer buffer = new WordBuffer();
    if (ydelta>0) buffer.append(ydelta+"y");
    if (mdelta>0) buffer.append(mdelta+"m");
    if (ddelta>0) buffer.append(ddelta+"d");
    
    return buffer.toString();    
  }

  /**
   * Image
   */
  public ImageIcon getImage(boolean checkValid) {
    // check sex (no need to check valid here)
    switch (getSex()) {
      case PropertySex.MALE: return IMG_MALE;
      case PropertySex.FEMALE: return IMG_FEMALE;
      default: return IMG_UNKNOWN;
    }
  }
  
} //Indi