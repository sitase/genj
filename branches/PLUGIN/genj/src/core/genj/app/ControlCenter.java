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
package genj.app;

import genj.common.ContextListWidget;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;
import genj.gedcom.GedcomLifecycleEvent;
import genj.gedcom.GedcomLifecycleListener;
import genj.gedcom.Indi;
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
import genj.plugin.PluginManager;
import genj.util.DirectAccessTokenizer;
import genj.util.EnvironmentChecker;
import genj.util.Origin;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.WordBuffer;
import genj.util.swing.Action2;
import genj.util.swing.ChoiceWidget;
import genj.util.swing.FileChooser;
import genj.util.swing.HeapStatusWidget;
import genj.util.swing.ProgressWidget;
import genj.view.ViewContext;
import genj.window.WindowBroadcastEvent;
import genj.window.WindowBroadcastListener;
import genj.window.WindowClosingEvent;
import genj.window.WindowManager;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import spin.Spin;

/**
 * The central component of the GenJ application
 */
public class ControlCenter extends JPanel implements WindowBroadcastListener {
  
  private final static String
    ACC_SAVE = "ctrl S",
    ACC_EXIT = "ctrl X",
    ACC_NEW = "ctrl N",
    ACC_OPEN = "ctrl O";

  /** members */
  private GedcomTableWidget tGedcoms;
  private Registry registry;
  private Resources resources = Resources.get(this);
  private WindowManager windowManager;
  private Stats stats = new Stats();
  
  /**
   * Constructor
   */
  public ControlCenter(Registry setRegistry, WindowManager winManager, String[] args) {

    // Initialize data
    registry = new Registry(setRegistry, "cc");
    windowManager = winManager;
    
    // Table of Gedcoms
    tGedcoms = new GedcomTableWidget(registry) {
      public ViewContext getContext() {
        ViewContext result = super.getContext();
        if (result!=null) {
          result.addAction(new ActionSave(false, true));
          result.addAction(new ActionClose(true));
          result.addSeparator();
        }
        return result;
      };
    };
    
    // status bar
    HeapStatusWidget mem = new HeapStatusWidget();
    mem.setToolTipText(resources.getString("cc.heap"));
    JPanel status = new JPanel(new BorderLayout());
    status .add(BorderLayout.CENTER, stats);
    status.add(BorderLayout.EAST, mem);
    
    // listen to selections
    tGedcoms.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        new UpdateActions().trigger();
      }
    });

    // Layout
    setLayout(new BorderLayout());
    add(new JScrollPane(tGedcoms), BorderLayout.CENTER);
    add(status, BorderLayout.SOUTH);
    
    // Load known gedcoms
    SwingUtilities.invokeLater(new UpdateActions());
    SwingUtilities.invokeLater(new ActionAutoOpen(args));
    
    // Done
  }

  /**
   * @see WindowBroadcastListener#handleBroadcastEvent(WindowBroadcastEvent)
   */
  public boolean handleBroadcastEvent(WindowBroadcastEvent event) {
    // let us handle closing
    if (event instanceof WindowClosingEvent) {
      ((WindowClosingEvent)event).cancel();
      new ActionExit().trigger();
    }
    // continue
    return true;
  }
  
  /**
   * Adds another Gedcom to the list of Gedcoms
   */
  /*package*/ void addGedcom(Gedcom gedcom) {
    
    // keep it
    tGedcoms.addGedcom(gedcom);
    gedcom.addLifecycleListener((GedcomLifecycleListener)Spin.over((GedcomLifecycleListener)stats));
    
    // let plugins know
    PluginManager.get().extend(new ExtendGedcomOpened(gedcom));

  }

  /**
   * Removes a Gedcom from the list of Gedcoms
   */
  /*package*/ void removeGedcom(Gedcom gedcom) {
    
    // let plugins know
    PluginManager.get().extend(new ExtendGedcomClosed(gedcom));

    // forget about it
    tGedcoms.removeGedcom(gedcom);
    gedcom.removeLifecycleListener((GedcomLifecycleListener)Spin.over((GedcomLifecycleListener)stats));
  }
  
  /**
   * Let the user choose a file
   */
  private File chooseFile(String title, String action, JComponent accessory) {
    FileChooser chooser = new FileChooser(
      ControlCenter.this, title, action, "ged",
      EnvironmentChecker.getProperty(ControlCenter.this, new String[] { "genj.gedcom.dir", "user.home" } , ".", "choose gedcom file")
    );
    chooser.setCurrentDirectory(new File(registry.get("last.dir", "user.home")));
    if (accessory!=null) chooser.setAccessory(accessory);
    if (JFileChooser.APPROVE_OPTION!=chooser.showDialog())
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
   * Updated actions
   */
  private class UpdateActions extends Action2 {
    protected void execute() {
      
      Gedcom selection = tGedcoms.getSelectedGedcom();
    
      // collect our menubar as an extension point
      ExtendMenubar em = new ExtendMenubar(selection, ControlCenter.this);
  
      em.addAction(ExtendMenubar.FILE_MENU, new ActionNew());
      em.addAction(ExtendMenubar.FILE_MENU, new ActionOpen());
      em.addSeparator(ExtendMenubar.FILE_MENU);
      em.addAction(ExtendMenubar.FILE_MENU, new ActionSave(false, selection!=null));
      em.addAction(ExtendMenubar.FILE_MENU, new ActionSave(true, selection!=null));
      em.addAction(ExtendMenubar.FILE_MENU, new ActionClose(selection!=null));
      if (!EnvironmentChecker.isMac()) { // Mac's don't need exit actions in application menus apparently
        em.addSeparator(ExtendMenubar.FILE_MENU);
        em.addAction(ExtendMenubar.FILE_MENU, new ActionExit());
      }
      em.addSeparator(ExtendMenubar.FILE_MENU);
      
      // ask plugins for their contributions
      PluginManager.get().extend(em);
      
      // add tools
      em.addAction(ExtendMenubar.TOOLS_MENU, new ActionOptions());
      
      // add help menu
      em.addAction(ExtendMenubar.HELP_MENU, new ActionHelp());
      em.addAction(ExtendMenubar.HELP_MENU, new ActionAbout());
      
      // set menubar as is
      WindowManager.setMenubar(ControlCenter.this, em.getMenuBar());
      
      // collect our toolbar as an extension point
      ExtendToolbar et = new ExtendToolbar(selection, ControlCenter.this);
      et.addAction(new ActionNew());
      et.addAction(new ActionOpen());
      et.addAction(new ActionSave(false, selection!=null));
      et.addSeparator();
  
      // ask plugins for their contributions
      PluginManager.get().extend(et);
      
      // set toolbar as is
      JToolBar tb = et.getToolbar();
      tb.setFloatable(false);
      WindowManager.setToolbar(ControlCenter.this, tb);
      
      // Done
    }
 
  } //UpdateActions
  
  /**
   * Action - about
   */
  private class ActionAbout extends Action2 {
    /** constructor */
    protected ActionAbout() {
      setText(resources, "cc.menu.about");
      setImage(Images.imgAbout);
    }
    /** run */
    protected void execute() {
      if (windowManager.show("about"))
        return;
      windowManager.openDialog("about",resources.getString("cc.menu.about"),WindowManager.INFORMATION_MESSAGE,new AboutWidget(),Action2.okOnly(),ControlCenter.this);
      // done      
    }
  } //ActionAbout

  /**
   * Action - help
   */
  private class ActionHelp extends Action2 {
    /** constructor */
    protected ActionHelp() {
      setText(resources, "cc.menu.contents");
      setImage(Images.imgHelp);
    }
    /** run */
    protected void execute() {
      if (windowManager.show("help"))
        return;
      windowManager.openWindow("help",resources.getString("cc.menu.help"),Images.imgHelp,new HelpWidget());
      // done
    }
  } //ActionHelp

  /**
   * Action - exit
   */
  private class ActionExit extends Action2 {
    /** constructor */
    protected ActionExit() {
      setAccelerator(ACC_EXIT);
      setText(resources, "cc.menu.exit");
      setImage(Images.imgExit);
      setTarget(ControlCenter.this);
    }
    /** run */
    protected void execute() {
      // Remember open gedcoms
      Collection save = new ArrayList();
      for (Iterator gedcoms=new ArrayList(tGedcoms.getAllGedcoms()).iterator(); gedcoms.hasNext(); ) {
        // next gedcom
        Gedcom gedcom = (Gedcom) gedcoms.next();
        // changes need saving?
        if (gedcom.hasUnsavedChanges()) {
          // close file officially
          int rc = windowManager.openDialog(
              "confirm-exit", null, WindowManager.WARNING_MESSAGE, 
              resources.getString("cc.savechanges?", gedcom.getName()), 
              Action2.yesNoCancel(), ControlCenter.this
            );
          // cancel - we're done
          if (rc==2) return;
          // yes - close'n save it
          if (rc==0) {
            // block exit
            ActionExit.this.setEnabled(false);
            // run save
            new ActionSave(gedcom) {
              // apres save
              protected void postExecute(boolean preExecuteResult) {
                try {
                  // super first
                  super.postExecute(preExecuteResult);
                  // stop still unsaved changes that didn't make it through saving
                  if (gedcomBeingSaved.hasUnsavedChanges()) 
                    return;
                } finally {
                  // unblock exit
                  ActionExit.this.setEnabled(true);
                }
                // continue with exit
                ActionExit.this.trigger();
              }
            }.trigger();
            return;
          }
          // no - skip it
        }
        // drop it
        removeGedcom(gedcom);
        // remember as being open, password and open views
        File file =gedcom.getOrigin().getFile(); 
        if (file==null||file.exists()) { 
          StringBuffer restore = new StringBuffer();
          restore.append(gedcom.getOrigin());
          restore.append(",");
          if (gedcom.hasPassword())
            restore.append(gedcom.getPassword());
          save.add(restore);
        }
        // next gedcom
      }
      registry.put("open", save);
      
      // Close all Windows
      windowManager.closeAll();
      
      // Shutdown - wanted to do without but SingUtilities creates
      // a hidden frame that sticks around (for null-parent dialogs)
      // preventing the event dispatcher thread from shutting down
      System.exit(0);

      // Done
    }
  } //ActionExit

  /**
   * Action - new
   */
  private class ActionNew extends Action2 {
    
    /** constructor */
    ActionNew() {
      setAccelerator(ACC_NEW);
      setText(resources, "cc.menu.new" );
      setTip(resources, "cc.tip.create_file");
      setImage(Images.imgNew);
    }

    /** execute callback */
    protected void execute() {
      
        // let user choose a file
        File file = chooseFile(resources.getString("cc.create.title"), resources.getString("cc.create.action"), null);
        if (file == null)
          return;
        if (!file.getName().endsWith(".ged"))
          file = new File(file.getAbsolutePath()+".ged");
        if (file.exists()) {
          int rc = windowManager.openDialog(
            null,
            resources.getString("cc.create.title"),
            WindowManager.WARNING_MESSAGE,
            resources.getString("cc.open.file_exists", file.getName()),
            Action2.yesNo(),
            ControlCenter.this
          );
          if (rc!=0)
            return;
        }
        // form the origin
        try {
          Gedcom gedcom  = new Gedcom(Origin.create(new URL("file", "", file.getAbsolutePath())));
          // create default entities
          try {
            Indi adam = (Indi)gedcom.createEntity(Gedcom.INDI);
            adam.addDefaultProperties();
            adam.setName("Adam","");
            adam.setSex(PropertySex.MALE);
            Submitter submitter = (Submitter)gedcom.createEntity(Gedcom.SUBM);
            submitter.setName(EnvironmentChecker.getProperty(this, "user.name", "?", "user name used as submitter in new gedcom"));
          } catch (GedcomException e) {
          }
          // remember
          addGedcom(gedcom);
        } catch (MalformedURLException e) {
        }

    }
    
  } //ActionNew
  
  /**
   * Action - open
   */
  private class ActionOpen extends Action2 {

    /** a preset origin we're reading from */
    private Origin origin;

    /** a reader we're working on */
    private GedcomReader reader;

    /** an error we might encounter */
    private GedcomIOException exception;

    /** a gedcom we're creating */
    protected Gedcom gedcomBeingLoaded;
    
    /** key of progress dialog */
    private String progress;
    
    /** password in use */
    private String password = Gedcom.PASSWORD_NOT_SET;
    
    /** views to load */
    private List views2restore = new ArrayList();
    
    /** constructor - good for reloading */
    protected ActionOpen(String restore) throws MalformedURLException {
      
      setAsync(ASYNC_SAME_INSTANCE);
      
      // grab "file[, password][, view#x]"
      DirectAccessTokenizer tokens = new DirectAccessTokenizer(restore, ",", false);
      String url = tokens.get(0);
      String pwd = tokens.get(1);
      if (url==null)
        throw new IllegalArgumentException("can't restore "+restore);

      origin = Origin.create(url);
      if (pwd!=null&&pwd.length()>0) password = pwd;
      
      // grab views we're going to open if successful
      for (int i=2; ; i++) {
        String token = tokens.get(i);
        if (token==null) break;
        if (token.length()>0) views2restore.add(tokens.get(i));
      }
      
      // done
    }

    /** constructor - good for button or menu item */
    protected ActionOpen() {
      setAccelerator(ACC_OPEN); 
      setTip(resources, "cc.tip.open_file");
      setText(resources, "cc.menu.open");
      setImage(Images.imgOpen);
      setAsync(ASYNC_NEW_INSTANCE);
    }

    /** constructor - good for loading a specific file*/
    protected ActionOpen(Origin setOrigin) {
      setAsync(ASYNC_SAME_INSTANCE);
      origin = setOrigin;
    }

    /**
     * (sync) pre execute
     */
    protected boolean preExecute() {
      
      // need to ask for origin?
      if (origin==null) {
        Action actions[] = {
          new Action2(resources, "cc.open.choice.local"),
          new Action2(resources, "cc.open.choice.inet" ),
          Action2.cancel(),
        };
        int rc = windowManager.openDialog(
          null,
          resources.getString("cc.open.title"),
          WindowManager.QUESTION_MESSAGE,
          resources.getString("cc.open.choice"),
          actions,
          ControlCenter.this
        );
        switch (rc) {
          case 0 :
            origin = chooseExisting();
            break;
          case 1 :
            origin = chooseURL();
            break;
        }
      }      
      // try to open it
      return origin==null ? false : open(origin);
    }

    /**
     * (Async) execute
     */
    protected void execute() {
      try {
        gedcomBeingLoaded = reader.read();
      } catch (GedcomIOException ex) {
        exception = ex;
      }
    }

    /**
     * (sync) post execute
     */
    protected void postExecute(boolean preExecuteResult) {
      
      // close progress
      windowManager.close(progress);
      
      // any error bubbling up?
      if (exception != null) {
        
        // maybe try with different password
        if (exception instanceof GedcomEncryptionException) {
          
          password = windowManager.openDialog(
            null, 
            origin.getName(), 
            WindowManager.QUESTION_MESSAGE, 
            resources.getString("cc.provide_password"),
            "", 
            ControlCenter.this
          );
          
          if (password==null)
            password = Gedcom.PASSWORD_UNKNOWN;
          
          // retry
          exception = null;
          trigger();
          
          return;
        }

        // tell the user about it        
        windowManager.openDialog(
          null, 
          origin.getName(), 
          WindowManager.ERROR_MESSAGE, 
          resources.getString("cc.open.read_error", "" + exception.getLine()) + ":\n" + exception.getMessage(),
          Action2.okOnly(), 
          ControlCenter.this
        );
        
        return;

      }
        
      // got a successfull gedcom
      if (gedcomBeingLoaded != null) 
        addGedcom(gedcomBeingLoaded);
      
      // show warnings&stats
      if (reader!=null) {
        
        stats.handleRead(reader.getLines());
        
        // warnings
        List warnings = reader.getWarnings();
        if (!warnings.isEmpty()) {
          windowManager.openNonModalDialog(
            null,
            resources.getString("cc.open.warnings", gedcomBeingLoaded.getName()),
            WindowManager.WARNING_MESSAGE,
            new JScrollPane(new ContextListWidget(gedcomBeingLoaded, warnings)),
            Action2.okOnly(),
            ControlCenter.this
          );
        }
      }
        
      // done
    }

    /**
     * choose a file
     */
    private Origin chooseExisting() {
      // ask user
      File file = chooseFile(resources.getString("cc.open.title"), resources.getString("cc.open.action"), null);
      if (file == null)
        return null;
      // remember last directory
      registry.put("last.dir", file.getParentFile().getAbsolutePath());
      // form origin
      try {
        return Origin.create(new URL("file", "", file.getAbsolutePath()));
      } catch (MalformedURLException e) {
        return null;
      }
      // done
    }

    /**
     * choose a url
     */
    private Origin chooseURL() {

      // pop a chooser    
      String[] choices = (String[])registry.get("urls", new String[0]);
      ChoiceWidget choice = new ChoiceWidget(choices, "");
      JLabel label = new JLabel(resources.getString("cc.open.enter_url"));
      
      int rc = windowManager.openDialog(null, resources.getString("cc.open.title"), WindowManager.QUESTION_MESSAGE, new JComponent[]{label,choice}, Action2.okCancel(), ControlCenter.this);
    
      // check the selection
      String item = choice.getText();
      if (rc!=0||item.length()==0) return null;

      // Try to form Origin
      Origin origin;
      try {
        origin = Origin.create(item);
      } catch (MalformedURLException ex) {
        windowManager.openDialog(null, item, WindowManager.ERROR_MESSAGE, resources.getString("cc.open.invalid_url"), Action2.okCancel(), ControlCenter.this);
        return null;
      }

      // Remember URL for dialog
      Set remember = new HashSet();
      remember.add(item);
      for (int c=0; c<choices.length&&c<9; c++) {
        remember.add(choices[c]);
      }
      registry.put("urls", remember);

      // ... continue
      return origin;
    }

    /**
     * Open Origin - continue with (async) execute if true
     */
    private boolean open(Origin origin) {

      // Check if already open
      if (tGedcoms.getGedcom(origin.getName())!=null) {
        windowManager.openDialog(null,origin.getName(),WindowManager.ERROR_MESSAGE,resources.getString("cc.open.already_open", origin.getName()),Action2.okOnly(),ControlCenter.this);
        return false;
      }

      // Open Connection and get input stream
      try {
        
        // .. prepare our reader
        reader = new GedcomReader(origin);
        
        // .. set password we're using
        reader.setPassword(password);

      } catch (IOException ex) {
        String txt = 
          resources.getString("cc.open.no_connect_to", origin)
            + "\n["
            + ex.getMessage()
            + "]";
        windowManager.openDialog(null, origin.getName(), WindowManager.ERROR_MESSAGE, txt, Action2.okOnly(), ControlCenter.this);
        return false;
      }

      // .. show progress dialog
      progress = windowManager.openNonModalDialog(
        null,
        resources.getString("cc.open.loading", origin.getName()),
        WindowManager.INFORMATION_MESSAGE,
        new ProgressWidget(reader, getThread()),
        Action2.cancelOnly(),
        ControlCenter.this
      );

      // .. continue into (async) execute
      return true;
    }
    
  } //ActionOpen

  /**
   * Action - LoadLastOpen
   */
  private class ActionAutoOpen extends Action2 {
    /** files to load */
    private Collection files;
    /** constructor */
    private ActionAutoOpen(String[] args) {
      
      // if we got files then we don't open old ones
      if (args.length>0) {
        files = Arrays.asList(args);
        return;
      }
      
      // by default we offer the user to load example.ged
      HashSet deflt = new HashSet();
      if (args.length==0) try {
        deflt.add(new File("gedcom/example.ged").toURL());
      } catch (Throwable t) {
        // ignored
      }

      // check registry for the previously opened now
      files = (Set)registry.get("open", deflt);
      
    }
    
    /** run */
    public void execute() {

      // Loop over files to open
      for (Iterator it = files.iterator(); it.hasNext(); ) {
        String restore = it.next().toString();
        try {
          
          // check if it's a local file
          File local  = new File(restore);
          if (local.exists())
            restore = local.toURL().toString();
          
          ActionOpen open = new ActionOpen(restore);
          open.trigger();
        } catch (Throwable t) {
          App.LOG.log(Level.WARNING, "cannot restore "+restore, t);
        }
        
        // next
      }

      // done
    }
  } //LastOpenLoader

  /**
   * Action - Save
   */
  private class ActionSave extends Action2 {
    /** whether to ask user */
    private boolean ask;
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
     * Constructor for saving gedcom file without interaction
     */
    protected ActionSave(Gedcom gedcom) {
      this(false, true);
      
      // remember gedcom
      this.gedcomBeingSaved = gedcom;
    }
    /** 
     * Constructor
     */
    protected ActionSave(boolean ask, boolean enabled) {
      // setup default target
      setTarget(ControlCenter.this);
      // setup accelerator - IF this is a no-ask save it instead of SaveAs
      if (!ask) setAccelerator(ACC_SAVE);
      // remember
      this.ask = ask;
      // text
      if (ask)
        setText(resources.getString("cc.menu.saveas"));
      else
        setText(resources.getString("cc.menu.save"));
      setTip(resources, "cc.tip.save_file");
      // setup
      setImage(Images.imgSave);
      setAsync(ASYNC_NEW_INSTANCE);
      setEnabled(enabled);
    }
    /**
     * Initialize save
     * @see genj.util.swing.Action2#preExecute()
     */
    protected boolean preExecute() {

      // Choose currently selected Gedcom if necessary
      if (gedcomBeingSaved==null) {
	      gedcomBeingSaved = tGedcoms.getSelectedGedcom();
	      if (gedcomBeingSaved == null)
	        return false;
      }
      
      // Do we need a file-dialog or not?
      Origin origin = gedcomBeingSaved.getOrigin();
      String encoding = gedcomBeingSaved.getEncoding();
      password = gedcomBeingSaved.getPassword();
      
      if (ask || origin==null || origin.getFile()==null) {

        // .. choose file
        // FIXME PLUGIN make a plugin extension point for save filters 
        SaveOptionsWidget options = new SaveOptionsWidget(gedcomBeingSaved, new Filter[0]);
        file = chooseFile(resources.getString("cc.save.title"), resources.getString("cc.save.action"), options);
        if (file==null)
          return false;

        // .. take choosen one & filters
        if (!file.getName().endsWith(".ged"))
          file = new File(file.getAbsolutePath()+".ged");
        filters = options.getFilters();
        if (gedcomBeingSaved.hasPassword())
          password = options.getPassword();
        encoding = options.getEncoding();

        // .. create new origin
        try {
          newOrigin = Origin.create(new URL("file", "", file.getAbsolutePath()));
        } catch (Throwable t) {
        }
        

      } else {

        // .. form File from URL
        file = origin.getFile();

      }

      // Need confirmation if File exists?
      if (file.exists()&&ask) {

        int rc = windowManager.openDialog(null,resources.getString("cc.save.title"),WindowManager.WARNING_MESSAGE,resources.getString("cc.open.file_exists", file.getName()),Action2.yesNo(),ControlCenter.this);
        if (rc!=0) {
          newOrigin = null;
          //20030221 no need to go for newOrigin in postExecute()
          return false;
        }
        
      }
      
      // .. open io 
      try {
        
        // .. create a temporary output
        temp = File.createTempFile("genj", ".ged", file.getParentFile());

        // .. create writer
        gedWriter =
          new GedcomWriter(gedcomBeingSaved, file.getName(), encoding, new FileOutputStream(temp));
          
        // .. set options
        gedWriter.setFilters(filters);
        gedWriter.setPassword(password);
        
        // don't think we have to catch GedcomEncodingException here
        
      } catch (IOException ex) {
          
          windowManager.openDialog(null,gedcomBeingSaved.getName(),
                      WindowManager.ERROR_MESSAGE,
                      resources.getString("cc.save.open_error", file.getAbsolutePath()),
                      Action2.okOnly(),
                      ControlCenter.this);
        return false;
      }

      // .. open progress dialog
      progress = windowManager.openNonModalDialog(
        null,
        resources.getString("cc.save.saving", file.getName()),
        WindowManager.INFORMATION_MESSAGE,
        new ProgressWidget(gedWriter, getThread()),
        Action2.cancelOnly(),
        getTarget()
      );

      // .. continue (async)
      return true;

    }

    /** 
     * (async) execute
     * @see genj.util.swing.Action2#execute()
     */
    protected void execute() {

      // catch io problems
      try {

        // .. do the write
        gedWriter.write();

        // .. make backup
        if (file.exists()) {
          File bak = new File(file.getAbsolutePath()+"~");
          if (bak.exists()) 
            bak.delete();
          file.renameTo(bak);
        }
        
        // .. and now !finally! move from temp to result
        if (!temp.renameTo(file))
          throw new GedcomIOException("Couldn't move temporary "+temp.getName()+" to "+file.getName(), -1);
       
        // .. note changes are saved now
        if (newOrigin == null) gedcomBeingSaved.doMuteUnitOfWork(new UnitOfWork() {
          public void perform(Gedcom gedcom) throws GedcomException {
            gedcomBeingSaved.setUnchanged();
          }
        });

      } catch (GedcomIOException ex) {
        ioex = ex;
      }

      // done
    }

    /**
     * (sync) post write
     * @see genj.util.swing.Action2#postExecute(boolean)
     */
    protected void postExecute(boolean preExecuteResult) {

      // close progress
      windowManager.close(progress);
      
      // problem encountered?
      if (ioex!=null) {
          if( ioex instanceof GedcomEncodingException)  {
              windowManager.openDialog(null,
                      gedcomBeingSaved.getName(),
                      WindowManager.ERROR_MESSAGE,
                      resources.getString("cc.save.write_encoding_error", ioex.getMessage() ), 
                      Action2.okOnly(),ControlCenter.this);
          }
          else {
              windowManager.openDialog(null,
                      gedcomBeingSaved.getName(),
                      WindowManager.ERROR_MESSAGE,
                      resources.getString("cc.save.write_error", "" + ioex.getLine()) + ":\n" + ioex.getMessage(),
                      Action2.okOnly(),ControlCenter.this);
              
          }
      } else {
        // SaveAs?
        if (newOrigin != null) {
          
          // .. close old
          Gedcom alreadyOpen  = tGedcoms.getGedcom(newOrigin.getName());
          if (alreadyOpen!=null)
            removeGedcom(alreadyOpen);
          
          // .. open new
          ActionOpen open = new ActionOpen(newOrigin);
          open.password = password;
          open.trigger();
          
        }
      }
      
      // track what we read
      if (gedWriter!=null)
        stats.handleWrite(gedWriter.getLines());

      // .. done
    }
    
  } //ActionSave

  /**
   * Action - Close
   */
  private class ActionClose extends Action2 {
    /** constructor */
    protected ActionClose(boolean enabled) {
      setText(resources.getString("cc.menu.close"));
      setImage(Images.imgClose);
      setEnabled(enabled);
    }
    /** run */
    protected void execute() {
  
      // Current Gedcom
      final Gedcom gedcom = tGedcoms.getSelectedGedcom();
      if (gedcom == null)
        return;
  
      // changes we should care about?      
      if (gedcom.hasUnsavedChanges()) {
        
        int rc = windowManager.openDialog(null,null,WindowManager.WARNING_MESSAGE,
            resources.getString("cc.savechanges?", gedcom.getName()),
            Action2.yesNoCancel(),ControlCenter.this);
        // cancel everything?
        if (rc==2)
          return;
        // save now?
        if (rc==0) {
          // Remove it so the user won't change it while being saved
          removeGedcom(gedcom);
          // and save
          new ActionSave(gedcom) {
            protected void postExecute(boolean preExecuteResult) {
              // super first
              super.postExecute(preExecuteResult);
              // add back if still changed
              if (gedcomBeingSaved.hasUnsavedChanges())
                addGedcom(gedcomBeingSaved);
            }
          }.trigger();
          return;
        }
      }
  
      // Remove it
      removeGedcom(gedcom);
  
      // Done
    }
  } //ActionClose

  /**
   * Action - Options
   */
  private class ActionOptions extends Action2 {
    /** constructor */
    protected ActionOptions() {
      setText(resources.getString("cc.menu.options"));
      setImage(OptionsWidget.IMAGE);
    }
    /** run */
    protected void execute() {
      // tell options about window manager - curtesy only
      Options.getInstance().setWindowManager(windowManager);
      // create widget for options
      OptionsWidget widget = new OptionsWidget(getText());
      widget.setOptions(OptionProvider.getAllOptions());
      // open dialog
      windowManager.openDialog("options", getText(), WindowManager.INFORMATION_MESSAGE, widget, Action2.okOnly(), ControlCenter.this);
      // done
    }
  } //ActionOptions

  /**
   * a little status tracker
   */
  private class Stats extends JLabel implements GedcomLifecycleListener {
    
    private int commits;
    private int read,written;
    
    private Stats() {
      setHorizontalAlignment(SwingConstants.LEFT);
    }

    public void handleLifecycleEvent(GedcomLifecycleEvent event) {
      if (event.getId()==GedcomLifecycleEvent.WRITE_LOCK_RELEASED) {
        commits++;
        update();
      }
    }
    
    public synchronized void handleRead(int lines) {
      read+=lines;
      update();
    }
    
    public synchronized void handleWrite(int lines) {
      written+=lines;
      update();
    }
    
    private void update() {
      WordBuffer buf = new WordBuffer(", ");
      if (commits>0)
        buf.append(resources.getString("stat.commits", new Integer(commits)));
      if (read>0)
        buf.append(resources.getString("stat.lines.read", new Integer(read)));
      if (written>0)
        buf.append(resources.getString("stat.lines.written", new Integer(written)));
      setText(buf.toString());
    }
    
  } //Stats
  
} //ControlCenter
