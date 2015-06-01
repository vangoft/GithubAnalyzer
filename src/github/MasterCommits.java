/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package github;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineBuilder;

import org.kohsuke.github.GHCommit;

/**
 *
 * @author Alexander
 */
public class MasterCommits {
    
    private List<GHCommit> commits = new ArrayList<>();
    
    private List<Line> lines = new ArrayList<>();
    private List<Node> nodes = new ArrayList<>();
    
    public MasterCommits()
    {
        Line redLine = LineBuilder.create()
               .startX(0)
               .startY(0)
               .endX(50)
               .endY(140)
               .fill(Color.RED)
               .strokeWidth(3.0f)
               .translateY(20)
               .build();

        Line blackLine = LineBuilder.create()
               .startX(170)
               .startY(30)
               .endX(20)
               .endY(0)
               .fill(Color.BLACK)
               .strokeWidth(2.0f)
               .translateY(20)
               .build();

        lines.add(blackLine);
        lines.add(redLine);
    }
    
    public List getLines()
    {
        return lines;
    }
}
