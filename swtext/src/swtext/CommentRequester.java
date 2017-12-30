package swtext;
import java.util.ArrayList;
import java.util.List;

import net.dean.jraw.models.CommentSort;
import swtext.RequestById;

public class CommentRequester {

	public static void main(String[] args) {
		
		int totalRequests = 0;
		
		CommentSort[] sortTypes = {
//				CommentSort.TOP,
				CommentSort.OLD,
//				CommentSort.CONTROVERSIAL,
//				CommentSort.NEW,
//				CommentSort.HOT,				
//				CommentSort.QA,
		};			
		Secrets getSecrets = new Secrets();	
		String[] submissionIds = getSecrets.getSubIds();
		
		for (int i = 0; i < submissionIds.length; i++) {
			String stringOfId = submissionIds[i];
			
			for (int j = 0; j < sortTypes.length; j++) {
				
				CommentSort whichSort = sortTypes[j];
				RequestById.exeCute( stringOfId, whichSort );
				totalRequests++;				

			        Thread t = new Thread(new ticker());				//instantiate ticker + wait        
			        t.start(); try {Thread.sleep((12000));} catch (InterruptedException e) {	e.printStackTrace();}	t.interrupt();				
			}//for sort types
			System.out.println(totalRequests);			
		}//for loop ids
		


	}//main method
	
	private static class 	ticker	implements Runnable {
		public void run() {
			String importantInfo = "[]";
			try { //double for loop
			for (int z = 0; z < 60; z++) {			    	
				// Pause for 1 second
				Thread.sleep(1000);
				// Print a message
				System.out.print(importantInfo);
			}
			System.out.println("60 seconds waiting...");
			} catch (InterruptedException e) {				System.out.println("[tock!]");				}
		}
	}
}

	

