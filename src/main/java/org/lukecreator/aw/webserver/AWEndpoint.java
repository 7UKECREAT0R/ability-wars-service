package org.lukecreator.aw.webserver;

import com.google.gson.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

@RestController
@RequestMapping("/aw")
public class AWEndpoint {
    private static final String WEBHOOK_BASE = "https://discord.com/api/webhooks/";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Runnable completionHandler = () -> {
    };
    private final Gson gsonInstance = new Gson();
    private final String API_KEY = System.getenv("AW_API_KEY");

    @PostMapping("/test")
    public ResponseEntity<String> test(@RequestBody String body) {
        return ResponseEntity.ok("WE GOOD! " + body.toUpperCase());
    }

    @GetMapping("/poll")
    public ResponseEntity<String> poll(@RequestHeader("Api-Key") String inputApiKey) {
        if (inputApiKey == null || !inputApiKey.equals(this.API_KEY)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        // remove any expired requests
        PendingRequests.removeExpiredRequests();

        // return JSON of all current requests
        return ResponseEntity.ok(this.gsonInstance.toJson(PendingRequests.getPendingRequestsJSON()));
    }

    @PostMapping("/fulfill")
    public ResponseEntity<?> fulfill(
            @RequestHeader("Api-Key") String inputApiKey,
            @RequestBody String body) {
        if (inputApiKey == null || !inputApiKey.equals(this.API_KEY)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        ArrayList<Fulfillment> fulfillments = new ArrayList<>();

        try {
            JsonObject json = JsonParser
                    .parseString(body)
                    .getAsJsonObject();
            if (!json.has("fulfill")) {
                JsonObject error = new JsonObject();
                error.addProperty("error", "Missing 'fulfill' array in JSON.");
                System.out.println(this.gsonInstance.toJson(error));
                return ResponseEntity.badRequest().body(this.gsonInstance.toJson(error));
            }
            for (JsonElement f : json.getAsJsonArray("fulfill")) {
                JsonObject fulfillmentJson = f.getAsJsonObject();
                Fulfillment parsed = Fulfillment.parse(fulfillmentJson);
                fulfillments.add(parsed);
            }
        } catch (JsonParseException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Couldn't parse JSON");
            System.out.println(this.gsonInstance.toJson(error));

            return ResponseEntity.badRequest().body(this.gsonInstance.toJson(error));
        } catch (IllegalStateException e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", "Input JSON was not a JSON object {}. Additional info: " + e.getMessage());
            System.out.println(this.gsonInstance.toJson(error));

            return ResponseEntity.badRequest().body(this.gsonInstance.toJson(error));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            System.out.println(this.gsonInstance.toJson(error));

            return ResponseEntity.badRequest().body(this.gsonInstance.toJson(error));
        }

        if (fulfillments.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        // process fulfillments from start to finish.
        // the method calls `process()` internally
        for (Fulfillment fulfillment : fulfillments) {
            PendingRequests.fulfill(fulfillment);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader("Api-Key") String inputApiKey,
            @RequestHeader("Webhook-Id") String webhookId,
            @RequestHeader("Webhook-Token") String webhookToken,
            @RequestBody byte[] body
    ) {
        System.out.println(System.currentTimeMillis() + " - Got webhook request");
        if (inputApiKey == null || !inputApiKey.equals(this.API_KEY)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        String discordUri = WEBHOOK_BASE + webhookId + "/" + webhookToken;

        try {
            HttpRequest request = HttpRequest.newBuilder(new URI(discordUri))
                    .headers("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();

            httpClient.sendAsync(request,
                    HttpResponse.BodyHandlers.discarding(), null
            ).thenRun(completionHandler);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}
