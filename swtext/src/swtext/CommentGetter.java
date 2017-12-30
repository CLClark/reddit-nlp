package swtext;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bson.Document;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import com.mongodb.util.JSON;

public class CommentGetter {
	
	public static int docCount = 0;

	public static void main(String[] args) throws IOException {
		@JsonPropertyOrder({
		 	"link_id",
		 	"parent_id",
		 	"body",
		 	"author",
		 	"created_utc",
		 	"permalink",
		 	"id",
		 	"score",
		 	"depth",
		 	"json"		 	
		 }) class DocForm {

		 	String link_id;
		 	String parent_id;
		 	String body;
		 	String author;
		 	String created_utc;
		 	String permalink;
		 	String id;
		 	String score;
		 	String json;
		 	
		 	private String getLink_id() {
		 		return link_id;
		 	}
		 	private void setLink_id(String okay) {
		 		this.link_id = okay;
		 	}
		 	private String getParent_id() {
		 		return parent_id;
		 	}
		 	private void setParent_id(String parent_id) {
		 		this.parent_id = parent_id;
		 	}
		 	private String getBody() {
		 		return body;
		 	}
		 	private void setBody(String body) {
		 		this.body = body;
		 	}
		 	private String getAuthor() {
		 		return author;
		 	}
		 	private void setAuthor(String author) {
		 		this.author = author;
		 	}
		 	private String getCreated_utc() {
		 		return created_utc;
		 	}
		 	private void setCreated_utc(String created_utc) {
		 		this.created_utc = created_utc;
		 	}
		 	private String getPermalink() {
		 		return permalink;
		 	}
		 	private void setPermalink(String permalink) {
		 		this.permalink = permalink;
		 	}
		 	private String getId() {
		 		return id;
		 	}
		 	private void setId(String id) {
		 		this.id = id;
		 	}
		 	private String getScore() {
		 		return score;
		 	}
		 	private void setScore(int okay) {
		 		this.score = new Integer(okay).toString();
		 	}
		 	private String getJson() {
		 		return json;
		 	}
		 	private void setJson(String json) {
		 		this.json = json;
		 	}
		 			
		 	public  DocForm(){
		 		//constructor
		 	}
		 	public void append(String st, Object okay){

		 		switch (st) {
		 	            case "link_id":  setLink_id((String) okay);
		 	                     break;
		 	            case "parent_id":  setParent_id((String) okay);
		 	                     break;
		 	            case "body":  setBody((String) okay);
		 	                     break;
		 	            case "author":  setAuthor((String) okay);
		 	                     break;
		 	            case "created_utc":  setCreated_utc((String) okay);
		 	                     break;
		 	            case "permalink":  setPermalink((String) okay);
		 	                     break;
		 	            case "id":  setId((String) okay);
		 	                     break;
		 	            case "score":  setScore((int) okay);
		 	                     break;
		 	            case "json":  setJson((String) okay);
		 	                     break;		            
		 	        }
		 	}
		 }
		
		//Mongo connection initialize
		localMongo newMongo = new localMongo();		
		MongoCollection<Document> collectMongo = newMongo.initMongo("nlpdb","sw");
		System.out.println(collectMongo.count());
//		newMongo.dupeCollection("sw","sw-backup");		
		MongoIterable<Document> docs = collectMongo.find();		
		
		ArrayList<Document> docBuilder = new ArrayList<Document>();

		Block<Document> idGetter = (Document d) -> {
			Document docTo = new Document();		
			
			String li = d.getString("link_id");
			String par = d.getString("parent_id");
			String bod = d.getString("body");
			String auth = d.getString("author");
			String crea = new Double(d.getDouble("created_utc")).toString();
			String perm = d.getString("permalink");
			String its = d.getString("id");			
			int sco = new Integer(d.getInteger("score")).intValue();
			int dep = new Integer(d.getInteger("depth")).intValue();
			String likesSt; try {
				likesSt = d.getBoolean("likes").toString();
			} catch (NullPointerException e) {	likesSt = "0";	}
			int contro = new Integer(d.getInteger("controversiality")).intValue();
			
			//String jso = d.toJson();			
			
			docTo.append("link_id", li);
			docTo.append("parent_id", par);
			docTo.append("body", bod);
			docTo.append("author", auth);
			docTo.append("created_utc", crea);
			docTo.append("permalink", perm);
			docTo.append("id", its);
			docTo.append("score", sco);
			docTo.append("depth", dep);
			docTo.append("likes", likesSt);
			docTo.append("controversiality", contro);
			//docTo.append("json", jso);		
			
			docCount++;
//			System.out.println(docCount);	
			
			docBuilder.add(docTo);		
		};	
		docs.forEach(idGetter);
		Date now = new Date();
		String fileName = "all-docs_" + now.getTime();
			String fileP = "F:/Java/swtext/bin/" + fileName + ".txt";
			writeUniformDocs(docBuilder, fileP);		
			
//		ArrayList<Document> checkLis = docs.into(new ArrayList<Document>());
//		
//		if(docBuilder.size() == checkLis.size() ){
//			System.out.println(docBuilder.size()+"asdfadfasd");
//			for (int i = 0; i < checkLis.size(); i++) {
////				System.out.println(checkLis.get(i).toJson());
//				System.out.println(docBuilder.get(i));
//			}//			 
//		}			
	}//main method
		
	/*	
	public static void toCSV (List<DocForm> docBuilder, String filePath) throws JsonGenerationException, JsonMappingException, IOException{

		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(DocForm.class);//.withHeader();				
		//mapper.writer(schema).writeValues(fOut);
		
		 // output writer
		 ObjectWriter myObjectWriter = mapper.writer(schema);
		 //filePath
		 File tempFile = new File("rando.csv");
		 FileOutputStream tempFileOutputStream = new FileOutputStream(tempFile);
		 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(tempFileOutputStream, 1024);
		 OutputStreamWriter writerOutputStream = new OutputStreamWriter(bufferedOutputStream, "UTF-8");
		 myObjectWriter.writeValue(writerOutputStream, docBuilder);	
		
	}
	*/
	
	public static void writeUniformDocs(ArrayList<org.bson.Document> list, String filePath) {
		
		//String that will hold all key fields
		String delimChar = "█";
		String superString = "";
		Set<String> keySet = list.get(0).keySet();
		for(String s : keySet){
			superString =  superString.concat(s + delimChar);
		}
		superString = superString.concat("\n");

		//now iterate over all the docs
		for(org.bson.Document d : list){
			StringBuilder docRow = new StringBuilder();
			Iterator<String> keyIterator = keySet.iterator();
			while(keyIterator.hasNext()){
				String keyNow = keyIterator.next();
				Object foundValue = d.get(keyNow);
				String escapedValue = "";				
//				docRow = docRow.concat(foundValue + delimChar);
				docRow.append((foundValue + delimChar).replaceAll("\n", "  ◙  ◙"));
//				System.out.println(docRow.toString());
			}
			docRow.append("\n");			
			superString = superString.concat(docRow.toString());
		}		
		
//		String filePath = "F:/Java/MicroPricer/parsing/" + fileName + ".txt";
		File dataNameStore = new File(filePath);
		
		try {
			FileUtils.writeStringToFile(dataNameStore, superString, "UTF-8");
		} catch (IOException e) {			
			e.printStackTrace();
		}		
	}
	
/*	
	public  DocForm docMaker(Document dilly){
//		Document docTo = new Document();
		CommentGetter.DocForm docTo = this.new DocForm();		
		
		String li = dilly.getString("link_id");
		String par = dilly.getString("parent_id");
		String bod = dilly.getString("body");
		String auth = dilly.getString("author");
		String crea = new Double(dilly.getDouble("created_utc")).toString();
		String perm = dilly.getString("permalink");
		String its = dilly.getString("id");
		int sco = new Integer(dilly.getInteger("score")).intValue();		
		String jso = dilly.toJson();
		
		docTo.append("link_id", li);
		docTo.append("parent_id", par);
		docTo.append("body", bod);
		docTo.append("author", auth);
		docTo.append("created_utc", crea);
		docTo.append("permalink", perm);
		docTo.append("id", its);
		docTo.append("score", sco);
		docTo.append("json", jso);
		
		
		docCount++;
		System.out.println(docCount);
		
		return (docTo);				
	}//docMaker
*/	
	public static String stringMaker(Document dilly){							

		String li = dilly.getString("link_id");
		String par = dilly.getString("parent_id");
		String bod = dilly.getString("body");
		String auth = dilly.getString("author");
		String crea = dilly.getString("created_utc");
		String perm = dilly.getString("permalink");
		String its = dilly.getString("id");
		String sco = dilly.getString("score");		
		String jso = dilly.toJson();
		String[] fieldsOut = {
				li,
				par,
				bod,
				auth,
				crea,
				perm,
				its,
				sco,
				jso
		};
		StringBuilder sBuilder = new StringBuilder();
		for (int i = 0; i < fieldsOut.length; i++) {
			String string = fieldsOut[i];
			sBuilder.append(string);
			sBuilder.append("|");
		}		
		
		return (sBuilder.toString());		
	}//stringMaker	
	

}//class
