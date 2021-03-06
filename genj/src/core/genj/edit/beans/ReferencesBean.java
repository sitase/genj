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
import genj.gedcom.Gedcom;
import genj.gedcom.MetaProperty;
import genj.gedcom.Property;
import genj.gedcom.PropertyChild;
import genj.gedcom.PropertyFamilyChild;
import genj.gedcom.PropertyFamilySpouse;
import genj.gedcom.PropertyHusband;
import genj.gedcom.PropertyWife;
import genj.gedcom.PropertyXRef;
import genj.gedcom.TagPath;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

/**
 * A complex bean displaying references 
 */
public class ReferencesBean extends PropertyBean {

  public static Icon IMG = MetaProperty.IMG_LINK;

  private PropertyTableWidget table;
  
  public ReferencesBean() {
    
    // prepare a simple table
    table = new PropertyTableWidget();
    table.setVisibleRowCount(2);
    
    setLayout(new BorderLayout());
    add(BorderLayout.CENTER, table);

  }
  
  @Override
  public void removeNotify() {
    REGISTRY.put("refcols", table.getColumnLayout());
    super.removeNotify();
  }
  
  @Override
  protected void commitImpl(Property property) {
    // noop
  }

  /**
   * Set context to edit
   */
  protected void setPropertyImpl(Property prop) {

    Model model = null;
    
    if (prop!=null)
      model = getModel(prop);
      
    table.setModel(model);
    String columnLayout = REGISTRY.get("refcols", "");
    if (columnLayout != "")
      table.setColumnLayout(columnLayout);
  }
  
  private Model getModel(Property root) {
    
    List<PropertyXRef> rows = new ArrayList<PropertyXRef>();
    
    // refs
    for (PropertyXRef ref : root.getProperties(PropertyXRef.class)) {
      // ignore relationships or invalid refs
      if (ref instanceof PropertyHusband || ref instanceof PropertyWife || ref instanceof PropertyChild 
          || ref instanceof PropertyFamilyChild || ref instanceof PropertyFamilySpouse || !ref.isValid())
        continue;
      rows.add(ref);
    }
    
    return new Model(root, rows);
  }
  
  private class Model extends AbstractPropertyTableModel {
    
    private List<PropertyXRef> rows;
    private TagPath[] columns = new TagPath[] {
        new TagPath(".", Gedcom.getName("REFN")), 
        new TagPath("*:..:..", "*"), 
      };
        
    Model(Property root, List<PropertyXRef> rows) {
      super(root.getGedcom());
      this.rows = rows;
    }

    public int getNumCols() {
      return columns.length;
    }

    public int getNumRows() {
      return rows.size();
    }

    public TagPath getColPath(int col) {
      return columns[col];
    }

    public Property getRowRoot(int row) {
      return rows.get(row);
    }

    @Override
    public String getCellValue(Property property, int row, int col) {

      switch (col) {
        // the name of the reference xref - specially treat foreign xrefs
        case 0:
          if (property instanceof PropertyXRef) {
            PropertyXRef ref = (PropertyXRef)property;
            if (ref.isTransient())
              property = ref.getTarget().getParent();
          }
          return property.getPropertyName();

        // the referenced entity
        case 1:
        default:
          return property.toString();
      }
    }
    
    @Override
    public int getCellAlignment(Property property, int row, int col) {
      return LEFT;
    }
  }
} 
