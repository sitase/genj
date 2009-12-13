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
package genj.table;

import genj.common.AbstractPropertyTableModel;
import genj.common.PropertyTableModel;
import genj.common.PropertyTableWidget;
import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.TagPath;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.view.ToolBar;
import genj.view.View;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.table.TableModel;

/**
 * Component for showing entities of a gedcom file in a tabular way
 */
public class TableView extends View {
  
  private final static Logger LOG = Logger.getLogger("genj.table");

  /** a static set of resources */
  private Resources resources = Resources.get(this);
  
  /** the registry we keep */
  private Registry registry;
  
  /** the table we're using */
  /*package*/ PropertyTableWidget propertyTable;
  
  /** the modes we're offering */
  private Map<String, Mode> modes = new HashMap<String, Mode>();
    {
      modes.put(Gedcom.INDI, new Mode(Gedcom.INDI, new String[]{"INDI","INDI:NAME","INDI:SEX","INDI:BIRT:DATE","INDI:BIRT:PLAC","INDI:FAMS", "INDI:FAMC", "INDI:OBJE:FILE"}));
      modes.put(Gedcom.FAM , new Mode(Gedcom.FAM , new String[]{"FAM" ,"FAM:MARR:DATE","FAM:MARR:PLAC", "FAM:HUSB", "FAM:WIFE", "FAM:CHIL" }));
      modes.put(Gedcom.OBJE, new Mode(Gedcom.OBJE, new String[]{"OBJE","OBJE:TITL"}));
      modes.put(Gedcom.NOTE, new Mode(Gedcom.NOTE, new String[]{"NOTE","NOTE:NOTE"}));
      modes.put(Gedcom.SOUR, new Mode(Gedcom.SOUR, new String[]{"SOUR","SOUR:TITL", "SOUR:TEXT"}));
      modes.put(Gedcom.SUBM, new Mode(Gedcom.SUBM, new String[]{"SUBM","SUBM:NAME" }));
      modes.put(Gedcom.REPO, new Mode(Gedcom.REPO, new String[]{"REPO","REPO:NAME", "REPO:NOTE"}));
    };
  
  /** current type we're showing */
  private Mode currentMode;
  
  /**
   * Constructor
   */
  public TableView(Registry registry) {
    
    // keep some stuff
    this.registry = registry;
    
    // get modes
    for (Mode mode : modes.values())
      mode.load(registry);

    // create our table
    propertyTable = new PropertyTableWidget(null);
    propertyTable.setAutoResize(false);

    // lay it out
    setLayout(new BorderLayout());
    add(propertyTable, BorderLayout.CENTER);
    
    // get current mode
    currentMode = getMode(Gedcom.INDI);
    String tag = registry.get("mode", "");
    if (modes.containsKey(tag))
      currentMode = getMode(tag);
    
    // shortcuts KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK)
    new NextMode(true).install(this, "ctrl pressed LEFT");
    new NextMode(false).install(this, "ctrl pressed RIGHT");
    
    // done
  }
  
  /*package*/ TableModel getModel() {
    return propertyTable.getTableModel();
  }
  
  /**
   * @see javax.swing.JComponent#getPreferredSize()
   */
  public Dimension getPreferredSize() {
    return new Dimension(480,320);
  }
  
  /**
   * Returns a mode for given tag
   */
  /*package*/ Mode getMode() {
    return currentMode;
  }
  
  /**
   * Returns a mode for given tag
   */
  /*package*/ Mode getMode(String tag) {
    // known mode?
    Mode mode = (Mode)modes.get(tag); 
    if (mode==null) {
      mode = new Mode(tag, new String[0]);
      modes.put(tag, mode);
    }
    return mode;
  }
  
  /**
   * Sets the type of entities to look at
   */
  /*package*/ void setMode(Mode set) {
    PropertyTableModel currentModel = propertyTable.getModel();
    
    // give mode a change to grab what it wants to preserve
    if (currentModel!=null&&currentMode!=null)
      currentMode.save(registry);
    
    // remember current mode
    currentMode = set;
    
    // tell to table
    if (currentModel!=null) {
      propertyTable.setModel(new Model(currentModel.getGedcom(),currentMode));
      propertyTable.setColumnLayout(currentMode.layout);
    }
  }
  
  @Override
  public void select(Context context, boolean isActionPerformed) {

    PropertyTableModel old = propertyTable.getModel();
    
    // clear?
    if (context==null) {
      if (old!=null)
        propertyTable.setModel(null);
      return;
    }
    
    // new gedcom?
    if (old==null||old.getGedcom()!=context.getGedcom()) {
      propertyTable.setModel(new Model(context.getGedcom(), currentMode));
      propertyTable.setColumnLayout(currentMode.layout);
    }

    // select
    propertyTable.select(context);
  }
  
  /**
   * @see genj.view.ToolBarSupport#populate(JToolBar)
   */
  public void populate(ToolBar toolbar) {
	  
    for (int i=0, j=1;i<Gedcom.ENTITIES.length;i++) {
      String tag = Gedcom.ENTITIES[i];
      toolbar.add(new SwitchMode(getMode(tag)));
    }

  }
  
  /**
   * Write table settings before going
   */
  @Override
  public void removeNotify() {
    // save modes
    for (Mode mode : modes.values())
      mode.save(registry);
    // continue
    super.removeNotify();
  }
  
  /**
   * Action - go to next mode
   */
  private class NextMode extends Action2 {
    private int dir;
    private NextMode(boolean left) {
      if (left) {
        dir = -1;
      } else {
        dir = 1;
      }
    }
    public void actionPerformed(ActionEvent event) {
      int next = -1;
      for (int i=0,j=Gedcom.ENTITIES.length; i<j; i++) {
        next = (i+j+dir)%Gedcom.ENTITIES.length;
        if (currentMode == getMode(Gedcom.ENTITIES[i])) 
          break;
      }
      setMode(getMode(Gedcom.ENTITIES[next]));
    }
  } //NextMode
  
  /**
   * Action - flip view to entity type
   */
  private class SwitchMode extends Action2 {
    /** the mode this action triggers */
    private Mode mode;
    /** constructor */
    SwitchMode(Mode mode) {
      this.mode = mode;
      setTip(resources.getString("mode.tip", Gedcom.getName(mode.getTag(),true)));
      setImage(Gedcom.getEntityImage(mode.getTag()));
    }
    /** run */
    public void actionPerformed(ActionEvent event) {
      setMode(mode);
      
      // save current type
      registry.put("mode", mode.getTag());
    }
  } //ActionMode
  
  /** 
   * A PropertyTableModelWrapper
   */
  private class Model extends AbstractPropertyTableModel {

    /** mode */
    private Mode mode;
    
    /** our cached rows */
    private List<Entity> rows;
    
    /** constructor */
    private Model(Gedcom gedcom, Mode set) {
      super(gedcom);
      mode = set;
    }
    
    /** # columns */
    public int getNumCols() {
      return mode.getPaths().length;
    }
    
    /** # rows */
    public int getNumRows() {
      // cache entities if not there yet
      if (rows==null) 
        rows = new ArrayList<Entity>(getGedcom().getEntities(mode.getTag()));
      // ready 
      return rows.size();
    }
    
    /** path for colum */
    public TagPath getPath(int col) {
      return mode.getPaths()[col];
    }

    /** property for row */
    public Property getProperty(int row) {
      
      // init rows
      getNumRows();

      // and look it up
      Property result = (Property)rows.get(row);
      if (result==null)
        return result;
      
      // since we do a lazy update after a gedcom write lock we check if cached properties are still good 
      if (result.getEntity()==null) {
        result = null;
        rows.set(row, null);
      }
      
      // done
      return result;
    }
    
    /** gedcom callback */
    public void gedcomEntityAdded(Gedcom gedcom, Entity entity) {
      // an entity we're not looking at?
      if (!mode.getTag().equals(entity.getTag())) 
        return;
      // add it
      rows.add(entity);
      // tell about it
      fireRowsAdded(rows.size()-1, rows.size()-1);
      // done
    }

    /** gedcom callback */
    public void gedcomEntityDeleted(Gedcom gedcom, Entity entity) {
      // an entity we're not looking at?
      if (!mode.getTag().equals(entity.getTag())) 
        return;
      // delete it
      for (int i=0;i<rows.size();i++) {
        if (rows.get(i)==entity) {
          rows.remove(i);
          // tell about it
          fireRowsDeleted(i, i);
          // done
          return;
        }
      }
      // hmm, strange
      LOG.warning("got notified that entity "+entity.getId()+" was deleted but it wasn't in rows in the first place");
    }

    /** gedcom callback */
    public void gedcomPropertyAdded(Gedcom gedcom, Property property, int pos, Property added) {
      invalidate(gedcom, property.getEntity(), property.getPath());
    }

    /** gedcom callback */
    public void gedcomPropertyChanged(Gedcom gedcom, Property property) {
      invalidate(gedcom, property.getEntity(), property.getPath());
    }

    /** gedcom callback */
    public void gedcomPropertyDeleted(Gedcom gedcom, Property property, int pos, Property deleted) {
      invalidate(gedcom, property.getEntity(), new TagPath(property.getPath(), deleted.getTag()));
    }
    
    private void invalidate(Gedcom gedcom, Entity entity, TagPath path) {
      // an entity we're not looking at?
      if (!mode.getTag().equals(entity.getTag())) 
        return;
      // a path we're interested in?
      TagPath[] paths = mode.getPaths();
      for (int i=0;i<paths.length;i++) {
        if (paths[i].equals(path)) {
          for (int j=0;j<rows.size();j++) {
            if (rows.get(j)==entity) {
                fireRowsChanged(j,j,i);
                return;
            }
          }      
        }
      }
      // done
    }

  } //Model

  /**
   * A mode is a configuration for a set of entities
   */
  /*package*/ class Mode {
    
    /** attributes */
    private String tag;
    private String[] defaults;
    private TagPath[] paths;
    private String layout;
    
    /** constructor */
    private Mode(String t, String[] d) {
      // remember
      tag      = t;
      defaults = d;
      paths    = TagPath.toArray(defaults);
    }
    
    /** load properties from registry */
    private void load(Registry r) {
      
      String[] ps = r.get(tag+".paths" , (String[])null);
      if (ps!=null) 
        paths = TagPath.toArray(ps);

      layout = r.get(tag+".layout", (String)null);
      
    }
    
    /** set paths */
    /*package*/ void setPaths(TagPath[] set) {
      paths = set;
      if (currentMode==this)
        setMode(currentMode);
    }
    
    /** get paths */
    /*package*/ TagPath[] getPaths() {
      return paths;
    }
    
    /** save properties from registry */
    private void save(Registry r) {
      
      // grab current column widths & sort column
      if (currentMode==this && propertyTable.getModel()!=null) 
        layout = propertyTable.getColumnLayout();

	    registry.put(tag+".paths" , paths);
	    registry.put(tag+".layout", layout);
    }
    
    /** tag */
    /*package*/ String getTag() {
      return tag;
    }
    
  } //Mode
  
} //TableView