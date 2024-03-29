package com.base;

import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.criterion.Example;

/**
 * Data access object (DAO) for domain model class Parent.
 * @see com.base.Parent
 * @author MyEclipse - Hibernate Tools
 */
public class ParentDAO extends BaseHibernateDAO {

    private static final Log log = LogFactory.getLog(ParentDAO.class);

	//property constants
	public static final String NAME = "name";

    
    public void save(Parent transientInstance) {
        log.debug("saving Parent instance");
        try {
            getSession().save(transientInstance);
            log.debug("save successful");
        } catch (RuntimeException re) {
            log.error("save failed", re);
            throw re;
        }
    }
    
	public void delete(Parent persistentInstance) {
        log.debug("deleting Parent instance");
        try {
            getSession().delete(persistentInstance);
            log.debug("delete successful");
        } catch (RuntimeException re) {
            log.error("delete failed", re);
            throw re;
        }
    }
    
    public Parent findById( java.lang.Integer id) {
        log.debug("getting Parent instance with id: " + id);
        try {
            Parent instance = (Parent) getSession()
                    .get("com.base.Parent", id);
            return instance;
        } catch (RuntimeException re) {
            log.error("get failed", re);
            throw re;
        }
    }
    
    
    public List findByExample(Parent instance) {
        log.debug("finding Parent instance by example");
        try {
            List results = getSession()
                    .createCriteria("com.base.Parent")
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
      log.debug("finding Parent instance with property: " + propertyName
            + ", value: " + value);
      try {
         String queryString = "from Parent as model where model." 
         						+ propertyName + "= ?";
         Query queryObject = getSession().createQuery(queryString);
		 queryObject.setParameter(0, value);
		 return queryObject.list();
      } catch (RuntimeException re) {
         log.error("find by property name failed", re);
         throw re;
      }
	}

	public List findByName(Object name) {
		return findByProperty(NAME, name);
	}
	
    public Parent merge(Parent detachedInstance) {
        log.debug("merging Parent instance");
        try {
            Parent result = (Parent) getSession()
                    .merge(detachedInstance);
            log.debug("merge successful");
            return result;
        } catch (RuntimeException re) {
            log.error("merge failed", re);
            throw re;
        }
    }

    public void attachDirty(Parent instance) {
        log.debug("attaching dirty Parent instance");
        try {
            getSession().saveOrUpdate(instance);
            log.debug("attach successful");
        } catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }
    
    public void attachClean(Parent instance) {
        log.debug("attaching clean Parent instance");
        try {
            getSession().lock(instance, LockMode.NONE);
            log.debug("attach successful");
        } catch (RuntimeException re) {
            log.error("attach failed", re);
            throw re;
        }
    }
}