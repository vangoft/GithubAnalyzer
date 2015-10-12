/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
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
    private Label infoDescription;
    @FXML
    private AnchorPane contentPane;
    @FXML
    private AnchorPane labelPane;
    @FXML
    private AnchorPane datePane;
    
    @FXML
    private ScrollPane labelScrollPane;    
    @FXML
    private ScrollPane dateScrollPane;
    @FXML
    private ScrollPane contentScrollPane;
    @FXML
    private BorderPane borderPane;
    
    HashMap<String, NetworkGraph> graphs = new HashMap();
    NetworkGraph ng;
        
    @FXML
    private void loadButtonAction(ActionEvent event) {
        try {
            
            if(graphs.containsKey(searchField.getText()))
            {
                ng = graphs.get(searchField.getText());
                infoName.setText(ng.getName());
                infoForks.setText(ng.getForks());
                infoDescription.setText(ng.getDesc());
            }
            else{
                GitHub github;
                github = GitHub.connect();
                GHRepository repo = github.getRepository(searchField.getText());
                List<GHRepository> forks = repo.listForks().asList();

                infoName.setText(repo.getFullName());
                infoForks.setText(String.valueOf(repo.listForks().asList().size()));       
                infoDescription.setText(repo.getDescription());

                ng = new NetworkGraph(repo.listCommits().asList(), forks, contentPane,
                    labelPane, datePane, contentScrollPane, labelScrollPane,
                    dateScrollPane, searchField.getText(), repo.getDescription());
                
                graphs.put(searchField.getText(), ng);                 
            }
            ng.drawGraph();                        
        } catch (Exception ex) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("error");
                alert.setHeaderText(null);
                alert.setContentText("repository not found. check the spelling.");
                alert.showAndWait();
                
                ex.printStackTrace();
        } 
    }
            
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    } 
   
    @FXML
    private void toggleMultiButtonAction(ActionEvent event){
        ng.toggleAllMulti();
    }
    
    @FXML
    private void toggleExpandButtonAction(ActionEvent event){
        ng.toggleCompact();
    }
    
}
