

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDatabaseClient extends MongoClient{
	private static MongoDatabaseClient instance = new MongoDatabaseClient();
	private static DB databaseRiw;
	private static DBCollection collectionDirectIndex;
	private static DBCollection collectionIndirectIndex;
	private static DBCollection collectionTF;
	private static DBCollection collectionIDF;
	private static DBCollection collectionDocumentsVector;
	private static int directIndexCount = 0;
	private static int indirectIndexCount = 0;
	private static int idfIndexCount = 0;
	private static int documentsVectorCount = 0;
	private static DBCursor directIndexInstance;
	private static String lastReadDirectIndexFilename;
	
	private MongoDatabaseClient()
	{
		super(new MongoClientURI("mongodb://localhost:27017"));
		databaseRiw = getDB("riw");
		collectionDirectIndex = databaseRiw.getCollection("DirectIndex");
		collectionIndirectIndex = databaseRiw.getCollection("IndirectIndex");
		collectionTF = databaseRiw.getCollection("TF");
		collectionIDF = databaseRiw.getCollection("IDF");
		collectionDocumentsVector = databaseRiw.getCollection("DocumentsVector");
	}
	
	public static boolean isCollectionDirectIndexEmpty()
	{
		DBCursor cursor = collectionDirectIndex.find();
		return cursor.hasNext();
	}
	public static boolean isCollectionIndirectIndexEmpty()
	{
		DBCursor cursor = collectionIndirectIndex.find();
		return (cursor.hasNext() == false);
	}
	public static boolean isCollectionTFEmpty()
	{
		DBCursor cursor = collectionTF.find();
		return (cursor.hasNext() == false);
	}
	public static boolean isCollectionIDFEmpty()
	{
		DBCursor cursor = collectionIDF.find();
		return (cursor.hasNext() == false);
	}
	public static boolean isCollectionDocumentsVectorEmpty()
	{
		DBCursor cursor = collectionDocumentsVector.find();
		return (cursor.hasNext() == false);
	}
	
	public static void emptyCollectionDirectIndex()
	{
		BasicDBObject doc = new BasicDBObject();
		collectionDirectIndex.remove(doc);
	}
	public static void emptyCollectionIndirectIndex()
	{
		BasicDBObject doc = new BasicDBObject();
		collectionIndirectIndex.remove(doc);
	}
	public static void emptyCollectionTF()
	{
		BasicDBObject doc = new BasicDBObject();
		collectionTF.remove(doc);
	}
	public static void emptyCollectionIDF()
	{
		BasicDBObject doc = new BasicDBObject();
		collectionIDF.remove(doc);
	}
	public static void emptyCollectionDocumentsVector()
	{
		BasicDBObject doc = new BasicDBObject();
		collectionDocumentsVector.remove(doc);
	}
	public static MongoDatabaseClient getMongoDatabaseClient()
	{
		return instance;
	}
	public static int getNrOfDirectIndexedFiles()
	{
		//System.out.println((int)collectionDirectIndex.count());
		return (int) collectionDirectIndex.count();
	}
	public static String getFilename()
	{
		return lastReadDirectIndexFilename;
	}
	public static void insertInDirectIndex(String filename,HashMap<String, Integer> wordList)
	{
		BasicDBObject doc = new BasicDBObject();
		doc.put("_id",directIndexCount++);
		doc.put("_filename",filename);
		
		BasicDBObject subDoc = new BasicDBObject(wordList);		
		doc.put("_direct_index",subDoc);
		collectionDirectIndex.insert(doc);
	}
	public static HashMap<String, Integer>getColectionInstance(int instanceID)
	{
		BasicDBObject query = new BasicDBObject("_id",instanceID);
		directIndexInstance = collectionDirectIndex.find(query);
		Iterator<DBObject>directIndexInstanceIterator = directIndexInstance.iterator();
		DBObject obj = directIndexInstanceIterator.next();
		lastReadDirectIndexFilename = (String) obj.get("_filename");
		return (HashMap<String, Integer>) obj.get("_direct_index");
	}
	public static void insertIntoIndirectIndex(TreeMap<String, HashMap<String, Integer>> indirectIndex)
	{
		for(Entry<String, HashMap<String, Integer>> entry:indirectIndex.entrySet())
		{
			BasicDBObject doc = new BasicDBObject("_id",indirectIndexCount++);
			doc.put("_word",entry.getKey());
			doc.put("_indirect_index",new BasicDBObject(entry.getValue()));
			collectionIndirectIndex.insert(doc);
		}
	}
	public static TreeMap<String, HashMap<String, Integer>> getIndirectIndex()
	{
		TreeMap<String, HashMap<String, Integer>> doc = new TreeMap<String, HashMap<String, Integer>>();
		
		DBCursor instance = collectionIndirectIndex.find();
		
		while(instance.hasNext())
		{
			DBObject obj = instance.next();
			doc.put((String)obj.get("_word"), (HashMap<String, Integer>)obj.get("_indirect_index"));
		}
		return doc;
	}
	
	public static void insertInTF(String filename,HashMap<String, Double> tfList)
	{
		BasicDBObject doc = new BasicDBObject();
		doc.put("_id",directIndexCount-1);
		doc.put("_filename",filename);
		
		BasicDBObject subDoc = new BasicDBObject(tfList);		
		doc.put("_tf",subDoc);
		
		collectionTF.insert(doc);
	}
	public static void insertIntoIDF(HashMap<String, Double> idfList)
	{
		for(Entry<String, Double> entry:idfList.entrySet())
		{
			BasicDBObject doc = new BasicDBObject("_id",idfIndexCount++);
			doc.put("_word",entry.getKey());
			doc.put("_idf",entry.getValue());
			collectionIDF.insert(doc);
		}
	}
	public static TFInstance getTFInstance(int instanceID)
	{
		TFInstance instance = new TFInstance();
		BasicDBObject query = new BasicDBObject("_id",instanceID);
		DBCursor tfInstance = collectionTF.find(query);
		Iterator<DBObject>directIndexInstanceIterator = tfInstance.iterator();
		DBObject obj = directIndexInstanceIterator.next();
		instance.filename = (String) obj.get("_filename");
		instance.tf = (HashMap<String, Double>) obj.get("_tf");
		return instance;
	}
	public static HashMap<String, Double> getIDF()
	{
		DBCursor instance = collectionIDF.find();
		HashMap<String, Double> idf=new HashMap<String, Double>();
		while(instance.hasNext())
		{
			DBObject obj = instance.next();
			idf.put((String)obj.get("_word"),(Double)obj.get("_idf"));
		}
		return idf;
	}
	public static double getIDF(String word)
	{
		BasicDBObject query = new BasicDBObject("_word",word);
		DBCursor instance = collectionIDF.find(query);
		return (double)instance.next().get("_idf");
	}
	public static void insertIntoDocumentVectorCollection(HashMap<String, TreeMap<String, Double>> documentVectors) 
	{
		for(Entry<String, TreeMap<String, Double>> entry:documentVectors.entrySet())
		{
			BasicDBObject doc = new BasicDBObject("_id",documentsVectorCount++);
			doc.put("_filename",entry.getKey());
			doc.put("_vector_search_data",entry.getValue());
			collectionDocumentsVector.insert(doc);
		}
	}
	public static HashMap<String, HashMap<String, Double>> getVectorsDocuments()
	{
		HashMap<String, HashMap<String, Double>> doc = new HashMap<String, HashMap<String, Double>>();
		
		DBCursor instance = collectionDocumentsVector.find();
		
		while(instance.hasNext())
		{
			DBObject obj = instance.next();
			System.out.println(obj.get("_vector_search_data"));
			doc.put((String)obj.get("_filename"), (HashMap<String, Double>)obj.get("_vector_search_data"));
		}
		return doc;
	}
}
