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
package genj.io;

import genj.gedcom.Entity;
import genj.util.Resources;

/**
 * An IO Exception for cases where data cannot be encoded
 */
public class GedcomEncodingException extends GedcomIOException {
  
  private final static Resources RESOURCES = Resources.get("genj.io");
  
  /** the entity causing the problem */
  private Entity  entity;
  
  /** constructor */
  public GedcomEncodingException(Entity entity, String encoding) {
      super(RESOURCES.getString("write.error.cantencode", new Object[]{ entity, encoding } ), 0);
      this.entity = entity;
  }

  /** accessor */
  public Entity getEntity() {
    return entity;
  }
  
}