package com.example.filerename.util;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public final class AnimationUtils {

    private AnimationUtils() {
    }

    public static void fadeAndSlideIn(Node node, double distance, Duration duration) {
        node.setOpacity(0);
        node.setTranslateY(distance);

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromY(distance);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    public static void pulse(Node node) {
        ScaleTransition up = new ScaleTransition(Duration.millis(120), node);
        up.setToX(1.012);
        up.setToY(1.012);

        ScaleTransition down = new ScaleTransition(Duration.millis(160), node);
        down.setToX(1);
        down.setToY(1);
        down.setDelay(Duration.millis(120));
        down.play();
        up.play();
    }

    public static void installButtonScale(Node node) {
        node.setOnMouseEntered(event -> scaleTo(node, 1.025, 110));
        node.setOnMouseExited(event -> scaleTo(node, 1.0, 130));
        node.setOnMousePressed(event -> scaleTo(node, 0.975, 70));
        node.setOnMouseReleased(event -> scaleTo(node, node.isHover() ? 1.025 : 1.0, 90));
    }

    public static void scaleTo(Node node, double scale, double millis) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }
}
