package ReplicatedDatabaseSystem;
/*import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;

public class Client {
	public static void main(String[] args) {


		// To directly connect to a single MongoDB server (note that this will not auto-discover the primary even
		// if it's a member of a replica set:
		try {
			//		MongoClient	mongoClient = new MongoClient();

			// or
			 // Standard URI format: mongodb://[dbuser:dbpassword@]host:port/dbname
		       
	        MongoClientURI uri  = new MongoClientURI("mongodb://user:pass@host:port/db"); 
	        
			MongoClient mongoClient = new MongoClient( "localhost" );
			// or
			//	MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
			// or, to connect to a replica set, with auto-discovery of the primary, supply a seed list of members
			//	MongoClient mongoClient = new MongoClient(Arrays.asList(new ServerAddress("localhost", 27017),
			//			new ServerAddress("localhost", 27018),
			//			new ServerAddress("localhost", 27019)));

			DB db = mongoClient.getDB( "binitds" );
			System.out.println("Database: " + db.getName());
			DBCollection songColl = db.getCollection("song");
			DBCursor cur = songColl.find();

			while(cur.hasNext()){
				System.out.println(cur.next());
			}

			BasicDBObject obj = new BasicDBObject();
			obj.put("name", "summer of 69");
			songColl.insert(obj);
			
			cur = songColl.find();

			while(cur.hasNext()){
				System.out.println(cur.next());
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
*/

/** 
 * @author Binit Shah 
 */
  
import com.mongodb.*; 
import java.net.UnknownHostException; 
  
public class LoadDatabase { 
    public static void main(String args[]) throws UnknownHostException { 
  
        System.setProperty("java.net.preferIPv4", "true"); 
  
        MongoClient mongoClient = new MongoClient( "localhost", 27017);
        DB db = mongoClient.getDB("mydb"); 
        DBCollection items = db.getCollection("items");

        for(int i=1;i<=5;i++) { 
            BasicDBObject doc = new BasicDBObject(); 
            doc.put("number",i); 
            doc.put("name",("Distributed System " + i)); 
            items.insert(doc); 
        } 
        //doc.put("task", "Write Code"); 
        //doc.put("task1", "Write Code E"); 
        //doc.put("priority", "high"); 
        //items.insert(doc); 
  
        BasicDBObject query = new BasicDBObject(); 
        //query.put("task", "Write Code E"); 
        DBCursor cursor = items.find(); 
  
        while (cursor.hasNext()) { 
            System.out.println(cursor.next()); 
        } 
  
        BasicDBObject findTestItemQuery = new BasicDBObject(); 
        findTestItemQuery.put("task", "Write Code"); 
        DBCursor testItemsCursor = items.find(findTestItemQuery); 
        if(testItemsCursor.hasNext()) { 
            DBObject testCodeItem = testItemsCursor.next(); 
            testCodeItem.put("task", "Test and Review Code"); 
            items.save(testCodeItem); 
        } 
  
        query = new BasicDBObject(); 
        //query.put("task", "Write Code E"); 
        
        /*BasicDBObject deleteQuery = new BasicDBObject(); 
        //deleteQuery.put("Priority","Highest"); 
        DBCursor cursor1 = items.find(); 
        while (cursor1.hasNext()) { 
            DBObject item = cursor1.next(); 
            items.remove(item); 
            //System.out.println(cursor.next()); 
        }*/
  
        /*cursor = items.find(); 
        while (cursor.hasNext()) { 
            System.out.println("After deletion"+cursor.next()); 
        }*/
/* 
        String uriString = "mongodb://binitds:1234@129.21.95.182:27017/DistributedDb"; 
        MongoURI uri = new MongoURI(uriString); 
  
  
  
        //MongoClient mongoClient = null; 
  
        try { 
            DB db = uri.connectDB(); 
            Set<String> colls = db.getCollectionNames(); 
  
            for(String s : colls) { 
                System.out.println("s"); 
            } 
            System.out.println("done"); 
            System.out.println("Here"); 
        } catch (UnknownHostException e) { 
                e.printStackTrace(); 
        }*/
    } 
}