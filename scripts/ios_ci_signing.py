#!/usr/bin/env python3
"""仅手动iOS CI调用：临时Keychain、描述文件与IPA导出；不上传商店。"""
import base64
import json
import os
from pathlib import Path
import plistlib
import secrets
import shutil
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[1]

def run(argv, capture=False):
    return subprocess.run(argv, check=True, capture_output=capture, text=capture)

def private(path, value):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "wb") as handle: handle.write(value)

def data(name):
    raw = os.environ.get(name, "")
    if not raw: raise ValueError("缺少签名配置：" + name)
    return base64.b64decode("".join(raw.split()), validate=True)

def main():
    if os.environ.get("GITHUB_ACTIONS") != "true": raise ValueError("此脚本仅用于手动CI，不修改本机开发者Keychain")
    directory = Path(os.environ["RUNNER_TEMP"]) / "mnd-ios-signing"
    state = directory / "state.json"
    action = sys.argv[1] if len(sys.argv)>1 else ""
    if action == "prepare":
        directory.mkdir(mode=0o700, exist_ok=False)
        keychain = directory / "build.keychain-db"
        old = run(["security", "list-keychains", "-d", "user"], True).stdout
        # 保存原搜索列表，不改默认钥匙串；always步骤恢复。
        import shlex
        settings = {"keychain": str(keychain), "old": shlex.split(old), "profile": ""}
        private(state, json.dumps(settings).encode())
        certificate = directory / "certificate.p12";private(certificate, data("CERTIFICATE_DATA"))
        password = secrets.token_urlsafe(36)
        run(["security", "create-keychain", "-p", password, str(keychain)])
        run(["security", "set-keychain-settings", "-lut", "21600", str(keychain)])
        run(["security", "unlock-keychain", "-p", password, str(keychain)])
        run(["security", "import", str(certificate), "-P", os.environ.get("CERTIFICATE_PASSWORD", ""), "-k", str(keychain), "-T", "/usr/bin/codesign", "-T", "/usr/bin/security"])
        run(["security", "set-key-partition-list", "-S", "apple-tool:,apple:", "-s", "-k", password, str(keychain)])
        run(["security", "list-keychains", "-d", "user", "-s", str(keychain), *settings["old"]])
        profile = directory / "profile.mobileprovision";private(profile, data("PROFILE_DATA"))
        decoded = run(["security", "cms", "-D", "-i", str(profile)], True).stdout
        info = plistlib.loads(decoded.encode());identifier = str(uuid.UUID(info["UUID"]))
        target = Path.home() / "Library/Developer/Xcode/UserData/Provisioning Profiles" / (identifier + ".mobileprovision")
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists(): raise ValueError("描述文件已存在，拒绝覆盖")
        settings["profile"] = str(target)
        state.write_text(json.dumps(settings));state.chmod(0o600)
        private(target, profile.read_bytes())
        import re
        identities = re.findall(r'"([^"\n]+)"', run(["security", "find-identity", "-v", "-p", "codesigning", str(keychain)], True).stdout)
        if len(identities) != 1: raise ValueError("需要唯一有效签名身份")
        with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as handle:
            handle.write("MND_IOS_SIGNING_STYLE=Manual\nMND_IOS_PROFILE_SPECIFIER=" + identifier + "\nMND_IOS_CODE_SIGN_IDENTITY=" + identities[0] + "\n")
        print("临时签名材料已配置；未上传任何App")
    elif action == "export":
        options = directory / "ExportOptions.plist";private(options, data("EXPORT_OPTIONS_DATA"))
        parsed = plistlib.loads(options.read_bytes())
        if parsed.get("destination", "export") != "export": raise ValueError("只允许本地导出，不允许上传")
        run([sys.executable, str(ROOT / "scripts/mobile.py"), "ios", "export", "--export-options", str(options)])
    elif action == "cleanup":
        failed = False
        try:
            if state.exists():
                settings = json.loads(state.read_text())
                commands = []
                if settings["old"]: commands.append(["security", "list-keychains", "-d", "user", "-s", *settings["old"]])
                if Path(settings["keychain"]).exists(): commands.append(["security", "delete-keychain", settings["keychain"]])
                for command in commands:
                    try: run(command)
                    except subprocess.CalledProcessError: failed = True
                profile = settings.get("profile")
                if profile: Path(profile).unlink(missing_ok=True)
        finally: shutil.rmtree(directory, ignore_errors=True)
        if failed: raise ValueError("临时签名清理有未完成项")
    else: raise ValueError("签名动作：prepare/export/cleanup")
if __name__ == "__main__":
    try: main()
    except (ValueError,OSError,subprocess.CalledProcessError,KeyError):
        # CalledProcessError含完整参数，不能打印，避免证书密码进入日志。
        print("iOS临时签名／导出未完成，请检查配置；不输出凭据", file=sys.stderr)
        sys.exit(1)
