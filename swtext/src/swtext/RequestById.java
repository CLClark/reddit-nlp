package swtext;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import org.bson.Document;
import org.mongojack.JacksonDBCollection;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import swtext.localMongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;

import de.undercouch.bson4jackson.BsonFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;

import net.dean.jraw.*;
import net.dean.jraw.http.*;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.*;
import net.dean.jraw.paginators.CommentStream;
import net.dean.jraw.paginators.RedditIterable;
import net.dean.jraw.paginators.Sorting.*;
import net.dean.jraw.paginators.TimePeriod;

import static com.mongodb.client.model.Filters.eq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestById {
	
	static String userAgentString = "java:starwars-nlp:v0.0.1 (by /u/AngelKitty47)";
	
	public static String exeCute(String submissId, CommentSort sortType) {
		if(submissId.isEmpty()){return "no link provided";}		
				
		//Mongo connection initialize
		localMongo newMongo = new localMongo();
		MongoCollection<org.bson.Document> collectMongo = newMongo.initMongo("nlpdb","sw");
System.out.println(collectMongo.count() + ": docs in collection");
		//JacksonDBCollection<org.bson.Document> collect = JacksonDBCollection.wrap(collectMongo, org.bson.Document.class);
System.out.println(submissId);
		//init reddit api
		UserAgent userAgent = UserAgent.of(userAgentString);
		Secrets getApi = new Secrets();		
		Credentials credentials = Credentials.script(
				getApi.get(2), getApi.get(3),
				getApi.get(0), getApi.get(1));
		// Authenticate and get a RedditClient instance
		RedditClient reddit = new RedditClient(userAgent);		
		OAuthHelper oHelper = reddit.getOAuthHelper();
		try {
			OAuthData easyPeasy = oHelper.easyAuth(credentials);	
			reddit.authenticate(easyPeasy);
			System.out.printf("rate limiter: " + reddit.isAdjustingRatelimit());			
		} catch (NetworkException e) {
		System.out.println(credentials.getClientId());
		e.printStackTrace();}
		catch (OAuthException e) { e.printStackTrace();}				
System.out.println(reddit.isAuthenticated() + " : API Authenticated");
/////////////////////////////////////////////////////////////////////////////////////////		
		//config the submission request 

//		String subId = "7lolan"; //id of page		
//		String subId = "7lifxa"; //id of page
		String subId = submissId;
		
		SubmissionRequest.Builder reqBuilder = new SubmissionRequest.Builder(subId);
		reqBuilder
			.sort(sortType)
			.depth(new Integer(0))
		;		
		SubmissionRequest linkSpecial = reqBuilder.build();
		//send the request (for submission page)
		Submission linkResponse  = reddit.getSubmission(linkSpecial);
				
		int reqLimit = 30; // max requests for "more comments" : increase if needed
		int requests = 0;
		int sizeThen =0;
		int sizeNow = 0;
		
		CommentNode initNodes = linkResponse.getComments();
System.out.println(linkResponse.getCommentCount()+": submission comments");
		do{			
			sizeThen = initNodes.getTotalSize();
System.out.printf(sizeThen + ": Total size |");
			System.out.println(linkResponse.getComments().getTotalSize()+ ": PRE load fully ()");
			linkResponse.getComments().loadFully(reddit, 15, 20);		
			sizeNow = linkResponse.getComments().getTotalSize();
			requests++;
System.out.println(requests + " : requests loop");
System.out.println(sizeNow + " : Now size |");
		}
		while( requests <= reqLimit && sizeNow > sizeThen );
/////////////////////////////////////////////////////////////////////////////////////////
		//convert  comments to an array
		FluentIterable<CommentNode> linkComments = linkResponse.getComments().walkTree();		
		CommentNode[] comArray = linkComments.toArray(CommentNode.class);	
System.out.println(comArray.length);
		
		//get ids and check if they already exist in mongo		
		ArrayList<String> existingIds = new ArrayList<String>();
		ArrayList<String> gottenIds = new ArrayList<String>();
		//get existing docs
		MongoIterable<Document> verifyDocs = collectMongo.find();	
		Block<Document> idGetter = (Document d) -> {
			existingIds.add(d.getString("id"));
		};
		verifyDocs.forEach(idGetter);
		String idsNow = "";
		if(!existingIds.isEmpty()){idsNow = existingIds.get(0);}
System.out.println(existingIds.size()+ " : existing ids, ex: " +  idsNow);

		//iterate over each comment
		for (int i = 0; i < comArray.length; i++) {

			CommentNode thisNode = comArray[i];
			Comment thisCom = thisNode.getComment();	
			//add id to list of ids
			String comId = thisCom.getId();
			//check if this.id exists in mongo already
			int existInd = existingIds.indexOf(comId);
			if(	existInd == -1	){
			//convert it to BSON
				//create POJO from Comment.JsonNode
				JsonNode jigglyPuff = thisCom.getDataNode();
				//serialize the JsonNode w/ bson factory
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectMapper mapper = new ObjectMapper(new BsonFactory());
				try {
					mapper.writeValue(baos, jigglyPuff);
				} catch (IOException e1) {e1.printStackTrace();}
				//deserialize json data
				ByteArrayInputStream bais = new ByteArrayInputStream(	baos.toByteArray()	);
				//declare mongo object
				Document bsonJiggly;
				try {
				//build mongo object
				bsonJiggly = mapper.readValue(bais, Document.class);			
				assert jigglyPuff.findValue("id").equals(bsonJiggly.get("id"));
				
				//add it to mongodb
				//Date now = new Date();
				//BasicDBObject serverTime = new BasicDBObject("date", now);
				//Create mongo doc and insert the raw html
				/*
				bsonJiggly        	
		        			.append("uri", hyperlinkIterator(pageCursor))
		                    .append("datePulled", now.toString())
		                    .append("dboDate", serverTime.get("date"))
//		                    .append("searchCategory", chosenCat())
		                    .append("docType", "SEARCH")
		                    .append("rawHTML",singleReq.toString())
		                    .append("storeSelected", storeNumPicked)
		                    .append("headers", headersMongoMap)
		                    ;
				*/
				collectMongo.insertOne(bsonJiggly);								
				//Document verifyOne = collectMongo.find(new org.bson.Document()).filter(	eq(	"id"	,	bsonJiggly.get("id")	)	).first();
	//System.out.printf(verifyOne.getString("name"));
				
				} catch (IOException e) {	e.printStackTrace();}

			/////////////////////////////////////////////////////////////////////
			}else{
				existingIds.remove(existInd);
			}//else "found"
			
			//////////////////////////////
			/*
				String bodyOf = thisCom.getBody();
				
				thisCom.getAuthor();
				thisCom.getCreated();					
				thisCom.getScore();
				thisCom.isControversial();
				thisNode.getSubmissionName();
				
				thisCom.getId();
				thisCom.getFullName();
				if(thisCom.hasBeenEdited()){
					thisCom.getEditDate();	
				}			
			String pareId = "";
			try {
				pareId = thisNode.getParent().getComment().getId();
			} catch (NullPointerException e) {System.out.println("no parent node");			}
			*/
			////////////////////////////////////////////////////////////////////////
		}//for loop	
//System.out.println("\n" + collectMongo.count() + ": count in collect");
		
	return ("\n" + collectMongo.count() + ": count in collect |" + newMongo.closeMongo());	
	}//exeCute
	
	
	public void notes(){
		/*
		// Create our credentials
		Credentials credentials = Credentials.script("<username>", "<password>",
		    "<client ID>", "<client secret>");

		// This is what really sends HTTP requests
		NetworkAdapter adapter = new OkHttpNetworkAdapter(userAgent);

		// Authenticate and get a RedditClient instance
		RedditClient reddit = OAuthHelper.automatic(adapter, credentials);
		*/
	}

}





























