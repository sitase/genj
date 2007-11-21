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

import genj.crypto.Enigma;
import genj.edit.actions.CreateAlias;
import genj.edit.actions.CreateAssociation;
import genj.edit.actions.CreateChild;
import genj.edit.actions.CreateEntity;
import genj.edit.actions.CreateParent;
import genj.edit.actions.CreateSibling;
import genj.edit.actions.CreateSpouse;
import genj.edit.actions.CreateXReference;
import genj.edit.actions.DelEntity;
import genj.edit.actions.DelProperty;
import genj.edit.actions.OpenForEdit;
import genj.edit.actions.Redo;
import genj.edit.actions.RunExternal;
import genj.edit.actions.SetPlaceHierarchy;
import genj.edit.actions.SetSubmitter;
import genj.edit.actions.SwapSpouses;
import genj.edit.actions.TogglePrivate;
import genj.edit.actions.Undo;
import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.MetaProperty;
import genj.gedcom.Property;
import genj.gedcom.PropertyEvent;
import genj.gedcom.PropertyFamilyChild;
import genj.gedcom.PropertyFile;
import genj.gedcom.PropertyMedia;
import genj.gedcom.PropertyNote;
import genj.gedcom.PropertyPlace;
import genj.gedcom.PropertyRepository;
import genj.gedcom.PropertySource;
import genj.gedcom.PropertySubmitter;
import genj.gedcom.Submitter;
import genj.io.FileAssociation;
import genj.plugin.ExtensionPoint;
import genj.util.Registry;
import genj.util.swing.ImageIcon;
import genj.view.ExtendContextMenu;
import genj.view.ViewContext;
import genj.view.ViewPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;

/**
 * A view plugin providing editing view and actions 
 */
public class EditViewPlugin extends ViewPlugin {
  
  /**
   * Adding our custom edit actions
   * @see genj.view.ViewPlugin#extend(genj.plugin.ExtensionPoint)
   */
  public void extend(ExtensionPoint ep) {
    
    // let super do its thing
    super.extend(ep);
    
    // our context extension
    if (ep instanceof ExtendContextMenu)
      extend(((ExtendContextMenu)ep).getContext());
    
  }

  /** our image */
  protected ImageIcon getImage() {
    return Images.imgView;
  }
  
  /** our text */
  protected String getTitle() {
    return EditView.resources.getString("title");
  }
  
  /** our view */
  protected JComponent createView(Gedcom gedcom, Registry registry) {
    return new EditView(gedcom, registry);
  }
  
  /**
   * Enrich a view context with our actions
   */
  private void extend(ViewContext context) {
    
    // list of properties or a single property in there?
    Property[] properties = context.getProperties();
    if (properties.length>1) {
      extend(context, properties);
    } else if (properties.length==1) {
      // recursively 
      Property property = properties[0];
      while (property!=null&&!(property instanceof Entity)&&!property.isTransient()) {
        extend(context, property);
        property = property.getParent();
      }
    }    
    
    // items for set or single entity
    Entity[] entities = context.getEntities();
    if (entities.length>1) {
      extend(context, entities);
    } else if (entities.length==1) {
      Entity entity = entities[0];
      extend(context, entity);
    }
        
    // items for gedcom
    extend(context, context.getGedcom());

    // done
  }
  
  /**
   * actions for properties
   */
  private void extend(ViewContext context, Property[] properties) {
    // Toggle "Private"
    if (Enigma.isAvailable())
      context.addAction(properties, new TogglePrivate(properties[0].getGedcom(), Arrays.asList(properties)));
    // Delete
    context.addAction(properties, new DelProperty(properties));
  }

  /**
   * actions for a single property
   */
  private void extend(ViewContext context, Property property) {
    
    // FileAssociationActions for PropertyFile
    if (property instanceof PropertyFile)  {
      PropertyFile file = (PropertyFile)property; 
      
      // find suffix
      String suffix = file.getSuffix();
        
      // lookup associations
      List assocs = FileAssociation.getAll(suffix);
      if (assocs.isEmpty()) {
        context.addAction(property, new RunExternal(file));
      } else {
        for (Iterator it = assocs.iterator(); it.hasNext(); ) {
          FileAssociation fa = (FileAssociation)it.next(); 
          context.addAction(property, new RunExternal(file,fa));
        }
      }
    }
    
    // Place format for PropertyFile
    if (property instanceof PropertyPlace)  {
      context.addAction(property, new SetPlaceHierarchy((PropertyPlace)property)); 
    }
    
    // Check what xrefs can be added
    MetaProperty[] subs = property.getNestedMetaProperties(0);
    for (int s=0;s<subs.length;s++) {
      // NOTE REPO SOUR SUBM (BIRT|ADOP)FAMC
      Class type = subs[s].getType();
      if (type==PropertyNote.class||
          type==PropertyRepository.class||
          type==PropertySource.class||
          type==PropertySubmitter.class||
          type==PropertyFamilyChild.class||
          (type==PropertyMedia.class&&genj.gedcom.Options.getInstance().isAllowNewOBJEctEntities) 
        ) {
        // .. make sure @@ forces a non-substitute!
        context.addAction(property, new CreateXReference(property,subs[s].getTag()));
        // continue
        continue;
      }
    }
    
    // Add Association to this one (*only* for events)
    if (property instanceof PropertyEvent)
      context.addAction(property, new CreateAssociation(property));
    
    // Toggle "Private"
    if (Enigma.isAvailable())
      context.addAction(property, new TogglePrivate(property.getGedcom(), Collections.singletonList(property)));
    
    // Delete
    if (!property.isTransient()) 
      context.addAction(property, new DelProperty(property));

    // done
  }

  /**
   * actions for entitites
   */
  private void extend(ViewContext context, Entity[] entities) {
    // none
  }
  
  /**
   * actions for a single entity
   */
  private void extend(ViewContext context, Entity entity) {
    
    // indi?
    if (entity instanceof Indi) extend(context, (Indi)entity);
    // fam?
    if (entity instanceof Fam) extend(context, (Fam)entity);
    // submitter?
    if (entity instanceof Submitter) extend(context, (Submitter)entity);
    
    // Check what xrefs can be added
    MetaProperty[] subs = entity.getNestedMetaProperties(0);
    for (int s=0;s<subs.length;s++) {
      // NOTE||REPO||SOUR||SUBM
      Class type = subs[s].getType();
      if (type==PropertyNote.class||
          type==PropertyRepository.class||
          type==PropertySource.class||
          type==PropertySubmitter.class||
          (type==PropertyMedia.class&&genj.gedcom.Options.getInstance().isAllowNewOBJEctEntities) 
          ) {
        context.addAction(entity, new CreateXReference(entity,subs[s].getTag()));
      }
    }

    // add delete
    context.addAction(entity, new DelEntity(entity));
    
    // add an "edit in EditView"
    EditView[] edits = EditView.getInstances(entity.getGedcom());
    if (edits.length==0) {
      context.addAction(entity, new OpenForEdit(new ViewContext(entity)));
    }
    
    // done
  }

  /**
   * Create actions for Individual
   */
  private void extend(ViewContext context, Indi indi) {
    context.addAction(indi, new CreateChild(indi));
    context.addAction(indi, new CreateParent(indi));
    context.addAction(indi, new CreateSpouse(indi));
    context.addAction(indi, new CreateSibling(indi, true));
    context.addAction(indi, new CreateSibling(indi, false));
    context.addAction(indi, new CreateAlias(indi));
  }
  
  /**
   * Create actions for Families
   */
  private void extend(ViewContext context, Fam fam) {
    context.addAction(fam, new CreateChild(fam));
    if (fam.getNoOfSpouses()<2)
      context.addAction(fam, new CreateParent(fam));
    if (fam.getNoOfSpouses()!=0)
      context.addAction(fam, new SwapSpouses(fam));
  }
  
  /**
   * Create actions for Submitters
   */
  private void extend(ViewContext context, Submitter submitter) {
    context.addAction(submitter, new SetSubmitter(submitter));
  }
  
  /**
   * actions for gedcom
   */
  private void extend(ViewContext context, Gedcom gedcom) {
    // create the actions
    context.addAction(gedcom, new CreateEntity(gedcom, Gedcom.INDI));
    context.addAction(gedcom, new CreateEntity(gedcom, Gedcom.FAM ));
    context.addAction(gedcom, new CreateEntity(gedcom, Gedcom.NOTE));
    if (genj.gedcom.Options.getInstance().isAllowNewOBJEctEntities)
      context.addAction(gedcom, new CreateEntity(gedcom, Gedcom.OBJE));
    context.addAction(gedcom, new CreateEntity(gedcom, Gedcom.REPO));
    context.addAction(gedcom, new CreateEntity(gedcom, Gedcom.SOUR));
    context.addAction(gedcom, new CreateEntity(gedcom, Gedcom.SUBM));

    context.addSeparator(gedcom);
    context.addAction(gedcom, new Undo(gedcom));
    context.addAction(gedcom, new Redo(gedcom));

    // done
  }

} //EditViewPlugin
