package com.example.client;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;

import com.letsgobiking.wsdl.IService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.datacontract.schemas._2004._07.router.ArrayOfCoordinate;
import org.datacontract.schemas._2004._07.router.ArrayOfRoute;
import org.datacontract.schemas._2004._07.router.Coordinate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LetsGoBiking extends Application {
    MapPoint start;
    MapPoint end;
    private final ListView<String> directions = new ListView<>();
    private final VBox controls = new VBox();
    private final TextField searchField = new TextField();
    private final RouteLayer walkingRouteLayer1 = new RouteLayer(List.of(), Color.BLUE);

    private boolean showCycling = false;
    private final RouteLayer cyclingRouteLayer = new RouteLayer(List.of());
    private final RouteLayer walkingRouteLayer2 = new RouteLayer(List.of(), Color.BLUE);
    private MapView mapView;
    private IService service;
    private List<NumberCircleMarkerLayer> markers;
    private final Button nextButton = new Button("Next");

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        System.setProperty("javafx.platform", "desktop");
        System.setProperty("http.agent", "Gluon Mobile/1.0.3");

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(IService.class);
        factory.setAddress("http://localhost:5229/Service.svc");
        service = (IService) factory.create();

        HBox root = new HBox();

        mapView = new MapView();

        /* Création et ajoute une couche à la carte */
        mapView.addLayer(walkingRouteLayer1);
        mapView.addLayer(cyclingRouteLayer);
        mapView.addLayer(walkingRouteLayer2);

        mapView.setZoom(10);
        mapView.flyTo(0, new MapPoint(43.61551, 7.07170), 0.1);

        // Create a VBox to hold the controls with minimal width of 200px and taking all the available height
        controls.setMinWidth(200);
        controls.setMaxHeight(Double.MAX_VALUE);

        // Add the controls to the VBox
        // List of directions that takes all the available height
        directions.setMaxHeight(Double.MAX_VALUE);
        directions.setOnMouseClicked(this::onSearchSelect);

        // on key pressed, search for the text in the search field
        searchField.setOnKeyPressed(this::onSearchUpdate);

        controls.getChildren().add(searchField);
        controls.getChildren().add(directions);
        controls.getChildren().add(nextButton);
        root.getChildren().add(controls);
        root.getChildren().add(mapView);

        Scene scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    private void presentOptions(ArrayOfCoordinate coordinates) {
        markers = new ArrayList<>();

        for (int i = 0; i < coordinates.getCoordinate().size(); i++) {
            System.out.println(coordinates.getCoordinate().get(i).getLatitude() + " " + coordinates.getCoordinate().get(i).getLongitude());
            NumberCircleMarkerLayer marker = new NumberCircleMarkerLayer(new MapPoint(coordinates.getCoordinate().get(i).getLatitude(), coordinates.getCoordinate().get(i).getLongitude()), i);
            markers.add(marker);
            mapView.addLayer(marker);
        }

        directions.getItems().clear();
        for (int i = 0; i < coordinates.getCoordinate().size(); i++) {
            directions.getItems().add(coordinates.getCoordinate().get(i).getLatitude() + " " + coordinates.getCoordinate().get(i).getLongitude());
        }
    }

    private void onSearchUpdate(KeyEvent e) {
        // Search only when the user presses enter
        if (!e.getCode().equals(KeyCode.ENTER)) {
            return;
        }

        presentOptions(service.geocode(searchField.getText()));
        searchField.setText("");
    }

    private void onSearchSelect(MouseEvent e) {
        // Get the selected item
        String selected = directions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // Get the coordinates
        System.out.println(selected);
        String[] coordinates = selected.split(" ");
        double latitude = Double.parseDouble(coordinates[0]);
        double longitude = Double.parseDouble(coordinates[1]);

        if (start != null && end != null) {
            start = null;
            end = null;
        }

        if (start == null) {
            start = new MapPoint(latitude, longitude);
        } else {
            end = new MapPoint(latitude, longitude);
        }

        // remove all markers
        for (NumberCircleMarkerLayer marker : markers) {
            mapView.removeLayer(marker);
        }
        markers.clear();
        directions.getItems().clear();

        if (start != null && end != null) {
            drawRoute();
        }

    }

    private void drawRoute() {
        if (start == null || end == null) {
            return;
        }

        Coordinate _start = new Coordinate();
        _start.setLatitude(start.getLatitude());
        _start.setLongitude(start.getLongitude());

        Coordinate _end = new Coordinate();
        _end.setLatitude(end.getLatitude());
        _end.setLongitude(end.getLongitude());

        System.out.println("Start: " + _start.getLatitude() + " " + _start.getLongitude());
        System.out.println("End: " + _end.getLatitude() + " " + _end.getLongitude());

        // Get the route
        ArrayOfRoute routes = service.getRoute(
                _start,
                _end
        );

        System.out.println(routes.getRoute().size());

        if (routes.getRoute().size() == 3) {
            List<MapPoint> w1 = routes.getRoute().get(0).getCoordinates().getValue().getCoordinate().stream().map(c -> new MapPoint(c.getLatitude(), c.getLongitude())).toList();
            List<MapPoint> c = routes.getRoute().get(1).getCoordinates().getValue().getCoordinate().stream().map(co -> new MapPoint(co.getLatitude(), co.getLongitude())).toList();
            List<MapPoint> w2 = routes.getRoute().get(2).getCoordinates().getValue().getCoordinate().stream().map(coordinate -> new MapPoint(coordinate.getLatitude(), coordinate.getLongitude())).toList();


            walkingRouteLayer1.setPoints(w1);
            cyclingRouteLayer.setPoints(c);
            walkingRouteLayer2.setPoints(w2);

            directions.getItems().clear();

            if (!showCycling) {
                showCycling = true;
                mapView.addLayer(cyclingRouteLayer);
                mapView.addLayer(walkingRouteLayer2);
            }

            directions.getItems().addAll(routes.getRoute().get(0).getInstructions().getValue().getString());
            directions.getItems().addAll(routes.getRoute().get(1).getInstructions().getValue().getString());
            directions.getItems().addAll(routes.getRoute().get(2).getInstructions().getValue().getString());
        } else {
            List<MapPoint> w1 = routes.getRoute().get(0).getCoordinates().getValue().getCoordinate().stream().map(c -> new MapPoint(c.getLatitude(), c.getLongitude())).toList();
            walkingRouteLayer1.setPoints(w1);
            walkingRouteLayer2.setPoints(List.of());
            cyclingRouteLayer.setPoints(List.of());

            directions.getItems().clear();
            directions.getItems().addAll(routes.getRoute().get(0).getInstructions().getValue().getString());

            showCycling = false;
            mapView.removeLayer(cyclingRouteLayer);
            mapView.removeLayer(walkingRouteLayer2);
        }
    }

}