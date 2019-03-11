package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

public class SuperGsonEnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    public static final TypeAdapterFactory SuperGsonEnumTypeAdapterFactory = new TypeAdapterFactory() {
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public <Z> TypeAdapter<Z> create(Gson gson, TypeToken<Z> typeToken) {
            Class<? super Z> rawType = typeToken.getRawType();
            if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
                return null;
            }
            if (!rawType.isEnum()) {
                rawType = rawType.getSuperclass();
            }
            return (TypeAdapter<Z>) new SuperGsonEnumTypeAdapter(rawType);
        }
    };
    private final Map<String, T> nameToConstant = new HashMap<>();
    private final Map<T, String> constantToName = new HashMap<>();
    private T defaultValue;

    private SuperGsonEnumTypeAdapter(Class<T> classOfT) {
        try {
            for (T constant : classOfT.getEnumConstants()) {
                String name = constant.name();
                SerializedName annotation = classOfT.getField(name).getAnnotation(SerializedName.class);
                Unknown unknown = classOfT.getField(name).getAnnotation(Unknown.class);
                if (annotation != null) {
                    name = annotation.value();
                    for (String alternate : annotation.alternate()) {
                        nameToConstant.put(alternate, constant);
                    }
                }
                nameToConstant.put(name, constant);
                constantToName.put(constant, name);
                defaultValue = (unknown != null ? constant : defaultValue);
            }
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return defaultValue;
        }
        T t = nameToConstant.get(in.nextString());
        return t != null ? t : defaultValue;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        out.value(value == null ? null : constantToName.get(value));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface Unknown {
    }
}
