package com.example.filerename.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.Objects;

public final class ToastManager {

    private static final Duration SHOW_DURATION = Duration.millis(220);
    private static final Duration HIDE_DURATION = Duration.millis(170);
    private static final Duration DISPLAY_DURATION = Duration.seconds(3.4);

    private final Region container;
    private final Label titleLabel;
    private final Label iconLabel;
    private final Label messageLabel;

    private PauseTransition dismissDelay;
    private Animation activeAnimation;

    public ToastManager(
            Region container,
            Label titleLabel,
            Label iconLabel,
            Label messageLabel
    ) {
        this.container = Objects.requireNonNull(container, "container");
        this.titleLabel = Objects.requireNonNull(titleLabel, "titleLabel");
        this.iconLabel = Objects.requireNonNull(iconLabel, "iconLabel");
        this.messageLabel = Objects.requireNonNull(messageLabel, "messageLabel");

        container.setOnMouseClicked(event -> hide());
        container.setOnMouseEntered(event -> {
            if (dismissDelay != null) {
                dismissDelay.pause();
            }
        });
        container.setOnMouseExited(event -> {
            if (dismissDelay != null && container.isVisible()) {
                dismissDelay.play();
            }
        });
    }

    public void show(String message, Type type) {
        Objects.requireNonNull(type, "type");
        stopCurrentAnimation();

        container.getStyleClass().removeAll(
                Type.SUCCESS.cssClass,
                Type.ERROR.cssClass,
                Type.INFO.cssClass
        );
        container.getStyleClass().add(type.cssClass);

        titleLabel.setText(type.title);
        iconLabel.setText(type.icon);
        messageLabel.setText(Objects.requireNonNullElse(message, ""));

        container.setManaged(true);
        container.setVisible(true);
        container.toFront();
        container.setOpacity(0);
        container.setTranslateX(28);
        container.setTranslateY(-6);
        container.setScaleX(0.98);
        container.setScaleY(0.98);

        FadeTransition fadeIn = new FadeTransition(SHOW_DURATION, container);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slideIn = new TranslateTransition(SHOW_DURATION, container);
        slideIn.setToX(0);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleIn = new ScaleTransition(SHOW_DURATION, container);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        activeAnimation = new ParallelTransition(fadeIn, slideIn, scaleIn);
        activeAnimation.play();

        dismissDelay = new PauseTransition(DISPLAY_DURATION);
        dismissDelay.setOnFinished(event -> hide());
        dismissDelay.play();
    }

    private void hide() {
        if (!container.isVisible()) {
            return;
        }

        if (dismissDelay != null) {
            dismissDelay.stop();
        }
        if (activeAnimation != null) {
            activeAnimation.stop();
        }

        FadeTransition fadeOut = new FadeTransition(HIDE_DURATION, container);
        fadeOut.setToValue(0);
        fadeOut.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition slideOut = new TranslateTransition(HIDE_DURATION, container);
        slideOut.setToX(20);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition scaleOut = new ScaleTransition(HIDE_DURATION, container);
        scaleOut.setToX(0.985);
        scaleOut.setToY(0.985);
        scaleOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition transition = new ParallelTransition(fadeOut, slideOut, scaleOut);
        transition.setOnFinished(done -> {
            container.setVisible(false);
            container.setManaged(false);
            container.setTranslateX(0);
            container.setTranslateY(0);
            container.setScaleX(1);
            container.setScaleY(1);
        });

        activeAnimation = transition;
        transition.play();
    }

    private void stopCurrentAnimation() {
        if (dismissDelay != null) {
            dismissDelay.stop();
        }
        if (activeAnimation != null) {
            activeAnimation.stop();
        }
    }

    public enum Type {
        SUCCESS("✓", "Success", "toast-success"),
        ERROR("!", "Action failed", "toast-error"),
        INFO("i", "Notice", "toast-info");

        private final String icon;
        private final String title;
        private final String cssClass;

        Type(String icon, String title, String cssClass) {
            this.icon = icon;
            this.title = title;
            this.cssClass = cssClass;
        }
    }
}
