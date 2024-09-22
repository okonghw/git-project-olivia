import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Scanner;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.*;


public class Git {
    public static void main (String[] args) throws DigestException, NoSuchAlgorithmException, IOException {
        //Test creating repository when it doesn't exist (should print "Initialized repository and deleted files")
       System.out.println(initRepoTester());
       //Test creating respository when it already exists (should print "Git Repository already exists")
       initRepo();
       System.out.println(initRepoTester());
       blobTester(Paths.get("/users/pranaviyer/test/file.txt"), true);
    }
    
    //Tests initRepo() for when directory already exists or doesn't exist yet
    public static String initRepoTester() throws IOException{
        //Creates all three directories/files - git, objects and index within git
        Path file1 = Paths.get("./git/objects");
        File file2 = new File("./git/index");
        Path file3 = Paths.get("./git");
        //Tests if repository already exists, which should print "Git repository already exists"
        if (file1.toFile().exists() && file2.exists()){
            initRepo();
            return "";
        }
        //Initializes repo
        initRepo();
        //Checks if files were created
        boolean bool1 = file1.toFile().exists();
        boolean bool2 = file2.exists();
        //Deletes all the files (have to delete objects and index first as files.delete() only deletes empty directories)
        boolean delete = file1.toFile().delete() && file2.delete() && file3.toFile().delete();
        //Checks if files were created and then deleted
        if (bool1&&bool2&&delete){
            return "Initialized repository and deleted files";
        }
        return "Did not initialize repository";
    }

    //Initializes repo
    public static void initRepo() throws IOException{
        //Create directory/files in git folder, which creates parent directory git along the way
        Path file1 = Paths.get("./git/objects");
        File file2 = new File("./git/index");
        //Makes directories - will be false if they already exist
        boolean bool1 = file1.toFile().mkdirs();
        boolean bool2 = file2.createNewFile();
        //Returns "Git repository already exists" if both directories already exist
        if (!(bool1)&&!(bool2)){
            System.out.println("Git Repository already exists");
        }
    }

    public static void blobTester(Path path, boolean compress) throws DigestException, NoSuchAlgorithmException, IOException{
        path = createBlob(path, compress);
        Path path2 = Paths.get("./git/objects/" + sha1(path));
        System.out.println("Copied file exists within objects directory: " + path2.toFile().exists());
        System.out.println("Contents of copied and original are the same: " + (Files.mismatch(path, path2) == -1));
        boolean bool1 = path2.toFile().delete();
        System.out.println("Path deleted correctly in objects directory in order to reset: " + (bool1));
        checkIndex(path);
        if(compress){
            boolean bool2 = path.toFile().delete();
            System.out.println("Unzipped path deleted correctly in order to reset: " + (bool2));
        }
    }

    //Implements sha1 hash function
    public static String sha1(Path path) throws DigestException, IOException, NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] fileBytes = md.digest(Files.readAllBytes(path));
        BigInteger file_int = new BigInteger(1, fileBytes);
        String str = file_int.toString(16);
        return(str);
    }

    //Prints sha1 (check using sha1 website)
    public static String sha1Tester(Path path) throws DigestException, NoSuchAlgorithmException, IOException{
        return (sha1(path));
    }

    //Creates blob using fileToSave, compress - zip-compression true or false, returns the path of the unzipped file
    public static Path createBlob(Path fileToSave, boolean compress) throws DigestException, NoSuchAlgorithmException, IOException{
        //compresses if true, unzips in order to copy data
        if (compress){
            String str1 = compressData(fileToSave);
            fileToSave = unzip(str1, fileToSave.getFileName().toString());
        }
        Path hash = Paths.get("./git/objects/" + sha1(fileToSave));
        //copies data
        Files.copy(fileToSave, hash, REPLACE_EXISTING);
        //writes onto index file
        BufferedWriter bw = new BufferedWriter(new FileWriter("./git/index"));
        BufferedReader br = new BufferedReader(new FileReader("./git/index"));
        String str = "";
        if (br.readLine() == null){
            str = sha1(fileToSave) + " " + fileToSave.getFileName().toString();
        }
        else{
            str = "\n" + sha1(fileToSave) + " " + fileToSave.getFileName().toString();
        }
        br.close();
        bw.write(str);
        bw.close();
        return(fileToSave);
    }

    //zip-compression method
    public static String compressData(Path path) throws IOException, DigestException, NoSuchAlgorithmException{
        StringBuilder str = new StringBuilder();
        Scanner scanner = new Scanner(new FileReader(path.toString()));
        while (scanner.hasNextLine()){
            str.append(scanner.nextLine());
        }
        scanner.close();
        //creates zip file
        File f = new File(path.getFileName().toString() + ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        ZipEntry e = new ZipEntry(path.getFileName().toString());
        out.putNextEntry(e);
        byte[] data = str.toString().getBytes();
        out.write(data, 0, data.length);
        out.closeEntry();
        out.close();
        return (f.getPath());
    }

    //writes zip file onto blank file
    public static Path unzip(String path, String destDir) throws IOException{
        File dest = new File (destDir);
        if (!dest.exists()) {
            dest.createNewFile();
        }
        extract(path, dest);
        //deletes zipped file after testing that contents are correct
        System.out.println ("Zipped file deleted successfully: " + Paths.get(path).toFile().delete());
        return(Paths.get(dest.getPath()));
    }

    //extracts data from zip file
    public static void extract(String zipPath, File dest) throws IOException{
        ZipFile zipFile = new ZipFile(zipPath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            InputStream instream = zipFile.getInputStream(entry);
            FileOutputStream outstream = new FileOutputStream(dest);
            BufferedOutputStream buffstream = new BufferedOutputStream(outstream);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = instream.read(buffer)) != -1) {
                buffstream.write(buffer, 0, bytesRead);
            }
            instream.close();
            buffstream.close();
            outstream.close();
        }
        zipFile.close();
    }

    //checks if entry in index is correct
    public static void checkIndex(Path path) throws IOException, DigestException, NoSuchAlgorithmException{
        Scanner scanner = new Scanner(new FileReader("./git/index"));
        String line = scanner.nextLine();
        while (scanner.hasNextLine()){
            line = scanner.nextLine();
        }
        scanner.close();
        System.out.println("Correct entry in index: " + line.equals(sha1(path) + " " + path.getFileName().toString()));
        deleteIndex(line);
    }

    //deletes entry in index
    public static void deleteIndex(String line) throws IOException{
        File inputFile = new File("./git/index");
        File tempFile = new File("./git/tempfile");
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        String lineToRemove = line;
        String currentLine;
        while((currentLine = reader.readLine()) != null) {
            String trimmedLine = currentLine.trim();
            if(trimmedLine.equals(lineToRemove)) continue;
            writer.write(currentLine + System.getProperty("line.separator"));
        }
        writer.close(); 
        reader.close(); 
        boolean successful = tempFile.renameTo(inputFile);
        System.out.println("Entry deleted in index: " + successful);
    }
}