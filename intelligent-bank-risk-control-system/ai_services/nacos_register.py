import requests
import time
import threading
from fastapi import FastAPI

class NacosRegister:
    def __init__(self, service_name, service_ip, service_port, nacos_addr="http://localhost:8848"):
        self.service_name = service_name
        self.service_ip = service_ip
        self.service_port = service_port
        self.nacos_addr = nacos_addr
        self.instance_id = f"{service_ip}:{service_port}"

    def register(self):
        """注册服务到 Nacos"""
        url = f"{self.nacos_addr}/nacos/v1/ns/instance"
        params = {
            "serviceName": self.service_name,
            "ip": self.service_ip,
            "port": self.service_port,
            "weight": 1.0,
            "enable": True,
            "healthy": True,
            "ephemeral": True,
            "metadata": '{"preserved.heart.beat.interval": 3000, "preserved.heart.beat.timeout": 3000, "preserved.ip.delete.timeout": 3000}'
        }

        try:
            response = requests.put(url, params=params)
            if response.status_code == 200:
                print(f"✓ 服务 [{self.service_name}] 注册成功: {self.service_ip}:{self.service_port}")
                return True
            else:
                print(f" 服务注册失败: {response.text}")
                return False
        except Exception as e:
            print(f"✗ 服务注册异常: {e}")
            return False

    def deregister(self):
        """从 Nacos 注销服务"""
        url = f"{self.nacos_addr}/nacos/v1/ns/instance"
        params = {
            "serviceName": self.service_name,
            "ip": self.service_ip,
            "port": self.service_port,
            "ephemeral": True
        }

        try:
            response = requests.delete(url, params=params)
            if response.status_code == 200:
                print(f"✓ 服务 [{self.service_name}] 注销成功")
                return True
            else:
                print(f"✗ 服务注销失败: {response.text}")
                return False
        except Exception as e:
            print(f"✗ 服务注销异常: {e}")
            return False

    def heartbeat(self):
        """心跳保持"""
        url = f"{self.nacos_addr}/nacos/v1/ns/instance/beat"
        params = {
            "serviceName": self.service_name,
            "ip": self.service_ip,
            "port": self.service_port,
            "beat": f'{{"ip":"{self.service_ip}","port":{self.service_port},"serviceName":"{self.service_name}"}}'
        }

        try:
            response = requests.put(url, params=params)
            return response.status_code == 200
        except:
            return False

    def start_heartbeat_timer(self):
        """启动心跳定时器（每 5 秒一次）"""
        def heartbeat_loop():
            while True:
                time.sleep(5)
                self.heartbeat()

        timer_thread = threading.Thread(target=heartbeat_loop, daemon=True)
        timer_thread.start()
