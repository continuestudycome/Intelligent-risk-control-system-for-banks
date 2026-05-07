import json
import threading
import time

import requests


class NacosRegister:
    def __init__(
        self,
        service_name,
        service_ip,
        service_port,
        nacos_addr="http://localhost:8848",
        namespace_id="public",
        group_name="DEFAULT_GROUP",
        cluster_name="DEFAULT",
    ):
        self.service_name = service_name
        self.service_ip = service_ip
        self.service_port = service_port
        self.nacos_addr = nacos_addr.rstrip("/")
        self.namespace_id = namespace_id
        self.group_name = group_name
        self.cluster_name = cluster_name
        self.instance_id = f"{service_ip}:{service_port}"

    def register(self):
        """注册服务到 Nacos"""
        url = f"{self.nacos_addr}/nacos/v1/ns/instance"
        params = {
            "serviceName": self.service_name,
            "groupName": self.group_name,
            "namespaceId": self.namespace_id,
            "clusterName": self.cluster_name,
            "ip": self.service_ip,
            "port": self.service_port,
            "weight": 1.0,
            "enable": True,
            "healthy": True,
            "ephemeral": True,
            "metadata": json.dumps(
                {"source": "ai_services", "protocol": "http"},
                ensure_ascii=False,
            ),
        }

        try:
            response = requests.post(url, params=params, timeout=5)
            if response.status_code == 200:
                print(f"✓ 服务 [{self.service_name}] 注册成功: {self.service_ip}:{self.service_port}")
                return True
            print(f"✗ 服务注册失败: status={response.status_code}, body={response.text}")
            return False
        except Exception as e:
            print(f"✗ 服务注册异常: {e}")
            return False

    def deregister(self):
        """从 Nacos 注销服务"""
        url = f"{self.nacos_addr}/nacos/v1/ns/instance"
        params = {
            "serviceName": self.service_name,
            "groupName": self.group_name,
            "namespaceId": self.namespace_id,
            "clusterName": self.cluster_name,
            "ip": self.service_ip,
            "port": self.service_port,
            "ephemeral": True,
        }

        try:
            response = requests.delete(url, params=params, timeout=5)
            if response.status_code == 200:
                print(f"✓ 服务 [{self.service_name}] 注销成功")
                return True
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
            "groupName": self.group_name,
            "namespaceId": self.namespace_id,
            "clusterName": self.cluster_name,
            "ip": self.service_ip,
            "port": self.service_port,
            "beat": json.dumps(
                {
                    "ip": self.service_ip,
                    "port": self.service_port,
                    "serviceName": self.service_name,
                    "groupName": self.group_name,
                    "cluster": self.cluster_name,
                    "ephemeral": True,
                },
                ensure_ascii=False,
            ),
        }

        try:
            response = requests.put(url, params=params, timeout=5)
            return response.status_code == 200
        except Exception:
            return False

    def start_heartbeat_timer(self):
        """启动心跳定时器（每 5 秒一次）"""

        def heartbeat_loop():
            while True:
                time.sleep(5)
                self.heartbeat()

        threading.Thread(target=heartbeat_loop, daemon=True).start()
