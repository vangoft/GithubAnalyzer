/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import org.kohsuke.github.*;

/**
 *
 * @author Alexander Distergoft
 */
public class Controller implements Initializable {
        
    //left side panel
    @FXML
    private TextField searchField;        
    @FXML
    private Label infoName;
    @FXML
    private Label infoForks;
    @FXML
    private Label infoBranches;
    @FXML
    private Label infoReleases;
    @FXML
    private Label infoContributors;
    @FXML
    private Label infoDescription;
    @FXML
    private AnchorPane contentPane;
        
    @FXML
    private void loadButtonAction(ActionEvent event) {
        GitHub github;
        try {
            github = GitHub.connect();
            GHRepository repo = github.getRepository(searchField.getText());
            
            infoName.setText(repo.getFullName());
            //infoForks.setText(String.valueOf(repo.listForks().asList().size()));
            //infoBranches.setText(String.valueOf(repo.getBranches().size()));
            //infoReleases.setText(String.valueOf(repo.listReleases().asList().size()));
            //infoContributors.setText(String.valueOf(repo.listContributors().asList().size()));            
            infoDescription.setText(repo.getDescription());
            
            List<GHRepository> forks = repo.listForks().asList();
            List<GHCommit> commits = repo.listCommits().asList();
            
            if(contentPane.getChildren() != null)
                contentPane.getChildren().removeAll(contentPane.getChildren());

            
            NetworkGraph ng = new NetworkGraph(commits, forks);
            ng.createGraph();
            
            Group content = new Group();
            content.getChildren().addAll(ng.getLines());
            content.getChildren().addAll(ng.getNodes());
            
            //contentPane.getChildren().addAll(ng.getLines());
            //contentPane.getChildren().addAll(ng.getNodes()); 
            
            contentPane.getChildren().add(content);
            
            contentPane.setOnScroll(
            new EventHandler<ScrollEvent>() {
              @Override
              public void handle(ScrollEvent event) {
                double zoomFactor = 1.05;
                double deltaY = event.getDeltaY();
                if (deltaY < 0){
                  zoomFactor = 2.0 - zoomFactor;
                }
                System.out.println(zoomFactor);
                content.setScaleX(content.getScaleX() * zoomFactor);
                content.setScaleY(content.getScaleY() * zoomFactor);
                event.consume();
              }
            });
                        

        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.WARNING, null, ex);
        }

    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }        
}
