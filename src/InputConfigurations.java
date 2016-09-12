import java.util.Properties;
import java.io.*;

public class InputConfigurations {
	
    private final static String nameOfFileConfig = "MailServer.properties";
    private InputStream input = null; 

    public void configInputFunc(Properties prop)
    {
        try {
            input = new FileInputStream(nameOfFileConfig);
            // load a properties file
            prop.load(input);
            // Default values
            if( !prop.containsKey("SMTPPortNumber") )
                    prop.setProperty("SMTPPortNumber", "25");

            if( !prop.containsKey("POP3PortNumber") )
                    prop.setProperty("POP3PortNumber", "110");
        } 
        catch (IOException ex) {
                ex.printStackTrace();
        } 
        finally {
            if (input != null) {
                try {
                    input.close();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    } 	// End ConfigInputFunc()
} // End InputConfigurations class

