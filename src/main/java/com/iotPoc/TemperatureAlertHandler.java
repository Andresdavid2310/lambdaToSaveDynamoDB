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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    // El nombre de la tabla se obtiene de las variables de entorno
    private final String tableName = System.getenv("TABLE_NAME");

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        try {
            JsonNode payload = objectMapper.valueToTree(event);
            double temperatura = payload.get("temperatura").asDouble();
            String dispositivoId = payload.get("dispositivoId").asText();

            context.getLogger().log("Recibido del dispositivo " + dispositivoId + " temperatura: " + temperatura);

            if (temperatura > 49) {
                crearIncidencia(dispositivoId, temperatura, context);
                return "Incidencia creada para " + dispositivoId;
            } else {
                return "Temperatura normal, sin incidencia.";
            }
        } catch (Exception e) {
            context.getLogger().log("Error procesando el evento: " + e.getMessage());
            return "Error";
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
