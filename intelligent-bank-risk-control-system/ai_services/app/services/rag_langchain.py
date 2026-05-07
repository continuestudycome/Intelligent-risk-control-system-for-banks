"""
传统 RAG：文档切分（500/50）→ Ollama 嵌入 → Chroma 持久化 → top-k 检索 → LLM 生成 + 引用。
"""
from __future__ import annotations

import json
import logging
import os
import shutil
import threading
from typing import Any

from langchain_community.vectorstores import Chroma
from langchain_core.documents import Document
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import ChatOllama, OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.config.ollama_rag import ollama_base, ollama_chat_model, ollama_embed_model

logger = logging.getLogger(__name__)

_lock = threading.RLock()
_vs: Chroma | None = None

CHUNK_SIZE = int(os.getenv("RAG_CHUNK_SIZE", "500"))
CHUNK_OVERLAP = int(os.getenv("RAG_CHUNK_OVERLAP", "50"))
TOP_K = int(os.getenv("RAG_TOP_K", "3"))
COLLECTION = os.getenv("CHROMA_COLLECTION", "bot_knowledge")
_DEFAULT_CHROMA = os.path.join(os.path.dirname(__file__), "..", "..", "data", "chroma_db")
PERSIST_DIR = os.path.abspath(os.getenv("CHROMA_PERSIST_DIR", _DEFAULT_CHROMA))


def _embeddings() -> OllamaEmbeddings:
    return OllamaEmbeddings(base_url=ollama_base(), model=ollama_embed_model())


def _format_item_text(it: dict[str, Any]) -> str:
    cat = it.get("category") or ""
    dt = it.get("docType") or ""
    q = it.get("question") or ""
    a = it.get("answer") or ""
    kw = it.get("keywords") or ""
    sim = it.get("similarQuestions")
    sim_txt = ""
    if sim:
        if isinstance(sim, str):
            try:
                arr = json.loads(sim)
                if isinstance(arr, list):
                    sim_txt = "；相似问法：" + "；".join(str(x) for x in arr[:8])
            except json.JSONDecodeError:
                pass
        elif isinstance(sim, list):
            sim_txt = "；相似问法：" + "；".join(str(x) for x in sim[:8])
    return f"[{cat}/{dt}] {q}\n{a}\n关键词：{kw}{sim_txt}"


def _items_to_documents(items: list[dict[str, Any]]) -> list[Document]:
    docs: list[Document] = []
    for it in items:
        kid = int(it["id"])
        cat = (it.get("category") or "").strip().upper()
        text = _format_item_text(it)
        docs.append(
            Document(
                page_content=text[:12000],
                metadata={
                    "knowledge_id": kid,
                    "category": cat,
                    "doc_type": str(it.get("docType") or ""),
                    "question_preview": (it.get("question") or "")[:200],
                },
            )
        )
    return docs


def _clear_persist_dir() -> None:
    if os.path.isdir(PERSIST_DIR):
        try:
            shutil.rmtree(PERSIST_DIR)
        except OSError as e:
            logger.warning("清除 Chroma 目录失败: %s", e)
    os.makedirs(PERSIST_DIR, exist_ok=True)


def _delete_by_knowledge_ids(vs: Chroma, kid_list: list[int]) -> None:
    """删除指定知识条目对应的全部向量切片。"""
    coll = vs._collection
    for kid in kid_list:
        for clause in (
            {"knowledge_id": kid},
            {"knowledge_id": {"$eq": kid}},
            {"knowledge_id": str(kid)},
        ):
            try:
                coll.delete(where=clause)
                break
            except Exception:
                continue


def upsert_knowledge_items(knowledge_items: list[dict[str, Any]]) -> dict[str, Any]:
    """
    增量索引：先删除这些 knowledge_id 的旧切片，再写入新切块（500/50）、嵌入并写入 Chroma。
    """
    global _vs
    if not knowledge_items:
        return {"ok": True, "chunks_added": 0, "knowledge_ids": []}

    ids_to_replace = [int(x["id"]) for x in knowledge_items]
    with _lock:
        emb = _embeddings()
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=CHUNK_SIZE,
            chunk_overlap=CHUNK_OVERLAP,
            length_function=len,
        )
        docs = _items_to_documents(knowledge_items)
        splits = splitter.split_documents(docs)

        vs = get_vectorstore()
        if vs is None:
            os.makedirs(PERSIST_DIR, exist_ok=True)
            _vs = Chroma.from_documents(
                documents=splits,
                embedding=emb,
                persist_directory=PERSIST_DIR,
                collection_name=COLLECTION,
            )
            return {"ok": True, "chunks_added": len(splits), "knowledge_ids": ids_to_replace}

        _delete_by_knowledge_ids(vs, ids_to_replace)
        vs.add_documents(splits)
        return {"ok": True, "chunks_added": len(splits), "knowledge_ids": ids_to_replace}


def delete_knowledge_from_index(knowledge_id: int) -> dict[str, Any]:
    """从向量库移除某条知识（逻辑删除后调用）。"""
    global _vs
    with _lock:
        vs = get_vectorstore()
        if vs is None:
            return {"ok": True, "deleted": False}
        _delete_by_knowledge_ids(vs, [int(knowledge_id)])
        return {"ok": True, "deleted": True}


def rebuild_index(knowledge_items: list[dict[str, Any]]) -> dict[str, Any]:
    """全量重建向量索引。"""
    global _vs
    with _lock:
        _vs = None
        if not knowledge_items:
            _clear_persist_dir()
            return {"ok": True, "chunks": 0, "sources": 0}

        docs = _items_to_documents(knowledge_items)
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=CHUNK_SIZE,
            chunk_overlap=CHUNK_OVERLAP,
            length_function=len,
        )
        splits = splitter.split_documents(docs)
        emb = _embeddings()
        _clear_persist_dir()
        _vs = Chroma.from_documents(
            documents=splits,
            embedding=emb,
            persist_directory=PERSIST_DIR,
            collection_name=COLLECTION,
        )
        return {"ok": True, "chunks": len(splits), "sources": len(docs)}


def get_vectorstore() -> Chroma | None:
    global _vs
    with _lock:
        if _vs is not None:
            return _vs
        if not os.path.isdir(PERSIST_DIR) or not os.listdir(PERSIST_DIR):
            return None
        try:
            _vs = Chroma(
                persist_directory=PERSIST_DIR,
                embedding_function=_embeddings(),
                collection_name=COLLECTION,
            )
            return _vs
        except Exception:
            logger.exception("加载 Chroma 失败")
            return None


def get_index_stats() -> dict[str, Any]:
    vs = get_vectorstore()
    if vs is None:
        return {"chunk_count": 0, "knowledge_ids": []}
    try:
        data = vs.get(include=["metadatas"])
        metas = data.get("metadatas") or []
        kid_set: set[int] = set()
        for m in metas:
            if m and "knowledge_id" in m:
                kid_set.add(int(m["knowledge_id"]))
        return {
            "chunk_count": len(metas),
            "knowledge_ids": sorted(kid_set),
        }
    except Exception as e:
        logger.exception("读取索引统计失败")
        return {"chunk_count": 0, "knowledge_ids": [], "error": str(e)}


def _distance_to_score(d: float) -> float:
    return round(1.0 / (1.0 + float(d)), 4)


def _dedupe_citations(pairs: list[tuple[Document, float]]) -> list[dict[str, Any]]:
    """按 knowledge_id 去重，保留最高相似分。"""
    best: dict[int, float] = {}
    for doc, dist in pairs:
        meta = doc.metadata or {}
        kid = meta.get("knowledge_id")
        if kid is None:
            continue
        kid = int(kid)
        sc = _distance_to_score(dist)
        if kid not in best or sc > best[kid]:
            best[kid] = sc
    out = [{"id": k, "score": best[k]} for k in sorted(best.keys())]
    out.sort(key=lambda x: x["score"], reverse=True)
    return out


def run_rag_chroma(
    question: str,
    history: list[dict[str, str]],
    category_hint: str | None,
    category_scope: str | None = None,
) -> dict[str, Any]:
    """
    history: [{"role":"user"|"assistant","content":"..."}]
    category_scope: 若指定，仅在对应业务分类的切片中检索（与 Java scopeCategory 一致，大写）
    """
    q = (question or "").strip()
    if not q:
        return {"answer": "请输入有效的问题。", "citations": [], "mode": "empty", "model": None}

    vs = get_vectorstore()
    if vs is None:
        return {
            "answer": "知识索引尚未构建或暂不可用，请联系管理员同步知识库后重试。",
            "citations": [],
            "mode": "no_index",
            "model": None,
        }

    scope = (category_scope or "").strip().upper() or None
    flt: dict[str, str] | None = {"category": scope} if scope else None

    try:
        if flt:
            pairs = vs.similarity_search_with_score(q, k=TOP_K, filter=flt)
        else:
            pairs = vs.similarity_search_with_score(q, k=TOP_K)
    except Exception as e:
        logger.exception("向量检索失败")
        err_lower = str(e).lower()
        hint = ""
        if "not found" in err_lower or "404" in err_lower:
            em = ollama_embed_model()
            hint = (
                f" 请在 Ollama 所在机器执行：ollama pull {em} "
                f"（或设置环境变量 OLLAMA_EMBED_MODEL 为已安装的嵌入模型后重启 ai_services。）"
            )
        return {
            "answer": f"检索失败：{e}{hint}",
            "citations": [],
            "mode": "error",
            "model": ollama_chat_model(),
        }

    if not pairs:
        msg = (
            "当前分类下未找到匹配知识，请尝试不指定分类提问或联系人工客服。"
            if flt
            else "知识库中暂无与问题匹配的条目，请换个说法或联系人工客服。"
        )
        return {"answer": msg, "citations": [], "mode": "no_hit", "model": None}

    citations = _dedupe_citations(pairs)

    ctx_parts: list[str] = []
    seen = set()
    for doc, _dist in pairs:
        meta = doc.metadata or {}
        kid = meta.get("knowledge_id")
        key = (kid, doc.page_content[:80])
        if key in seen:
            continue
        seen.add(key)
        ctx_parts.append(f"【知识点#{kid} | {meta.get('category', '')}】\n{doc.page_content}")
    ctx = "\n\n".join(ctx_parts)

    hist_lines: list[str] = []
    for h in history[-8:]:
        role = "用户" if h.get("role") == "user" else "助手"
        hist_lines.append(f"{role}：{h.get('content', '')}")
    hist_txt = "\n".join(hist_lines)
    hint = f"\n当前用户提问可能所属业务域：{category_hint}\n" if category_hint else ""

    prompt = ChatPromptTemplate.from_template(
        """你是商业银行的智能客服助手，称呼用户为「您」。请严格依据下方「参考资料」与「对话历史」回答；不要使用参考资料以外的银行业务承诺。
若参考资料不足以完整回答，请诚实说明，并建议用户通过网银「在线客服转人工」或网点核实。
回答使用简洁中文段落，可使用少量条目列举。

{hint}参考资料：
{context}

对话历史：
{history}

用户最新问题：{question}

请作答："""
    )
    llm = ChatOllama(
        base_url=ollama_base(),
        model=ollama_chat_model(),
        temperature=0.3,
    )
    chain = prompt | llm | StrOutputParser()
    try:
        reply = chain.invoke(
            {
                "hint": hint,
                "context": ctx,
                "history": hist_txt,
                "question": q,
            }
        )
        reply = (reply or "").strip()
    except Exception as e:
        logger.exception("LLM 生成失败")
        return {
            "answer": f"大模型暂时不可用（请确认已 ollama pull {ollama_chat_model()}）：{e}",
            "citations": citations,
            "mode": "error",
            "model": ollama_chat_model(),
        }

    return {
        "answer": reply or "（未生成有效回复）",
        "citations": citations,
        "mode": "rag",
        "model": ollama_chat_model(),
    }
