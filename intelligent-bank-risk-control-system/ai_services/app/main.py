from fastapi import FastAPI
from nacos_register import NacosRegister

app = FastAPI()

nacos_register = None

@app.on_event("startup")
async def startup_event():
    global nacos_register
    nacos_register = NacosRegister(
        service_name="ai-service",
        service_ip="127.0.0.1",
        service_port=8000,
        nacos_addr="http://localhost:8848"
    )
    
    nacos_register.register()
    nacos_register.start_heartbeat_timer()

@app.on_event("shutdown")
async def shutdown_event():
    global nacos_register
    if nacos_register:
        nacos_register.deregister()

@app.get("/")
async def root():
    return {"message": "AI Service is running", "service": "ai-service"}


@app.get("/hello/{name}")
async def say_hello(name: str):
    return {"message": f"Hello {name}", "service": "ai-service"}
