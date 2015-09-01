/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import static java.util.Collections.list;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
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
    private final AnchorPane contentPane;
    
    private final List<Line> lines = new ArrayList<>();
    private final List<Shape> nodes = new ArrayList<>();
    private final HashMap<String, Double> posX = new HashMap<>();
    private final HashMap<String, Double> posY = new HashMap<>(); 
    private final ArrayList<Color> colors = new ArrayList();
    private final HashMap<Integer, Boolean> spaces = new HashMap<>();
    private static final int numSpaces = 10000;
    
    
    private final HashMap<String, List<NGCommit>> multiActive = new HashMap<>();
    private final HashMap<String, List<NGCommit>> multiInactive = new HashMap<>();
    
    private boolean compact = true;
    
    public NetworkGraph(List<GHCommit> commits, List<GHRepository> forks, AnchorPane cp)
    {
        processCommits(commits, forks);
        setColors();
        contentPane = cp;
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
        
        
        processExpandedSpaces(ngcommits, procOrder);
    }
    
    public void processExpandedSpaces(List<NGCommit> commits, List<String> procOrder)
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
                
        for(String owner : procOrder)
        {            
            //init spacing, first commit in current branch set to "0"
            for(int i = 0; i < ngcommits.size(); i++)
            {
                if(ngcommits.get(i).getOwner().equals(owner))
                {
                    ngcommits.get(i).setExpandedSpace(maxSpace);
                    tempMaxSpace = maxSpace;
                    
                    for(int j = 0; j <= ngcommits.get(i).getExpandedSpace(); j++)
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
                        if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getExpandedSpace() < 0)
                        {
                            hs.get(ngcommits.get(i).getParentSHA1s().get(0)).setExpandedSpace(ngcommits.get(i).getExpandedSpace());
                        }
                        else if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getExpandedSpace() >= 0)
                        {
                            if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getExpandedSpace() > ngcommits.get(i).getExpandedSpace())
                                hs.get(ngcommits.get(i).getParentSHA1s().get(0)).setExpandedSpace(ngcommits.get(i).getExpandedSpace());
                        }

                        //set second parent space
                        //downfork
                        if((hs.get(ngcommits.get(i).getParentSHA1s().get(1)).getExpandedSpace() > -1)
                                && (hs.get(ngcommits.get(i).getParentSHA1s().get(1)).getExpandedSpace() < ngcommits.get(i).getExpandedSpace()))
                            continue;

                        //second parent has same parent
                        String par1 = ngcommits.get(i).getParentSHA1s().get(0);
                        String par2 = hs.get(ngcommits.get(i).getParentSHA1s().get(1)).getParentSHA1s().get(0);

                        if(par1.equals(par2) && hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getExpandedSpace() < 0)
                            hs.get(ngcommits.get(i).getParentSHA1s().get(1)).setExpandedSpace(ngcommits.get(i).getExpandedSpace());                   
                        //normal 
                        else
                        {
                            int s = getFreeSpace();
                            if(s > tempMaxSpace)
                                tempMaxSpace = s;
                            hs.get(ngcommits.get(i).getParentSHA1s().get(1)).setExpandedSpace(s); 
                        }               
                    }
                    //if only 1 parent
                    else
                    {
                        if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getExpandedSpace() < 0)
                        {
                            hs.get(ngcommits.get(i).getParentSHA1s().get(0)).setExpandedSpace(ngcommits.get(i).getExpandedSpace());
                        }
                        else if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getExpandedSpace() >= 0)
                        {
                            if(hs.get(ngcommits.get(i).getParentSHA1s().get(0)).getExpandedSpace() > ngcommits.get(i).getExpandedSpace())
                                hs.get(ngcommits.get(i).getParentSHA1s().get(0)).setExpandedSpace(ngcommits.get(i).getExpandedSpace());
                        }
                    }                     
                }
                //update maxSpace for forks
                if(i == ngcommits.size() - 1)
                    maxSpace = tempMaxSpace + 1;
            }
        }
        
        processCompactSpaces(ngcommits, procOrder);
        
        try {
            processMultiCommits();
        } catch (IOException ex) {
            Logger.getLogger(NetworkGraph.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void processCompactSpaces(List<NGCommit> commits, List<String> order)
    {
        //space map
        List<Integer> spacing = new ArrayList();
        
        String owner;
        
        int currMax = -1;
        int currMin = -1;
        int currLast = -1;
        int currFirst = -1;
        
        Iterator<String> it = order.iterator();
        
        if(it.hasNext())
        {
            owner = it.next();
                        
            //master init
            for(int i = 0; i < commits.size(); i++)
            {
                if(owner.equals(commits.get(i).getOwner()))
                {
                    if(currMax < commits.get(i).getExpandedSpace())
                        currMax = commits.get(i).getExpandedSpace();
                    
                    //master has same spacing in expanded/compact form
                    commits.get(i).setCompactSpace(commits.get(i).getExpandedSpace());
                }
            }
            //init spacing for master
            for(int i = 0; i < commits.size(); i++)
                spacing.add(currMax);
            
            //forks
            while(it.hasNext())
            {
                owner = it.next();
                
                currMax = -1;
                currMin = -1;
                currLast = -1;
                currFirst = -1;
                    
                //get current owner's min/max/first/last values
                for(int i = 0; i < commits.size(); i++)
                {
                    if(owner.equals(commits.get(i).getOwner()))
                    {
                        if(currFirst < 0)
                            currFirst = i;
                        
                        if(currLast < i)
                            currLast = i;
                                                
                        if(currMin > commits.get(i).getExpandedSpace() || currMin < 0)
                            currMin = commits.get(i).getExpandedSpace();

                        if(currMax < commits.get(i).getExpandedSpace())
                            currMax = commits.get(i).getExpandedSpace();  
                    }
                }
                
                //compute difference to spacing
                int diff = currMin - (spacing.get(currFirst) + 1);
                
                for(int i = currFirst; i <= commits.indexOf(hs.get(commits.get(currLast).getParentSHA1s().get(0))); i++)
                {
                    if(owner.equals(commits.get(i).getOwner()))
                    {
                        commits.get(i).setCompactSpace(commits.get(i).getExpandedSpace() - diff);
                    }
                    
                    spacing.remove(i);
                    spacing.add(i, currMax - diff);
                                        
                }                
            }
        } 
        System.out.println("bla");
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
                if(hs.get(ng.getChildrenSHA1s().get(j)).getExpandedSpace() != ng.getExpandedSpace())
                    spaces.put(hs.get(ng.getChildrenSHA1s().get(j)).getExpandedSpace(), false);
            }
            else
            {
                if(hs.get(ng.getChildrenSHA1s().get(j)).getExpandedSpace() != ng.getExpandedSpace())
                    spaces.put(hs.get(ng.getChildrenSHA1s().get(j)).getExpandedSpace(), false);

                for(int k = 0; k < hs.get(ng.getChildrenSHA1s().get(j)).getParentSHA1s().size(); k++)
                {
                    String parent = hs.get(ng.getChildrenSHA1s().get(j)).getParentSHA1s().get(k);

                    if(!parent.equals(ng.getSHA1()) 
                            && hs.get(parent).getExpandedSpace() == hs.get(ng.getChildrenSHA1s().get(j)).getExpandedSpace())
                        spaces.put(hs.get(hs.get(ng.getChildrenSHA1s().get(j)).getParentSHA1s().get(k)).getExpandedSpace(), true);
                }
            }
        }
    }
    
    public void drawGraph() throws IOException
    {       
        double stepSize = 30;
        double yOffset =  50;
        double xOffset = 50;
                        
        //clear nodes and lines
        nodes.clear();
        lines.clear();
        
        
        for(int i = ngcommits.size() - 1; i >= 0; i--)
        {            
            if(!multiActive.containsKey(ngcommits.get(i).getSHA1()) && compact)
                nodes.add(createSingleNode(i, nodes.size(), xOffset, yOffset * (ngcommits.get(i).getExpandedSpace() + 1) * 0.75, stepSize));
            else if(!multiActive.containsKey(ngcommits.get(i).getSHA1()) && !compact)
                nodes.add(createSingleNode(i, nodes.size(), xOffset, yOffset * (ngcommits.get(i).getCompactSpace() + 1) * 0.75, stepSize));
            else{
                int multiSize = i - multiActive.get(ngcommits.get(i).getSHA1()).size() + 1;
                if(compact)
                    nodes.add(createMultiNode2(nodes.size(),i ,multiSize, xOffset, yOffset * (ngcommits.get(i).getExpandedSpace() + 1) * 0.75, stepSize));
                else
                    nodes.add(createMultiNode2(nodes.size(),i ,multiSize, xOffset, yOffset * (ngcommits.get(i).getCompactSpace() + 1) * 0.75, stepSize));
                i = multiSize;
            }                    
        }
        
        for(int i = 0; i < ngcommits.size(); i++)
            createLine(ngcommits.get(i));
        
        //erase contentPane content
         if(contentPane.getChildren() != null)
            contentPane.getChildren().removeAll(contentPane.getChildren());

        //add to contentpane
        Group content = new Group();
        content.getChildren().addAll(lines);
        content.getChildren().addAll(nodes);

        contentPane.getChildren().add(content);      
        
        contentPane.setOnScroll(
            (ScrollEvent event) -> {
                double zoomFactor = 1.05;
                double deltaY = event.getDeltaY();
                if (deltaY < 0){
                    zoomFactor = 2.0 - zoomFactor;
                }
                //System.out.println(zoomFactor);
                content.setScaleX(content.getScaleX() * zoomFactor);
                content.setScaleY(content.getScaleY() * zoomFactor);
                event.consume();
        });
        
    }
    
    /* put all multicommits in map for expansion/compression */
    public void processMultiCommits() throws IOException
    {
        int multiCnt = 0;
        for(int i = ngcommits.size() - 1; i > 0; i--)
        {
            multiCnt = checkMultiNode(i);
            
            if(multiCnt == i)
                continue;
            
            else
            {
                List<NGCommit> multi = new ArrayList<>();
                
                for(int j = i; j >= multiCnt; j--)
                {
                    multi.add(ngcommits.get(j));
                }
                
                multiActive.put(ngcommits.get(i).getSHA1(), multi); 
                i = multiCnt;
            }                    
        }
    }
    
    public void toggleAllMulti()
    {
        if(!multiActive.isEmpty())
        {
            List<List<NGCommit>> active = new ArrayList<>();
            
            Iterator it = multiActive.entrySet().iterator();
            while (it.hasNext()) 
            {
               Map.Entry pair = (Map.Entry)it.next();
               active.add((List) pair.getValue());
               it.remove();
            }
            
            for(int i = 0; i < active.size(); i++)
            {
                for(int j = 0; j < active.get(i).size(); j++)
                {
                    multiInactive.put(active.get(i).get(j).getSHA1(), active.get(i));
                }
            }
            
            multiActive.clear();
            
            try {
                drawGraph();
            } catch (IOException ex) {
                Logger.getLogger(NetworkGraph.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else if(!multiInactive.isEmpty())
        {
            List<List<NGCommit>> inactive = new ArrayList<>();
            
            Iterator it = multiInactive.entrySet().iterator();
            while (it.hasNext()) 
            {
               Map.Entry pair = (Map.Entry)it.next();
               inactive.add((List) pair.getValue());
               it.remove();
            }
            
            for(int i = 0; i < inactive.size(); i++)
            {
                if(!multiActive.containsKey(inactive.get(i).get(0).getSHA1()))
                    multiActive.put(inactive.get(i).get(0).getSHA1(), inactive.get(i));
            }
            
            multiInactive.clear();
            
            try {
                drawGraph();
            } catch (IOException ex) {
                Logger.getLogger(NetworkGraph.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void toggleSingleMulti(String sha)
    {
        //expansion
        if(multiActive.containsKey(sha))
        {
            List<NGCommit> active = multiActive.get(sha);
            
            //add multiActive content to multiInactive
            for(int i = 0; i < active.size(); i++)            
                multiInactive.put(active.get(i).getSHA1(), active);
            
            
            //remove multiActive content
            multiActive.remove(sha);
            
            try {
                drawGraph();
            } catch (IOException ex) {
                Logger.getLogger(NetworkGraph.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else if(multiInactive.containsKey(sha))
        {
            List<NGCommit> inactive = multiInactive.get(sha);
            
            //add multiInactive to multiActive
            multiActive.put(inactive.get(0).getSHA1(), inactive);
            
            //remove multiInactive content
            for(int i = 0; i < inactive.size(); i++)
                multiInactive.remove(inactive.get(i).getSHA1());
                
            try {
                drawGraph();
            } catch (IOException ex) {
                Logger.getLogger(NetworkGraph.class.getName()).log(Level.SEVERE, null, ex);
            }            
        }
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
        boolean space = ngcommits.get(start).getExpandedSpace() == 
                    ngcommits.get(start - 1).getExpandedSpace();

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
                space = ngcommits.get(start).getExpandedSpace() == 
                    ngcommits.get(start - 1).getExpandedSpace();                
            }
        }
        return start;
    } 
    
    public void toggleCompact()
    {
        //if is compact, shrink
        if(compact)
            compact = false;

        //else expand
        else
            compact = true;
        
        try {
            drawGraph();
        } catch (IOException ex) {
            Logger.getLogger(NetworkGraph.class.getName()).log(Level.SEVERE, null, ex);
        }     
    }
    
    private Circle createSingleNode(int i, double pos, double xOffset, double yOffset, double stepSize){
             
        Circle node = new Circle(pos * stepSize + xOffset,
                    yOffset,
                    5,
                    colors.get(ngcommits.get(i).getExpandedSpace() % 8));
        
        Tooltip tt = new Tooltip("Author: " + ngcommits.get(i).getAuthor() + "\n\n"
                + "Commit Date: " + ngcommits.get(i).getDate() + "\n"
                + "SHA: " + ngcommits.get(i).getSHA1() + "\n"
                + "Commit Info: " + ngcommits.get(i).getMessage());  
        
        //getAvatar(tt, ngcommits.get(i));
        
        Tooltip.install(node, tt);
        node.setId(ngcommits.get(i).getSHA1());
        
        
        //click event for expansion/compression
        

        node.setOnMouseClicked(e -> {
            System.out.println(node.getId());
            toggleSingleMulti(node.getId());
            e.consume();
        });
        
        
        posX.put(ngcommits.get(i).getSHA1(), node.getCenterX());
        posY.put(ngcommits.get(i).getSHA1(), node.getCenterY());
   
        return node;
    }
    
    private Rectangle createMultiNode2(double pos, int start, int end, double xOffset, double yOffset, double stepSize)
    {      
        Rectangle rec = new Rectangle(pos * stepSize + xOffset - 5,
                    yOffset - 5,
                    10,10);
        rec.setStroke(colors.get(ngcommits.get(start).getExpandedSpace() % 8));
        rec.setFill(colors.get(ngcommits.get(start).getExpandedSpace() % 8));
                
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
        
        rec.setId(ngcommits.get(start).getSHA1());
        
        rec.setOnMouseClicked(e -> {
            System.out.println(rec.getId());
            toggleSingleMulti(rec.getId());
            e.consume();
        });
        
        Tooltip.install(rec, tt);
        
        return rec;
    }
    
    private Circle createMultiNode(double pos, int start, int end, double xOffset, double yOffset, double stepSize)
    {        
        Circle node = new Circle(pos * stepSize + xOffset,
                    yOffset,
                    5,
                    colors.get(ngcommits.get(start).getExpandedSpace() % 8));
        
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
           
    private void createLine(NGCommit commit) throws IOException
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
                    line1.setStroke(colors.get(commit.getExpandedSpace() % 8));
                    if(line2 != null)
                        line2.setStroke(colors.get(commit.getExpandedSpace() % 8));
                    if(line3 != null)
                        line3.setStroke(colors.get(commit.getExpandedSpace() % 8));
                }
                if(j == 1)
                {
                    line1.setStroke(colors.get(hs.get(commit.getParentSHA1s().get(j)).getExpandedSpace() % 8));
                    if(line2 != null)
                        line2.setStroke(colors.get(hs.get(commit.getParentSHA1s().get(j)).getExpandedSpace() % 8));
                    if(line3 != null)
                        line2.setStroke(colors.get(hs.get(commit.getParentSHA1s().get(j)).getExpandedSpace() % 8));
                }
                
                lines.add(line1);
                if(line2 != null)
                    lines.add(line2);
                if(line3 != null)
                    lines.add(line3);
            }
    }
        
}
