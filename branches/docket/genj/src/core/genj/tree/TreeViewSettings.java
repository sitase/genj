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

import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ButtonHelper;
import genj.util.swing.ColorsWidget;
import genj.util.swing.FontChooser;
import genj.util.swing.ListWidget;
import genj.util.swing.NestedBlockLayout;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * The settings component for the Tree View
public class TreeViewSettings extends JTabbedPane {

  /** spinners */
  private JSpinner[] spinners = new JSpinner[5]; 
  
  /** colorchooser for colors */
  private ColorsWidget colorWidget;
  
  /** resources */
  private Resources resources = Resources.get(this);

  /** Checkboxes */
  private JCheckBox 
    checkBending = new JCheckBox(resources.getString("bend" )),
    checkAntialiasing = new JCheckBox(resources.getString("antialiasing" )),
    checkAdjustFonts = new JCheckBox(resources.getString("adjustfonts" )),
    checkMarrSymbols = new JCheckBox(resources.getString("marrsymbols" ))
  ;
  
  /** buttons */
  private Action2 
    bookmarkUp = new ActionMove(-1), 
    bookmarkDown = new ActionMove( 1), 
    bookmarkDelete =  new ActionDelete(); 
  
  /** font chooser */
  private FontChooser fontChooser = new FontChooser();
  
  /** bookmark list */
  private JList bookmarkList;

  public TreeViewSettings(TreeView view) {
    
    // panel for checkbox options    
    JPanel options = new JPanel(new NestedBlockLayout(
        "<col>"+
         "<check gx=\"1\"/>"+
         "<check gx=\"1\"/>"+
         "<check gx=\"1\"/>"+
         "<check gx=\"1\"/>"+
         "<font gx=\"1\"/>"+
         "<row><label/><spinner/></row>"+
         "<row><label/><spinner/></row>"+
         "<row><label/><spinner/></row>"+
         "<row><label/><spinner/></row>"+
         "<row><label/><spinner/></row>"+
         "</col>"
     ));

    checkBending       .setToolTipText(resources.getString("bend.tip"));
    checkAntialiasing  .setToolTipText(resources.getString("antialiasing.tip"));
    checkAdjustFonts .setToolTipText(resources.getString("adjustfonts.tip"));
    checkMarrSymbols.setToolTipText(resources.getString("marrsymbols.tip"));
    options.add(checkBending);
    options.add(checkAntialiasing);
    options.add(checkAdjustFonts);
    options.add(checkMarrSymbols);
    options.add(fontChooser);    
    
    spinners[0] = createSpinner("indiwidth",  options, 0.4, 16.0);
    spinners[1] = createSpinner("indiheight", options, 0.4, 16.0);
    spinners[2] = createSpinner("famwidth",   options, 0.4, 16.0);
    spinners[3] = createSpinner("famheight",  options, 0.4, 16.0);
    spinners[4] = createSpinner("padding",    options, 0.4,  4.0);
    
    // color chooser
    colorWidget = new ColorsWidget();
    
    // bookmarks
    Box bookmarks = new Box(BoxLayout.Y_AXIS);
    bookmarkList = new ListWidget();
    bookmarkList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    bookmarks.add(new JScrollPane(bookmarkList));
    
    JPanel bookmarkActions = new JPanel();
    ButtonHelper bh = new ButtonHelper().setContainer(bookmarkActions);
    bh.create(bookmarkUp);
    bh.create(bookmarkDown);
    bh.create(bookmarkDelete);
    bookmarkList.addListSelectionListener(new ListSelectionListener() {
      /** update buttons */
      public void valueChanged(ListSelectionEvent e) {
        int 
          i = bookmarkList.getSelectedIndex(),
          n = bookmarkList.getModel().getSize();
      
        bookmarkUp.setEnabled(i>0);
        bookmarkDown.setEnabled(i>=0&&i<n-1);
        bookmarkDelete.setEnabled(i>=0);
      }
    });
    bookmarks.add(bookmarkActions);
    
    // add those tabs
    add(resources.getString("page.main")  , options);
    add(resources.getString("page.colors"), colorWidget);
    add(resources.getString("page.bookmarks"), bookmarks);
    
    // options
    checkBending.setSelected(view.getModel().isBendArcs());
    checkAntialiasing.setSelected(view.isAntialising());
    checkAdjustFonts.setSelected(view.isAdjustFonts());
    fontChooser.setSelectedFont(view.getContentFont());
    checkMarrSymbols.setSelected(view.getModel().isMarrSymbols());
    // colors
    colorWidget.removeAllColors();
    for (String key : view.colors.keySet()) 
      colorWidget.addColor(key, resources.getString("color."+key), view.colors.get(key));
    // bookmarks
    bookmarkList.setModel(new DefaultComboBoxModel(view.getModel().getBookmarks().toArray()));
    // metrics
    TreeMetrics m = view.getModel().getMetrics();
    int[] values = new int[] {
      m.wIndis, m.hIndis, m.wFams, m.hFams, m.pad   
    };
    for (int i=0;i<values.length;i++) {
      spinners[i].setValue(new Double(values[i]*0.1D));
    }
    
    // done
  }
  
  /**
   * Create a spinner
   */
  private JSpinner createSpinner(String key, Container c, double min, double max) {
    
    JSpinner result = new JSpinner(new SpinnerNumberModel(1D, min, max, 0.1D));
    JSpinner.NumberEditor editor = new JSpinner.NumberEditor(result, "##0.0");
    result.setEditor(editor);
    result.addChangeListener(editor);
    result.setToolTipText(resources.getString("info."+key+".tip"));
    
    c.add(new JLabel(resources.getString("info."+key)));
    c.add(result);
    
    // done
    return result;
  }
  
  public void commit(TreeView view) {
    // options
    view.getModel().setBendArcs(checkBending.isSelected());
    view.setAntialiasing(checkAntialiasing.isSelected());
    view.setAdjustFonts(checkAdjustFonts.isSelected());
    view.setContentFont(fontChooser.getSelectedFont());
    view.getModel().setMarrSymbols(checkMarrSymbols.isSelected());
    // colors
    for (String key : view.colors.keySet()) {
      view.colors.put(key, colorWidget.getColor(key));
    }
    // bookmarks
    List<Bookmark> bookmarks = new ArrayList<Bookmark>();
    ListModel list = bookmarkList.getModel();
    for (int i=0;i<list.getSize();i++) {
      bookmarks.add((Bookmark)list.getElementAt(i));
    }
    view.getModel().setBookmarks(bookmarks);
    // metrics
    view.getModel().setMetrics(new TreeMetrics(
      (int)(((Double)spinners[0].getModel().getValue()).doubleValue()*10),
      (int)(((Double)spinners[1].getModel().getValue()).doubleValue()*10),
      (int)(((Double)spinners[2].getModel().getValue()).doubleValue()*10),
      (int)(((Double)spinners[3].getModel().getValue()).doubleValue()*10),
      (int)(((Double)spinners[4].getModel().getValue()).doubleValue()*10)
    ));
    // done
  }

  /**
   * Action - move a bookmark
   */
  private class ActionMove extends Action2 {
    /** by how much to move */
    private int by;
    private ActionMove(int how) {
      setText(resources.getString("bookmark.move."+how));
      setEnabled(false);
      by = how;
    }
    public void actionPerformed(java.awt.event.ActionEvent e) {
      int i = bookmarkList.getSelectedIndex();
      DefaultComboBoxModel model = (DefaultComboBoxModel)bookmarkList.getModel();
      Object bookmark = model.getElementAt(i);
      model.removeElementAt(i);
      model.insertElementAt(bookmark, i+by);
      bookmarkList.setSelectedIndex(i+by);
    }
  } //ActionMove
  
  /**
   * Action - delete a bookmark
   */
  private class ActionDelete extends Action2 {
    private ActionDelete() {
      setText(resources.getString("bookmark.del"));
      setEnabled(false);
    }
    public void actionPerformed(java.awt.event.ActionEvent e) {
      int i = bookmarkList.getSelectedIndex();
      if (i>=0)
        ((DefaultComboBoxModel)bookmarkList.getModel()).removeElementAt(i);
    }
  } //ActionDelete
  
} //TreeViewSettings