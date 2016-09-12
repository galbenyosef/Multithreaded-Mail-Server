import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SMTPClientService implements Runnable {

    private final Socket socket;
    private final MailServerDB database;
    private static final Logger log= Logger.getLogger( SMTPClientService.class.getName() );
    
    public SMTPClientService(Socket clientSocket,MailServerDB db) {
        this.socket = clientSocket;
        this.database = db;
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        BufferedReader is = null;
        FileWriter os = null;
        @SuppressWarnings("unused")
		int length;
        try {
            is = new BufferedReader(new FileReader(source));
            os = new FileWriter(dest);
            char[] buffer = new char[1024];
            while ((length = is.read(buffer)) > 0) {
                os.write(String.valueOf(buffer));
            }
        }
        catch (IOException ex){
            String msg = "Could not deliever"+dest.getName();
            log.log(Level.SEVERE,msg);
        } 
        finally {
            if (null!=is){
                is.close();
            }
            if (null!=os){
            os.close();
            }
        }
    }

    @Override
    public void run() {
        //get input from socket
        BufferedReader brinp;
        //output to socket
        OutputStreamWriter out;
        //assistance vars
        String line,mode,user,domain;
        String hello="Default SMTP Client Hostname";
        boolean datable,domain_exist;
        datable=domain_exist=false;
        log.log(Level.INFO,"SMTP client service starting...");
        try {
            mode=user=domain=null;
            brinp = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new OutputStreamWriter(socket.getOutputStream());
            out.write("Welcome to my SMTP server\r\n");
            out.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE,"SMTP client service starting failed...");
            return;
        }
        log.log(Level.INFO,"SMTP client service started");
        while (true) {
            try {
                line = brinp.readLine();
                if ((line == null) || line.equalsIgnoreCase("QUIT")) {
                    log.log(Level.INFO,"SMTP client service finished");
                    out.write("221 Bye\r\n");
                    out.flush();
                    socket.close();
                    return;
                }
                else if (line.indexOf("HELO ") == 0){
                    log.log(Level.FINE,line);
                    hello = line.substring(5);
                    out.write("250 Hello "+ hello+"\r\n");
                    out.flush();

                }
                else if (line.indexOf("MAIL FROM:") == 0){
                    log.log(Level.FINE,line);
                    out.write("250 Ok\r\n");
                    out.flush();

                }

                else if (line.indexOf("RCPT TO:") == 0){
                    
                    log.log(Level.FINE,line);
                    String delim;
                    //parse command
                    delim = ":";
                    String[] rcpt = line.split(delim);
                    //maybe space has been entered
                    //rcpt[0] is command
                    //rcpt[1] is user@domain/alias
                    //introducing .replaceAll("\\s","") to remove w-spaces
                    rcpt[1] = rcpt[1].replaceAll("\\s","");
                    //get user and domain or alias
                    delim = "@";
                    rcpt = rcpt[1].split(delim);
                    //if rcpt[1] is null, then no domain is given, means alias
                    if (rcpt.length == 2){
                    	user = rcpt[0];
                    	domain = rcpt[1];
                    }
                    else{
                    	log.log(Level.WARNING,rcpt[0]);
                    }

                    //check if user and domain exist
                    if (database.getDomain(domain) != null){
                        log.log(Level.FINE,"Domain found");
                        domain_exist=true;
                    }
                    if (!domain_exist) {
                        log.log(Level.WARNING,"Domain not found");
                        out.write("Relay access denied\r\n");
                        out.flush();
                        continue;
                    }
                    if (database.getAlias(domain, user) != null){
                        log.log(Level.FINE,"Alias found");
                        mode = "alias";
                    }
                    else if (database.getUser(user) != null){
                        log.log(Level.FINE,"User found");
                        mode = "user";
                    }
                    
                    if (mode == null) {
                        log.log(Level.WARNING,"User/Alias not found");
                        out.write("Bad destination mailbox address\r\n");
                        out.flush();
                    }
                    if (domain_exist && mode != null){
                        datable=true;
                        out.write("250 Ok\r\n");
                        out.flush();
                        log.log(Level.FINE,"Recipient found");
                    }
                    else {
                        log.log(Level.WARNING,"User or Domain not exists");
                        out.write("RCPT ERROR!\r\n");
                        out.flush();
                        datable=false;
                    }

                }
                else if (line.indexOf("DATA") == 0 && datable){
                    log.log(Level.FINE,line);
                    out.write("End data with <CR><LF>.<CR><LF>\r\n");
                    out.flush();
                    //Date
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();
                    // creating a temporary file.
                    File dir = new File("temp");
                    dir.mkdir();
                	//create temp file which will be used for later copy to receipents
                    File temp = File.createTempFile( "temp", ".tmp", new File( dir.getAbsolutePath() ) );
                    // Arrange for this temporary file to be automatically deleted on exit
                    // if we forget to delete it.
                    temp.deleteOnExit();
                    log.log(Level.FINE,"Temp file created");
                    FileWriter writer = new FileWriter(temp);
                    writer.write("Received: from "+socket.getInetAddress()+" helo= "+hello);
            		writer.write(" by " + socket.getLocalAddress()+"for "+user+domain+";"+ date +"0000\r\n");
                    while (true) {
                        line = brinp.readLine();
                        if ((line == null) || line.equals(".")){
                            break;
                        }
                        log.log(Level.FINEST,line);
                        writer.write(line);
                    }
                    writer.flush();
                    writer.close();
                    log.log(Level.FINE,"Temp file written");
                    //one reciepent
                    if (mode.compareTo("user")==0 ){
                        log.log(Level.FINE,"User mode delivery started...");
                        File deliverer = new File(database.getUser(user).getAbsolutePath()+"/inbox/"+temp.getName() );
                        deliverer.createNewFile();
                        copyFileUsingStream(temp,deliverer);
                        out.write("Mail sent OK!\r\n");
                        out.flush();
                        log.log(Level.FINE,"User mode delivery finished");
                    }
                    //multiple receipents
                    else if (mode.compareTo("alias")==0){
                        log.log(Level.FINE,"Alias mode delivery started...");
                    	//File array of multiple receipents
                    	File[] rcpts = database.getAlias(domain, user);
                        //copy content from temp
                        for (File rcpt : rcpts) {
                        	File rcpt_msg = new File(rcpt.getAbsolutePath()+"/inbox/"+temp.getName());
                            copyFileUsingStream(temp, rcpt_msg);
                        }
                        out.write("250 Ok!\r\n");
                        out.flush();
                        log.log(Level.FINE,"Alias mode delivery finished");
                    }
                }
                else {
                    log.log(Level.WARNING,line);
                }
            }
            catch (IOException e) {
                log.log(Level.SEVERE,"SMTP client service failed");
                System.out.println(e.getMessage());
                return;
            }
        }
    }
}