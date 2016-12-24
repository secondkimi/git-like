import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.io.File;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import org.apache.commons.io.FileUtils;

public class Gitlet implements Serializable {

	private ArrayList<CommitNode> commitNode; // store all the commit nodes in the arrayList
    private static HashMap<String, CommitNode> branchMap; // key: branch name; value: the newest node in that branch
	private CommitNode head_pointer;
    private ArrayList<String> staged; // all the staged 
    private ArrayList<String> removal; // files in the commit node but will be removed in the next commit
    private String currBranch; // which branch we are in
    
    public static void main(String[] args) {
        String command;
        String token = "o";
        Gitlet gitlet = new Gitlet();
        gitlet.readGit();
        if (args.length == 0) {
            System.out.println("No command detected.");
            System.out.println("use ' java Gitlet help ' for help");
            return;
        } else if (args.length == 1) {
            command = args[0];
        } else {
            command = args[0];
            token = args[1];
        }
        switch (command) {
            case "init":
                gitlet.initialize();
                break;
            case "add":
            	// be able to add multiple files at a time 
            	for (int i =1; i<args.length;i++) {
                	gitlet.add(args[i]);
            	}
                break;
            case "commit":
                gitlet.commit(token);
                break;
            case "rm":
                // be able to remove multiple files at a time 
                for (int i =1; i<args.length;i++) {
                    gitlet.remove(args[i]);
                }
                break;
            case "log":
                gitlet.log();
                break;
            case "global-log":
                gitlet.globallog();
                break;
            case "find":
                gitlet.find(token);
                break;
            case "status":
                gitlet.status();
                break;
            case "stash":
                if ( token == null || "save".equals(token) )
                {
                    gitlet.stash();
                }
                else if ( "pop".equals(token) ) {
                    gitlet.pop();
                }
                break;
            case "checkout":
                if ( args.length == 1) {
                    gitlet.checkoutBranch("master");
                }
                else if (args.length == 3) {
                    String token1 = args[1];
                    String token2 = args[2];
                    gitlet.checkout(token1, token2);
                    break;
                } else {
                    if (branchMap.containsKey(token)) {
                        gitlet.checkoutBranch(token);
                    } else {
                        gitlet.checkout(token);
                    }
                    break;
                }
            case "branch":
                gitlet.branch(token);
                break;
            case "rm-branch":
                gitlet.rmbranch(token);
                break;
            case "reset":
                gitlet.reset(token);
                break;
            case "merge":
                gitlet.merge(token);
                break;
            case "rebase":
                gitlet.rebase(token);
                break;
            case "i-rebase":
                gitlet.irebase(token);
                break;
            default:
                System.out.println("Unrecognized command.");
                break;
        }
        gitlet.saveGit();
    }

    private String currTimeStrGenerator() {
    	// generate a time string in the format yyyy-month-date mm:mm:mm
    	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    	Date date = new Date();
    	return dateFormat.format(date);
    }


    private void readGit() {
        // to save run time we can use arraylist instead of hashmap.
        HashMap<String, Object> gitMap;
        File gitFile = new File(".gitlet/git.ser");
        if (gitFile.exists()) {

            try {
                FileInputStream fileIn = new FileInputStream(gitFile);
                ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                gitMap = (HashMap<String, Object>) objectIn.readObject();
                commitNode = (ArrayList<CommitNode>) gitMap.get("commitNode");
                head_pointer = (CommitNode) gitMap.get("head_pointer");
                branchMap = (HashMap<String, CommitNode>)gitMap.get("branchMap");
                staged = (ArrayList<String>) gitMap.get("staged");
                removal = (ArrayList<String>) gitMap.get("removal");
                currBranch = (String) gitMap.get("currBranch");
            } catch (IOException e) {
                String msg = "IOException when loading git files";
                System.out.println(msg);
            } catch (ClassNotFoundException e) {
                String msg = "ClassNotFoundException when loading git files";
                System.out.println(msg);
            } 

        }
    }

    private void saveGit() {
        // to save run time, we can use arraylist instead of hashmap.But right now we are using hashmap
        HashMap<String, Object> gitMap = new HashMap<String, Object>();
        gitMap.put("commitNode", commitNode);
        gitMap.put("head_pointer", head_pointer);
        gitMap.put("branchMap", branchMap);
        gitMap.put("staged", staged);
        gitMap.put("removal", removal);
        gitMap.put("currBranch", currBranch);
        File git = new File(".gitlet","git.ser");
        if (git.getParentFile()!=null) {
            try (FileOutputStream fileOut = new FileOutputStream(git)){
                try(ObjectOutputStream objectOut = new ObjectOutputStream(fileOut))
                {
                    objectOut.writeObject(gitMap);
                } catch (IOException e) {
                     System.out.println("Unable to save git files. Encountering IOException");
                }
            } catch(IOException e) {
                System.out.println("Unable to save git files. Encountering IOException");   
            }
            
        }
      
    }

    /* initialize the gitlet system, create the .gitlet folder , and create the initial commitnode
    with the commitID 0*/
    private void initialize() {
    	String dateStr = currTimeStrGenerator();
    	CommitNode rootNode = new CommitNode(dateStr);
        commitNode = new ArrayList<CommitNode>();
        branchMap = new HashMap<String, CommitNode>();
        staged = new ArrayList<String>();
        removal = new ArrayList<String>();
        currBranch = "master";
        commitNode.add(rootNode);
    	head_pointer = rootNode;
        // create a .gitlet directory
        File file = new File(".gitlet");

        if ( !file.exists()) {
            file.mkdir();
        }
        else {
            System.out.println("A git repository has already been initialized");
        }
    }

     /* put the filename into the stage arraylist, all remove the removed file in removal arraylist */
    private void add(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File "+"<"+fileName+">"+" does not exist");
            return;
        }
        if ( file.isDirectory()) {
            System.out.println("Cannot add a directory");
            return;
        }

        if ( !head_pointer.addressBook.containsKey(fileName) )
        {
            
            if (!staged.contains(fileName)) {
                staged.add(fileName);
            }
            else {
                System.out.println("File <"+fileName+"> has already been added");
            }
        }

        else {
            // the file has already been added to the head pointer node.

            // check whether the file has been modified since the last commit. If No, do nothing 
            File oldFile = new File(head_pointer.addressBook.get(fileName));
            try {
                if (!FileUtils.contentEquals(oldFile,file)) {
                    staged.add(fileName);
                }
                else {
                    System.out.println("No changes for the file <"+fileName+"> since the last commit");
                }
            } catch(IOException e) {
                System.out.println("Exceptions while comparing two files in git add");
            }
            
            if (removal.contains(fileName)) {
                removal.remove(fileName);
            }
        }
    }

    /* create a new commitnode, copy the staged files into the folder in .gitlet corresponding 
    to this commitnode. Clear the removal and stage arraylists */
    private void commit(String message){
        if (message==null) {
            System.out.println("No message associated with this commit, please enter a message");
            return;
        }   
        if (staged.size()==0 && removal.size()==0) {
            System.out.println("No changes added to the commit");
            return;
        }

        // create a new directory .gitlet/<commitId>
        int newId = commitNode.size();
        String newPath = ".gitlet/"+newId+"/";
        File file = new File(newPath);
        if (!file.exists()) file.mkdir();
    
        CommitNode newNode = new CommitNode(newId, head_pointer.commitId, message, newPath, currTimeStrGenerator());
        CommitNode oldNode = head_pointer;
        File oldFile;
        File newFile;
        StandardCopyOption[] options = new StandardCopyOption[]{
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        }; 
        String oldELem;
        // first copy all the staged files
        for ( int i=0; i<staged.size(); i++ ) {
            oldELem = staged.get(i);
            oldFile = new File(oldELem);
            newFile = new File(newPath+oldELem);
            if ( oldFile.exists()) {
                try {
                    Files.copy(oldFile.toPath(), newFile.toPath(), options);
                    newNode.addressBook.put(oldELem, newPath+oldELem);
                } catch (IOException e) {
                    System.out.println("Cannot copy the file <"+oldELem+">");
                }
            }   
        }
        // then copy all the old files except removed files adn staged files
        for ( String key : oldNode.addressBook.keySet() ) {
            if ( !removal.contains(key) && !staged.contains(key) ) {
                oldFile = new File(".gitlet/"+head_pointer.commitId+"/"+key);
                newFile = new File(newPath+key);
                if ( oldFile.exists()) {
                    try {
                        Files.copy(oldFile.toPath(), newFile.toPath(), options);
                        newNode.addressBook.put(key, newPath+key);
                    } catch (IOException e) {
                        System.out.println("Cannot copy the file <"+key+">");
                    }
                }   
            }
        }
        // copy inBranch from the old node
        
        newNode.inBranch.add(currBranch);
        
        
        oldNode.addChild(newNode);
        commitNode.set(oldNode.commitId, oldNode);
        commitNode.add(newNode);
        head_pointer = newNode;
        branchMap.remove(currBranch);
        branchMap.put(currBranch, newNode);
        staged.clear();
        removal.clear();
    }

    /* put the filename into the removal arraylist, or remove the staged file from the stage arraylist*/
    private void remove(String fileName){
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File <"+fileName+"> does not exist");
            return;
        }
        if (file.isDirectory()){
            System.out.println("Cannot remove a directory");
            return;
        }
        if ( staged.contains(fileName)  ) {
            staged.remove(fileName);
        }
        else if ( head_pointer.addressBook.containsKey(fileName) ) {
            removal.add(fileName);
        }
        else {
            System.out.println("No reason to remove the file <"+fileName+">");
        }
    }

    /*find the commitid using the commitmessage*/
    private void find(String commitMessage){
        CommitNode node;
        boolean flag = false;
        if ( commitNode == null) {
            System.out.println("No such commit message exists");
            return;
        }
        for ( int i = 0; i < commitNode.size(); i++) {
            node = commitNode.get(i);
            if ( node.commitMessage.equals(commitMessage) ) {
                System.out.println("The id of the commit is: "+node.commitId);
                flag = true;
            }
        }
        if ( !flag ) {
            System.out.println("No such commit message exists");
        }

    }

    /*Merges files from the head of the given branch into the head of the current branch. */
    public void merge(String branchName) {
        if ( currBranch.equals(branchName) ) {
            System.out.println("Cannot merge a branch with itself");
            return;
        }
        if ( !branchMap.containsKey(branchName) ) {
            System.out.println("The branch <"+branchName+"> does not exist");
            return;
        }
        if ( branchMap.get(branchName).commitId == head_pointer.commitId ) {
            System.out.println("The branch is already up-to-date");
            return;
        }
        if ( staged.size() > 0 || removal.size() > 0) {
            System.out.println("error: Your local changes to the following files will be overwritten by merge");
            if ( staged.size() > 0) {
                for ( int i = 0; i <staged.size(); i++) {
                    System.out.println(staged.get(i));
                }
            }
            if ( removal.size() > 0 ) {
                for ( int j = 0; j <staged.size(); j++) {
                    System.out.println(removal.get(j));
                }
            }
            System.out.println("Please commit your changes or stash them before you can merge");
            return;
        }

        boolean flag = true;
        
        System.out.println("This operation might add, remove and modify some files in the working directory");
        System.out.println("Do you want to continue, Y / N ?");
        Scanner scanner = new Scanner(System.in);
        while ( scanner.hasNextLine() ) {
            String s = scanner.nextLine();
            if ( s != null && ( s.toLowerCase().equals("y") || s.toLowerCase().equals("yes") )) {
                flag = true;
                break;
            }
            else if ( s != null && ( s.toLowerCase().equals("n") || s.toLowerCase().equals("no")) ) {
                flag = false;
                break;
            }
            else {
                System.out.println("Do you want to continue, Y / N ?");
            }
        }
        
        if ( flag ) {
            // Do the merge task here

            // first find the common ancestor of two branches. Starting from either of the branches 
            CommitNode commonNode = head_pointer;
            while ( commonNode.parentId > 0 ) {
                commonNode = commitNode.get(commonNode.parentId);
                if ( commonNode.inBranch.contains(branchName) && commonNode.inBranch.contains(currBranch)) {
                    break;
                }
            }
            // now commonNode is the node with the common ancestor 
            CommitNode toMerge = branchMap.get(branchName);
            CommitNode currNode = head_pointer; 
            File oldFile;
            File newFile;
            File newFile2;
            StandardCopyOption[] options = new StandardCopyOption[]{
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            }; 
            // Here is the logic of how merge works
            for (String item : toMerge.addressBook.keySet()) {
                if (currNode.addressBook.containsKey(item)) {
                    // This file exists in both curr and merge. Compare it

                    try {
                        oldFile = new File(".gitlet/"+commonNode.commitId+"/"+item);
                        newFile = new File(".gitlet/"+toMerge.commitId+"/"+item);
                        newFile2 = new File(".gitlet/"+head_pointer.commitId+"/"+item);
                        if (!FileUtils.contentEquals(oldFile,newFile)&& 
                            FileUtils.contentEquals(oldFile,newFile2)) {
                            // copy merged to curr 
                            try {
                                Files.copy(newFile.toPath(), newFile2.toPath(), options);
                                newFile2 = new File(item);
                                Files.copy(newFile.toPath(), newFile2.toPath(), options);
                            } catch (IOException e) {
                                System.out.println("Cannot copy the file <"+item+">");
                            }
                        }

                        else if (!FileUtils.contentEquals(oldFile,newFile)&& 
                            !FileUtils.contentEquals(oldFile,newFile2) && !FileUtils.contentEquals(newFile,newFile2)){
                            // created a conflicted version in curr
                            try {
                                newFile2 = new File(".gitlet/"+head_pointer.commitId+"/"+item+".conflicted");
                                Files.copy(newFile.toPath(), newFile2.toPath(), options);
                                newFile2 = new File(item+".conflicted");
                                Files.copy(newFile.toPath(), newFile2.toPath(), options);
                            } catch (IOException e) {
                                System.out.println("Cannot copy the file <"+item+">");
                            }
                        }
                    } catch(IOException e) {
                        System.out.println("Exceptions while comparing two files in git add");
                    }
                }
                else {
                    // Add this file to curr
                    oldFile = new File(".gitlet/"+toMerge.commitId+"/"+item);
                    newFile = new File(".gitlet/"+head_pointer.commitId+"/"+item);
                    try {
                        Files.copy(oldFile.toPath(), newFile.toPath(), options);
                        newFile = new File(item);
                        Files.copy(oldFile.toPath(), newFile.toPath(), options);
                    } catch (IOException e) {
                        System.out.println("Cannot copy the file <"+item+">");
                    }
                }
            }
            for (String item2 : currNode.addressBook.keySet()) {
                if (!toMerge.addressBook.containsKey(item2)) {
                    // Remove this file from curr
                    oldFile = new File(item2);
                    newFile = new File(".gitlet/"+head_pointer.commitId+"/"+item2);
                    try {
                        Files.deleteIfExists(newFile.toPath());
                        Files.deleteIfExists(oldFile.toPath());
                    } catch (IOException e) {
                        System.out.println("Cannot delete the file <"+item2+">");
                    }
                }
            
            }
        }
    }


     /*find the split point of the current branch and the given branch, then snaps off the 
    current branch at this point, then reattaches the current branch to the head of the given branch. */
    public void rebase(String branchName) {
        // copy the CommitNode and enter new time stamp and new commit id and parent, child ptr.
        if ( currBranch.equals(branchName) ) {
            System.out.println("Cannot rebase a branch on itself");
            return;
        }
        if ( !branchMap.containsKey(branchName) ) {
            System.out.println("The branch <"+branchName+"> does not exist");
            return;
        }
        if ( branchMap.get(branchName).commitId == head_pointer.commitId ) {
            System.out.println("The branch is already up-to-date");
            return;
        }
        if ( staged.size() > 0 || removal.size() > 0) {
            System.out.println("error: Your local changes to the following files will be overwritten by merge");
            if ( staged.size() > 0) {
                for ( int i = 0; i <staged.size(); i++) {
                    System.out.println(staged.get(i));
                }
            }
            if ( removal.size() > 0 ) {
                for ( int j = 0; j <staged.size(); j++) {
                    System.out.println(removal.get(j));
                }
            }
            System.out.println("Please commit your changes or stash them before you can merge");
            return;
        }

        boolean flag = true;
        
        System.out.println("This operation might add, remove and modify some files in the working directory");
        System.out.println("Do you want to continue, Y / N ?");
        Scanner scanner = new Scanner(System.in);
        while ( scanner.hasNextLine() ) {
            String s = scanner.nextLine();
            if ( s != null && ( s.toLowerCase().equals("y") || s.toLowerCase().equals("yes") )) {
                flag = true;
                break;
            }
            else if ( s != null && ( s.toLowerCase().equals("n") || s.toLowerCase().equals("no")) ) {
                flag = false;
                break;
            }
            else {
                System.out.println("Do you want to continue, Y / N ?");
            }
        }
        
        if ( flag ) {
            // Do the rebase task here

            // first find the common ancestor of two branches. Starting from either of the branches 
            CommitNode commonNode = head_pointer;
            while ( commonNode.parentId > 0 ) {
                commonNode = commitNode.get(commonNode.parentId);
                if ( commonNode.inBranch.contains(branchName) && commonNode.inBranch.contains(currBranch)) {
                    break;
                }
            }
            // now commonNode is the node with the common ancestor 
            //TODO
        }
    }

    /*A interactive version of rebase*/
    public void irebase(String branchName) {


    }

    /*scp [some file] cs61b-[xxx]@torus.cs.berkeley.edu:[some other file]*/
    public void addRemote(String remoteName, String usernameOnServer, String server, 
        String locationOnServer) {


    }

    public void rmRemote(String remoteName) {

    }

    /*Restores the given file in the working directory to its state 
    at the commit at the head of the current branch.*/
    private void checkout(String fileName){
        if ( !head_pointer.addressBook.containsKey(fileName) ) {
            System.out.println("File does not exist in the most recent commit, or no such branch exists");
            return;
        }
        File oldFile = new File(".gitlet/"+head_pointer.commitId+"/"+fileName);
        File newFile = new File(fileName);
        if ( oldFile.exists() && newFile.exists() ) {

            StandardCopyOption[] options = new StandardCopyOption[]{
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            };
            try {
                Files.copy(oldFile.toPath(), newFile.toPath(), options);
            } catch (IOException e) {
                System.out.println("Cannot copy the file <"+fileName+">");
            }
        
        }
        else {
            System.out.println("File <"+fileName+"> has been deleted from the working directory");
        }
    }

    /*Restores the given file in the working directory to its state at the given commit.*/
    private void checkout(String commitId, String fileName) {
        int id = Integer.parseInt(commitId);
        if ( id < 0 || id >= commitNode.size() ) {
            System.out.println("No commit with that id exists");
            return;
        }
        CommitNode node = commitNode.get(id);
        if ( !node.addressBook.containsKey(fileName) ) {
            System.out.println("File does not exist in this commit");
            return;
        }
        File oldFile = new File(".gitlet/"+node.commitId+"/"+fileName);
        File newFile = new File(fileName);
        if ( oldFile.exists() && newFile.exists() ) {
            
            StandardCopyOption[] options = new StandardCopyOption[]{
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            };
            try {
                Files.copy(oldFile.toPath(), newFile.toPath(), options);
            } catch (IOException e) {
                System.out.println("Cannot copy the file <"+fileName+">");
            }
        
        }
        else {
            System.out.println("File <"+fileName+"> has been deleted from the working directory");
        }
    }

    /*Restores all files in the working directory to their versions in the commit at the head of the 
    given branch. Considers the given branch to now be the current branch.*/
    private void checkoutBranch(String branchName) {
        if ( !branchMap.containsKey(branchName) ) {
            System.out.println("File does not exist in the most recent commit, or no such branch exists");
            return;
        }
        if ( currBranch.equals(branchName) ) {
            System.out.println("Already in branch <"+branchName+">");
            return;
        }
        // restore all files in the working directory 
        CommitNode node = branchMap.get(branchName);
        HashSet<String> rm = new HashSet<String>();
        for ( String s : head_pointer.addressBook.keySet() ) {
            if ( !node.addressBook.containsKey(s) ) {
                rm.add(s);
            }
        }

        File oldFile;
        File newFile;
        StandardCopyOption[] options = new StandardCopyOption[]{
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES
        }; 
        // copy all the files from commit node to working directory 
        for (String key : node.addressBook.keySet() ) {
            oldFile = new File(".gitlet/"+node.commitId+"/"+key);
            newFile = new File(key);
            try {
                Files.copy(oldFile.toPath(), newFile.toPath(), options);
            } catch (IOException e) {
                System.out.println("Cannot copy the file <"+key+">");
            }
        }
        // remove deleted files from the working directory
        for ( String r : rm ) {
            newFile = new File(r);
            try {
                Files.deleteIfExists(newFile.toPath());
            } catch (IOException e) {
                System.out.println("Cannot delete the file <"+r+">");
            }
        }

        head_pointer = branchMap.get(branchName);
        currBranch = branchName;
        System.out.println("Switched to branch <"+branchName+">");

    }

    /*create a new branch on current commitnode using the given branchname*/
    private void branch(String branchName) {
        if ( branchMap.containsKey(branchName) ) {
            System.out.println("The branch <"+branchName+"> already exists");
            return;
        }
        CommitNode node = head_pointer;
        node.inBranch.add(branchName);
        commitNode.set(node.commitId, node);
        int par = node.parentId;
        head_pointer = node;
        while ( par > 0) {
            node = commitNode.get(par);
            node.inBranch.add(branchName);
            par = node.parentId;
            commitNode.set(node.commitId, node);
        }
        branchMap.put(branchName, head_pointer);
    }

    /*remove the branchname key in the hashmap of branches*/
    private void rmbranch(String branchName){
        if ( !branchMap.containsKey(branchName) ) {
            System.out.println("The branch <"+branchName+"> does not exist");
            return;
        }
        if ( currBranch.equals(branchName) ) {
            System.out.println("Cannot remove the current branch");
            return;
        }
        branchMap.remove(branchName);
        // bad runtime. explore all the commit nodes in the tree and remove inBranch items
        // to improve
        CommitNode node;
        for ( int i = 0; i <commitNode.size(); i++) {
            node = commitNode.get(i);
            if ( node.inBranch.contains(branchName)) {
                node.inBranch.remove(branchName);
                commitNode.set(node.commitId,node);
            }
        }
    }

    /*print all information*/
    private void globallog(){
        for ( int i = commitNode.size()-1; i>-1; i--) {
            displayMessage(commitNode.get(i));   
        }
    }

    /*print the current status including branch, removal, and stage*/
    private void status() {
        System.out.println("=== Branches ===");
        System.out.println("On branch "+currBranch);
        int size = branchMap.size();
        if ( size > 1) {
            
            System.out.println("Other branches: ");
            for (String s : branchMap.keySet()) {
                if ( !s.equals(currBranch) )
                    System.out.println(s);
            }
        }
        System.out.println();

        if ( staged.size() > 0 ) {
            System.out.println("=== Staged Files ===");
            for ( int i = 0; i < staged.size(); i++) {
                System.out.println(staged.get(i));
            }
            System.out.println();
        }
        if ( removal.size() > 0 ) {
            System.out.println("=== Files Marked for Removal ===");
            for ( int j = 0; j < removal.size(); j++) {
                System.out.println(removal.get(j));
            }
            System.out.println();
        }

        if ( staged.size() == 0 && removal.size() == 0) {
            System.out.println("Nothing to commit. Working directory clean");
        }
    }

    /*print out the information using "print" method, one by one in the commitnode*/
    private void log() {
        CommitNode currNode = head_pointer;
        while ( currNode != null ) {
            displayMessage(currNode);
            int parent = currNode.parentId;
            if ( parent == currNode.commitId || parent < 0) {
                // no parent commit node
                break;
            }
            currNode = commitNode.get(parent);
        }
    }

    private void displayMessage(CommitNode node) {
        if (node==null) return;
        System.out.println();
        System.out.println("====");
        System.out.println("Commit "+node.commitId+".");
        System.out.println(node.time);
        System.out.println(node.commitMessage);

    }

    /*print the information of current node
        the same as displayMasseage()*/ 
    private void print(CommitNode cn) {
      // currentlly replaced by displayMessage method

    }

     /*reset the file state to the given commit id and move the head to the given node*/
    public void reset(String commitId) {
        int id = Integer.parseInt(commitId);
        if ( id < 0 || id >= commitNode.size() ) {
            System.out.println("No such commit exists");
            return;
        }  
        if ( !commitNode.get(id).inBranch.contains(currBranch) ) {
            System.out.println("This commit is not from the current branch");
            return;
        }
        boolean flag = true;
        if ( removal.size() > 0 || staged.size() > 0) {
            System.out.println("This operation will abort all the staged files and removed files");
            System.out.println("Do you want to continue, Y / N ?");
            Scanner scanner = new Scanner(System.in);
            while ( scanner.hasNextLine() ) {
                String s = scanner.nextLine();
                if ( s != null && ( s.toLowerCase().equals("y") || s.toLowerCase().equals("yes") )) {
                    flag = true;
                    break;
                }
                else if ( s != null && ( s.toLowerCase().equals("n") || s.toLowerCase().equals("no")) ) {
                    flag = false;
                    break;
                }
                else {
                    System.out.println("Do you want to continue, Y / N ?");
                }
            }
        }
        if ( flag ) {
            CommitNode temp_head_pointer = commitNode.get(id);
            removeAllChildrenFromCurrBranch(temp_head_pointer, currBranch);
            // now copy all the files from commitNode to working directory 
            File oldFile;
            File newFile;
            StandardCopyOption[] options = new StandardCopyOption[]{
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            };
            for ( String key : temp_head_pointer.addressBook.keySet() ) {
            
                oldFile = new File(".gitlet/"+id+"/"+key);
                newFile = new File(key);
                if ( oldFile.exists()) {
                    try {
                        Files.copy(oldFile.toPath(), newFile.toPath(), options);
                    } catch (IOException e) {
                        System.out.println("Cannot copy the file <"+key+">");
                    }
                }   
            } // end for loop

            // remove all the files that are the curr commit but not in the previous
            for ( String r : head_pointer.addressBook.keySet() ) {
                if ( !temp_head_pointer.addressBook.containsKey(r) ) {
                    
                    newFile = new File(r);
                    try {
                        Files.deleteIfExists(newFile.toPath());
                    } catch (IOException e) {
                        System.out.println("Cannot delete the file <"+r+">");
                    }
                }
            }  
            head_pointer = temp_head_pointer;
        }
    }

    public void removeAllChildrenFromCurrBranch(CommitNode node, String branch) {
        // helper method for reset()
        if ( node.childs == null ) {
            return;
        }
        CommitNode childNode;
        for ( int i = 0; i < node.childs.size(); i++) {
            childNode = node.childs.get(i);
            if ( childNode.inBranch.contains(branch) ) {
                
                CommitNode oldNode = commitNode.get(childNode.commitId);
                oldNode.inBranch.remove(branch);
                commitNode.set(oldNode.commitId, oldNode);

                // if you remove a node from a commit tree the commit id of other nodes would change
                // and this is no good for our design
                /*
                else {
                    CommitNode newNode = commitNode.get(node.commitId);
                    newNode.childs.remove(childNode);
                    commitNode.set(newNode.commitId, newNode);
                    if ( childNode.childs == null ) {
                        commitNode.remove(childNode.commitId);
                    }
        
                }*/
                removeAllChildrenFromCurrBranch(childNode, branch);

            }
            
        }
        

    }

    public void push(String remoteName, String remoteBranchName) {

    }

    public void pull(String remoteName, String remoteBranchName){
        
    }

    // Notes for stash and pop
    // Thr original git allow user to stash multiple states of files in a stack
    // Here we just assume one state of files should be saved. Which means the size of 
    // stack is always zero
    public void stash() {

    }

    public void pop() {

    }
}