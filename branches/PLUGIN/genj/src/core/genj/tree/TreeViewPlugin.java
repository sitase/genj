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
import genj.gedcom.Gedcom;
import genj.plugin.ExtensionPoint;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.view.ExtendContextMenu;
import genj.view.ViewContext;
import genj.view.ViewPlugin;

import javax.swing.JComponent;

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
    
    super.extend(ep);
    
    if (ep instanceof ExtendContextMenu)
      extend(((ExtendContextMenu)ep).getContext());
  }
  
  /** our image */
  public ImageIcon getImage() {
    return  Images.imgView;
  }
  
  /** our text */
  public String getTitle() {
    return RESOURCES.getString("title");
  }
  
  /** our view */
  protected JComponent createView(Gedcom gedcom, Registry registry) {
    return new TreeView(gedcom, registry);
  }
  
  private void extend(ViewContext context) {

    // create an action for our tree
// FIXME    
//    Entity[] entities = context.getEntities();
//    if (entities.length==1&&(entities[0] instanceof Indi||entities[0] instanceof Fam)) {
//      Entity entity = entities[0];
//      context.addAction(entity, new ActionRoot(entity));
//    }
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
