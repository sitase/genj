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
package genj.entity;

import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomListener;
import genj.gedcom.GedcomListenerAdapter;
import genj.gedcom.Property;
import genj.renderer.Blueprint;
import genj.renderer.BlueprintManager;
import genj.renderer.EntityRenderer;
import genj.util.Registry;
import genj.util.Resources;
import genj.view.ContextProvider;
import genj.view.View;
import genj.view.ViewContext;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

import spin.Spin;

/**
 * A rendering component showing the currently selected entity
 * via html
 */
public class EntityView extends View implements ContextProvider {

  /** language resources we use */  
  /*package*/ final static Resources resources = Resources.get(EntityView.class);

  /** a dummy blueprint */
  private final static Blueprint BLUEPRINT_SELECT = new Blueprint(resources.getString("html.select"));
  
  /** a registry we keep */
  private Registry registry;
  
  /** the renderer we're using */      
  private EntityRenderer renderer = null;
  
  /** our current context */
  /*package*/ Context context = null;
  
  /** the blueprints we're using */
  private Map<String, Blueprint> type2blueprint = new HashMap<String, Blueprint>();
  
  /** whether we do antialiasing */
  private boolean isAntialiasing = false;
  
  private GedcomListener callback = new GedcomListenerAdapter() {
    public void gedcomEntityDeleted(Gedcom gedcom, Entity entity) {
      if (context.getEntity()==entity)
        setContext(new Context(context.getGedcom()), true);
    }
    public void gedcomPropertyChanged(Gedcom gedcom, Property property) {
      if (context.getEntity() == property.getEntity())
        repaint();
    }
    public void gedcomPropertyAdded(Gedcom gedcom, Property property, int pos, Property added) {
      gedcomPropertyChanged(gedcom, property);
    }
    public void gedcomPropertyDeleted(Gedcom gedcom, Property property, int pos, Property removed) {
      gedcomPropertyChanged(gedcom, property);
    }
  };
  
  /**
   * Constructor
   */
  public EntityView(Registry registry) {
    
    // save some stuff
    this.registry = registry;
    
    // grab data from registry
    BlueprintManager bpm = BlueprintManager.getInstance();
    for (int t=0;t<Gedcom.ENTITIES.length;t++) {
      String tag = Gedcom.ENTITIES[t];
      type2blueprint.put(tag, bpm.getBlueprint(tag, registry.get("blueprint."+tag, "")));
    }
    isAntialiasing  = registry.get("antial"  , false);
    
    // done    
  }
  
  /**
   * ContextProvider - callback
   */
  public ViewContext getContext() {
    return context!=null ? new ViewContext(context) : null;
  }

  /**
   * our context setter
   */
  @Override
  public void setContext(Context newContext, boolean isActionPerformed) {
    
    // disconnect from old
    if (context!=null) 
      context.getGedcom().removeGedcomListener((GedcomListener)Spin.over(callback));
    renderer = null;
    
    // keep new
    context = newContext;
    
    // hook-up
    if (context!=null) {
      context.getGedcom().addGedcomListener((GedcomListener)Spin.over(callback));

      // resolve blueprint & renderer
      Entity e = context.getEntity();
      Blueprint blueprint;
      if (e==null) blueprint = BLUEPRINT_SELECT;
      else blueprint = getBlueprint(e.getTag()); 
      renderer = new EntityRenderer(blueprint);
      
    }
    
    repaint();
  }
  
  /**
   * @see javax.swing.JComponent#getPreferredSize()
   */
  public Dimension getPreferredSize() {
    return new Dimension(256,160);
  }
  
//    // store settings in registry
//    for (int t=0;t<Gedcom.ENTITIES.length;t++) {
//      String tag = Gedcom.ENTITIES[t];
//      registry.put("blueprint."+tag, getBlueprint(tag).getName()); 
//    }
//    registry.put("antial", isAntialiasing );

  /**
   * @see javax.swing.JComponent#paintComponent(Graphics)
   */
  protected void paintComponent(Graphics g) {
    
    Rectangle bounds = getBounds();
    g.setColor(Color.white);
    g.fillRect(0,0,bounds.width,bounds.height);
    g.setColor(Color.black);

    if (context==null||renderer==null)
      return;
    
      ((Graphics2D)g).setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        isAntialiasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF
      );

    renderer.render(g, context!=null ? context.getEntity() : null, new Rectangle(0,0,bounds.width,bounds.height));
  }

  /** 
   * Get blueprint used for given type
   */
  /*package*/ Blueprint getBlueprint(String tag) {
    Blueprint result = (Blueprint)type2blueprint.get(tag);
    if (result==null) {
      result = BlueprintManager.getInstance().getBlueprint(tag, "");
      type2blueprint.put(tag, result);
    }
    return result;
  }
  
  /**
   * Set the blueprints used (map tag to blueprint)
   */
  /*package*/ void setBlueprints(Map<String,Blueprint> setType2Blueprints) {
    type2blueprint = setType2Blueprints;
    // show
    repaint();
  }
  
  /**
   * Gets the blueprints used (map tag to blueprint)
   */
  /*package*/ Map<String,Blueprint> getBlueprints() {
    return type2blueprint;
  }
  
  /**
   * Sets isAntialiasing   */
  public void setAntialiasing(boolean set) {
    isAntialiasing = set;
    repaint();
  }
  
  /**
   * Gets isAntialiasing
   */
  public boolean isAntialiasing() {
    return isAntialiasing;
  }

} //EntityView
