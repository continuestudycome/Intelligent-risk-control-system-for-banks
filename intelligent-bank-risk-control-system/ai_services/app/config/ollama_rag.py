"""Ollama RAG 相关环境配置（与 Java 侧 ai.service 独立，直连本机 Ollama）。"""
from __future__ import annotations

import os


def ollama_base() -> str:
    return os.getenv("OLLAMA_BASE", "http://127.0.0.1:11434").rstrip("/")


def ollama_embed_model() -> str:
    return os.getenv("OLLAMA_EMBED_MODEL", "nomic-embed-text")


def ollama_chat_model() -> str:
    # 默认用 7B 兼顾体量与中文效果；更小可选 qwen2.5:3b / qwen2.5:1.5b，环境变量 OLLAMA_CHAT_MODEL 覆盖
    return os.getenv("OLLAMA_CHAT_MODEL", "qwen2.5:7b")
