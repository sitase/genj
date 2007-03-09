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
package genj.edit.beans;

import genj.gedcom.Property;
import genj.util.Registry;
import genj.util.swing.TextFieldWidget;
import genj.view.ViewManager;

import java.awt.BorderLayout;

/**
 * A Proxy knows how to generate interaction components that the user
 * will use to change a property : UNKNOWN
 */
public class SimpleValueBean extends PropertyBean {

  /** members */
  private TextFieldWidget tfield;

  void initialize(ViewManager setViewManager, Registry setRegistry) {
    super.initialize(setViewManager, setRegistry);
    
    tfield = new TextFieldWidget("", 8);
    tfield.addChangeListener(changeSupport);
    
    setLayout(new BorderLayout());
    add(BorderLayout.NORTH, tfield);
  }

  /**
   * Finish editing a property through proxy
   */
  public void commit(Property property) {
    
    super.commit(property);
    
    if (!property.isReadOnly())
      property.setValue(tfield.getText());
  }

  /**
   * Editable depends on property
   */  
  public boolean isEditable() {
    return tfield.isEditable();
  }
  
  /**
   * we accept anything
   */
  public boolean accepts(Property prop) {
    return true;
  }

  /**
   * Set context to edit
   */
  public void setProperty(Property property) {

    // remember property
    this.property = property;
    
    // show value
    String txt = property.getDisplayValue();
    tfield.setText(txt);
    tfield.setEditable(!property.isReadOnly());
    tfield.setVisible(!property.isReadOnly()||txt.length()>0);
    
    defaultFocus = tfield.isEditable() ? tfield : null;
  }
  
}