/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2009 Nils Meier <nils@meiers.net>
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
package genj.app;

import genj.gedcom.Gedcom;
import genj.util.Registry;
import genj.view.ToolBar;
import genj.view.View;
import genj.view.ViewFactory;

import javax.swing.Action;
import javax.swing.JComponent;

import swingx.docking.DefaultDockable;
import swingx.docking.Docked;


/**
 * A dockable for views
 */
public class ViewDockable extends DefaultDockable {

  /**
   * Constructor
   */
  public ViewDockable(ViewFactory factory, Gedcom gedcom) {
    
    // title 
    String title = factory.getTitle();
    
    // get a registry 
    Registry registry = new Registry(Registry.lookup(gedcom.getOrigin().getFileName(), gedcom.getOrigin()), factory.getClass().getName()+".1");
    
    // create new View
    JComponent view = factory.createView(title, gedcom, registry);

    // create the view
    setContent(view);
    setTitle(title);
  }
  
  @Override
  public void docked(final Docked docked) {
    super.docked(docked);

    // only if ToolBarSupport and no bar installed
    View view = (View)getContent();    
    view.populate(new ToolBar() {
    	public void add(Action action) {
    		docked.addTool(action);
    	}
    	public void add(JComponent component) {
    		docked.addTool(component);
    	}
    	public void addSeparator() {
    		docked.addToolSeparator();
    	}
    });

    // Fill Toolbar
//    ((ToolBarSupport)view).populate(bar);
    
    // .. a button for editing the View's settings
    // FIXME docket show settings button
//    if (SettingsWidget.hasSettings(view))
//      bh.create(new ActionOpenSettings());
    
    // .. a button for printing View
    // FIXME docket show print button
//    try {
//      Printer printer = (Printer)Class.forName(view.getClass().getName()+"Printer").newInstance();
//      try {
//        printer.setView(view);
//        PrintTask print = new PrintTask(printer, viewHandle.getTitle(), view,  new PrintRegistry(viewHandle.getRegistry(), "print"));
//        print.setTip(ViewManager.RESOURCES, "view.print.tip");
//        bh.create(print);
//      } catch (Throwable t) {
//        ViewManager.LOG.log(Level.WARNING, "can't setup printing for printer "+printer.getClass().getName());
//        ViewManager.LOG.log(Level.FINE, "throwable while setting up "+printer.getClass().getName(), t);
//      }
//    } catch (Throwable t) {
//    }

    // done
  }
  
}
