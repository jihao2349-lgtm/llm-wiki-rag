ALTER TABLE wiki_page
  ADD COLUMN embedding        VECTOR(1024)     COMMENT '页面摘要向量',
  ADD COLUMN embedding_model  VARCHAR(64)      COMMENT '生成向量所用模型',
  ADD COLUMN embedded_at      DATETIME         COMMENT '向量生成时间',
  ADD COLUMN embed_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
  ADD COLUMN embed_content_hash VARCHAR(64)    COMMENT '向量化时内容的 sha256 hash，用于增量判断',
  ADD COLUMN embed_error      VARCHAR(500)     COMMENT '失败原因';

CREATE VECTOR INDEX idx_wiki_embedding ON wiki_page(embedding) USING HNSW;

CREATE INDEX idx_wiki_embed_status ON wiki_page(vault_id, embed_status);
