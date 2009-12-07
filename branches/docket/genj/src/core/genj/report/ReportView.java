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
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.EditorHyperlinkSupport;
import genj.util.swing.ImageIcon;
import genj.view.ToolBar;
import genj.view.View;
import genj.window.WindowManager;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import spin.Spin;

/**
 * Component for running reports on genealogic data
 */
public class ReportView extends View {

  /*package*/ static Logger LOG = Logger.getLogger("genj.report");

  /** time between flush of output writer to output text area */
  private final static String EOL= System.getProperty("line.separator");

  /** statics */
  private final static ImageIcon
    imgStart = new ImageIcon(ReportView.class,"Start"),
    imgStop  = new ImageIcon(ReportView.class,"Stop"),
    imgSave  = new ImageIcon(ReportView.class,"Save");


  /** gedcom this view is for */
  private Gedcom      gedcom;

  /** components to show report info */
  private Output      output;
  private ActionStart actionStart = new ActionStart();
  private ActionStop  actionStop = new ActionStop(actionStart);

  /** registry for settings */
  private Registry registry;

  /** resources */
  /*package*/ static final Resources RESOURCES = Resources.get(ReportView.class);

  /** title of this view */
  private String title;
  
  /** plugin */
  private ReportPlugin plugin = null;

  /**
   * Constructor
   */
  public ReportView(String theTitle, Context context, Registry theRegistry) {

    // data
    gedcom   = context.getGedcom();
    registry = theRegistry;
    title    = theTitle;

    // Output
    output = new Output();

    // Layout for this component
    setLayout(new BorderLayout());
    add(new JScrollPane(output), BorderLayout.CENTER);

    // done
  }

  /**
   * @see javax.swing.JComponent#removeNotify()
   */
  public void removeNotify() {
    // continue
    super.removeNotify();
    // save report options
    ReportLoader.getInstance().saveOptions();
  }
  
  /*package*/ void setPlugin(ReportPlugin plugin) {
    this.plugin = plugin;
  }
  
  /**
   * start a report
   */
  public void startReport(final Report report, Object context) {
    
    if (!actionStart.isEnabled())
      return;
    
    if (report.getStartMethod(gedcom)==null) {
      for (int i=0;i<Gedcom.ENTITIES.length;i++) {
        String tag = Gedcom.ENTITIES[i];
        Entity sample = gedcom.getFirstEntity(tag);
        if (sample!=null && report.accepts(sample)!=null) {

          // give the report a chance to name our dialog
          String txt = report.accepts(sample.getClass());
          if (txt==null) 
            Gedcom.getName(tag);
          
          // ask user for context now
          context = report.getEntityFromUser(txt, gedcom, tag);
          if (context==null) 
            return;
          break;
        }
      }
    }

    // check if appropriate
    if (context==null||report.accepts(context)==null) {
      WindowManager.getInstance(ReportView.this).openDialog(null,report.getName(),WindowManager.ERROR_MESSAGE,RESOURCES.getString("report.noaccept"),Action2.okOnly(),ReportView.this);
      return;
    }
    
    // clear the current output
    output.clear();
    
    // set running
    actionStart.setEnabled(false);
    actionStop.setEnabled(true);
    if (plugin!=null)
      plugin.setEnabled(false);
    
    // kick it off
    new Thread(new Runner(gedcom, context, report, (Runner.Callback)Spin.over(new RunnerCallback()))).start();

  }

  /**
   * callback for runner
   */
  private class RunnerCallback implements Runner.Callback {
    
    public void handleOutput(Report report, String s) {
      output.add(s);
    }

    public void handleResult(Report report, Object result) {

// FIXME docket handle report outputs
// File, URL, Document, JComponent
      
      LOG.fine("Result of report "+report.getName()+" = "+result);
      
      actionStart.setEnabled(true);
      actionStop.setEnabled(false);
      if (plugin!=null)
        plugin.setEnabled(true);
    }

  }
  
  /**
   * Start a report after selection
   */
  public void startReport() {
    
    ReportSelector selector = new ReportSelector();
    try {
      selector.select(ReportLoader.getInstance().getReportByName(registry.get("lastreport", (String)null)));
    } catch (Throwable t) {
    }
    
    if (0!=WindowManager.getInstance().openDialog("report", RESOURCES.getString("report.reports"),
        WindowManager.QUESTION_MESSAGE, selector, Action2.okCancel(), ReportView.this))
      return;
    
    Report report = selector.getReport();
    if (report==null)
      return;
    
    registry.put("lastreport", report.getClass().getName());
    
    startReport(report, gedcom);

  }

  /**
   * stop any running report
   */
  public void stopReport() {
    // FIXME docket stopReport()
  }
  
  /**
   * @see genj.view.ToolBarSupport#populate(javax.swing.JToolBar)
   */
  public void populate(ToolBar toolbar) {

    toolbar.add(actionStart);
    toolbar.add(actionStop);
    toolbar.add(new ActionSave());

    // done
  }

  /**
   * Action: STOP
   */
  private class ActionStop extends Action2 {
    private Action2 start;
    protected ActionStop(Action2 start) {
      setImage(imgStop);
      setTip(RESOURCES, "report.stop.tip");
      setEnabled(false);
      this.start=start;
    }
    public void actionPerformed(ActionEvent event) {
      stopReport();
    }
  } //ActionStop

  /**
   * Action: START
   */
  private class ActionStart extends Action2 {

    /** context to run on */
    private Object context;

    /** the running report */
    private Report report;

    /** an output writer */
    private PrintWriter out;

    /** constructor */
    protected ActionStart() {
      // show
      setImage(imgStart);
      setTip(RESOURCES, "report.start.tip");
    }
    /**
     * execute
     */
    public void actionPerformed(ActionEvent event) {
      startReport();
    }

  } //ActionStart

  /**
   * Action: SAVE
   */
  private class ActionSave extends Action2 {
    protected ActionSave() {
      setImage(imgSave);
      setTip(RESOURCES, "report.save.tip");
    }
    public void actionPerformed(ActionEvent event) {
      
      // .. choose file
      JFileChooser chooser = new JFileChooser(".");
      chooser.setDialogTitle("Save Output");

      if (JFileChooser.APPROVE_OPTION != chooser.showDialog(ReportView.this,"Save")) {
        return;
      }
      File file = chooser.getSelectedFile();
      if (file==null) {
        return;
      }

      // .. exits ?
      if (file.exists()) {
        int rc = WindowManager.getInstance(getTarget()).openDialog(null, title, WindowManager.WARNING_MESSAGE, "File exists. Overwrite?", Action2.yesNo(), ReportView.this);
        if (rc!=0) {
          return;
        }
      }

      // .. open file
      final OutputStreamWriter out;
      try {
        out = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF8"));
      } catch (IOException ex) {
        WindowManager.getInstance(getTarget()).openDialog(null,title,WindowManager.ERROR_MESSAGE,"Error while saving to\n"+file.getAbsolutePath(),Action2.okOnly(),ReportView.this);
        return;
      }

      // .. save data
      try {
        BufferedReader in = new BufferedReader(new StringReader(output.getText()));
        while (true) {
          String line = in.readLine();
          if (line==null) break;
          out.write(line);
          out.write("\n");
        }
        in.close();
        out.close();

      } catch (Exception ex) {
      }

      // .. done
    }

  } //ActionSave

  /**
   * output
   */
  private class Output extends JEditorPane implements MouseListener, MouseMotionListener {

    /** the currently found entity id */
    private String id = null;

    /** constructor */
    private Output() {
      setContentType("text/plain");
      setFont(new Font("Monospaced", Font.PLAIN, 12));
      setEditable(false);
      addHyperlinkListener(new EditorHyperlinkSupport(this));
      addMouseMotionListener(this);
      addMouseListener(this);
    }
    
    /**
     * Check if user moves mouse above something recognizeable in output
     */
    public void mouseMoved(MouseEvent e) {

      // try to find id at location
      id = markIDat(e.getPoint());

      // done
    }

    /**
     * Check if user clicks on marked ID
     */
    public void mouseClicked(MouseEvent e) {
      if (id!=null) {
        Entity entity = gedcom.getEntity(id);
        if (entity!=null)
          fireSelection(new Context(entity), e.getClickCount()>1);
      }
    }

    /**
     * Tries to find an entity id at given position in output
     */
    private String markIDat(Point loc) {

      try {
        // do we get a position in the model?
        int pos = viewToModel(loc);
        if (pos<0)
          return null;

        // scan doc
        Document doc = getDocument();

        // find ' ' to the left
        for (int i=0;;i++) {
          // stop looking after 10
          if (i==10)
            return null;
          // check for starting line or non digit/character
          if (pos==0 || !Character.isLetterOrDigit(doc.getText(pos-1, 1).charAt(0)) )
            break;
          // continue
          pos--;
        }

        // find ' ' to the right
        int len = 0;
        while (true) {
          // stop looking after 10
          if (len==10)
            return null;
          // stop at end of doc
          if (pos+len==doc.getLength())
            break;
          // or non digit/character
          if (!Character.isLetterOrDigit(doc.getText(pos+len, 1).charAt(0)))
            break;
          // continue
          len++;
        }

        // check if it's an ID
        if (len<2)
          return null;
        String id = doc.getText(pos, len);
        if (gedcom.getEntity(id)==null)
          return null;

        // mark it
        requestFocusInWindow();
        setCaretPosition(pos);
        moveCaretPosition(pos+len);

        // return in betwee
        return id;

        // done
      } catch (BadLocationException ble) {
      }

      // not found
      return null;
    }

    /**
     * have to implement MouseMotionListener.mouseDragger()
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e) {
      // ignored
    }
    
    void clear() {
      setContentType("text/plain");
      setText("");
    }
    
    void add(String txt) {
      Document doc = getDocument();
      try {
        doc.insertString(doc.getLength(), txt, null);
      } catch (Throwable t) {
      }
    }
    
    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

  } //Output


} //ReportView
