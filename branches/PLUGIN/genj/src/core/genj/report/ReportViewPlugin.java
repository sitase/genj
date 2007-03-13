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

import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.UnitOfWork;
import genj.plugin.ExtensionPoint;
import genj.util.swing.Action2;
import genj.view.BeforeShowContext;
import genj.view.ViewContext;
import genj.view.ViewPlugin;

import java.util.logging.Level;

/**
 * A view plugin providing editing view and actions 
 */
public class ReportViewPlugin extends ViewPlugin {
  
  /**
   * Adding our custom edit actions
   * @see genj.view.ViewPlugin#enrich(genj.plugin.ExtensionPoint)
   */
  public void extend(ExtensionPoint ep) {
    if (ep instanceof BeforeShowContext)
      enrich(((BeforeShowContext)ep).getContext());
  }
  
  /**
   * Enrich a view context with our actions
   */
  private void enrich(ViewContext context) {
    
    // list of properties or a single property in there?
    Property[] properties = context.getProperties();
    if (properties.length>1) {
      enrich(context, properties);
    } else if (properties.length==1) {
      // recursively 
      Property property = properties[0];
      while (property!=null&&!(property instanceof Entity)&&!property.isTransient()) {
        enrich(context, property);
        property = property.getParent();
      }
    }    
    
    // items for set or single entity
    Entity[] entities = context.getEntities();
    if (entities.length>1) {
      enrich(context, entities);
    } else if (entities.length==1) {
      Entity entity = entities[0];
      enrich(context, entity);
    }
        
    // items for gedcom
    enrich(context, context.getGedcom());

    // done
  }
  
  /**
   * collects reports valid for given argument
   */
  private void enrich(ViewContext context, Object argument) {
    
    // Look through reports
    Report[] reports = ReportLoader.getInstance().getReports();
    for (int r=0;r<reports.length;r++) {
      Report report = reports[r];
      try {
        String accept = report.accepts(argument); 
        if (accept!=null) 
          context.addAction(argument, new ActionRun(accept, argument, context.getGedcom(), report));
      } catch (Throwable t) {
        ReportView.LOG.log(Level.WARNING, "Report "+report.getClass().getName()+" failed in accept()", t);
      }
    }
    
    // done
  }

  /**
   * Run a report
   */
  private class ActionRun extends Action2 {
    /** context */
    private Object context;
    /** gedcom */
    private Gedcom gedcom;
    /** report */
    private Report report;
    /** constructor */
    private ActionRun(String txt, Object context, Gedcom gedcom, Report report) {
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
