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

/**
 *
 * @author Alexander
 */
public class CommitView {
    
    private List<GHCommit> commits = new ArrayList<>();
    
    private final List<Line> lines = new ArrayList<>();
    private final List<Circle> nodes = new ArrayList<>();
    
    public CommitView()
    {
    }
    
    public void buildGraph()
    {
        int commitSize = commits.size();
        int stepSize = 30;
        int yOffset = 50;
        int xOffset = 50;
        
        for(int i = 0; i < commitSize; i++)
        {
            if(i + 1 < commitSize)
                lines.add(new Line(i * stepSize + xOffset,
                        yOffset, i * stepSize + stepSize + xOffset,
                        yOffset));
            
            nodes.add(createNode(i, xOffset, yOffset, stepSize));

        }
    }
    
    private Circle createNode(int i, int xOffset, int yOffset, int stepSize){
        
        Circle node = new Circle(i * stepSize + xOffset,
                    yOffset,
                    5,
                    Color.RED);
        
        Tooltip tt = new Tooltip("Author: " + commits.get(i).getCommitShortInfo().getAuthor().getName() + "\n"
                + "Commit Date: " + commits.get(i).getCommitShortInfo().getAuthor().getDate() + "\n"
                + "Commit Info: " + commits.get(i).getCommitShortInfo().getMessage());  
        
        // Try getting the avatar
        ImageView imgView = null;
        try {
            imgView = ImageViewBuilder.create()
                    .image(new Image(commits.get(i).getAuthor().getAvatarUrl()))
                    .build();
        } catch (IOException ex) {
            Logger.getLogger(CommitView.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // if avatar not empty add to tooltip
        if(imgView != null)
        {
            imgView.setFitHeight(50);
            imgView.setFitWidth(50);
            tt.setGraphic(imgView);
        }
        
        Tooltip.install(node, tt);
   
        return node;
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
}
