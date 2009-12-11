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
package genj.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A proxy for calling interfaces without exceptions coming through
 */
public class SafeProxy {

  /**
   * harden an implementation of interfaces against exceptions 
   * @param implementation implementation to harden
   * @return
   */
  public static<T> T harden(final T implementation) {
    return harden(implementation, Logger.getAnonymousLogger());
  }
  
  /**
   * harden an implementation of interfaces against exceptions 
   * @param implementation implementation to harden
   * @return
   */
  @SuppressWarnings("unchecked")
  public static<T> T harden(final T implementation, Logger logger) {
    if (logger==null||implementation==null)
      throw new IllegalArgumentException("implementation|logger==null");
    
    return (T)Proxy.newProxyInstance(implementation.getClass().getClassLoader(), implementation.getClass().getInterfaces(), new SafeHandler<T>(implementation, logger));
  }
  
  /** the proxy handler */
  private static class SafeHandler<T> implements InvocationHandler {
    
    private T impl;
    private Logger logger;
    
    private SafeHandler(T impl, Logger logger) {
      this.impl = impl;
      this.logger = logger;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Throwable t;
      try {
        return method.invoke(impl, args);
      } catch (InvocationTargetException ite) {
        t = ite.getCause();
      } catch (Throwable tt) {
        t = tt;
      }
      logger.log(Level.WARNING, "Implementation "+impl.getClass().getName() + "." + method.getName()+" threw exception "+t.getClass().getName()+"("+t.getMessage()+")", t);
      return null;
    }
  }

}
