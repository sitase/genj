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

import genj.app.PluginFactory;
import genj.app.Workbench;
import genj.app.WorkbenchListener;
import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.UnitOfWork;
import genj.util.swing.Action2;
import genj.view.ActionProvider;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * A plugin for adding report tools
 */
public class ReportPluginFactory implements PluginFactory {

  /** factory */
  public Object createPlugin(Workbench workbench) {
    return new ReportPlugin(workbench);
  }

  /** plugin */
  private class ReportPlugin implements ActionProvider, WorkbenchListener {
    
    private Workbench workbench;
    private Action2.Group actions = new Action2.Group("Reports");
    
    public ReportPlugin(Workbench workbench) {
      this.workbench = workbench;
      
      workbench.addWorkbenchListener(this);

      installActions();
    }
    
    private void installActions() {
      workbench.uninstallTool(actions);
      Context context = workbench.getContext();
      if (context!=null) {
        Action2.Group action = getAction(context.getGedcom(), context.getGedcom());
        if (action.size()>0)
          workbench.installTool(action, false);
      }
    }
    
    public void commitRequested() {
    }
    
    public void selectionChanged(Context context, boolean isActionPerformed) {
    }
    
    public boolean workbenchClosing() {
      return true;
    }
    
    public void gedcomClosed(Gedcom gedcom) {
      installActions();
    }
    
    public void gedcomOpened(Gedcom gedcom) {
      installActions();
    }
    
    public int getPriority() {
      return NORMAL;
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
    private List<Action2> getActions(Object context, Gedcom gedcom) {

      Action2.Group action = getAction(context, gedcom);
      List<Action2> result = new ArrayList<Action2>();
      if (action.size()>0)
        result.add(action);
      return result;
      
    }
    
    private Action2.Group getAction(Object context, Gedcom gedcom) {
    
      Action2.Group group = new Action2.Group("Reports", ReportViewFactory.IMG);
      Action2.Group batch = group;
      
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
      return group;
    }
    
    /**
     * Run a report
     */
    private class ActionRun extends Action2 {
      /** context */
      private Object context;
      private Gedcom gedcom;
      /** report */
      private Report report;
      /** constructor */
      private ActionRun(Report report) {
        setImage(report.getImage());
        setText(report.getName());
      }
      /** constructor */
      private ActionRun(String txt, Object context, Gedcom gedcom, Report report) {
        // remember
        this.context = context;
        this.gedcom = gedcom;
        this.report = report;
        // show
        setImage(report.getImage());
        setText(txt);
      }
      
      /** callback */
      public void actionPerformed(ActionEvent event) {
        
//        try{
//          // FIXME docket on report start figure out ReportView and report output
//          if (report.isReadOnly())
//            report.start(context);
//          else
//            gedcom.doUnitOfWork(new UnitOfWork() {
//              public void perform(Gedcom gedcom) {
//                try {
//                  report.start(context);
//                } catch (Throwable t) {
//                  throw new RuntimeException(t);
//                }
//              }
//            });
//        } catch (Throwable t) {
//          Throwable cause = t.getCause();
//          if (cause instanceof InterruptedException)
//            report.println("***cancelled");
//          else
//            ReportView.LOG.log(Level.WARNING, "encountered throwable in "+report.getClass().getName()+".start()", cause!=null?cause:t);
//        }
      }
    } //ActionRun
 }
}
