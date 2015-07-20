/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

/**
 *
 * @author Alexander Distergoft
 */
public class NetworkGraph {
    
    //private List<GHCommit> commits = new ArrayList<>();
    private GHRepository repo;
    private final List<NGCommit> ngcommits = new ArrayList();
    private final HashMap<String, NGCommit> hs = new HashMap();
    
    private final List<Line> lines = new ArrayList<>();
    private final List<Circle> nodes = new ArrayList<>();
    private final HashMap<String, Double> posX = new HashMap<>();
    private final HashMap<String, Double> posY = new HashMap<>(); 
    private final HashMap<Integer, Color> colors = new HashMap<>();
    private final HashMap<Integer, Boolean> spaces = new HashMap<>();
    private static final int maxSpaces = 100;
    
    
    public NetworkGraph(List<GHCommit> commits, GHRepository repo)
    {
        this.repo = repo;
        processCommits(commits);
        setColors();
    }
    
    private void processCommits(List<GHCommit> commits){
        HashMap<String, NGCommit> hs = new HashMap();
        
        //put all commits with new format in hashmap
        for(GHCommit com : commits)
            hs.put(com.getSHA1(), new NGCommit(com));
        
        //add missing parent/child information
        //reverse list so that earlier date children come first
        Collections.reverse(commits);
        for(GHCommit com : commits)
            if(com.getParentSHA1s().size() > 0)
                for(String sha1 : com.getParentSHA1s())
                    hs.get(sha1).addChildSHA1(com.getSHA1());        
        
        //undo reverse and add all new commits into list
        Collections.reverse(commits);
        for(GHCommit com: commits)
            ngcommits.add(hs.get(com.getSHA1()));   
        
        processSpaces(ngcommits);
    }
    
    public void processSpaces(List<NGCommit> commits)
    {
        //put all commits with new format in hashmap
        for(NGCommit com : commits)
            hs.put(com.getSHA1(), com);
        
        spaces.put(0,true);
        for(int i = 1; i < maxSpaces; i++)
            spaces.put(i,false);
 
        ngcommits.get(0).setSpace(0);
        
        for(int i = 0; i < ngcommits.size(); i++)
        {
            //remove empty vertical space
            if(ngcommits.get(i).getChildrenSHA1s().size() > 1)
            {
                for(int j = 0; j < ngcommits.get(i).getChildrenSHA1s().size(); j++)
                {
                    if(hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getParentSHA1s().size() < 2)
                    {
                        if(hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getSpace() != ngcommits.get(i).getSpace())
                            spaces.put(hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getSpace(), false);
                    }
                    else
                    {
                        if(hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getSpace() != ngcommits.get(i).getSpace())
                            spaces.put(hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getSpace(), false);
                        
                        for(int k = 0; k < hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getParentSHA1s().size(); k++)
                        {
                            String parent = hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getParentSHA1s().get(k);
                            
                            if(!parent.equals(ngcommits.get(i).getSHA1()) 
                                    && hs.get(parent).getSpace() == hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getSpace())
                                spaces.put(hs.get(hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getParentSHA1s().get(k)).getSpace(), true);
                        }
                    }
                }
                
            }



            //if no parent, end of commits
            if(ngcommits.get(i).getParentSHA1s().isEmpty())
            {
                break;
            }
            //if more than 1 parent
            else if(ngcommits.get(i).getParentSHA1s().size() > 1)
            {
                //set first parent space
                if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getSpace() < 0)
                {
                    hs.get(ngcommits.get(i).getParentSHA1s().get(0)).setSpace(ngcommits.get(i).getSpace());
                }
                else if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getSpace() >= 0)
                {
                    if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getSpace() > ngcommits.get(i).getSpace())
                        hs.get(ngcommits.get(i).getParentSHA1s().get(0)).setSpace(ngcommits.get(i).getSpace());
                }
                
                //set second parent space
                //downfork
                if((hs.get(ngcommits.get(i).getParentSHA1s().get(1)).getSpace() > -1)
                        && (hs.get(ngcommits.get(i).getParentSHA1s().get(1)).getSpace() < ngcommits.get(i).getSpace()))
                    continue;
                
                //second parent has same parent
                String par1 = ngcommits.get(i).getParentSHA1s().get(0);
                String par2 = hs.get(ngcommits.get(i).getParentSHA1s().get(1)).getParentSHA1s().get(0);
                
                if(par1.equals(par2) && hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getSpace() < 0)
                    hs.get(ngcommits.get(i).getParentSHA1s().get(1)).setSpace(ngcommits.get(i).getSpace());                   
                //normal 
                else
                {
                    int s = getFreeSpace();
                    hs.get(ngcommits.get(i).getParentSHA1s().get(1)).setSpace(s); 
                }               
            }
            //if only 1 parent
            else
            {
                if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getSpace() < 0)
                {
                    hs.get(ngcommits.get(i).getParentSHA1s().get(0)).setSpace(ngcommits.get(i).getSpace());
                }
                else if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getSpace() >= 0)
                {
                    if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getSpace() > ngcommits.get(i).getSpace())
                        hs.get(ngcommits.get(i).getParentSHA1s().get(0)).setSpace(ngcommits.get(i).getSpace());
                }
            }                     
        }
    }
    
    private int getFreeSpace()
    {
        for(int i = 0; i < spaces.size(); i++)
            if(!spaces.get(i))
            {
                spaces.put(i ,true);
                return i;
            }
        return 0;
    }
    
    public void createGraph() throws IOException
    {
        //Collections.reverse(ngcommits);
        
        double stepSize = 30;
        double yOffset =  50;
        double xOffset = 50;
                
        HashMap<String, Integer> levels = new HashMap<>();
        int maxLev = 1;  
        levels.put(ngcommits.get(0).getSHA1(), maxLev);
        
        for(int i = 0; i < ngcommits.size(); i++)
            nodes.add(createSingleNode(i, nodes.size(), xOffset, yOffset * (ngcommits.get(i).getSpace()+1) * 0.75, stepSize));
        
        for(int i = 0; i < ngcommits.size(); i++)
            drawLine(ngcommits.get(i));
        
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
        boolean child1 = ngcommits.get(start).getChildrenSHA1s().size() > 1;
        boolean child2 = ngcommits.get(start + 1).getChildrenSHA1s().size() > 1;
        
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
            
            if(ngcommits.get(start).getParentSHA1s().size() < 1 || (start >= ngcommits.size() - 1))
                break;
            else {                            
                par1 = ngcommits.get(start).getParentSHA1s().size() > 1;
                par2 = ngcommits.get(start + 1).getParentSHA1s().size() > 1; 
                child1 = ngcommits.get(start).getChildrenSHA1s().size() > 1;
                child2 = ngcommits.get(start + 1).getChildrenSHA1s().size() > 1;
                auth = ngcommits.get(start).getAuthor()
                    .equals(ngcommits.get(start + 1).getAuthor()); 
                samePar = ngcommits.get(start).getParentSHA1s()
                    .equals(ngcommits.get(start + 1).getParentSHA1s());
            }
        }
        return start;
    }   
    
    private Circle createSingleNode(int i, double pos, double xOffset, double yOffset, double stepSize){
             
        Circle node = new Circle(pos * stepSize + xOffset,
                    yOffset,
                    5,
                    colors.get(ngcommits.get(i).getSpace()));
        
        Tooltip tt = new Tooltip("Author: " + ngcommits.get(i).getAuthor() + "\n\n"
                + "Commit Date: " + ngcommits.get(i).getDate() + "\n"
                + "SHA: " + ngcommits.get(i).getSHA1() + "\n"
                + "Commit Info: " + ngcommits.get(i).getMessage());  
        
        //getAvatar(tt, ngcommits.get(i));
        
        Tooltip.install(node, tt);
        
        posX.put(ngcommits.get(i).getSHA1(), node.getCenterX());
        posY.put(ngcommits.get(i).getSHA1(), node.getCenterY());
   
        return node;
    }
    
    private Circle createMultiNode(double pos, int start, int end, double xOffset, double yOffset, double stepSize)
    {        
        Circle node = new Circle(pos * stepSize + xOffset,
                    yOffset,
                    5,
                    colors.get(ngcommits.get(start).getSpace()));
        
        String toolTip = "Author: " + ngcommits.get(start).getAuthor() + "\n\n";
        for(int i = start; i <= end; i++)
        {
            toolTip += "Commit Date: " + ngcommits.get(i).getDate() + "\n"
                + "SHA: " + ngcommits.get(i).getSHA1() + "\n"
                + "Commit Info: " + ngcommits.get(i).getMessage() + "\n\n";
            
            posX.put(ngcommits.get(i).getSHA1(), node.getCenterX());
            posY.put(ngcommits.get(i).getSHA1(), node.getCenterY());
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
        for (int i = 0; i < maxSpaces; i++) {
            if (colors.get(i) == null) 
            {
                clr = Color.rgb(rand.nextInt((255 - 1) + 1) + 1,
                        rand.nextInt((255 - 1) + 1) + 1,
                        rand.nextInt((255 - 1) + 1) + 1);
                colors.put(i, clr);
            }
        }
    }
           
    private void drawLine(NGCommit commit) throws IOException
    {
            for(int j = 0; j < commit.getParentSHA1s().size(); j++)
            {                
                Line line1 = null;
                Line line2 = null;

                if(posY.get(commit.getParentSHA1s().get(j)) < posY.get(commit.getSHA1()))
                {
                    line1 = new Line(posX.get(commit.getParentSHA1s().get(j)),
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getParentSHA1s().get(j)) - 4,
                        posY.get(commit.getSHA1()));

                    line2 = new Line(posX.get(commit.getParentSHA1s().get(j)) - 4,
                        posY.get(commit.getSHA1()),
                        posX.get(commit.getSHA1()),
                        posY.get(commit.getSHA1()));
                }
                else if(posY.get(commit.getParentSHA1s().get(j)) > posY.get(commit.getSHA1()))
                {
                    line1 = new Line(posX.get(commit.getParentSHA1s().get(j)),
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getSHA1()) + 4,
                        posY.get(commit.getParentSHA1s().get(j)));

                    line2 = new Line(posX.get(commit.getSHA1()) + 4,
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getSHA1()),
                        posY.get(commit.getSHA1()));
                }
                else
                    line1 = new Line(posX.get(commit.getParentSHA1s().get(j)),
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getSHA1()),
                        posY.get(commit.getSHA1()));

                if(j == 0)
                {
                    line1.setStroke(colors.get(commit.getSpace()));
                    if(line2 != null)
                        line2.setStroke(colors.get(commit.getSpace()));
                }
                if(j == 1)
                {
                    line1.setStroke(colors.get(hs.get(commit.getParentSHA1s().get(j)).getSpace()));
                    if(line2 != null)
                        line2.setStroke(colors.get(hs.get(commit.getParentSHA1s().get(j)).getSpace()));
                }
                
                lines.add(line1);
                if(line2 != null)
                    lines.add(line2);
            }
    }
        
}
