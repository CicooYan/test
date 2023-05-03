package view;

import model.IndexNode;
import tool.ProcessTool;
import uk.co.caprica.vlcj.player.base.ControlsApi;
import uk.co.caprica.vlcj.player.base.StatusApi;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VideoPlayer extends JPanel {
    EmbeddedMediaPlayerComponent mediaPlayerComponent;

    JPanel buttonBar;
    JButton playButton;
    JButton pauseButton;
    JButton stopButton;

    List<Double> timeBoundaries = new ArrayList<>();
    Set<Double> timeSet = new HashSet<>();

    JPanel indexPanel;

    public void init(List<IndexNode> nodes, String videoUrl) {
        ;
        setLayout(new BorderLayout());

        initMedia();

        initButtons();

        initTree(nodes);

        mediaPlayerComponent.mediaPlayer().media().play(videoUrl);
    }

    private void initMedia() {
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        add(mediaPlayerComponent, BorderLayout.CENTER);
    }

    private void initButtons() {
        buttonBar = new JPanel();
        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");

        buttonBar.add(playButton);
        playButton.addActionListener(e -> playVideo());
        buttonBar.add(pauseButton);
        pauseButton.addActionListener(e -> pauseVideo());
        buttonBar.add(stopButton);
        stopButton.addActionListener(e -> stopVideo());
        add(buttonBar, BorderLayout.SOUTH);
    }

    private void initTree(List<IndexNode> nodes) {
        timeBoundaries = new ArrayList<>();
        timeSet = new HashSet<>();

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new IndexNode("Root", 0));

        for (IndexNode scene : nodes) {
            DefaultMutableTreeNode sceneNode = new DefaultMutableTreeNode(scene);
            addTimePoint(scene.getTime());
            rootNode.add(sceneNode);
            if (!scene.isLeaf()) {
                for (int j = 0; j < scene.getChildren().size(); j++) {
                    IndexNode shot = scene.getChildren().get(j);
                    DefaultMutableTreeNode shotNode = new DefaultMutableTreeNode(shot);
                    addTimePoint(shot.getTime());
                    sceneNode.add(shotNode);
                    if (!shot.isLeaf()) {
                        for (int k = 0; k < shot.getChildren().size(); k++) {
                            IndexNode subshot = shot.getChildren().get(k);
                            DefaultMutableTreeNode subshotNode = new DefaultMutableTreeNode(subshot);
                            addTimePoint(subshot.getTime());
                            shotNode.add(subshotNode);
                        }
                    }
                }
            }
        }

        JTree tree = new JTree(rootNode);
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.isLeaf()) {
                IndexNode nodeData = (IndexNode) selectedNode.getUserObject();
                playVideo(nodeData.getTime());
            }
        });
        indexPanel = new JPanel();
        indexPanel.add(tree);

        add(indexPanel, BorderLayout.WEST);
    }

    private void addTimePoint(double timePoint) {
        if (!timeSet.contains(timePoint)) {
            timeSet.add(timePoint);
            timeBoundaries.add(timePoint * 1000);
        }
    }

    private double findStopTime(double curTime) {
        double prevTimeBoundary = 0;
        for(double timeBoundary: timeBoundaries){
            if(timeBoundary > curTime){
                return prevTimeBoundary;
            }
            prevTimeBoundary = timeBoundary;
        }
        return timeBoundaries.get(timeBoundaries.size()-1);
    }

    private void pauseVideo() {
        mediaPlayerComponent.mediaPlayer().controls().pause();
    }

    private void playVideo() {
        mediaPlayerComponent.mediaPlayer().controls().play();
    }

    private void playVideo(double second) {
        mediaPlayerComponent.mediaPlayer().controls().setTime((long) second * 1000);
    }

    private void stopVideo() {
        // This is millisecond
        double curTime = mediaPlayerComponent.mediaPlayer().status().time();
        //System.out.println(findStopTime());
        mediaPlayerComponent.mediaPlayer().controls().setTime((long)(findStopTime(curTime)));
        mediaPlayerComponent.mediaPlayer().controls().pause();
    }
}
