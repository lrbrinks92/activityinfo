package org.activityinfo.test.steps.json;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.activityinfo.test.driver.AliasTable;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



class Placeholders {

    public static final String ID_PREFIX = "$";
    public static final java.lang.String ALIAS_PREFIX = "~";

    private final AliasTable aliasTable;
    private final JsonNodeFactory factory;

    @Inject
    public Placeholders(AliasTable aliasTable) {
        this.aliasTable = aliasTable;
        this.factory = new ObjectMapper().getNodeFactory();
    }
    
    public JsonNode resolve(JsonNode node) {
        if(node instanceof ObjectNode) {
            return resolveObject((ObjectNode)node);
        
        } else if(node instanceof ArrayNode) {
            return resolveArray((ArrayNode) node);
        
        } else if(node instanceof TextNode) {
            return resolveText((TextNode)node);
            
        } else {
            return node;
        }
    }


    public String resolvePath(String url) {

        String path;

        int querySymbol = url.indexOf('?');
        if(querySymbol != -1) {
            path = url.substring(0, querySymbol);
        } else {
            path = url;
        }
        
        List<String> result = new ArrayList<>();
        for(String part : path.split("/")) {
            result.add(resolveUrlPart(part));
        }
        return Joiner.on("/").join(result);
    }
    
    public MultivaluedMap<String, String> resolveQueryParams(String url) {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        int querySymbol = url.indexOf('?');
        if(querySymbol != -1) {
            String queryString = url.substring(querySymbol+1);
            String[] pairs = queryString.split("&");
            for(int i=0;i!=pairs.length;++i) {
                String[] keyValue = pairs[i].split("=");
                params.putSingle(keyValue[0], resolveUrlPart(keyValue[1]));
            }
        }
        return params;
    }

    private String resolveUrlPart(String part) {
        String s;
        if(isPlaceholder(part)) {
            s = Integer.toString(resolveId(part));
        } else {
            s = part;              
        }
        return s;
    }

    private ObjectNode resolveObject(ObjectNode node) {
        ObjectNode result = factory.objectNode();
        Iterator<Map.Entry<String, JsonNode>> it = node.getFields();
        while(it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            result.put(entry.getKey(), resolve(entry.getValue()));
        }
        return result;
    }
    
    private ArrayNode resolveArray(ArrayNode node) {
        ArrayNode result = factory.arrayNode();
        for (JsonNode aNode : node) {
            result.add(resolve(aNode));
        }
        return result;
    }

    private JsonNode resolveText(TextNode node) {
        String text = node.getTextValue();
        if(isPlaceholder(text)) {
            if (text.startsWith(ID_PREFIX)) {
                return factory.numberNode(resolveId(text));
            } else {
                return factory.textNode(aliasTable.getAlias(text));
            }
        } else {
            return node;
        }
    }
    
    
    public String deAliasText(String text) {
        if(aliasTable.isAlias(text)) {
            return aliasTable.getTestHandleForAlias(text);
        } else {
            return text;
        }
    }

    private int resolveId(String placeholder) {
        return aliasTable.getOrGenerateId(parseName(placeholder));
    }

    public int resolveId(JsonNode node) {
        return resolveId(node.asText());
    }

    public boolean isPlaceholder(String textValue) {
        return textValue.startsWith(ID_PREFIX) || textValue.startsWith(ALIAS_PREFIX);
    }

    public String parseName(String placeholder) {
        Preconditions.checkArgument(isPlaceholder(placeholder));
        String name = placeholder.substring(1);
        if(name.startsWith("{") && name.endsWith("}")) {
            return name.substring(1, name.length()-1);
        } else {
            return name;
        }
    }

    public void bind(String alias, int id) {
        aliasTable.bindTestHandleToId(alias, id);
    }

    public String resolveName(JsonNode expected) {
        String alias = parseName(expected.asText());
        return aliasTable.getAlias(alias);
    }
}
