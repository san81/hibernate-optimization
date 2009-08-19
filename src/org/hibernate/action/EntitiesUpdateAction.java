//$Id: EntityUpdateAction.java 11070 2007-01-20 19:16:38Z steve.ebersole@jboss.com $
package org.hibernate.action;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.CacheConcurrencyStrategy.SoftLock;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.engine.ValueInclusion;
import org.hibernate.engine.Versioning;
import org.hibernate.event.EventSource;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

public final class EntitiesUpdateAction extends EntityAction {

	private final Object[] state;
	private final Object[] previousState;	
	private int[] dirtyFields;
	private final boolean hasDirtyCollection;
	private final Object rowId;
	private Object[] cacheEntry;
//	private SoftLock lock;
	private ArrayList updates = new ArrayList();
	private EntityMetamodel entityMetamodel;
	List propsToUpdate = new ArrayList();

	public EntitiesUpdateAction(
	        final Serializable id,
	        final Object[] state,
	        final int[] dirtyProperties,
	        final boolean hasDirtyCollection,
	        final Object[] previousState,
	        final Object instance,
	        final Object rowId,
	        final EntityPersister persister,
	        final SessionImplementor session,
	        final ArrayList updates) throws HibernateException {
		super( session, id, instance, persister );
		this.state = state;
		this.previousState = previousState;		
		this.dirtyFields = dirtyProperties;
		this.hasDirtyCollection = hasDirtyCollection;
		this.rowId = rowId;
		this.updates=updates;
		this.entityMetamodel = getPersister().getEntityMetamodel();
	}

	public void execute() throws HibernateException {
		//execute has to take care abt all the Actions in the col
		Iterator updateActions = updates.iterator();
		EntityPersister persister = getPersister();		
		StringBuffer inClause = new StringBuffer("");
		Object[] values;		
				
		String[] idPropertyNames=null;
		boolean isCompositID = persister.getIdentifierType() instanceof ComponentType;
		
		if(isCompositID)
			idPropertyNames = ((ComponentType)persister.getIdentifierType()).getPropertyNames();
		else
			inClause.append(" in (");
		
		final SessionFactoryImplementor factory = getSession().getFactory();
		SessionImplementor session = getSession();
		Object previousVersion[] = new Object[updates.size()];
		
		//while block for preUpdate actions
		int i=0;
		while(updateActions.hasNext()){
			
			EntityUpdateAction entityUpdateAction = (EntityUpdateAction)updateActions.next();
			Serializable id = entityUpdateAction.getId();			 			
			Object instance = entityUpdateAction.getInstance();

			boolean veto = preUpdate(entityUpdateAction);
			
			previousVersion[i] = entityUpdateAction.getPreviousVersion();
			if ( persister.isVersionPropertyGenerated() ) {
				// we need to grab the version value from the entity, otherwise
				// we have issues with generated-version entities that may have
				// multiple actions queued during the same flush
				previousVersion[i] = persister.getVersion( instance, session.getEntityMode() );
			}
			
			
			if ( !veto ) {
				//we call update together
				//persister.update(...);
				//add this id into IN clause
				if(isCompositID) {					
					values = ((ComponentType)persister.getIdentifierType()).getPropertyValues(id,getSession());					
					inClause.append("(");
					for(int idIndex=0;idIndex<idPropertyNames.length;idIndex++){
						inClause.append(idPropertyNames[idIndex])
								.append("=")
								.append(values[idIndex]);								
						if(idIndex+1 != idPropertyNames.length)
							inClause.append(" and ");
					}					
					inClause.append(") or ");
				}else{
					if(i!=0 && i%1000 == 0){
						String keyColName =((AbstractEntityPersister)persister)
							.getPropertyColumnNames(persister.getIdentifierPropertyName())[0];
						inClause.deleteCharAt(inClause.length()-1);
						inClause.append(") or "+keyColName+" in ( ");
						}

						inClause.append(id+",");
					}
			}
			i++;
		}
		
		//Construction of update query start ****************************
		String inClauseStr = inClause.toString();
		if(isCompositID){
			if(inClauseStr.endsWith(" or "))
				inClauseStr = inClauseStr.substring(0,inClause.length()-4);
		}else{
			if(inClauseStr.endsWith(","))
				inClauseStr = inClauseStr.substring(0,inClause.length()-1);
			inClauseStr+=")";		
		}
		
		//execute update query		
		String[] queries = getPersister().returnUpdateQuery(getId(), state, dirtyFields, 
				hasDirtyCollection, previousState, previousVersion, getInstance(), rowId, session,propsToUpdate);
		Type[] allColTypes = persister.getPropertyTypes();
		
		
		
		for(int j=0;j<queries.length;j++){
			
			PreparedStatement updatePS;			
			final boolean useVersion = j == 0 && entityMetamodel.isVersioned();
			String query = queries[j];
			
			// includePropes to be derived based on the dynamic-update falg of this entity.
			boolean[] includeProps = new boolean[]{true};			
			try { 				 
				 int dirtyColIndex=0;
				 int psVarIndex=1;
				 if ( useVersion && Versioning.OPTIMISTIC_LOCK_VERSION == entityMetamodel.getOptimisticLockMode() ) {
						if ( checkVersion() ) {
							//insert version field, as the first item, into the dirtyFields array
							int[] newDirtyFields = new int[dirtyFields.length+1];
							//insert the first dirty field as the version column
							newDirtyFields[0]=0;
							for(int dirtyIndex=0;dirtyIndex<dirtyFields.length;dirtyIndex++)
								newDirtyFields[dirtyIndex+1]=dirtyFields[dirtyIndex];
							dirtyFields = newDirtyFields;
						}
						if(isCompositID){
							query = query.substring(0,query.indexOf("where ")+6)+inClauseStr+query.substring(query.lastIndexOf(" and "), query.length());
						}else{
//							query will be having where clause with two conditions.
							//update set version=?,x=?,y=? where ( sno=? and key2=v2) and verion=?
							int indexToReplace = queries[j].indexOf("=?", queries[j].indexOf("where"));
							query = query.substring(0,indexToReplace)+inClauseStr+query.substring(indexToReplace+2, query.length());
						}
												
				 }else{
					 if(isCompositID)
						 query = query.substring(0,query.indexOf("where ")+6)+inClauseStr;
					 else
						 query = query.substring(0,query.length()-2)+inClauseStr;
				 }
					 
				 
				 updatePS = session.getBatcher().prepareStatement( query );
				 for(;dirtyColIndex<dirtyFields.length;dirtyColIndex++){				
						allColTypes[dirtyFields[dirtyColIndex]].nullSafeSet( updatePS, state[dirtyFields[dirtyColIndex]], psVarIndex++,includeProps, session );						
					}
				 //only incase versioning is eanbled
				 if ( useVersion && Versioning.OPTIMISTIC_LOCK_VERSION == entityMetamodel.getOptimisticLockMode() ) {
						if ( checkVersion() ) {
							persister.getVersionType().nullSafeSet( updatePS, previousVersion[0], psVarIndex++, session );
						}
					}
				 
				 updatePS.execute();
			} catch (SQLException e) {
				throw new HibernateException(e);				
			}			
			
		}
		//Construction of update query end ***************************
		
		updateActions = updates.iterator();
		i=0;
		while(updateActions.hasNext()){
			SoftLock lock;
			EntityUpdateAction entityUpdateAction = (EntityUpdateAction)updateActions.next();			
			Serializable id = entityUpdateAction.getId();
			
			EntityEntry entry = getSession().getPersistenceContext().getEntry( entityUpdateAction.getInstance() );
			if ( entry == null ) {
				throw new AssertionFailure( "possible nonthreadsafe access to session" );
			}

			if ( entry.getStatus()==Status.MANAGED || persister.isVersionPropertyGenerated() ) {
				// get the updated snapshot of the entity state by cloning current state;
				// it is safe to copy in place, since by this time no-one else (should have)
				// has a reference  to the array
				TypeFactory.deepCopy(
						entityUpdateAction.getState(),
						persister.getPropertyTypes(),
						persister.getPropertyCheckability(),
						entityUpdateAction.getState(),
						session
				);
				if ( persister.hasUpdateGeneratedProperties() ) {
					// this entity defines proeprty generation, so process those generated
					// values...
					persister.processUpdateGeneratedProperties( id, entityUpdateAction.getInstance(), entityUpdateAction.getState(), session );
					if ( persister.isVersionPropertyGenerated() ) {
						entityUpdateAction.setNextVersion(Versioning.getVersion( entityUpdateAction.getState(), persister ));
					}
				}
				// have the entity entry perform post-update processing, passing it the
				// update state and the new version (if one).
				entry.postUpdate( entityUpdateAction.getInstance(), entityUpdateAction.getState(), entityUpdateAction.getNextVersion() );
			}
			final CacheKey ck;
			if ( persister.hasCache() ) {
				ck = new CacheKey( 
						id, 
						persister.getIdentifierType(), 
						persister.getRootEntityName(), 
						session.getEntityMode(), 
						session.getFactory() 
					);
				lock = persister.getCache().lock(ck, previousVersion);
			}
			else {
				ck = null;
			}

			if ( persister.hasCache() ) {
				if ( persister.isCacheInvalidationRequired() || entry.getStatus()!=Status.MANAGED ) {
					persister.getCache().evict(ck);
				}
				else {
					//TODO: inefficient if that cache is just going to ignore the updated state!
					CacheEntry ce = new CacheEntry(
							entityUpdateAction.getState(), 
							persister, 
							persister.hasUninitializedLazyProperties( entityUpdateAction.getInstance(), session.getEntityMode() ), 
							entityUpdateAction.getNextVersion(),
							getSession(),
							entityUpdateAction.getInstance()
					);
					cacheEntry[i] = persister.getCacheEntryStructure().structure(ce);
//					boolean put = persister.getCache().update(ck, cacheEntry);
					boolean put = persister.getCache().update( ck, cacheEntry, entityUpdateAction.getNextVersion(), previousVersion );
					
					if ( put && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatisticsImplementor()
								.secondLevelCachePut( getPersister().getCache().getRegionName() );
					}
				}
			}

			postUpdate(entityUpdateAction);
			i++;
		}
		
		
		if ( factory.getStatistics().isStatisticsEnabled()) {
			factory.getStatisticsImplementor()
					.updateEntity( getPersister().getEntityName() );
		}
	}

	private void postUpdate(EntityUpdateAction entityUpdateAction) {
		PostUpdateEventListener[] postListeners = getSession().getListeners()
				.getPostUpdateEventListeners();
		if (postListeners.length>0) {
			PostUpdateEvent postEvent = new PostUpdateEvent( 
					getInstance(), 
					getId(), 
					entityUpdateAction.getState(), 
					entityUpdateAction.getPreviousState(), 
					getPersister(),
					(EventSource) getSession() 
				);
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostUpdate(postEvent);
			}
		}
	}

//	private void postCommitUpdate() {
//		PostUpdateEventListener[] postListeners = getSession().getListeners()
//				.getPostCommitUpdateEventListeners();
//		if (postListeners.length>0) {
//			PostUpdateEvent postEvent = new PostUpdateEvent( 
//					getInstance(), 
//					getId(), 
//					state, 
//					previousState, 
//					getPersister(),
//					(EventSource) getSession()
//				);
//			for ( int i = 0; i < postListeners.length; i++ ) {
//				postListeners[i].onPostUpdate(postEvent);
//			}
//		}
//	}

	private boolean preUpdate(EntityUpdateAction entityUpdateActio) {
		PreUpdateEventListener[] preListeners = getSession().getListeners()
				.getPreUpdateEventListeners();
		boolean veto = false;
		if (preListeners.length>0) {
			PreUpdateEvent preEvent = new PreUpdateEvent( 
					getInstance(), 
					getId(), 
					entityUpdateActio.getState(), 
					entityUpdateActio.getPreviousState(), 
					getPersister(),
					getSession()
				);
			for ( int i = 0; i < preListeners.length; i++ ) {
				veto = preListeners[i].onPreUpdate(preEvent) || veto;
			}
		}
		return veto;
	}

	public void afterTransactionCompletion(boolean success) throws CacheException {
//		EntityPersister persister = getPersister();
//		if ( persister.hasCache() ) {
//			
//			final CacheKey ck = new CacheKey( 
//					getId(), 
//					persister.getIdentifierType(), 
//					persister.getRootEntityName(), 
//					getSession().getEntityMode(), 
//					getSession().getFactory() 
//				);
//			
//			if ( success && cacheEntry!=null /*!persister.isCacheInvalidationRequired()*/ ) {
//				boolean put = persister.getCache().afterUpdate(ck, cacheEntry, nextVersion, lock );
//				
//				if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
//					getSession().getFactory().getStatisticsImplementor()
//							.secondLevelCachePut( getPersister().getCache().getRegionName() );
//				}
//			}
//			else {
//				persister.getCache().release(ck, lock );
//			}
//		}
//		postCommitUpdate();
	}
//
	protected boolean hasPostCommitEventListeners() {
		return getSession().getListeners().getPostCommitUpdateEventListeners().length>0;
	}
	private boolean checkVersion() {
		//return true;
		int propIndex = entityMetamodel.getVersionPropertyIndex();
		boolean[] propsToUpdateBoolean = (boolean[])propsToUpdate.get(0);
		return propsToUpdateBoolean[ propIndex ] ||
		entityMetamodel.getPropertyUpdateGenerationInclusions()[ propIndex ] != ValueInclusion.NONE;

	}
}







