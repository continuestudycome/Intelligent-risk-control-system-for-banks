"""OCR 识别与字段组装（RapidOCR + 文本解析）。"""

from __future__ import annotations

import io
from typing import Any

import numpy as np
from PIL import Image
from rapidocr_onnxruntime import RapidOCR

from app.services import ocr_text_parse


class OcrPipeline:
    def __init__(self) -> None:
        self._engine: RapidOCR | None = None

    def _get_engine(self) -> RapidOCR:
        if self._engine is None:
            self._engine = RapidOCR()
        return self._engine

    def run_on_image_bytes(self, content: bytes) -> tuple[Any, np.ndarray]:
        image = Image.open(io.BytesIO(content)).convert("RGB")
        image_np = np.array(image)
        engine = self._get_engine()
        ocr_result, _ = engine(image_np)
        return ocr_result, image_np

    def build_response(
        self,
        doc_type: str,
        ocr_result: Any,
    ) -> dict:
        if not ocr_result:
            return {
                "documentType": doc_type,
                "confidenceHint": "低",
                "rawTextHint": "未识别到有效文本，请上传更清晰的证件图片",
            }

        texts: list[str] = []
        scores: list[float] = []
        for line in ocr_result:
            if len(line) >= 3:
                texts.append(str(line[1]))
                try:
                    scores.append(float(line[2]))
                except Exception:
                    pass

        full_text = " ".join(texts)
        avg_score = sum(scores) / len(scores) if scores else 0.0
        confidence_hint = "高" if avg_score >= 0.85 else ("中" if avg_score >= 0.65 else "低")

        if doc_type == "BUSINESS_LICENSE":
            fields = ocr_text_parse.extract_business_fields(full_text, texts)
        else:
            fields = ocr_text_parse.extract_id_card_fields(full_text, texts)

        return {
            "documentType": doc_type,
            "realName": fields.get("realName"),
            "idCardNo": fields.get("idCardNo"),
            "address": fields.get("address"),
            "confidenceHint": confidence_hint,
            "rawTextHint": f"识别文本片段：{full_text[:180]}",
        }


ocr_pipeline = OcrPipeline()
