package com.base.util;



import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import com.base.Child;
import com.base.Parent;
import com.base.Rad;
import com.base.RadId;

public class Test {
	 public static final SessionFactory sessionFactory;
		static {
		    try {
		      // Create the SessionFactory from hibernate.cfg.xml
		      Configuration config = new Configuration().configure();
		      sessionFactory = config.buildSessionFactory();
		    } catch (Throwable ex) {
		      // Make sure you log the exception, as it might be swallowed
		      System.err.println("Initial SessionFactory creation failed." + ex);
		      throw new ExceptionInInitializerError(ex);
		    }
		  }
		public static final ThreadLocal threadLocal = new ThreadLocal();		
		public static Session currentSession() throws HibernateException {
		    Session s = (Session) threadLocal.get();		    
		    // Open a new Session, if this thread has none yet
		    if (s == null) {
		      s = sessionFactory.openSession();
		      // Store it in the ThreadLocal variable
		      threadLocal.set(s);
		    }		    
		    return s;
		  }

		public static void main1(String ar[]){
			try{
					System.out.println("in try");
				return ;
			}finally{
				System.out.println("finally block");
			}
		}
		
	public static void main3(String ar[]){
		Session ses = currentSession();		 
		Transaction tr = ses.beginTransaction();
		ses.createQuery("update Child set comment='san1' where sno in (1,3,4)").executeUpdate();
		tr.commit();
		ses.close();
		try{
			Connection con = DriverManager.getConnection("");
			Statement st = con.createStatement();
			PreparedStatement pst = con.prepareStatement("");
			pst.setInt(1,2);
			pst.addBatch();
			
			st.addBatch("");
			st.addBatch("");
			st.executeBatch();
		}catch (Exception e) {
			// TODO: handle exception
		}
		
		
	}
	
	public static void main_0(String ar[]){
		Session ses = currentSession();		
		System.out.println("session created *********************************");
		Transaction tr = ses.beginTransaction();
		System.out.println("transaction begining *********************************");
		RadId radId = new RadId();
		radId.setKey1(new Integer(1));
		radId.setKey2(new Integer(1));
		Rad rad = (Rad)ses.load(Rad.class, radId);
		
		Rad rad1 = (Rad)ses.load(Rad.class, new RadId(1,2));
		System.out.println("Name = "+rad1.getName());
		rad.setName("first2");
		rad1.setName("first2");
		tr.commit();
		System.out.println("after comiting *********************************");
		ses.close();
	}
	public static void main(String ar[]){
		System.out.println("from the main begining *********************************");
		Session ses = currentSession();	
		System.out.println("session created *********************************");
		Transaction tr = ses.beginTransaction();
		System.out.println(" is tr active "+tr.isActive());
		System.out.println("transaction begining *********************************");
//		Child c = (Child)ses.load(Child.class, 2);
//		c.setComment("abcd1");
//		AbstractEntityPersister persistor = (AbstractEntityPersister)sessionFactory.getAllClassMetadata().get("com.base.Rad");
//		DeleteEntityWrapper delWrapper = new DeleteEntityWrapper(persistor,new Long[]{1L});
//		delWrapper.delete();
		Parent p = (Parent)ses.load(Parent.class, 2);
		Parent p4 = (Parent)ses.load(Parent.class, 3);
		Long t = System.currentTimeMillis();
		Set<Child> childs = p4.getChilds();
		
//		for(int i=1;i<50;i++){ 
//			Child c = new Child(); 
//			//c.setSno(i);
//			c.setChildName("child"+i);
//			c.setComment("comment");
//			childs.add(c);
//			
//		}
//		int i=0;
		for(Child child:childs){			
			child.setChildName("child-t");
			child.setComment("commentx");
			child.setParent(p4);
			p4.getChilds().add(child);
//			if(i++==35)
//				break;
		}

		tr.commit();
		System.out.println("after comiting *********************************");
		System.out.println("time take -- "+(System.currentTimeMillis()-t));
		ses.close();
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main2(String[] args) {
		
		Session ses = currentSession();
		Transaction tr = ses.beginTransaction();		
//		Parent p = new Parent();
//		p.setSno(2);
//		p.setName("first");
//		Child c = new Child(); c.setSno(1);
//		c.setChildName("chidl name");
//		c.setParent(p);
//		p.getChilds().add(c);
//			ses.save(p);
//			CreditCardPayment ccp = new CreditCardPayment();
//			ccp.setAmount(2000.00);
//			ccp.setCreditCardType("Gold");
//			ses.save(ccp);
		Filter filter =	ses.enableFilter("childsWithA");
		filter.setParameter("letterToStartWith", "first%");
		List<Parent> parents = ses.createQuery("from Parent").list();
		Parent p  = (Parent)parents.get(0);
		filter.setParameter("letterToStartWith", "ab%");
		Set<Child> childs = p.getChilds();
		System.out.println(childs.size());
		p = (Parent)ses.load(Parent.class, 2);
		filter.setParameter("letterToStartWith", "xy%");
		System.out.println(p.getChilds().size());
//    	Map<String, SingleTableEntityPersister> allClassMeta = sessionFactory.getAllClassMetadata();
//    	AbstractEntityPersister persister = allClassMeta.get("com.base.Parent");
//    	DeleteEntityWrapper deleteEntityWrapper = new DeleteEntityWrapper(persister,new Long[]{1L});
//    	deleteEntityWrapper.delete();
		//ses.delete(p);
		//p.setName("first updatedo from test1");
//		Set<Child> childs = p.getChilds();
//		for(Child child:childs)
//		{
//			System.out.println(child.getChildName());
//			//child.setParent(null);
//			//p.getChilds().remove(child);
//		}
		
		//p.setName("sreenu5");
		//p.setCiti("test1");
			
		tr.commit();		
//		tr = ses.beginTransaction();
//		CreditCardPayment ccpOut = (CreditCardPayment)ses.load(Payment.class, 2L);
//		System.out.println(ccpOut.getId()+" "+ccpOut.getCreditCardType());
		
		ses.close();
	}

}
