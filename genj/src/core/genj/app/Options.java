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

import genj.gedcom.PropertyFile;
import genj.io.FileAssociation;
import genj.lnf.LnF;
import genj.option.Option;
import genj.option.OptionProvider;
import genj.option.OptionUI;
import genj.option.OptionsWidget;
import genj.option.PropertyOption;
import genj.util.ActionDelegate;
import genj.util.Debug;
import genj.util.GridBagHelper;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.FileChooserWidget;
import genj.util.swing.PopupWidget;
import genj.util.swing.TextFieldWidget;
import genj.window.CloseWindow;
import genj.window.WindowManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Application options
 */
public class Options implements OptionProvider {

  /** singleton */
  private final static Options instance = new Options();
  
  /** window manager */
  private WindowManager windowManager;
  
  /** resources */
  private Resources resources;
 
  /** the current looknfeel */
  private int lookAndFeel = -1;
  
  /** the current language code */    
  private int language = -1;
  
  /** all available language codes */
  private static String[] languages;

  /** all available language codes */
  private final static String[] codes = findCodes();
  
  private static String[] findCodes() {

    // Check available language libraries
    // prepare result with default "en"
    TreeSet result = new TreeSet();
    result.add("en");

    // look for development mode ./language/xy (except 'CVS')
    File[] dirs = new File("./language").listFiles();
    if (dirs!=null) {
      for (int i = 0; i < dirs.length; i++) {
        String dir = dirs[i].getName();
        if (!"CVS".equals(dir))
          result.add(dir);
      }
    }
    
    // look for language libraries (./lib/genj_pt_BR.jar)
    File[] libs = new File("./lib").listFiles();
    if (libs!=null)
      for (int l=0;l<libs.length;l++) {
        File lib = libs[l];
        if (!lib.isFile()) continue;
        String name = lib.getName();
        if (!name.startsWith("genj_")) continue;
        if (!name.endsWith  (".jar" )) continue;
        result.add(name.substring(5, name.length()-4));
      }

    // done
    return (String[])result.toArray(new String[result.size()]);
  }

  /**
   * Instance access
   */
  public static Options getInstance() {
    return instance;
  }
  
  /**
   * Register a window manager
   */
  public void setWindowManager(WindowManager set) {
    windowManager = set;
  }

  /** 
   * Getter - looknfeel
   */
  public int getLookAndFeel() {
    // this is invoked once on option introspection
    if (lookAndFeel<0)
      setLookAndFeel(0);
    return lookAndFeel;
  }
  
  /** 
   * Setter - looknfeel
   */
  public void setLookAndFeel(int set) {
    
    // Check against available LnFs
    LnF[] lnfs = LnF.getLnFs();
    if (set<0||set>lnfs.length-1)
      set = 0;
      
    // set it - apply on root components if available
    lnfs[set].apply(windowManager!=null?windowManager.getRootComponents():null);

    // remember
    lookAndFeel = set;
    
    // done
  }
  
  /**
   * Getter - looknfeels
   */
  public LnF[] getLookAndFeels() {
    return LnF.getLnFs();
  }
  
  /**
   * Setter - language
   */
  public void setLanguage(int language) {
    
    // check bounds
    if (language<0||language>codes.length-1)
      return;

    // set locale if applicable - only from unknown
    if (this.language==-1) {
      String lang = codes[language];
      if (lang.length()>0) {
        Debug.log(Debug.INFO, this, "Switching language to "+lang);
        String country = "";
        int i = lang.indexOf('_');
        if (i>0) {
          country = lang.substring(i+1);
          lang = lang.substring(0, i);
        }
        try {
          Locale.setDefault(new Locale(lang,country));
        } catch (Throwable t) {}
      }
    }
    
    // remember
    this.language = language;

    // done
  }
  
  /**
   * Getter - language
   */
  public int getLanguage() {
    return Math.max(0,language);
  }
  
  /**
   * Getter - languages
   */
  public String[] getLanguages() {
    // not known yet?
    if (languages==null) {
      
      Resources resources = getResources();
      
      // init 'em
      languages = new String[codes.length];
      for (int i=0;i<languages.length;i++) {
        String language = resources.getString("option.language."+codes[i], false);
        languages[i] = language!=null ? language : codes[i];
      }
    }
    // done
    return languages;
  }
  
  /**
   * Lazy resources
   */
  private Resources getResources() {
    if (resources==null)
      resources = Resources.get(this);
    return resources;
  }
  
  /** 
   * Provider callback 
   */
  public List getOptions() {
    // bean property options of instance
    List result = PropertyOption.introspect(instance);
    // add an option for FileAssociations
    result.add(new FileAssociationOption());
    // done
    return result;
  }
  
  /** 
   * Option for File Associations
   */
  private static class FileAssociationOption extends Option implements OptionUI {
    
    /** the options widget */
    private OptionsWidget widget;
    
    /** the current popup widget */
    private PopupWidget popup;

    /** callback - user readble name */
    public String getName() {
      return getInstance().getResources().getString("option.fileassociations");
    }

    /** callback - persist */
    public void persist(Registry registry) {
      registry.put("associations", FileAssociation.getAll());
    }

    /** callback - restore */
    public void restore(Registry registry) {
      String[] associations = registry.get("associations", new String[0]);
      for (int i = 0; i < associations.length; i++)
        FileAssociation.add(new FileAssociation(associations[i]));
    }

    /** callback - resolve ui */    
    public OptionUI getUI(OptionsWidget widget) {
      this.widget = widget;
      return this;
    }

    /** callback - text representation = none */
    public String getTextRepresentation() {
      return null;
    }

    /** callback - component representation = button */
    public JComponent getComponentRepresentation() {
      // prepare popup widget
      popup = new PopupWidget("...");
      popup.setActions(getActions());
      // done
      return popup;
    }
    
    /**
     * calculate actions for popup
     */
    private List getActions() {
      // create action for each association
      List result = new ArrayList(10);
      Iterator it = FileAssociation.getAll().iterator();
      for (int i=1;it.hasNext();i++)
        result.add(new Action(i, (FileAssociation)it.next()));
      result.add(new Action(0, null));
      // done
      return result;
    }

    /** callback - commit */    
    public void endRepresentation() {
      // already done
    }

    /**
     * Action for UI
     */
    private class Action extends ActionDelegate { 
      /** file association */
      private FileAssociation association;
      /** constructor */
      private Action(int i, FileAssociation fa) {
        association = fa;
        setImage(PropertyFile.DEFAULT_IMAGE);
        setText(fa!=null ? i+" "+fa.getName()+" ("+fa.getSuffixes()+')' : localize("new"));
      }
      /** localize */
      private String localize(String key) {
        return Options.getInstance().getResources().getString("option.filesssociations."+key);        
      }
      /** action main */
      protected void execute() {

        // create panel with association fields
        JPanel panel = new JPanel();
        final TextFieldWidget 
          suffixes   = new TextFieldWidget(),
          name       = new TextFieldWidget();
        final FileChooserWidget
          executable = new FileChooserWidget(FileChooserWidget.EXECUTABLES);
        GridBagHelper gh = new GridBagHelper(panel);
        gh.add(new JLabel(localize("suffix"), JLabel.LEFT), 0,0,1,1,GridBagHelper.FILL_HORIZONTAL);
        gh.add(suffixes                                   , 1,0,1,1,GridBagHelper.GROWFILL_HORIZONTAL);
        gh.add(new JLabel(localize("name"  ), JLabel.LEFT), 0,1,1,1,GridBagHelper.FILL_HORIZONTAL);
        gh.add(name                                       , 1,1,1,1,GridBagHelper.GROWFILL_HORIZONTAL);
        gh.add(new JLabel(localize("exec"  ), JLabel.LEFT), 0,2,1,1,GridBagHelper.FILL_HORIZONTAL);
        gh.add(executable                                 , 1,2,1,1,GridBagHelper.GROWFILL_HORIZONTAL);

        // setup data from existing FileAssociation
        if (association!=null) {
          suffixes  .setText(association.getSuffixes()  );
          name      .setText(association.getName()      );
          executable.setFile(association.getExecutable());
        }
        
        // create actions for dialog
        final ActionDelegate[] actions = {
          new CloseWindow(CloseWindow.TXT_OK), 
          new CloseWindow(localize("delete")).setEnabled(association!=null), 
          new CloseWindow(CloseWindow.TXT_CANCEL)
        };
        
        // track changes
        ChangeListener l = new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
            boolean ok = !suffixes.isEmpty() && !name.isEmpty() && !executable.isEmpty();
            actions[0].setEnabled(ok);
          }
        };
        suffixes.addChangeListener(l);
        name.addChangeListener(l);
        executable.addChangeListener(l);
        l.stateChanged(null);
        
        // show a dialog with file association fields
        WindowManager mgr = widget.getWindowManager();
        int rc = mgr.openDialog(null, getName(), WindowManager.IMG_QUESTION, panel, actions, widget);

        // analyze option
        switch (rc) {
          // cancel?
          case 2:
            return;
          // ok?
          case 0:
            // create new?
            if (association==null)
              association = FileAssociation.add(new FileAssociation());
            // keep input
            association.setSuffixes(suffixes.getText());
            association.setName(name.getText());
            association.setExecutable(executable.getFile().toString());
            break;
          // delete?
          case 1:
            FileAssociation.del(association);
            break;
        }
        
        // update actions
        popup.setActions(getActions());
      }
    } // Action

  } //FileAssociationOption
  
} //Options
