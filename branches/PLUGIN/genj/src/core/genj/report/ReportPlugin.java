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
package genj.report;

import genj.app.ExtendMenubar;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.UnitOfWork;
import genj.plugin.ExtensionPoint;
import genj.plugin.Plugin;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.view.ExtendContextMenu;
import genj.view.ViewContext;

import java.util.logging.Level;

/**
 * A  plugin providing report management 
 */
public class ReportPlugin implements Plugin {
  
  /*package*/ final static ImageIcon IMG = new ImageIcon(ReportPlugin.class, "View.gif");

  private final static Resources RESOURCES =  Resources.get(ReportPlugin.class);

  /**
   * Adding our custom edit actions
   * @see genj.view.ViewPlugin#enrich(genj.plugin.ExtensionPoint)
   */
  public void extend(ExtensionPoint ep) {
    
    if (ep instanceof ExtendMenubar)
      extend((ExtendMenubar)ep);
    
    if (ep instanceof ExtendContextMenu)
      extend(((ExtendContextMenu)ep).getContext());
  }
  
  
  /**
   * Extend a menubar with our management functions
   */
  private void extend(ExtendMenubar menubar) {
    String menu = RESOURCES.getString("report.reports");
    menubar.addAction(menu, new Catalog());
    menubar.addAction(menu, new Reload());
  }
  
  /**
   * Extend a view context with our actions
   */
  private void extend(ViewContext context) {
    
    // list of properties or a single property in there?
    Property[] properties = context.getProperties();
    if (properties.length>1) {
      extend(context, properties);
    } else if (properties.length==1) {
      // recursively 
      Property property = properties[0];
      while (property!=null&&!(property instanceof Entity)&&!property.isTransient()) {
        extend(context, property);
        property = property.getParent();
      }
    }    
    
    // items for set or single entity
    Entity[] entities = context.getEntities();
    if (entities.length>1) {
      extend(context, entities);
    } else if (entities.length==1) {
      Entity entity = entities[0];
      extend(context, entity);
    }
        
    // items for gedcom
    extend(context, context.getGedcom());

    // done
  }
  
  /**
   * collects reports valid for given argument
   */
  private void extend(ViewContext context, Object argument) {
    
    // Look through reports
    boolean found = false;
    
    Report[] reports = ReportLoader.getInstance().getReports();
    for (int r=0;r<reports.length;r++) {
      Report report = reports[r];
      try {
        String accept = report.accepts(argument); 
        if (accept!=null) {
          if (!found) {
            context.addSeparator(argument);
            found = true;
          }
          context.addAction(argument, new Run(accept, argument, context.getGedcom(), report));
        }
      } catch (Throwable t) {
        ReportView.LOG.log(Level.WARNING, "Report "+report.getClass().getName()+" failed in accept()", t);
      }
    }
    
    if (found)
      context.addSeparator(argument);
    
    // done
  }

  /**
   * Report Management
   */
  private class Catalog extends Action2 {
    private Catalog() {
      setText(RESOURCES.getString("catalog"));
      setImage(IMG);
    }
    protected void execute() {
    }
  }

  /**
   * Report Management
   */
  private class Reload extends Action2 {
    private Reload() {
      setText(RESOURCES.getString("report.reload.tip"));
      setImage(ReportView.imgReload);
      setEnabled(false);
    }
    protected void execute() {
    }
  }

  /**
   * Run a report
   */
  private class Run extends Action2 {
    /** context */
    private Object context;
    /** gedcom */
    private Gedcom gedcom;
    /** report */
    private Report report;
    /** constructor */
    private Run(String txt, Object context, Gedcom gedcom, Report report) {
      // remember
      this.context = context;
      this.gedcom = gedcom;
      this.report = report;
      // show
      setImage(report.getImage());
      setText(txt);
      // we're async
      setAsync(Action2.ASYNC_SAME_INSTANCE);
    }
    /** callback (edt sync) */
    protected boolean preExecute() {
      // a report with standard out?
      if (report.usesStandardOut()) {
        // FIXME need a report view
//        // run it in view
//        view.run(report, context);
//        // we're done ourselves - don't go into execute()
//        return false;
      }
      // go ahead into async execute
      return true;
    }
    /** callback */
    protected void execute() {
      
      final Report instance = report.getInstance(getTarget(), null);
      
      try{
        
        if (instance.isReadOnly())
          instance.start(context);
        else
          gedcom.doUnitOfWork(new UnitOfWork() {
            public void perform(Gedcom gedcom) {
              try {
                instance.start(context);
              } catch (Throwable t) {
                throw new RuntimeException(t);
              }
            }
          });
      
      } catch (Throwable t) {
        Throwable cause = t.getCause();
        if (cause instanceof InterruptedException)
          instance.println("***cancelled");
        else
          ReportView.LOG.log(Level.WARNING, "encountered throwable in "+instance.getClass().getName()+".start()", cause!=null?cause:t);
      }
    }
    
  } //ActionRun

} //ReportViewPlugin
