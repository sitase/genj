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
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;
import genj.gedcom.MetaProperty;
import genj.gedcom.Property;
import genj.gedcom.TagPath;
import genj.util.ChangeSupport;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.NestedBlockLayout;
import genj.view.ContextProvider;
import genj.view.ViewContext;

import java.awt.Component;
import java.awt.Container;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

/**
 * A panel for laying out beans for an entity
 */
public class BeanPanel extends JPanel implements ContextProvider {
  
  private final static Resources RES = Resources.get(BeanPanel.class);
  private final static Registry REGISTRY = Registry.get(BeanPanel.class);
  
  /** keep a cache of descriptors */
  private static Map<String, NestedBlockLayout> DESCRIPTORCACHE = new HashMap<String, NestedBlockLayout>();

  private Property root;
  protected ChangeSupport changeSupport = new ChangeSupport(this);
  private List<PropertyBean> beans = new ArrayList<PropertyBean>(32);
  
  /**
   * Find a descriptor 
   */
  private static NestedBlockLayout getLayout(String key) {
    
    // already loaded?
    NestedBlockLayout result = DESCRIPTORCACHE.get(key);
    if (result!=null)
      return result.copy();
    if (DESCRIPTORCACHE.containsKey(key))
      return null;
    
    try {
      // read
      InputStream in = BeanPanel.class.getResourceAsStream(key);
      if (in!=null) try {
        result = new NestedBlockLayout(in);
      } finally {
        in.close();
      }
    } catch (Throwable t) {
      // 20060601 don't let IllegalArgumentException go through (cought IOExcption only previously)
      // A web-server might return an invalid 404 input stream instead of null that we end up reading
      EditView.LOG.log(Level.WARNING, "cannot read descriptor "+key+" ("+t.getMessage()+")");
    }
    
    // read only once ever
    DESCRIPTORCACHE.put(key, result);

    return result!=null ? result.copy() : null;
  }
  
  private static NestedBlockLayout getLayout(MetaProperty meta) {
    if (Entity.class.isAssignableFrom(meta.getType()))
      return getLayout("descriptors/entities/" + meta.getTag()+".xml");

    // try to read a descriptor by tag
    String key = "descriptors/properties/" + meta.getTag() +".xml";
    NestedBlockLayout result = getLayout(key);
    if (result!=null) 
      return result;
      
    // fallback to property type
    Class<?> type = meta.getType();
    while (type!=null) {
      result = getLayout("descriptors/properties/" + type.getSimpleName() +".xml");
      if (result!=null)
        return result;
      type = type.getSuperclass();
    }
    
    // not found
    return null;
  }
  
  public BeanPanel() {
    
    // make user focus root
    setFocusTraversalPolicy(new FocusPolicy());
    setFocusCycleRoot(true);
  }
  
  @Override
  public ViewContext getContext() {
    return root!=null ? new ViewContext(root) : null;
  }

  public void addChangeListener(ChangeListener listener) {
    changeSupport.addChangeListener(listener);
  }
  
  public void removeChangeListener(ChangeListener listener) {
    changeSupport.removeChangeListener(listener);
  }

  /**
   * Courtesy check whether the beans in the panel can all be committed
   */
  public boolean isCommittable() {
    for (PropertyBean bean : getBeans())
      if (!bean.isCommittable())
        return false;
    return true;
  }

  /**
   * commit beans - transaction has to be running already
   */
  public void commit() throws GedcomException {
    
    // commit beans' changes
    for (PropertyBean bean : beans) {
      if (bean.hasChanged()) 
        bean.commit();
    }
  
    changeSupport.setChanged(false);
    
    // done
  }
  
  /**
   * Beans in panel
   */
  public List<PropertyBean> getBeans() {
    return beans;
  }

  /**
   * Actions related to panel
   */
  public List<? extends Action> getActions() {
    ArrayList<Action> result = new ArrayList<Action>();
    for (PropertyBean bean : beans)
      result.addAll(bean.getActions());
    return result;
  }

  @Override
  public void requestFocus() {
    if (beans.isEmpty())
      return;
    beans.get(0).requestFocus();
  }

  @Override
  public boolean requestFocusInWindow() {
    if (beans.isEmpty())
      return false;
    beans.get(0).requestFocusInWindow();
    return true;
  }
  
  
  /** set context */
  public void setRoot(Property root) {
    
    // clean up first
    List<PropertyBean> bs = new ArrayList<PropertyBean>(beans);
    beans.clear(); // clear first to let focus policy not look for any before/after/default lookups
    for (PropertyBean bean : bs) {
      bean.removeChangeListener(changeSupport);
      bean.getParent().remove(bean);
      PropertyBean.recycle(bean);
    }
    removeAll();

    // keep
    this.root = root;
    
    // something to layout?
    if (root!=null) {
    
      // keep track of tags
      Set<String> beanifiedTags = new HashSet<String>();
      
      // layout from descriptor
      NestedBlockLayout descriptor = getLayout(root.getMetaProperty());
      if (descriptor!=null) 
        parse(root, root, descriptor, beanifiedTags);
    }
      
    // done
    revalidate();
    repaint();
  }

  /**
   * Parse descriptor for beans into panel
   */
  private void parse(Property root, Property property, NestedBlockLayout descriptor, Set<String> beanifiedTags)  {

    setLayout(descriptor);
    
    // fill cells with beans
    for (NestedBlockLayout.Cell cell : descriptor.getCells()) {
      JComponent comp = createComponent(root, property, cell, beanifiedTags);
      if (comp!=null)
        add(comp, cell);
    }
    
    // done
  }
  
  /**
   * Create a component for given cell
   */
  protected JComponent createComponent(Property root, Property property, NestedBlockLayout.Cell cell, Set<String> beanifiedTags) {
    
    String element = cell.getElement();
    
    // right gedcom version?
    String version = cell.getAttribute("gedcom");
    if (version!=null & !property.getGedcom().getGrammar().getVersion().equals(version))
      return null;
    
    // text?
    if ("text".equals(element)) 
      return new JLabel(cell.getAttribute("value"));
    
    // a folder handle?
    if ("fold".equals(element)) {
      String key = cell.getAttribute("key");
      if (key==null)
        throw new IllegalArgumentException("fold without key");
      String label = RES.getString(key,false);
      if (label==null) label = Gedcom.getName(key);
      
      String indent = cell.getAttribute("indent");
      NestedBlockLayout.Expander result = new NestedBlockLayout.Expander(label, indent!=null ? Integer.parseInt(indent) : 1);
      
      Registry r = new Registry(REGISTRY, root.getTag()+'.'+key);
      result.setCollapsed(r.get("folded", "1".equals(cell.getAttribute("collapsed"))));
      result.addPropertyChangeListener("folded", r);
      return result;
    }
    
    // a label?
    if ("label".equals(element)) {

      String key = cell.getAttribute("key");
      if (key!=null)
        return new JLabel(RES.getString(key));
      
      String path = cell.getAttribute("path");
      if (path==null)
        throw new IllegalArgumentException("label without key or path");

      MetaProperty meta = property.getMetaProperty().getNestedRecursively(new TagPath(path), false);
      if (Entity.class.isAssignableFrom(meta.getType())) 
        return new JLabel(meta.getName() + ' ' + ((Entity)root).getId(), null, SwingConstants.LEFT);
      else
        return new JLabel(meta.getName(cell.isAttribute("plural")), null, SwingConstants.LEFT);
    }
    
    // a bean?
    if ("bean".equals(element)) {
      
      TagPath path = new TagPath(cell.getAttribute("path"));
      MetaProperty meta = property.getMetaProperty().getNestedRecursively(path, false);
      
      // create bean
      PropertyBean bean = createBean(property, path, cell.getAttribute("type"));
      if (bean==null)
        return null;
      // patch it
      String dir = cell.getAttribute("dir");
      if (dir!=null&&dir.startsWith("h"))
        bean.setPreferHorizontal(true);
      // track it
      if (beanifiedTags!=null&&property==root&&path.length()>1)
        beanifiedTags.add(path.get(1));

      return bean;
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
  private PropertyBean createBean(Property root, TagPath path, String beanOverride) {

    // try to resolve existing prop (first possible path to avoid merging of branches)
    Property prop = root.getProperty(path, false);
    
    // create bean for property
    PropertyBean bean;
    if (beanOverride!=null)
      bean = PropertyBean.getBean(beanOverride);
    else if (prop!=null)
      bean = PropertyBean.getBean(prop.getClass());
    else 
      bean = PropertyBean.getBean(root.getMetaProperty().getNestedRecursively(path, false).getType(""));
      
    bean.setContext(root, path, prop, beans);
    bean.addChangeListener(changeSupport);
    beans.add(bean);

    // done
    return bean;
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
    @Override
    public Component getComponentAfter(Container container, Component component) {
      return beans.isEmpty() ? null : super.getComponentAfter(container, component);
    }
    @Override
    public Component getComponentBefore(Container container, Component component) {
      return beans.isEmpty() ? null : super.getComponentBefore(container, component);
    }
    @Override
    public Component getDefaultComponent(Container container) {
      return beans.isEmpty() ? null : super.getDefaultComponent(container);
    }
  } //FocusPolicy

 }