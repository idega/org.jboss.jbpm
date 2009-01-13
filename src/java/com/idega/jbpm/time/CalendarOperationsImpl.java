package com.idega.jbpm.time;

import java.util.Date;

import org.jbpm.calendar.BusinessCalendar;
import org.jbpm.calendar.Duration;
import org.jbpm.util.Clock;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.CoreConstants;

/**
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 * 
 *          Last modified: $Date: 2009/01/13 13:11:29 $ by $Author: civilis $
 */
@Service("calendarOps")
@Scope("singleton")
public class CalendarOperationsImpl implements CalendarOperations {

	private BusinessCalendar businessCalendar;

	public CalendarOperationsImpl() {

		businessCalendar = new BusinessCalendar();
	}

	public Date add(Date date, String expression) {

		expression = CoreConstants.PLUS + expression;
		return eval(date, expression);
	}

	public Date substract(Date date, String expression) {

		expression = CoreConstants.MINUS + expression;
		return eval(date, expression);
	}

	private Date eval(Date baseDate, String expression) {

		Date resultDate = null;
		Duration duration;

		if (baseDate != null
				&& (expression == null || expression.length() == 0)) {
			resultDate = baseDate;
		} else {
			duration = new Duration(expression);
			resultDate = getBusinessCalendar().add(
					(baseDate != null) ? baseDate : Clock.getCurrentTime(),
					duration);
		}

		return resultDate;
	}

	protected BusinessCalendar getBusinessCalendar() {
		return businessCalendar;
	}
}