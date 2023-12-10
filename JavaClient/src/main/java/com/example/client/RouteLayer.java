package com.example.client;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class RouteLayer extends MapLayer {
    private List<MapPoint> points;
    private final Polyline polyline;

    public RouteLayer(List<MapPoint> points, Color color) {
        super();

        this.polyline = new Polyline();
        this.polyline.setStroke(color);
        this.polyline.setStrokeWidth(4);
        this.getChildren().add(polyline);

        this.points = points;
    }

    public RouteLayer(List<MapPoint> points) {
        this(points, Color.RED);
    }

    public List<MapPoint> getPoints() {
        return points;
    }

    public void setPoints(List<MapPoint> points) {
        this.points = points;
    }

    @Override
    protected void layoutLayer() {
        // Clear previous lines
        polyline.getPoints().clear();

        System.out.println(points.size());

        // For each pair of points, draw a line
        for (int i = 0; i < points.size() - 1; i++) {
            MapPoint startCoord = points.get(i);
            MapPoint endCoord = points.get(i + 1);

            Point2D startPoint = this.getMapPoint(startCoord.getLatitude(), startCoord.getLongitude());
            Point2D endPoint = this.getMapPoint(endCoord.getLatitude(), endCoord.getLongitude());

            polyline.getPoints().addAll(startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
        }
    }
}
