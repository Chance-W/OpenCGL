package com.opencgl.controller;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.listener.Config;
import com.opencgl.model.OpenCGLSelfProperties;
import com.opencgl.util.NumberUtil;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/04 11:05
 * @since v9.0
 */
@SuppressWarnings("unused")
public class HomePaneController implements Initializable {
    private final Logger logger = LoggerFactory.getLogger(HomePaneController.class);

    @FXML
    protected BorderPane homeRootPane;
    @FXML
    protected Label timeLabel;
    @FXML
    protected VBox nodesList;
    @FXML
    protected MFXButton newButton;
    @FXML
    protected MFXButton fileButton;
    @FXML
    protected MFXButton commentButton;
    @FXML
    protected MFXButton filterButton;
    @FXML
    protected TextArea softwareIntroduce;

    @FXML
    protected TextArea softwareFrameworkIntroduce;

    @FXML
    protected TextArea softwareSuggestion;

    @FXML
    protected void jumpHomePage() throws URISyntaxException, IOException {
        Desktop desktop = Desktop.getDesktop();
        desktop.browse(new URI("https://www.tool-graphical.top/"));
    }

    private void showDate() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            int year = LocalDateTime.now().getYear();
            int month = LocalDateTime.now().getMonthValue();
            int day = LocalDateTime.now().getDayOfMonth();
            String week = LocalDateTime.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
            int second = LocalDateTime.now().getSecond();
            int minute = LocalDateTime.now().getMinute();
            int hour = LocalDateTime.now().getHour();
            timeLabel.setText(year + "年" +
                NumberUtil.addZeroForNum(month, 2) + "月" +
                NumberUtil.addZeroForNum(day, 2) + "日 " +
                week + " " +
                NumberUtil.addZeroForNum(hour, 2) + ":" +
                NumberUtil.addZeroForNum(minute, 2) + ":" +
                NumberUtil.addZeroForNum(second, 2));
        }),
            new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void setSoftIntroduce() {
        softwareIntroduce.setText("""
            一直在想，什么时候能将一些常用的工具集合到一起，并通过插件的方式扩展。看了一些网上类似的软件或工具，始终没法满足自己的所谓要求，于是便利用闲暇时间边学习，边CODE，所以就有了OpenCGL。
                        
            当前版本："""
            + Config.readInternalConfigure(OpenCGLSelfProperties.CURRENT_VERSION_KEY));
    }

    private void setSoftwareFrameworkIntroduce() {
        softwareFrameworkIntroduce.setText(
            """
                Zulu Jdk
                JavaFX
                MaterialFX
                FontAwesome
                ...
                """);
    }

    private void setSoftwareSuggestion() {
        softwareSuggestion.setText("""
            对于工具或者插件，如果你有什么想法，欢迎通过如下方式与我联系
            邮箱：chance.w@qq.com;chance_w@126.com
            微信号：Chance_W-
            钉钉号：xxx
            """);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setSoftIntroduce();
        setSoftwareFrameworkIntroduce();
        setSoftwareSuggestion();
        showDate();
    }
}
