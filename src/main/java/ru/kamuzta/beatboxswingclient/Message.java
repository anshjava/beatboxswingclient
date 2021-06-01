package ru.kamuzta.beatboxswingclient;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

@JsonAutoDetect
public class Message implements Comparable<Message> {
    private int id;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime senderTime;
    private String senderName;
    private String senderMessage;
    private boolean[] senderMelody;

    //no args constructor for JSON
    public Message() {

    }

    public void setSenderTime(LocalDateTime senderTime) {
        this.senderTime = senderTime;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setSenderMessage(String senderMessage) {
        this.senderMessage = senderMessage;
    }

    public void setSenderMelody(boolean[] senderMelody) {
        this.senderMelody = senderMelody;
    }

    public LocalDateTime getSenderTime() {
        return this.senderTime;
    }

    public String getSenderName() {
        return this.senderName;
    }

    public String getSenderMessage() {
        return this.senderMessage;
    }

    public boolean[] getSenderMelody() {
        return this.senderMelody;
    }

    public static String convertSenderMelodyToText(boolean[] melodyArray) {
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    sb.append(melodyArray[j + (16 * i)]);
                    if (j != 15) {
                        sb.append(";");
                    }
                }
                if (i != 15) {
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
        }
        return sb.toString();
    }

    public static boolean[] convertTextToSenderMelody(String text) {
        text = text.replaceAll("\n",";");
        String[] flags = text.split(";");
        boolean[] result = new boolean[flags.length];
        for (int i = 0; i < flags.length; i++) {
            result[i] = Boolean.parseBoolean(flags[i]);
        }
        return result;
    }

    public Message(LocalDateTime senderTime, String senderName, String senderMessage, boolean[] senderMelody) {
        setSenderTime(senderTime);
        setSenderName(senderName);
        setSenderMessage(senderMessage);
        setSenderMelody(senderMelody);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.getSenderTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " : " + this.getSenderName() + " : " + this.getSenderMessage();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id == message.id &&
                Objects.equals(senderTime, message.senderTime) &&
                Objects.equals(senderName, message.senderName) &&
                Objects.equals(senderMessage, message.senderMessage) &&
                Arrays.equals(senderMelody, message.senderMelody);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, senderTime, senderName, senderMessage);
        result = 31 * result + Arrays.hashCode(senderMelody);
        return result;
    }

    //To storage messages in order
    @Override
    public int compareTo(Message o) {
        int result;

        if (this.equals(o)) {
            result = 0;
        } else if ((result = Integer.compare(this.getId(), o.getId())) == 0) {
            if ((result = this.getSenderTime().compareTo(o.getSenderTime())) == 0) {
                if ((result = this.getSenderName().compareTo(o.getSenderName())) == 0) {
                    result = this.getSenderMessage().compareTo(o.getSenderMessage());
                }
            }
        }
        return result;
    }


}
