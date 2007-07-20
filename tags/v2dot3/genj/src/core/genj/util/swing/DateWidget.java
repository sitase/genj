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
package genj.util.swing;

import genj.gedcom.GedcomException;
import genj.gedcom.MetaProperty;
import genj.gedcom.time.Calendar;
import genj.gedcom.time.PointInTime;
import genj.util.ActionDelegate;
import genj.util.ChangeSupport;
import genj.util.WordBuffer;
import genj.window.CloseWindow;
import genj.window.WindowManager;

import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

/**
 * Generic component for editing dates
 * @author Tomas Dahlqvist fix for US, European and ISO handling of Date
 */
public class DateWidget extends JPanel {
  
  /** components */
  private PopupWidget widgetCalendar; 
  private TextFieldWidget widgetDay,widgetYear;
  private ChoiceWidget widgetMonth;
  
  /** current calendar */
  private Calendar calendar; 
  
  /** window manager */
  private WindowManager manager;
  
  /** change support */
  private ChangeSupport changeSupport = new ChangeSupport(this) {
    public void fireChangeEvent() {
      // update our status
      updateStatus();
      // continue
      super.fireChangeEvent();
    }
  };
    
  /**
   * Constructor
   */
  public DateWidget(PointInTime pit, WindowManager mgr) {

    manager = mgr;
    calendar = pit.getCalendar();
        
    // create calendar switches
    ArrayList switches = new ArrayList(PointInTime.CALENDARS.length+1);
    for (int s=0;s<PointInTime.CALENDARS.length;s++)
      switches.add(new SwitchCalendar(PointInTime.CALENDARS[s]));
    
    // initialize Sub-components
    widgetCalendar = new PopupWidget(); 
    widgetCalendar.setActions(switches);
    
    widgetYear  = new TextFieldWidget("",5+1);
    widgetYear.setSelectAllOnFocus(true);
    widgetYear.addChangeListener(changeSupport);
    
    widgetMonth = new ChoiceWidget(new Object[0], null);
    widgetMonth.setIgnoreCase(true);
    widgetMonth.setSelectAllOnFocus(true);
    widgetMonth.addChangeListener(changeSupport);

    widgetDay   = new TextFieldWidget("",2+1);
    widgetDay.setSelectAllOnFocus(true);
    widgetDay.addChangeListener(changeSupport);
    
    // Layout
    NestedBlockLayout layout = new NestedBlockLayout(true, 1);
    setLayout(layout);
    
    add(widgetCalendar);

    String format;
    switch (new SimpleDateFormat().toPattern().charAt(0)) {
      case 'm': case 'M':
        add(widgetMonth); add(widgetDay) ; add(widgetYear); format = "mmm/dd/yyyy"; break;
      case 'd': case 'D':
        add(widgetDay) ; add(widgetMonth); add(widgetYear); format = "dd.mmm.yyyy"; break;
      default: 
        add(widgetYear); add(widgetMonth); add(widgetDay ); format = "yyyy-mmm-dd"; break;
    }
    
    widgetDay.setToolTipText(format);
    widgetMonth.setToolTipText(format);
    widgetYear.setToolTipText(format);
    
    // Status
    setValue(pit);
    updateStatus();

    // Done
  }
  
  /**
   * Add change listener
   */
  public void addChangeListener(ChangeListener l) {
    changeSupport.addChangeListener(l);
  }
  
  /**
   * Remove change listener
   */
  public void removeChangeListener(ChangeListener l) {
    changeSupport.removeChangeListener(l);
  }
  
  /**
   * Set current value
   */
  public void setValue(PointInTime pit) {

    // keep calendar    
    calendar = pit.getCalendar();

    // update tooltip
    widgetCalendar.setToolTipText(calendar.getName());
    
    // update year widget
    widgetYear.setText(calendar.getYear(pit.getYear (), true));

    // update day widget
    widgetDay.setText(calendar.getDay(pit.getDay()));

    // update month widget
    String[] months = calendar.getMonths(true);
    widgetMonth.setValues(Arrays.asList(months));
    try {
      widgetMonth.setSelectedItem(null);
      widgetMonth.setSelectedItem(months[pit.getMonth()]);
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    
    // focus
    getComponent(1).requestFocusInWindow();
    
    // done
  }
  
  /**
   * Get current value
   */
  public PointInTime getValue() {

    int 
      u = PointInTime.UNKNOWN,
      d = u,
      m = u,
      y = u;
      
    // analyze day
    String day = widgetDay.getText().trim();
    if (day.length()>0) {
      try {
        d = Integer.parseInt(day) - 1;
      } catch (NumberFormatException e) {
        return null; 
      }
    }
    // analyze year
    String year = widgetYear.getText().trim();
    if (year.length()>0) {
      try {
        y = calendar.getYear(year);
      } catch (GedcomException e) {
        return null; 
      }
    }
    // analyze month
    String month = widgetMonth.getText();
    if (month.length()>0) {
      try {
        m = Integer.parseInt(month) - 1;
      } catch (NumberFormatException e) {
        String[] months = calendar.getMonths(true);
        for (m=0;m<months.length;m++)
          if (month.equals(months[m])) break;
        if (m==months.length) 
          return null;
      }
    }
    
    // generate result
    PointInTime result = new PointInTime(d, m, y, calendar);
    
    // is it valid?
    if ((d==u&&m==u&&y==u)||result.isValid())
      return result;
    
    // done 
    return null;
  }

  /**
   * Update the status icon
   */
  private void updateStatus() {
    // check whether valid
    PointInTime value = getValue();
    if (value==null) {
      // show 'X' on disabled button
      widgetCalendar.setEnabled(false);
      widgetCalendar.setIcon(MetaProperty.IMG_ERROR);
    } else {
      // show current calendar on enabled button
      widgetCalendar.setEnabled(true);
      widgetCalendar.setIcon(calendar.getImage());
    }
  }

  /**
   * Return the maximum size this component should be sized to
   */
  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, super.getPreferredSize().height);
  }

  /**
   * @see javax.swing.JComponent#requestFocus()
   */
  public void requestFocus() {
    getComponent(1).requestFocus();
  }
  
  /**
   * @see javax.swing.JComponent#requestFocusInWindow()
   */
  public boolean requestFocusInWindow() {
    return getComponent(1).requestFocusInWindow();
  }
  
  /**
   * Action to switch calendar
   */
  private class SwitchCalendar extends ActionDelegate {
    /** the calendar to switch to */
    private Calendar newCalendar;
    /**
     * Constructor
     */
    private SwitchCalendar(Calendar cal) {
      newCalendar = cal;
      setImage(newCalendar.getImage());
    }
    /**
     * @see genj.util.ActionDelegate#getText()
     */
    public String getText() {
      WordBuffer result = new WordBuffer(newCalendar.getName());
      result.setFiller(" - ");
      try {
        PointInTime pit = getValue().getPointInTime(newCalendar); 
        result.append(pit.getDayOfWeek(true));
        result.append(pit);
      } catch (Throwable t) {
      }
      return result.toString();
    }
    /**
     * @see genj.util.ActionDelegate#execute()
     */
    protected void execute() {
      PointInTime pit = getValue();
      if (pit==null)
        return;
      try {
        pit.set(newCalendar);
      } catch (GedcomException e) {
        int rc = manager.openDialog(null, Calendar.TXT_CALENDAR_SWITCH, WindowManager.IMG_ERROR, e.getMessage(), CloseWindow.OKand(Calendar.TXT_CALENDAR_RESET), DateWidget.this);
        if (rc==0) 
          return;
        pit = new PointInTime(newCalendar);
      }
      // change
      setValue(pit);
      // update current status
      updateStatus();
    }
  } // SwitchCalendar
  
} //DateEntry