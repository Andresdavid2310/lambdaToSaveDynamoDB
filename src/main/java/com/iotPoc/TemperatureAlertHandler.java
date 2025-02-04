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
            context.getLogger().log("Evento Recibido: " + event);
            JsonNode payload = objectMapper.valueToTree(event);
            context.getLogger().log("Payload Recibido: " + payload);

            // Verifica si el campo "Temperatura" existe y no es null
            JsonNode temperaturaNode = payload.get("Temperatura");
            if (temperaturaNode == null || temperaturaNode.isNull()) {
                context.getLogger().log("Error: El campo 'Temperatura' no existe o es null.");
                return "Error: El campo 'Temperatura' no existe o es null.";
            }

            // Verifica si el campo "DispositivoId" existe y no es null
            JsonNode dispositivoIdNode = payload.get("DispositivoId");
            if (dispositivoIdNode == null || dispositivoIdNode.isNull()) {
                context.getLogger().log("Error: El campo 'DispositivoId' no existe o es null.");
                return "Error: El campo 'DispositivoId' no existe o es null.";
            }

            // Obtén los valores de los campos
            double temperatura = temperaturaNode.asDouble();
            String dispositivoId = dispositivoIdNode.asText();

            context.getLogger().log("Recibido del dispositivo " + dispositivoId + " temperatura: " + temperatura);

            if (temperatura > 49) {
                crearIncidencia(dispositivoId, temperatura, context);
                return "Incidencia creada para " + dispositivoId;
            } else {
                return "Temperatura normal, sin incidencia.";
            }
        } catch (Exception e) {
            context.getLogger().log("Error procesando el evento: " + e.getMessage());
            return "Error: " + e.getMessage();
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