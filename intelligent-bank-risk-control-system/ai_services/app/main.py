"""
智能服务入口：在 ai_services 目录执行
  uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
"""

from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import chat_rag, credit, fraud, health, kb_extract, ocr, rule_fraud
from app.config.settings import load_service_settings
from app.integrations.nacos import NacosRegister
from app.services.credit_score_lr import credit_lr_model
from app.services.fraud_isolation import fraud_isolation_service

_nacos_ref: dict[str, NacosRegister | None] = {"client": None}


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = load_service_settings()
    reg = NacosRegister(
        service_name="ai-service",
        service_ip=settings.service_ip,
        service_port=settings.service_port,
        nacos_addr=settings.nacos_addr,
        namespace_id=settings.nacos_namespace,
        group_name=settings.nacos_group,
    )
    _nacos_ref["client"] = reg
    reg.register()
    reg.start_heartbeat_timer()
    fraud_isolation_service.fit_default()
    credit_lr_model.fit_default()
    yield
    client = _nacos_ref.get("client")
    if client:
        client.deregister()
    _nacos_ref["client"] = None


def create_app() -> FastAPI:
    application = FastAPI(title="AI Service", lifespan=lifespan)
    application.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=False,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    application.include_router(health.router)
    application.include_router(fraud.router)
    application.include_router(ocr.router)
    application.include_router(credit.router)
    application.include_router(rule_fraud.router)
    application.include_router(chat_rag.router)
    application.include_router(kb_extract.router)
    return application


app = create_app()
