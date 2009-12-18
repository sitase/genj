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

import genj.common.AbstractPropertyTableModel;
import genj.common.PropertyTableWidget;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.TagPath;

import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * A complex bean displaying children of a family
 */
public class ChildrenBean extends PropertyBean {

  private final static String COLS_KEY = "bean.children.layout";
  
  private final static TagPath PATHS[] = {
    new TagPath("INDI", Gedcom.getName("CHIL")),
    new TagPath("INDI:NAME"),
    new TagPath("INDI:BIRT:DATE"),
    new TagPath("INDI:BIRT:PLAC")
  };
  
  private PropertyTableWidget table;
  
  public ChildrenBean() {
    
    // a table for the families
    table = new PropertyTableWidget();
    table.setPreferredSize(new Dimension(64,64));
    
    setLayout(new BorderLayout());
    add(BorderLayout.CENTER, table);
    
  }

  @Override
  protected void commitImpl(Property property) {
    // noop
  }
  
  /**
   * on add - set column widths
   */
  public void addNotify() {
    // let super continue
    super.addNotify();
    // set widths
    table.setColumnLayout(REGISTRY.get(COLS_KEY, (String)null));
  }
  
  /**
   * on remove - keep column widths
   */
  public void removeNotify() {
    REGISTRY.put(COLS_KEY, table.getColumnLayout());
    // let super continue
    super.removeNotify();
  }
  
  /**
   * Set context to edit
   */
  public void setPropertyImpl(Property prop) {
    table.setModel(prop!=null ? new Children((Fam)prop) : null);
  }
  
  private class Children extends AbstractPropertyTableModel {
    private Fam fam;
    private Children(Fam fam) {
      super(fam.getGedcom());
      this.fam = fam;
    }
    public int getNumCols() {
      return PATHS.length;
    }
    public int getNumRows() {
      return fam.getNoOfChildren();
    }
    public TagPath getPath(int col) {
      return PATHS[col];
    }
    public Property getProperty(int row) {
      return fam.getChild(row);
    }
  }

} //ChildrenBean
