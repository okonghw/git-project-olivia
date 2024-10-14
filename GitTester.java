import java.nio.file.Paths;
import java.io.*;

public class GitTester {
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        try {
            // part 1: create initial commit
            Git.deleteEverything(Paths.get("git"));
            // Testing git creation and repo initialization:
            File testFile = new File("testFile.txt");
            File testDir = new File("testDir");
            File testFile2 = new File("testDir/testFile2.txt");
            testFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter("testFile.txt"));
            writer.write("hello!!!!");
            writer.close();
            testDir.mkdir();
            testFile2.createNewFile();
            writer = new BufferedWriter(new FileWriter("testDir/testFile2.txt"));
            writer.write("yoooooo");
            writer.close();
            // do the commit
            Git dir = new Git();
            dir.stage("testFile.txt");
            dir.stage("testDir");
            System.out.println(dir.commit("Riyan", "part 1"));

            // part 2: create second commit
            // this tests editing a file and creating a new file but only staging the new file
            File newTestFile = new File("newTestFile.txt");
            newTestFile.createNewFile();
            writer = new BufferedWriter(new FileWriter("newTestFile.txt"));
            writer.write("insert test content here");
            writer.close();
            writer = new BufferedWriter(new FileWriter ("testDir/testFile2.txt", true));
            writer.write ("\nand more");
            writer.close();
            // do the commit
            dir.stage("newTestFile.txt");
            System.out.println(dir.commit("Riyan", "part 2"));

            // part 3: create third commit
            // this stages the last file
            dir.stage("testDir/testFile2.txt");
            System.out.println(dir.commit("Riyan", "part 3"));

            // part 4: check out second commit
            dir.checkout("9a79fdcee8a5f88d0e54adbce45c54b335ce7dce");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}