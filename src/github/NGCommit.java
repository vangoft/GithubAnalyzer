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
    private final List<String> parents;
    private List<String> children;
    
    public NGCommit(GHCommit commit){
        this.commit = commit;
        parents = commit.getParentSHA1s();
        children = new ArrayList<String>();
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
        return parents;
    }
    
    public String getSHA1(){
        return commit.getSHA1();
    }
    
    public boolean hasChild(){
        return (children.size() > 0);
    }      

    public List<String> getChildren()
    {
        return children;
    }
    
    public void addChild(String sha1){
        children.add(sha1);
    }
    
    public String getAvatarUrl() throws IOException{
        return commit.getAuthor().getAvatarUrl();
    }

}
