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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.misc.Service;

/**
 * Manager of all GenJ plugins
 */
public class PluginManager {
  
  private static PluginManager instance;
  private Plugin[] plugins;
  
  /*package*/ static Logger LOG = Logger.getLogger("genj.plugin");
  
  /**
   * Singleton
   */
  private PluginManager() {
  }
  
  /**
   * Singleton access
   */
  public static PluginManager get() {
    if (instance==null) {
      synchronized (PluginManager.class) {
        if (instance==null)
          instance = new PluginManager();
      }
    }
    return instance;
  }
  
  /**
   * Let plugins enrich an extension point
   */
  public void extend(ExtensionPoint ep) {
    // pass it around
    Plugin[] plugins = getPlugins();
    for (int i = 0; i < plugins.length; i++) {

      ep.before(plugins[i]);
      
      try {
        plugins[i].extend(ep);
      } catch (Throwable t) {
        LOG.log(Level.WARNING, "Unexpected throwable in plugin "+plugins[i].getClass().getName(), t);
      }
    }
  }
  
  /**
   * Resolve all plugins
   */
  public synchronized Plugin[] getPlugins() {
    if (plugins==null) {
      List result = new ArrayList();
      Iterator it = Service.providers(Plugin.class);
      while (it.hasNext()) {
        try {
          Plugin plugin = (Plugin)it.next();
          //plugin.initPlugin(this);
          LOG.log(Level.FINE, "Loaded plugin "+plugin.getClass().getName()); 
					result.add(plugin);
        } catch (Throwable t) {
          LOG.log(Level.WARNING, "Couldn't load plugin", t);
        }
      }
      plugins = (Plugin[])result.toArray(new Plugin[result.size()]);
    }
    return plugins;
  }

} //PluginManager
