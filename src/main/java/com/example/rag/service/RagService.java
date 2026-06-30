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
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Value("${GOOGLE_API_KEY}")
    private String googleApiKey;

    private EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public RagService() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    public void processPdf(Path pdfPath) {
        // Load PDF
        Document document = FileSystemDocumentLoader.loadDocument(pdfPath, new ApachePdfBoxDocumentParser());
        
        // Split text
        DocumentSplitter splitter = DocumentSplitters.recursive(1000, 100);
        List<TextSegment> segments = splitter.split(document);
        
        // Embed and store
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        // Initialize new in-memory store
        embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(embeddings, segments);
    }

    public String chat(String query) {
        if (embeddingStore == null) {
            throw new IllegalStateException("Please upload a PDF first.");
        }

        // Embed query and find relevant context
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> relevantMatches = embeddingStore.findRelevant(queryEmbedding, 5);

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
