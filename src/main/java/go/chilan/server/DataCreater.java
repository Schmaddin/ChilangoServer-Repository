package go.chilan.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.data.JsonHelper;
import com.graphhopper.chilango.data.Route;
import com.graphhopper.chilango.data.database.RankingModel;

public class DataCreater {
	public static void writeHighscores(List<RankingModel> highscore,List<RankingModel> reviser,List<RankingModel> creator){
		writeHighscore(highscore,"highscore.html","Highscore","alltime.png");
		writeHighscore(reviser,"reviser.html","Best Reviser","reviser.png");
		writeHighscore(creator,"creator.html","Best Informationcreators","creator.png");
	}
	
	private static void writeHighscore(List<RankingModel> ranking,String path,String name,String image){
		List<String> output=new LinkedList<>();
		output.add("<html>");
		output.add("<head> "+ "<title>Highscore</title> <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
				+ "<style> img {display:block; margin-left:auto; margin-right:auto } tr {color: #e73886;} td { color: #ffffff; padding-left: .5em; padding-right: .5em;  padding-top: .9em; padding-bottom: .9em; }  h2 {color: #e73886;}   h3 {color: #e73886;})</style></head>");
		output.add("<body><h2>"+name+"</h2>");
		output.add("<image src=\""+image+"\"/>");
		output.add("<table style=\"width:90%\"><tr> <th>Position</th> <th>Name</th>  <th>Points</th> </tr>");

		for(RankingModel rank:ranking)
			output.add("<tr> <td>"+rank.getPlace()+"</td><td>"+rank.getId()+"</td><td>"+rank.getPoints()+"</td></tr>");
			output.add("</table></body></html>");
		try {
			Files.write(Paths.get((new File(App.getBaseFolderForCreation(),path)).getAbsolutePath()), output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void writeRoutes(TreeMap<Integer,Route> routes){
		FileHelper.writeObject(new File(App.getBaseFolderForCreation(),"routes.dat"), routes);
		
		File routesFolder=new File(App.getBaseFolderForCreation(),"routes");
		if(!routesFolder.exists() || !routesFolder.isDirectory())
			routesFolder.mkdir();
		
		for(Integer i:routes.keySet()){
			JsonHelper.writeJsonToFile(JsonHelper.createJsonFromObject(routes.get(i)), new File(routesFolder,i+"-"+routes.get(i).getFrom()+"-"+routes.get(i).getTo())+".json");
		}
	}
}
