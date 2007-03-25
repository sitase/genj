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
package genj.util.swing;

import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

/**
 * Helper for button creation etc.
 */
public class ButtonHelper {
  
  /** Members */
  private Insets insets           = null;
  private JComponent container     = null;
  
  /** Setters */    
  public ButtonHelper setInsets(int val) { insets=new Insets(val,val,val,val); return this; }
  public ButtonHelper setContainer(JComponent set) { container=set; return this; }
  
  /**
   * Creates the button
   */
  public AbstractButton create(Action action) {
    
    // a NOOP results in separator
    if (action == Action2.NOOP) {
      if (container instanceof JToolBar)
        ((JToolBar)container).addSeparator();
      return null;
    }
    
    // create the button and hook it up to action
    final AbstractButton result = new JButton();
    if (result instanceof JButton) {
        result.setVerticalTextPosition(SwingConstants.BOTTOM);
        result.setHorizontalTextPosition(SwingConstants.CENTER);
    }
    result.setAction(action);
    
    // no mnemonic and text in JToolbars please
    if (container instanceof JToolBar) {
      result.setMnemonic(0);
      result.setText(null);
    }
    
    // patch its look
    if (insets!=null)
      result.setMargin(insets);
    
    // context
    if (container!=null) {
      if (action instanceof Action2)
        ((Action2)action).setTarget(container);
      container.add(result);
    }

    // done
    return result;
  }

} //ButtonHelper
