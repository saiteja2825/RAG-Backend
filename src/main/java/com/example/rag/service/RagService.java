package com.example.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Value("${GOOGLE_API_KEY}")
    private String googleApiKey;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public RagService(@Value("${DB_HOST}") String dbHost,
                      @Value("${DB_USERNAME}") String dbUser,
                      @Value("${DB_PASSWORD}") String dbPassword,
                      @Value("${DB_NAME}") String dbName,
                      @Value("${GOOGLE_API_KEY}") String googleApiKey) {
        this.googleApiKey = googleApiKey;
        this.embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(googleApiKey)
                .modelName("embedding-001")
                .build();
        // Use default port 5432
        this.embeddingStore = PgVectorEmbeddingStore.builder()
                .host(dbHost)
                .port(5432)
                .database(dbName)
                .user(dbUser)
                .password(dbPassword)
                .table("embeddings_gemini")
                .dimension(768)
                .build();
    }

    public void processPdf(Path pdfPath, Long userId) {
        // Load PDF
        Document document = FileSystemDocumentLoader.loadDocument(pdfPath, new ApachePdfBoxDocumentParser());
        
        // Add userId to document metadata for isolation
        document.metadata().put("userId", userId.toString());
        
        // Split text
        DocumentSplitter splitter = DocumentSplitters.recursive(1000, 100);
        List<TextSegment> segments = splitter.split(document);
        
        // Embed and store
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
    }

    public String chat(String query, Long userId) {
        // Embed query and find relevant context, filtered by userId
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest = 
            dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(5)
                .filter(dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey("userId").isEqualTo(userId.toString()))
                .build();
                
        List<EmbeddingMatch<TextSegment>> relevantMatches = embeddingStore.search(searchRequest).matches();

        String context = relevantMatches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n"));

        if (context.isEmpty()) {
            context = "No relevant context found.";
        }

        String prompt = String.format("Use the following context to answer the question at the end.\n" +
                "Keep your answer brief, concise, and beautifully structured. Do not output unnecessarily large information.\n" +
                "Context: %s\n" +
                "Question: %s\n" +
                "Answer:", context, query);

        ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(googleApiKey)
                .modelName("gemini-3.1-flash-lite-preview")
                .build();

        return chatModel.generate(prompt);
    }
}
