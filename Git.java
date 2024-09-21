import java.io.File;
public class Git {
    public static void main (String[] args) {
       //Test creating repository when it doesn't exist (should print "Initialized repository and deleted files")
       System.out.println(initRepoTester());
       //Test creating respository when it already exists (should print "Git Repository already exists")
       initRepo();
       System.out.println(initRepoTester());
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
        File file1 = new File("./git/objects");
        File file2 = new File("./git/index");
        File file3 = new File("./git");
        //Tests if repository already exists, which should print "Git repository already exists"
        if (file1.exists() && file2.exists()){
            initRepo();
            return "";
        }
        //Initializes repo
        initRepo();
        //Checks if files were created
        boolean bool1 = file1.exists();
        boolean bool2 = file2.exists();
        //Deletes all the files (have to delete objects and index first as files.delete() only deletes empty directories)
        boolean delete = file1.delete() && file2.delete() && file3.delete();
        //Checks if files were created and then deleted
        if (bool1&&bool2&&delete){
            return "Initialized repository and deleted files";
        }
        return "Did not initialize repository";
    }
}