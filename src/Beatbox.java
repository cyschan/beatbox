import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileOutputStream;
import java.util.*;
import java.io.*;

public class Beatbox { // Implement Controller Events listener interface

    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList; //Store checkbox in ArrayList
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;
    final int beats = 16;
    boolean[] checkboxState;
    String userName;
    ObjectOutputStream in, out;


    String[] instrumentNames =
            {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Share", "Crash Cymbal", "Hand Clap", "High Tom",
            "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo",
            "Open Hi Conga"};
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args) {
        new Beatbox().buildGUI();
    }

    public void buildGUI() {
        theFrame = new JFrame("Cyber Beatbox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout); //Create a new buffered JPanel with the specified layout manager
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); //Gives border margin

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS); //Y Axis for vertical stack
        //Box class was designed to be a simple, and slightly more efficient, substitute for a JPanel with a BoxLayout
        //It doesn't support everything that JPanel does (eg, borders)

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton save = new JButton("Save");
        save.addActionListener(new MySendListener());
        buttonBox.add(save);

        JButton load = new JButton("Load");
        load.addActionListener(new MyReadInListener());
        buttonBox.add(load);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (String instrument : instrumentNames) {
            nameBox.add(new Label(instrument));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background); //add JPanel background to JFrame

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel); // Add JPanel Main on top of JPanel background in Centre

        //Make 256 checkboxes, set to unselected by default, add to ArrayList and GUI
        for (int i = 0; i <256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();

        theFrame.setBounds(50,50,300,300); //Moves and resizes this component
        theFrame.pack(); //Causes this Window to be sized to fit the preferred size and layouts of its subcomponents.
        theFrame.setVisible(true);
    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Turn checkbox states into MIDI events.  Add them to Track.
    //Make 16-element array to hold values for one instrument across 16 beats
    public void buildTrackAndStart() {
        int[] trackList = null;

        // Clear any old tracks
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        //Create track for each instrument (ie each row)
        for (int i = 0; i < instrumentNames.length; i++) {
            trackList = new int[instruments.length];

            //Set the key.  Represented by actual MIDI number of each instrument
            int key = instruments[i];

            //Go through each beat, for each instrument (hence nested loops)
            //If instrument should play on that beat, value will be the key, else is 0
            for (int j = 0; j < beats; j++) {
                // Cast list object to JCheckBox.  List index is 16*16 totalling 256.
                JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
                if (jc.isSelected()) {
                    trackList[j] = key;
                } else {
                    trackList[j] = 0;
                }
            }
            //TrackList now contains all keys, ready to add to track.
            makeTracks(trackList);

            //Force make event for all 16 beats
            //176 is ControllerEvent, with event number 127. 127 is placeholder event
            //Control Change (176) to turn off all sound after tick 16
            track.add(makeEvent(176,1,127,0, beats ));
        }
        // 192 is instrument change event
        // Reads: change instrument on channel 9 to instrument 1 at tick 15.
        // Nothing really happens except an event is fired
        // Required to ensure player goes through all beats before starting over
        track.add(makeEvent(192,9,1,0,15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start(); //Start Playing
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Implement listener for buttons. Trigger buildTrackAndStart upon click
    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
                    sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * .97));
        }
    }

    //Make events for one instrument at a time for 16 beats
    //Takes a list, each index in list will hold key for that instrument or 0
    public void makeTracks(int[] list) {
        for (int i = 0; i < beats; i++) {
            int key = list[i];
            if (key != 0 ) {
                track.add(makeEvent(144,9,key,100,i)); //Note On
                track.add(makeEvent(144,9,key,100,i + i)); //Note Off
            }
        }
    }

    public static MidiEvent makeEvent (int command, int channel, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(command, channel, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) {}
        return event;
    }

    public class MySendListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            checkboxState = new boolean[instrumentNames.length * beats];
            for (int i = 0; i < instrumentNames.length * beats; i++) {
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (check.isSelected()) {
                    checkboxState[i] = true;
                }
            }
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(theFrame);
            saveFile(fileSave.getSelectedFile(), checkboxState);
        }
    }

    public void saveFile(File file, boolean[] checkboxList) {
        try {
            FileOutputStream fileStream = new FileOutputStream(file);
            ObjectOutputStream os = new ObjectOutputStream(fileStream);
            os.writeObject(checkboxList);
        } catch (IOException e) {
            System.out.println("Error writing file");
            e.printStackTrace();
        }
    }

    public class MyReadInListener implements ActionListener {
        public void actionPerformed (ActionEvent ev) {
            JFileChooser fileOpen = new JFileChooser();
            fileOpen.showOpenDialog(theFrame);
            loadFile(fileOpen.getSelectedFile());
        }
    }

    private void loadFile(File file) {
        boolean[] checkboxInputList = null;
        try {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream is = new ObjectInputStream(fileIn);
            checkboxInputList = (boolean[]) is.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < instrumentNames.length * beats; i++) {
            JCheckBox check = (JCheckBox) checkboxList.get(i);
            if (checkboxInputList[i]) {
                check.setSelected(true);
            } else {
                check.setSelected(false);
            }
        }
        sequencer.stop();
        buildTrackAndStart();
    }
}
