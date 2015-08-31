/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
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
    
    
    /* filechooser stuff */
    final FileChooser fileChooser = new FileChooser();
    private String filePath;
    private FileSaver fileSaver;
    
    NetworkGraph ng;
        
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
            
            /* store commits and forks in FileSaver object */
            fileSaver = new FileSaver(repo.listCommits().asList(),
                    repo.listForks().asList());
            
            //fileSaver.setForks(repo.listForks().asList());
            //fileSaver.setCommits(repo.listCommits().asList());
            //List<GHRepository> forks = repo.listForks().asList();
            //List<GHCommit> commits = repo.listCommits().asList();
            
            if(contentPane.getChildren() != null)
                contentPane.getChildren().removeAll(contentPane.getChildren());

            loadNetworkGraph();                        

        } catch (Exception ex) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("error");
                alert.setHeaderText(null);
                alert.setContentText("repository not found. check the spelling.");
                alert.showAndWait();
                
                ex.printStackTrace();
        } 
    }
    
    
    @FXML
    private void openFileButtonAction(ActionEvent event) {        
        File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
                    if (file != null) {
                        filePath = file.getAbsolutePath();
                    }       
    }
    
    @FXML
    private void loadFileButtonAction(ActionEvent event) throws IOException, ClassNotFoundException {   
        if(filePath == null)
        {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("error");
            alert.setHeaderText(null);
            alert.setContentText("no file has been selected.\nopen a file first before loading it.");
            alert.showAndWait();
            
            return;
        }
        
        try(ObjectInputStream inFile = new ObjectInputStream(new FileInputStream(filePath)))
        {
            //ObjectInputStream inFile = new ObjectInputStream(new FileInputStream(filePath));
            fileSaver = (FileSaver)inFile.readObject();
            loadNetworkGraph();            
        }
        catch(ClassNotFoundException cnfe)
        {
            cnfe.printStackTrace();
        }
        catch(FileNotFoundException fnfe)
        {
            fnfe.printStackTrace();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }      
    }
    
    @FXML
    private void saveFileButtonAction(ActionEvent event) throws IOException {

        File file = fileChooser.showSaveDialog(contentPane.getScene().getWindow());
            if (file != null) {
                try(ObjectOutputStream write = new ObjectOutputStream (new FileOutputStream(file.getAbsolutePath())))
                {
                    //ObjectOutputStream write = new ObjectOutputStream (new FileOutputStream(file.getAbsolutePath()));
                    write.writeObject(fileSaver);
                }
                catch(NotSerializableException nse)
                {
                    nse.printStackTrace();
                }
                catch(IOException ioe)
                {
                    ioe.printStackTrace();
                }
                
            }  
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    } 
    
    private void loadNetworkGraph()
    {
        //erase contentPane content
        if(contentPane.getChildren() != null)
            contentPane.getChildren().removeAll(contentPane.getChildren());
            
        ng = new NetworkGraph(fileSaver.getCommits(), fileSaver.getForks(), contentPane);
        try {
            ng.drawGraph();
        } catch (IOException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }
            
    }    
}
