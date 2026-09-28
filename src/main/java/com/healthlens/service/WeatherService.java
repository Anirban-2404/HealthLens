package com.healthlens.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WeatherService {

    public String fetchWeather() throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=22.8456&longitude=89.5403&current=temperature_2m,relative_humidity_2m,weather_code";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject current = root.getAsJsonObject("current");

        double temp = current.get("temperature_2m").getAsDouble();
        int humidity = current.get("relative_humidity_2m").getAsInt();
        int code = current.get("weather_code").getAsInt();

        return String.format("🌤 Temperature: %.1f°C   |   Humidity: %d%%   |   Weather code: %d",
                temp, humidity, code);
    }
}
