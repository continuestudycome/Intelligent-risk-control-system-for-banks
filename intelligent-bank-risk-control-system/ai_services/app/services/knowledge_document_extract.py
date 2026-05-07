"""知识库文档正文抽取（PDF / docx / txt / md），供 Java 上传接口调用。"""
from __future__ import annotations

import re
from io import BytesIO
from pathlib import PurePosixPath

MAX_CHARS = 15_000_000


def _normalize(text: str) -> str:
    t = text.replace("\r\n", "\n").replace("\r", "\n")
    t = re.sub(r"\n{3,}", "\n\n", t)
    return t.strip()


def _read_plain(data: bytes) -> str:
    for enc in ("utf-8", "gbk"):
        try:
            return data.decode(enc)
        except UnicodeDecodeError:
            continue
    return data.decode("utf-8", errors="replace")


def suggested_title(filename: str | None) -> str:
    if not filename or not filename.strip():
        return "上传文档"
    name = PurePosixPath(filename.strip().replace("\\", "/")).name
    stem = name.rsplit(".", 1)[0] if "." in name else name
    return stem[:512] if stem else "上传文档"


def extract_text(filename: str | None, data: bytes) -> str:
    if not data:
        raise ValueError("文件为空")
    fn = (filename or "file.txt").lower()
    suffix = PurePosixPath(fn.replace("\\", "/")).suffix.lower()

    if suffix in {".txt", ".md", ".markdown"}:
        raw = _read_plain(data)
    elif suffix == ".pdf":
        from pypdf import PdfReader

        reader = PdfReader(BytesIO(data))
        parts: list[str] = []
        for page in reader.pages:
            t = page.extract_text()
            if t:
                parts.append(t)
        raw = "\n".join(parts)
    elif suffix == ".docx":
        import docx

        doc = docx.Document(BytesIO(data))
        raw = "\n".join(p.text.strip() for p in doc.paragraphs if p.text and p.text.strip())
    else:
        raise ValueError(f"不支持的格式（仅 txt/md/pdf/docx）：{suffix or fn}")

    raw = _normalize(raw)
    if not raw:
        raise ValueError("未能从文件中解析出文本内容")
    if len(raw) > MAX_CHARS:
        raw = raw[:MAX_CHARS]
    return raw
