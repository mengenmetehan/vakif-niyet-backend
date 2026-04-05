CREATE TABLE quran_verses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    verse_key       VARCHAR(20)  NOT NULL UNIQUE,   -- örn. "2:148"
    sure_no         INT          NOT NULL,
    ayet_no         INT          NOT NULL,
    sure_name       VARCHAR(100) NOT NULL,
    text            TEXT         NOT NULL,
    translation_id  INT          NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_quran_verses_sure_no ON quran_verses(sure_no);
