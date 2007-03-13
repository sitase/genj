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
package genj.tree;

import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Indi;
import genj.plugin.ExtensionPoint;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.view.BeforeShowContext;
import genj.view.ViewContext;
import genj.view.ViewPlugin;

/**
 * A view plugin providing editing view and actions 
 */
public class TreeViewPlugin extends ViewPlugin {
  
  /** need resources */
  /*package*/ final static Resources RESOURCES = Resources.get(TreeViewPlugin.class);
  
  /**
   * Adding our custom edit actions
   * @see genj.view.ViewPlugin#enrich(genj.plugin.ExtensionPoint)
   */
  public void extend(ExtensionPoint ep) {
    if (ep instanceof BeforeShowContext)
      enrich(((BeforeShowContext)ep).getContext());
  }
  
  private void enrich(ViewContext context) {

    // create an action for our tree
    Entity[] entities = context.getEntities();
    if (entities.length==1&&(entities[0] instanceof Indi||entities[0] instanceof Fam)) {
      Entity entity = entities[0];
      context.addAction(entity, new ActionRoot(entity));
    }
  }
  
  /**
   * ActionTree
   */
  private class ActionRoot extends Action2 {
    /** entity */
    private Entity root;
    /**
     * Constructor
     */
    private ActionRoot(Entity entity) {
      root = entity;
      setText(RESOURCES.getString("root","?"));
      setImage(Images.imgView);
      setEnabled(false);
    }
    
    /**
     * @see genj.util.swing.Action2#execute()
     */
    protected void execute() {
      // FIXME setRoot(root);
    }
  } //ActionTree

} //ReportViewPlugin
