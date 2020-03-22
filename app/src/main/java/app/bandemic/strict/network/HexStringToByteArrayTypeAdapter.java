package app.bandemic.strict.network;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.lang.reflect.Type;

public class HexStringToByteArrayTypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    @Override
    public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return Hex.decodeHex(json.getAsString().toCharArray());
        } catch (DecoderException e) {
            throw new JsonParseException(e.getMessage());
        }
    }

    @Override
    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(String.valueOf(Hex.encodeHex(src)));
    }
}
