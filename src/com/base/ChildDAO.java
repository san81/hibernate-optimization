package com.base;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.criterion.Example;

/**
 * Data access object (DAO) for domain model class Child.
 * @see com.base.Child
 * @author MyEclipse - Hibernate Tools
 */
public class ChildDAO extends BaseHibernateDAO {

    private static final Log log = LogFactory.getLog(ChildDAO.class);

	//property constants
	public static final String CHILD_NAME = "childName";

    
    public void save(Child transientInstance) {
        log.debug("saving Child instance");
        try {
            getSession().save(transientInstance);
            log.debug("save successful");
        } catch (RuntimeException re) {
            log.error("save failed", re);
            throw re;
        }
    }
    
	public void delete(Child persistentInstance) {
        log.debug("deleting Child instance");
        try {
            getSession().delete(persistentInstance);
            log.debug("delete successful");
        } catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }
    
    public Child findById( java.lang.Integer id) {
        log.debug("getting Child instance with id: " + id);
        try {
            Child instance = (Child) getSession()
                    .get("com.base.Child", id);
            return instance;
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }
    
    
    public List findByExample(Child instance) {
        log.debug("finding Child instance by example");
        try {
            List results = getSession()
                    .createCriteria("com.base.Child")
                    .add(Example.create(instance))
            .list();
            log.debug("find by example successful, result size: " + results.size());
            return results;
        } catch (RuntimeException re) {
            log.error("find by example failed", re);
            throw re;
        }
    }    
    
    public List findByProperty(String propertyName, Object value) {
      log.debug("finding Child instance with property: " + propertyName
            + ", value: " + value);
      try {
         String queryString = "from Child as model where model." 
         						+ propertyName + "= ?";
         Query queryObject = getSession().createQuery(queryString);
		 queryObject.setParameter(0, value);
		 return queryObject.list();
      } catch (RuntimeException re) {
         log.error("find by property name failed", re);
         throw re;
      }
	}

	public List findByChildName(Object childName) {
		return findByProperty(CHILD_NAME, childName);
	}
	
    public Child merge(Child detachedInstance) {
        log.debug("merging Child instance");
        try {
            Child result = (Child) getSession()
                    .merge(detachedInstance);
            log.debug("merge successful");
            return result;
        } catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public void attachDirty(Child instance) {
        log.debug("attaching dirty Child instance");
        try {
            getSession().saveOrUpdate(instance);
            log.debug("attach successful");
        } catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }
    
    public void attachClean(Child instance) {
        log.debug("attaching clean Child instance");
        try {
            getSession().lock(instance, LockMode.NONE);
            log.debug("attach successful");
        } catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }
}