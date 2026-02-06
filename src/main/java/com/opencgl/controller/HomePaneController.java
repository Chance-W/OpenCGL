package com.opencgl.controller;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.listener.Config;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.i18n.I18N;
import com.opencgl.util.NumberUtil;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/04 11:05
 * @since v2.0
 */
@SuppressWarnings("unused")
public class HomePaneController implements Initializable {
    private final Logger logger = LoggerFactory.getLogger(HomePaneController.class);

    @FXML
    protected BorderPane homeRootPane;
    @FXML
    protected Label timeLabel;
    @FXML
    protected Label softwareIntroduce;
    @FXML
    protected Label versionLabel;

    @FXML
    protected MFXButton jumpHomeBtn;
    @FXML
    protected Label mainTitleLabel;
    @FXML
    protected Label frameworkTitleLabel;
    @FXML
    protected Label suggestionTitleLabel;
    @FXML
    protected Label welcomeLabel;
    @FXML
    protected Label frameworkDescriptionLabel;
    @FXML
    protected Label guideTitleLabel;
    @FXML
    protected Label guideFirstLabel;
    @FXML
    protected Label guideSecondLabel;
    @FXML
    protected Label guideThirdLabel;
    @FXML
    protected Label contactDescriptionLabel;
    @FXML
    protected Label wechatKeyLabel;

    private Timeline clock;

    @FXML
    protected void jumpHomePage() throws URISyntaxException, IOException {
        Desktop desktop = Desktop.getDesktop();
        desktop.browse(new URI("https://www.tool-graphical.top/"));
    }

    private void showDate() {
        clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            int year = LocalDateTime.now().getYear();
            int month = LocalDateTime.now().getMonthValue();
            int day = LocalDateTime.now().getDayOfMonth();
            String week = LocalDateTime.now().getDayOfWeek().getDisplayName(TextStyle.FULL,
                    I18N.getLocale());
            int second = LocalDateTime.now().getSecond();
            int minute = LocalDateTime.now().getMinute();
            int hour = LocalDateTime.now().getHour();
            timeLabel.setText(year + com.opencgl.i18n.I18N.get("opencgl.home.year") +
                    NumberUtil.addZeroForNum(month, 2) + com.opencgl.i18n.I18N.get("opencgl.home.month") +
                    NumberUtil.addZeroForNum(day, 2) + com.opencgl.i18n.I18N.get("opencgl.home.day") + " " +
                    week + " " +
                    NumberUtil.addZeroForNum(hour, 2) + ":" +
                    NumberUtil.addZeroForNum(minute, 2) + ":" +
                    NumberUtil.addZeroForNum(second, 2));
        }),
                new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void bindHomeText() {
        softwareIntroduce.textProperty().bind(I18N.getBinding("opencgl.home.description"));
        versionLabel.textProperty().bind(I18N.getBinding(() -> I18N.get("opencgl.home.current_version",
                Config.readInternalConfigure(OpenCGLSelfProperties.CURRENT_VERSION_KEY))));
        welcomeLabel.textProperty().bind(I18N.getBinding("opencgl.home.welcome"));
        frameworkDescriptionLabel.textProperty().bind(I18N.getBinding("opencgl.home.framework_description"));
        guideTitleLabel.textProperty().bind(I18N.getBinding("opencgl.home.guide_title"));
        guideFirstLabel.textProperty().bind(I18N.getBinding("opencgl.home.guide_first"));
        guideSecondLabel.textProperty().bind(I18N.getBinding("opencgl.home.guide_second"));
        guideThirdLabel.textProperty().bind(I18N.getBinding("opencgl.home.guide_third"));
        contactDescriptionLabel.textProperty().bind(I18N.getBinding("opencgl.home.contact_description"));
        wechatKeyLabel.textProperty().bind(I18N.getBinding("opencgl.home.wechat"));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        bindHomeText();

        // 绑定静态标签
        jumpHomeBtn.textProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.home.jump_page"));
        mainTitleLabel.textProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.home.main_title"));
        frameworkTitleLabel.textProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.home.framework_title"));
        suggestionTitleLabel.textProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.home.suggestion_title"));

        showDate();

        homeRootPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null && clock != null) {
                clock.stop();
            } else if (newScene != null && clock != null && clock.getStatus() != Animation.Status.RUNNING) {
                clock.play();
            }
        });
    }
}
