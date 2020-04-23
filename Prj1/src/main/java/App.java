

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.TreeMap;


/**
 * Hello world!
 *
 */
public class App 
{
	static long startTime, stopTime, elapsedTime; // pentru masurarea timpilor de executie
    public static void main( String[] args ) throws IOException
    {
    	//DirectIndex.createDirectIndex(new WebsiteInfo("./ietf.org/", "http://ietf.org/"));
    	//HashMap<String,Integer> map = MongoDatabaseClient.getColectionInstance(0);
    	//for(Entry<String, Integer> entry:map.entrySet())
    	//{
    	//	System.out.println(entry.getKey() + " " + entry.getValue());
    	//}
    	//IndirectIndex.createIndirectIndex();
    	//SortedSet<HashMap.Entry<String, Double>> set = VectorSearch.Search("html page", VectorSearch.createAssociatedVector());
    	//System.out.println(set.toString());
    	
        // pentru cautari
        String query;
        Set<String> booleanSearchResults;
        SortedSet<HashMap.Entry<String, Double>> vectorSearchResults;
        Scanner queryScanner = new Scanner(System.in);
    	
    	System.out.print("Incarcare website... ");
        WebsiteInfo websiteInfo = new WebsiteInfo("./ietf.org/", "http://ietf.org/");
        System.out.println("OK\n");
    	
    	do {
            System.out.println("1. Creare index direct + tf");
            System.out.println("2. Creare index indirect + idf");
            System.out.println("3. Cautare booleana");
            System.out.println("4. Creare vectori asociati documentelor HTML");
            System.out.println("5. Cautare vectoriala");
            System.out.println("6. Iesire");

            System.out.print("Optiunea dvs: ");
            Scanner reader = new Scanner(System.in);
            int option = reader.nextInt();
            System.out.println();

            switch (option)
            {
                case 1:
                    System.out.print("Se creeaza index-ul direct, asteptati... ");
                    startTime = System.currentTimeMillis();
                    try {
                        DirectIndex.createDirectIndex(websiteInfo);
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nEROARE: Nu se poate crea indexul direct si tf-ul.");
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " secunde)");
                    break;
                case 2:
                    System.out.print("Se creeaza index-ul indirect, asteptati... ");
                    startTime = System.currentTimeMillis();
                    try {
                        IndirectIndex.createIndirectIndex();
                    }
                    catch (IOException e)
                    {
                        System.out.println("\nEROARE: Index-ul direct nu a fost creat.");
                        break;
                    }
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("OK (" + (double)elapsedTime / 1000 + " secunde)");
                    break;
                case 3:
                    if (MongoDatabaseClient.isCollectionIndirectIndexEmpty() == true)
                    {
                        System.out.println("\nEROARE: Index-ul indirect nu este creat. Nu se poate efectua cautarea booleana!");
                        break;
                    }
                    System.out.println("Introduceti interogarea pentru cautare:");
                    query = queryScanner.nextLine();

                    System.out.print("\nSe cauta... ");
                    startTime = System.currentTimeMillis();
                    booleanSearchResults = BooleanSearch.Search(MongoDatabaseClient.getIndirectIndex(), query);
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    if (booleanSearchResults != null)
                    {
                        System.out.println("OK (" + booleanSearchResults.size() + " rezultate gasite in " + (double)elapsedTime / 1000 + " secunde)");
                        System.out.println("\nRezultatele cautarii:");
                        for (String doc : booleanSearchResults) {
                            System.out.println("\t" + doc);
                        }
                    }
                    else
                    {
                        System.out.println("niciun rezultat gasit! (" + (double)elapsedTime / 1000 + " secunde)");
                    }
                    break;
                case 4:
                    if (MongoDatabaseClient.isCollectionIndirectIndexEmpty() == true)
                    {
                        System.out.println("\nEROARE: Index-ul indirect nu este creat. Nu se pot crea vectorii asociati documentelor!");
                        break;
                    }
                    System.out.print("Se creeaza vectorii asociati documentelor HTML, asteptati... ");
                    startTime = System.currentTimeMillis();
                    VectorSearch.createAssociatedVector();
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    System.out.println("\nOK (" + (double)elapsedTime / 1000 + " secunde)");
                    break;
                case 5:
                    if (MongoDatabaseClient.isCollectionDocumentsVectorEmpty() == true)
                    {
                        System.out.println("\nEROARE: Vectorii asociati documentelor nu au fost creati. Nu se poate efectua cautarea vectoriala!");
                        break;
                    }
                    System.out.println("Introduceti interogarea pentru cautare:");
                    query = queryScanner.nextLine();

                    System.out.print("\nSe cauta... ");
                    startTime = System.currentTimeMillis();
                    vectorSearchResults = VectorSearch.Search(query, MongoDatabaseClient.getVectorsDocuments());
                    stopTime = System.currentTimeMillis();
                    elapsedTime = stopTime - startTime;
                    if (vectorSearchResults != null && !vectorSearchResults.isEmpty())
                    {
                        System.out.println("OK (" + vectorSearchResults.size() + " rezultate gasite in " + (double)elapsedTime / 1000 + " secunde)");
                        System.out.println("\nRezultatele cautarii:");
                        for (Map.Entry<String, Double> resultDoc : vectorSearchResults)
                        {
                            System.out.println("\t" + resultDoc.getKey() + " (relevanta " + (double)Math.round(resultDoc.getValue() * 100.0 * 100.0) / 100.0 + "%)");
                        }
                    }
                    else
                    {
                        System.out.println("niciun rezultat gasit! (" + (double)elapsedTime / 1000 + " secunde)");
                    }
                    break;
                case 6:
                    System.exit(0);
                default:
                    System.out.println("\nEROARE: Optiunea nu exista!");
            }

            System.out.flush();
            Runtime.getRuntime().exec("clear");
        } while (true);
    }

}
