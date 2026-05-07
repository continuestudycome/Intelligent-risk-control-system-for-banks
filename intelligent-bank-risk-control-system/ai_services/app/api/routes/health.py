from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/")
async def root():
    return {"message": "AI Service is running", "service": "ai-service"}


@router.get("/hello/{name}")
async def say_hello(name: str):
    return {"message": f"Hello {name}", "service": "ai-service"}
