package ru.kamuzta.beatboxswingclient;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

@JsonAutoDetect
public class Message implements Comparable<Message> {
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

    public Message(LocalDateTime senderTime, String senderName, String senderMessage, boolean[] senderMelody) {
        setSenderTime(senderTime);
        setSenderName(senderName);
        setSenderMessage(senderMessage);
        setSenderMelody(senderMelody);
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
        return getSenderTime().equals(message.getSenderTime()) &&
                getSenderName().equals(message.getSenderName()) &&
                getSenderMessage().equals(message.getSenderMessage()) &&
                Arrays.equals(getSenderMelody(), message.getSenderMelody());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getSenderTime(), getSenderName(), getSenderMessage());
        result = 31 * result + Arrays.hashCode(getSenderMelody());
        return result;
    }

    //To storage messages in order
    @Override
    public int compareTo(Message o) {
        int result;

        if (this.equals(o)) {
            result = 0;
        } else if ((result = this.getSenderTime().compareTo(o.getSenderTime())) == 0) {
            if ((result = this.getSenderName().compareTo(o.getSenderName())) == 0) {
                result = this.getSenderMessage().compareTo(o.getSenderMessage());
            }
        }
        return result;
    }
}


