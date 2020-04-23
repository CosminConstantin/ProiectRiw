import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

public class IndirectIndex {
	private static TreeMap<String, HashMap<String, Integer>> indirectIndex = new TreeMap<>();
	public static void createIndirectIndex() throws IOException
	{
		int nrOfFiles = MongoDatabaseClient.getNrOfDirectIndexedFiles();
		
		//goleste colectiile indirect index si idf din mongo
        MongoDatabaseClient.emptyCollectionIndirectIndex();
        MongoDatabaseClient.emptyCollectionIDF();
        
		for(int instanceID = 0; instanceID < nrOfFiles; instanceID++)
		{
			HashMap<String,Integer> fileMap = MongoDatabaseClient.getColectionInstance(instanceID);
			String filename = MongoDatabaseClient.getFilename();
			for(Entry<String, Integer> entry:fileMap.entrySet())
			{
				String word = entry.getKey();
                int numberOfApparitions = entry.getValue();

                // adaugam intrarea in TreeMap-ul final
                if (indirectIndex.containsKey(word)) // daca acel cuvant exista in TreeMap
                {
                    // il adaugam in vectorul de aparitii
                    HashMap<String, Integer> apparitions = indirectIndex.get(word);
                    apparitions.put(filename, numberOfApparitions);
                }
                else
                {
                    HashMap<String, Integer> apparitions = new HashMap<>();
                    apparitions.put(filename, numberOfApparitions);
                    indirectIndex.put(word, apparitions);
                }
			}
		}
		//
		System.out.println("pune indirectIndex in mongo");
		MongoDatabaseClient.insertIntoIndirectIndex(indirectIndex);
		
		//Calculeaza IDF-ul
		HashMap<String, Double> idf = new HashMap<String, Double>();
		int nrOfDocuments = MongoDatabaseClient.getNrOfDirectIndexedFiles();
		for(String word:indirectIndex.keySet()) 
		{
			int nrOfDocumentsPresentIn = indirectIndex.get(word).size();
			idf.put(word, Math.log((double)nrOfDocuments / nrOfDocumentsPresentIn));
		}
		
		System.out.println("pune idf in mongo");
		MongoDatabaseClient.insertIntoIDF(idf);
	}
}
