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
package genj.edit.actions;

import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;
import genj.gedcom.Indi;
import genj.gedcom.Property;
import genj.gedcom.PropertyChild;
import genj.view.ViewManager;

/**
 * Create a child of a family or person
 */
public class CreateChild extends CreateRelationship {
  
  /** the parent  or family we're creating a child for */
  private Entity parentOrFamily;
  
  /** constructor */
  public CreateChild(Fam family, ViewManager mgr) {
    super(resources.getString("create.child"), family.getGedcom(), Gedcom.INDI, mgr);
    this.parentOrFamily = family;
  }
  
  /** constructor */
  public CreateChild(Indi parent, ViewManager mgr) {
    super(resources.getString("create.child"), parent.getGedcom(), Gedcom.INDI, mgr);
    this.parentOrFamily = parent;
  }

  /** description of what this'll do */
  public String getDescription() {
    // "Child of Meier, Sven (I1)"
    if (parentOrFamily instanceof Indi)
      return resources.getString("create.child.of", parentOrFamily);
    // "Child in Meier, Sven (I1) + Radovcic Sandra (I2) (F1)"
    return resources.getString("create.child.in", parentOrFamily);
  }

  /** a warning in case the target indi is already a child of another family */
  public String getWarning(Entity indi) {
    
    // existing individual choosen?
    if (indi!=null) { 
      // check biological parents
      Fam fam = ((Indi)indi).getFamilyWhereBiologicalChild();
      if (fam!=null)
        return PropertyChild.getLabelChildAlreadyinFamily((Indi)indi, fam);
    }
    
    // no prob
    return null;
  }

  /** change impl */
  protected Property change(Entity target, boolean targetIsNew) throws GedcomException {
    
    // cast to what we now is the child now
    Indi child = (Indi)target;
    Gedcom ged = child.getGedcom();
    
    // are we adding that to an indi?
    Fam family;
    if (parentOrFamily instanceof Indi) {

      Indi parent = (Indi)parentOrFamily;
      
      // lookup family for child    
      Fam[] fams = parent.getFamiliesWhereSpouse();
      if (fams.length>0) {
        // add child to first fam
        family = fams[0];
        family.addChild(child);
      } else {
        // create a new fam
        family = (Fam)ged.createEntity(Gedcom.FAM);
        try {
          family.addChild(child);
        } catch (GedcomException e) {
          ged.deleteEntity(family);
          throw e;
        }
        // set spouse
        family.setSpouse(parent);
        // 20040619 adding missing spouse automatically now
        family.setSpouse((Indi)ged.createEntity(Gedcom.INDI).addDefaultProperties());
      }
      
    } else {
      
      // add child to family
      family = (Fam)parentOrFamily;
      family.addChild(child);
    
    }
    
    // set name of child if it's a new individual
    if (targetIsNew) {
      Indi parent  = family.getHusband();
      if (parent==null) parent = family.getWife();
      if (parent!=null)
        child.setName("", parent.getLastName());
    }
    
    // focus stays with parent or family
    return parentOrFamily;
  }

}
