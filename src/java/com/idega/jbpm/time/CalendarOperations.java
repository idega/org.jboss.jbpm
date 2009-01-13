package com.idega.jbpm.time;

import java.util.Date;

/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2009/01/13 13:11:29 $ by $Author: civilis $
 */
public interface CalendarOperations {

	/**
	 * adds to the date provider the time, specified in expression.
	 * 
	 * @param date
	 * @param expression
	 *            time units to add to the date, e.g.: 1 day, 5 minutes, 3
	 *            months. @see org.jbpm.calendar.BusinessCalendar for units
	 *            available, or jbpm documentation
	 * @return result of addition
	 */
	public abstract Date add(Date date, String expression);

	/**
	 * @see add
	 * 
	 * @param date
	 * @param expression
	 * @return
	 */
	public abstract Date substract(Date date, String expression);
}