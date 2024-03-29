// $Id: Tuplizer.java 10119 2006-07-14 00:09:19Z steve.ebersole@jboss.com $
package org.hibernate.tuple;

import org.hibernate.HibernateException;

/**
 * A tuplizer defines the contract for things which know how to manage
 * a particular representation of a piece of data, given that
 * representation's {@link org.hibernate.EntityMode} (the entity-mode
 * essentially defining which representation).
 * </p>
 * If that given piece of data is thought of as a data structure, then a tuplizer
 * is the thing which knows how to<ul>
 * <li>create such a data structure appropriately
 * <li>extract values from and inject values into such a data structure
 * </ul>
 * </p>
 * For example, a given piece of data might be represented as a POJO class.
 * Here, it's representation and entity-mode is POJO.  Well a tuplizer for POJO
 * entity-modes would know how to<ul>
 * <li>create the data structure by calling the POJO's constructor
 * <li>extract and inject values through getters/setter, or by direct field access, etc
 * </ul>
 * </p>
 * That same piece of data might also be represented as a DOM structure, using
 * the tuplizer associated with the DOM4J entity-mode, which would generate instances
 * of {@link org.dom4j.Element} as the data structure and know how to access the
 * values as either nested {@link org.dom4j.Element}s or as {@link org.dom4j.Attribute}s.
 *
 * @see org.hibernate.tuple.entity.EntityTuplizer
 * @see org.hibernate.tuple.component.ComponentTuplizer
 *
 * @author Steve Ebersole
 */
public interface Tuplizer {

	/**
	 * Extract the current values contained on the given entity.
	 *
	 * @param entity The entity from which to extract values.
	 * @return The current property values.
	 * @throws HibernateException
	 */
	public Object[] getPropertyValues(Object entity) throws HibernateException;

	/**
	 * Inject the given values into the given entity.
	 *
	 * @param entity The entity.
	 * @param values The values to be injected.
	 * @throws HibernateException
	 */
	public void setPropertyValues(Object entity, Object[] values) throws HibernateException;

	/**
	 * Extract the value of a particular property from the given entity.
	 *
	 * @param entity The entity from which to extract the property value.
	 * @param i The index of the property for which to extract the value.
	 * @return The current value of the given property on the given entity.
	 * @throws HibernateException
	 */
	public Object getPropertyValue(Object entity, int i) throws HibernateException;

	/**
	 * Generate a new, empty entity.
	 *
	 * @return The new, empty entity instance.
	 * @throws HibernateException
	 */
	public Object instantiate() throws HibernateException;
	
	/**
	 * Is the given object considered an instance of the the entity (acconting
	 * for entity-mode) managed by this tuplizer.
	 *
	 * @param object The object to be checked.
	 * @return True if the object is considered as an instance of this entity
	 *      within the given mode.
	 */
	public boolean isInstance(Object object);

	/**
	 * Return the pojo class managed by this tuplizer.
	 * </p>
	 * Need to determine how to best handle this for the Tuplizers for EntityModes
	 * other than POJO.
	 * </p>
	 * todo : be really nice to not have this here since it is essentially pojo specific...
	 *
	 * @return The persistent class.
	 */
	public Class getMappedClass();

}
