/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GitUser;

/**
 *
 * @author Alexander
 */
public class NetworkGraph {
    
    private List<GHCommit> commits = new ArrayList<>();
    
    private final List<Line> lines = new ArrayList<>();
    private final List<Circle> nodes = new ArrayList<>();
    
    public NetworkGraph()
    {
    }
    

    public void createGraph()
    {
        int stepSize = 30;
        int yOffset = 50;
        int xOffset = 50;
        
        int multiCnt = 0;
         
        for(int i = 0; i < commits.size(); i++)
        {
            multiCnt = checkMultiNode(i);
            if(multiCnt == i)
            {
                nodes.add(createSingleNode(nodes.size(), xOffset, yOffset, stepSize));  
            }
            else
            {
                nodes.add(createMultiNode(nodes.size(), multiCnt, xOffset, yOffset, stepSize));
                i = multiCnt;
            }    
        }
        
        for(int i = 0; i < nodes.size() - 1; i++)
        {
                lines.add(new Line(i * stepSize + xOffset,
                    yOffset, i * stepSize + stepSize + xOffset,
                    yOffset));
        }
        
    }
    
    private int checkMultiNode(int i)
    {
        int start = i;
       
        if(start + 1 >= commits.size())
            return start;
        
        boolean par1 = commits.get(start).getParentSHA1s().size() <= 1;
        boolean par2 = commits.get(start + 1).getParentSHA1s().size() <= 1;  
        boolean auth = commits.get(start).getCommitShortInfo().getAuthor().getName()
                    .equals(commits.get(start + 1).getCommitShortInfo().getAuthor().getName());       
        if(start == 15)
            System.out.println(commits.get(start).getCommitShortInfo().getAuthor().getName()
                     + " " + commits.get(start + 1).getCommitShortInfo().getAuthor().getName());
        while((par1)
                && (par2)
                && (auth))
        {
            start++;
            
            if(commits.get(start).getParentSHA1s().size() < 1)
                break;
            else {
                            
            par1 = commits.get(start).getParentSHA1s().size() <= 1;
            par2 = commits.get(start + 1).getParentSHA1s().size() <= 1;  
            auth = commits.get(start).getCommitShortInfo().getAuthor().getName()
                .equals(commits.get(start + 1).getCommitShortInfo().getAuthor().getName()); 
            }
        }
        return start;
    }   
    
    private Circle createSingleNode(int i, int xOffset, int yOffset, int stepSize){
        
        Circle node = new Circle(i * stepSize + xOffset,
                    yOffset,
                    5,
                    Color.RED);
        
        Tooltip tt = new Tooltip("Author: " + commits.get(i).getCommitShortInfo().getAuthor().getName() + "\n\n"
                + "Commit Date: " + commits.get(i).getCommitShortInfo().getAuthor().getDate() + "\n"
                + "SHA: " + commits.get(i).getSHA1() + "\n"
                + "Commit Info: " + commits.get(i).getCommitShortInfo().getMessage());  
        
        //getAvatar(tt, commits.get(i));
        
        Tooltip.install(node, tt);
   
        return node;
    }
    
    private Circle createMultiNode(int start, int end, int xOffset, int yOffset, int stepSize){
        
        Circle node = new Circle(start * stepSize + xOffset,
                    yOffset,
                    5,
                    Color.RED);
        
        String toolTip = "Author: " + commits.get(start).getCommitShortInfo().getAuthor().getName() + "\n\n";
        for(int i = start; i <= end; i++)
        {
            toolTip += "Commit Date: " + commits.get(i).getCommitShortInfo().getAuthor().getDate() + "\n"
                + "SHA: " + commits.get(i).getSHA1() + "\n"
                + "Commit Info: " + commits.get(i).getCommitShortInfo().getMessage() + "\n\n";
        }
        Tooltip tt = new Tooltip(toolTip);   
        
        //getAvatar(tt, commits.get(start));
        
        Tooltip.install(node, tt);
   
        return node;
    }
    
    private void getAvatar(Tooltip tt, GHCommit commit)
    {
        // Try getting the avatar
        ImageView imgView = null;
        String url = "";
        try {
            url = commit.getAuthor().getAvatarUrl();
            imgView = ImageViewBuilder.create()
                    .image(new Image(url))
                    .build();
        } catch (IOException | NullPointerException ex) {
        }
        
        // if avatar not empty add to tooltip
        if(imgView != null)
        {
            imgView.setFitHeight(50);
            imgView.setFitWidth(50);
            tt.setGraphic(imgView);
        }
    }
    
    public List getLines()
    {
        return lines;
    }
    
    public List getNodes()
    {
        return nodes;
    }
    
    public void setCommits(List<GHCommit> commits)
    {
        this.commits = commits;
    }
    
//    public void buildGraph()
//    {
//        int commitSize = commits.size();
//        int stepSize = 30;
//        int yOffset = 50;
//        int xOffset = 50;
//        
//        for(int i = 0; i < commitSize; i++)
//        {                   
//            nodes.add(createSingleNode(i, xOffset, yOffset, stepSize));
//            
//            if(i + 1 < commitSize)
//                lines.add(new Line(i * stepSize + xOffset,
//                        yOffset, i * stepSize + stepSize + xOffset,
//                        yOffset));
//        }
//    }
    
}
