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
package genj.gedcom;

import genj.util.swing.ImageIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Gedcom Property : ABC
 * Class for encapsulating a property that describes a Reference to an entity
 * @author  Nils Meier
 * @version 0.1 04/29/98
 */
public abstract class PropertyXRef extends Property {

  /** the target property that this xref references */
  private PropertyXRef target = null;

  /** the value for a broken xref */
  private String       value  = null;

  /**
   * Empty Constructor
   */
  public PropertyXRef() {
  }

  /**
   * Constructor with reference
   * @param target reference of property this property links to
   */
  public PropertyXRef(PropertyXRef target) {
    setTarget(target);
  }

  /**
   * Method for notifying being removed from another parent
   */
  /*package*/ void delNotify(Property oldParent) {

    // Let it through
    super.delNotify(oldParent);
    
    // are we referencing something that points back?
    if (target==null)
      return;

    // is it owned by a parent?
    if (target.getParent()==null)
      return;
      
    // ... delete back referencing property unless this is a rollback.
    // We have to use oldParent for transaction resolution since 
    // parent is already set to null by super
    Transaction tx = oldParent.getTransaction();
    if (tx!=null&&!tx.isRollback())
      target.getParent().delProperty(target);

  }

  /**
   * Returns the logical name of the proxy-object which knows this object
   * @return proxy's logical name
   */
  public String getProxy() {
    return "XRef";
  }

  /**
   * Returns the entity this reference points to
   * @return entity this property links to
   */
  public Entity getReferencedEntity() {
    if (target==null) {
      return null;
    }
    return target.getEntity();
  }

  /**
   * Returns the id of the referenced entity
   * @return referenced entity's id
   */
  public String getReferencedId() {
    return value==null ? EMPTY_STRING : value;
  }

  /**
   * Returns the value of this property as string.
   * @return value of this property as <code>String</code>
   */
  public String getValue() {
    return value==null ? EMPTY_STRING : '@'+value+'@';
  }

  /**
   * Returns <b>true</b> if this property is valid
   * @return <code>boolean</code> true or false
   */
  public boolean isValid() {
    return target!=null;
  }
  
  /**
   * Links reference to entity (if not already done)
   * @exception GedcomException when processing link would result in inconsistent state
   */
  public abstract void link() throws GedcomException;

  /**
   * @see genj.gedcom.Property#getDisplayValue()
   */
  public String getDisplayValue() {
    if (target==null)
      return "";
    return target.getEntity().toString();
  }
  
  /**
   * Returns a display value to be shown on the foreign end
   */
  protected String getForeignDisplayValue() {
    return resources.getString("foreign.xref", getEntity().toString());
  }
  
  /**
   * Sets the entity's property this reference points to
   * @param target referenced entity's property
   */
  protected void setTarget(PropertyXRef target) {

    String old = getValue();
    
    // Do it
    this.target=target;
    this.value =target.getEntity().getId();

    // Remember change
    propagateChange(old);

    // Done
  }
  

  /**
   * Returns this reference's target
   * @return target or null
   */  
  public PropertyXRef getTarget() {
    return target;
  }

  /**
   * Sets this property's value as string.
   */
  public void setValue(String set) {

    // ignore if linked
    if (target!=null)
      return;
      
    String old = getValue();

    // remember value
    value = set.replace('@',' ').trim();

    // remember change
    propagateChange(old);
    
    // done
  }
  
  /**
   * @see genj.gedcom.Property#setTag(java.lang.String)
   */
  /*package*/ Property init(MetaProperty meta, String value) throws GedcomException {
    assume(getTag().equals(meta.getTag()), UNSUPPORTED_TAG);
    return super.init(meta, value);
  }

  /**
   * This property as a verbose string
   */
  public String toString() {
    Entity e = getReferencedEntity();
    if (e==null) {
      return super.toString();
    }
    return e.toString();
  }

  /**
   * The expected referenced type
   */
  public abstract String getTargetType();

  /**
   * @see genj.gedcom.Property#getDeleteVeto()
   */
  public String getDeleteVeto() {
    // warn if linked
    if (getReferencedEntity()==null) 
      return null;
    // a specialized message?
    String key = "prop."+getTag().toLowerCase()+".veto";
    if (resources.contains(key))
      return resources.getString(key);
    // fallback to default
    return resources.getString("prop.xref.veto");
  }

  /**
   * Return all entities that are connected to the given entity through
   * a PropertyXRef
   */
  public static Entity[] getReferences(Entity ent) {
    List result = new ArrayList(10);
    // loop through pxrefs
    List ps = ent.getProperties(PropertyXRef.class);
    for (int p=0; p<ps.size(); p++) {
    	PropertyXRef px = (PropertyXRef)ps.get(p);
      Property target = px.getTarget(); 
      if (target!=null) result.add(target.getEntity());
    }
    // done
    return (Entity[])result.toArray(new Entity[result.size()]);
  }

  /**
   * overriden to make sure we always look for sub-meta-properties
   * with FILTER_XREF
   * @see genj.gedcom.Property#getMetaProperties(int)
   */
  public MetaProperty[] getSubMetaProperties(int filter) {
    return super.getSubMetaProperties(MetaProperty.FILTER_XREF|filter);
  }
  
  /**
   * Final impl for image of xrefs
   * @see genj.gedcom.Property#getImage(boolean)
   */
  public ImageIcon getImage(boolean checkValid) {
    return overlay(super.getImage(false));
  }
  
  /**
   * Overlay image with current status
   */
  protected ImageIcon overlay(ImageIcon img) {
    ImageIcon overlay = target!=null?MetaProperty.IMG_LINK:MetaProperty.IMG_ERROR;
    return img.getOverLayed(overlay);
  }
  
  /**
   * Patched to now allow private - would require that
   * the opposite link is made private, too
   * @see genj.gedcom.Property#setPrivate(boolean, boolean)
   */
  public void setPrivate(boolean set, boolean recursively) {
    // ignored
  }

  /**
   * Comparison based on target
   */  
  public int compareTo(Object o) {
    
    // safety check
    if (!(o instanceof PropertyXRef)) 
      return super.compareTo(o);
    PropertyXRef that = (PropertyXRef)o;
    
    // got the references?
    if (this.getReferencedEntity()==null||that.getReferencedEntity()==null)
      return super.compareTo(that);

    // compare references - using toString() but it should really depend
    // on what the renderer renders
    
    return compare(getReferencedEntity().toString(), that.getReferencedEntity().toString());
  }

} //PropertyXRef