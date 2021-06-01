package ru.kamuzta.beatboxswingclient;

//TODO добавить ограничения по колву символов в полях согласно БД
//TODO настроить часовой пояс при отображении времени в чате

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import javax.sound.midi.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.awt.event.*;
import java.net.*;
import java.util.List;
import javax.swing.event.*;

public class BBSwingClient {
    private String userName;
    private String serverIp;
    private String serverPort;
    private boolean connectionStatus;

    private JFrame theFrame;
    private JList<Message> incomingList;
    private JTextField userMessage;
    private ArrayList<JCheckBox> checkboxList;
    private Vector<Message> messages = new Vector<>();
    private HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;

    private final String[] instrumentNames = {
            "Bass Drum",
            "Closed Hi-Cat",
            "Open Hi-Cat",
            "Acoustic Snare",
            "Crash Cymbal",
            "Hand Clap",
            "High Tom",
            "Hi Bongo",
            "Maracas",
            "Whistle",
            "Low Conga",
            "Cowbell ",
            "Vibraslap",
            "Low-mid Tom",
            "High Agogo",
            "Open Hi Conga"
    };
    private final int[] instruments = {
            34,
            42,
            46,
            38,
            49,
            39,
            50,
            60,
            70,
            72,
            64,
            56,
            58,
            47,
            67,
            63
    };

    public BBSwingClient() {
        this.setupGui();
        this.setupMidi();
        new Thread(new RemoteReader()).start();
    }

    public static void main(String[] args) {
        BBSwingClient client = new BBSwingClient();
    }

    private void setupGui() {
        theFrame = new JFrame("BeatBox SwingClient");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        checkboxList = new ArrayList<>();

        JMenuBar menuBar = new JMenuBar();
        JMenuItem saveMenuItem = new JMenuItem("Save melody");
        saveMenuItem.addActionListener(new SaveMenuListener());
        JMenuItem loadMenuItem = new JMenuItem("Load melody");
        loadMenuItem.addActionListener(new LoadMenuListener());
        JMenuItem connectMenuItem = new JMenuItem("Connect to new server");
        connectMenuItem.addActionListener(new ConnectMenuListener());

        menuBar.add(connectMenuItem);
        menuBar.add(saveMenuItem);
        menuBar.add(loadMenuItem);

        theFrame.setJMenuBar(menuBar);

        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton play = new JButton("Play");
        play.addActionListener(new MyPlayListener());
        buttonBox.add(play);

        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton sendMessage = new JButton("Send Message");
        sendMessage.addActionListener(new MySendMessageListener());
        buttonBox.add(sendMessage);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        incomingList = new JList<>();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(messages);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);
        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        JPanel mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }

        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    private void setupMidi() {
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

    //Connect to REST server endpoint and check if connection is ok
    private String checkConnection() {
        StringBuilder response = new StringBuilder();
        String exceptions = "";
        int responseCode = 0;
        try {
            URL url = new URL("http://" + serverIp + ":" + serverPort + "/checkconnection");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            responseCode = connection.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            connection.disconnect();
        } catch (IOException e) {
            exceptions = e.getMessage();
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            connectionStatus = true;
            return response.toString();
        } else {
            connectionStatus = false;
            return "Can't connect\n" + exceptions;
        }

    }

    private void buildTrackAndStart() {
        ArrayList<Integer> trackList;
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<>();

            for (int j = 0; j < 16; j++) {
                JCheckBox jc = checkboxList.get(j + (16 * i));
                if (jc.isSelected()) {
                    int key = instruments[i];
                    trackList.add(key);
                } else {
                    trackList.add(null);
                }
            }
            makeTracks(trackList);
        }
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeSequence(boolean[] checkboxState) {
        for (int i = 0; i < 256; i++) {
            JCheckBox check = checkboxList.get(i);
            check.setSelected(checkboxState[i]);
        }
    }

    private void makeTracks(ArrayList<Integer> list) {
        for (int i = 0; i < 16; i++) {
            Integer num = list.get(i);
            if (num != null) {
                track.add(makeEvent(144, 9, num, 100, i));
                track.add(makeEvent(128, 9, num, 100, i + 1));
            }
        }

    }

    private MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }

    //Create message from userName, Date + text + melody and send to REST Server
    private class MySendMessageListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (connectionStatus) {
                boolean[] melodyToSend = new boolean[256];
                for (int i = 0; i < 256; i++) {
                    JCheckBox check = checkboxList.get(i);
                    if (check.isSelected()) {
                        melodyToSend[i] = true;
                    }
                }
                Message msg = new Message(LocalDateTime.now(), userName, userMessage.getText(), melodyToSend);
                StringWriter writer = new StringWriter();
                ObjectMapper mapper = getJsonMapper();

                try {
                    mapper.writeValue(writer, msg);
                    byte[] byteMsg = writer.toString().getBytes(StandardCharsets.UTF_8);

                    URL url = new URL("http://" + serverIp + ":" + serverPort + "/sendmessage");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setRequestProperty("Content-Length", Integer.toString(byteMsg.length));
                    connection.connect();
                    OutputStream os = connection.getOutputStream();
                    os.write(byteMsg);
                    os.flush();
                    os.close();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                        JOptionPane.showMessageDialog(theFrame, "Failed : HTTP error code : "
                                + connection.getResponseCode());
                    }
                    connection.disconnect();
                } catch (IOException malformedURLException) {
                    malformedURLException.printStackTrace();
                }
                userMessage.setText("");
            } else {
                JOptionPane.showMessageDialog(theFrame, "There is no connection to server!");
            }
        }
    }

    private class MyPlayListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            buildTrackAndStart();
        }
    }

    private class MyStopListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            sequencer.stop();
        }
    }

    private class MyUpTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    private class MyDownTempoListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));
        }
    }

    //play music from smbd's message
    private class MyListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                Message selected = incomingList.getSelectedValue();
                if (selected != null) {
                    boolean[] selectedState = otherSeqsMap.get(selected.toString());
                    sequencer.stop();
                    changeSequence(selectedState);
                    buildTrackAndStart();
                }
            }
        }
    }

    //save current melody to txt file
    private class SaveMenuListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileSave = new JFileChooser();
            fileSave.showSaveDialog(theFrame);
            saveFile(fileSave.getSelectedFile());
        }

        private void saveFile(File file) {
            boolean[] melodyArray = new boolean[256];
            for (int i = 0; i < 256; i++) {
                JCheckBox check = checkboxList.get(i);
                melodyArray[i] = check.isSelected();
            }
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                bw.write(Message.convertSenderMelodyToText(melodyArray));
                bw.close();
                JOptionPane.showMessageDialog(theFrame, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " melody has been saved to file " + file);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(theFrame, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " couldn't save the melody to file");
            }
        }
    }

    //load melody from txt file and play
    private class LoadMenuListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileLoad = new JFileChooser();
            fileLoad.showOpenDialog(theFrame);
            loadFile(fileLoad.getSelectedFile());
        }

        private void loadFile(File file) {
            StringBuilder stringBuilder = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                for (int i = 0; i < 16; i++) {
                    line = br.readLine();
                    stringBuilder.append(line);
                    if (i != 15) {
                        stringBuilder.append("\n");
                    }
                }
                br.close();
                sequencer.stop();
                changeSequence(Message.convertTextToSenderMelody(stringBuilder.toString()));
                buildTrackAndStart();

                JOptionPane.showMessageDialog(theFrame, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " melody has been loaded from file " + file);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(theFrame, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + " couldn't load the melody from " + file);
            }
        }
    }

    //Configure connection to new RESTful Beatbox Server
    private class ConnectMenuListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            userName = JOptionPane.showInputDialog(theFrame, "Enter your name:", "Andrey");
            serverIp = JOptionPane.showInputDialog(theFrame, "Enter server IP-address:", "beatboxrestfulserver.kamuzta.ru");
            serverPort = JOptionPane.showInputDialog(theFrame, "Enter server port:", "80");
            JOptionPane.showMessageDialog(theFrame, checkConnection());
            //clearing memory from old messages from previous server
            messages.clear();
            otherSeqsMap.clear();
            incomingList.setListData(messages);

        }
    }

    //Task for Thread to get messages from REST server every 2sec.
    private class RemoteReader implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (connectionStatus) {
                    List<Message> remoteMessages = getChat();
                    if (remoteMessages.size() > messages.size()) {
                        messages.clear();
                        otherSeqsMap.clear();
                        for (Message msg : remoteMessages) {
                            otherSeqsMap.put(msg.toString(), msg.getSenderMelody());
                        }
                        messages.addAll(remoteMessages);
                        incomingList.setListData(messages);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        //Connect to REST server, read array of Messages and pack them into List
        private List<Message> getChat() {
            List<Message> messages = new ArrayList<>();
            ObjectMapper mapper = getJsonMapper();
            StringBuilder stringBuilder = new StringBuilder();
            try {
                URL url = new URL("http://" + serverIp + ":" + serverPort + "/getchat");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json; charset=UTF-8");
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(streamReader);
                    String response;
                    while ((response = bufferedReader.readLine()) != null) {
                        stringBuilder.append(response).append("\n");
                    }
                    bufferedReader.close();
                    messages = mapper.readValue(stringBuilder.toString(), new TypeReference<List<Message>>() {
                    });

                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    System.out.println("Server returned no messages");
                } else {
                    System.out.println("Some error happened: " + responseCode);
                }

            } catch (IOException e) {
                JOptionPane.showMessageDialog(theFrame, "Connection losted!");
                connectionStatus = false;
            }

            return messages;
        }

    }

    //Create, configure and return ObjectMapper for JSON serialization
    private ObjectMapper getJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        return mapper;
    }
}



