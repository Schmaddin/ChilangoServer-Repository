package go.chilan.server;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.mail.MessagingException;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.jts.JtsAdapterFactory;
import com.github.filosganga.geogson.model.Coordinates;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.Point;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.data.database.PointModel;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.ServerConnection;
import com.graphhopper.chilango.network.ServerMessageAuth;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;
import com.vividsolutions.jts.geom.Coordinate;

public class TestConnection {
	public static void main(String[] args) throws IOException, ParseException {
		Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GeometryAdapterFactory())
				.registerTypeAdapterFactory(new JtsAdapterFactory()).create();
		
		
		
		String json = "{$push:{scores:{type:'quiz', score:99}}}";
		
		PointModel point=new PointModel(3,4,5,2,3);
		
		
		json=gson.toJson(point);
		
		System.out.println(json);
		
		BasicDBObject push = (BasicDBObject) JSON.parse(json);
		
		String search= "{  _id: ObjectId(\""+"596783ee08813b000107134e"+"\"), \"pointModel.transactionId\":"+234234+"}";
		String replace= "{ \"pointModel.$.revisorPoints\" : "+22+", \"pointModel.$.creatorPoints\":"+33+" }";
String queryString="db.users.update( "+search+", { $set: "+replace+" })";

BasicDBObject query1 = BasicDBObject.parse(search);
BasicDBObject query2 = BasicDBObject.parse(replace);

System.out.println(query1.toString());
System.out.println(query2.toString());
		


DBObject listItem = new BasicDBObject("pointModel", push);
DBObject updateQuery = new BasicDBObject("$push", listItem);
System.out.println(updateQuery.toString());
//myCol.update(findQuery, updateQuery);


	MongoClient a = MongoDB.createDatabaseConnection();
	MongoDatabase b = a.getDatabase("chilangodb");
	MongoDB.changeTransactionPoints(b, 173437805, 2, 2, "596783ee08813b000107134e");


	}
}
