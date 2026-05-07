"""RAG 智能客服：LangChain 切分 + Chroma 向量库 + Ollama 嵌入/生成。"""
from __future__ import annotations

from fastapi import APIRouter

from app.services.rag_langchain import (
    delete_knowledge_from_index,
    get_index_stats,
    rebuild_index,
    run_rag_chroma,
    upsert_knowledge_items,
)

router = APIRouter(tags=["chat-rag"])


@router.get("/rag/stats")
async def rag_stats():
    """返回 Chroma 切片数与已索引的知识条目 id 列表（用于 Java 侧判断是否需要全量重建）。"""
    return get_index_stats()


@router.post("/rag/index/upsert")
async def rag_index_upsert(body: dict):
    """
    增量索引（推荐）：对给定 knowledge_items 切块、嵌入并写入 Chroma；会先删除同 id 的旧向量。
    body: { knowledge_items: [{id, category, docType, question, answer, ...}] }
    """
    items = body.get("knowledge_items") or []
    if not isinstance(items, list):
        items = []
    return upsert_knowledge_items(items)


@router.delete("/rag/index/knowledge/{knowledge_id}")
async def rag_index_delete(knowledge_id: int):
    """按知识 id 删除向量切片（知识逻辑删除后调用）。"""
    return delete_knowledge_from_index(knowledge_id)


@router.post("/rag/index/rebuild")
async def rag_index_rebuild(body: dict):
    """
    body:
      knowledge_items: [{id, category, docType, question, answer, keywords?, similarQuestions?}]
    """
    items = body.get("knowledge_items") or []
    if not isinstance(items, list):
        items = []
    return rebuild_index(items)


@router.post("/chat/rag")
async def chat_rag(body: dict):
    """
    body:
      question: str
      history: [{role: user|assistant, content: str}]
      category_hint: optional str — 会话主题/分类，辅助指代消解
      category_scope: optional str — 仅检索该业务分类（与 Java scopeCategory 一致）
    向后兼容：若传入 knowledge_items，则先执行全量重建再问答（便于手动调试）。
    """
    q = body.get("question") or ""
    history = body.get("history") or []
    hint = body.get("category_hint")
    scope = body.get("category_scope")
    legacy = body.get("knowledge_items")
    if not isinstance(history, list):
        history = []
    if legacy is not None and isinstance(legacy, list) and len(legacy) > 0:
        rebuild_index(legacy)
    result = run_rag_chroma(
        q,
        history,
        category_hint=str(hint) if hint else None,
        category_scope=str(scope) if scope else None,
    )
    return result
