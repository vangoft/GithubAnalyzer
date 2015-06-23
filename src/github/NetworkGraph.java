/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.kohsuke.github.GHCommit;

/**
 *
 * @author Alexander Distergoft
 */
public class NetworkGraph {
    
    //private List<GHCommit> commits = new ArrayList<>();
    private final List<NGCommit> ngcommits = new ArrayList<>();
    
    private final List<Line> lines = new ArrayList<>();
    private final List<Circle> nodes = new ArrayList<>();
    private final HashMap<String, Color> colors = new HashMap<>();
    
    public NetworkGraph(List<GHCommit> commits)
    {
        processCommits(commits);
        setColors();
    }
    
    private void processCommits(List<GHCommit> commits){
        HashMap<String, NGCommit> hs = new HashMap();
        
        //add all commits in new format
        for(GHCommit com : commits)
            hs.put(com.getSHA1(), new NGCommit(com));
        
        //add missing parent/child information
        for(GHCommit com : commits)
            if(com.getParentSHA1s().size() > 0)
                for(String sha1 : com.getParentSHA1s())
                    hs.get(sha1).addChild(com.getSHA1());
        
        for(GHCommit com: commits)
            ngcommits.add(hs.get(com.getSHA1()));                
    }
    
    public void createGraph()
    {
        int stepSize = 30;
        int yOffset = 50;
        int xOffset = 50;
        
        int multiCnt = 0;
         
        for(int i = 0; i < ngcommits.size(); i++)
        {
            multiCnt = checkMultiNode(i);
            if(multiCnt == i)
            { 
                nodes.add(createSingleNode(i, nodes.size(), xOffset, yOffset, stepSize));  
            }
            else
            {
                nodes.add(createMultiNode(nodes.size(), i, multiCnt, xOffset, yOffset, stepSize));
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
       
        if(start + 1 >= ngcommits.size())
            return start;
        
        //has multiple parents?
        boolean par1 = ngcommits.get(start).getParentSHA1s().size() > 1;
        boolean par2 = ngcommits.get(start + 1).getParentSHA1s().size() > 1; 
        
        //has multiple children?
        boolean child1 = ngcommits.get(start).getChildren().size() > 1;
        boolean child2 = ngcommits.get(start + 1).getChildren().size() > 1;
        
        //has same author?
        boolean auth = ngcommits.get(start).getAuthor()
                    .equals(ngcommits.get(start + 1).getAuthor());
        
        boolean samePar = ngcommits.get(start).getParentSHA1s()
                    .equals(ngcommits.get(start + 1).getParentSHA1s());


        while((!par1) && (!par2)
                && (!child1) && (!child2)
                && (auth) && (!samePar))
        {
            start++;
            
            if(ngcommits.get(start).getParentSHA1s().size() < 1)
                break;
            else {                            
                par1 = ngcommits.get(start).getParentSHA1s().size() > 1;
                par2 = ngcommits.get(start + 1).getParentSHA1s().size() > 1; 
                child1 = ngcommits.get(start).getChildren().size() > 1;
                child2 = ngcommits.get(start + 1).getChildren().size() > 1;
                auth = ngcommits.get(start).getAuthor()
                    .equals(ngcommits.get(start + 1).getAuthor()); 
                samePar = ngcommits.get(start).getParentSHA1s()
                    .equals(ngcommits.get(start + 1).getParentSHA1s());
            }
        }
        return start;
    }   
    
    private Circle createSingleNode(int i, int pos, int xOffset, int yOffset, int stepSize){
             
        Circle node = new Circle(pos * stepSize + xOffset,
                    yOffset,
                    5,
                    colors.get(ngcommits.get(i).getAuthor()));
        
        Tooltip tt = new Tooltip("Author: " + ngcommits.get(i).getAuthor() + "\n\n"
                + "Commit Date: " + ngcommits.get(i).getDate() + "\n"
                + "SHA: " + ngcommits.get(i).getSHA1() + "\n"
                + "Commit Info: " + ngcommits.get(i).getMessage());  
        
        //getAvatar(tt, ngcommits.get(i));
        
        Tooltip.install(node, tt);
   
        return node;
    }
    
    private Circle createMultiNode(int x, int start, int end, int xOffset, int yOffset, int stepSize)
    {        
        Circle node = new Circle(x * stepSize + xOffset,
                    yOffset,
                    5,
                    colors.get(ngcommits.get(start).getAuthor()));
        
        String toolTip = "Author: " + ngcommits.get(start).getAuthor() + "\n\n";
        for(int i = start; i <= end; i++)
        {
            toolTip += "Commit Date: " + ngcommits.get(i).getDate() + "\n"
                + "SHA: " + ngcommits.get(i).getSHA1() + "\n"
                + "Commit Info: " + ngcommits.get(i).getMessage() + "\n\n";
        }
        Tooltip tt = new Tooltip(toolTip);   
        
        //getAvatar(tt, ngcommits.get(start));
        
        Tooltip.install(node, tt);
   
        return node;
    }
    
    private void getAvatar(Tooltip tt, NGCommit commit)
    {
        // Try getting the avatar
        ImageView imgView = null;
        String url = "";
        try {
            url = commit.getAvatarUrl();
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
        
    private void setColors(){
        Random rand = new Random();
        Color clr;
        for (NGCommit ngcommit : ngcommits) {
            if (colors.get(ngcommit.getAuthor()) == null) 
            {
                clr = Color.rgb(rand.nextInt((255 - 1) + 1) + 1,
                        rand.nextInt((255 - 1) + 1) + 1,
                        rand.nextInt((255 - 1) + 1) + 1);
                colors.put(ngcommit.getAuthor(), clr);
            }
        }
    }
        
}
