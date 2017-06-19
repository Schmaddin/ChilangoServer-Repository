package go.chilan.server;

import java.io.File;
import java.io.IOException;

import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;
import javax.mail.MessagingException;

import com.graphhopper.chilango.FileHelper;
import com.graphhopper.chilango.network.ConnectionMessage;
import com.graphhopper.chilango.network.EasyCrypt;
import com.graphhopper.chilango.network.ServerConnection;
import com.graphhopper.chilango.network.ServerMessageAuth;
import com.graphhopper.chilango.network.ConnectionMessage.ConnectionInformation;

public class TestConnection {
	public static void main(String[] args) throws IOException {
		try {
			FileHelper.writeObject(new File(App.inputFile), new File("../"));
			System.out.println("lala");
			MailUser.writeMail("martin.wuerflein@gmx.de", "sdfasdf", "hallo account");
			System.out.println("ready");
		} catch (MessagingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return;


	}
}
