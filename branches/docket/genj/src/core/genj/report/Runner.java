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
package genj.report;

import genj.gedcom.Gedcom;
import genj.gedcom.UnitOfWork;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A runner for reports
 */
/*package*/ class Runner implements Runnable {

  private final static long FLUSH_WAIT = 500;

  private Gedcom gedcom;
  private Object context;
  private Report report;
  private RunnerListener listener;
  
  /*package*/ Runner(Gedcom gedcom, Object context, Report report, RunnerListener listener) {
    this.gedcom = gedcom;
    this.context= context;
    this.report = report;
    this.listener = listener;
  }
  
  public void run() {
    
    // set report context
    report.setOut(new PrintWriter(new WriterImpl()));
    
    // signal start
    listener.started();
    
    try{
      if (report.isReadOnly()) {
        report.start(context);
      } else {
        final Object finalContext = context;
        gedcom.doUnitOfWork(new UnitOfWork() {
          public void perform(Gedcom gedcom) {
            try {
              report.start(finalContext);
            } catch (Throwable t) {
              throw new RuntimeException(t);
            }
          }
        });
      }    
    } catch (Throwable t) {
      Throwable cause = t.getCause();
      if (cause instanceof InterruptedException)
        report.println("***cancelled");
      else
        report.println(cause!=null?cause:t);
    } finally {
      // signal stop
      listener.stopped();
    }

    // flush
    report.flush();

//  // check last line for url
//  URL url = null;
//  try {
//    AbstractDocument doc = (AbstractDocument)output.getDocument();
//    Element p = doc.getParagraphElement(doc.getLength()-1);
//    String line = doc.getText(p.getStartOffset(), p.getEndOffset()-p.getStartOffset());
//    url = new URL(line);
//  } catch (Throwable t) {
//  }
//
//  if (url!=null) {
//    try {
//      output.setPage(url);
//    } catch (IOException e) {
//      LOG.log(Level.WARNING, "couldn't show html in report output", e);
//    }
//  }
//
  
  }
  
  /**
   * A printwriter that directs output to listener
   */
  private class WriterImpl extends Writer {

    /** buffer */
    private StringBuffer buffer = new StringBuffer(4*1024);

    /** timer */
    private long lastFlush = -1;
    
    /**
     * @see java.io.Writer#close()
     */
    public void close() {
      // clear buffer
      buffer.setLength(0);
    }

    /**
     * @see java.io.Writer#flush()
     */
    public void flush() {

      // something to flush?
      if (buffer.length()==0)
        return;

      // mark
      lastFlush = System.currentTimeMillis();

      // grab text, reset buffer and dump it
      String txt = buffer.toString();
      buffer.setLength(0);
      listener.flushed(txt);
        
      // done
    }

    
    /**
     * @see java.io.Writer#write(char[], int, int)
     */
    public void write(char[] cbuf, int off, int len) throws IOException {
      // append to buffer - strip any \r from \r\n
      for (int i=0;i<len;i++) {
        char c = cbuf[off+i];
        if (c!='\r') buffer.append(c);
      }
      // check flush
      if (System.currentTimeMillis()-lastFlush > FLUSH_WAIT)
        flush();
      // done
    }

  } //OutputWriter
}
