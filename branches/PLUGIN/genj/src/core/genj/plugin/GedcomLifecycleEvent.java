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
package genj.plugin;

import genj.gedcom.Gedcom;

/**
 * A gedcom lifecycle event for plugins
 */
public class GedcomLifecycleEvent extends PluginEvent {

  /** after gedcom has been loaded */
  public final static int AFTER_GEDCOM_LOADED = 0;
  
  /** before gedcom is saved */
  public final static int BEFORE_GEDCOM_SAVED = 10; 
  
  /** before gedcom is closed */
  public final static int BEFORE_GEDCOM_CLOSED = 20;
  
  /** after gedcom has been closed */
  public final static int AFTER_GEDCOM_CLOSED = 30; 
  
  private Gedcom gedcom;
  private int id;
  
  /**
   * Constructor
   */
  public GedcomLifecycleEvent(Gedcom gedcom, int id) {
    this.gedcom = gedcom;
    this.id = id;
  }
  
  /**
   * Accessor - event id
   */
  public int getId() {
    return id;
  }

  /**
   * Accessor - gedcom object
   */
  public Gedcom getGedcom() {
    return gedcom;
  }

} //PluginGedcomEvent
