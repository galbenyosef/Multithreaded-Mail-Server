
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MailServerDB {
    
    private ArrayList<File> domains;
    private ArrayList<File> users;
    private Map< String,ArrayList<String> > domain_aliases;
    private Map< String,ArrayList<File> > aliases;
    private static final Logger log= Logger.getLogger( MailServerDB.class.getName() );
    
    MailServerDB(){

        init();

    }
    
    public File getDomain(String domain){
    	if (domain!=null){
    		for (int i=0;i<domains.size();i++){
    			if (domains.get(i).getName().compareTo(domain)==0)
    				return domains.get(i);
    		}
    	}
        return null;
    }
    
    public File getUser(String user){
    	if (user != null){
	        for (int i=0;i<users.size();i++){
	            if (users.get(i).getName().compareTo(user)==0)
	                return users.get(i);
	        }
    	}
        return null;
    }
    public File[] getAlias(String domain,String alias){
        ArrayList<String> temp;
    	if (domain_aliases.containsKey(domain)){
            temp = domain_aliases.get(domain);
            for (int i=0;i<temp.size();i++)
                if (temp.get(i).compareTo(alias)==0)
                    return (File[])aliases.get(alias).toArray((new File[temp.size()]));
        }
    	return null;
    }
    
    private boolean isDomain(String str){

        for (int j = 1; j < str.length()-1; j++){
            if (Character.isLetter(str.charAt(j-1)) && str.charAt(j) == '.' && Character.isLetter(str.charAt(j+1)))
                return true;
        } 
        return false;
    }
    
    private void init(){

        File main_folder = new File(".");
        File[] main_dir_content = main_folder.listFiles();
        domains = new ArrayList<>();
        users = new ArrayList<>();
        domain_aliases = new HashMap<>();
        aliases = new HashMap<>();
    //get potential folder names from current directory
    //and add to Domains if isDomain
        for (File pot_domain : main_dir_content) {
            if (pot_domain.isDirectory()) {
                if (isDomain(pot_domain.getName())){
                    domains.add(pot_domain);
                    domain_aliases.put(pot_domain.getName(), new ArrayList<>());
                }
            }
        }
    //from every domain from Domains
        for (File domain : domains){
            //list domain content
            File[] domain_content = domain.listFiles();
    //for every domain content , consider folder as User, therefore
    //add to Users
            for (File pot_user_folder : domain_content){
                if (pot_user_folder.isDirectory())
                    users.add(pot_user_folder);
                
    //we can also use this loop to build aliases database
            	if (pot_user_folder.isFile() == true ){
                    if (pot_user_folder.getName().compareTo("aliases")==0){
                        File _alias = pot_user_folder;
                        try (BufferedReader br = new BufferedReader(new FileReader(_alias))) {
                                //read aliases file
                            String aline = br.readLine();
                            while (aline != null) {
                                //parse alias_name and user list
                                String alias_name=aline.split("=")[0];
                                alias_name=alias_name.replaceAll("\\s","");
                                domain_aliases.get(domain.getName()).add(alias_name);
                                aliases.put(alias_name,new ArrayList<>());
                                //parse users from user list
                                String[] names = aline.split("=")[1].split(",");
                                //add user name to list
                                for (String single_name : names){
                                    single_name=single_name.replaceAll("\\s+","");
                                    aliases.get(alias_name).add(new File(domain.getAbsolutePath()+'/'+single_name));
                                }
                                //read next
                                aline = br.readLine();
                            }
                        } 
                        catch (IOException ex) {
                            log.log(Level.SEVERE,"Failed building aliases database");
                            System.out.println(ex.getMessage());
                        }
                    }
                }
            }
        }
    //for every user folder from Users
        for (File user_folder : users){
            boolean inbox_exists=false;
            boolean _passwd_exists=false;
            //list user folder files
            File[] user_files = user_folder.listFiles();
            //for every user folder's file
            for (File user_file : user_files){
                //search for inbox
                if (user_file.isDirectory() && user_file.getName().toLowerCase().compareTo("inbox") == 0){
                    inbox_exists=true;
                }
                if (user_file.isFile() && user_file.getName().toLowerCase().compareTo(".passwd") == 0){
                    _passwd_exists=true;
                }
            }
            //inbox not exist, create it
            if (!inbox_exists){
                String msgStart = "inbox of "+user_folder.getName()+" not exists, creating...";
                log.log(Level.INFO,msgStart);
                File new_inbox_folder = new File(user_folder.getAbsolutePath()+"/inbox");
                new_inbox_folder.mkdir();
                String msgEnd = "inbox of "+user_folder.getName()+" created: "+new_inbox_folder.getAbsolutePath();
                log.log(Level.INFO,msgEnd);
            }
            //password not exist, make default
            if (!_passwd_exists){

                File new_passwd_file = new File(user_folder.getAbsolutePath()+"/.passwd");
                String msgStart = "password of "+user_folder.getName()+" not exists, creating...";
                log.log(Level.SEVERE,msgStart);
                try {
                    new_passwd_file.createNewFile();
                    FileWriter fw;
                    fw = new FileWriter(new_passwd_file.getAbsoluteFile());
                    fw.write("password");
                    fw.close();
                    String msgEnd = "password of "+user_folder.getName()+" created: 'password'";
                    log.log(Level.SEVERE,msgEnd);
                }
                catch (IOException e) {
                    log.log(Level.SEVERE,"Error creating password file");
                }
            }
        }
    }
    
    
    
    
}
