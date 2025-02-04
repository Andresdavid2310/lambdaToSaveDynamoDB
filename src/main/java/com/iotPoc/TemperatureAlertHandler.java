package com.iotPoc;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TemperatureAlertHandler implements RequestHandler<Map<String, Object>, String> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    // El nombre de la tabla se obtiene de las variables de entorno
    private final String tableName = System.getenv("TABLE_NAME");

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Inicio de la función Lambda.\n");

        try {
            double temperatura = Double.parseDouble(event.get("Temperatura").toString());
            String dispositivoId = event.get("DispositivoId").toString();
            context.getLogger().log("Temperatura recibida: " + temperatura + "\n");

            if (temperatura > 49) {
                crearIncidencia(dispositivoId,temperatura, context);
                context.getLogger().log("Incidencia creada para el dispositivo: " + dispositivoId + "\n");
                return "{\"statusCode\": 200, \"message\": \"Incidencia creada para " + dispositivoId + "\"}";
            } else {
                context.getLogger().log("Temperatura normal. No se creó incidencia.\n");
                return "{\"statusCode\": 200, \"message\": \"Temperatura normal, sin incidencia.\"}";
            }
        } catch (Exception e) {
            context.getLogger().log("Error procesando el evento: " + e.getMessage() + "\n");
            return "{\"statusCode\": 500, \"message\": \"Error al procesar el evento: " + e.getMessage() + "\"}";
        } finally {
            context.getLogger().log("Fin de la función Lambda.\n");
        }
    }

    private void crearIncidencia(String dispositivoId, double temperatura, Context context) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("incidenciaId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
        item.put("dispositivoId", AttributeValue.builder().s(dispositivoId).build());
        item.put("temperatura", AttributeValue.builder().n(String.valueOf(temperatura)).build());
        item.put("timestamp", AttributeValue.builder().s(Instant.now().toString()).build());
        item.put("estado", AttributeValue.builder().s("Pendiente").build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        context.getLogger().log("Incidencia registrada en DynamoDB.");
    }
}