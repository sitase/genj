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
package genj.edit;

import genj.gedcom.Entity;
import genj.gedcom.PropertyXRef;
import genj.view.ViewManager;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A proxy for a property that links entities
 */
class ProxyXRef extends Proxy {

  /** references entity */
  private Entity entity;


  /**
   * Finish editing a property through proxy
   */
  protected void finish() {
  }
  
  /**
   * Returns change state of proxy
   */
  protected boolean hasChanged() {
    return false;
  }

  /**
   * Start editing a property through proxy
   */
  protected JComponent start(JPanel in) {

    // Calculate reference information
    entity = ((PropertyXRef) property).getReferencedEntity();

    // setup content
    if (entity!=null) in.add(new Preview(entity));
    
    // done
    return null;
  }
  
  /**
   * @see genj.edit.Proxy#isClickAction()
   */
  protected boolean isClickAction() {
    return entity!=null;
  }
  
  /**
   * @see genj.edit.Proxy#click()
   */
  protected void click() {
    boolean sticky = view.setSticky(false);
    ViewManager.getInstance().setCurrentEntity(entity);
    view.setSticky(sticky);
  }
  
} //ProxyXRef
