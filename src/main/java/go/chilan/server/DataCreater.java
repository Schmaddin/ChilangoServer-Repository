package go.chilan.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.data.JsonHelper;
import com.graphhopper.chilango.data.MapBoard;
import com.graphhopper.chilango.data.Route;
import com.graphhopper.chilango.data.database.RankingModel;
import com.graphhopper.chilango.data.database.SubmitType;

public class DataCreater {
	public static void writeHighscores(List<RankingModel> highscore, List<RankingModel> reviser,
			List<RankingModel> creator) {
		writeHighscore(highscore, "highscore.html", "Highscore", "alltime.png");
		writeHighscore(reviser, "reviser.html", "Best Reviser", "reviser.png");
		writeHighscore(creator, "creator.html", "Best Informationcreators", "creator.png");
	}

	private static void writeHighscore(List<RankingModel> ranking, String path, String name, String image) {
		List<String> output = new LinkedList<>();
		output.add("<html>");
		output.add("<head> "
				+ "<title>Highscore</title> <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
				+ "<style> img {display:block; margin-left:auto; margin-right:auto } tr {color: #e73886;} td { color: #ffffff; padding-left: .5em; padding-right: .5em;  padding-top: .9em; padding-bottom: .9em; }  h2 {color: #e73886;}   h3 {color: #e73886;})</style></head>");
		output.add("<body><h2>" + name + "</h2>");
		output.add("<image src=\"" + image + "\"/>");
		output.add("<table style=\"width:90%\"><tr> <th>Position</th> <th>Name</th>  <th>Points</th> </tr>");

		for (RankingModel rank : ranking)
			output.add("<tr> <td>" + rank.getPlace() + "</td><td>" + rank.getId() + "</td><td>" + rank.getPoints()
					+ "</td></tr>");
		output.add("</table></body></html>");
		try {
			Files.write(Paths.get((new File(App.getBaseFolderForCreation(), path)).getAbsolutePath()), output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String getMd5ForFile(File crunchifyFile) {
		String hash = null;
		FileInputStream crunchifyInputStream = null;
		try {
			crunchifyInputStream = new FileInputStream(crunchifyFile);

			hash = DigestUtils.md5Hex(IOUtils.toByteArray(crunchifyInputStream));
		} catch (IOException e) {
			System.out.println("Error in hashing");
		} finally {
			IOUtils.closeQuietly(crunchifyInputStream);
		}
		return hash;
	}
	public static void writeRoutes(TreeMap<Integer, Route> routes) {
		File routesFile = new File(App.getBaseFolderForCreation(), "routes.dat");
		FileHelper.writeObject(routesFile, routes);

		String hash = getMd5ForFile(routesFile);
		List<String> write=new LinkedList<>();
		write.add(hash);
		try {
			Files.write(Paths.get(App.getBaseFolderForCreation(),"hash"), write);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<File> list=new LinkedList<>();
		list.add(routesFile);
		FileHelper.writeZipFile(list,new File(App.getBaseFolderForCreation(),"update.zip"));

		File routesFolder = new File(App.getBaseFolderForCreation(), "routes");
		if (!routesFolder.exists() || !routesFolder.isDirectory())
			routesFolder.mkdir();

		for (Integer i : routes.keySet()) {
			String fileName=routes.get(i).getFrom() + "-" + routes.get(i).getTo();
			fileName.replaceAll("/", "_");
			JsonHelper.writeJsonToFile(JsonHelper.createJsonFromObject(routes.get(i)),
					new File(routesFolder, i + "-" + fileName) + ".json");
		}
	}

	

	public static void writeMapBoard(MapBoard board) {
		FileHelper.writeObject(new File(App.getBaseFolderForCreation(), "game.board"), board);
		MapBoard b = (MapBoard) FileHelper.readObject(new File(App.getBaseFolderForCreation(), "game.board"));
		System.out.println("can reade" + b.getMapContent()[0][0][0]);
	}

	public static void writeStatistics(Map<Integer, List<String>> countMap, Set<String> users, String userReport) {
		List<String> output = new LinkedList<>();
		output.add("<html>");
		output.add("<head> "
				+ "<title>Statistics</title> <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
				+ "<style> img {display:block; margin-left:auto; margin-right:auto } tr {color: #e73886;} td { color: #ffffff; padding-left: .5em; padding-right: .5em;  padding-top: .9em; padding-bottom: .9em; }  h2 {color: #e73886;}   h3 {color: #e73886;})</style></head>");
		output.add("<body><h2>Statistics</h2>");
		output.add(userReport + "</br>");
		output.add("Users participated:" + users.size());

		output.add("</br>");
		for (int type : countMap.keySet()) {
			output.add(SubmitType.getByValue(type).name() + " : " + countMap.get(type).size() + "</br>");
		}
		output.add("//////////</br>");
		for (int type : countMap.keySet()) {
			for (String current : countMap.get(type))
				output.add(current + "</br>");
		}
		output.add("</body></html>");
		try {
			Files.write(Paths.get((new File(App.getBaseFolderForCreation(), "statistics.html")).getAbsolutePath()),
					output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
