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
            if (doc.containsKey(word)) // facem produsele scalare doar pentru elementele ce exista in ambele documente
            {
                atLeastOneWordInCommon = true;
                tfIdf1 = doc.get(word);
                tfIdf2 = queryDoc.get(word);
                dotProduct += Math.abs(tfIdf1 * tfIdf2);
                sumSquaresD1 += tfIdf1 * tfIdf1;
                sumSquaresD2 += tfIdf2 * tfIdf2;
            }
        }

        if (!atLeastOneWordInCommon || dotProduct == 0)
        {
            return 0;
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
        // impartim interogarea in cuvinte, dupa spatii
        String[] splitQuery = query.split("\\s+");
        ArrayList<String> queryWords = new ArrayList<>();

        int i = 0;
        while (i <= splitQuery.length - 1)
        {
            // ordinea fireasca este: operand OPERATOR operand OPERATOR ...
            String word = splitQuery[i];

            // mai intai, verificam daca este exceptie
            if (DirectIndex.exceptions.contains(word))
            {
                // il adaugam asa cum este
                queryWords.add(word); ++i;
            }
            // apoi daca este stopword
            else if (DirectIndex.stopwords.contains(word))
            {
                // ignoram cuvantul de tot
                ++i;
            }
            else // cuvant de dictionar
            {
                // se foloseste algoritmul Porter pentru stemming
                PorterStemmer stemmer = new PorterStemmer();
                stemmer.add(word.toCharArray(), word.length());
                stemmer.stem();
                word = stemmer.toString();

                queryWords.add(word); ++i;
            }
        }

        // transformam interogarea in vector
        TreeMap<String, Double> queryVector = new TreeMap<>();
        for (String word : queryWords)
        {
            queryVector.put(word, getTfQuery(word, queryWords) * MongoDatabaseClient.getIDF(word));
        }

        // calculam similaritatile cosinus pentru toate documentele existente
        HashMap<String, Double> similarities = new HashMap<>();
        for (String document: documentVectors.keySet())
        {
            // calculam similaritatea cosinus intre documentul curent si interogarea utilizatorului
            double similarity = cosineSimilarity(documentVectors.get(document), queryVector);
            if (similarity != 0)
            {
                // luam in calcul doar documentele in care exista cel putin un cuvant al utilizatorului
                similarities.put(document, similarity);
            }
        }

        // sortam documentele descrescator d.p.d.v. a similaritatii cosinus
        return entriesSortedByValues(similarities);
    }
}
