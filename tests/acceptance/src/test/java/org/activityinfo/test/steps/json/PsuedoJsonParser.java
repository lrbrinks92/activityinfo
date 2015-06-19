package org.activityinfo.test.steps.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.*;

import java.io.StringReader;

/**
 * Parses a YAML document into a JsonNode, replacing placeholders
 * for ids and aliases.
 * 
 * <p>The API acceptance tests are meant to ensure that we do not break external applications
 * that interact with AI through the public API. In this case, the precise format of the JSON
 * emitted or expected is important, and so we want to actually validate the representation itself
 * in addition to the business logic.</p>
 * 
 * <p>However, JSON is a bit nasty to include in human readable text files, so purely in the 
 * interests of legibility (or perhaps aesthetics), we parse the blocks as YAML rather than pure
 * JSON. This class merely turns the YAML into a Jackson JSON tree for convenience.</p>
 * 
 * 
 */
class PsuedoJsonParser {

    private final Yaml yaml = new Yaml();
    private final JsonNodeFactory json;

    public PsuedoJsonParser(ObjectMapper objectMapper) {
        this.json = objectMapper.getNodeFactory();
    }

    /**
     * Parses a request body written in YAML, replacing <code>@Placeholders</code> with
     * ids from the alias table.
     * 
     * @param requestBody The request body, written in YAML syntax
     * @return a Jackson JSON tree object.
     */
    public JsonNode parse(String requestBody) {
        Node node = yaml.compose(new StringReader(requestBody));
        return toJson(node);
    }

    private JsonNode toJson(Node node) {
        if(node instanceof MappingNode) {
            return toObjectNode((MappingNode) node);
        }

        if(node instanceof SequenceNode) {
            return toArrayNode((SequenceNode) node);
        }

        if(node instanceof ScalarNode) {
            return toScalarNode((ScalarNode) node);
        }

        throw new IllegalArgumentException("Node: " + node);
    }


    private ObjectNode toObjectNode(MappingNode node) {
        ObjectNode objectNode = json.objectNode();
        for (NodeTuple tuple : node.getValue()) {
            objectNode.put(stringKey(tuple), toJson(tuple.getValueNode()));
        }
        return objectNode;
    }

    private String stringKey(NodeTuple tuple) {
        if(!(tuple.getKeyNode() instanceof ScalarNode)) {
            throw new UnsupportedOperationException("Json keys must be strings");
        }
        return ((ScalarNode) tuple.getKeyNode()).getValue();
    }


    private ArrayNode toArrayNode(SequenceNode node) {
        ArrayNode arrayNode = json.arrayNode();
        for (Node element : node.getValue()) {
            arrayNode.add(toJson(element));
        }
        return arrayNode;
    }


    private JsonNode toScalarNode(ScalarNode node) {
        String value = node.getValue();
        try {
            Double doubleValue = Double.parseDouble(value);
            return json.numberNode(doubleValue);
        } catch (NumberFormatException ignore) {
        } 

        if(value.equals("true")) {
            return json.booleanNode(true);
        }
        if(value.equals("false")) {
            return json.booleanNode(false);
        }
        
        return json.textNode(value);
    }


}
