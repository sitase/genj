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
package genj.view;

import genj.app.ExtendMenubar;
import genj.app.ExtendToolbar;
import genj.gedcom.Gedcom;
import genj.plugin.ExtensionPoint;
import genj.plugin.Plugin;
import genj.plugin.PluginManager;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;

/**
 * A plugin that provides [a] view(s) onto gedcom data
 */
public abstract class ViewPlugin implements Plugin {
  
  private static Resources RESOURCES = Resources.get(ViewPlugin.class);

  /**
   * @see genj.plugin.Plugin#initPlugin(genj.plugin.PluginManage)
   */
  public void initPlugin(PluginManager manager) {
  }
  
  /** 
   * Provide an image
   */
  protected abstract ImageIcon getImage();
  
  /** 
   * Provide a text
   */
  protected abstract String getTitle();
  
  /**
   * @see genj.plugin.Plugin#enrich(genj.plugin.ExtensionPoint)
   */
  public void extend(ExtensionPoint ep) {
    
    // menubar we can hook into?
    if (ep instanceof ExtendMenubar) 
      extend((ExtendMenubar)ep);
    
    // toolbar we can hook into?
    if (ep instanceof ExtendToolbar) 
      extend((ExtendToolbar)ep);
    
    // done
  }

  /** extend toolbar */
  private void extend(ExtendToolbar toolbar) {
    toolbar.addAction(new Open(toolbar.getGedcom()));
  }
  
  /** extend menubar */
  private void extend(ExtendMenubar menu) {
    
    Gedcom gedcom = menu.getGedcom();
    if (gedcom!=null)
      menu.addAction(RESOURCES.getString("views"), new Open(gedcom));
  }
  
  /**
   * Action - Open
   */
  private class Open extends Action2 {
    
    private Gedcom gedcom;
    
    private Open(Gedcom gedcom) {
      this.gedcom = gedcom;
      String txt = RESOURCES.getString("view.open", ViewPlugin.this.getTitle());
      setText(txt);
      setTip(txt);
      setImage(ViewPlugin.this.getImage());
      setEnabled(gedcom!=null);
    }
  } //Open

} //ViewPlugin
