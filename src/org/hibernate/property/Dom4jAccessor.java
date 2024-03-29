// $Id: Dom4jAccessor.java 8063 2005-09-01 18:49:39Z oneovthafew $
package org.hibernate.property;

import java.lang.reflect.Method;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Node;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Responsible for accessing property values represented as a dom4j Element
 * or Attribute.
 *
 * @author Steve Ebersole
 */
public class Dom4jAccessor implements PropertyAccessor {
	private String nodeName;
	private Type propertyType;
	private final SessionFactoryImplementor factory;

	public Dom4jAccessor(String nodeName, Type propertyType, SessionFactoryImplementor factory) {
		this.factory = factory;
		this.nodeName = nodeName;
		this.propertyType = propertyType;
		
	}

	/**
	 * Create a "getter" for the named attribute
	 */
	public Getter getGetter(Class theClass, String propertyName) 
	throws PropertyNotFoundException {
		if (nodeName==null) {
			throw new MappingException("no node name for property: " + propertyName);
		}
		if ( ".".equals(nodeName) ) {
			return new TextGetter(propertyType, factory);
		}
		else if ( nodeName.indexOf('/')>-1 ) {
			return new ElementAttributeGetter(nodeName, propertyType, factory);
		}
		else if ( nodeName.indexOf('@')>-1 ) {
			return new AttributeGetter(nodeName, propertyType, factory);
		}
		else {
			return new ElementGetter(nodeName, propertyType, factory);
		}
	}

	/**
	 * Create a "setter" for the named attribute
	 */
	public Setter getSetter(Class theClass, String propertyName) 
	throws PropertyNotFoundException {
		if (nodeName==null) {
			throw new MappingException("no node name for property: " + propertyName);
		}
		if ( ".".equals(nodeName) ) {
			return new TextSetter(propertyType);
		}
		else if ( nodeName.indexOf('/')>-1 ) {
			return new ElementAttributeSetter(nodeName, propertyType);
		}
		else if ( nodeName.indexOf('@')>-1 ) {
			return new AttributeSetter(nodeName, propertyType);
		}
		else {
			return new ElementSetter(nodeName, propertyType);
		}
	}

	/**
	 * Defines the strategy for getting property values out of a dom4j Node.
	 */
	public abstract static class Dom4jGetter implements Getter {
		protected final Type propertyType;
		protected final SessionFactoryImplementor factory;
		
		Dom4jGetter(Type propertyType, SessionFactoryImplementor factory) {
			this.propertyType = propertyType;
			this.factory = factory;
		}
		
		public Object getForInsert(Object owner, Map mergeMap, SessionImplementor session) 
		throws HibernateException {
			return get( owner );
		}

		/**
		 * Get the declared Java type
		 */
		public Class getReturnType() {
			return Object.class;
		}

		/**
		 * Optional operation (return null)
		 */
		public String getMethodName() {
			return null;
		}

		/**
		 * Optional operation (return null)
		 */
		public Method getMethod() {
			return null;
		}
	}

	public abstract static class Dom4jSetter implements Setter {
		protected final Type propertyType;

		Dom4jSetter(Type propertyType) {
			this.propertyType = propertyType;
		}
		
		/**
		 * Optional operation (return null)
		 */
		public String getMethodName() {
			return null;
		}

		/**
		 * Optional operation (return null)
		 */
		public Method getMethod() {
			return null;
		}
	}
	
	/**
	 * For nodes like <tt>"."</tt>
	 * @author Gavin King
	 */
	public static class TextGetter extends Dom4jGetter {
		
		TextGetter(Type propertyType, SessionFactoryImplementor factory) {
			super(propertyType, factory);
		}

		public Object get(Object owner) throws HibernateException {
			Element ownerElement = (Element) owner;
			return super.propertyType.fromXMLNode(ownerElement, super.factory);
		}	
		
	}
	
	/**
	 * For nodes like <tt>"@bar"</tt>
	 * @author Gavin King
	 */
	public static class AttributeGetter extends Dom4jGetter {
		private final String attributeName;
		
		AttributeGetter(String name, Type propertyType, SessionFactoryImplementor factory) {
			super(propertyType, factory);
			attributeName = name.substring(1);
		}

		public Object get(Object owner) throws HibernateException {
			Element ownerElement = (Element) owner;
			Node attribute = ownerElement.attribute(attributeName);
			return attribute==null ? null : 
				super.propertyType.fromXMLNode(attribute, super.factory);
		}	
		
	}

	/**
	 * For nodes like <tt>"foo"</tt>
	 * @author Gavin King
	 */
	public static class ElementGetter extends Dom4jGetter {
		private final String elementName;
		
		ElementGetter(String name, Type propertyType, SessionFactoryImplementor factory) {
			super(propertyType, factory);
			elementName = name;
		}

		public Object get(Object owner) throws HibernateException {
			Element ownerElement = (Element) owner;
			Node element = ownerElement.element(elementName);
			return element==null ? 
					null : super.propertyType.fromXMLNode(element, super.factory);
		}	
		
	}
	
	/**
	 * For nodes like <tt>"foo/@bar"</tt>
	 * @author Gavin King
	 */
	public static class ElementAttributeGetter extends Dom4jGetter {
		private final String elementName;
		private final String attributeName;
		
		ElementAttributeGetter(String name, Type propertyType, SessionFactoryImplementor factory) {
			super(propertyType, factory);
			elementName = name.substring( 0, name.indexOf('/') );
			attributeName = name.substring( name.indexOf('/')+2 );
		}

		public Object get(Object owner) throws HibernateException {
			Element ownerElement = (Element) owner;
			
			Element element = ownerElement.element(elementName);
			
			if ( element==null ) {
				return null;
			}
			else {
				Attribute attribute = element.attribute(attributeName);
				if (attribute==null) {
					return null;
				}
				else {
					return super.propertyType.fromXMLNode(attribute, super.factory);
				}
			}
		}
	}
	
	
	/**
	 * For nodes like <tt>"."</tt>
	 * @author Gavin King
	 */
	public static class TextSetter extends Dom4jSetter {
		
		TextSetter(Type propertyType) {
			super(propertyType);
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory) 
		throws HibernateException {
			Element owner = ( Element ) target;
			if ( !super.propertyType.isXMLElement() ) { //kinda ugly, but needed for collections with a "." node mapping
				if (value==null) {
					owner.setText(null); //is this ok?
				}
				else {
					super.propertyType.setToXMLNode(owner, value, factory);
				}
			}
		}

	}
	
	/**
	 * For nodes like <tt>"@bar"</tt>
	 * @author Gavin King
	 */
	public static class AttributeSetter extends Dom4jSetter {
		private final String attributeName;
		
		AttributeSetter(String name, Type propertyType) {
			super(propertyType);
			attributeName = name.substring(1);
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory) 
		throws HibernateException {
			Element owner = ( Element ) target;
			Attribute attribute = owner.attribute(attributeName);
			if (value==null) {
				if (attribute!=null) attribute.detach();
			}
			else {
				if (attribute==null) {
					owner.addAttribute(attributeName, "null");
					attribute = owner.attribute(attributeName);
				}
				super.propertyType.setToXMLNode(attribute, value, factory);
			}
		}

	}
	
	/**
	 * For nodes like <tt>"foo"</tt>
	 * @author Gavin King
	 */
	public static class ElementSetter extends Dom4jSetter {
		private final String elementName;
		
		ElementSetter(String name, Type propertyType) {
			super(propertyType);
			elementName = name;
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory) 
		throws HibernateException {
			if (value!=CollectionType.UNFETCHED_COLLECTION) {
				Element owner = ( Element ) target;
				Element existing = owner.element(elementName);
				if (existing!=null) existing.detach();
				if (value!=null) {
					Element element = owner.addElement(elementName);
					super.propertyType.setToXMLNode(element, value, factory);
				}
			}
		}

	}
	
	/**
	 * For nodes like <tt>"foo/@bar"</tt>
	 * @author Gavin King
	 */
	public static class ElementAttributeSetter extends Dom4jSetter {
		private final String elementName;
		private final String attributeName;
		
		ElementAttributeSetter(String name, Type propertyType) {
			super(propertyType);
			elementName = name.substring( 0, name.indexOf('/') );
			attributeName = name.substring( name.indexOf('/')+2 );
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory) 
		throws HibernateException {
			Element owner = ( Element ) target;
			Element element = owner.element(elementName);
			if (value==null) {
				if (element!=null) element.detach();
			}
			else {
				Attribute attribute;
				if (element==null) {
					element = owner.addElement(elementName);
					attribute = null;
				}
				else {
					attribute = element.attribute(attributeName);
				}
				
				if (attribute==null) {
					element.addAttribute(attributeName, "null");
					attribute = element.attribute(attributeName);
				}				
				super.propertyType.setToXMLNode(attribute, value, factory);
			}
		}

	}
	

}
