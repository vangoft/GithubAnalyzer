/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

/**
 *
 * @author Alexander Distergoft
 */
public class NetworkGraph {
    
    private final List<NGCommit> ngcommits = new ArrayList();
    private final HashMap<String, NGCommit> hs = new HashMap();
    
    private final List<Line> lines = new ArrayList<>();
    private final List<Shape> nodes = new ArrayList<>();
    private final HashMap<String, Double> posX = new HashMap<>();
    private final HashMap<String, Double> posY = new HashMap<>(); 
    //private final HashMap<Integer, Color> colors = new HashMap<>();
    private final ArrayList<Color> colors = new ArrayList();
    private final HashMap<Integer, Boolean> spaces = new HashMap<>();
    private static final int numSpaces = 10000;
    
    
    public NetworkGraph(List<GHCommit> commits, List<GHRepository> forks)
    {
        processCommits(commits, forks);
        setColors();
    }
    
    private void processCommits(List<GHCommit> commits, List<GHRepository> forks){
        HashMap<String, NGCommit> hm = new HashMap();
        
        //processing order for space processing
        //set first commit of master as the first to be processed
        List<String> procOrder = new ArrayList();
        procOrder.add(commits.get(0).getOwner().getFullName());
        
        //put all commits with new format in hashmap
        for(GHCommit com : commits)
            hm.put(com.getSHA1(), new NGCommit(com));
        
        
        //put fork data in hashmap
        for(GHRepository fork: forks)
            for(GHCommit com : fork.listCommits().asList())
                if(!hm.containsKey(com.getSHA1()))
                {
                    hm.put(com.getSHA1(), new NGCommit(com));
                    commits.add(com);
                }
        
        //add missing parent/child information
        //reverse list so that earlier date children come first
        Collections.reverse(commits);
        for(GHCommit com : commits)
            if(com.getParentSHA1s().size() > 0)
                for(String sha1 : com.getParentSHA1s())
                    hm.get(sha1).addChildSHA1(com.getSHA1());        
        
        //undo reverse and add all new commits into list
        Collections.reverse(commits);
        for(GHCommit com: commits)
            ngcommits.add(hm.get(com.getSHA1()));                  
                
        //sort new commit list by date
        Collections.sort(ngcommits, new Comparator<NGCommit>() {
            public int compare(NGCommit o1, NGCommit o2) {
                return o2.getDate().compareTo(o1.getDate());
            }
        });
        
//        for(NGCommit com: ngcommits)
//            System.out.println(com.getDate() + " " + com.getOwner() + " " + com.getSHA1());
//        
        //prepare procesSpaces-order (first master then forks)
        for(NGCommit com : ngcommits)
            if(!procOrder.contains(com.getOwner()))
                procOrder.add(com.getOwner());
        
        
        processSpaces(ngcommits, procOrder);
    }
    
    public void processSpaces(List<NGCommit> commits, List<String> order)
    {
        //put all commits with new format in hashmap
        for(NGCommit com : commits)
            hs.put(com.getSHA1(), com);
        
        //current max space
        int maxSpace = 0;
        int tempMaxSpace = 0;
        int cnt = 0;
        
        //init maxSpacing, all false besides 1st
        for(int i = 0; i < numSpaces; i++)
            spaces.put(i,false); 
                
        for(String owner : order)
        {            
            //init spacing, first commit in current branch set to "0"
            for(int i = 0; i < ngcommits.size(); i++)
            {
                if(ngcommits.get(i).getOwner().equals(owner))
                {
                    ngcommits.get(i).setSpace(maxSpace);
                    tempMaxSpace = maxSpace;
                    
                    for(int j = 0; j <= ngcommits.get(i).getSpace(); j++)
                    {
                        spaces.put(j,true);
                    }                    
                    cnt = i;
                    break;
                }
            }

            for(int i = cnt; i < ngcommits.size(); i++)
            {
                if(ngcommits.get(i).getOwner().equals(owner))
                {
                    //remove empty vertical space
                    if(ngcommits.get(i).getChildrenSHA1s().size() > 1)
                    {
                        clearSpaces(ngcommits.get(i));
                    }

                    //if no parent, end of commits
                    if(ngcommits.get(i).getParentSHA1s().isEmpty())
                    {
                        maxSpace = tempMaxSpace + 1;
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
                            if(s > tempMaxSpace)
                                tempMaxSpace = s;
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
                //update maxSpace for forks
                if(i == ngcommits.size() - 1)
                    maxSpace = tempMaxSpace + 1;
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
    
    private void clearSpaces(NGCommit ng)
    {
        for(int j = 0; j < ng.getChildrenSHA1s().size(); j++)
        {
            if(hs.get(ng.getChildrenSHA1s().get(j)).getParentSHA1s().size() < 2)
            {
                if(hs.get(ng.getChildrenSHA1s().get(j)).getSpace() != ng.getSpace())
                        //&& hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getSpace() != -1)
                    spaces.put(hs.get(ng.getChildrenSHA1s().get(j)).getSpace(), false);
            }
            else
            {
                if(hs.get(ng.getChildrenSHA1s().get(j)).getSpace() != ng.getSpace())
                        //&& hs.get(ngcommits.get(i).getChildrenSHA1s().get(j)).getSpace() != -1)
                    spaces.put(hs.get(ng.getChildrenSHA1s().get(j)).getSpace(), false);

                for(int k = 0; k < hs.get(ng.getChildrenSHA1s().get(j)).getParentSHA1s().size(); k++)
                {
                    String parent = hs.get(ng.getChildrenSHA1s().get(j)).getParentSHA1s().get(k);

                    if(!parent.equals(ng.getSHA1()) 
                            && hs.get(parent).getSpace() == hs.get(ng.getChildrenSHA1s().get(j)).getSpace())
                        spaces.put(hs.get(hs.get(ng.getChildrenSHA1s().get(j)).getParentSHA1s().get(k)).getSpace(), true);
                }
            }
        }
    }
    
    public void createGraph() throws IOException
    {
        //Collections.reverse(ngcommits);
        
        double stepSize = 30;
        double yOffset =  50;
        double xOffset = 50;
        int multiCnt = 0;
                        
        for(int i = ngcommits.size() - 1; i >= 0; i--)
        {
            multiCnt = checkMultiNode(i);
            
            if(multiCnt == i)
                nodes.add(createSingleNode(i, nodes.size(), xOffset, yOffset * (ngcommits.get(i).getSpace() + 1) * 0.75, stepSize));
            
            else{
                nodes.add(createMultiNode2(nodes.size(),i ,multiCnt, xOffset, yOffset * (ngcommits.get(i).getSpace() + 1) * 0.75, stepSize));
                i = multiCnt;
            }                    
        }
        
        for(int i = 0; i < ngcommits.size(); i++)
            drawLine(ngcommits.get(i));
        
    }
                
    private int checkMultiNode(int i)
    {
        int start = i;
       
        if(start - 1 < 0)
            return start;
        
        //has multiple parents?
        boolean par1 = ngcommits.get(start).getParentSHA1s().size() > 1;
        boolean par2 = ngcommits.get(start - 1).getParentSHA1s().size() > 1; 
        
        //has multiple children?
        boolean child1 = ngcommits.get(start).getChildrenSHA1s().size() > 1;
        boolean child2 = ngcommits.get(start - 1).getChildrenSHA1s().size() > 1;
        
        //has same space?
        boolean space = ngcommits.get(start).getSpace() == 
                    ngcommits.get(start - 1).getSpace();

        while((!par1) && (!par2)
                && (!child1) && (!child2)
                && (space))
        {
            start--;
            
            if(ngcommits.get(start).getParentSHA1s().size() < 1 || (start <=  0))
                break;
            else {                            
                par1 = ngcommits.get(start).getParentSHA1s().size() > 1;
                par2 = ngcommits.get(start - 1).getParentSHA1s().size() > 1; 
                child1 = ngcommits.get(start).getChildrenSHA1s().size() > 1;
                child2 = ngcommits.get(start - 1).getChildrenSHA1s().size() > 1;
                space = ngcommits.get(start).getSpace() == 
                    ngcommits.get(start - 1).getSpace();                
            }
        }
        return start;
    }   
    
    private Circle createSingleNode(int i, double pos, double xOffset, double yOffset, double stepSize){
             
        Circle node = new Circle(pos * stepSize + xOffset,
                    yOffset,
                    5,
                    colors.get(ngcommits.get(i).getSpace() % 8));
        
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
    
    private Rectangle createMultiNode2(double pos, int start, int end, double xOffset, double yOffset, double stepSize)
    {      
        Rectangle rec = new Rectangle(pos * stepSize + xOffset - 5,
                    yOffset - 5,
                    10,10);
        rec.setStroke(colors.get(ngcommits.get(start).getSpace() % 8));
        rec.setFill(colors.get(ngcommits.get(start).getSpace() % 8));
                
        String toolTip = "Author: " + ngcommits.get(start).getAuthor() + "\n\n";
        for(int i = start; i >= end; i--)
        {
            toolTip += "Commit Date: " + ngcommits.get(i).getDate() + "\n"
                + "SHA: " + ngcommits.get(i).getSHA1() + "\n"
                + "Commit Info: " + ngcommits.get(i).getMessage() + "\n\n";
            
            posX.put(ngcommits.get(i).getSHA1(), rec.getX() + 5);
            posY.put(ngcommits.get(i).getSHA1(), rec.getY() + 5);
        }
        Tooltip tt = new Tooltip(toolTip);   
        
        //getAvatar(tt, ngcommits.get(start));
        
        Tooltip.install(rec, tt);
        
        return rec;
    }
    
    private Circle createMultiNode(double pos, int start, int end, double xOffset, double yOffset, double stepSize)
    {        
        Circle node = new Circle(pos * stepSize + xOffset,
                    yOffset,
                    5,
                    colors.get(ngcommits.get(start).getSpace() % 8));
        
        String toolTip = "Author: " + ngcommits.get(start).getAuthor() + "\n\n";
        for(int i = start; i >= end; i--)
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
        colors.add(Color.BLACK);
        colors.add(Color.CORNFLOWERBLUE);
        colors.add(Color.ORANGE);
        colors.add(Color.LIGHTSEAGREEN);       
        colors.add(Color.MEDIUMORCHID);
        colors.add(Color.PLUM);
        colors.add(Color.SALMON);
        colors.add(Color.SLATEBLUE);
    }
           
    private void drawLine(NGCommit commit) throws IOException
    {
            for(int j = 0; j < commit.getParentSHA1s().size(); j++)
            {                
                Line line1;
                Line line2 = null;
                Line line3 = null;
                
                //downmerge
                if((j == 1) && posY.get(commit.getParentSHA1s().get(j)) < posY.get(commit.getSHA1()))
                {
                    line1 = new Line(posX.get(commit.getParentSHA1s().get(j)),
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getParentSHA1s().get(j)) + 4,
                        posY.get(commit.getSHA1()) - 10);

                    line2 = new Line(posX.get(commit.getParentSHA1s().get(j)) + 4,
                        posY.get(commit.getSHA1()) - 10,
                        posX.get(commit.getSHA1()) - 4,
                        posY.get(commit.getSHA1()) - 10);
                    
                    line3 = new Line(posX.get(commit.getSHA1()) - 4,
                        posY.get(commit.getSHA1()) - 10,
                        posX.get(commit.getSHA1()),
                        posY.get(commit.getSHA1()));
                }
                //downfork
                else if(posY.get(commit.getParentSHA1s().get(j)) < posY.get(commit.getSHA1()))
                {
                    line1 = new Line(posX.get(commit.getParentSHA1s().get(j)),
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getParentSHA1s().get(j)) + 4,
                        posY.get(commit.getSHA1()));

                    line2 = new Line(posX.get(commit.getParentSHA1s().get(j)) + 4,
                        posY.get(commit.getSHA1()),
                        posX.get(commit.getSHA1()),
                        posY.get(commit.getSHA1()));
                }
                //upmerge
                else if(posY.get(commit.getParentSHA1s().get(j)) > posY.get(commit.getSHA1()))
                {
                    line1 = new Line(posX.get(commit.getParentSHA1s().get(j)),
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getSHA1()) - 4,
                        posY.get(commit.getParentSHA1s().get(j)));

                    line2 = new Line(posX.get(commit.getSHA1()) - 4,
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getSHA1()),
                        posY.get(commit.getSHA1()));
                }
                //simple line
                else
                    line1 = new Line(posX.get(commit.getParentSHA1s().get(j)),
                        posY.get(commit.getParentSHA1s().get(j)),
                        posX.get(commit.getSHA1()),
                        posY.get(commit.getSHA1()));
                
                //color the lines
                if(j == 0)
                {
                    line1.setStroke(colors.get(commit.getSpace() % 8));
                    if(line2 != null)
                        line2.setStroke(colors.get(commit.getSpace() % 8));
                    if(line3 != null)
                        line3.setStroke(colors.get(commit.getSpace() % 8));
                }
                if(j == 1)
                {
                    line1.setStroke(colors.get(hs.get(commit.getParentSHA1s().get(j)).getSpace() % 8));
                    if(line2 != null)
                        line2.setStroke(colors.get(hs.get(commit.getParentSHA1s().get(j)).getSpace() % 8));
                    if(line3 != null)
                        line2.setStroke(colors.get(hs.get(commit.getParentSHA1s().get(j)).getSpace() % 8));
                }
                
                lines.add(line1);
                if(line2 != null)
                    lines.add(line2);
                if(line3 != null)
                    lines.add(line3);
            }
    }
        
}
