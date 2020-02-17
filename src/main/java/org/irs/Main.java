package org.irs;

import groovy.util.logging.Slf4j;
import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.SneakyThrows;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Slf4j
public class Main {

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

    private final MediaPlayer mediaPlayer;

    public static void main(String[] args) {
        //Needed to start context for MediaPlayer
        JFXPanel fxPanel = new JFXPanel();
        new Main().start();
    }

    @SneakyThrows
    public Main() throws HeadlessException {
        super();

        String filePath = getClass()
                .getClassLoader()
                .getResource("alarm.mp3")
                .toURI()
                .toString();
        Media media = new Media(filePath);
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnEndOfMedia(() -> {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.play();
        });
    }

    @SneakyThrows
    public void start() {
        LocalDateTime end = LocalDateTime.now().plusHours(10);

        while (LocalDateTime.now().isBefore(end)) {
            if (areThereAppointments()) {
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
        mediaPlayer.play();
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(60));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mediaPlayer.stop();
    }

    public long getWait() {
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(RELEASE_TIME.minusMinutes(2)) && now.isBefore(RELEASE_TIME.plusMinutes(2)))
            return SHORT_WAIT;
        return LONG_WAIT;
    }
}
