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

import genj.util.ChangeSupport;
import genj.util.EnvironmentChecker;

import java.awt.Dimension;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

/**
 * Our own JTextField
 */
public class TextFieldWidget extends JTextField {

  /** whether we're a template */
  private boolean isTemplate = false;
  
  /** whether we do a selectAll() on focus */
  private boolean isSelectAllOnFocus = false;
  
  /** change support */
  private ChangeSupport changeSupport = new ChangeSupport(this) {
    public void fireChangeEvent() {
      // no template anymore
      isTemplate = false;
      // continue
      super.fireChangeEvent();
    }
  };
  
  /**
   * Constructor
   */
  public TextFieldWidget() {
    this("", 0);
  }
  
  /**
   * Constructor
   */
  public TextFieldWidget(String text) {
    this(text, 0);
  }

  /** 
   * Constructor
   */
  public TextFieldWidget(String text, int cols) {
    super(text, cols);
    setAlignmentX(0);

    getDocument().addDocumentListener(changeSupport);
  }
  
  /**
   * Add change listener
   */
  public void addChangeListener(ChangeListener l) {
    changeSupport.addChangeListener(l);
  }
  
  /**
   * Remove change listener
   */
  public void removeChangeListener(ChangeListener l) {
    changeSupport.removeChangeListener(l);
  }
  
  /**
   * Make this a template - the field is set to unchanged, any
   * current value in the text-field is not returned but empty
   * string until the user edits the content
   */
  public TextFieldWidget setTemplate(boolean set) {
    isTemplate = set;
    return this;
  }
  
  /**
   * @see javax.swing.JComponent#getMaximumSize()
   */
  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, super.getPreferredSize().height);
  }
  
  /**
   * Accessor isSelectAllOnFocus
   */
  public void setSelectAllOnFocus(boolean set) {
    isSelectAllOnFocus = set;
  }
  
  /**
   * @see java.awt.Component#processFocusEvent(java.awt.event.FocusEvent)
   */
  protected void processFocusEvent(FocusEvent e) {
    
    // catch focus gained to preselect all for easy overwrite/editing (only on windows though 
    // since auto-select auto-copies!)
    if (EnvironmentChecker.isWindows()&&e.getID()==FocusEvent.FOCUS_GAINED) {
      if (isTemplate||isSelectAllOnFocus) 
        selectAll();
    }
    
    // continue
    super.processFocusEvent(e);
  }
  
  /**
   * Our own selection that places the cursor at the beginning instead of the end
   */
  public void selectAll() {
    // 20040307 wrote my own selectAll() so that the
    // caret is at position 0 after selection - this
    // makes sure the beginning of the text is visible
    if (getDocument() != null) {
      setCaretPosition(getDocument().getLength());
      moveCaretPosition(0);
    }
  }
    
  /**
   * Returns the current content unless isTemplate  is
   * still true (the component didn't receive focus) in
   * which case empty string is returned
   * @see javax.swing.text.JTextComponent#getText()
   */
  public String getText() {
    if (isTemplate) 
      return "";
    return super.getText();
  }
  
  /**
   * Status check - getText().trim().length()==0
   */
  public boolean isEmpty() {
    return getText().trim().length()==0;
  }
  
  /**
   * Sets the content 
   * @see javax.swing.text.JTextComponent#setText(java.lang.String)
   */
  public void setText(String txt) {
    super.setText(txt);
    
    // 20040307 reset caret to 0 - this makes sure the
    // first part of the string is visible
    setCaretPosition(0);
  }
  
} //JTextField