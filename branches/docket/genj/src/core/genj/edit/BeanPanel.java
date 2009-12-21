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

import genj.edit.beans.PropertyBean;
import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.MetaProperty;
import genj.gedcom.Property;
import genj.gedcom.PropertyComparator;
import genj.gedcom.PropertyXRef;
import genj.gedcom.TagPath;
import genj.gedcom.UnitOfWork;
import genj.util.ChangeSupport;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.util.swing.LinkWidget;
import genj.util.swing.NestedBlockLayout;
import genj.util.swing.PopupWidget;
import genj.view.ContextProvider;
import genj.view.SelectionSink;
import genj.view.ViewContext;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

/**
 * A panel for laying out beans for an entity
 */
public class BeanPanel extends JPanel {

  /** keep a cache of descriptors */
  private static Map<String, NestedBlockLayout> DESCRIPTORCACHE = new HashMap<String, NestedBlockLayout>();

  /** change support */
  protected ChangeSupport changeSupport = new ChangeSupport(this);
  
  /** top level tags */
  private Set<String> topLevelTags = new HashSet<String>();
  
  /** beans */
  private List<PropertyBean> beans = new ArrayList<PropertyBean>(32);
  
  /** content */
  private JPanel top = new JPanel();
  private JTabbedPane bottom = new ContextTabbedPane();
  
  /** whether to show tabs or not */
  private boolean isShowTabs = true;
    
  /**
   * Find a descriptor 
   */
  private static NestedBlockLayout getSharedDescriptor(String key) {
    
    // already determined we don't have one?
    NestedBlockLayout result = DESCRIPTORCACHE.get(key); 
    if (result!=null)
      return result;
    
    try {
      // read
      InputStream in = BeanPanel.class.getResourceAsStream(key);
      if (in!=null) try {
        result = new NestedBlockLayout(in);
      } finally {
        in.close();
      }
    } catch (IOException e) {
      EditView.LOG.log(Level.WARNING, "problem reading descriptor "+key+" ("+e.getMessage()+")");
    } catch (Throwable t) {
      // 20060601 don't let iae go through - a custom server 404 might return an invalid in
      EditView.LOG.log(Level.WARNING, "problem parsing descriptor "+key+" ("+t.getMessage()+")");
    }
    
    // read only once ever
    DESCRIPTORCACHE.put(key, result);

    return result;
  }
  
  private static NestedBlockLayout getSharedDescriptor(Entity entity, int sub) {
    return getSharedDescriptor("descriptors/entities/" + entity.getTag()+(sub==0?"":"."+sub)+".xml");
  }
  
  private static NestedBlockLayout getSharedDescriptor(MetaProperty meta) {
    
    // try to read a descriptor (looking up the inheritance chain)
    NestedBlockLayout result = null;
    for (MetaProperty cursor = meta; result==null && cursor!=null ; cursor = cursor.getSuper() ) 
      result = getSharedDescriptor("descriptors/properties/" + cursor.getTag() +".xml");
    
    // remember for next time
    DESCRIPTORCACHE.put("descriptors/properties/" + meta.getTag() +".xml", result);
    
    // done
    return result;
  }
  
  public BeanPanel(boolean showTabs) {
    
    isShowTabs = showTabs;
    
    // layout
    setLayout(new BorderLayout());
    add(top, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);
    
    // make user focus root
    setFocusTraversalPolicy(new FocusPolicy());
    setFocusCycleRoot(true);
  }
  
  public BeanPanel() {
    this(true);
  }
  
  /** set context */
  public void setEntity(Entity entity) {
    
    // clean up first
    for (PropertyBean bean : beans) {
      bean.removeChangeListener(changeSupport);
      PropertyBean.recycle(bean);
    }
    beans.clear();

    top.removeAll();
    bottom.removeAll();
    
    topLevelTags.clear();
    
    // something to layout?
    if (entity!=null) {
    
      // layout main descriptor
      NestedBlockLayout descriptor = getSharedDescriptor(entity, 0);
      if (descriptor!=null) {

        parse(top, entity, entity, descriptor.copy() );
      
        // add and layout sub-descriptors
        for (int i=1;;i++) {
          descriptor = getSharedDescriptor(entity, i);
          if (descriptor==null) break;
          JPanel tab = new JPanel();
          int watermark = beans.size();
          parse(tab, entity, entity, descriptor.copy());
          bottom.addTab("", beans.size()>watermark ? beans.get(watermark).getProperty().getImage(false) : entity.getImage(false), tab);
        }
    
        // add sub-descriptors for top-level properties in tabs
        createTabs(entity);
      }
    }
      
    // done
    revalidate();
    repaint();
  }
  
  public void addChangeListener(ChangeListener listener) {
    changeSupport.addChangeListener(listener);
  }
  
  public void removeChangeListener(ChangeListener listener) {
    changeSupport.removeChangeListener(listener);
  }
    
  /**
   * commit beans - transaction has to be running already
   */
  public void commit() {
    
    // loop over beans 
    for (PropertyBean bean : beans) {
      // check next
      if (bean.hasChanged()&&bean.getProperty()!=null) {
        Property prop = bean.getProperty();
        // proxied?:
        PropertyProxy proxy = (PropertyProxy)prop.getContaining(PropertyProxy.class);
        if (proxy!=null) 
          prop = proxy.getProxied().setValue(prop.getPathToContaining(proxy), "");
        // commit its changes
        bean.commit(prop);
        // next
      }
    }
    
    changeSupport.setChanged(false);
    
    // done
  }

  /**
   * Select a property's bean
   */
  public void select(Property prop) {
    
    // find bean
    JComponent bean = find(prop);
    if (bean==null) 
      return;

    // bring forward in a tabbed pane
    Component parent = bean;
    while (true) {
      if (parent.getParent() instanceof JTabbedPane) {
        ((JTabbedPane)parent.getParent()).setSelectedComponent(parent);
      }
      parent = parent.getParent();
      if (parent==null||parent==this)
        break;
    }        

    // request now
    if (!bean.requestFocusInWindow())
      Logger.getLogger("genj.edit").fine("requestFocusInWindow()==false");
    
    // done
  }
  
  private JComponent find(Property prop) {
    if (prop==null||beans.isEmpty())
      return null;
    
    // look for appropriate bean showing prop
    for (PropertyBean bean : beans) {
      if (bean.getProperty()==prop) 
        return bean;
    }
    
    // check if one of the beans' properties is contained in prop
    for (PropertyBean bean : beans) {
      if (bean.isDisplayable() && bean.getProperty()!=null && bean.getProperty().isContained(prop)) 
        return bean;
    }
    
    // check tabs specifically (there might be no properties yet)
    for (Component c : bottom.getComponents()) {
      JComponent jc = (JComponent)c;
      if (jc.getClientProperty(Property.class)==prop) 
        return jc;
    }
    
    // otherwise use first bean
    return (PropertyBean)beans.get(0);
    
    // done
  }

  /**
   * Parse descriptor
   */
  private void parse(JPanel panel, Entity entity, Property root, NestedBlockLayout descriptor)  {

    panel.setLayout(descriptor);
    
    // fill cells with beans
    for (NestedBlockLayout.Cell cell : descriptor.getCells()) {
      JComponent comp = createComponent(entity, root, cell);
      if (comp!=null) 
        panel.add(comp, cell);
    }
    
    // done
  }
  
  /**
   * Create a component for given cell
   */
  private JComponent createComponent(Entity entity, Property root, NestedBlockLayout.Cell cell) {
    
    String element = cell.getElement();
    
    // right gedcom version?
    String version = cell.getAttribute("gedcom");
    if (version!=null & !root.getGedcom().getGrammar().getVersion().equals(version))
      return null;
    
    // prepare some info and state
    TagPath path = new TagPath(cell.getAttribute("path"));
    MetaProperty meta = root.getMetaProperty().getNestedRecursively(path, false);
    
    // conditional?
    String iff = cell.getAttribute("if"); 
    if (iff!=null&&root.getProperty(new TagPath(iff))==null)
        return null;
    String ifnot = cell.getAttribute("ifnot"); 
    if (ifnot!=null&&root.getProperty(new TagPath(ifnot))!=null)
        return null;
    
    // a label?
    if ("label".equals(element)) {

      JLabel label;
      if (path.length()==1&&path.getLast().equals(entity.getTag()))
        label = new JLabel(meta.getName() + ' ' + entity.getId(), entity.getImage(false), SwingConstants.LEFT);
      else
        label = new JLabel(meta.getName(cell.isAttribute("plural")), meta.getImage(), SwingConstants.LEFT);

      return label;
    }
    
    // a bean?
    if ("bean".equals(element)) {
      // create bean
      PropertyBean bean = createBean(root, path, meta, cell.getAttribute("type"));
      if (bean==null)
        return null;
      // patch it
      if ("horizontal".equals(cell.getAttribute("dir")))
        bean.setPreferHorizontal(true);
      // track it
      if (root==entity&&path.length()>1)
        topLevelTags.add(path.get(1));
      // finally wrap in popup if requested?
      return cell.getAttribute("popup")==null ? bean : (JComponent)new PopupBean(bean);
    }

    // bug in the descriptor
    throw new IllegalArgumentException("Template element "+cell.getElement()+" is unkown");
  }
  
  /**
   * create a bean
   * @param root we need the bean for
   * @param path path to property we need bean for
   * @param explicit bean type
   */
  private PropertyBean createBean(Property root, TagPath path, MetaProperty meta, String beanOverride) {

    // try to resolve existing prop - this has to be a property along
    // the first possible path to avoid that in this case:
    //  INDI
    //   BIRT
    //    DATE sometime
    //   BIRT
    //    PLAC somewhere
    // the result of INDI:BIRT:DATE/INDI:BIRT:PLAC is
    //   somtime/somewhere
    // => !backtrack
    Property prop = root.getProperty(path, false);
    
    // addressed property doesn't exist yet? create a proxy that mirrors
    // the root and add create a temporary holder (enjoys the necessary
    // context - namely gedcom)
    if (prop==null) 
      prop = new PropertyProxy(root).setValue(path, "");

    // create bean for property
    PropertyBean bean = beanOverride==null ? PropertyBean.getBean(prop) : PropertyBean.getBean(beanOverride, prop);
    bean.addChangeListener(changeSupport);
    beans.add(bean);
    
    // done
    return bean;
  }
  
  /**
   * Create tabs from introspection
   */
  private void createTabs(Entity entity) {
    
    // create all tabs
    Set<String> skippedTags = new HashSet<String>();
    
    Property[] props = entity.getProperties();
    
    Arrays.sort(props, new PropertyComparator(".:DATE"));
    
    for (Property prop : props) {
      // check tag - skipped or covered already?
      String tag = prop.getTag();
      if (skippedTags.add(tag)&&topLevelTags.contains(tag)) 
        continue;
      topLevelTags.add(tag);
      // create a tab for it
      createTab(entity, prop);
      // next
    }
    
    // 'create' a tab for creating new properties
    JPanel newTab = new JPanel(new FlowLayout(FlowLayout.LEFT));
    newTab.setPreferredSize(new Dimension(64,64));
    bottom.addTab("", Images.imgNew, newTab);
    
    // add buttons for creating sub-properties 
    MetaProperty[] nested = entity.getNestedMetaProperties(MetaProperty.WHERE_NOT_HIDDEN);
    Arrays.sort(nested);
    for (int i=0;i<nested.length;i++) {
      MetaProperty meta = nested[i];
      // if there's a descriptor for it
      NestedBlockLayout descriptor = getSharedDescriptor(meta);
      if (descriptor==null||descriptor.getCells().isEmpty())
        continue;
      // .. and if there's no other already with isSingleton
      if (topLevelTags.contains(meta.getTag())&&meta.isSingleton())
        continue;
      // create a button for it
      newTab.add(new LinkWidget(new AddTab(entity, meta)));
    }
  
    // done
  }
  
  /**
   * Create a tab
   */
  private void createTab(Entity entity, Property prop) {
     
    // show simple xref bean for PropertyXRef
    if (prop instanceof PropertyXRef) {
      // don't create tabs for individuals and families
      try {
        String tt = ((PropertyXRef)prop).getTargetType();
        if (tt.equals(Gedcom.INDI)||tt.equals(Gedcom.FAM))
          return;
      } catch (IllegalArgumentException e) {
        // huh? non target type? (like in case of a foreign xref) ... ignore this prop
        return;
      }
      // add a tab for anything else
      bottom.addTab(prop.getPropertyName(), prop.getImage(false), PropertyBean.getBean(prop), prop.getPropertyInfo());
      return;
    }
     
    // got a descriptor for it?
    MetaProperty meta = prop.getMetaProperty();
    NestedBlockLayout descriptor = getSharedDescriptor(meta);
    if (descriptor==null) 
      return;
     
    // create the panel
    JPanel tab = new JPanel();
    tab.putClientProperty(Property.class, prop);

    parse(tab, entity, prop, descriptor.copy());
    bottom.addTab(meta.getName() + prop.format("{ $y}"), prop.getImage(false), tab, meta.getInfo());

    // done
  }
   
  private class ContextTabbedPane extends JTabbedPane implements ContextProvider {
    private ContextTabbedPane() {
      super(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
    }
    public ViewContext getContext() {
      // check if tab for property
      Component selection = bottom.getSelectedComponent();
      Property prop = (Property)((JComponent)selection).getClientProperty(Property.class);
      if (prop==null)
        return null;
      // provide a context with delete
      return new ViewContext(prop).addAction(new DelTab(prop));
    }
  } //ContextTabbedPane
    
   /** An action for adding 'new tabs' */
   private class AddTab extends Action2 {
     
     private MetaProperty meta;
     private Entity entity;
     private Property property;
     
     /** constructor */
     private AddTab(Entity entity, MetaProperty meta) {
       // remember
       this.meta = meta;
       this.entity = entity;
       // looks
       setText(meta.getName());
       setImage(meta.getImage());
       setTip(meta.getInfo());
     }
   
     /** callback initiate create */
     public void actionPerformed(ActionEvent event) {
       
       entity.getGedcom().doMuteUnitOfWork(new UnitOfWork() {
         public void perform(Gedcom gedcom) {
           
           // commit bean changes
           if (BeanPanel.this.changeSupport.hasChanged())
             commit();
           
           // add property for tab
           property = entity.addProperty(meta.getTag(), "");
         }
       });
       
       // send selection
       SwingUtilities.invokeLater(new Runnable() {
         // not deferring this won't make the focus switch happen :(
         public void run() {
           SelectionSink.Dispatcher.fireSelection(BeanPanel.this, new Context(property), false);
         }
       });
       
       // done
     }
     
   } //AddTab
   
   /**
    * A remove tab action
    */
   private class DelTab extends Action2 {
     private Property prop;
     private DelTab(Property prop) {
       setText(EditView.RESOURCES.getString("action.del", prop.getPropertyName()));
       setImage(Images.imgCut);
       this.prop = prop;
     }
    public void actionPerformed(ActionEvent event) {
      prop.getGedcom().doMuteUnitOfWork(new UnitOfWork() {
        public void perform(Gedcom gedcom) {
          
          // commit bean changes
          if (BeanPanel.this.changeSupport.hasChanged())
            commit();
          
          // delete property
          prop.getParent().delProperty(prop);
          
        }
      });
      

      // done
    }
  }

  /**
   * A 'bean' we use for groups
   */
  private class PopupBean extends PopupWidget implements MouseMotionListener {
    
    private PropertyBean wrapped;
    private JPanel content;
    
    /**
     * constructor
     */
    private PopupBean(PropertyBean wrapped) {
      
      // fix button's looks
      setFocusable(false);
      setBorder(null);
      
      // remember wrapped bean
      this.wrapped = wrapped;
      
      // setup button's look
      Property prop = wrapped.getProperty();
      setToolTipText(prop.getPropertyName());
      ImageIcon img = prop.getImage(false);
      if (prop.getValue().length()==0)
        img = img.getGrayedOut();
      setIcon(img);
      
      // prepare panel we're going to show
      content = new JPanel(new BorderLayout());
      content.setAlignmentX(0);
      content.setBorder(new TitledBorder(prop.getPropertyName()));
      content.addMouseMotionListener(this);
      content.add(wrapped);
      
      // prepare 'actions'
      addItem(content);
  
      // done
    }
    
    private ImageIcon getImage(Property prop) {
      while (prop.getParent()!=null && !(prop.getParent() instanceof Entity)) {
        prop = prop.getParent();
      }
      return prop.getImage(false);
    }
    
    /**
     * intercept popup
     */
    public void showPopup() {
      // let super do its thing
      super.showPopup();
      // resize if available
      Dimension d = BasicEditor.REGISTRY.get("popup."+wrapped.getProperty().getTag(), (Dimension)null);
      if (d!=null) {
        getPopup().getParent().setSize(d);
        getPopup().revalidate();
      }
      // request focus
      SwingUtilities.getWindowAncestor(wrapped).setFocusableWindowState(true);
      wrapped.requestFocus();
      // update image
      setIcon(wrapped.getProperty().getImage(false));
    }
    
    public void mouseDragged(MouseEvent e) {
      // allow to resize 
      if (wrapped.getCursor()!=Cursor.getDefaultCursor()) {
        Dimension d = new Dimension(e.getPoint().x, e.getPoint().y);
        BasicEditor.REGISTRY.put("popup."+wrapped.getProperty().getTag(), d);
        getPopup().getParent().setSize(d);
        getPopup().revalidate();
      }
    }
  
    public void mouseMoved(MouseEvent e) {
      // indicate resize cursor if applicable
      if (e.getX()>content.getWidth()-content.getInsets().right
        &&e.getY()>content.getHeight()-content.getInsets().bottom)
        content.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
      else
        content.setCursor(Cursor.getDefaultCursor());
    }
  
  } //Label

  /**
   * A proxy for a property - it can be used as a container
   * for temporary sub-properties that are not committed to
   * the proxied context 
   */
  private class PropertyProxy extends Property {
    private Property proxied;
    /** constructor */
    private PropertyProxy(Property prop) {
      this.proxied = prop;
    }
    public Property getProxied() {
      return proxied;
    }
    public boolean isContained(Property in) {
      return proxied==in ? true : proxied.isContained(in);
    }
    public Gedcom getGedcom() { return proxied.getGedcom(); }
    public String getValue() { throw new IllegalArgumentException(); };
    public void setValue(String val) { throw new IllegalArgumentException(); };
    public String getTag() { return proxied.getTag(); }
    public TagPath getPath() { return proxied.getPath(); }
    public MetaProperty getMetaProperty() { return proxied.getMetaProperty(); }
  }
     
  /**
   * The default container FocusTravelPolicy works based on
   * x/y coordinates which doesn't work well with the column
   * layout used.
   * ContainerOrderFocusTraversalPolicy would do fine accept()-check 
   * is placed in a protected method of LayoutFocusTraversalPolicy 
   * basically rendering the former layout useless.
   * I'm doing a hack to get the ContainerOrderFTP with
   * LayoutFTP's accept :(
   */
  private class FocusPolicy extends ContainerOrderFocusTraversalPolicy {
    private Hack hack = new Hack();
    protected boolean accept(Component c) {
      return hack.accept(c);
    }
    private class Hack extends LayoutFocusTraversalPolicy {
      protected boolean accept(Component c) {
        return super.accept(c);
      }
    }
  } //FocusPolicy
 }