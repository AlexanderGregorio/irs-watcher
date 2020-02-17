package org.irs;

import groovy.util.logging.Slf4j;
import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import lombok.SneakyThrows;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Slf4j
public class Main extends JFrame {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static final String URL = "https://burghquayregistrationoffice.inis.gov.ie/Website/AMSREG/AMSRegWeb.nsf/(getAppsNear)?readform" +
            "&cat=All" +
            "&sbcat=All" +
            "&typ=New";

    private static final long SHORT_WAIT = TimeUnit.SECONDS.toMillis(30);
    private static final long LONG_WAIT = TimeUnit.SECONDS.toMillis(5);
    private static final LocalDateTime RELEASE_TIME = LocalDateTime.now()
            .withHour(10)
            .withMinute(0)
            .withSecond(0);

    private final AudioStream audioStream;

    public static void main(String[] args) {
        new Main().start();
    }

    @SneakyThrows
    public Main() throws HeadlessException {
        super();

        InputStream stream = getClass()
                .getClassLoader()
                .getResourceAsStream("alarm.mp3");
        Objects.requireNonNull(stream);
        audioStream = new AudioStream(stream);
    }

    @SneakyThrows
    public void start() {
        LocalDateTime end = LocalDateTime.now().plusHours(10);

        while(LocalDateTime.now().isBefore(end)) {
            if(!areThereAppointments()) {
                try {
                    log.info("Waiting");
                    Thread.sleep(getWait());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                playAlarm();
                break;
            }
        }
    }

    public boolean areThereAppointments() {
        log.info("Making request");
        return RestAssured.given()
                .filter(new ResponseLoggingFilter())
                .relaxedHTTPSValidation()
                .get(URL)
                .jsonPath()
                .getBoolean("empty");
    }

    public void playAlarm() {
        AudioPlayer.player.start(audioStream);
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(60));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        AudioPlayer.player.stop(audioStream);
    }

    public long getWait() {
        LocalDateTime now = LocalDateTime.now();

        if(now.isAfter(RELEASE_TIME.minusMinutes(2)) && now.isBefore(RELEASE_TIME.plusMinutes(2)))
            return SHORT_WAIT;
        return LONG_WAIT;
    }
 }
