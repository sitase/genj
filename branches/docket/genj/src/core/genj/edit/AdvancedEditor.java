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
package genj.edit;

import genj.common.SelectEntityWidget;
import genj.edit.beans.PropertyBean;
import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;
import genj.gedcom.MetaProperty;
import genj.gedcom.Property;
import genj.gedcom.PropertyEvent;
import genj.gedcom.TagPath;
import genj.gedcom.UnitOfWork;
import genj.io.PropertyReader;
import genj.io.PropertyTransferable;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ButtonHelper;
import genj.util.swing.NestedBlockLayout;
import genj.util.swing.TextAreaWidget;
import genj.view.ActionProvider;
import genj.view.SelectionSink;
import genj.view.ViewContext;
import genj.window.WindowManager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * Our advanced version of the editor allowing low-level
 * access at the Gedcom record-structure
 */
/*package*/ class AdvancedEditor extends Editor {
  
  private final static String
    ACC_CUT = "ctrl X",
    ACC_COPY = "ctrl C",
    ACC_PASTE = "ctrl V";

  private final static Clipboard clipboard = initClipboard();
  
  private boolean ignoreSelection = false;

  /**
   * Initialize clipboard - trying system falling back to private
   */
  private static Clipboard initClipboard() {
    try {
      return Toolkit.getDefaultToolkit().getSystemClipboard();
    } catch (Throwable t) {
      return new Clipboard("GenJ");
    }

  }
  
  /** resources */
  private static Resources resources = Resources.get(AdvancedEditor.class);

  /** gedcom */
  private Gedcom gedcom;

  /** tree for record structure */
  private PropertyTreeWidget tree = null;

  /** everything for the bean */
  private JPanel            editPane;
  private PropertyBean      bean = null;

  /** splitpane for tree/bean */
  private JSplitPane        splitPane = null;

  /** view */
  private EditView editView;

  /** actions */
  private Action2    
    ok   = new OK(), 
    cancel = new Cancel();

  /** registry */
  private Registry registry;
  
  /** interaction callback */
  private InteractionListener callback;

  /**
   * Initialize
   */
  public AdvancedEditor(Gedcom gedcom, EditView view, Registry regty) {
    
    // remember
    this.gedcom = gedcom;
    editView = view;
    registry = regty;
    
    // TREE Component's 
    tree = new Tree();
    callback = new InteractionListener();
    tree.addTreeSelectionListener(callback);
    
    JScrollPane treePane = new JScrollPane(tree);
    treePane.setMinimumSize  (new Dimension(160, 128));
    treePane.setPreferredSize(new Dimension(160, 128));
    treePane.getHorizontalScrollBar().setFocusable(false); // dont allow focus on scroll bars
    treePane.getVerticalScrollBar().setFocusable(false);
        
    // EDIT Component
    editPane = new JPanel(new BorderLayout());
    JScrollPane editScroll = new JScrollPane(editPane);
    // .. don't want scrollbars to get focus
    editScroll.getVerticalScrollBar().setFocusable(false);
    editScroll.getHorizontalScrollBar().setFocusable(false);

    // SplitPane with tree/edit
    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treePane, editScroll);
    splitPane.setDividerLocation(registry.get("divider",-1));
    splitPane.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(evt.getPropertyName()))
          registry.put("divider",splitPane.getDividerLocation());
      }
    });

    // layout
    setLayout(new BorderLayout());
    add(splitPane, BorderLayout.CENTER);
    
    // setup focus policy
    setFocusTraversalPolicy(new FocusPolicy());
    setFocusCycleRoot(true);
    
    // shortcuts
    new Cut().install(this, ACC_CUT, JComponent.WHEN_FOCUSED);
    new Copy().install(this, ACC_COPY, JComponent.WHEN_FOCUSED);
    new Paste().install(this, ACC_PASTE, JComponent.WHEN_FOCUSED);
    // done    
  }
  
  /**
   * Provider current context 
   */
  public ViewContext getContext() {
    return tree.getContext();
  }

  /**
   * Accessor - current context 
   * @param context context to switch to
   */
  public void setContext(Context context) {
    
    // clear?
    if (context==null) {
      tree.setRoot(null);
      return;
    }
    
    // ignore?
    if (ignoreSelection||context.getEntities().isEmpty())
      return;

    // clear current selection
    tree.clearSelection();

    // change root if necessary
    Entity entity = context.getEntity();
    if (entity!=tree.getRoot())
      tree.setRoot(entity);

    // set selection
    List<? extends Property> props = context.getProperties();
    if (props.isEmpty()&&entity.getNoOfProperties()>0) 
      props = Collections.singletonList(entity.getProperty(0)); 
    tree.setSelection(props);
    
    // 20060301 set focus since selection change won't do that anymore
    if (bean!=null)
      bean.requestFocusInWindow();
    
  
    // Done
  }
  
  @Override
  public void commit() {
    if (ok.isEnabled()) {
      Property root = tree.getRoot();
      if (root==null)
        return;
      Gedcom gedcom = root.getGedcom();
  
      if (bean!=null) 
        gedcom.doMuteUnitOfWork(new UnitOfWork() {
          public void perform(Gedcom gedcom) {
            bean.commit();
          }
        });

      ok.setEnabled(false);
      cancel.setEnabled(false);
    }
  }
  
  /**
   * Action - propagate properties
   */
  private class Propagate extends Action2 {
    /** selection to propagate */
    private Entity entity;
    private List<Property> properties;
    private String what;
    
    /** constructor */
    private Propagate(List<Property> selection) {
      // remember
      this.entity = (Entity)tree.getRoot();
      properties = Property.normalize(selection);
      // something there?
      if (properties.isEmpty()) {
        setText(resources.getString("action.propagate", ""));
        setEnabled(false);
        return;
      }
      // setup looks
      this.what = "'"+Property.getPropertyNames(properties,5)+"' ("+properties.size()+")";
      setText(resources.getString("action.propagate", what)+" ...");
    }
    /** apply it */
    public void actionPerformed(ActionEvent event) {
      
      // prepare options
      final TextAreaWidget text = new TextAreaWidget("", 4, 10, false, true);
      final SelectEntityWidget select = new SelectEntityWidget(gedcom, entity.getTag(), resources.getString("action.propagate.toall"));
      select.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Entity target = select.getSelection();
          String string = target==null ? resources.getString("action.propagate.all", new Object[] { what, ""+select.getEntityCount(), Gedcom.getName(entity.getTag()) } )
              : resources.getString("action.propagate.one", new Object[]{ what, target.getId(), Gedcom.getName(target.getTag()) });
          text.setText(string);
        }
      });
      
      final JCheckBox check = new JCheckBox(resources.getString("action.propagate.value"));
      
      JPanel panel = new JPanel(new NestedBlockLayout("<col><select wx=\"1\"/><note wx=\"1\" wy=\"1\"/><check wx=\"1\"/></col>"));
      panel.add(select);
      panel.add(new JScrollPane(text));
      panel.add(check);
      
      // preselect something?
      select.setSelection(gedcom.getEntity(registry.get("select."+entity.getTag(), (String)null)));

      // show it
      boolean cancel = 0!=WindowManager.getInstance().openDialog("propagate", getText(), WindowManager.WARNING_MESSAGE, panel, Action2.okCancel(), AdvancedEditor.this);
      if (cancel)
        return;

      final Entity selection = select.getSelection();
      
      // remember selection
      registry.put("select."+entity.getTag(), selection!=null ? selection.getId() : null);
      
      // change it
      try {
        gedcom.doUnitOfWork(new UnitOfWork() {        
          public void perform(Gedcom gedcom) throws GedcomException {
            for (Entity to : selection!=null ? Collections.singletonList(selection) : gedcom.getEntities(entity.getTag())) 
              Propagate.this.copy(properties, entity, to, check.isSelected());
          }
        });
      } catch (GedcomException e) {
        WindowManager.getInstance().openDialog(null,null,WindowManager.ERROR_MESSAGE,e.getMessage(),Action2.okOnly(), AdvancedEditor.this);
      }

      // done
    }
    
    private void copy(List<Property> selection, Entity from, Entity to, boolean values)  throws GedcomException {
      // make sure we're not propagating to self
      if (from==to)
        return;
      // loop over selection
      for (Property property : selection) {
        TagPath path = property.getParent().getPath();
        Property root = to.getProperty(path);
        if (root==null)
          root = to.setValue(path, "");
        root.copyProperties(property, values);
      }
      // done
    }
  } //Propagate
  
  /**
   * Action - cut
   */
  private class Cut extends Action2 {

    /** selection */
    protected List<Property> presetSelection; 
    
    /** constructor */
    private Cut(List<Property> preset) {
      presetSelection = Property.normalize(preset);
      super.setImage(Images.imgCut);
      super.setText(resources.getString("action.cut"));
    }
    
    private Cut() {
      
    }
    
    /** run */
    public void actionPerformed(ActionEvent event) {
      
      // available
      final List<Property> selection = presetSelection!=null ? presetSelection : Property.normalize(tree.getSelection());
      if (selection.isEmpty())
        return;
      
      // contains entity?
      if (selection.contains(tree.getRoot())) {
        selection.clear();
        selection.addAll(Arrays.asList(tree.getRoot().getProperties()));
      }
      
      // warn about cut
      String veto = getVeto(selection);
      if (veto.length()>0) {
        int rc = WindowManager.getInstance().openDialog("cut.warning", resources.getString("action.cut"), WindowManager.WARNING_MESSAGE, veto, new Action[]{ new Action2(resources.getString("action.cut")), Action2.cancel() }, AdvancedEditor.this );
        if (rc!=0)
          return;
      }
      
      // copy first
      try {
        clipboard.setContents(new PropertyTransferable(selection).getStringTransferable(), null);
      } catch (Throwable t) {
        EditView.LOG.log(Level.WARNING, "Couldn't copy properties", t);
        return;
      }
      
      // now cut
      gedcom.doMuteUnitOfWork(new UnitOfWork() {
        public void perform(Gedcom gedcom) {
          for (Property p : selection)  
            p.getParent().delProperty(p);
        }
      });
      // done
    }
    
    /** assemble a list of vetos for cutting properties */
    private String getVeto(List<Property> properties) {
      
      StringBuffer result = new StringBuffer();
      for (Property p : properties) {
        
        String veto = p.getDeleteVeto();
        if (veto!=null) {
          // Removing property {0} from {1} leads to:\n{2}
          result.append(resources.getString("del.warning", new String[] { p.getPropertyName(), p.getParent().getPropertyName(), veto  }));
          result.append("\n");
        }
      }

      return result.toString();
    }
      
  } //Cut

  /**
   * Action - copy
   */
  private class Copy extends Action2 {
  	
    /** selection */
    protected List<Property> presetSelection; 
    
    /** constructor */
    protected Copy(List<Property> preset) {
      presetSelection = Property.normalize(preset);
      setText(resources.getString("action.copy"));
      setImage(Images.imgCopy);
    }
    /** constructor */
    protected Copy() {
    }
    /** run */
    public void actionPerformed(ActionEvent event) {
      
      // check selection
      List<Property> selection = presetSelection;
      if (selection==null) 
        selection = Property.normalize(tree.getSelection());
      
      // contains entity?
      if (selection.contains(tree.getRoot()))
        selection = Arrays.asList(tree.getRoot().getProperties());
      
      try {
        clipboard.setContents(new PropertyTransferable(selection).getStringTransferable(), null);
      } catch (Throwable t) {
        EditView.LOG.log(Level.WARNING, "Couldn't copy properties", t);
      }
    }

  } //ActionCopy
    
  /**
   * Action - paste
   */
  private class Paste extends Action2 {
  	
    /** selection */
    private Property presetParent; 
    
    /** constructor */
    protected Paste(Property property) {
      presetParent = property;
      setText(resources.getString("action.paste"));
      setImage(Images.imgPaste);
      
      // 20060404 isPasteAvail() apparently is VERY costly - depending on what's in the system clipboard
      // so let's not do this anymore and check on execute() instead
      // setEnabled(isPasteAvail());
    }
    /** constructor */
    protected Paste() {
    }
    /** run */
    public void actionPerformed(ActionEvent event) {

      // grab the clipboard content now
      final String content;
      try {
        content = clipboard.getContents(this).getTransferData(DataFlavor.stringFlavor).toString();
      } catch (Throwable t) {
        EditView.LOG.log(Level.INFO, "Accessing system clipboard as stringFlavor failed ("+t.getMessage()+")");
        return;
      }
      
      // select
      final Property parent;
      if (presetParent!=null) 
        parent = presetParent;
      else if (tree.getSelectionCount()==1)
        parent = (Property)tree.getSelection().get(0);
      else 
        return;
      
      // grab from clipboard
      gedcom.doMuteUnitOfWork(new UnitOfWork() {
        public void perform(Gedcom gedcom) throws GedcomException {
          PropertyReader reader = new PropertyReader(new StringReader(content), null, true);
          reader.setMerge(true);
          try {
            reader.read(parent);
          } catch (IOException e) {
            throw new GedcomException("IO during read()");
          }
        }
      });

      // done
    }
  
  } //Paste
  
  /**
   * Action - add
   */
  private class Add extends Action2 {
    /** parent */
    private Property parent; 
    private String[] tags;
    private boolean addDefaults = true;
    /** constructor */
    protected Add(Property parent, MetaProperty meta) {
      this.parent = parent;
      String txt = meta.getName();
      if (!txt.equals(meta.getTag()))
        txt += " ("+meta.getTag()+")";
      setText(txt);
      setImage(meta.getImage());
      tags = new String[]{meta.getTag()};
    }
    /** constructor */
    protected Add(Property parent) {
      this.parent = parent;
      setText(resources.getString("action.add")+" ...");
      setImage(Images.imgNew);
    }
    /** run */
    public void actionPerformed(ActionEvent event) {
      
      // need to let user select tags to add?
      if (tags==null) {
        JLabel label = new JLabel(resources.getString("add.choose"));
        ChoosePropertyBean choose = new ChoosePropertyBean(parent, resources);
        JCheckBox check = new JCheckBox(resources.getString("add.default_too"),addDefaults);
        int option = WindowManager.getInstance().openDialog("add",resources.getString("add.title"),WindowManager.QUESTION_MESSAGE,new JComponent[]{ label, choose, check },Action2.okCancel(), AdvancedEditor.this); 
        if (option!=0)
          return;
        // .. calculate chosen tags
        tags = choose.getSelectedTags();
        addDefaults = check.isSelected();
        if (tags.length==0)  {
          WindowManager.getInstance().openDialog(null,null,WindowManager.ERROR_MESSAGE,resources.getString("add.must_enter"),Action2.okOnly(), AdvancedEditor.this);
          return;
        }
      }
      
      // .. stop current 
      tree.clearSelection();
  
      // .. add properties
      final List<Property> newProps = new ArrayList<Property>();
      gedcom.doMuteUnitOfWork(new UnitOfWork() {
        public void perform(Gedcom gedcom) {
          for (int i=0;i<tags.length;i++) {
            Property prop = parent.addProperty(tags[i], "");
            newProps.add(prop);
            if (addDefaults) prop.addDefaultProperties();
          } 
        };
      });
    
      // .. select added
      Property newProp = newProps.isEmpty() ? null : (Property)newProps.get(0);
      if (newProp instanceof PropertyEvent) {
        Property pdate = ((PropertyEvent)newProp).getDate(false);
        if (pdate!=null) newProp = pdate;
      }
      tree.setSelectionPath(new TreePath(tree.getPathFor(newProp)));
      
      // bean we can give focus to (in case of single selection)?
      if (bean!=null)
        bean.requestFocusInWindow();
      
      // done
    }

  } //Add
    
  /**
   * A ok action
   */
  private class OK extends Action2 {
  
    /** constructor */
    private OK() {
      setText(Action2.TXT_OK);
      setEnabled(false);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
      commit();
    }
  
  } //OK
  
  /**
   * A cancel action
   */
  private class Cancel extends Action2 {
  
    /** constructor */
    private Cancel() {
      setText(Action2.TXT_CANCEL);
      setEnabled(false);
    }
  
    /** cancel current proxy */
    public void actionPerformed(ActionEvent event) {
      // disable ok&cancel
      ok.setEnabled(false);
      cancel.setEnabled(false);
      // simulate a selection change
      List<Property> selection = tree.getSelection();
      tree.clearSelection();
      tree.setSelection(selection);
    }
  
  } //Cancel
  
  /**
   * Handling selection of properties
   */
  private class InteractionListener implements TreeSelectionListener, ChangeListener {
    
    /**
     * callback - selection in tree has changed
     */
    public void valueChanged(TreeSelectionEvent e) {

      // current root
      Property root = tree.getRoot();
      if (root!=null) {
        Gedcom gedcom = root.getGedcom();
        // ask user for commit if
        if (ok.isEnabled()&&!gedcom.isWriteLocked()&&editView.isCommitChanges()) 
          commit();
      }

      // Clean up
      if (bean!=null) {
        bean.removeChangeListener(this);
        editView.getBeanFactory().recycle(bean);
      }
      bean = null;
      editPane.removeAll();
      editPane.revalidate();
      editPane.repaint();
      
      // can show bean if single selection
      List<Property> selection = tree.getSelection();
      if (selection.size()==1) {
        Property prop = selection.get(0);
        try {
  
          // get a bean for property
          bean = editView.getBeanFactory().get(prop);
          
          // add bean to center of editPane 
          editPane.add(bean, BorderLayout.CENTER);
  
          // and a label to the top
          final JLabel label = new JLabel(Gedcom.getName(prop.getTag()), prop.getImage(false), SwingConstants.LEFT);
          editPane.add(label, BorderLayout.NORTH);
  
          // and actions to the bottom
          if (bean.isEditable()) {
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            ButtonHelper bh = new ButtonHelper().setInsets(0).setContainer(buttons);
            bh.create(ok).setFocusable(false);
            bh.create(cancel).setFocusable(false);
            editPane.add(buttons, BorderLayout.SOUTH);
          }
          
          // listen to it
          bean.addChangeListener(this);
  
        } catch (Throwable t) {
          EditView.LOG.log(Level.WARNING,  "Property bean "+bean, t);
        }
        
        // start without ok and cancel
        ok.setEnabled(false);
        cancel.setEnabled(false);

      }
      
      // tell to others
      if (!selection.isEmpty()) try {
        ignoreSelection = true;
        ViewContext context = new ViewContext(gedcom, new ArrayList<Entity>(), selection);

        SelectionSink.Dispatcher.fireSelection(AdvancedEditor.this, context, false);
      } finally {
        ignoreSelection = false;
      }
  
      // Done
    }

    /**
     * callback for state change - enable buttons
     */
    public void stateChanged(ChangeEvent e) {
      ok.setEnabled(true);
      cancel.setEnabled(true);
    }
  
  } //InteractionListener

  /**
   * Intercept focus policy requests to automate tree node traversal on TAB
   */
  private class FocusPolicy extends LayoutFocusTraversalPolicy {
    public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
      // let super find out who's getting focus - this might be null!
      Component result = super.getComponentAfter(focusCycleRoot, aComponent);
      if (result==null)
        return null;
      // choose next row in tree IF
      //  - a bean is still displayed at the moment
      //  - next component is not part of that bean
      if (bean!=null&&!SwingUtilities.isDescendingFrom(result, bean)) {
        tree.setSelectionRow( (tree.getSelectionRows()[0]+1) % tree.getRowCount());
      }
      // done for me
      return result;
    }
    public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
      // let super find out who's getting focus - this might be null!
      Component result = super.getComponentBefore(focusCycleRoot, aComponent);
      if (result==null)
        return null;
      // choose previous row in tree IF
      //  - a bean is still displayed at the moment
      //  - prev component is not part of that bean
      if (bean!=null&&!SwingUtilities.isDescendingFrom(result, bean)) 
        tree.setSelectionRow( (tree.getSelectionRows()[0]-1) % tree.getRowCount());
      // done for me
      return result;
    }
  } //FocusPolicy
  
  /**
   * our patched up PropertyTreeWidget
   */
  private class Tree extends PropertyTreeWidget {
    
    /** constructor */
    private Tree() {
      super(gedcom);
    }

    /** provide context */
    public ViewContext getContext() {
      
      // check selection
      ViewContext result = super.getContext();
      List<? extends Property> props = result.getProperties();
      List<Property> selection = tree.getSelection();

      // cut copy paste
      if (!props.isEmpty()) {
        result.addAction(new Cut(selection));
        result.addAction(new Copy(selection));
      }
      if (selection.size()==1) {
        result.addAction(new Paste((Property)selection.get(0)));
        
        // add
        result.addAction(new ActionProvider.SeparatorAction());
        Property prop = (Property)selection.get(0);
        if (!prop.isTransient()) {
          result.addAction(new Add(prop));
          Action2.Group group = new Action2.Group(resources.getString("action.add"));
          MetaProperty[] metas = prop.getNestedMetaProperties(MetaProperty.WHERE_NOT_HIDDEN | MetaProperty.WHERE_CARDINALITY_ALLOWS);
          Arrays.sort(metas);
          for (int i=0;i<metas.length;i++)
            if (metas[i].isInstantiated())
              group.add(new Add(prop, metas[i]));
          result.addActions(group);
        }
      }
      
      if (!selection.isEmpty()&&!selection.contains(tree.getRoot()))
          result.addAction(new Propagate(selection));

      // done
      return result;
    }

  } //Tree
  
} //AdvancedEditor