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

import genj.edit.Images;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;
import genj.view.ViewManager;

/**
 * EDelete - delete an entity
 */  
public class DelEntity extends AbstractChange {
  
  /** the candidate to delete */
  private Entity candidate;
  
  /**
   * Constructor
   */
  public DelEntity(Entity entity, ViewManager manager) {
    super(entity.getGedcom(), Images.imgDelEntity, resources.getString("delete"), manager);
    candidate = entity;
  }
  
  /**
   * @see genj.edit.EditViewFactory.Change#getConfirmMessage()
   */
  protected String getConfirmMessage() {
    // You are about to delete {0} of type {1} from {2}! Deleting this ...
    return resources.getString("confirm.del", new String[] { 
      candidate.toString(), Gedcom.getName(candidate.getTag(),false), gedcom.getName() 
    });
  }

  /**
   * @see genj.edit.EditViewFactory.Change#change()
   */
  protected void change() throws GedcomException {
    candidate.getGedcom().deleteEntity(candidate);
  }
  
} //DelEntity
