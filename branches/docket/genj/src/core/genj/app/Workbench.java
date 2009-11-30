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

import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomDirectory;
import genj.gedcom.GedcomException;
import genj.gedcom.GedcomMetaListener;
import genj.gedcom.Indi;
import genj.gedcom.Property;
import genj.gedcom.PropertySex;
import genj.gedcom.Submitter;
import genj.gedcom.UnitOfWork;
import genj.io.Filter;
import genj.io.GedcomEncodingException;
import genj.io.GedcomEncryptionException;
import genj.io.GedcomIOException;
import genj.io.GedcomReader;
import genj.io.GedcomWriter;
import genj.option.OptionProvider;
import genj.option.OptionsWidget;
import genj.util.EnvironmentChecker;
import genj.util.Origin;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.ServiceLookup;
import genj.util.WordBuffer;
import genj.util.swing.Action2;
import genj.util.swing.ButtonHelper;
import genj.util.swing.FileChooser;
import genj.util.swing.HeapStatusWidget;
import genj.util.swing.MenuHelper;
import genj.util.swing.NestedBlockLayout;
import genj.view.ActionProvider;
import genj.view.View;
import genj.view.ViewFactory;
import genj.window.WindowManager;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import swingx.docking.Dockable;
import swingx.docking.DockingPane;

/**
 * The central component of the GenJ application
 */
public class Workbench extends JPanel {

  private final static Logger LOG = Logger.getLogger("genj.app");
  
  private final static String ACC_SAVE = "ctrl S", ACC_EXIT = "ctrl X", ACC_NEW = "ctrl N", ACC_OPEN = "ctrl O";

  private final static Resources RES = Resources.get(Workbench.class);

  /** members */
  private JMenuBar menuBar;
  private Registry registry;
  private WindowManager windowManager;
  private List<Action> gedcomActions = new ArrayList<Action>();
  private StatusBar stats = new StatusBar();
  private DockingPane dockingPane = new DockingPane();
  private Context context= null;
  private Runnable runOnExit;
  private List<WorkbenchListener> listeners = new CopyOnWriteArrayList<WorkbenchListener>();
  private List<Object> plugins = new ArrayList<Object>();

  /**
   * Constructor
   */
  public Workbench(Registry registry, Runnable onExit) {

    // Initialize data
    this.registry = new Registry(registry, "cc");
    windowManager = WindowManager.getInstance();
    runOnExit = onExit;
    
    // plugins
    for (PluginFactory pf : ServiceLookup.lookup(PluginFactory.class)) {
      LOG.info("Loading plugin "+pf.getClass());
      try {
        plugins.add(pf.createPlugin(this));
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "Plugin creation threw exception", t);
      }
    }

    // Layout
    setLayout(new BorderLayout());
    add(createToolBar(), BorderLayout.NORTH);
    add(dockingPane, BorderLayout.CENTER);
    add(stats, BorderLayout.SOUTH);

    // Init menu bar at this point (so it's ready when the first file is loaded)
    menuBar = createMenuBar();
    
    // start clean
    for (Action a : gedcomActions) 
      a.setEnabled(false);

    // Done
  }

  /**
   * asks and loads gedcom file
   */
  public boolean openGedcom() {

    // close what we have
    if (!closeGedcom())
      return false;
    
    // ask user
    File file = chooseFile(RES.getString("cc.open.title"), RES.getString("cc.open.action"), null);
    if (file == null)
      return false;
    registry.put("last.dir", file.getParentFile().getAbsolutePath());
    
    // form origin
    try {
      return openGedcom(new URL("file", "", file.getAbsolutePath()));
    } catch (MalformedURLException e) {
      // shouldn't
      return false;
    }
    // done
  }
  
  /**
   * loads gedcom file
   */
  public boolean openGedcom(URL url) {

    // close what we have
    if (!closeGedcom())
      return false;
    
    // open connection
    Origin origin = Origin.create(url);

//    try {
//    } catch (MalformedURLException e) {
//      windowManager.openDialog(null, RES.getString("cc.open.invalid_url"), WindowManager.ERROR_MESSAGE, url.toString(), Action2.okOnly(), Workbench.this);
//      return;
//    }

    // Check if already open
    if (GedcomDirectory.getInstance().getGedcom(origin.getName()) != null) {
      windowManager.openDialog(null, origin.getName(), WindowManager.ERROR_MESSAGE, RES.getString("cc.open.already_open", origin.getName()), Action2.okOnly(), Workbench.this);
      return false;
    }
    
    // FIXME docket read async
    // Read it - we might try multiple times
    Gedcom gedcom = null;
    String password = Gedcom.PASSWORD_UNKNOWN;
    while (gedcom==null) {
      
      // Open Connection and get input stream
      GedcomReader reader;
      try {
  
        // .. prepare our reader
        reader = new GedcomReader(origin);
  
        // .. set password we're using
        reader.setPassword(password);
  
      } catch (IOException ex) {
        String txt = RES.getString("cc.open.no_connect_to", origin) + "\n[" + ex.getMessage() + "]";
        windowManager.openDialog(null, origin.getName(), WindowManager.ERROR_MESSAGE, txt, Action2.okOnly(), Workbench.this);
        return false;
      }

      // .. show progress dialog
      //String progress = windowManager.openNonModalDialog(null, RES.getString("cc.open.loading", origin.getName()), WindowManager.INFORMATION_MESSAGE, new ProgressWidget(reader, getThread()), Action2.cancelOnly(), Workbench.this);
    
      try {
        gedcom = reader.read();
        
        // grab warnings
        // FIXME docket warnings in docket
//        List warnings = reader.getWarnings();
//        if (!warnings.isEmpty()) {
//          windowManager.openNonModalDialog(null, RES.getString("cc.open.warnings", gedcom.getName()), WindowManager.WARNING_MESSAGE, new JScrollPane(new ContextListWidget(gedcom, warnings)), Action2.okOnly(), Workbench.this);
//        }
        
      } catch (GedcomEncryptionException e) {
        // retry with new password
        password = windowManager.openDialog(null, origin.getName(), WindowManager.QUESTION_MESSAGE, RES.getString("cc.provide_password"), "", Workbench.this);
      } catch (GedcomIOException ex) {
        // tell the user about it
        windowManager.openDialog(null, origin.getName(), WindowManager.ERROR_MESSAGE, RES.getString("cc.open.read_error", "" + ex.getLine()) + ":\n" + ex.getMessage(), Action2.okOnly(), Workbench.this);
        // abort
        return false;
      } finally {
        stats.handleRead(reader.getLines());
        
        // close progress
        //windowManager.close(progress);
      }
    }

    // remember
    context = new Context(gedcom);

    GedcomDirectory.getInstance().registerGedcom(gedcom);

    // enable actions
    for (Action a : gedcomActions) 
      a.setEnabled(true);

    // FIXME open views again
    // if (Options.getInstance().isRestoreViews) {
    // for (int i=0;i<views2restore.size();i++) {
    // ViewHandle handle = ViewHandle.restore(viewManager,
    // gedcomBeingLoaded, (String)views2restore.get(i));
    // if (handle!=null)
    // new
    // ActionSave(gedcomBeingLoaded).setTarget(handle.getView()).install(handle.getView(),
    // JComponent.WHEN_IN_FOCUSED_WINDOW);
    // }
    // }

    stats.setGedcom(gedcom);

    // done
    return true;
  }
  
  /**
   * save gedcom file
   */
  public boolean saveAsGedcom() {
    
    if (context == null)
      return false;
    
    // ask everyone to commit their data
    fireCommit();
    
    // .. choose file
    // FIXME docket let views participate in save filter
    SaveOptionsWidget options = new SaveOptionsWidget(context.getGedcom(), new Filter[] {});
    // (Filter[])viewManager.getViews(Filter.class, gedcomBeingSaved));
    File file = chooseFile(RES.getString("cc.save.title"), RES.getString("cc.save.action"), options);
    if (file == null)
      return false;
  
    // Need confirmation if File exists?
    if (file.exists()) {
      int rc = windowManager.openDialog(null, RES.getString("cc.save.title"), WindowManager.WARNING_MESSAGE, RES.getString("cc.open.file_exists", file.getName()), Action2.yesNo(), Workbench.this);
      if (rc != 0) 
        return false;
    }
    
    // .. take chosen one & filters
    if (!file.getName().endsWith(".ged"))
      file = new File(file.getAbsolutePath() + ".ged");
    
    Filter[] filters = options.getFilters();
    String password = Gedcom.PASSWORD_NOT_SET;
    if (context.getGedcom().hasPassword())
      password = options.getPassword();
    String encoding = options.getEncoding();
    
    // swivel gedcom
    Gedcom gedcom = context.getGedcom();
    gedcom.setPassword(password);
    gedcom.setEncoding(encoding);
    // .. create new origin
    try {
      gedcom.setOrigin(Origin.create(new URL("file", "", file.getAbsolutePath())));
    } catch (Throwable t) {
      LOG.log(Level.FINER, "Failed to create origin for file "+file, t);
      return false;
    }
  
    return saveGedcomImpl(gedcom, filters);
  }
  
  /**
   * save gedcom file
   */
  public boolean saveGedcom() {

    if (context == null)
      return false;
    
    // ask everyone to commit their data
    fireCommit();
    
    // do it
    return saveGedcomImpl(context.getGedcom(), new Filter[0]);
    
  }
  
  /**
   * save gedcom file
   */
  private boolean saveGedcomImpl(Gedcom gedcom, Filter[] filters) {
  
//  // .. open progress dialog
//  progress = windowManager.openNonModalDialog(null, RES.getString("cc.save.saving", file.getName()), WindowManager.INFORMATION_MESSAGE, new ProgressWidget(gedWriter, getThread()), Action2.cancelOnly(), getTarget());

    // FIXME docket async write
    try {
      
      // prep files and writer
      GedcomWriter writer = null;
      File file = null, temp = null;
      try {
        // .. resolve to canonical file now to make sure we're writing to the
        // file being pointed to by a symbolic link
        file = gedcom.getOrigin().getFile().getCanonicalFile();

        // .. create a temporary output
        temp = File.createTempFile("genj", ".ged", file.getParentFile());

        // .. create writer
        writer = new GedcomWriter(gedcom, new FileOutputStream(temp));
      } catch (GedcomEncodingException gee) {
        windowManager.openDialog(null, gedcom.getName(), WindowManager.ERROR_MESSAGE, RES.getString("cc.save.write_encoding_error", gee.getMessage()), Action2.okOnly(), Workbench.this);
        return false;
      } catch (IOException ex) {
        windowManager.openDialog(null, gedcom.getName(), WindowManager.ERROR_MESSAGE, RES.getString("cc.save.open_error", gedcom.getOrigin().getFile().getAbsolutePath()), Action2.okOnly(), Workbench.this);
        return false;
      }
      writer.setFilters(filters);

      // .. write it
      writer.write();
      
      // track what we read
      stats.handleWrite(writer.getLines());
      
      // .. make backup
      if (file.exists()) {
        File bak = new File(file.getAbsolutePath() + "~");
        if (bak.exists())
          bak.delete();
        file.renameTo(bak);
      }

      // .. and now !finally! move from temp to result
      if (!temp.renameTo(file))
        throw new GedcomIOException("Couldn't move temporary " + temp.getName() + " to " + file.getName(), -1);

    } catch (GedcomIOException gioex) {
      windowManager.openDialog(null, gedcom.getName(), WindowManager.ERROR_MESSAGE, RES.getString("cc.save.write_error", "" + gioex.getLine()) + ":\n" + gioex.getMessage(), Action2.okOnly(), Workbench.this);
      return false;
    }

//  // close progress
//  windowManager.close(progress);
    
    // .. note changes are saved now
    gedcom.doMuteUnitOfWork(new UnitOfWork() {
      public void perform(Gedcom gedcom) throws GedcomException {
        gedcom.setUnchanged();
      }
    });

    // .. done
    return false;
  }
  
  /**
   * exit workbench
   */
  public void exit() {
    
    // remember current context for exit
    registry.put("restore.url", context!=null ? context.getGedcom().getOrigin().toString() : "");
    
    // Close all Windows
    windowManager.closeAll();

    // Shutdown
    runOnExit.run();
  }
  
  /**
   * closes gedcom file
   */
  public boolean closeGedcom() {
    
    if (context==null)
      return true;
    
    // commit changes
    fireCommit();
    
    // changes?
    if (context.getGedcom().hasChanged()) {
      
      // close file officially
      int rc = windowManager.openDialog("confirm-exit", null, WindowManager.WARNING_MESSAGE, RES.getString("cc.savechanges?", context.getGedcom().getName()), Action2.yesNoCancel(), Workbench.this);
      // cancel - we're done
      if (rc == 2)
        return false;
      // yes - close'n save it
      if (rc == 0) 
        if (!saveGedcom())
          return false;

    }

    // close dockets
    for (Object key : dockingPane.getDockableKeys())
      dockingPane.removeDockable(key);
    
    // disable buttons
    for (Action a : gedcomActions) 
      a.setEnabled(false);
    
    // clear stats
    stats.setGedcom(null);
    
    // unregister
    GedcomDirectory.getInstance().unregisterGedcom(context.getGedcom());
    context = null;

    // done
    return true;
  }
  
  /**
   * Restores last loaded gedcom file
   */
  public void restoreGedcom() {

    String restore = registry.get("restore.url", (String)null);
    try {
      // no known key means load default
      if (restore==null)
        restore = new File("gedcom/example.ged").toURI().toURL().toString();
      // known key needs value
      if (restore.length()>0)
        openGedcom(new URL(restore));
    } catch (Throwable t) {
      LOG.log(Level.WARNING, "unexpected error", t);
    }
  }
  
  /**
   * Returns a menu for frame showing this controlcenter
   */
  /* package */JMenuBar getMenuBar() {
    return menuBar;
  }

  /**
   * Lookup all workbench related action providers (from views and plugins)
   */
  /*package*/ List<ActionProvider> getActionProviders() {
    
    List<ActionProvider> result = new ArrayList<ActionProvider>();
    
    // check all dock'd views
    for (Object key : dockingPane.getDockableKeys()) {
      Dockable dockable = dockingPane.getDockable(key);
      if (dockable instanceof ViewDockable) {
        ViewDockable vd = (ViewDockable)dockable;
        if (vd.getContent() instanceof ActionProvider)
          result.add((ActionProvider)vd.getContent());
      }
    }
    
    // check all plugins
    for (Object plugin : plugins) {
      if (plugin instanceof ActionProvider)
        result.add((ActionProvider)plugin);
    }
    
    // sort by priority
    Collections.sort(result, new Comparator<ActionProvider>() {
      public int compare(ActionProvider a1, ActionProvider a2) {
        return a2.getPriority() - a1.getPriority();
      }
    });
    
    return result;
  }

  /**
   * Returns a button bar for the top
   */
  private JToolBar createToolBar() {

    // create toolbar and setup helper
    JToolBar result = new JToolBar();
    result.setFloatable(false);
    ButtonHelper bh = new ButtonHelper().setInsets(4).setContainer(result).setFontSize(10);

    // Open & New |
    Action2 actionNew = new ActionNew();
    Action2 actionOpen = new ActionOpen();
    Action2 actionSave = new ActionSave(false);
    actionNew.setText(null);
    actionOpen.setText(null);
    actionSave.setText(null);
    gedcomActions.add(actionSave);

    bh.create(actionNew);
    bh.create(actionOpen);
    bh.create(actionSave);

    result.addSeparator();

// Views is not something folks will choose much - we want to fill it with functionality instead
//    for (ViewFactory factory : ServiceLookup.lookup(ViewFactory.class)) {
//      ActionOpenView action = new ActionOpenView(factory);
//      action.setText(null);
//      bh.create(action);
//      gedcomActions.add(action);
//    }

    // some glue at the end to space things out
    result.add(Box.createGlue());

    // done
    return result;
  }

  /**
   * Creates our MenuBar
   */
  private JMenuBar createMenuBar() {

    MenuHelper mh = new MenuHelper();
    JMenuBar result = mh.createBar();

    // Create Menues
    mh.createMenu(RES.getString("cc.menu.file"));
    mh.createItem(new ActionNew());
    mh.createItem(new ActionOpen());

    Action2 save = new ActionSave(false);
    Action2 saveAs = new ActionSave(true);
    gedcomActions.add(save);
    gedcomActions.add(saveAs);
    mh.createItem(save);
    mh.createItem(saveAs);

    mh.createSeparator();
    mh.createItem(new ActionClose());

    if (!EnvironmentChecker.isMac()) { // Mac's don't need exit actions in
                                       // application menus apparently
      mh.createItem(new ActionExit());
    }

    mh.popMenu().createMenu(RES.getString("cc.menu.view"));

    for (ViewFactory factory : ServiceLookup.lookup(ViewFactory.class)) {
      ActionOpenView action = new ActionOpenView(factory);
      gedcomActions.add(action);
      mh.createItem(action);
    }
    mh.createSeparator();
    mh.createItem(new ActionOptions());

    // 20060209
    // Stephane reported a problem running GenJ on MacOS Tiger:
    //
    // java.lang.ArrayIndexOutOfBoundsException: 3 > 2::
    // at java.util.Vector.insertElementAt(Vector.java:557)::
    // at apple.laf.ScreenMenuBar.add(ScreenMenuBar.java:266)::
    // at apple.laf.ScreenMenuBar.addSubmenu(ScreenMenuBar.java:207)::
    // at apple.laf.ScreenMenuBar.addNotify(ScreenMenuBar.java:53)::
    // at java.awt.Frame.addNotify(Frame.java:478)::
    // at java.awt.Window.pack(Window.java:436)::
    // atgenj.window.DefaultWindowManager.openFrameImpl(Unknown Source)::
    // at genj.window.AbstractWindowManager.openFrame(Unknown Source)::
    // at genj.app.App$Startup.run(Unknown Source)::
    // 
    // apparently something wrong with how the Mac parses the menu-bar
    // According to this post
    // http://lists.apple.com/archives/java-dev/2005/Aug/msg00060.html
    // the offending thing might be a non-menu-item (glue) added to the menu
    // as we did here previously - so let's remove that for Macs for now
    // 20061116 remove the glue in all situations - we don't have to hide help
    // on the right
    // if (!EnvironmentChecker.isMac())
    // result.add(Box.createHorizontalGlue());

    mh.popMenu().createMenu(RES.getString("cc.menu.help"));

    mh.createItem(new ActionHelp());
    mh.createItem(new ActionAbout());

    // Done
    return result;
  }

  public void fireCommit() {
    for (WorkbenchListener listener : listeners)
      listener.commitRequested();
  }
  
  public void fireSelection(Context context, boolean isActionPerformed) {
    
    this.context = new Context(context);
    
    for (WorkbenchListener listener : listeners) 
      listener.selectionChanged(context, isActionPerformed);
  }

  public void addWorkbenchListener(WorkbenchListener listener) {
    listeners.add(listener);
  }

  public void removeWorkbenchListener(WorkbenchListener listener) {
    listeners.remove(listener);
  }

  /**
   * (re)open a view
   */
  public View openView(ViewFactory factory, Context context) {
    
    // grab current Gedcom
    if (context == null)
      throw new IllegalArgumentException("Cannot open view without context");

    // already open or new
    ViewDockable dockable = (ViewDockable)dockingPane.getDockable(factory);
    if (dockable != null) {
      // bring forward
      dockingPane.putDockable(factory, dockable);
      // done
      return dockable.getView();
    }
    dockable = new ViewDockable(Workbench.this, factory, context);

    dockingPane.putDockable(factory, dockable);

    dockable.getDocked().addTool(new ActionCloseView(factory));

    // FIXME install some accelerators
    // new
    // ActionSave(gedcom).setTarget(handle.getView()).install(handle.getView(),
    // JComponent.WHEN_IN_FOCUSED_WINDOW);
    
    return dockable.getView();
  }

  /**
   * Let the user choose a file
   */
  private File chooseFile(String title, String action, JComponent accessory) {
    FileChooser chooser = new FileChooser(Workbench.this, title, action, "ged", EnvironmentChecker.getProperty(Workbench.this, new String[] { "genj.gedcom.dir", "user.home" }, ".", "choose gedcom file"));
    chooser.setCurrentDirectory(new File(registry.get("last.dir", "user.home")));
    if (accessory != null)
      chooser.setAccessory(accessory);
    if (JFileChooser.APPROVE_OPTION != chooser.showDialog())
      return null;
    // check the selection
    File file = chooser.getSelectedFile();
    if (file == null)
      return null;
    // remember last directory
    registry.put("last.dir", file.getParentFile().getAbsolutePath());
    // done
    return file;
  }

  /**
   * Action - about
   */
  private class ActionAbout extends Action2 {
    /** constructor */
    protected ActionAbout() {
      setText(RES, "cc.menu.about");
      setImage(Images.imgAbout);
    }

    /** run */
    public void actionPerformed(ActionEvent event) {
      if (windowManager.show("about"))
        return;
      windowManager.openDialog("about", RES.getString("cc.menu.about"), WindowManager.INFORMATION_MESSAGE, new AboutWidget(), Action2.okOnly(), Workbench.this);
      // done
    }
  } // ActionAbout

  /**
   * Action - help
   */
  private class ActionHelp extends Action2 {
    /** constructor */
    protected ActionHelp() {
      setText(RES, "cc.menu.contents");
      setImage(Images.imgHelp);
    }

    /** run */
    public void actionPerformed(ActionEvent event) {
      if (windowManager.show("help"))
        return;
      windowManager.openWindow("help", RES.getString("cc.menu.help"), Images.imgHelp, new HelpWidget(), null, null);
      // done
    }
  } // ActionHelp

  /**
   * Action - exit
   */
  private class ActionExit extends Action2 {
    
    /** constructor */
    protected ActionExit() {
      setAccelerator(ACC_EXIT);
      setText(RES, "cc.menu.exit");
      setImage(Images.imgExit);
      setTarget(Workbench.this);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      exit();
    }
  }
  
  /**
   * Action - close and exit
   */
  private class ActionClose extends Action2 {
    
    /** constructor */
    protected ActionClose() {
      setText(RES, "cc.menu.close");
      setImage(Images.imgClose);
      setTarget(Workbench.this);
    }
    
    /** run */
    public void actionPerformed(ActionEvent event) {
      closeGedcom();
    }
  } // ActionExit

  /**
   * Action - new
   */
  private class ActionNew extends Action2 {

    /** constructor */
    ActionNew() {
      setAccelerator(ACC_NEW);
      setText(RES, "cc.menu.new");
      setTip(RES, "cc.tip.create_file");
      setImage(Images.imgNew);
    }

    /** execute callback */
    public void actionPerformed(ActionEvent event) {

      // let user choose a file
      File file = chooseFile(RES.getString("cc.create.title"), RES.getString("cc.create.action"), null);
      if (file == null)
        return;
      if (!file.getName().endsWith(".ged"))
        file = new File(file.getAbsolutePath() + ".ged");
      if (file.exists()) {
        int rc = windowManager.openDialog(null, RES.getString("cc.create.title"), WindowManager.WARNING_MESSAGE, RES.getString("cc.open.file_exists", file.getName()), Action2.yesNo(), Workbench.this);
        if (rc != 0)
          return;
      }
      // form the origin
      try {
        Gedcom newGedcom = new Gedcom(Origin.create(new URL("file", "", file.getAbsolutePath())));
        // create default entities
        try {
          Indi adam = (Indi) newGedcom.createEntity(Gedcom.INDI);
          adam.addDefaultProperties();
          adam.setName("Adam", "");
          adam.setSex(PropertySex.MALE);
          Submitter submitter = (Submitter) newGedcom.createEntity(Gedcom.SUBM);
          submitter.setName(EnvironmentChecker.getProperty(this, "user.name", "?", "user name used as submitter in new gedcom"));
        } catch (GedcomException e) {
        }
        // remember
        GedcomDirectory.getInstance().registerGedcom(newGedcom);
      } catch (MalformedURLException e) {
      }

    }

  } // ActionNew

  /**
   * Action - open
   */
  private class ActionOpen extends Action2 {

    /** constructor - good for button or menu item */
    protected ActionOpen() {
      setAccelerator(ACC_OPEN);
      setTip(RES, "cc.tip.open_file");
      setText(RES, "cc.menu.open");
      setImage(Images.imgOpen);
    }

    /**
     * (Async) execute
     */
    public void actionPerformed(ActionEvent event) {
      openGedcom();
    }
  } // ActionOpen

  /**
   * Action - Save
   */
  private class ActionSave extends Action2 {
    /** whether to ask user */
    private boolean saveAs;
    /** gedcom */
    protected Gedcom gedcomBeingSaved;
    /** writer */
    private GedcomWriter gedWriter;
    /** origin to load after successfull save */
    private Origin newOrigin;
    /** filters we're using */
    private Filter[] filters;
    /** progress key */
    private String progress;
    /** exception we might encounter */
    private GedcomIOException ioex = null;
    /** temporary and target file */
    private File temp, file;
    /** password used */
    private String password;

    /**
     * Constructor for saving gedcom 
     */
    protected ActionSave(boolean saveAs) {
      // setup default target
      setTarget(Workbench.this);
      // setup accelerator - IF this is a no-ask save it instead of SaveAs
      if (!saveAs)
        setAccelerator(ACC_SAVE);
      // remember
      this.saveAs = saveAs;
      // text
      if (saveAs)
        setText(RES.getString("cc.menu.saveas"));
      else
        setText(RES.getString("cc.menu.save"));
      setTip(RES, "cc.tip.save_file");
      // setup
      setImage(Images.imgSave);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (saveAs)
        saveAsGedcom();
      else
        saveGedcom();
    }

  } // ActionSave

  /**
   * Action - View
   */
  private class ActionOpenView extends Action2 {
    /** which ViewFactory */
    private ViewFactory factory;

    /** constructor */
    protected ActionOpenView(ViewFactory vw) {
      factory = vw;
      setText(factory.getTitle());
      setTip(RES.getString("cc.tip.open_view", factory.getTitle()));
      setImage(factory.getImage());
      setEnabled(false);
    }

    /** run */
    public void actionPerformed(ActionEvent event) {
      if (context==null)
        return;
      if (context.getEntity()==null) {
        Entity adam = context.getGedcom().getFirstEntity(Gedcom.INDI);
        if (adam!=null)
          context = new Context(adam);
      }
      openView(factory, context);
    }
  } // ActionOpenView

  /**
   * Action - close view
   */
  private class ActionCloseView extends Action2 {
    private ViewFactory factory;

    /** constructor */
    protected ActionCloseView(ViewFactory factory) {
      setImage(Images.imgClose);
      this.factory = factory;
    }

    /** run */
    public void actionPerformed(ActionEvent event) {
      dockingPane.removeDockable(factory);
    }
  } // ActionCloseView

  /**
   * Action - Options
   */
  private class ActionOptions extends Action2 {
    /** constructor */
    protected ActionOptions() {
      setText(RES.getString("cc.menu.options"));
      setImage(OptionsWidget.IMAGE);
    }

    /** run */
    public void actionPerformed(ActionEvent event) {
      // create widget for options
      OptionsWidget widget = new OptionsWidget(getText());
      widget.setOptions(OptionProvider.getAllOptions());
      // open dialog
      windowManager.openDialog("options", getText(), WindowManager.INFORMATION_MESSAGE, widget, Action2.okOnly(), Workbench.this);
      // done
    }
  } // ActionOptions

  /**
   * a little status tracker
   */
  private static class StatusBar extends JPanel implements GedcomMetaListener {

    private Gedcom gedcom;
    private int commits;
    private int read, written;

    private JLabel[] ents = new JLabel[Gedcom.ENTITIES.length];
    private JLabel changes;

    StatusBar() {

      super(new NestedBlockLayout("<row><i/><f/><m/><n/><s/><b/><r/><cs wx=\"1\" gx=\"1\"/><mem/></row>"));

      for (int i = 0; i < Gedcom.ENTITIES.length; i++) {
        ents[i] = new JLabel("0", Gedcom.getEntityImage(Gedcom.ENTITIES[i]), SwingConstants.LEFT);
        add(ents[i]);
      }
      changes = new JLabel("", SwingConstants.RIGHT);
      
      add(changes);
      add(new HeapStatusWidget());

    }
    
    void setGedcom(Gedcom gedcom) {
      if (this.gedcom!=null)
        this.gedcom.removeGedcomListener(this);
      this.gedcom = gedcom;
      if (this.gedcom!=null)
        this.gedcom.addGedcomListener(this);
      commits = 0;
      read = 0;
      written = 0;
      update();
    }

    public void gedcomWriteLockReleased(Gedcom gedcom) {
      commits++;
      update();
    }

    public synchronized void handleRead(int lines) {
      read += lines;
      update();
    }

    public synchronized void handleWrite(int lines) {
      written += lines;
      update();
    }
    
    private String count(int type) {
      return gedcom==null ? "-" : ""+gedcom.getEntities(Gedcom.ENTITIES[type]).size();
    }

    private void update() {

      for (int i=0;i<Gedcom.ENTITIES.length;i++) {
        ents[i].setText(count(i));
      }

      WordBuffer buf = new WordBuffer(", ");
      if (commits > 0)
        buf.append(RES.getString("stat.commits", new Integer(commits)));
      if (read > 0)
        buf.append(RES.getString("stat.lines.read", new Integer(read)));
      if (written > 0)
        buf.append(RES.getString("stat.lines.written", new Integer(written)));
      changes.setText(buf.toString());
    }

    public void gedcomHeaderChanged(Gedcom gedcom) {
    }

    public void gedcomBeforeUnitOfWork(Gedcom gedcom) {
    }

    public void gedcomAfterUnitOfWork(Gedcom gedcom) {
    }

    public void gedcomWriteLockAcquired(Gedcom gedcom) {
    }

    public void gedcomEntityAdded(Gedcom gedcom, Entity entity) {
    }

    public void gedcomEntityDeleted(Gedcom gedcom, Entity entity) {
    }

    public void gedcomPropertyAdded(Gedcom gedcom, Property property, int pos, Property added) {
    }

    public void gedcomPropertyChanged(Gedcom gedcom, Property prop) {
    }

    public void gedcomPropertyDeleted(Gedcom gedcom, Property property, int pos, Property removed) {
    }

  } // Stats

} // ControlCenter
