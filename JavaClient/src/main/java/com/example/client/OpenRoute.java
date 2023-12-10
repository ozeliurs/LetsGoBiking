package com.example.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gluonhq.maps.MapPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OpenRoute {
    private String api_key;
    private String base_url = "https://api.openrouteservice.org/";

    public OpenRoute() {
        this.api_key = "5b3ce3597851110001cf6248ba0ea999ab9e47e39f4ae0415f4840e3";
    }

    public List<MapPoint> getRoute(MapPoint start, MapPoint end) throws IOException {
        URL url = new URL(this.base_url + "v2/directions/driving-car?api_key=" + this.api_key + "&start=" + start.getLongitude() + "," + start.getLatitude() + "&end=" + end.getLongitude() + "," + end.getLatitude());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String inputLine;
        StringBuffer content = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(content.toString());

        JsonNode features = jsonNode.get("features");
        JsonNode geometry = features.get(0).get("geometry");
        JsonNode coordinates = geometry.get("coordinates");

        List<List<Double>> points = objectMapper.readValue(coordinates.toString(), List.class);

        List<MapPoint> mapPoints = new ArrayList<>();
        for (List<Double> point : points) {
            mapPoints.add(new MapPoint(point.get(1), point.get(0)));
        }

        return mapPoints;
    }

    public List<String> getRouteInstructions(MapPoint start, MapPoint end) throws IOException {
        URL url = new URL(this.base_url + "v2/directions/driving-car?api_key=" + this.api_key + "&start=" + start.getLongitude() + "," + start.getLatitude() + "&end=" + end.getLongitude() + "," + end.getLatitude());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String inputLine;
        StringBuffer content = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(content.toString());

        JsonNode steps = jsonNode.get("features").get(0).get("properties").get("segments").get(0).get("steps");

        // for each step, take "instruction"
        List<String> instructions = new ArrayList<>();
        for (JsonNode step : steps) {
            instructions.add(step.get("instruction").toString());
        }

        return instructions;
    }

    public MapPoint getCoordinates(String address) throws IOException {
        // replace spaces with %20
        address = address.replaceAll(" ", "%20");
        URL url = new URL(this.base_url + "geocode/search?api_key=" + this.api_key + "&text=" + address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        String inputLine;
        StringBuffer content = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(content.toString());
        JsonNode features = jsonNode.get("features").get(0).get("geometry").get("coordinates");
        return new MapPoint(features.get(1).asDouble(), features.get(0).asDouble());
    }

    public static void main(String[] args) throws IOException {
        OpenRoute or = new OpenRoute();
        or.getRoute(new MapPoint(43.61524, 7.07188), new MapPoint(43.58807, 7.05226));
    }
}
