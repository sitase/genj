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

import genj.util.ColorSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Event;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A 'custom' color chooser
 */
public class ColorChooser extends JColorChooser {
  
  /** combo for color sets */
  private JComboBox comboSets = new JComboBox();
  
  /** list for colors */
  private JList listColors = new JList();
  
  /** changed colors */
  private Map changes = new IdentityHashMap();
    
  /**
   * Constructor
   */
  public ColorChooser() {
    
    // connecting to events through glue
    EventGlue glue = new EventGlue();
    getSelectionModel().addChangeListener(glue);
    
    // preview
    JPanel preview = new JPanel(new BorderLayout());

    comboSets.addActionListener(glue);
    preview.add(comboSets, BorderLayout.NORTH);
    
    ColorSetModel csm = new ColorSetModel();
    listColors.setModel(csm);
    listColors.setCellRenderer(glue);
    listColors.addListSelectionListener(glue);
    listColors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    //listColors.setMinimumSize(listColors.getPreferredSize());
    preview.add(listColors, BorderLayout.CENTER);
    setPreviewPanel(preview);
    
    // done
  }
  
  /**
   * Adds a color set
   */
  public void addSet(ColorSet set) {
    comboSets.addItem(set);
  }
  
  /**
   * Applies current settings to the colors
   */
  public void apply() {
    // check ColorSet
    for (int i=0; i<comboSets.getModel().getSize(); i++) {
      ColorSet cs = (ColorSet)comboSets.getModel().getElementAt(i);
      cs.substitute(changes);
      // next color-set
    }
    comboSets.getModel();
    // done
  }
  
  /**
   * Resets current settings 
   */
  public void reset() {
    changes.clear();
    repaint();
  }
  
  /** 
   * Current ColorSet
   */
  private ColorSet getColorSet() {
    return (ColorSet)comboSets.getSelectedItem();
  }
  
  /**
   * Current color (made have been changed)
   */
  private Color getColor(Color c) {
    Color change = (Color)changes.get(c);
    return change==null?c:change;
  }
  
  /**
   * List model for color set
   */
  private class ColorSetModel extends AbstractListModel {
    /**
     * @see javax.swing.ListModel#getSize()
     */
    public int getSize() {
      ColorSet cs = (ColorSet)comboSets.getSelectedItem();
      if (cs==null) return 0;
      return cs.getSize()-1;
    }
    /**
     * @see javax.swing.ListModel#getElementAt(int)
     */
    public Object getElementAt(int index) {
      ColorSet cs = (ColorSet)comboSets.getSelectedItem();
      return cs.getName(index+1);
    }
  } //ColorSetModel
  
  /**
   * Renderer knowing how to render picks
   */
  private class EventGlue extends JLabel implements ActionListener, ListCellRenderer, ChangeListener, ListSelectionListener {
    /** the original font */
    private Font oFont = ColorChooser.this.getFont();
    /**
     * set selected from combo box
     * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
      comboSets.setModel(comboSets.getModel());
      listColors.clearSelection();
      listColors.repaint();
    }
    /**
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(JList, Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      // update text
      setText(value.toString());
      // current ColorSet
      ColorSet cs = getColorSet();
      // check bg
      setOpaque(true);
      setBackground(getColor(cs.getColor(0)));
      // check fg
      setForeground(getColor(cs.getColor(index+1)));
      // check font
      setFont(isSelected?oFont.deriveFont(Font.BOLD):oFont);
      // done
      return this;
    }
    /**
     * callback color was choosen
     * @see javax.swing.event.ChangeListener#stateChanged(ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
      // grab current color from color selector
      Color color = getColorSet().getColor(listColors.getSelectedIndex()+1);
      if (!color.equals(getColor())) changes.put(color, getColor());
      // show it
      listColors.repaint();
      // done
    }
    /**
     * callback list selection changed
     * @see javax.swing.event.ListSelectionListener#valueChanged(ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
      setColor(getColorSet().getColor(listColors.getSelectedIndex()+1));
    }
  } //PickRenderer

  /**
   * A color model
   */
    
} //ColourChooser
