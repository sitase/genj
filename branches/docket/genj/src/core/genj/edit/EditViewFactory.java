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
import genj.crypto.Enigma;
import genj.edit.actions.AbstractChange;
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
import genj.gedcom.GedcomDirectory;
import genj.gedcom.GedcomException;
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
import genj.gedcom.TagPath;
import genj.io.FileAssociation;
import genj.util.Registry;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.util.swing.NestedBlockLayout;
import genj.view.ActionProvider;
import genj.view.ContextSelectionEvent;
import genj.view.ViewContext;
import genj.view.ViewFactory;
import genj.window.WindowManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The factory for the TableView
 */
public class EditViewFactory implements ViewFactory, ActionProvider {
    
  /** a noop is used for separators in returning actions */  
  private final static Action2 aNOOP = Action2.NOOP;
  
  /**
   * @see genj.view.ViewFactory#createView(genj.gedcom.Gedcom, genj.util.Registry, java.awt.Frame)
   */
  public JComponent createView(String title, Gedcom gedcom, Registry registry) {
    return new EditView(title, gedcom, registry);
  }

  /**
   * @see genj.view.ViewFactory#getImage()
   */
  public ImageIcon getImage() {
    return Images.imgView;
  }
  
  /**
   * @see genj.view.ViewFactory#getName(boolean)
   */
  public String getTitle() {
    return EditView.resources.getString("title");
  }

// FIXME need to provide for auto-open edit on selection
//  /**
//   * Callback - context change information
//   */
//  public void handleContextSelectionEvent(ContextSelectionEvent event) {
//    ViewContext context = event.getContext();
//    ViewManager manager = context.getManager();
//    // editor needed?
//    if (!Options.getInstance().isOpenEditor)
//      return;
//    // what's the entity
//    Entity[] entities = context.getEntities();
//    if (entities.length!=1)
//      return;
//    Entity entity = entities[0];
//    // noop if EditView non-sticky or current is open
//    EditView[] edits = EditView.getInstances(context.getGedcom());
//    for (int i=0;i<edits.length;i++) {
//      if (!edits[i].isSticky()||edits[i].getEntity()==entity) 
//        return;
//    }
//    // open
//    new OpenForEdit(context, manager).trigger();
//  }
  
  /**
   * @see genj.view.ActionProvider#createActions(Entity[], ViewManager)
   */
  public List createActions(Property[] properties) {
    List result = new ArrayList();
    // not accepting any entities here
    for (int i = 0; i < properties.length; i++) 
      if (properties[i] instanceof Entity) return result;
    // Toggle "Private"
    if (Enigma.isAvailable())
      result.add(new TogglePrivate(properties[0].getGedcom(), Arrays.asList(properties)));
    // Delete
    result.add(new DelProperty(properties));
    // done
    return result;
  }

  /**
   * @see genj.view.ContextSupport#createActions(Property)
   */
  public List createActions(Property property) {
    
    // create the actions
    List result = new ArrayList();
    
    // FileAssociationActions for PropertyFile
    if (property instanceof PropertyFile)  
      createActions(result, (PropertyFile)property); 
      
    // Place format for PropertyFile
    if (property instanceof PropertyPlace)  
      result.add(new SetPlaceHierarchy((PropertyPlace)property)); 
      
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
          type==PropertyMedia.class 
        ) {
        // .. make sure @@ forces a non-substitute!
        result.add(new CreateXReference(property,subs[s].getTag()));
        // continue
        continue;
      }
    }
    
    // Add Association to events if property is contained in individual
    // or ASSO allows types
    if ( property instanceof PropertyEvent
        && ( (property.getEntity() instanceof Indi)
            || property.getGedcom().getGrammar().getMeta(new TagPath("INDI:ASSO")).allows("TYPE"))  )
      result.add(new CreateAssociation(property));
    
    // Toggle "Private"
    if (Enigma.isAvailable())
      result.add(new TogglePrivate(property.getGedcom(), Collections.singletonList(property)));
    
    // Delete
    if (!property.isTransient()) 
      result.add(new DelProperty(property));

    // done
    return result;
  }

  /**
   * @see genj.view.ViewFactory#createActions(Entity)
   */
  public List createActions(Entity entity) {
    // create the actions
    List result = new ArrayList();
    
    // indi?
    if (entity instanceof Indi) createActions(result, (Indi)entity);
    // fam?
    if (entity instanceof Fam) createActions(result, (Fam)entity);
    // submitter?
    if (entity instanceof Submitter) createActions(result, (Submitter)entity);
    
    // separator
    result.add(Action2.NOOP);

    // Check what xrefs can be added
    MetaProperty[] subs = entity.getNestedMetaProperties(0);
    for (int s=0;s<subs.length;s++) {
      // NOTE||REPO||SOUR||SUBM
      Class type = subs[s].getType();
      if (type==PropertyNote.class||
          type==PropertyRepository.class||
          type==PropertySource.class||
          type==PropertySubmitter.class||
          type==PropertyMedia.class
          ) {
        result.add(new CreateXReference(entity,subs[s].getTag()));
      }
    }

    // add delete
    result.add(Action2.NOOP);
    result.add(new DelEntity(entity));
    
    // add an "edit in EditView"
    EditView[] edits = EditView.getInstances(entity.getGedcom());
    if (edits.length==0) {
      result.add(Action2.NOOP);
      result.add(new OpenForEdit(new ViewContext(entity)));
    }
    // done
    return result;
  }

  /**
   * @see genj.view.ContextMenuSupport#createActions(Gedcom)
   */
  public List createActions(Gedcom gedcom) {
    // create the actions
    List result = new ArrayList();
    result.add(new CreateEntity(gedcom, Gedcom.INDI));
    result.add(new CreateEntity(gedcom, Gedcom.FAM));
    result.add(new CreateEntity(gedcom, Gedcom.NOTE));
    result.add(new CreateEntity(gedcom, Gedcom.OBJE));
    result.add(new CreateEntity(gedcom, Gedcom.REPO));
    result.add(new CreateEntity(gedcom, Gedcom.SOUR));
    result.add(new CreateEntity(gedcom, Gedcom.SUBM));

    for (Gedcom other : GedcomDirectory.getInstance().getGedcoms()) {
      if (other!=gedcom && other.getEntities(Gedcom.INDI).size()>0)
        result.add(new CopyIndividual(gedcom, other));
    }

    result.add(Action2.NOOP);
    result.add(new Undo(gedcom, gedcom.canUndo()));
    result.add(new Redo(gedcom, gedcom.canRedo()));

    // done
    return result;
  }
  
  /** 
   * Frederic - a test action showing cross-gedcom work 
   */
  private class CopyIndividual extends AbstractChange {
    
    private Gedcom source;
    private Indi existing;

    public CopyIndividual(Gedcom dest, Gedcom source) {
      super(dest, Gedcom.getEntityImage(Gedcom.INDI), "Copy individual from "+source);
      this.source = source;
    }
    
    /**
     * Override content components to show to user 
     */
    @Override
    protected JPanel getDialogContent() {
      
      JPanel result = new JPanel(new NestedBlockLayout("<col><row><select wx=\"1\"/></row><row><text wx=\"1\" wy=\"1\"/></row><row><check/><text/></row></col>"));

      // create selector
      final SelectEntityWidget select = new SelectEntityWidget(source, Gedcom.INDI, null);
   
      // wrap it up
      result.add(select);
      result.add(getConfirmComponent());

      // add listening
      select.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // grab current selection (might be null)
          existing = (Indi)select.getSelection();
          refresh();
        }
      });
      
      existing = (Indi)select.getSelection();
      refresh();
      
      // done
      return result;
    }
    
    @Override
    protected void refresh() {
      // TODO Auto-generated method stub
      super.refresh();
    }
    
    private boolean dupe() {
      return gedcom.getEntity(existing.getId())!=null;
    }
    
    @Override
    protected String getConfirmMessage() {
      if (existing==null)
        return "Please select an individual";
      String result = "Copying individual "+existing+" from "+source.getName()+" to "+gedcom.getName();
      if (dupe())
        result += "\n\nNote: Duplicate ID - a new ID will be assigned";
      return result;
    }
    
    @Override
    public void perform(Gedcom gedcom) throws GedcomException {
      Entity e = gedcom.createEntity(Gedcom.INDI, dupe() ? null : existing.getId());
      e.copyProperties(existing.getProperties(), true);
      WindowManager.broadcast(new ContextSelectionEvent(new ViewContext(e), getTarget()));
    }
  
  }

  /**
   * Create actions for Individual
   */
  private void createActions(List result, Indi indi) {
    result.add(new CreateChild(indi, true));
    result.add(new CreateChild(indi, false));
    result.add(new CreateParent(indi));
    result.add(new CreateSpouse(indi));
    result.add(new CreateSibling(indi, true));
    result.add(new CreateSibling(indi, false));
    result.add(new CreateAlias(indi));
  }
  
  /**
   * Create actions for Families
   */
  private void createActions(List result, Fam fam) {
    result.add(new CreateChild(fam, true));
    result.add(new CreateChild(fam, false));
    if (fam.getNoOfSpouses()<2)
      result.add(new CreateParent(fam));
    if (fam.getNoOfSpouses()!=0)
      result.add(new SwapSpouses(fam));
  }
  
  /**
   * Create actions for Submitters
   */
  private void createActions(List result, Submitter submitter) {
    result.add(new SetSubmitter(submitter));
  }
  
  /**  
   * Create actions for PropertyFile
   */
  public static void createActions(List result, PropertyFile file) {

    // find suffix
    String suffix = file.getSuffix();
      
    // lookup associations
    List assocs = FileAssociation.getAll(suffix);
    if (assocs.isEmpty()) {
      result.add(new RunExternal(file));
    } else {
      for (Iterator it = assocs.iterator(); it.hasNext(); ) {
        FileAssociation fa = (FileAssociation)it.next(); 
        result.add(new RunExternal(file,fa));
      }
    }
    // done
  }

} //EditViewFactory
