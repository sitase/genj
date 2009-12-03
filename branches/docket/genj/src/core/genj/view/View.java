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
package genj.view;

import genj.gedcom.Context;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;

public abstract class View extends JPanel implements SelectionListener {

  private List<SelectionListener> listeners = new CopyOnWriteArrayList<SelectionListener>();

  /**
   * Constructor
   */
  public View() {
  }
  
  /**
   * Constructor
   */
  public View(LayoutManager lm) {
    super(lm);
  }
  
  /**
   * commit any outstanding changes
   */
  public void commit() {
  }

  /**
   * prepare to close
   */
  public boolean closing() {
    return true;
  }
  /**
   * add listener
   */
  public void addSelectionListener(SelectionListener listener) {
    listeners.add(listener);
  }
  
  /**
   * remove listener
   */
  public void removeSelectionListener(SelectionListener listener) {
    listeners.remove(listener);
  }

  /**
   * fire selection event
   * @param context
   * @param isActionPerformed
   */
  public void fireSelection(Context context, boolean isActionPerformed) {
    if (context==null)
      throw new IllegalArgumentException("context cannot be null");
    for (SelectionListener listener : listeners) {
      listener.select(context, isActionPerformed);
    }
  }
  
  /**
   * Find the view for given component
   */
  
  // FIXME docket this doesn't work for out of view components - e.g. component shown from report dialog
  public static View getView(Component componentInView) {
    do {
      if (componentInView instanceof View)
        return (View)componentInView;
      if (componentInView instanceof Window)
        componentInView = ((Window)componentInView).getOwner();
      else
        componentInView = componentInView.getParent();
    } while (componentInView!=null);
    
    throw new IllegalArgumentException("Cannot find view for component");
  }
  
  public static void fireSelection(Component componentInView, Context context, boolean isActionPerformed) {
    View.getView(componentInView).fireSelection(context, isActionPerformed);
  }

  /**
   * Retrieve an existing view 
   * @return open view or null
   */
  public View get(ViewFactory factory) {
    // FIXME docket view from view
    return null;
  }
  
  /**
   * follow selection
   */
  public void select(Context context, boolean isActionPerformed) {
    // noop
  }

  /**
   * populate a toolbar
   */
  public void populate(ToolBar toolbar) {
    // noop
  }

}
