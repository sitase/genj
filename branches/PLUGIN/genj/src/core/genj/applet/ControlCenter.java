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
package genj.applet;

import genj.gedcom.Gedcom;
import genj.util.GridBagHelper;
import genj.util.WordBuffer;
import genj.util.swing.LinkWidget;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The main content from which the user start using GenJ in Applet
 */
public class ControlCenter extends JPanel {

  /** gedcom */
  private Gedcom gedcom;

  /**
   * Constructor
   */
  public ControlCenter(Gedcom ged) {
    
    // remember
    gedcom = ged;
    
    // layout components
    GridBagHelper gh = new GridBagHelper(this);
    gh.add(getHeaderPanel() ,1,1);
    gh.add(getLinkPanel()   ,1,2);

    // make use white
    setBackground(Color.white);

    // done
  }

  /**
   * Create a header
   */
  private JPanel getHeaderPanel() {
    
    JPanel p = new JPanel(new GridLayout(2,1));
    p.setOpaque(false);
    p.add(new JLabel(gedcom.getOrigin().getFileName(), SwingConstants.CENTER));
    
    WordBuffer words = new WordBuffer();
    words.append(gedcom.getEntities(Gedcom.INDI).size()+" "+Gedcom.getName(Gedcom.INDI, true));
    words.append(gedcom.getEntities(Gedcom.FAM ).size()+" "+Gedcom.getName(Gedcom.FAM , true));
    
    p.add(new JLabel(words.toString(), SwingConstants.CENTER));
    
    return p;
  }
  

  /**
   * Collect buttons for views
   */
  private JPanel getLinkPanel() {

    // FIXME PLUGIN need plugin support for applet
    int foo = 4;
    JPanel p = new JPanel(new GridLayout(foo, 1));
    p.setOpaque(false);
    
    for (int v=0; v<foo; v++) {
      p.add(new LinkWidget(""+v, null));
    }
    
    // done
    return p;
  }
  
} //ControlCenter