
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SMTPServer implements Runnable {

    private final int port;
    private final MailServerDB database;
    private Socket socket = null;
    private static final Logger log= Logger.getLogger( SMTPServer.class.getName() );
    
    SMTPServer(int port,MailServerDB db) {
        this.port = port;
        this.database = db;
    }

    @Override
    public void run(){
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                socket = serverSocket.accept();	
                log.log(Level.INFO,"Connection accepted");
                new Thread(new SMTPClientService(socket,database)).start();	
            }
        }  
        catch (IOException e) {
        	log.log(Level.SEVERE,"SMTP socket problem");
        	System.out.println("I/O error: " + e);
        }
    } 
}

