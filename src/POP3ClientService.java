import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;


public class POP3ClientService implements Runnable {

    protected Socket socket;
    MailServerDB database;
    private static final Logger log= Logger.getLogger( SMTPClientService.class.getName() );

    public POP3ClientService(Socket clientSocket,MailServerDB db) {
        this.socket = clientSocket;
        this.database = db;
    }
    private File[] sortMails(String user_name){
        
        File inbox_dir = new File(database.getUser(user_name)+"/inbox");
        File[] mails_to_sort = inbox_dir.listFiles();
        Arrays.sort( mails_to_sort , new Comparator<File>()
        {
            @Override
            public int compare(final File o1, final File o2) {

                if (((File)o1).lastModified() > ((File)o2).lastModified()) {
                    return -1;
                } else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }); 
        return mails_to_sort;
    }
    public void run() {
        //get input from socket
        BufferedReader brinp;
        //output to socket
        OutputStreamWriter out;
        String line,user_name,domain_name;
        user_name=domain_name=null;
        File[] mails=null;
        boolean auth=false;
        try {
            brinp = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new OutputStreamWriter(socket.getOutputStream());
            out.write("+Welcome to my POP3 server\r\n");
            out.flush();
        } 
        catch (IOException e) {
            log.log(Level.SEVERE,"POP3 client service starting failed...");
            return;
        }
        log.log(Level.INFO,"POP3 client service started");
        while (true) {
            try {
                line = brinp.readLine();
                if ((line == null) || line.equalsIgnoreCase("QUIT")) {
                    log.log(Level.INFO,"POP3 client service finished");
                    out.write("Bye!\r\n");
                    out.flush();
                    socket.close();
                    return;
                } 
                else if (line.indexOf("USER ") == 0){
                    log.log(Level.FINE,line);
                    String delim = " ";
                    String[] command_parse = line.split(delim);
                    delim = "@";
                    if (command_parse.length > 1){
                        String[] user_domain_parse = command_parse[1].split(delim);
                        user_name = user_domain_parse[0];
                        if (user_domain_parse.length > 1){
                            domain_name = user_domain_parse[1];
                        }
                    }
                    else {
                        log.log(Level.WARNING,command_parse[0]);
                    }
                    if (database.getDomain(domain_name)==null){
                        String msg = "Domain: " + domain_name + " not exist";
                        log.log(Level.WARNING,msg);
                        domain_name=null;
                    }
                    if (database.getUser(user_name)==null){
                        String msg = "User: " + user_name + " not exist";
                        log.log(Level.WARNING,msg);
                        user_name=null;
                    }
                    else {
                    	out.write("+OK password required\r\n");
                    	out.flush();
                    }

                }
                else if (line.indexOf("PASS ") == 0 && user_name != null && domain_name != null){
                    log.log(Level.FINE,line);
                    String password_file_path=null;
                    String password=null;
                    File password_file;
                    String delim = " ";
                    String[] command_parse = line.split(delim);
                    if (command_parse.length > 1){
                        password = command_parse[1];
                    }
                    else {
                        log.log(Level.WARNING,command_parse[0]);
                    }
                    password_file_path = database.getUser(user_name).getAbsolutePath()+"/.passwd";
                    password_file = new File(password_file_path);
                    if (password_file.exists()){
                        String msg = "Found password file for "+user_name;
                        log.log(Level.FINE,msg);
                        String pass;
                        try(BufferedReader br = new BufferedReader(new FileReader(password_file))) {
                            pass = br.readLine();
                        }
                        if (password.compareTo(pass)==0){
                        	out.write("+OK "+user_name+" you are now logged in\r\n");
                            String _msg = "Authentication success for "+user_name;
                            log.log(Level.FINE,_msg);
                            auth = true;
                            out.flush();
                        }
                    }
                    else{
                        String msg = "No password file for "+user_name;
                        log.log(Level.WARNING,msg);
                    }
                }
                else if (line.indexOf("LIST") == 0 && auth){
                    log.log(Level.FINE,line);
                    mails=sortMails(user_name);
                    out.write("+OK "+mails.length+" messages\r\n");
                    int i=0;
                    for (File print_file : mails){
                        out.write(++i + " " + print_file.getName()+"\r\n");
                    }
                    out.flush();
                }
                else if (line.indexOf("RETR ") == 0 && auth){
                    log.log(Level.FINE,line);
                    mails=sortMails(user_name);
                    String delim = " ";
                    String[] command_parse = line.split(delim);
                    File message;
                    int msgNumber=0;
                    if (command_parse.length > 1){
                        msgNumber = Integer.parseInt(command_parse[1]);
                    }
                    if (mails.length > msgNumber-1){
                    	out.write("+OK "+msgNumber+" to read:\r\n");
                    	out.flush();
                        message = mails[msgNumber-1];
                        try (BufferedReader br = new BufferedReader(new FileReader(message))) {
                            String msg_line = null;
                            while ((msg_line = br.readLine()) != null) {
                                out.write(msg_line);
                            }
                        }
                        out.write("\r\n");
                        out.flush();
                    }
                    else{
                        String msg = "No such mail: "+msgNumber;
                        log.log(Level.WARNING,msg);
                    }
                }
                else if (line.indexOf("DELE ") == 0 && auth){
                    log.log(Level.FINE,line);
                    mails=sortMails(user_name);
                    String delim = " ";
                    String[] command_parse = line.split(delim);
                    int msgNumber=0;
                    if (command_parse.length > 1){
                        msgNumber = Integer.parseInt(command_parse[1]);
                    }
                    if (mails.length > msgNumber-1){
                        if(mails[msgNumber-1].delete()){
                            out.write("+OK message "+ msgNumber + " deleted\r\n");
                            out.flush();
                        }
                    }
                    else{
                        String msg = "No such mail: "+msgNumber;
                        log.log(Level.WARNING,msg);
                    }
                }
            } 
            catch (IOException e) {
                log.log(Level.SEVERE,"POP3 client service failed");
                System.out.println(e.getMessage());
                return;
            }
        }
    }
}

