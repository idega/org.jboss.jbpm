package com.idega.jbpm.exe;import java.lang.annotation.ElementType;import java.lang.annotation.Retention;import java.lang.annotation.RetentionPolicy;import java.lang.annotation.Target;import org.springframework.beans.factory.annotation.Qualifier;/** * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a> * @version $Revision: 1.1 $ * * Last modified: $Date: 2008/08/05 07:18:01 $ by $Author: civilis $ */@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})@Retention(RetentionPolicy.RUNTIME)@Qualifierpublic @interface ProcessWatchType {    String value();}