package health.guardian.modules.knowledgebase.model;

/**
 * RAG backend selected by the user.
 */
public enum RagProvider {
    CURRENT,
    LIGHTRAG;

    public static RagProvider normalize(RagProvider provider) {
        return provider == null ? CURRENT : provider;
    }
}
