import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class GitletPrivateTest {
    private static final String GITLET_DIR = ".gitlet/";
    private static final String TESTING_DIR = "test_files/";
    private static final String LINE_SEPARATOR = "\r\n|[\r\n]";

    /**
     * Deletes existing gitlet system, resets the folder that stores files used
     * in testing.
     * 
     * This method runs before every @Test method. This is important to enforce
     * that all tests are independent and do not interact with one another.
     */
    @Before
    public void setUp() {
        File f = new File(GITLET_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f = new File(TESTING_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f.mkdirs();
    }

    /**
     * Tests that init creates a .gitlet directory. Does NOT test that init
     * creates an initial commit, which is the other functionality of init.
     */
    @Test
    public void testInitialize() {
        gitlet("init");
        File f = new File(GITLET_DIR);
        assertTrue(f.exists());
    }

    /**
    * Tests the correctness of add, remove and commit. Involves init, add,
    * remove, and commit.
    */
    @Test
    public void testAddRemoveCommit() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String hugFileName = TESTING_DIR + "hug.txt";
        String wugText = "This is a wug.";
        String hugText = "This is a hug.";
        createFile(wugFileName, wugText);
        createFile(hugFileName, hugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("add", hugFileName);
        gitlet("rm", hugFileName);
        gitlet("commit", "added wug");
        File wug = new File(".gitlet/1/" + wugFileName);
        File hug = new File(".gitlet/1/" + hugFileName);
        assertTrue(wug.exists());
        assertTrue(!hug.exists());
    }

    /**
     * Tests that log prints out commit messages in the right order. Involves
     * init, add, commit, and log.
     */
    @Test
    public void testLog() {
        gitlet("init");
        String commitMessage1 = "initial commit";

        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("add", wugFileName);
        String commitMessage2 = "added wug";
        gitlet("commit", commitMessage2);

        String logContent = gitlet("log");
        assertArrayEquals(new String[] { commitMessage2, commitMessage1 },
                extractCommitMessages(logContent));
    }

    /**
     * Tests that checking out a file name will restore the version of the file
     * from the previous commit. Involves init, add, commit, and checkout.
     */
    @Test
    public void testCheckoutFileName() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("checkout", wugFileName);
        assertEquals(wugText, getText(wugFileName));
    }

    /**
    * Tests that checking out a file name and the commitId will estores 
    * the given file in the working directory to its state at the given commit.
    * Involves init, add, commit, and checkout
    */
    @Test
    public void testCheckoutFileNameAndId() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "changed wug");
        writeFile(wugFileName, "This is still a wug.");
        gitlet("checkout", "1", wugFileName);
        assertEquals(wugText, getText(wugFileName));
    }

    /**
    * Tests that checking out a branch name will restore the given file in the 
    * working directory to its state at the commit at the head of the current branch.
    * Involves init, add, commit, branch, and checkout(branch)
    */
    @Test
    public void testBranchAndCheckoutBranch() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String hugFileName = TESTING_DIR + "hug.txt";
        String wugText = "This is a wug.";
        String hugText = "This is a hug.";
        createFile(wugFileName, wugText);
        createFile(hugFileName, hugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "b");
        gitlet("checkout", "b");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "changed wug on b");
        gitlet("checkout", "master");
        gitlet("add", hugFileName);
        gitlet("commit", "added hug");
        gitlet("checkout", "b");
        assertEquals("This is not a wug.", getText(wugFileName));
        assertEquals("This is a hug.", getText(hugFileName));
    }

    /**
    * Tests that reseting will restore all files to their versions in the commit with the given id
    * Involves init, add, commit, and reset.
    */
    @Test
    public void testReset() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String hugFileName = TESTING_DIR + "hug.txt";
        String wugText = "This is a wug.";
        String hugText = "This is a hug.";
        createFile(wugFileName, wugText);
        createFile(hugFileName, hugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("add", hugFileName);
        gitlet("commit", "added hug");
        writeFile(wugFileName, "This is not a wug.");
        writeFile(hugFileName, "This is not a hug.");
        gitlet("add", wugFileName);
        gitlet("add", hugFileName);
        gitlet("commit", "changed hug and wug");
        writeFile(wugFileName, "This is still a wug.");
        writeFile(hugFileName, "This is still a hug.");
        gitlet("reset", "2");
        assertEquals(wugText, getText(wugFileName));
        assertEquals(hugText, getText(hugFileName));
    }

    /**
    * Tests that merge works correctly. Involves init add commit, branch, checkout,
    * and merge
    */
    @Test
    public void testMerge() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String hugFileName = TESTING_DIR + "hug.txt";
        String bugFileName = TESTING_DIR + "bug.txt";
        String hugFileConflicted = TESTING_DIR + "hug.txt" + ".conflicted";
        // String dugFileName = TESTING_DIR + "dug.txt";
        String wugText = "This is a wug.";
        String hugText = "This is a hug.";
        String bugText = "This is a bug.";
        // String dugText = "This is a dug.";
        createFile(wugFileName, wugText);
        createFile(hugFileName, hugText);
        createFile(bugFileName, bugText);
        // createFile(dugFileName, dugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "b");
        gitlet("checkout", "b");
        gitlet("add", hugFileName);
        gitlet("add", bugFileName);
        gitlet("commit", "added hug and bug on b");
        gitlet("checkout", "master");
        writeFile(hugFileName, "This is not a hug");
        gitlet("add", hugFileName);
        gitlet("commit", "added hug on b");
        writeFile(bugFileName, "This is not a bug");
        gitlet("merge", "b");
        assertEquals(bugText, getText(bugFileName));
        assertEquals("This is not a hug", getText(hugFileName));
        assertTrue(new File(hugFileConflicted).exists());
    }

    /**
    * Tests that rebase works well, Involves init, add, commit, branch, checkout
    * rebase
    */
    @Test
    public void testRebase() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText1 = "This is a wug.";
        String wugText2 = "This is a wug2.";
        String wugText3 = "This is a wug3.";
        String wugText4 = "This is a wug4.";
        createFile(wugFileName, wugText1);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "splitPoint");
        gitlet("branch", "b");
        writeFile(wugFileName, wugText2);
        gitlet("add", wugFileName);
        gitlet("commit", "added wug2 on master");
        gitlet("checkout", "b");
        writeFile(wugFileName, wugText3);
        gitlet("add", wugFileName);
        gitlet("commit", "added wug3 on b");
        writeFile(wugFileName, wugText4);
        gitlet("add", wugFileName);
        gitlet("commit", "added wug4 on b");
        gitlet("rebase", "master");
        gitlet("checkout", wugFileName);
        assertEquals(wugText4, getText(wugFileName));
        gitlet("checkout", "6", wugFileName);
        assertEquals(wugText3, getText(wugFileName));
    }


    private static String gitlet(String... args) {
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        ByteArrayOutputStream printingResults = new ByteArrayOutputStream();
        try {
            /*
             * Below we change System.out, so that when you call
             * System.out.println(), it won't print to the screen, but will
             * instead be added to the printingResults object.
             */
            System.setOut(new PrintStream(printingResults));

            /*
             * Prepares the answer "yes" on System.In, to pretend as if a user
             * will type "yes". You won't be able to take user input during this
             * time.
             */
            String answer = "yes";
            InputStream is = new ByteArrayInputStream(answer.getBytes());
            System.setIn(is);

            /* Calls the main method using the input arguments. */
            Gitlet.main(args);

        } finally {
            /*
             * Restores System.out and System.in (So you can print normally and
             * take user input normally again).
             */
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return printingResults.toString();
    }

    /**
     * Returns the text from a standard text file (won't work with special
     * characters).
     */
    private static String getText(String fileName) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(fileName));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Creates a new file with the given fileName and gives it the text
     * fileText.
     */
    private static void createFile(String fileName, String fileText) {
        File f = new File(fileName);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeFile(fileName, fileText);
    }

    /**
     * Replaces all text in the existing file with the given text.
     */
    private static void writeFile(String fileName, String fileText) {
        FileWriter fw = null;
        try {
            File f = new File(fileName);
            fw = new FileWriter(f, false);
            fw.write(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes the file and all files inside it, if it is a directory.
     */
    private static void recursiveDelete(File d) {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                recursiveDelete(f);
            }
        }
        d.delete();
    }

    /**
     * Returns an array of commit messages associated with what log has printed
     * out.
     */
    private static String[] extractCommitMessages(String logOutput) {
        String[] logChunks = logOutput.split("====");
        int numMessages = logChunks.length - 1;
        String[] messages = new String[numMessages];
        for (int i = 0; i < numMessages; i++) {
            System.out.println(logChunks[i + 1]);
            String[] logLines = logChunks[i + 1].split(LINE_SEPARATOR);
            messages[i] = logLines[3];
        }
        return messages;
    }

    public static void main(String[] args) {
        jh61b.junit.textui.runClasses(GitletPrivateTest.class);
    }
}
