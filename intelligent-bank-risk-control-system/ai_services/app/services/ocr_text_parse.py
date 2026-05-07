"""OCR 全文解析为结构化字段（身份证 / 营业执照）。"""

from __future__ import annotations

import re
from typing import Optional


def infer_doc_type(document_type: Optional[str], filename: str) -> str:
    if document_type and document_type.strip():
        return document_type.strip().upper()
    lower = filename.lower()
    if "营业执照" in lower or "license" in lower:
        return "BUSINESS_LICENSE"
    return "ID_CARD"


def extract_id_card_fields(full_text: str, text_lines: list[str]) -> dict:
    name_match = re.search(r"(?:姓名|名)[:：]?\s*([\u4e00-\u9fa5·]{2,20})", full_text)
    id_match = re.search(r"\b\d{17}[0-9Xx]\b", full_text)
    if not id_match:
        compact_text = re.sub(r"[\s\-–—_]", "", full_text)
        id_match = re.search(r"\d{17}[0-9Xx]", compact_text)
    addr_match = re.search(
        r"(?:住址|地址)[:：]?\s*([\u4e00-\u9fa5A-Za-z0-9\-#号室栋单元路街巷区县市省]{6,120})",
        full_text,
    )

    name_val = name_match.group(1) if name_match else None
    id_val = id_match.group(0).upper() if id_match else None
    addr_val = addr_match.group(1).strip() if addr_match else None

    if not name_val:
        for line in text_lines:
            candidate = (
                line.strip().replace("姓名", "").replace(":", "").replace("：", "").strip()
            )
            if re.fullmatch(r"[\u4e00-\u9fa5·]{2,10}", candidate):
                name_val = candidate
                break

    if not addr_val:
        for line in text_lines:
            text = line.strip()
            if len(text) >= 6 and any(k in text for k in ["省", "市", "区", "县", "路", "街", "号"]):
                addr_val = (
                    text.replace("住址", "")
                    .replace("地址", "")
                    .replace("：", "")
                    .replace(":", "")
                    .strip()
                )
                if len(addr_val) >= 6:
                    break

    return {
        "realName": name_val,
        "idCardNo": id_val,
        "address": addr_val,
    }


def extract_business_fields(full_text: str, text_lines: list[str]) -> dict:
    company_match = re.search(
        r"([\u4e00-\u9fa5A-Za-z0-9（）()·\s]{4,80}(?:公司|企业|银行|合作社))",
        full_text,
    )
    code_match = re.search(r"\b[0-9A-Z]{18}\b", full_text)
    if not code_match:
        compact_text = re.sub(r"[\s\-–—_]", "", full_text).upper()
        code_match = re.search(r"[0-9A-Z]{18}", compact_text)
    addr_match = re.search(
        r"(?:住所|地址|营业场所)[:：]?\s*([\u4e00-\u9fa5A-Za-z0-9\-#号室栋单元路街巷区县市省]{6,120})",
        full_text,
    )

    addr_val = addr_match.group(1).strip() if addr_match else None
    if not addr_val:
        for line in text_lines:
            text = line.strip()
            if len(text) >= 6 and any(k in text for k in ["省", "市", "区", "县", "路", "街", "号"]):
                addr_val = (
                    text.replace("住所", "")
                    .replace("地址", "")
                    .replace("营业场所", "")
                    .replace("：", "")
                    .replace(":", "")
                    .strip()
                )
                if len(addr_val) >= 6:
                    break

    return {
        "realName": company_match.group(1).strip() if company_match else None,
        "idCardNo": code_match.group(0).upper() if code_match else None,
        "address": addr_val,
    }
