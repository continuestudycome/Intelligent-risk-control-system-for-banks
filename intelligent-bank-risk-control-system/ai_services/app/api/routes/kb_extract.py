"""知识库文档抽取接口（由 Java 后端转发 multipart）。"""
from __future__ import annotations

from fastapi import APIRouter, File, UploadFile

from app.services.knowledge_document_extract import extract_text, suggested_title

router = APIRouter(tags=["kb-extract"])


@router.post("/kb/extract")
async def kb_extract(file: UploadFile = File(...)):
    """
    返回抽取的正文与建议标题（文件名去后缀）。
    Java 侧写入 bot_knowledge.question / answer 并同步向量索引。
    """
    data = await file.read()
    fn = file.filename or "upload.bin"
    try:
        text = extract_text(fn, data)
    except ValueError as e:
        from fastapi import HTTPException

        raise HTTPException(status_code=400, detail=str(e)) from e
    return {
        "text": text,
        "suggested_title": suggested_title(file.filename),
        "filename": fn,
    }
