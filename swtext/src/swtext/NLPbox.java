package swtext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;

import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizer;
import opennlp.tools.doccat.DocumentCategorizerEventStream;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.doccat.NGramFeatureGenerator;
import opennlp.tools.formats.ontonotes.DocumentToLineStream;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ObjectStreamUtils;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.BigramNameFeatureGenerator;
import opennlp.tools.util.featuregen.BrownBigramFeatureGenerator;
import opennlp.tools.util.featuregen.BrownTokenFeatureGenerator;

public class NLPbox {
	
	static int catCounter = 0;

	public static void main(String[] args) throws InvalidFormatException {
		
		Secrets secretGetter = new Secrets();
		String source = secretGetter.get(4) + "source.train";
		String modelPath = secretGetter.get(4) + "destination.model";
		
//		trainModel(source, modelPath);		
		
//		get the mongo data...
		try {
			Date now = new Date();
			String fileName = "cate-docs_" + now.getTime();
			String fileP = secretGetter.get(4) + fileName + ".txt";
			
			categorizeDocs(modelPath, fileP);
			
			} catch (IOException e) {			e.printStackTrace();		}		
		
	}

	private static void trainModel(String source, String destination) throws InvalidFormatException {
		
		
		SimpleTokenizer tolk = SimpleTokenizer.INSTANCE;
		
		NGramFeatureGenerator ngfg = new NGramFeatureGenerator();
		NGramFeatureGenerator tgfg = new NGramFeatureGenerator(2,3);
		BagOfWordsFeatureGenerator bowfg = new BagOfWordsFeatureGenerator();
				
		FeatureGenerator[] fgArray = {ngfg, tgfg, bowfg};
		DoccatFactory df = new DoccatFactory(fgArray);
		
		TrainingParameters tp = new TrainingParameters();		
			tp.put(TrainingParameters.CUTOFF_PARAM, "5");
			tp.put(TrainingParameters.ITERATIONS_PARAM, "250");
					
		DoccatModel model = null;
		InputStreamFactory dataIn = null;
		List<String> finalTokenized = new ArrayList<String>();
		try {
			dataIn = new MarkableFileInputStreamFactory(
					new File(source)			);
			ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, StandardCharsets.UTF_8);
			String line = lineStream.read();
			StringBuilder lineBuffer; 
			while(line != null){
				lineBuffer = new StringBuilder();
				int delim = line.indexOf("\t");
				
				//extract comment from line
				String nonCategory = line.substring((delim+1));
				int end = nonCategory.indexOf("\t");
				
				//remove quote lines
				StringBuilder cleaner = new StringBuilder(nonCategory.substring(0, end));
				Integer firstQuote = new Integer(cleaner.indexOf("&gt;"));				
				while(firstQuote.intValue() > -1 && cleaner.length()>0){
					Integer endQuote = new Integer(cleaner.indexOf("◙",firstQuote.intValue()));
					if(endQuote == -1){
						endQuote = firstQuote.intValue() + 3;
						if (endQuote > cleaner.length()){ endQuote = cleaner.length();}
					}		
					cleaner.delete(firstQuote.intValue(), endQuote.intValue());					
					firstQuote = cleaner.indexOf("&gt;");
				};				
				System.out.println(cleaner.toString());				
				
				String category = line.substring(0, delim);
				String[] tolked = tolk.tokenize(nonCategory.substring(0, end));	
				
				//concat original category and new String[]				
				lineBuffer.append(category).append("\t").append(String.join(" ", tolked));				
				line = lineStream.read(); //second reading
				finalTokenized.add(lineBuffer.toString());				
			}
			lineStream.close();
			//TODO something with lineBuffer object, like objectstream...
			ObjectStream<String> tolkenizedStream = ObjectStreamUtils.createObjectStream(finalTokenized);			
			ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(tolkenizedStream);			
			
			//train the model using the tokenized data
			model = DocumentCategorizerME.train(destination, sampleStream, tp, df);	
			File fileOut = new File(destination);
			FileOutputStream fos = new FileOutputStream(fileOut);
			model.serialize(fos);
			
		} catch(Exception e){ e.printStackTrace();}
	}
	
	private static void categorizeDocs(String modelSource, String destination) throws IOException {
		NGramFeatureGenerator ngfg = new NGramFeatureGenerator(); //customizable
		NGramFeatureGenerator tgfg = new NGramFeatureGenerator(2,3);		
		BagOfWordsFeatureGenerator bowfg = new BagOfWordsFeatureGenerator();		
		
		SimpleTokenizer tolk = SimpleTokenizer.INSTANCE;
						
		FeatureGenerator[] fgArray = {ngfg, tgfg, bowfg};
		DoccatFactory df = new DoccatFactory(fgArray);		
					
		File modelFile = new File(modelSource); 
		//TODO
		DoccatModel model = new DoccatModel(modelFile);
		DocumentCategorizer dillyCat = new DocumentCategorizerME(model);
		
		//GET THE MONGO FILE HERE... or something
		ObjectStream<DocumentSample> osds = docs2Collection();
		DocumentSample unCat = osds.read();
		ArrayList<Document> categorized = new ArrayList<Document>(); 
		while( unCat != null){
			
			//categorize the document, iterate over each			
			double[] catArray = dillyCat.categorize(unCat.getText());
			String result = Arrays.stream(catArray)
				        .mapToObj(String::valueOf)
				        .collect(Collectors.joining("|"));			
			String category = dillyCat.getBestCategory(catArray);			
			
			Map<String, Object> extraInfo = unCat.getExtraInformation();
			
			categorized.add(new Document()
					.append("category", category)
					.append("not_shill", Double.toString(catArray[0]))
					.append("shill", Double.toString(catArray[1]))					
					.append("id", extraInfo.get("id"))
					.append("link_id", extraInfo.get("link_id"))
					.append("parent_id", extraInfo.get("parent_id"))
					.append("body", extraInfo.get("body"))
					.append("author", extraInfo.get("author"))
					.append("created_utc", extraInfo.get("created_utc"))
					.append("permalink", extraInfo.get("permalink"))
					.append("id", extraInfo.get("id"))
					.append("score", extraInfo.get("score"))
					.append("depth", extraInfo.get("depth"))
					.append("likes", extraInfo.get("likes"))
					.append("controversiality", extraInfo.get("controversiality"))
					.append("probArray", dillyCat.getAllResults(catArray))
					.append("body_length", unCat.getText().length)
					);
			System.out.println(catCounter++);
			unCat = osds.read();			
		}
			writeUniformDocs(categorized, destination);	

	}
	
	private static ObjectStream<DocumentSample> docs2Collection(){
		
				//Mongo connection initialize		
				localMongo newMongo = new localMongo();		
				MongoCollection<Document> collectMongo = newMongo.initMongo("nlpdb","sw");
				MongoIterable<Document> docs = collectMongo.find();
				
				ArrayList<DocumentSample> dSamples = new ArrayList<DocumentSample>();
						
				//tokenizer
				SimpleTokenizer tolk = SimpleTokenizer.INSTANCE;
				
				Block<Document> idGetter = (Document d) -> {

					String bod = d.getString("body");
					int end = (bod.length() - 1);
					
					//remove quote lines
					StringBuilder cleaner = new StringBuilder(bod.substring(0, end));
					Integer firstQuote = new Integer(cleaner.indexOf("&gt;"));				
					while(firstQuote.intValue() > -1 && cleaner.length()>0){
						Integer endQuote = new Integer(cleaner.indexOf("◙",firstQuote.intValue()));
						if(endQuote == -1){
							endQuote = firstQuote.intValue() + 3;
							if (endQuote > cleaner.length()){ endQuote = cleaner.length();}
						}		
						cleaner.delete(firstQuote.intValue(), endQuote.intValue());					
						firstQuote = cleaner.indexOf("&gt;");
					};				
					System.out.println(cleaner.toString());	
					
					//tokenize			
					String[] tolked = tolk.tokenize(bod);	
					
					DocumentSample ds = new DocumentSample("uncategorized", tolked,d);
					dSamples.add(ds);					
				};				
				
				docs.forEach(idGetter);
				
				ObjectStream<DocumentSample> toRe = ObjectStreamUtils.createObjectStream(dSamples);				
				return toRe;
	}
	
	public static void writeUniformDocs(ArrayList<org.bson.Document> list, String filePath) {
		
		//String that will hold all key fields
//		String delimChar = "█";
		String delimChar = "\t";
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
				docRow.append((foundValue + delimChar).replaceAll("\n", "◙"));
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


	private static void brownDocs(){
		BrownTokenFeatureGenerator btfg = new BrownTokenFeatureGenerator(null);
		BrownBigramFeatureGenerator bbfg = new BrownBigramFeatureGenerator(null);
	}

}
		
		
		
		
		
		
		
		
		
		




