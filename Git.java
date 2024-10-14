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
import java.util.Calendar;

public class Git implements GitInterface {
    private String currentDate;
    private String headHash;
    private String treeHash;
    private String hashOfCommit;

    public void stage(String filePath) {
        try {
            if (filePath.contains("/")) {
                int charIndex = filePath.indexOf("/");
                filePath = filePath.substring(0, charIndex);
            }
            Git.createBlob(Paths.get(filePath), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String commit(String author, String message) {
        currentDate = getDate();
        findHeadHash();
        if (!headHash.equals("")) {
            // add extra files from head into index
            writeToIndex();
        }
        getTreeContents();
        StringBuffer toCommit = new StringBuffer("");
        toCommit.append("tree: " + treeHash);
        toCommit.append("\nparent: " + headHash);
        toCommit.append("\nauthor: " + author);
        toCommit.append("\ndate: " + currentDate);
        toCommit.append("\nmessage: " + message);
        String commitContent = toCommit.toString();
        try {
            // create a file to put the commit in
            // make the commit file
            // get that hash and put it in head

            File tempCommitFile = File.createTempFile("contentToHash", null);
            FileWriter writer = new FileWriter(tempCommitFile);
            writer.write(commitContent);
            writer.close();
            hashOfCommit = Git.sha1(tempCommitFile.toPath());
            writer = new FileWriter(new File("./git/HEAD"));
            writer.write(hashOfCommit);
            writer.close();
            writer = new FileWriter(new File("./git/objects/" + hashOfCommit));
            writer.write(commitContent);
            writer.close();
            // creates tree snapshot of the tree and puts in objects
            BufferedReader reader = new BufferedReader(new FileReader("./git/index"));
            StringBuffer sb = new StringBuffer();
            while (reader.ready()) {
                sb.append((char) reader.read());
            }
            reader.close();
            writer = new FileWriter(new File("./git/objects/" + treeHash));
            writer.write(sb.toString());
            writer.close();
            // overwrites index once commit is created
            writer = new FileWriter(new File("./git/index"));
            writer.write("");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashOfCommit;
    }

    public void checkout(String commitHash) {
        hashOfCommit = commitHash;
        try {
            // go through the current tree and delete everything
            BufferedReader reader = new BufferedReader(new FileReader("./git/objects/" + treeHash));
            while (reader.ready()) {
                Files.deleteIfExists(Paths.get(reader.readLine().substring(46)));
            }
            reader.close();
            // add everything from the previous tree back in
            reader = new BufferedReader(new FileReader("./git/objects/" + commitHash));
            treeHash = reader.readLine().substring(6);
            reader.close();
            reader = new BufferedReader(new FileReader("./git/objects/" + treeHash));
            while (reader.ready()) {
                String line = reader.readLine();
                String fileName = line.substring(46);
                File file = new File(fileName);
                if (file.isDirectory()) {
                    file.mkdirs();
                } else {
                    if (fileName.contains("/")) {
                        int charIndex = fileName.lastIndexOf("/");
                        File newDir = new File(fileName.substring(0, charIndex));
                        newDir.mkdirs();
                    }
                    file.createNewFile();
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    String fileHash = line.substring(5,46);
                    writer.write(getHashFileContent(fileHash));
                    writer.close();
                }
            }
            reader.close();

            // put the correct hash in head
            BufferedWriter writer = new BufferedWriter(new FileWriter ("./git/HEAD"));
            writer.write(commitHash);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getHashFileContent(String hash) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader reader2 = new BufferedReader(new FileReader("./git/objects/" + hash));
            while (reader2.ready()) {
                sb.append((char) reader2.read());
            }
            reader2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public void findHeadHash() {
        StringBuffer headHashReader = new StringBuffer("");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("./git/HEAD")));
            while (reader.ready()) {
                headHashReader.append(((char) reader.read()));
            }
            reader.close();
            headHash = headHashReader.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToIndex() {
        // part 1 finds the hash of the previous tree
        // currently we have the hash of the commit
        String oldTreeHash = "";
        try {
            // this finds the commit
            BufferedReader reader = new BufferedReader(new FileReader("./git/objects/" + headHash));
            // read just after the first 6 characters of the tree:
            // ("tree: " + hash of tree)
            // we just want the hash of the tree
            oldTreeHash = reader.readLine();
            oldTreeHash = oldTreeHash.substring(6);
            reader.close();

            if (oldTreeHash.equals("")) {
                return;
            }

            // part 2 actually writes to the file
            reader = new BufferedReader(new FileReader("./git/objects/" + oldTreeHash));
            BufferedWriter writer = new BufferedWriter(new FileWriter("./git/index", true));
            while (reader.ready()) { // read index list of previous tree
                String blobLine = reader.readLine();
                if (blobLine.equals("")) {
                    break;
                }
                String nextBlob = blobLine.substring(46);
                if (!fileInIndex(nextBlob)) {
                    writer.write(blobLine + "\n");
                }
                // if (!nextBlob.contains("/")) { // really jank technical way of doing this but
                // it works!
                // Git.createBlob(Paths.get(nextBlob), false);
                // }
                // add all files not already listed in index to index
            }
            reader.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean fileInIndex(String fileName) {
        int counter = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./git/index"));
            while (reader.ready()) {
                if (reader.readLine().substring(46).equals(fileName)) {
                    counter++;
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (counter > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void getTreeContents() {
        // StringBuffer content = new StringBuffer("");
        // try {
        // BufferedReader reader = new BufferedReader(new FileReader(new
        // File("./git/index")));
        // while (reader.ready()) {
        // content.append((char) reader.read());
        // }
        // reader.close();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        try {
            treeHash = Git.sha1(Paths.get("./git/index"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDate() {
        Calendar date = Calendar.getInstance();
        int day = date.get(Calendar.DAY_OF_MONTH);
        int month = date.get(Calendar.MONTH);
        int year = date.get(Calendar.YEAR);
        return ((month + 1) + "/" + day + "/" + year);
    }

    public static void main(String[] args) throws DigestException, NoSuchAlgorithmException, IOException {
        // Test creating repository when it doesn't exist (should print "Initialized
        // repository and deleted files")
        // System.out.println(initRepoTester());
        // Test creating respository when it already exists (should print "Git
        // Repository already exists")
        initRepo();
        // System.out.println(initRepoTester());
        blobTester(Paths.get(
                "/Users/RiyanKadribegovic/Desktop/School/12th/Honors Topics in Computer Science/git-project-olivia"),
                false);
        // Path path = Paths.get("/Users/oliviakong/Desktop/everything
        // basically/forkedcodetest/newFolder");
        // createBlob(path, false);
    }

    public Git() {
        try {
            initRepo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Tests initRepo() for when directory already exists or doesn't exist yet
    public static String initRepoTester() throws IOException {
        // Creates all three directories/files - git, objects and index within git
        Path file1 = Paths.get("./git/objects");
        File file2 = new File("./git/index");
        Path file3 = Paths.get("./git");
        // Tests if repository already exists, which should print "Git repository
        // already exists"
        if (file1.toFile().exists() && file2.exists()) {
            initRepo();
            return "";
        }
        // Initializes repo
        initRepo();
        // Checks if files were created
        boolean bool1 = file1.toFile().exists();
        boolean bool2 = file2.exists();
        // Deletes all the files (have to delete objects and index first as
        // files.delete() only deletes empty directories)
        boolean delete = file1.toFile().delete() && file2.delete() && file3.toFile().delete();
        // Checks if files were created and then deleted
        if (bool1 && bool2 && delete) {
            return "Initialized repository and deleted files";
        }
        return "Did not initialize repository";
    }

    // Initializes repo
    public static void initRepo() throws IOException {
        // Create directory/files in git folder, which creates parent directory git
        // along the way
        Path file1 = Paths.get("./git/objects");
        File file2 = new File("./git/index");
        File file3 = new File("./git/HEAD");
        // Makes directories - will be false if they already exist
        boolean bool1 = file1.toFile().mkdirs();
        boolean bool2 = file2.createNewFile();
        boolean bool3 = file3.createNewFile();
        // Returns "Git repository already exists" if both directories already exist
        if (!(bool1) && !(bool2) && !(bool3)) {
            System.out.println("Git Repository already exists");
        }
    }

    public static void blobTester(Path path, boolean compress)
            throws DigestException, NoSuchAlgorithmException, IOException {
        // path = createBlob(path, compress);
        // Path path2 = Paths.get("./git/objects/" + sha1(path));
        // System.out.println("Copied file exists within objects directory: " +
        // path2.toFile().exists());
        // // System.out.println("Contents of copied and original are the same: " +
        // (Files.mismatch(path, path2) == -1));
        // boolean bool1 = path2.toFile().delete();
        // System.out.println("Path deleted correctly in objects directory in order to
        // reset: " + (bool1));
        // checkIndex(path);
        // if (compress) {
        // boolean bool2 = path.toFile().delete();
        // System.out.println("Unzipped path deleted correctly in order to reset: " +
        // (bool2));
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

        for (File indexLine : allObjects) {
            checkIndex(indexLine.toPath());
        }

        if (allObjects != null) {
            for (File object : allObjects) {
                boolean objectDeletionStatus = object.delete();
                System.out.println(
                        "Deleted object in objects folder: " + object.getName() + " : " + objectDeletionStatus);
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
        String hashText = fileInt.toString(16);
        while (hashText.length() < 40) {
            hashText = "0" + hashText;
        }
        return hashText;
    }

    // Creates blob using fileToSave, compress - zip-compression true or false,
    // returns the path of the unzipped file
    public static Path createBlob(Path fileToSave, boolean compress)
            throws DigestException, NoSuchAlgorithmException, IOException {
        return createBlob(fileToSave, compress, "");
    }

    private static boolean inIndex(String line) {
        int counter = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./git/index"));
            while (reader.ready()) {
                if (reader.readLine().equals(line)) {
                    counter++;
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (counter > 0) {
            return true;
        } else {
            return false;
        }
    }

    private static Path createBlob(Path fileToSave, boolean compress, String parent)
            throws DigestException, NoSuchAlgorithmException, IOException {
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
                    String toWrite = "tree " + hash + " " + fileToSave.toFile().getName();
                    if (inIndex(toWrite)) {
                        System.out.println("already exists in index");
                    } else {
                        sb.append(toWrite).append("\n");
                    }
                } else {
                    String toWrite = "tree " + hash + " " + parent + "/" + fileToSave.toFile().getName();
                    if (inIndex(toWrite)) {
                        System.out.println("already exists in index");
                    } else {
                        sb.append(toWrite).append("\n");
                    }
                }
            } else { // if directory is not empty
                // compresses if true, unzips in order to copy data
                if (compress) {
                    String str1 = compressData(fileToSave);
                    fileToSave = unzip(str1, fileToSave.getFileName().toString());
                }

                String currentParent = parent.isEmpty() ? fileToSave.toFile().getName()
                        : parent + "/" + fileToSave.toFile().getName();
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

            String str = "blob " + sha1(fileToSave) + " " + (parent.isEmpty() ? "" : parent + "/")
                    + fileToSave.getFileName().toString() + "\n";
            sb.append(str);
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("./git/index", true))) {
            bw.write(sb.toString());
        }
        return (fileToSave);
    }

    // zip-compression method
    public static String compressData(Path path) throws IOException, DigestException, NoSuchAlgorithmException {
        StringBuilder str = new StringBuilder();
        Scanner scanner = new Scanner(new FileReader(path.toString()));
        while (scanner.hasNextLine()) {
            str.append(scanner.nextLine());
        }
        scanner.close();
        // creates zip file
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

    // writes zip file onto blank file
    public static Path unzip(String path, String destDir) throws IOException {
        File dest = new File(destDir);
        if (!dest.exists()) {
            dest.createNewFile();
        }
        extract(path, dest);
        // deletes zipped file after testing that contents are correct
        System.out.println("Zipped file deleted successfully: " + Paths.get(path).toFile().delete());
        return (Paths.get(dest.getPath()));
    }

    // extracts data from zip file
    public static void extract(String zipPath, File dest) throws IOException {
        ZipFile zipFile = new ZipFile(zipPath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
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

    // checks if entry in index is correct
    public static void checkIndex(Path path) throws IOException, DigestException, NoSuchAlgorithmException {
        Scanner scanner = new Scanner(new FileReader("./git/index"));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            deleteIndex(line);
        }
        scanner.close();
    }

    // deletes entry in index
    public static void deleteIndex(String line) throws IOException {
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

    public static void deleteEverything(Path path) {
        File file = new File(path.toString());
        if (file.listFiles() != null) {
            for (File childFile : file.listFiles()) {
                if (childFile.isDirectory()) {
                    deleteEverything(childFile.toPath());
                }
                childFile.delete();
            }
            file.delete();
        }
    }

}