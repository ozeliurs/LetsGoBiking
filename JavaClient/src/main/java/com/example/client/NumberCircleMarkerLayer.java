package com.example.client;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public class NumberCircleMarkerLayer extends MapLayer {
    private final MapPoint mapPoint;
    private final Circle circle;
    private final Text text;

    public NumberCircleMarkerLayer(MapPoint mapPoint, int number) {
        this.mapPoint = mapPoint;
        this.circle = new Circle(5, Color.RED);
        this.text = new Text(String.valueOf(number));
        this.getChildren().add(circle);
        this.getChildren().add(text);
    }

    @Override
    protected void layoutLayer() {
        Point2D point2d = this.getMapPoint(mapPoint.getLatitude(), mapPoint.getLongitude());
        text.setTranslateX(point2d.getX());
        text.setTranslateY(point2d.getY());
        circle.setTranslateX(point2d.getX());
        circle.setTranslateY(point2d.getY());
    }
}
