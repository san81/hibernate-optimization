package com.base.util;

public class SerialNum {
	      // The next serial number to be assigned
	      private static int nextSerialNum = 0;
	 
	     private static ThreadLocal serialNum = new ThreadLocal() {
	          protected synchronized Object initialValue() {
	              return new Integer(nextSerialNum++);
	          }
	      };
	 
	      public static int get() {
	          return ((Integer) (serialNum.get())).intValue();
	      }
	      
	      public static void main(String ar[]){
	    	 new Thread(){
	    		 public void run(){	 
	    			 SerialNum.serialNum.set(1234);
	   	    	  	System.out.println("first thread 1"+SerialNum.get());
		   	    	 System.out.println("2 "+SerialNum.get());
		   	    	System.out.println("3 "+SerialNum.get());
			   	     
	    		 }
	    	 }.start();
	    	 
	    	 new Thread(){
	    		 public void run(){	    			
	   	    	  	System.out.println("second thread "+SerialNum.get());
	    		 }
	    	 }.start();
	    	 new Thread(){
	    		 public void run(){	    			
	   	    	  	System.out.println("second thread "+SerialNum.get());
	    		 }
	    	 }.start();
	      }
	  }
