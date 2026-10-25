#!/usr/bin/env python3
"""原生客户端打包／安装／运行入口。仅显式调用对应动作时执行构建或启动。"""
import argparse
import ipaddress
import os
from pathlib import Path
import plistlib
import re
import shutil
import subprocess
import sys
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
KMP = ROOT / "kmp"
IOS = KMP / "iosApp"
ARTIFACTS = ROOT / "artifacts"

def tool(name):
    if not shutil.which(name): raise ValueError("缺少工具：" + name)
    return name

def call(command, cwd=ROOT, env=None):
    subprocess.run(command, cwd=cwd, env=env, check=True)

def origin(value, release=False):
    u = urlsplit(value)
    if u.scheme not in (["https"] if release else ["https", "http"]) or not u.hostname or u.username or u.password or u.query or u.fragment or u.path not in ("", "/"):
        raise ValueError("API地址必须是无凭据、无路径的origin；Release只允许HTTPS")
    try: ipaddress.ip_address(u.hostname)
    except ValueError:
        if not re.fullmatch(r"[a-zA-Z0-9.-]+", u.hostname): raise ValueError("API主机名含无效字符")
    if u.port is not None and not 1 <= u.port <= 65535: raise ValueError("API端口超出范围")
    if release and (u.hostname in ["localhost", "127.0.0.1", "::1"] or u.hostname.endswith((".invalid", ".test", ".example"))): raise ValueError("Release需要真实API地址，不能使用示例或loopback")
    return value.rstrip("/")

def gradle(tasks, args):
    tool("java")
    wrapper = KMP / ("gradlew.bat" if os.name == "nt" else "gradlew")
    command = [str(wrapper), "--no-daemon", "--max-workers=2", *tasks]
    if args.api_url: command.append("-Pmeow.api.base.url=" + origin(args.api_url, args.action == "release"))
    env = os.environ.copy()
    if args.platform == "desktop" and args.api_url: env["MND_API_BASE_URL"] = origin(args.api_url)
    call(command, KMP, env)

def android(args):
    if args.action not in ["debug", "release", "install", "run"]: raise ValueError("Android动作：debug/release/install/run")
    if args.action == "release":
        for name in ["MND_ANDROID_KEYSTORE", "MND_ANDROID_KEY_ALIAS", "MND_ANDROID_STORE_PASSWORD", "MND_ANDROID_KEY_PASSWORD"]:
            if not os.environ.get(name): raise ValueError("缺少发布配置：" + name)
        if not Path(os.environ["MND_ANDROID_KEYSTORE"]).is_file(): raise ValueError("签名文件不存在")
        if not args.api_url: raise ValueError("Release需要--api-url，禁止沿用模拟器地址")
        origin(args.api_url, True)
        gradle([":androidApp:assembleRelease", ":androidApp:bundleRelease"], args)
        source = KMP / "androidApp/build/outputs"
        if not list((source / "apk/release").glob("*.apk")) or not list((source / "bundle/release").glob("*.aab")): raise ValueError("本次构建未提供APK及AAB")
        dest = ARTIFACTS / "android/release"; dest.mkdir(parents=True, exist_ok=True)
        for relative in ["apk/release", "bundle/release"]:
            for path in (KMP / "androidApp/build/outputs" / relative).glob("*"):
                if path.suffix in [".apk", ".aab"]: shutil.copy2(path, dest / path.name)
        if not list(dest.glob("*.apk")) or not list(dest.glob("*.aab")): raise ValueError("未找到本次APK及AAB")
    else:
        apk = args.artifact or KMP / "androidApp/build/outputs/apk/debug/androidApp-debug.apk"
        if args.action in ["debug", "run"]: gradle([":androidApp:assembleDebug"], args)
        if not apk.is_file(): raise ValueError("找不到APK；先debug或指定--artifact")
        if args.action == "debug":
            dest = ARTIFACTS / "android/debug"; dest.mkdir(parents=True, exist_ok=True); shutil.copy2(apk, dest / apk.name)
        else:
            if not args.device: raise ValueError("请用--device明确选择设备序列号")
            adb = [tool("adb"), "-s", args.device]
            if args.reverse_port: call(adb + ["reverse", f"tcp:{args.reverse_port}", f"tcp:{args.reverse_port}"])
            call(adb + ["install", "-r", str(apk)])
            if args.action == "run": call(adb + ["shell", "am", "start", "-W", "-n", "com.ttkk0000.meowcircle.kmpapp/.MainActivity"])

def prepare_ios():
    if sys.platform != "darwin": raise ValueError("iOS打包需要macOS与完整Xcode")
    tool("xcodebuild");tool("xcrun")
    call([tool("xcodegen"), "generate", "--spec", "project.yml"], IOS)
    command = [tool("pod"), "install"]
    if (IOS / "Podfile.lock").exists(): command.append("--deployment")
    call(command, IOS)

def ios_settings(args, release=False):
    settings = []
    endpoint = args.api_url or os.environ.get("MND_API_BASE_URL", "http://127.0.0.1:8080")
    settings.append("MND_API_BASE_URL=" + origin(endpoint, release))
    public = ["MND_AMAP_IOS_KEY", "MND_TERMS_URL", "MND_PRIVACY_URL", "MND_WECHAT_IOS_APP_ID", "MND_QQ_IOS_APP_ID", "MND_WECHAT_UNIVERSAL_LINK", "MND_QQ_UNIVERSAL_LINK", "MND_APPLE_SIGNIN_ENABLED", "MND_PROVIDER_ASSOCIATED_DOMAIN", "MND_SHARE_ASSOCIATED_DOMAIN", "MND_APNS_ENABLED"]
    for name in public:
        if value := os.environ.get(name): settings.append(name + "=" + value)
    if version := os.environ.get("MND_VERSION_NAME"):
        if not re.fullmatch(r"\d+\.\d+(?:\.\d+)?", version): raise ValueError("iOS版本号必须是数字版本")
        settings.append("MARKETING_VERSION=" + version)
    if version := os.environ.get("MND_VERSION_CODE"):
        if not re.fullmatch(r"[1-9]\d{0,8}", version): raise ValueError("iOS构建号必须是正整数")
        settings.append("CURRENT_PROJECT_VERSION=" + version)
    style = os.environ.get("MND_IOS_SIGNING_STYLE", "Automatic")
    if style not in ["Automatic", "Manual"]: raise ValueError("签名方式必须是Automatic或Manual")
    settings.append("CODE_SIGN_STYLE=" + style)
    for name, setting in [("MND_IOS_PROFILE_SPECIFIER", "PROVISIONING_PROFILE_SPECIFIER"), ("MND_IOS_CODE_SIGN_IDENTITY", "CODE_SIGN_IDENTITY")]:
        if value := os.environ.get(name): settings.append(setting + "=" + value)
    return settings

def ios(args):
    if args.action == "prepare": return prepare_ios()
    if args.action == "framework":
        if sys.platform != "darwin": raise ValueError("iOS framework需要macOS/Xcode环境")
        return gradle([":iosUi:embedAndSignAppleFrameworkForXcode"], args)
    if args.action in ["simulator", "archive"]:
        settings = ios_settings(args, args.action == "archive")
        if args.action == "archive" and not re.fullmatch(r"[A-Z0-9]{10}", os.environ.get("MND_IOS_TEAM_ID", "")): raise ValueError("Archive需要真实MND_IOS_TEAM_ID及本机签名证书")
        prepare_ios()
        build = ["xcodebuild", "-workspace", "MndIOS.xcworkspace", "-scheme", "MndIOS"]
        if args.action == "simulator":
            call(build + ["-configuration", "Debug", "-sdk", "iphonesimulator", "-destination", "generic/platform=iOS Simulator", "-derivedDataPath", str(ARTIFACTS / "ios/simulator"), "CODE_SIGNING_ALLOWED=NO", *settings, "build"], IOS)
        else:
            team = os.environ.get("MND_IOS_TEAM_ID", "")
            if not re.fullmatch(r"[A-Z0-9]{10}", team): raise ValueError("Archive需要真实MND_IOS_TEAM_ID及本机签名证书")
            entitlements = {}
            domains = []
            for name in ["MND_PROVIDER_ASSOCIATED_DOMAIN", "MND_SHARE_ASSOCIATED_DOMAIN"]:
                domain = os.environ.get(name, "").lower()
                if not domain: continue
                if not re.fullmatch(r"[a-zA-Z0-9.-]+", domain) or domain.endswith((".invalid", ".example", ".test")): raise ValueError("Associated Domain必须是真实域名")
                if name == "MND_SHARE_ASSOCIATED_DOMAIN":
                    endpoint = urlsplit(args.api_url or os.environ.get("MND_API_BASE_URL", ""))
                    if endpoint.scheme != "https" or endpoint.hostname != domain: raise ValueError("分享Associated Domain必须与HTTPS API主机一致")
                if domain not in domains: domains.append(domain)
            if domains: entitlements["com.apple.developer.associated-domains"] = ["applinks:" + domain for domain in domains]
            if os.environ.get("MND_APPLE_SIGNIN_ENABLED") == "YES": entitlements["com.apple.developer.applesignin"] = ["Default"]
            if os.environ.get("MND_APNS_ENABLED") == "YES":
                apns = os.environ.get("MND_APNS_ENVIRONMENT") or "production"
                if apns not in ["development", "production"]: raise ValueError("APNs环境必须是development或production")
                entitlements["aps-environment"] = apns
            target = ARTIFACTS / "ios"; target.mkdir(parents=True, exist_ok=True)
            ent = target / "MndArchive.entitlements"; ent.write_bytes(plistlib.dumps(entitlements))
            call(build + ["-configuration", "Release", "-destination", "generic/platform=iOS", "-archivePath", str(target / "MndIOS.xcarchive"), "DEVELOPMENT_TEAM=" + team, "MND_ENTITLEMENTS_FILE=" + str(ent), *settings, "archive"], IOS)
    elif args.action == "export":
        archive = args.artifact or ARTIFACTS / "ios/MndIOS.xcarchive"
        if not archive.is_dir() or not args.export_options or not args.export_options.is_file(): raise ValueError("export需要Archive及--export-options文件")
        options = plistlib.loads(args.export_options.read_bytes())
        if options.get("destination", "export") != "export": raise ValueError("此入口仅导出IPA，不上传商店")
        if not re.fullmatch(r"[A-Z0-9]{10}", str(options.get("teamID", ""))): raise ValueError("ExportOptions需要真实teamID，不能沿用示例")
        call([tool("xcodebuild"), "-exportArchive", "-archivePath", str(archive.resolve()), "-exportOptionsPlist", str(args.export_options.resolve()), "-exportPath", str(ARTIFACTS / "ios/export")], IOS)
    elif args.action in ["install", "run"]:
        if not args.device: raise ValueError("用--device明确选择模拟器UDID或真机标识")
        app = args.artifact or ARTIFACTS / ("ios/MndIOS.xcarchive/Products/Applications/M&D.app" if args.physical else "ios/simulator/Build/Products/Debug-iphonesimulator/M&D.app")
        if not app.is_dir(): raise ValueError("请先构建或用--artifact指定已签名.app")
        info = plistlib.loads((app / "Info.plist").read_bytes());bundle = info["CFBundleIdentifier"]
        if args.physical:
            call([tool("xcrun"), "devicectl", "device", "install", "app", "--device", args.device, str(app.resolve())])
            if args.action == "run": call(["xcrun", "devicectl", "device", "process", "launch", "--device", args.device, bundle])
        else:
            call([tool("xcrun"), "simctl", "bootstatus", args.device, "-b"])
            call(["xcrun", "simctl", "install", args.device, str(app.resolve())])
            if args.action == "run": call(["xcrun", "simctl", "launch", args.device, bundle])
    else: raise ValueError("不支持的iOS动作")

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("platform", choices=["android", "ios", "desktop"])
    p.add_argument("action")
    p.add_argument("--api-url")
    p.add_argument("--artifact", type=Path)
    p.add_argument("--device")
    p.add_argument("--physical", action="store_true")
    p.add_argument("--reverse-port", type=int, choices=range(1, 65536), metavar="PORT")
    p.add_argument("--export-options", type=Path)
    args = p.parse_args()
    try:
        if args.platform == "android": android(args)
        elif args.platform == "ios": ios(args)
        elif args.action in ["package", "run"]: gradle([":desktopApp:packageDistributionForCurrentOS" if args.action == "package" else ":desktopApp:run"], args)
        else: raise ValueError("Desktop动作：package/run")
    except (ValueError, OSError, subprocess.CalledProcessError) as failure:
        print(str(failure) if isinstance(failure, ValueError) else "客户端交付命令未完成，请检查工具或构建日志；不会继续安装或上传", file=sys.stderr)
        return 1
    return 0
if __name__ == "__main__": sys.exit(main())
