package br.com.github.gtvnv.omnishift.domain.model;

// Se estiver usando Java 17+, use "sealed" para blindar o domínio
public sealed interface OmniNode permits OmniObject, OmniArray, OmniValue, OmniNull {

    NodeType getType();

    // Métodos utilitários default para navegação fluida na DSL
    default boolean isObject() {
        return getType() == NodeType.OBJECT;
    }

    default boolean isArray() {
        return getType() == NodeType.ARRAY;
    }

    default boolean isValue() {
        return getType() == NodeType.VALUE;
    }

    default boolean isNull() {
        return getType() == NodeType.NULL;
    }

    // Facilita o cast seguro dentro da Engine
    default OmniObject asObject() {
        if (isObject()) return (OmniObject) this;
        throw new IllegalStateException("Node is not an Object, it is " + getType());
    }

    default OmniArray asArray() {
        if (isArray()) return (OmniArray) this;
        throw new IllegalStateException("Node is not an Array, it is " + getType());
    }
}