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

import genj.util.swing.ImageIcon;

/**
 * Gedcom Property : SEX
 */
public class PropertySex extends Property {
  
  /** images */
  private final static ImageIcon
    IMG_MALE   = MetaProperty.get(new TagPath("INDI:SEX")).getImage("male"),
    IMG_FEMALE = MetaProperty.get(new TagPath("INDI:SEX")).getImage("female");

  /** sexes */
  private static final int UNKNOWN = -1;
  public static final int MALE    = 1;
  public static final int FEMALE  = 2;

  /** the sex code */
  private int sex;

  /** the sex as string (unknown code) */
  private String sexAsString;

  /**
   * Constructor for Gedcom Sex Line
   */
  public PropertySex() {
    // Setup data
    setSex(UNKNOWN);
    // Done
  }

  /**
   * Constructor for Gedcom Sex Line
   */
  public PropertySex(int setSex) {
    // Setup data
    sex = setSex;
    // Done
  }

  /**
   * Constructor for Gedcom Sex Line
   */
  public PropertySex(String tag, String value) {
    // Setup data
    if (value.length() == 0) {
      setSex(UNKNOWN);
    } else {
      setValue(value);
    }
    // Done
  }

  /**
   * Image
   */
  public static ImageIcon getDefaultImage(int sex) {
    switch (sex) {
      case MALE: return IMG_MALE;
      case FEMALE: return IMG_FEMALE;
    }
    throw new IllegalArgumentException("Unknown sex");
  }

  /**
   * Image
   */
  public ImageIcon getImage(boolean checkValid) {
    // validity?
    if (checkValid&&(!isValid()))
      return super.getImage(true);
    // check it
    switch (sex) {
      case MALE: return IMG_MALE;
      case FEMALE: return IMG_FEMALE;
      default:
        return super.getImage(checkValid);
    }
  }

  /**
   * Returns <b>true</b> if this property is valid
   */
  public boolean isValid() {
    return (sexAsString==null);
  }


  /**
   * Returns localized label for sex
   */
  static public String getLabelForSex() {
    return Gedcom.getResources().getString("prop.sex");
  }

  /**
   * Returns localized label for sex of male/female
   */
  static public String getLabelForSex(int which) {
    if (which==MALE)
      return Gedcom.getResources().getString("prop.sex.male");
    return Gedcom.getResources().getString("prop.sex.female");
  }

  /**
   * Returns the logical proxy to render/edit this property
   */
  public String getProxy() {
    if (sexAsString!=null)
      return "Unknown";
    return "Sex";
  }

  /**
   * Accessor for Sex
   */
  public int getSex() {
    return sex;
  }

  /**
   * Accessor for Tag
   */
  public String getTag() {
    return "SEX";
  }

  /**
   * Accessor for Value
   */
  public String getValue() {
    if (sexAsString != null)
      return sexAsString;
    if (sex == MALE)
      return "M";
    if (sex == FEMALE)
      return "F";
    return "";
  }

  /**
   * Accessor for Sex
   */
  public void setSex(int newSex) {
    noteModifiedProperty();
    sexAsString = null;
    sex = newSex;
    // Done
  }

  /**
   * Accessor for Value
   */
  public void setValue(String newValue) {

    noteModifiedProperty();

    // No information ?
    if (newValue.length()!=1) {
      sexAsString=newValue;
      // Done
      return;
    }
    // Female or Male ?
    switch (newValue.charAt(0)) {
      case 'f' :
      case 'F' :
        sex = FEMALE;
        sexAsString=null;
        return;
      case 'm' :
      case 'M' : 
        sex = MALE;
        sexAsString=null;
        return;
    }
    // Done
    sexAsString=newValue;
    // Done
  }

  /**
   * Tester for validity of sex
   */
  public static boolean isSex(int tst) {
    return tst==MALE||tst==FEMALE;
  }

  /**
   * Calculates opposite sex
   */
  public static int calcOppositeSex(int from, int fallback) {
    if (from==MALE)
      return FEMALE;
    if (from==FEMALE)
      return MALE;
    return fallback;
  }

  /**
   * Calculates opposite sex
   */
  public static int calcOppositeSex(Indi from, int fallback) {

    // Something to base calculation on?
    if (from==null) {
      return fallback;
    }

    // Check other's sex
    return calcOppositeSex(from.getSex(), fallback);

  }
}
