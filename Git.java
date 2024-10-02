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
    public static void main(String[] args) throws DigestException, NoSuchAlgorithmException, IOException {
        // Test creating repository when it doesn't exist (should print "Initialized
        // repository and deleted files")
        // System.out.println(initRepoTester());
        // Test creating respository when it already exists (should print "Git
        // Repository already exists")
        // initRepo();
        // System.out.println(initRepoTester());
        blobTester(Paths.get("/Users/oliviakong/Desktop/everything basically/forkedcodetest/newFolder"), false);
        // Path path = Paths.get("/Users/oliviakong/Desktop/everything basically/forkedcodetest/newFolder");
        // createBlob(path, false);
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

    public static void blobTester(Path path, boolean compress) throws DigestException, NoSuchAlgorithmException, IOException {
        // path = createBlob(path, compress);
        // Path path2 = Paths.get("./git/objects/" + sha1(path));
        // System.out.println("Copied file exists within objects directory: " + path2.toFile().exists());
        // // System.out.println("Contents of copied and original are the same: " + (Files.mismatch(path, path2) == -1));
        // boolean bool1 = path2.toFile().delete();
        // System.out.println("Path deleted correctly in objects directory in order to reset: " + (bool1));
        // checkIndex(path);
        // if (compress) {
        //     boolean bool2 = path.toFile().delete();
        //     System.out.println("Unzipped path deleted correctly in order to reset: " + (bool2));
        // }
        // Create the blob (or tree) from the file or directory

        Path resultingPath = createBlob(path, compress);
        
        String pathHash = sha1(resultingPath);
        Path objectsPath = Paths.get("./git/objects/" + pathHash);

        boolean existsInObjects = objectsPath.toFile().exists();
        System.out.println("Copied file/tree exists within objects directory: " + existsInObjects);
        if (!existsInObjects) {
            System.out.println("Test failed: Object does not exist in the objects directory.");
            return;
        }

        boolean deleted = objectsPath.toFile().delete();
        System.out.println("Path deleted correctly in objects directory in order to reset: " + deleted);

        Path objectsDir = Paths.get("./git/objects");
        File[] allObjects = objectsDir.toFile().listFiles();
        if (allObjects != null) {
            for (File object : allObjects) {
                boolean objectDeletionStatus = object.delete();
                System.out.println("Deleted object in objects folder: " + object.getName() + " : " + objectDeletionStatus);
            }
        }
        System.out.println("All files in objects directory cleared.");
    }

    public static String sha1(Path path) throws DigestException, IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
    
        // if it's a directory
        if (path.toFile().isDirectory()) {
            File[] allFiles = path.toFile().listFiles();
            StringBuilder indexContent = new StringBuilder();
    
            if (allFiles != null && allFiles.length > 0) {
                for (File file : allFiles) {
                    Path filePath = file.toPath();
                    String fileSha1 = sha1(filePath);
    
                    if (file.isDirectory()) { // if directory
                        indexContent.append("tree ");
                    } else { // if file
                        indexContent.append("blob ");
                    }
                    indexContent.append(fileSha1).append(" ").append(file.getName()).append("\n");
                }
                // remove the last newline
                indexContent.setLength(indexContent.length() - 1);
            }
            md.update(indexContent.toString().getBytes());
        } else {
            md.update(Files.readAllBytes(path));
        }
    
        byte[] digest = md.digest();
        BigInteger fileInt = new BigInteger(1, digest);
        return fileInt.toString(16);
    }

    //Creates blob using fileToSave, compress - zip-compression true or false, returns the path of the unzipped file
    public static Path createBlob(Path fileToSave, boolean compress) throws DigestException, NoSuchAlgorithmException, IOException {
        return createBlob(fileToSave, compress, "");
    }

    private static Path createBlob(Path fileToSave, boolean compress, String parent) throws DigestException, NoSuchAlgorithmException, IOException {
        StringBuilder sb = new StringBuilder();
        // if file is a directory
        if (fileToSave.toFile().isDirectory()) {
            File filesInside[] = fileToSave.toFile().listFiles();

            // if directory is empty
            if (filesInside == null || filesInside.length == 0) {
                // compresses if true, unzips in order to copy data
                if (compress) {
                    String str1 = compressData(fileToSave);
                    fileToSave = unzip(str1, fileToSave.getFileName().toString());
                }
                Path hash = Paths.get("./git/objects/" + sha1(fileToSave));
                // copies data
                Files.copy(fileToSave, hash, REPLACE_EXISTING);

                // index file line
                if (parent.equals("")) {
                    sb.append("tree " + hash + " " + fileToSave.toFile().getName() + "\n");
                } else {
                    sb.append("tree " + hash + " " + parent + "/" + fileToSave.toFile().getName() + "\n");
                }
            } else { // if directory is not empty
                // compresses if true, unzips in order to copy data
                if (compress) {
                    String str1 = compressData(fileToSave);
                    fileToSave = unzip(str1, fileToSave.getFileName().toString());
                }

                String currentParent = parent.isEmpty() ? fileToSave.toFile().getName() : parent + "/" + fileToSave.toFile().getName();
                sb.append("tree " + sha1(fileToSave) + " " + currentParent + "\n");

                for (File file : filesInside) {
                    Path insideFile = file.toPath();
                    createBlob(insideFile, compress, currentParent);
                }

                Path hash = Paths.get("./git/objects/" + sha1(fileToSave));
                // copies data
                Files.copy(fileToSave, hash, REPLACE_EXISTING);
            }
        } else { // if file is not a directory
            // compresses if true, unzips in order to copy data
            if (compress) {
                String str1 = compressData(fileToSave);
                fileToSave = unzip(str1, fileToSave.getFileName().toString());
            }
            Path hash = Paths.get("./git/objects/" + sha1(fileToSave));
            // copies data
            Files.copy(fileToSave, hash, REPLACE_EXISTING);

            String str = "blob " + sha1(fileToSave) + " " + (parent.isEmpty() ? "" : parent + "/") + fileToSave.getFileName().toString() + "\n";
            sb.append(str);
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("./git/index", true))) {
            bw.write(sb.toString());
        }
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
        // deletes zipped file after testing that contents are correct
        System.out.println("Zipped file deleted successfully: " + Paths.get(path).toFile().delete());
        return (Paths.get(dest.getPath()));
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
        while ((currentLine = reader.readLine()) != null) {
            String trimmedLine = currentLine.trim();
            if (trimmedLine.equals(lineToRemove))
                continue;
            writer.write(currentLine + System.getProperty("line.separator"));
        }
        writer.close();
        reader.close();
        boolean successful = tempFile.renameTo(inputFile);
        System.out.println("Entry deleted in index: " + successful);
    }

}