package br.com.github.gtvnv.omnishift.domain.model;

public enum OmniNull implements OmniNode {

    /**
     * O OmniNull é representado como um enum com uma única instância, garantindo que haja apenas um objeto representando o valor nulo em toda a aplicação. Isso é uma implementação clássica do padrão Singleton em Java, que é thread-safe e fácil de usar.
     */
    INSTANCE; // Esta é a única instância (O Singleton perfeito do Java)

    public static OmniNull getInstance() { return INSTANCE;}

    @Override
    public NodeType getType() {
        return NodeType.NULL;
    }
}