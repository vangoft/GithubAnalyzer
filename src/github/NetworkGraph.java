/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

/**
 *
 * @author Alexander Distergoft
 */
public class NetworkGraph {
    
    private final List<NGCommit> ngcommits = new ArrayList();
    private final HashMap<String, NGCommit> hs = new HashMap();
    private final Pane contentPane;
    private final Pane labelPane;
    private final Pane datePane;
    
    private final ScrollPane contentScrollPane;
    private final ScrollPane labelScrollPane;
    private final ScrollPane dateScrollPane;
    
    private final List<Line> lines = new ArrayList<>();
    private final List<Group> nodes = new ArrayList<>();
    private final List<Rectangle> outlines = new ArrayList<>();
    
    private final HashMap<String, Double> posX = new HashMap<>();
    private final HashMap<String, Double> posY = new HashMap<>(); 
    private final ArrayList<Color> colors = new ArrayList();
    private final HashMap<Integer, Boolean> spaces = new HashMap<>();
    private static final int numSpaces = 10000;    
    
    private final HashMap<String, List<NGCommit>> multiActive = new HashMap<>();
    private final HashMap<String, List<NGCommit>> multiInactive = new HashMap<>();
    
    private final HashMap<String, String> txtLabels = new HashMap<>();
    private final ArrayList<Integer> labelOrderSpacing = new ArrayList();
    
    private boolean compact = false;
    
    public NetworkGraph(List<GHCommit> commits, List<GHRepository> forks, Pane cp, Pane lp, Pane dp, ScrollPane csp, ScrollPane lsp, ScrollPane dsp)
    {
        processCommits(commits, forks);
        setColors();
        contentPane = cp;
        labelPane = lp;
        datePane = dp;
        contentScrollPane = csp;
        labelScrollPane = lsp;
        dateScrollPane = dsp;
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
               
        //prepare procesSpaces-order (first master then forks)
        for(NGCommit com : ngcommits)
            if(!procOrder.contains(com.getOwner()))
                procOrder.add(com.getOwner());
        
        processLabels(ngcommits);
        processExpandedSpaces(ngcommits, procOrder);
    }
    
    private void processLabels(List<NGCommit> commits)            
    {
        for(NGCommit com : commits)
            if(!txtLabels.containsKey(com.getOwner()))
                txtLabels.put(com.getOwner(), com.getSHA1());                
    }
    
    private void processExpandedSpaces(List<NGCommit> commits, List<String> procOrder)
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
                {
                    maxSpace = tempMaxSpace + 1;
                }
            }
            labelOrderSpacing.add(maxSpace);
        }
        
        processCompactSpaces(ngcommits, procOrder);
        
        try {
            processMultiCommits();
        } catch (IOException ex) {
            Logger.getLogger(NetworkGraph.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void processCompactSpaces(List<NGCommit> commits, List<String> order)
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
        double yOffset =  30;
        double xOffset = 30; //compact ? 50 : 150;
                        
        //clear nodes and lines
        nodes.clear();
        lines.clear();
        outlines.clear();
        
        //create all nodes
        for(int i = ngcommits.size() - 1; i >= 0; i--)
        {            
            if(!multiActive.containsKey(ngcommits.get(i).getSHA1()) && !compact)
                nodes.add(createSingleNode(i, nodes.size(), xOffset, yOffset * (ngcommits.get(i).getExpandedSpace() + 1) * 0.75, stepSize));
            else if(!multiActive.containsKey(ngcommits.get(i).getSHA1()) && compact)
                nodes.add(createSingleNode(i, nodes.size(), xOffset, yOffset * (ngcommits.get(i).getCompactSpace() + 1) * 0.75, stepSize));
            else{
                int multiSize = i - multiActive.get(ngcommits.get(i).getSHA1()).size() + 1;
                if(!compact)
                    nodes.add(createMultiNode(nodes.size(),i ,multiSize, xOffset, yOffset * (ngcommits.get(i).getExpandedSpace() + 1) * 0.75, stepSize));
                else
                    nodes.add(createMultiNode(nodes.size(),i ,multiSize, xOffset, yOffset * (ngcommits.get(i).getCompactSpace() + 1) * 0.75, stepSize));
                i = multiSize;
                xOffset += 30;
            }                    
        }
        
        //create all lines
        for(int i = 0; i < ngcommits.size(); i++)
            createLine(ngcommits.get(i));
        
        //create outlines for multicommits
        createAllOutlines();
        
        //erase contentPane content
         if(contentPane.getChildren() != null)
            contentPane.getChildren().removeAll(contentPane.getChildren());
         
        //erase contentPane content
         if(labelPane.getChildren() != null)
            labelPane.getChildren().removeAll(labelPane.getChildren());
         
        //erase contentPane content
         if(datePane.getChildren() != null)
            datePane.getChildren().removeAll(datePane.getChildren());

        //group elements for contentpane
        Group content = new Group();
        content.getChildren().addAll(lines);
        content.getChildren().addAll(nodes);
        content.getChildren().addAll(outlines);
        content.getChildren().add(createTextLabels());
        contentPane.getChildren().add(content);

        
        //regroup for background width
        Group contentA = new Group();
        if(!compact)
            contentA.getChildren().add(createForkBackground(contentPane));
        contentA.getChildren().addAll(content.getChildren());
        
        //erase contentPane again
         if(contentPane.getChildren() != null)
            contentPane.getChildren().removeAll(contentPane.getChildren());
        
        //add content to panes
        contentPane.getChildren().add(contentA);
        datePane.getChildren().add(createDateLine(contentPane));        
        if(!compact)
            labelPane.getChildren().add(createSideLabels(labelPane));
        
        //bind label pane v scroll pos to content pane v scroll pos
        DoubleProperty vPosition = new SimpleDoubleProperty();
            vPosition.bind(contentScrollPane.vvalueProperty());
            vPosition.addListener(new ChangeListener() {
                @Override
                public void changed(ObservableValue arg0, Object arg1, Object arg2) {
                labelScrollPane.setVvalue((double) arg2);
                }
         }); 

        //bind date pane h scroll pos to content pane h scroll pos               
        DoubleProperty hPosition = new SimpleDoubleProperty();
            hPosition.bind(contentScrollPane.hvalueProperty());
            hPosition.addListener(new ChangeListener() {
                @Override
                public void changed(ObservableValue arg0, Object arg1, Object arg2) {
                dateScrollPane.setHvalue((double) arg2);
                }
         }); 
        
        //add zoom to elements in contentpane
        contentPane.setOnScroll(
            (ScrollEvent event) -> {
                double zoomFactor = 1.05;
                double deltaY = event.getDeltaY();
                if (deltaY < 0){
                    zoomFactor = 2.0 - zoomFactor;
                }
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
    
    /* toggles all multi commits if available */
    public void toggleAllMulti()
    {
        //open if compressed multicommits are available
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
        }
        
        //close if uncompressed multicommits are available
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
        }
        //redraw graph
        try {
            drawGraph();
        } catch (IOException ex) {
            Logger.getLogger(NetworkGraph.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private Group createSingleNode(int i, double pos, double xOffset, double yOffset, double stepSize){
        
        Group group = new Group();
        Circle node = new Circle(pos * stepSize + xOffset,
                    yOffset,
                    5,
                    getColor(ngcommits.get(i)));
        
        
        Tooltip tt = new Tooltip("Author: " + ngcommits.get(i).getAuthor() + "\n\n"
                + "Commit Date: " + ngcommits.get(i).getDate() + "\n"
                + "SHA: " + ngcommits.get(i).getSHA1() + "\n"
                + "Commit Info: " + ngcommits.get(i).getMessage());  
        
        //getAvatar(tt, ngcommits.get(i));
        
        Tooltip.install(node, tt);
        node.setId(ngcommits.get(i).getSHA1());
        
        
        //click event for expansion/compression
        node.setOnMouseClicked(e -> {
            toggleSingleMulti(node.getId());
            e.consume();
        });
                
        posX.put(ngcommits.get(i).getSHA1(), node.getCenterX());
        posY.put(ngcommits.get(i).getSHA1(), node.getCenterY());
   
        group.getChildren().add(node);
        
        return group;
    }
    
    private Rectangle createOutline(double x1, double x2, int space){
        int yOffset = 30;
                
        Rectangle rec = new Rectangle();
        rec.setX(x1 - 6);
        rec.setY((yOffset * (space + 1) * 0.75) - 6);
        rec.setHeight(12);
        rec.setWidth(x2 - x1 + 12);
        rec.setArcHeight(10);
        rec.setArcWidth(10);
        rec.setStroke(colors.get(space % 8));
        rec.getStrokeDashArray().addAll(2d, 5d);
        rec.setFill(null);
        return rec;
    }
    
    private void createAllOutlines()
    {
        List<NGCommit> multi;            
        Iterator it = multiInactive.entrySet().iterator();
        HashSet<String> sha1 = new HashSet();
        
        while (it.hasNext()) 
        {
            Map.Entry pair = (Map.Entry)it.next();
            multi = (List<NGCommit>) pair.getValue();
            
            if(sha1.contains(multi.get(0).getSHA1()))
                continue;

            outlines.add(createOutline(
                    posX.get(multi.get(0).getSHA1()),
                    posX.get(multi.get(multi.size() - 1).getSHA1()),
                    compact ? multi.get(0).getCompactSpace() : multi.get(0).getExpandedSpace())); 
            
            for(NGCommit ng : multi)
               sha1.add(ng.getSHA1());
        }
    }
    
    private Group createMultiNode(double pos, int start, int end, double xOffset, double yOffset, double stepSize)
    {      
        Group multi = new Group();
        
        Rectangle rec = new Rectangle(pos * stepSize + xOffset - 5,
                    yOffset - 5,
                    40,10);
        rec.setArcHeight(10);
        rec.setArcWidth(10);
        
        rec.setStroke(getColor(ngcommits.get(start)));
        rec.setFill(getColor(ngcommits.get(start)));
        
        String toolTip = "Author: " + ngcommits.get(start).getAuthor() + "\n\n";
        for(int i = start; i >= end; i--)
        {
            toolTip += "Commit Date: " + ngcommits.get(i).getDate() + "\n"
                + "SHA: " + ngcommits.get(i).getSHA1() + "\n"
                + "Commit Info: " + ngcommits.get(i).getMessage() + "\n\n";
            
            posX.put(ngcommits.get(i).getSHA1(), rec.getX() + 5);
            posY.put(ngcommits.get(i).getSHA1(), rec.getY() + 5);
            ngcommits.get(i).setMulti(true);
        }
        Tooltip tt = new Tooltip(toolTip);   
        //getAvatar(tt, ngcommits.get(start));
        
        rec.setId(ngcommits.get(start).getSHA1());
        
        rec.setOnMouseClicked(e -> {
            toggleSingleMulti(rec.getId());
            e.consume();
        });
        
        Tooltip.install(rec, tt);
        
        Text cnt = new Text(Integer.toString(start - end + 1));
        cnt.setWrappingWidth(40);
        cnt.setTextAlignment(TextAlignment.CENTER);
        cnt.setFill(Color.WHITE);
        cnt.setStroke(Color.WHITE);
        cnt.setX(rec.getX());
        cnt.setY(rec.getY() + 9);
        
        cnt.setOnMouseClicked(e -> {
            toggleSingleMulti(rec.getId());
            e.consume();
        });
        
        multi.getChildren().addAll(rec, cnt);
        
        return multi;
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
    
    private Color getColor(NGCommit ng)
    {
        if(compact)            
            return colors.get(ng.getCompactSpace() % 8);
        else
            return colors.get(ng.getExpandedSpace() % 8);
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
                    line1.setStroke(getColor(commit));
                    if(line2 != null)
                        line2.setStroke(getColor(commit));
                    if(line3 != null)
                        line3.setStroke(getColor(commit));  
                }
                if(j == 1)
                {
                    line1.setStroke(getColor(hs.get(commit.getParentSHA1s().get(j))));
                    if(line2 != null)
                        line2.setStroke(getColor(hs.get(commit.getParentSHA1s().get(j))));
                    if(line3 != null)
                        line3.setStroke(getColor(hs.get(commit.getParentSHA1s().get(j))));
                }
                
                lines.add(line1);
                if(line2 != null)
                    lines.add(line2);
                if(line3 != null)
                    lines.add(line3);
            }
    }

    private Group createTextLabels()
    {
        Group grp = new Group(); 
        String owner;   
        String sha1;
        Double offset;
        
        Iterator it = txtLabels.entrySet().iterator(); 
        while (it.hasNext()) 
        {
            Map.Entry pair = (Map.Entry)it.next();
            owner = (String) pair.getKey();
            sha1 = (String) pair.getValue();
            
            double x  = posX.get(sha1);
            double y = posY.get(sha1);
            
            String[] parts = owner.split("/");            
            Text txt = new Text("\u25CF " + parts[0]);
            txt.setFill(getColor(hs.get(sha1)));            
            offset = hs.get(sha1).getMulti() ? 40.0 : 10.0;
            offset = multiInactive.containsKey(sha1) ? 10.0 : offset;       
            
            txt.setX(x + offset);
            txt.setY(y + 3);
            
            grp.getChildren().add(txt);            
        }        
        return grp;
    }    
    
    public Group createSideLabels(Pane pane)
    {
        Group grpTxt = new Group();
        Group recs = new Group();
        String owner = "";   
        String sha1 = "";
        
        Iterator it = txtLabels.entrySet().iterator();        
        while (it.hasNext()) 
        {
            Map.Entry pair = (Map.Entry)it.next();
            owner = (String) pair.getKey();
            sha1 = (String) pair.getValue();
            
            double x  = posX.get(sha1);
            double y = posY.get(sha1);
            
            //text label
            String[] parts = owner.split("/");            
            Text txt = new Text("\u25CF " + parts[0]);
            txt.setFill(getColor(hs.get(sha1)));
            txt.setX(5);
            txt.setY(y + 3);
            
            grpTxt.getChildren().add(txt);
        }
        
        int yOffset = 30;
        double startY = 1;

        boolean test = false;
        
        for(int i = 0; i < labelOrderSpacing.size(); i++)
        {
            Rectangle rec = new Rectangle();
            rec.setX(1);
            rec.setY(startY);
            rec.widthProperty().bind(pane.prefWidthProperty());
            rec.setHeight((yOffset * (labelOrderSpacing.get(i) + 1) * 0.75) - 12 - startY);
            rec.setFill(test ? null : Color.gray(.90));
            rec.setStroke(test ? null : Color.gray(.90));

            test = !test;
            startY = (yOffset * (labelOrderSpacing.get(i) + 1) * 0.75) - 12;

            recs.getChildren().add(rec);
        }
        recs.getChildren().addAll(grpTxt.getChildren());
        return recs;
    }
    
    private Group createForkBackground(Pane pane)
    {
        int yOffset = 30;
        double startY = 1;

        boolean test = false;
        Group recs = new Group();
        for(int i = 0; i < labelOrderSpacing.size(); i++)
        {            
            Rectangle rec = new Rectangle();
            rec.setX(0);
            rec.setY(startY);
            rec.setWidth(pane.getBoundsInLocal().getMaxX());
            //rec.widthProperty().bind(pane.widthProperty());
            rec.setHeight((yOffset * (labelOrderSpacing.get(i) + 1) * 0.75) - 12 - startY);
            rec.setFill(test ? Color.gray(.95) : Color.gray(.90));
            rec.setStroke(test ? Color.gray(.95) : Color.gray(.90));

            test = !test;
            startY = (yOffset * (labelOrderSpacing.get(i) + 1) * 0.75) - 12;

            recs.getChildren().add(rec);
        }
        
        return recs;
    }
    
    private Group createDateLine(Pane pane)
    {
        Group dateLine = new Group(); 
        
        //initialize width for correct scroll sync
        Rectangle rec = new Rectangle(0,0,pane.getBoundsInLocal().getMaxX(), 12);
        rec.setFill(Color.gray(.90));
        rec.setStroke(Color.gray(.90));
        dateLine.getChildren().add(rec);      
       
        rec = new Rectangle(0,13,pane.getBoundsInLocal().getMaxX(), 15);
        rec.setFill(Color.gray(1));
        rec.setStroke(Color.gray(1));
        dateLine.getChildren().add(rec);     
        
        rec = new Rectangle(0,30,pane.getBoundsInLocal().getMaxX(), 15);
        rec.setFill(Color.gray(.90));
        rec.setStroke(Color.gray(.90));
        dateLine.getChildren().add(rec); 
        
        Date currDate;
        Date prevDate = new Date();
        prevDate.setYear(0);
        prevDate.setMonth(0);
        prevDate.setDate(1);
        
        Text year, month, day;
        
        //create all nodes
        for(int i = ngcommits.size() - 1; i >= 0; i--)
        {            
            if(!multiActive.containsKey(ngcommits.get(i).getSHA1()))
            {
                currDate = ngcommits.get(i).getDate();
                if(currDate.getYear() > prevDate.getYear())
                    dateLine.getChildren().add(drawDate(currDate, ngcommits.get(i), true, true, true, false, 0));  

                if(currDate.getMonth() > prevDate.getMonth())
                    dateLine.getChildren().add(drawDate(currDate, ngcommits.get(i), false, true, true, false, 0));  

                if(currDate.getDate() > prevDate.getDate())
                    dateLine.getChildren().add(drawDate(currDate, ngcommits.get(i), false, false, true, false, 0));
                
                prevDate = currDate;
            }            
            else
            {
                int multiSize = i - multiActive.get(ngcommits.get(i).getSHA1()).size() + 1;                
                List<NGCommit> tmp = multiActive.get(ngcommits.get(i).getSHA1());
                
                List<NGCommit> multi = new ArrayList<>();
                multi.add(tmp.get(0));
                multi.add(tmp.get(tmp.size() - 1));
                
                for(int j = 0; j < multi.size(); j++)
                {
                    currDate = multi.get(j).getDate();
                    
                    if(currDate.getYear() > prevDate.getYear())
                        dateLine.getChildren().add(drawDate(currDate, multi.get(j), true, true, true, true, j));  

                    if(currDate.getMonth() > prevDate.getMonth())
                        dateLine.getChildren().add(drawDate(currDate, multi.get(j), false, true, true, true, j));  

                    if(currDate.getDate() > prevDate.getDate())
                        dateLine.getChildren().add(drawDate(currDate, multi.get(j), false, false, true, true, j));

                    prevDate = currDate;
                }
                
                i = multiSize;
            }                    
        }         
        return dateLine;
    }
    
    private Group drawDate(Date currDate, NGCommit ng, boolean y, boolean m, boolean d, boolean multi, int j)
    {
        Group grp = new Group();
        ArrayList<String> months = new ArrayList<>();
        months.addAll(Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"));
        
        if(y)
        {
            Text year = new Text(Integer.toString(currDate.getYear() + 1900));
            year.setX(posX.get(ng.getSHA1()) + (multi ? (j * 30) : 0));
            year.setY(10);  
            grp.getChildren().add(year);
        }
                
        if(m)
        {
            Text month = new Text(months.get(currDate.getMonth()));
            month.setX(posX.get(ng.getSHA1()) + (multi ? (j * 30) : 0));
            month.setY(25);
            
            grp.getChildren().add(month);
        }
        
        if(d)
        {
            Text day = new Text(Integer.toString(currDate.getDate()));
            day.setX(posX.get(ng.getSHA1()) + (multi ? (j * 30) : 0));
            day.setY(40);  
            
            grp.getChildren().add(day);
        }
        
        if(y || m || d)
        {
            Line line = new Line(posX.get(ng.getSHA1()) + (multi ? (j * 30) : 0), 
                45,
                posX.get(ng.getSHA1()) + (multi ? (j * 30) : 0),
                47);
            grp.getChildren().add(line);
        }
        
        return grp;
    }
}
