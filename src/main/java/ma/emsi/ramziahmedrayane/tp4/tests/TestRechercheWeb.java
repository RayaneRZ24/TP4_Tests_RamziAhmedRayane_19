package ma.emsi.ramziahmedrayane.tp4.tests;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15.BgeSmallEnV15EmbeddingModel;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

public class TestRechercheWeb {

    public static void main(String[] args) {

        String cle = System.getenv("GEMINI_KEY");

        ChatModel model = GoogleGenAiChatModel.builder()
                .apiKey(cle)
                .modelName("gemini-3.5-flash-lite")
                .temperature(0.3)
                .timeout(Duration.ofSeconds(100))
                .logRequestsAndResponses(true)
                .build();

        EmbeddingModel embeddingModel = new BgeSmallEnV15EmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore = chargerDocument(
                "Fine Tuning & RAG.pdf", embeddingModel);

        ContentRetriever contentRetriever =
                EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .maxResults(2)
                        .minScore(0.5)
                        .build();

        WebSearchEngine moteurWeb = TavilyWebSearchEngine.builder()
                .apiKey(System.getenv("TAVILY_API_KEY"))
                .build();

        ContentRetriever retrieverWeb = WebSearchContentRetriever.builder()
                .webSearchEngine(moteurWeb)
                .build();
// QueryRouter queryRouter = new DefaultQueryRouter(
//         contentRetriever, retrieverWeb);
        Map<ContentRetriever, String> sources = new LinkedHashMap<>();

        sources.put(contentRetriever,
                "Support de cours sur les modèles de langage, "
                        + "le fine-tuning et les mécanismes du RAG. "
                        + "Ne contient pas d'actualités.");

        sources.put(retrieverWeb,
                "Recherche sur le Web pour les événements récents, "
                        + "les actualités et les questions sans rapport avec le support.");

        QueryRouter queryRouter = new LanguageModelQueryRouter(model, sources);
        RetrievalAugmentor retrievalAugmentor =
                DefaultRetrievalAugmentor.builder()
                        .queryRouter(queryRouter)
                        .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .retrievalAugmentor(retrievalAugmentor)
                .build();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("Posez votre question :");
                String question = scanner.nextLine();

                if ("fin".equalsIgnoreCase(question)) {
                    break;
                }

                if (question.isBlank()) {
                    continue;
                }

                System.out.println(assistant.chat(question));
            }
        }
    }

    private static EmbeddingStore<TextSegment> chargerDocument(
            String nomFichier, EmbeddingModel embeddingModel) {

        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();

        Document document = ClassPathDocumentLoader.loadDocument(
                nomFichier, parser);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);
        List<TextSegment> segments = splitter.split(document);

        List<Embedding> embeddings =
                embeddingModel.embedAll(segments).content();

        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        store.addAll(embeddings, segments);

        return store;
    }
}