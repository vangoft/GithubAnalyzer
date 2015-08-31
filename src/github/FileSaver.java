/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.io.Serializable;
import java.util.List;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

/**
 *
 * @author Alexander
 */
public class FileSaver implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private List<GHRepository> forks;
    private List<GHCommit> commits;
    
    public FileSaver (List<GHCommit> commits, List<GHRepository> forks)
    {
        this.commits = commits;
        this.forks = forks;
    }
    
    public List<GHCommit> getCommits ()
    {
        return commits;
    }
    
    public void setCommits (List<GHCommit> commits)
    {
        this.commits = commits;
    }
    
    public List<GHRepository> getForks ()
    {
        return forks;
    }
    
    public void setForks (List<GHRepository> forks)
    {
        this.forks = forks;
    }
    
}
