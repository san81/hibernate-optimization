package com.base.util;



import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.base.Child;
import com.base.HibernateSessionFactory;
import com.base.Parent;

public class CopyOfTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Session ses = HibernateSessionFactory.getSession();
		Transaction tr = ses.beginTransaction();
//		Parent p = new Parent();
//		p.setSno(1);
//		p.setName("first");
//		Child c = new Child(); c.setSno(1);
//		c.setChildName("chidl name");
//		c.setParent(p);
//		p.getChilds().add(c);
//			ses.save(p);
		Parent p  = (Parent)ses.get(Parent.class, 2);
		p.setName("copy of test2");
//		Parent pNew = new Parent();
//		pNew.setSno(3);
//		pNew.setName("new parent");
//		ses.save(pNew);
//		Set<Child> childs = p.getChilds();
//		for(Child child:childs)
//		{
//			System.out.println(child.getChildName());
//			//child.setParent(null);
//			//p.getChilds().remove(child);
//		}
		
		
		tr.commit();
		ses.close();
	}

}
