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

import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.UnitOfWork;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.view.ActionProvider;
import genj.view.View;
import genj.view.ViewFactory;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;


/**
 * The factory for the TableView
 */
public class ReportViewFactory implements ViewFactory, ActionProvider {

  /*package*/ final static ImageIcon IMG = new ImageIcon(ReportViewFactory.class, "View");

  public int getPriority() {
    return 50; // normal
  }
  
  /**
   * Factory method - create instance of view
   */
  public View createView(String title, Registry registry, Context context) {
    return new ReportView(title,context,registry);
  }
  
  /**
   * @see genj.view.ViewFactory#getImage()
   */
  public ImageIcon getImage() {
    return IMG;
  }
  
  /**
   * @see genj.view.ViewFactory#getTitle(boolean)
   */
  public String getTitle() {
    return Resources.get(this).getString("title");
  }
  
  /**
   * Plugin actions for entities
   */
  public List<Action2> createActions(Property[] properties) {
    return getActions(properties, properties[0].getGedcom());
  }

  /**
   * Plugin actions for entity
   */
  public List<Action2> createActions(Entity entity) {
    return getActions(entity, entity.getGedcom());
  }

  /**
   * Plugin actions for gedcom
   */
  public List<Action2> createActions(Gedcom gedcom) {
    return getActions(gedcom, gedcom);
  }

  /**
   * Plugin actions for property
   */
  public List<Action2> createActions(Property property) {
    return getActions(property, property.getGedcom());
  }

  /**
   * collects actions for reports valid for given context
   */
  private List getActions(Object context, Gedcom gedcom) {
    
    Action2.Group result = new Action2.Group("Reports");
    Action2.Group batch = result;
    
    // Look through reports
    for (Report report : ReportLoader.getInstance().getReports()) {
      try {
        String accept = report.accepts(context); 
        if (accept!=null) {
          ActionRun run = new ActionRun(accept, context, gedcom, report);
          if (batch.size()>10) {
            Action2.Group next = new Action2.Group("...");
            batch.add(next);
            batch = next;
          }
          batch.add(run);
        }
      } catch (Throwable t) {
        ReportView.LOG.log(Level.WARNING, "Report "+report.getClass().getName()+" failed in accept()", t);
      }
    }
    
    // done
    return Collections.singletonList(result);
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
// FIXME docket need to open report view on stdout report    	  
//        // get handle of a ReportView 
//        Object[] views = manager.getViews(ReportView.class, gedcom);
//        ReportView view;
//        if (views.length==0)
//          view = (ReportView)manager.openView(ReportViewFactory.class, gedcom).getView();
//        else 
//          view = (ReportView)views[0];
//        // run it in view
//        view.run(report, context);
        // we're done ourselves - don't go into execute()
        return false;
      }
      // go ahead into async execute
      return true;
    }
    /** callback */
    protected void execute() {
      
      try{
        
        if (report.isReadOnly())
          report.start(context);
        else
          gedcom.doUnitOfWork(new UnitOfWork() {
            public void perform(Gedcom gedcom) {
              try {
                report.start(context);
              } catch (Throwable t) {
                throw new RuntimeException(t);
              }
            }
          });
      
      } catch (Throwable t) {
        Throwable cause = t.getCause();
        if (cause instanceof InterruptedException)
          report.println("***cancelled");
        else
          ReportView.LOG.log(Level.WARNING, "encountered throwable in "+report.getClass().getName()+".start()", cause!=null?cause:t);
      }
    }
    
  } //ActionRun

} //ReportViewFactory
