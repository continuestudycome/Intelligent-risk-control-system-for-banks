import os
import socket
from dataclasses import dataclass


def resolve_service_ip() -> str:
    configured = os.getenv("AI_SERVICE_IP")
    if configured:
        return configured
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


@dataclass
class ServiceSettings:
    service_ip: str
    service_port: int
    nacos_addr: str
    nacos_namespace: str
    nacos_group: str


def load_service_settings() -> ServiceSettings:
    return ServiceSettings(
        service_ip=resolve_service_ip(),
        service_port=int(os.getenv("AI_SERVICE_PORT", "8000")),
        nacos_addr=os.getenv("NACOS_ADDR", "http://localhost:8848"),
        nacos_namespace=os.getenv("NACOS_NAMESPACE", "public"),
        nacos_group=os.getenv("NACOS_GROUP", "DEFAULT_GROUP"),
    )
