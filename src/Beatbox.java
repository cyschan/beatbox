import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;

public class Beatbox { // Implement Controller Events listener interface

    static JFrame f = new JFrame("First Music Video");
    static MyDrawPanel ml;

    public static void main(String[] args) {
        Beatbox player = new Beatbox();
        player.go();
    }

    public void setUpGui() {
        ml = new MyDrawPanel();
        f.setContentPane(ml); //Set up a JPanel object and set in frame.  Alt of getContentFrame directly from frame.
        f.setBounds(30,30,300,300);
        f.setVisible(true);
    }

    public void go() {
        //Make series of MIDI messages and events
        //For each event: (1) make message instance, (2) set message, (3) make MidiEvent with message, (4) add event to track
        //Register a listener for events
        //Start sequencer playing
        setUpGui();
        try {
            Sequencer sequencer = MidiSystem.getSequencer();
            sequencer.open();
            //Register for events with the sequencer.  Takes listener and array to listen to. ml is the event listener
            //sequencer is event source
            sequencer.addControllerEventListener(ml, new int[] {127});
            Sequence seq = new Sequence(Sequence.PPQ, 4);
            Track track = seq.createTrack();

            int r = 0;
            for (int i = 5; i < 61; i += 4) { //Create MIDI Events and add to track using loop
                r = (int) ((Math.random() * 50) + 1);
                track.add(makeEvent(144, 1, r, 100, i));
                track.add(makeEvent(176,1,127,0,i)); //176 means ControllerEvent, with event argument 127.  127 is just placeholder event.
                track.add(makeEvent(128, 1, r, 100, i + 2));
            }
            sequencer.setSequence(seq);
            sequencer.setTempoInBPM(20);
            sequencer.start();
        } catch (Exception e) {
            e.printStackTrace();
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

    class MyDrawPanel extends JPanel implements ControllerEventListener { //sequencer will give events to this listener
        boolean msg = false;

        public void controlChange(ShortMessage event) {
            msg = true;
            repaint();
        }

        public void paintComponent(Graphics g) {
            if (msg) {
                Graphics2D g2 = (Graphics2D) g;

                int r = (int) (Math.random() * 250);
                int gr = (int) (Math.random() * 250);
                int b = (int) (Math.random() * 250);

                g.setColor(new Color(r,gr,b));

                int ht = (int) ((Math.random() * 120) + 10);
                int width = (int) ((Math.random() * 120) + 10);

                int x = (int) ((Math.random() * 40) + 10);
                int y = (int) ((Math.random() * 40) + 10);

                g.fillRect(x,y,ht,width); //Wipe out previously drawn graphics
                msg = false;
            }
        }
    }
}
