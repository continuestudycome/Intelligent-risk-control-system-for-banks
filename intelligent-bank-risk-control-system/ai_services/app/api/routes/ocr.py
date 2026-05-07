from typing import Optional

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.services.ocr_pipeline import ocr_pipeline
from app.services.ocr_text_parse import infer_doc_type

router = APIRouter(tags=["ocr"])


@router.post("/ocr/extract")
async def ocr_extract(
    file: UploadFile = File(...),
    documentType: Optional[str] = Form(default=None),
):
    if not file.filename:
        raise HTTPException(status_code=400, detail="文件名不能为空")
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="上传文件为空")

    doc_type = infer_doc_type(documentType, file.filename)
    if doc_type not in ["ID_CARD", "BUSINESS_LICENSE"]:
        raise HTTPException(status_code=400, detail="documentType 仅支持 ID_CARD 或 BUSINESS_LICENSE")

    try:
        ocr_result, _ = ocr_pipeline.run_on_image_bytes(content)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR识别失败: {str(e)}") from e

    return ocr_pipeline.build_response(doc_type, ocr_result)
