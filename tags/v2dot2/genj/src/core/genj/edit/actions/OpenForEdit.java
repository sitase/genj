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

import genj.edit.EditView;
import genj.edit.EditViewFactory;
import genj.edit.Images;
import genj.gedcom.Entity;
import genj.util.ActionDelegate;
import genj.view.ViewManager;

/**
 * ActionEdit - edit an entity
 */
public class OpenForEdit extends ActionDelegate {
  /** the entity to edit */
  private Entity candidate;
  /** the view manager */
  private ViewManager manager;
  /**
   * Constructor
   */
  public OpenForEdit(Entity entity, ViewManager mgr) {
    manager = mgr;
    candidate = entity;
    setImage(Images.imgView);
    setText(AbstractChange.resources.getString("edit"));
  }
  /**
   * @see genj.util.ActionDelegate#execute()
   */
  protected void execute() {
    EditView edit = (EditView)manager.openView(EditViewFactory.class, candidate.getGedcom());
    edit.setSticky(false);
    manager.setContext(candidate);
  }
  
} //OpenForEdit
