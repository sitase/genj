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


/**
 * Gedcom Property : SUBMISSION 
 * A property that either consists of SUBMISSION information or
 * refers to a SUBMISSION entity
 */
public class PropertySubmission extends PropertyXRef {

  /** applicable target types */
  public final static int[] 
    TARGET_TYPES = new int[]{ Gedcom.SUBMISSIONS };

  /**
   * Empty Constructor
   */
  public PropertySubmission() {
  }
  
  /**
   * Constructor with reference
   * @param entity reference of entity this property links to
   */
  public PropertySubmission(PropertyXRef target) {
    super(target);
  }

  /**
   * Returns the tag of this property
   */
  public String getTag() {
    return "SUBN";
  }

  /**
   * Links reference to entity (if not already done)
   * @exception GedcomException when processing link would result in inconsistent state
   */
  public void link() throws GedcomException {

    // something to do ?
    if (getReferencedEntity()!=null) return;

    // Look for SUBN
    String id = getReferencedId();
    if (id.length()==0) return;

    Submission subm = (Submission)getGedcom().getEntity(id, Gedcom.SUBMISSIONS);
    if (subm == null) 
      return;

    // Create Backlink
    PropertyForeignXRef fxref = new PropertyForeignXRef(this);
    subm.addProperty(fxref);

    // ... and point
    setTarget(fxref);

    // Done
  }

  /**
   * The expected referenced type
   */
  public int[] getTargetTypes() {
    return TARGET_TYPES;
  }
  
  /**
   * @see genj.gedcom.Submission#isValid()
   */
  public boolean isValid() {
    // always
    return true;
  }

} //PropertySubmission

