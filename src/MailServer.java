
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MailServer {
    
    private final MailServerDB database;
    public static final Logger log= Logger.getLogger( MailServer.class.getName() );
    
    MailServer(){
        
        database = new MailServerDB();
    
    }   
	
    public void start() {
        log.log(Level.INFO,"Mail server starting...");
        Properties prop = new Properties();
        InputConfigurations inConf = new InputConfigurations();
        inConf.configInputFunc(prop);
        int pop3port = Integer.parseInt(prop.getProperty("POP3PortNumber"));
        int smtpport = Integer.parseInt(prop.getProperty("SMTPPortNumber"));
        
        try{
            POP3Server PopServer = new POP3Server(pop3port,database);
            SMTPServer SmtpServer = new SMTPServer(smtpport,database);
            Thread POPThread = new Thread(PopServer);
            Thread SMTPThread = new Thread(SmtpServer);
            POPThread.start();
            SMTPThread.start();
        }
        catch (Exception ex){
            log.log(Level.SEVERE,"Mail server failed starting...");
            return;
        }
        finally{
            log.log(Level.INFO,"Mail server started");
        }
    }
    
    public static void main(String[] args) {
        MailServer myMailServer = new MailServer();
        myMailServer.start();
    }
}
