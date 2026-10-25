#!/usr/bin/env python3
"""显式部署入口；导入、--help和静态解析不会构建或启动服务。"""
import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
JOBS = ("push", "nearby_alerts", "content_updates", "privacy_cleanup", "provider_revocations", "maintenance", "search_projection", "audit_projection")

def write(path, value, mode=0o600):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, mode)
    with os.fdopen(fd, "w", encoding="utf-8") as handle:
        handle.write(value)

def initialize(args):
    domain = args.domain.lower()
    if not re.fullmatch(r"[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?", domain) or "." not in domain or domain.endswith((".invalid", ".test", ".example")) or ".." in domain:
        raise ValueError("请提供实际HTTPS域名，不使用示例域名")
    if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9._/:@-]+", args.image) or args.image.endswith(":latest"):
        raise ValueError("请提供明确镜像版本，不使用latest")
    directory = args.directory.resolve()
    if directory.is_relative_to(ROOT) and directory != ROOT / "deploy/local": raise ValueError("仓库内私有配置只能放deploy/local；其他目录请放在仓库外")
    directory.mkdir(mode=0o700, parents=True, exist_ok=False)
    (directory / "secrets").mkdir(mode=0o700)
    (directory / "reading").mkdir()
    (directory / "provider-secrets").mkdir(mode=0o755)
    (directory / "associations/.well-known").mkdir(parents=True)
    password, redis = secrets.token_urlsafe(36), secrets.token_urlsafe(36)
    values = {"postgres_password": password, "redis_password": redis,
              "database_url": f"postgres://meow:{quote(password, safe='')}@postgres:5432/meow?sslmode=disable",
              "redis_url": f"redis://:{quote(redis, safe='')}@redis:6379/0",
              "jwt_secret": secrets.token_urlsafe(48), "admin_key": secrets.token_urlsafe(48)}
    operators = []
    for job in JOBS:
        token = secrets.token_urlsafe(48)
        values["worker_" + job] = token
        roles = ["privacy"] if job == "maintenance" else ["worker", "privacy"] if job == "privacy_cleanup" else ["worker"]
        operators.append({"id": "service-" + job, "token": token, "roles": roles, "consumers": [] if job == "maintenance" else [job]})
    values["operators_json"] = json.dumps(operators, ensure_ascii=False)
    # 主机父目录0700限制访问，文件0444供容器内nonroot读取；不改变Docker镜像用户。
    for name, value in values.items(): write(directory / "secrets" / name, value + "\n", 0o444)
    write(directory / "redis.conf", f"bind 0.0.0.0\nprotected-mode yes\nport 6379\nappendonly yes\ndir /data\nrequirepass {redis}\n", 0o444)
    write(directory / "runtime.env", (ROOT / "deploy/runtime.example.env").read_text())
    settings = {"MND_CONFIG_MODE": "production", "MND_DOMAIN": domain, "MND_IMAGE": args.image,
                "MND_SOURCE_DIR": str(ROOT), "MND_DEPLOY_DIR": str(directory),
                "MND_READING_CATALOG": "", "VITE_MND_TERMS_URL": "", "VITE_MND_PRIVACY_URL": "",
                "VITE_MND_AMAP_JS_KEY": "", "VITE_MND_WECHAT_WEB_APP_ID": "", "VITE_MND_QQ_WEB_APP_ID": ""}
    write(directory / "compose.env", "".join(k + "=" + json.dumps(v, ensure_ascii=False) + "\n" for k, v in settings.items()))
    print("已生成私有部署配置；没有启动服务，未创建消费者账号或人工核验身份。请配置DNS、已发布材料和真实渠道后再部署。")

def compose(args):
    config = args.directory.resolve() / "compose.env"
    if not config.is_file(): raise ValueError("缺少compose.env，请先init或指定已有私有目录")
    for line in config.read_text(encoding="utf-8").splitlines():
        if "=" not in line or line.lstrip().startswith("#"): continue
        name, value = line.split("=", 1)
        if name.strip() == "MND_CONFIG_MODE" and value.strip().strip("\"'").lower() == "placeholder":
            raise ValueError("当前是占位配置，未调用Docker；请先填写真实环境并按init流程配置生产部署")
    if not shutil.which("docker"): raise ValueError("需要Docker与Compose插件")
    return ["docker", "compose", "--project-directory", str(ROOT), "--env-file", str(config), "-f", str(ROOT / "deploy/compose.production.yml")]

def execute(args):
    if args.action == "init": return initialize(args)
    base = compose(args)
    if args.action == "build": command = ["build", "api"]
    elif args.action == "up":
        # 先停写入口和旧worker，再迁移；不假设旧版本可跨Schema在线写入。
        subprocess.run(base + ["stop", "caddy", "api"] + ["worker-" + j.replace("_", "-") for j in JOBS], check=True)
        subprocess.run(base + ["up", "-d", "--wait", "postgres", "redis"], check=True)
        subprocess.run(base + ["run", "--rm", "migrate", "-dir=/app/migrations"], check=True)
        command = ["up", "-d", "--wait", "--wait-timeout", "180"]
    elif args.action == "stop": command = ["stop"]
    elif args.action == "status": command = ["ps"]
    elif args.action == "logs": command = ["logs", "--tail", "100", args.service] if args.service else ["logs", "--tail", "100"]
    elif args.action == "migrate":
        command = ["run", "--rm", "migrate", "-dir=/app/migrations"]
        if args.baseline_through:
            if not re.fullmatch(r"\d{3}_[a-z0-9_]+\.sql", args.baseline_through): raise ValueError("基线必须是已核实的迁移文件名")
            command += ["-baseline-through=" + args.baseline_through]
    elif args.action == "backup":
        subprocess.run(base + ["stop", "caddy", "api"] + ["worker-" + j.replace("_", "-") for j in JOBS], check=True)
        target = args.output.resolve()
        if target.exists(): raise ValueError("备份文件已存在，拒绝覆盖")
        target.parent.mkdir(parents=True, exist_ok=True)
        fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        try:
            with os.fdopen(fd, "wb") as handle:
                subprocess.run(base + ["exec", "-T", "postgres", "sh", "-c", 'PGPASSWORD="$(cat /run/secrets/postgres_password)" exec pg_dump -U meow -d meow --format=custom'], stdout=handle, check=True)
        except BaseException:
            target.unlink(missing_ok=True)
            raise
        print("数据库备份已保存；媒体卷、渠道密钥及worker游标须按交接手册单独保存。")
        return
    else: raise ValueError("不支持的部署动作")
    subprocess.run(base + command, cwd=ROOT, check=True)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["init", "build", "up", "stop", "status", "logs", "migrate", "backup"])
    parser.add_argument("--directory", type=Path, default=ROOT / "deploy/local")
    parser.add_argument("--domain")
    parser.add_argument("--image")
    parser.add_argument("--service", choices=["api", "caddy", "postgres", "redis", "migrate"] + ["worker-" + j.replace("_", "-") for j in JOBS])
    parser.add_argument("--baseline-through")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.action == "init" and (not args.domain or not args.image): parser.error("init需要--domain与--image")
    if args.action == "backup" and not args.output: parser.error("backup需要--output")
    try: execute(args)
    except (ValueError, OSError, subprocess.CalledProcessError) as failure:
        # 不打印命令环境或连接字符串。
        print(str(failure) if isinstance(failure, ValueError) else "交付操作未完成，请核对工具、文件权限或服务日志", file=sys.stderr)
        return 1
    return 0
if __name__ == "__main__": sys.exit(main())
