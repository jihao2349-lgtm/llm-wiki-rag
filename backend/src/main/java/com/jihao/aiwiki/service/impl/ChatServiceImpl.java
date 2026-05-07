package com.jihao.aiwiki.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.domain.llm.LlmChatRequest;
import com.jihao.aiwiki.domain.llm.LlmClient;
import com.jihao.aiwiki.domain.llm.LlmMessage;
import com.jihao.aiwiki.domain.llm.LlmStreamDelta;
import com.jihao.aiwiki.domain.search.ContextBudgetService;
import com.jihao.aiwiki.domain.vault.SecretCipher;
import com.jihao.aiwiki.domain.vault.VaultFileService;
import com.jihao.aiwiki.dto.chat.ChatStreamDTO;
import com.jihao.aiwiki.dto.chat.CreateSessionDTO;
import com.jihao.aiwiki.dto.chat.SaveAnswerDTO;
import com.jihao.aiwiki.entity.AppSettingDO;
import com.jihao.aiwiki.entity.ChatMessageDO;
import com.jihao.aiwiki.entity.ChatSessionDO;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.entity.WikiPageDO;
import com.jihao.aiwiki.mapper.AppSettingMapper;
import com.jihao.aiwiki.mapper.ChatMessageMapper;
import com.jihao.aiwiki.mapper.ChatSessionMapper;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import com.jihao.aiwiki.service.ChatService;
import com.jihao.aiwiki.service.SearchService;
import com.jihao.aiwiki.vo.chat.ChatMessageVO;
import com.jihao.aiwiki.vo.chat.ChatSessionVO;
import com.jihao.aiwiki.vo.wiki.WikiSearchResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 对话服务实现：RAG 流式问答与答案保存。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private static final String KEY_API_KEY_CIPHER = "llm.api_key_cipher";
    private static final String KEY_BASE_URL = "llm.base_url";
    private static final String KEY_MODEL = "llm.model";
    private static final String KEY_PROVIDER = "llm.provider";
    private static final String KEY_TEMPERATURE = "llm.temperature";
    private static final List<String> SETTING_KEYS = List.of(
            KEY_PROVIDER, KEY_BASE_URL, KEY_API_KEY_CIPHER, KEY_MODEL, KEY_TEMPERATURE);

    private static final ExecutorService SSE_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final VaultProjectMapper vaultMapper;
    private final WikiPageMapper wikiPageMapper;
    private final AppSettingMapper appSettingMapper;
    private final SecretCipher secretCipher;
    private final LlmClient llmClient;
    private final SearchService searchService;
    private final VaultFileService fileService;
    private final ObjectMapper objectMapper;

    public ChatServiceImpl(ChatSessionMapper sessionMapper,
                           ChatMessageMapper messageMapper,
                           VaultProjectMapper vaultMapper,
                           WikiPageMapper wikiPageMapper,
                           AppSettingMapper appSettingMapper,
                           SecretCipher secretCipher,
                           LlmClient llmClient,
                           SearchService searchService,
                           VaultFileService fileService,
                           ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.vaultMapper = vaultMapper;
        this.wikiPageMapper = wikiPageMapper;
        this.appSettingMapper = appSettingMapper;
        this.secretCipher = secretCipher;
        this.llmClient = llmClient;
        this.searchService = searchService;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ChatSessionVO createSession(CreateSessionDTO dto) {
        requireVault(dto.getVaultId());
        ChatSessionDO session = ChatSessionDO.builder()
                .vaultId(dto.getVaultId())
                .title(dto.getTitle())
                .deleted(0)
                .build();
        sessionMapper.insert(session);
        return toSessionVO(session);
    }

    @Override
    public List<ChatSessionVO> listSessions(Long vaultId) {
        return sessionMapper.selectByVaultId(vaultId).stream()
                .map(this::toSessionVO).toList();
    }

    @Override
    public List<ChatMessageVO> listMessages(Long sessionId) {
        return messageMapper.selectBySessionId(sessionId).stream()
                .map(this::toMessageVO).toList();
    }

    @Override
    public void streamChat(ChatStreamDTO dto, SseEmitter emitter) {
        SSE_EXECUTOR.submit(() -> {
            try {
                doStreamChat(dto, emitter);
            } catch (Exception e) {
                log.error("streamChat error sessionId={}", dto.getSessionId(), e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"message\":\"" + escape(e.getMessage()) + "\"}"));
                    emitter.complete();
                } catch (IOException ignored) {
                    emitter.completeWithError(e);
                }
            }
        });
    }

    private void doStreamChat(ChatStreamDTO dto, SseEmitter emitter) throws IOException {
        // ---- Search context ----
        ContextBudgetService.ContextAssemblyResult ctx =
                searchService.assembleContext(dto.getVaultId(), dto.getQuestion(), dto.getMaxContextTokens());

        List<WikiSearchResultVO> refs = ctx.includedPages().stream()
                .limit(dto.getMaxReferences())
                .map(p -> WikiSearchResultVO.builder()
                        .path(p.getPath()).title(p.getTitle()).type(p.getType())
                        .score(p.getScore()).snippet(p.getSnippet()).build())
                .toList();

        // Send reference event
        emitter.send(SseEmitter.event().name("reference").data(toJson(refs)));

        // Save user message
        ChatMessageDO userMsg = ChatMessageDO.builder()
                .sessionId(dto.getSessionId())
                .role("user")
                .content(dto.getQuestion())
                .build();
        messageMapper.insert(userMsg);

        // Build LLM request
        LlmChatRequest llmReq = buildLlmRequest(dto.getQuestion(), ctx.context());
        StringBuilder fullAnswer = new StringBuilder();

        // Stream delta events
        llmClient.streamChat(llmReq, (LlmStreamDelta delta) -> {
            String chunk = delta.getContent();
            if (chunk != null && !chunk.isEmpty()) {
                fullAnswer.append(chunk);
                try {
                    emitter.send(SseEmitter.event().name("delta")
                            .data("{\"content\":\"" + escape(chunk) + "\"}"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Save assistant message
        List<String> citationPaths = refs.stream().map(WikiSearchResultVO::getPath).toList();
        ChatMessageDO assistantMsg = ChatMessageDO.builder()
                .sessionId(dto.getSessionId())
                .role("assistant")
                .content(fullAnswer.toString())
                .citations(toJson(citationPaths))
                .build();
        messageMapper.insert(assistantMsg);

        // Send done event
        emitter.send(SseEmitter.event().name("done")
                .data("{\"messageId\":" + assistantMsg.getId() + "}"));
        emitter.complete();
    }

    @Override
    @Transactional
    public ChatMessageVO saveAnswer(Long vaultId, SaveAnswerDTO dto) {
        validateSavePath(dto.getTargetPath());

        ChatMessageDO msg = messageMapper.selectById(dto.getMessageId());
        if (msg == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "message not found: " + dto.getMessageId());
        }

        VaultProjectDO vault = requireVault(vaultId);
        Path vaultRoot = Path.of(vault.getPath());

        String today = LocalDate.now().toString();
        String content = buildSaveMarkdown(msg.getContent(), dto.getTargetPath(), today);
        fileService.writeStringAtomically(vaultRoot, dto.getTargetPath(), content);

        appendWikiLog(vaultRoot, dto.getTargetPath(), today);

        messageMapper.updateSavedPath(msg.getId(), dto.getTargetPath());
        msg.setSavedPath(dto.getTargetPath());
        return toMessageVO(msg);
    }

    // ---- Helpers ----

    private VaultProjectDO requireVault(Long vaultId) {
        VaultProjectDO vault = vaultMapper.selectById(vaultId);
        if (vault == null) {
            throw new BusinessException(ErrorCode.VAULT_NOT_INITIALIZED, "vault not found: " + vaultId);
        }
        return vault;
    }

    private void validateSavePath(String path) {
        if (path == null || path.isBlank()) {
            throw new BusinessException(ErrorCode.WIKI_PATH_FORBIDDEN, "target path is empty");
        }
        if (!path.startsWith("wiki/synthesis/") && !path.startsWith("wiki/questions/")) {
            throw new BusinessException(ErrorCode.WIKI_PATH_FORBIDDEN,
                    "save path must start with wiki/synthesis/ or wiki/questions/");
        }
        if (path.contains("..") || path.contains("\\") || path.contains("\0")) {
            throw new BusinessException(ErrorCode.WIKI_PATH_FORBIDDEN, "path contains illegal sequences");
        }
    }

    private LlmChatRequest buildLlmRequest(String question, String context) {
        Map<String, String> settings = loadSettings();
        String cipher = settings.get(KEY_API_KEY_CIPHER);
        String apiKey = StringUtils.hasText(cipher) ? secretCipher.decrypt(cipher) : null;
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "LLM API key not configured");
        }
        String systemPrompt = """
                你是一个知识库助手，根据提供的 Wiki 上下文回答用户问题。
                规则：
                1. 优先使用上下文中的信息作答；即使问题措辞与上下文略有出入（如拼写差异、缩写），也应尽量推断并给出有用的回答。
                2. 引用信息时使用格式：[序号] wiki/路径/页面.md
                3. 只有在上下文完全没有任何相关信息时，才说"上下文中未找到相关内容"，并建议用户导入更多资料。
                4. 用中文回答。
                """;
        String userPrompt = "## Wiki Context\n" + context + "\n\n## Question\n" + question;

        return LlmChatRequest.builder()
                .provider(firstText(settings.get(KEY_PROVIDER), "openai"))
                .baseUrl(firstText(settings.get(KEY_BASE_URL), "https://api.openai.com/v1"))
                .apiKey(apiKey)
                .model(firstText(settings.get(KEY_MODEL), "gpt-4o-mini"))
                .temperature(parseDecimal(settings.get(KEY_TEMPERATURE), BigDecimal.valueOf(0.3)))
                .messages(List.of(
                        new LlmMessage("system", systemPrompt),
                        new LlmMessage("user", userPrompt)
                ))
                .stream(true)
                .build();
    }

    private Map<String, String> loadSettings() {
        return appSettingMapper.findByKeys(SETTING_KEYS).stream()
                .filter(s -> StringUtils.hasText(s.getSettingValue()))
                .collect(Collectors.toMap(AppSettingDO::getSettingKey, AppSettingDO::getSettingValue));
    }

    private String buildSaveMarkdown(String content, String path, String today) {
        String filename = path.substring(path.lastIndexOf('/') + 1).replace(".md", "");
        return """
                ---
                title: %s
                type: %s
                sources: []
                updated: %s
                ---

                %s
                """.formatted(filename,
                path.startsWith("wiki/synthesis/") ? "synthesis" : "question",
                today, content);
    }

    private void appendWikiLog(Path vaultRoot, String savedPath, String today) {
        try {
            String existing = readSafe(vaultRoot, "wiki/log.md");
            String entry = "\n## " + today + " - Answer saved\n- Path: " + savedPath + "\n";
            fileService.writeStringAtomically(vaultRoot, "wiki/log.md", existing + entry);
        } catch (Exception e) {
            log.warn("Failed to update wiki/log.md", e);
        }
    }

    private String readSafe(Path vaultRoot, String path) {
        try {
            return fileService.readString(vaultRoot, path);
        } catch (Exception e) {
            return "";
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private String firstText(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) return v.trim();
        }
        return "";
    }

    private BigDecimal parseDecimal(String value, BigDecimal def) {
        try {
            return StringUtils.hasText(value) ? new BigDecimal(value) : def;
        } catch (Exception e) {
            return def;
        }
    }

    private ChatSessionVO toSessionVO(ChatSessionDO s) {
        return ChatSessionVO.builder()
                .id(s.getId()).vaultId(s.getVaultId()).title(s.getTitle())
                .createTime(s.getCreateTime()).updateTime(s.getUpdateTime())
                .build();
    }

    private ChatMessageVO toMessageVO(ChatMessageDO m) {
        List<String> citations = List.of();
        if (StringUtils.hasText(m.getCitations())) {
            try {
                citations = objectMapper.readValue(m.getCitations(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }
        return ChatMessageVO.builder()
                .id(m.getId()).sessionId(m.getSessionId()).role(m.getRole())
                .content(m.getContent()).citations(citations).savedPath(m.getSavedPath())
                .createTime(m.getCreateTime())
                .build();
    }
}
