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
import genj.gedcom.PropertySex;
import genj.util.Registry;
import genj.util.swing.Action2;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

/**
 * A Proxy knows how to generate interaction components that the user
 * will use to change a property : SEX
 */
public class SexBean extends PropertyBean {

  /** members */
  private AbstractButton[] buttons = new AbstractButton[3];
  
  /**
   * Finish editing a property through proxy
   */
  public void commit(Property property) {
    
    super.commit(property);
    
    PropertySex sex = (PropertySex)property; 
    sex.setSex(getSex());
  }
  
  void initialize(Registry setRegistry) {
    super.initialize(setRegistry);
    
    // use our layout
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      
    // create buttons    
    ButtonGroup group = new ButtonGroup();
    for (int i=0;i<buttons.length;i++) {
      buttons[i] = new JRadioButton(new Gender(i) );
      this.add(buttons[i]);
      group.add(buttons[i]);
    }
    
    // Done
  }
  
  /**
   * Get current sex
   */
  private int getSex() {
    
    // Gather data change
    for (int i=0;i<buttons.length;i++) {
      if (buttons[i].isSelected()) 
        return i;
    }
        
    // unknown
    return PropertySex.UNKNOWN;
  }

  /**
   * Set context to edit
   */
  public void setProperty(PropertySex sex) {

    // remember property
    property = sex;
    
    // show it
    buttons[sex.getSex()].setSelected(true);
    defaultFocus = buttons[0];

    // Done
  }
  
  /**
   * Gender change action
   */
  private class Gender extends Action2 {
    int sex;
    private Gender(int sex) {
      this.sex = sex;
      setText(PropertySex.getLabelForSex(sex));
    }
    protected void execute() {
      SexBean.this.changeSupport.fireChangeEvent();
    }

  } //Gender

} //ProxySex
