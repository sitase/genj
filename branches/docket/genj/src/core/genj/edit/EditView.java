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

import genj.edit.actions.Redo;
import genj.edit.actions.Undo;
import genj.edit.beans.BeanFactory;
import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomListener;
import genj.gedcom.GedcomListenerAdapter;
import genj.gedcom.Property;
import genj.gedcom.PropertyXRef;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.ButtonHelper;
import genj.view.ContextProvider;
import genj.view.ToolBar;
import genj.view.View;
import genj.view.ViewContext;
import genj.window.WindowManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import spin.Spin;

/**
 * Component for editing genealogic entity properties
 */
public class EditView extends View implements ContextProvider  {
  
  /*package*/ final static Logger LOG = Logger.getLogger("genj.edit");
  
  /** the gedcom we're looking at */
  private Gedcom  gedcom;
  
  /** the registry we use */
  private Registry registry;
  
  /** stack */
  private Stack<Context> backs = new Stack<Context>(), forwards = new Stack<Context>();
  
  /** bean factory */
  private BeanFactory beanFactory;

  /** the resources we use */
  static final Resources resources = Resources.get(EditView.class);

  /** actions we offer */
  private Sticky   sticky = new Sticky();
  private Back     back = new Back();
  private Forward  forward = new Forward();
  private Mode     mode;
  private Callback callback = new Callback();
  private Undo undo;
  private Redo redo;
  
  /** whether we're sticky */
  private  boolean isSticky = false;

  /** current editor */
  private Editor editor;
  
  /**
   * Constructor
   */
  public EditView(String setTitle, Context context, Registry setRegistry) {
    
    super(new BorderLayout());
    
    // remember
    gedcom   = context.getGedcom();
    registry = setRegistry;
    beanFactory = new BeanFactory(registry);

    // prepare action
    mode = new Mode();
    undo = new Undo(gedcom);
    redo = new Redo(gedcom);
    
    // run mode switch if applicable
    setAdvanced(registry.get("advanced", false));
    
    // add keybindings
    InputMap imap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
    ActionMap amap = getActionMap();
    imap.put(KeyStroke.getKeyStroke("alt LEFT"), back);
    amap.put(back, back);
    imap.put(KeyStroke.getKeyStroke("alt RIGHT"), forward);
    amap.put(forward, forward);

    // Check if we were sticky
    Entity entity = gedcom.getEntity(registry.get("entity", (String)null));
    if (registry.get("sticky", false) && entity!=null) {
      isSticky = true;
    } else {
      entity = context.getEntity();
    }
    if (entity!=null)
      setContext(new ViewContext(entity));

    // Done
  }
  
  

  
  /**
   * Set editor to use
   */
  private void setEditor(Editor set) {

    // preserve old context and reset current editor to force commit changes
    ViewContext old = null;
    if (editor!=null) {
      old = editor.getContext();
      editor.setContext(new ViewContext(gedcom));
    }
    
    // remove old editor 
    removeAll();
      
    // keep new
    editor = set;
    editor.init(gedcom, this, registry);

    // add to layout
    add(editor, BorderLayout.CENTER);

    // restore old context
    if (old!=null)
      editor.setContext(old);
      
    // show
    revalidate();
    repaint();
  }

  /**
   * @see javax.swing.JComponent#addNotify()
   */
  public void addNotify() {
    
    // let super do its thing first
    super.addNotify();    
    
    // listen to gedcom
    callback.enable();
    
  }

  /**
   * Notification when component is not used any more
   */
  public void removeNotify() {
    
    // remember context
    registry.put("sticky", isSticky);
    Entity entity = editor.getContext().getEntity();
    if (entity!=null)
      registry.put("entity", entity.getId());

    // remember mode
    registry.put("advanced", mode.advanced);

    // don't listen to gedcom
    callback.disable();
    gedcom.removeGedcomListener((GedcomListener)Spin.over(undo));
    gedcom.removeGedcomListener((GedcomListener)Spin.over(redo));
    
    // Continue
    super.removeNotify();

    // Done
  }
  
  /**
   * BeanFactory
   */
  /*package*/ BeanFactory getBeanFactory() {
    return beanFactory;
  }
  
  /**
   * Ask the user whether he wants to commit changes 
   */
  /*package*/ boolean isCommitChanges() {
    
    // we only consider committing IF we're still in a visible top level ancestor (window) - otherwise we assume 
    // that the containing window was closed and we're not going to throw a dialog out there or do a change
    // behind the covers - we really would need a about-to-close hook for contained components here :(
    if (!getTopLevelAncestor().isVisible())
      return false;
      
    // check for auto commit
    if (Options.getInstance().isAutoCommit)
      return true;
    
    JCheckBox auto = new JCheckBox(resources.getString("confirm.autocomit"));
    auto.setFocusable(false);
    
    int rc = WindowManager.getInstance().openDialog(null, 
        resources.getString("confirm.keep.changes"), WindowManager.QUESTION_MESSAGE, 
        new JComponent[] {
          new JLabel(resources.getString("confirm.keep.changes")),
          auto
        },
        Action2.yesNo(), 
        this
    );
    
    if (rc!=0)
      return false;
    
    Options.getInstance().isAutoCommit = auto.isSelected();
    
    return true;
    
  }
  
  /**
   * ContextProvider callback
   */
  public ViewContext getContext() {
    return editor.getContext();
  }
  
  @Override
  public void commit() {
    editor.commit();
  }
  
  public void select(Context context, boolean isActionPerformed) {
    
    // ignore if no entity info in it
    if (context.getEntity()==null)
      return;
    
    // set context unless sticky
    if (isSticky) 
      return;
    
    if (isActionPerformed) {
      
      if (context.getProperty() instanceof PropertyXRef) {
        
        PropertyXRef xref = (PropertyXRef)context.getProperty();
        xref = xref.getTarget();
        if (xref!=null)
          context = new ViewContext(xref);
      }
      
      
    }
    
    // follow
    setContext(context);
    
    
  }
  
  public void setAdvanced(boolean advanced) {
    setEditor(advanced ? (Editor)new AdvancedEditor() : new BasicEditor());
  }
  
  public void back() {
    
    if (backs.isEmpty())
      return;

    // push current on forward
    Context old = editor.getContext();
    if (old.getEntities().length>0) {
      forwards.push(editor.getContext());
      forward.setEnabled(true);
    }
    
    // return to last
    editor.setContext((Context)backs.pop());
    
    // reflect state
    back.setEnabled(backs.size()>0);

  }
  
  public void forward() {
    
    if (forwards.isEmpty())
      return;
    
    // push current on back
    Context old = editor.getContext();
    if (old.getEntities().length>0) {
      backs.push(editor.getContext());
      back.setEnabled(true);
    }
    
    // go forward
    ViewContext context = new ViewContext((Context)forwards.pop());
    
    editor.setContext(context);
    
    // reflect state
    forward.setEnabled(forwards.size()>0);
  }

  
  public void setContext(Context context) {
    
    // keep track of current editor's context
    ViewContext current = editor.getContext();
    if (current.getEntity()!=null && current.getEntity()!=context.getEntity()) {

      Context c = new Context(current.getEntity());
      for (Property p : current.getProperties()) 
        if (p.getEntity().equals(current.getEntity()))
          c.addProperty(p);
      
      backs.push(c);
      
      // trim stack - arbitrarily chosen size
      while (backs.size()>32)
        backs.remove(0);
      
      back.setEnabled(true);
      forwards.clear();
      forward.setEnabled(false);
    }

    // tell to editors
    editor.setContext(context);

    // update title
//    context = editor.getContext();
//    manager.setTitle(this, context!=null&&context.getEntity()!=null?context.getEntity().toString():"");
    
  }
  
  /**
   * @see genj.view.ToolBarSupport#populate(JToolBar)
   */
  public void populate(ToolBar toolbar) {

    // return in history
    toolbar.add(back);
    toolbar.add(forward);
    
    // toggle sticky
    ButtonHelper bh = new ButtonHelper();
    toolbar.add(bh.create(sticky, Images.imgStickOn, isSticky));
    
    // add undo/redo
    toolbar.add(undo);
    toolbar.add(redo);
    
    // add actions
    //bar.add(contextMenu);
    
    // add basic/advanced
    toolbar.addSeparator();
    toolbar.add(bh.create(mode, Images.imgAdvanced, mode.advanced));
    
    // done
  }
  
  /**
   * @see javax.swing.JComponent#getPreferredSize()
   */
  public Dimension getPreferredSize() {
    return new Dimension(256,480);
  }
  
  /**
   * whether we're sticky
   */
  public boolean isSticky() {
    return isSticky;
  }
  
  /**
   * Current entity
   */
  public Entity getEntity() {
    return editor.getContext().getEntity();
  }
    
//  /**
//   * ContextMenu
//   */
//  private class ContextMenu extends PopupWidget {
//    
//    /** constructor */
//    private ContextMenu() {
//      setIcon(Gedcom.getImage());
//      setToolTipText(resources.getString( "action.context.tip" ));
//    }
//    
//    /** override - popup creation */
//    protected JPopupMenu createPopup() {
//      // force editor to commit
//      editor.setContext(editor.getContext());
//      // create popup
//      return manager.getContextMenu(editor.getContext(), this);
//    }
//     
//  } //ContextMenu
  
  /**
   * Action - toggle
   */
  private class Sticky extends Action2 {
    /** constructor */
    protected Sticky() {
      super.setImage(Images.imgStickOff);
      super.setTip(resources, "action.stick.tip");
    }
    /** run */
    public void actionPerformed(ActionEvent event) {
      isSticky = !isSticky;
    }
  } //Sticky
  
  /**
   * Action - advanced or basic
   */
  private class Mode extends Action2 {
    private boolean advanced = false;
    private Mode() {
      setImage(Images.imgView);
      setEditor(new BasicEditor());
      setTip(resources, "action.mode");
    }
    public void actionPerformed(ActionEvent event) {
      advanced = !advanced;
      setAdvanced(advanced);
    }
  } //Advanced

  /**
   * Forward to a previous context
   */  
  private class Forward extends Action2 {
    
    /**
     * Constructor
     */
    public Forward() {
      
      // patch looks
      setImage(Images.imgForward);
      setTip(Resources.get(this).getString("action.forward.tip"));
      
    }
    
    /**
     * go forward
     */
    public void actionPerformed(ActionEvent event) {
      
      if (!forwards.isEmpty())
        forward();
    }
    
  } //Forward
  
  /**
   * Return to a previous context
   */  
  private class Back extends Action2 {
    
    /**
     * Constructor
     */
    public Back() {
      
      // setup looks
      setImage(Images.imgBack);
      setTip(Resources.get(this).getString("action.return.tip"));
      setEnabled(false);
      
    }

    /**
     * go back
     */
    public void actionPerformed(ActionEvent event) {
      
      if (!backs.isEmpty()) 
        back();
    }
    
  } //Back

  /**
   * Gedcom callback
   */  
  private class Callback extends GedcomListenerAdapter {
    
    void enable() {
      gedcom.addGedcomListener((GedcomListener)Spin.over(this));
      backs.clear();
      forwards.clear();
      back.setEnabled(false);
      forward.setEnabled(false);
    }
    
    void disable() {
      gedcom.removeGedcomListener((GedcomListener)Spin.over(this));
      backs.clear();
      forwards.clear();
      back.setEnabled(false);
      forward.setEnabled(false);
    }
    
    public void gedcomEntityDeleted(Gedcom gedcom, Entity entity) {
      remove(entity, backs);
      remove(entity, forwards);
    }

    public void gedcomPropertyDeleted(Gedcom gedcom, Property property, int pos, Property removed) {
      remove(removed, backs);
      remove(removed, forwards);
    }

    public void gedcomWriteLockReleased(Gedcom gedcom) {
      // check action possible
      back.setEnabled(!backs.isEmpty());
      forward.setEnabled(!forwards.isEmpty());
      // check if we should go back to one
      if (editor.getContext().getEntities().length==0 && !backs.isEmpty())
        back();
    }
    
    void remove(Entity entity,  Stack<Context> stack) {
      // parse stack
      for (Iterator<Context> it = stack.listIterator(); it.hasNext(); ) {
        Context ctx = it.next();
        if (ctx.getEntity().equals(entity))
          it.remove();
      }
    }
    
    void remove(Property prop, Stack<Context> stack) {
      List<Property> list = Collections.singletonList(prop);
      // parse stack
      for (Iterator<Context> it = stack.listIterator(); it.hasNext(); ) {
        Context ctx = it.next();
        ctx.removeProperties(list);
      }
      
    }

  } //Back
  
} //EditView
