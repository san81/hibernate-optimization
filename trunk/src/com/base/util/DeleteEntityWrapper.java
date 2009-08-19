package com.base.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Wrapper class for deleting an entity It scan through all its associations
 * (one to many collections) and delete/updates them from the last reachable
 * child colletion based on their mapping configuration ( like
 * delete-cascade=true ). All the delete/update sql queries are constructed with
 * keeping id's in IN clause.
 * 
 * Limitations: -This class works only for the entities, which are not having
 * composit primary key. -Collection level filter are not taken care yet.
 * 
 * @author santosh
 * 
 */
public class DeleteEntityWrapper {


	private Map<String, QueryableCollection> reachableCollectionMap;

	private Map<String, QueryableCollection> nullableCollectionMap;

	private AbstractEntityPersister persister;

	private String tableName;

	private List<DeleteEntityWrapper> childEntities;

	private DeleteEntityWrapper parent;

	private String associationColumnName;

	private Serializable[] ids;

	private boolean idsRetrived = false;

	public DeleteEntityWrapper(AbstractEntityPersister persister,
			Serializable[] ids) {
		this.persister = persister;
		reachableCollectionMap = new HashMap<String, QueryableCollection>();
		nullableCollectionMap = new HashMap<String, QueryableCollection>();
		findReachableCollectionMap();
		this.ids = ids; // to initialize the ids

	}

	/**
	 * Method to find all the collection mappings of
	 * the given entity persistor. and push them into
	 * respective (reachable/nullable) map based on the 
	 * cascade style given in the mapping hbm.
	 */
	private void findReachableCollectionMap() {
		tableName = persister.getTableName();
		String[] properties = persister.getPropertyNames();
		List<String> columnNameList = new ArrayList<String>();
		CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();

		columnNameList.add(persister.getPropertyColumnNames(persister
				.getIdentifierPropertyName())[0]);

		for (int i = 0; i < properties.length; i++) {

			// push all the collections in to reachableCollection map only if
			// cascade enabled
			if (persister.getPropertyTypes()[i] instanceof CollectionType) {
				CollectionType collectionType = (CollectionType) persister
						.getPropertyTypes()[i];
				String role = collectionType.getRole();
				SessionFactoryImplementor sessionFactory = persister
						.getFactory();
				QueryableCollection colPersister = (QueryableCollection) sessionFactory
						.getCollectionPersister(role);
				if (cascadeStyles[i].doCascade(CascadingAction.DELETE)) {
					// handling collection columns with cascade delete enabled
					reachableCollectionMap.put(role, colPersister);
				} else {
					/*
					 * for this collection cascade delete is disabled. we just
					 * have to set its FK value to null
					 */
					nullableCollectionMap.put(role, colPersister);
				}
			}
		}

	}

	private boolean hasChildEntities() {
		return !(reachableCollectionMap.isEmpty());
	}

	/**
	 * This method constructs the DeleteEntityWrapper for 
	 * each reachable child entity, and returns that collection.
	 * 
	 * @return
	 */
	private List<DeleteEntityWrapper> getChildEntities() {
		if (childEntities == null && hasChildEntities()) {
			childEntities = new ArrayList<DeleteEntityWrapper>();
			for (String key : reachableCollectionMap.keySet()) {
				DeleteEntityWrapper childWrapper = new DeleteEntityWrapper(
						(AbstractEntityPersister) reachableCollectionMap.get(
								key).getElementPersister(), null);
				childWrapper.parent = this;
				childWrapper.associationColumnName = reachableCollectionMap
						.get(key).getKeyColumnNames()[0];
				childEntities.add(childWrapper);
			}
		}
		return childEntities;
	}

	/**
	 * Method to delete the given entity record 
	 * with the given id value.
	 * 
	 * @throws DataAccessException
	 */
	public void delete()  {
		runDeleteQuery(this);
	}

	/**
	 * Method to return the ID column value list.
	 * 
	 * @return
	 * @
	 */
	private Serializable[] getIdList()  {
		if (ids == null && !idsRetrived) {
			retrieveValues();
		}
		return ids;
	}

	/**
	 * Method to set the ID column value list.
	 * 
	 * @param rowList
	 */
	private void setIdsList(List<Object> rowList) {
		if (!rowList.isEmpty()) {
			ids = new Serializable[rowList.size()];
			int count = 0;
			for (Object objects : rowList) {
				ids[count++] = (Serializable) objects;
			}
		}
	}

	/**
	 * Method to initialize the ID values.
	 * It constructs the select query by placing the 
	 * parent Id's list in IN cluase of the query and
	 * inializes the IDs of calling delete entitry wapper.
	 * if Parent's IDs are null, then it retuns null.
	 * 
	 * @
	 */
	private void retrieveValues()  {
		idsRetrived = true;
		// make sure parents ids are already fetched
		if (parent.ids == null)
			parent.ids = parent.getIdList();
		// still parent.ids is null i.e. there are no parent records exists.
		if (parent.ids == null) {
			return;
		}
		String[] columnNames = persister.getRootTableKeyColumnNames();
		Type[] keyColumnTypes = new Type[columnNames.length];
		StringBuilder sqlBuilder = new StringBuilder("SELECT ");

		for (int i = 0; i < columnNames.length; i++) {
			if (columnNames[i] != null) {
				sqlBuilder.append(columnNames[i]).append(", ");
				keyColumnTypes[i] = persister.getPropertyType(persister
						.getIdentifierPropertyName());
			}
		}
		sqlBuilder.delete(sqlBuilder.length() - 2, sqlBuilder.length());
		sqlBuilder.append(" from ").append(tableName).append(" ");
		sqlBuilder.append(" where ").append(associationColumnName);
		sqlBuilder.append(" in (");

		for (Serializable id : parent.ids) {
			sqlBuilder.append(id + ",");
		}
		sqlBuilder.delete(sqlBuilder.length() - 1, sqlBuilder.length());
		sqlBuilder.append(")");

		try {
			SQLQuery query = Test.currentSession()
					.createSQLQuery(sqlBuilder.toString());
			for (int i = 0; i < columnNames.length; i++) {
				query.addScalar(columnNames[i], keyColumnTypes[i]);
			}
			setIdsList((List<Object>) query.list());
		} catch (RuntimeException exc) {
			throw exc;
		}
	}

	/**
	 * This Method holds the logic to delete or null setting
	 * the records.  This method is called recursivly on each 
	 * child wrapper. Update or delete queries are constructed 
	 * and fired from the last rechable child.
	 *  
	 * @param deleteEntityWrapper
	 * @
	 */
	private void runDeleteQuery(DeleteEntityWrapper deleteEntityWrapper)
			 {
		Serializable[] ids;
		Session currentSession = persister.getFactory().getCurrentSession();
		if (deleteEntityWrapper.hasChildEntities()) {
			for (DeleteEntityWrapper childWrapper : deleteEntityWrapper
					.getChildEntities()) {
				/**
				 * In case self joining one-to-many mapping, this is
				 * the Condition to break the self recursive calls to 
				 * avoid the infinte recursive calling.
				 * Note: one level of self joining is allowed.
				 */
				if (deleteEntityWrapper.parent == null
						|| !childWrapper.tableName
								.equals(deleteEntityWrapper.parent.tableName))
					runDeleteQuery(childWrapper);
			}
		}
		// Nullable collection handling.
		if (deleteEntityWrapper.nullableCollectionMap.size() != 0) {
			ids = deleteEntityWrapper.getIdList();
			if (ids != null) {
				Set<String> keys = deleteEntityWrapper.nullableCollectionMap
						.keySet();
				for (String key : keys) {
					QueryableCollection fkCollection = deleteEntityWrapper.nullableCollectionMap
							.get(key);

					AbstractEntityPersister fkPersister = (AbstractEntityPersister) fkCollection
							.getElementPersister();
					String identifierProperty = fkCollection
							.getKeyColumnNames()[0];
					StringBuilder nullSettingQuery = new StringBuilder();
					nullSettingQuery.append("update "
							+ fkPersister.getTableName() + " set "
							+ identifierProperty + "=null where "
							+ identifierProperty + " in (");

					for (Serializable id : ids) {
						nullSettingQuery.append(id + ",");
					}
					nullSettingQuery.delete(nullSettingQuery.length() - 1,
							nullSettingQuery.length());
					nullSettingQuery.append(")");
					try {
						//logger.debug("Null setting ID's in "+fkPersister.getTableName());
						SQLQuery sqlQuery = Test.currentSession()
								.createSQLQuery(nullSettingQuery.toString());
						sqlQuery.executeUpdate();
					} catch (GenericJDBCException e) {
						throw e;
					}
				}
			}
		}

		// delete all reachable collection.
		StringBuilder query = new StringBuilder();
		String keyColumn = "";
		if (deleteEntityWrapper.parent != null) {
			ids = deleteEntityWrapper.parent.getIdList();
			keyColumn = deleteEntityWrapper.associationColumnName;
		} else {
			/* this means,we reached to the top. */
			ids = deleteEntityWrapper.getIdList();
			keyColumn = deleteEntityWrapper.persister.getKeyColumnNames()[0];
		}

		// ids==null means there are no records to delete
		if (ids == null)
			return;
		query.append("delete from ").append(
				deleteEntityWrapper.tableName + " where ").append(
				keyColumn + " in (");

		int idCount = 0;
		for (Serializable id : ids) {
			query.append(id + ",");
			// Break inClause into 1000 values, as Oracle doesn't take more than
			// 1K in In clause
			if (idCount != 0 && idCount % 1000 == 0) {
				query.deleteCharAt(query.length() - 1);
				query.append(") or " + keyColumn + " in (");
			}
			idCount++;
		}
		query.delete(query.length() - 1, query.length());
		query.append(")");
		try {
			// delete the collection
			//logger.debug("deleting records from "+deleteEntityWrapper.tableName);
			SQLQuery sqlQuery = Test.currentSession().createSQLQuery(query.toString());
			sqlQuery.executeUpdate();

		} catch (GenericJDBCException e) {
			throw e;
		}
	}
}