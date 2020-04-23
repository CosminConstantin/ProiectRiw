import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class VectorSearch {
	HashMap<String, TreeMap<String, Double>> associatedVector = null;
	public static HashMap<String, TreeMap<String, Double>> createAssociatedVector() throws IOException
	{
		MongoDatabaseClient.emptyCollectionDocumentsVector();
		int nrOfDocuments = MongoDatabaseClient.getNrOfDirectIndexedFiles();
		HashMap<String, TreeMap<String, Double>> documentVectors = new HashMap<>();
		HashMap<String, Double> idfDoc = MongoDatabaseClient.getIDF();
		for(int i=0; i<nrOfDocuments; i++)
		{
			TreeMap<String, Double> currentDocumentVector = new TreeMap<>();
			TFInstance tfFile = MongoDatabaseClient.getTFInstance(i);
			
			System.out.println(i);
			
			
			for(Map.Entry<String, Double> entry:tfFile.tf.entrySet())
			{
				double tf = entry.getValue();
				double idf = idfDoc.get(entry.getKey());
				String word = entry.getKey();
				
				currentDocumentVector.put(word, tf * idf);
			}
			documentVectors.put(tfFile.filename, currentDocumentVector);
		}
		//introduc vectorul in mongo
		System.out.println("introduc vectorul in mongo");
		MongoDatabaseClient.insertIntoDocumentVectorCollection(documentVectors);
		return documentVectors;
	}
	private static double cosineSimilarity(HashMap<String, Double> doc, TreeMap<String, Double> queryDoc)
    {
        double dotProduct = 0; // produsul scalar de la numarator
        double sumSquaresD1 = 0; // sumele de patrate pentru norme
        double sumSquaresD2 = 0;
        double tfIdf1;
        double tfIdf2;

        boolean atLeastOneWordInCommon = false;
        for(String word : queryDoc.keySet())
        {
            if (doc.containsKey(word))
            {
                atLeastOneWordInCommon = true;
                tfIdf1 = doc.get(word);
                tfIdf2 = queryDoc.get(word);
                dotProduct += Math.abs(tfIdf1 * tfIdf2);
            }
            tfIdf2 = queryDoc.get(word);
            sumSquaresD2 += tfIdf2 * tfIdf2;
        }

        if (!atLeastOneWordInCommon || dotProduct == 0)
        {
            return 0;
        }
        for(String word : doc.keySet())
        {
        	tfIdf1 = doc.get(word);
        	sumSquaresD1 += tfIdf1 * tfIdf1;
        }
        return Math.abs(dotProduct) / (Math.sqrt(sumSquaresD1) * Math.sqrt(sumSquaresD2));
    }

    // pentru sortarea rezultatelor
    static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e2.getValue().compareTo(e1.getValue());
                        return res != 0 ? res : 1;
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
    
    private static double getTfQuery(String word, ArrayList<String> query)
    {
        int numberOfApparitions = 0;
        for (String w : query)
        {
            if (w.equals(word))
            {
                ++numberOfApparitions;
            }
        }
        return (double)numberOfApparitions / query.size();
    }
    
    public static SortedSet<HashMap.Entry<String, Double>> Search(String query, HashMap<String, HashMap<String, Double>> documentVectors) throws IOException
    {
        String[] splitQuery = query.split("\\s+");
        ArrayList<String> queryWords = new ArrayList<>();

        int i = 0;
        while (i <= splitQuery.length - 1)
        {
            String word = splitQuery[i];

            if (DirectIndex.exceptions.contains(word))
            {
                
                queryWords.add(word); ++i;
            }

            else if (DirectIndex.stopwords.contains(word))
            {

                ++i;
            }
            else 
            {
                PorterStemmer stemmer = new PorterStemmer();
                stemmer.add(word.toCharArray(), word.length());
                stemmer.stem();
                word = stemmer.toString();

                queryWords.add(word); ++i;
            }
        }

        TreeMap<String, Double> queryVector = new TreeMap<>();
        for (String word : queryWords)
        {
            queryVector.put(word, getTfQuery(word, queryWords) * MongoDatabaseClient.getIDF(word));
        }

        HashMap<String, Double> similarities = new HashMap<>();
        for (String document: documentVectors.keySet())
        {
            double similarity = cosineSimilarity(documentVectors.get(document), queryVector);
            if (similarity != 0)
            {
                similarities.put(document, similarity);
            }
        }

        return entriesSortedByValues(similarities);
    }
}
