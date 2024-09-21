import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class Git {
    public static void main (String[] args) throws DigestException, NoSuchAlgorithmException, IOException {
       //Test creating repository when it doesn't exist (should print "Initialized repository and deleted files")
       //System.out.println(initRepoTester());
       //Test creating respository when it already exists (should print "Git Repository already exists")
       //initRepo();
       //System.out.println(initRepoTester());
       System.out.println(sha1Tester(Paths.get("/users/pranaviyer/test/file.txt")));
    }

    //Initializes repo
    public static void initRepo(){
        //Create paths for each of the files in git folder, which creates parent directory git along the way
        File file1 = new File("./git/objects");
        File file2 = new File("./git/index");
        //Makes directories - will be false if they already exist
        boolean bool1 = file1.mkdirs();
        boolean bool2 = file2.mkdirs();
        //Returns "Git repository already exists" if both directories already exist
        if (!(bool1)&&!(bool2)){
            System.out.println("Git Repository already exists");
        }
    }
    
    //Tests initRepo() for when directory already exists or doesn't exist yet
    public static String initRepoTester(){
        //Creates all three directories - git, objects and index within git
        Path file1 = Paths.get("./git/objects");
        Path file2 = Paths.get("./git/index");
        Path file3 = Paths.get("./git");
        //Tests if repository already exists, which should print "Git repository already exists"
        if (file1.toFile().exists() && file2.toFile().exists()){
            initRepo();
            return "";
        }
        //Initializes repo
        initRepo();
        //Checks if files were created
        boolean bool1 = file1.toFile().exists();
        boolean bool2 = file2.toFile().exists();
        //Deletes all the files (have to delete objects and index first as files.delete() only deletes empty directories)
        boolean delete = file1.toFile().delete() && file2.toFile().delete() && file3.toFile().delete();
        //Checks if files were created and then deleted
        if (bool1&&bool2&&delete){
            return "Initialized repository and deleted files";
        }
        return "Did not initialize repository";
    }

    public static String sha1(Path path) throws DigestException, IOException, NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] fileBytes = md.digest(Files.readAllBytes(path));
        BigInteger file_int = new BigInteger(1, fileBytes);
        String str = file_int.toString(16);
        return(str);
    }

    public static String sha1Tester(Path path) throws DigestException, NoSuchAlgorithmException, IOException{
        return (sha1(path));
    }
}