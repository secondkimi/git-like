import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
/*Commit class node of the tree*/

public class CommitNode implements Serializable {
    int commitId, parentId;
    String commitMessage, folderPath;
    ArrayList<CommitNode> childs;
    String time;
    HashSet<String> inBranch;
    HashMap<String, String> addressBook; // key: fileName value: path of the file

    public CommitNode(int commitId, int parentId, String commitMessage,
            String folderPath, String time) {
        this.commitId = commitId;
        this.parentId = parentId;
        this.commitMessage = commitMessage;
        this.folderPath = folderPath;
        this.time = time;
        this.addressBook = new HashMap<String, String>();
        this.inBranch = new HashSet<String>();
        this.childs = new ArrayList<CommitNode>();
    }

    public CommitNode(String time) {
    	// this node is used for git init
        this.commitId = 0;
        this.parentId = 0;
        this.commitMessage = "initial commit";
        this.time = time;
        this.addressBook = new HashMap<String, String>();
        this.inBranch = new HashSet<String>();
        this.inBranch.add("master");
        this.childs = new ArrayList<CommitNode>();
    }

    public CommitNode(CommitNode cn) {
        this.commitId = cn.commitId;
        this.parentId = -1;
        
    }

    public void addChild(CommitNode x) {
        (this.childs).add(x);
    }

}
