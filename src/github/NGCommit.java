/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.kohsuke.github.GHCommit;

/**
 *
 * @author Alexander Distergoft
 */
public class NGCommit {
    private final GHCommit commit;
    private final List<String> parentsSha1;
    private final List<String> childrenSha1;
    private final List<GHCommit> parents;
    private final List<GHCommit> children;
    
    public NGCommit(GHCommit commit){
        this.commit = commit;
        parentsSha1 = commit.getParentSHA1s();
        childrenSha1 = new ArrayList<>();
        parents = new ArrayList<>();
        children = new ArrayList<>();
    }
    
    public String getAuthor(){
        return commit.getCommitShortInfo().getAuthor().getName();
    }
    
    public String getMessage(){
        return commit.getCommitShortInfo().getMessage();
    }
    
    public Date getDate(){
        return commit.getCommitShortInfo().getAuthor().getDate();
    }
    
    public List<String> getParentSHA1s(){
        return parentsSha1;
    }
    
    public String getSHA1(){
        return commit.getSHA1();
    }
    
    public boolean hasChild(){
        return (childrenSha1.size() > 0);
    }      

    public List<String> getChildren()
    {
        return childrenSha1;
    }
    
    public void addChildSHA1(String sha1){
        childrenSha1.add(sha1);
    }
    
    public String getAvatarUrl() throws IOException{
        return commit.getAuthor().getAvatarUrl();
    }

}
