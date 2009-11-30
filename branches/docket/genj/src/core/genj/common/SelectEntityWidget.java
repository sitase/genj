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
package genj.common;

import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Grammar;
import genj.gedcom.MetaProperty;
import genj.gedcom.Property;
import genj.gedcom.PropertyComparator;
import genj.gedcom.PropertyDate;
import genj.gedcom.TagPath;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.WordBuffer;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.util.swing.PopupWidget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;

/**
 * A widget for choosing an entity amongst many
 */
public class SelectEntityWidget extends JPanel {
  
  private final static Resources RESOURCES = Resources.get(SelectEntityWidget.class);

  /** type of entities to choose from */
  private String type = Gedcom.INDI;
  
  /** entities to choose from */
  private Object[] list;
  private Object none;
  
  /** widgets */
  private PopupWidget sortWidget;
  private JComboBox listWidget;
  
  /** registry */
  private Registry registry = Registry.lookup("genj", null);
  
  /** sorts */
  private TagPath sort;
  private List<TagPath> sorts;
  
  private final static String[] SORTS = {
    "INDI:NAME",
    "INDI",
    "INDI:BIRT:DATE",
    "INDI:DEAT:DATE",
    "FAM",
    "FAM:MARR:DATE",
    "OBJE", 
    "OBJE:TITL", 
    "NOTE", 
    "NOTE:NOTE", 
    "SOUR", 
    "SOUR:TITL", 
    "SOUR:AUTH", 
    "SOUR:REPO", 
    "SUBM", 
    "REPO",
    "REPO:NAME",
    "REPO:REFN",
    "REPO:RIN"
  };
  
  /**
   * Constructor
   */
  public SelectEntityWidget(Gedcom gedcom, String type, String none) {

    // remember and lookup
    this.type = type;
    this.none = none;
    
    Collection entities = gedcom.getEntities(type);

    // init list
    if (none!=null) {
      list = new Object[entities.size()+1];
      list[0] = none;
    } else {
      list = new Object[entities.size()];
    }
    Iterator es=entities.iterator();
    for (int e= none!=null ? 1 : 0;e<list.length;e++) {
      Entity ent = (Entity)es.next();
      if (!ent.getTag().equals(type))
        throw new IllegalArgumentException("Type of all entities has to be "+type);
      list[e] = ent;
    }

    // assemble sorts
    sorts = new ArrayList<TagPath>(SORTS.length);
    for (int i=0;i<SORTS.length;i++) {
      String path = SORTS[i];
      if (!path.startsWith(type))
        continue;
      TagPath p = new TagPath(path);
      sorts.add(p);
      if (sort==null||path.equals(registry.get("select.sort."+type, ""))) sort = p;
    }

    // prepare sorting widget
    sortWidget = new PopupWidget();
    sortWidget.setActions(sorts);
    
    // prepare list widget    
    listWidget = new JComboBox();
    listWidget.setMaximumRowCount(16); // 20061020 as suggested by Daniel - show more
    listWidget.setEditable(false);
    listWidget.setRenderer(new Renderer());
    
    // layout
    setLayout(new BorderLayout());
    add(BorderLayout.CENTER, listWidget);
    add(BorderLayout.WEST  , sortWidget);

    // init state
    sort(sort);
    if (list.length>0) listWidget.setSelectedIndex(0);
    
    // done
  }
  
  /**
   * Sort by path
   */
  public void sort(TagPath path) {
    // remember
    sort = path;
    registry.put("select.sort."+type, path.toString());
    // Sort
    PropertyComparator comparator = new PropertyComparator(path);
    Arrays.sort(list, none!=null ? 1 : 0, list.length, comparator);
    // reset our data
    Entity selection = getSelection();
    listWidget.setModel(new DefaultComboBoxModel(list));
    sortWidget.setIcon(getPathImage(path));
    sortWidget.setToolTipText(getPathText(path));
    setSelection(selection);
  }
  
  /**
   * Override maximum size - we can't affort to stretch vertically
   */
  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, super.getPreferredSize().height);
  }
  
  /**
   * Override preferred size - we can't affort prefer horizontally based on contents 
   */
  public Dimension getPreferredSize() {
    return new Dimension(128, super.getPreferredSize().height);
  }
  
  /**
   * Number of entities
   */
  public int getEntityCount() {
    return listWidget.getItemCount()-1;
  }
  
  /**
   * The selected entity
   */
  public Entity getSelection() {
    // check selection
    Object item = listWidget.getSelectedItem();
    if (!(item instanceof Entity))
      return null;
    // done
    return (Entity)item;
  }
  
  /**
   * The selected entity
   */
  public void setSelection(Entity set) {
    // fallback to none?
    if (set==null)
      listWidget.setSelectedItem(none!=null ? none : null);
    // applicable?
    if (!(set instanceof Entity)||!set.getTag().equals(type))
      return;
    // set it
    listWidget.setSelectedItem(set);
  }
  
  /**
   * Add a listener
   */
  public void addActionListener(ActionListener listener) {
    listWidget.addActionListener(listener);
  }
  
  /**
   * Remove a listener
   */
  public void removeActionListener(ActionListener listener) {
    listWidget.removeActionListener(listener);
  }
  
  private MetaProperty getMeta(TagPath tagPath) {
    MetaProperty meta;
    if (tagPath.length()>1&&tagPath.getLast().equals(PropertyDate.TAG))
      meta = Grammar.V55.getMeta(new TagPath(tagPath, tagPath.length()-1));
    else
      meta = Grammar.V55.getMeta(tagPath);
    return meta;
  }
  
  private ImageIcon getPathImage(TagPath tagPath) {
    return getMeta(tagPath).getImage();
  }
  
  private String getPathText(TagPath tagPath) {
    return RESOURCES.getString("select.sort", tagPath.length()==1?"ID":getMeta(tagPath).getName());
  }
  
  /**
   * Entity Rendering
   */
  private class Renderer extends DefaultListCellRenderer {
    /**
     * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

      // might be text of entity
      String txt;
      if (value instanceof Entity) {
        txt = getString((Entity)value);
      } else {
        txt = value!=null ? value.toString() : "";
      }

      return super.getListCellRendererComponent(list, txt, index, isSelected, cellHasFocus);
    }

    /**
     * generate a string to show for entity&path
     */
    private String getString(Entity e) {
      
      if (sort==null)
        return e.toString();
      
      WordBuffer value = new WordBuffer(", ");
      
      // add sorting value first
      value.append(getString(e.getProperty(sort), "?"));
      
      // add all other sorts
      for (TagPath other : sorts) {
        if (!other.equals(sort) && other.getFirst().equals(sort.getFirst())) {
          value.append(getString(e.getProperty(other), ""));
        }
      }
      
      // done
      return value.toString(); // + " / " + e.toString();
    }
    
    private String getString(Property p, String fallback) {
      if (p instanceof Entity)
        return ((Entity)p).getId();
      else
        return p!=null&&p.isValid() ? p.getDisplayValue() : fallback;
    }

  } //Renderer
  
  /**
   * Sort action
   */
  private class Sort extends Action2 {

    /** path */
    private TagPath tagPath;
    
    /**
     * Constructor
     */
    private Sort(TagPath path) {
      tagPath = path;
      setImage(getPathImage(path));
      setTip(getPathText(path));
    }      
    
    /**
     * @see genj.util.swing.Action2#execute()
     */
    public void actionPerformed(ActionEvent event) {
      sort(tagPath);
    }
        
  } //Sort
   
} //PickEntityWidget
