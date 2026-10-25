"""只读检查部署／打包接线，不导入执行交付脚本，不调用构建或服务。"""
import ast
import json
from pathlib import Path
import plistlib
import re

root = Path(__file__).resolve().parents[1]
def read(name): return (root/name).read_text(encoding="utf-8")
for name in ["scripts/delivery.py", "scripts/mobile.py", "scripts/ios_ci_signing.py"]:
    tree=ast.parse(read(name))
    assert any(isinstance(n,ast.If) and "__name__" in ast.unparse(n.test) for n in tree.body),name
compose=read("deploy/compose.production.yml")
services=compose.split("services:\n",1)[1].split("\nsecrets:\n",1)[0]
names=set(re.findall(r"^  ([a-z0-9_-]+):",services,re.M))
jobs=["push","nearby_alerts","content_updates","privacy_cleanup","provider_revocations","maintenance","search_projection","audit_projection"]
assert names=={"api","postgres","redis","migrate","caddy"}|{"worker-"+j.replace("_","-") for j in jobs}
for j in jobs:
    assert "MND_WORKER_JOB: "+j in compose and f"worker_{j}_data:/app/worker-data" in compose
assert 'MND_WORKER_API_URL: https://' in compose and 'MND_WORKER_ALLOW_LOOPBACK_HTTP: "0"' in compose
for guard in ['service_completed_successfully','DATABASE_URL_FILE: /run/secrets/database_url','APP_ENV: production','MND_ALLOW_MEMORY_MOBILE: "0"','MND_ENABLE_LEGACY_AUTH: "0"','media_data:/app/data','read_only: true']:
    assert guard in compose,guard
assert 'ports:' not in services.split('  postgres:',1)[1].split('  migrate:',1)[0]
ignore=read('.dockerignore')
for guard in ['.env.*','**/secrets/','**/*.jks','**/*.p12','kmp/','deploy/local/','**/*.env']:
    assert guard in ignore,guard
image=read('Dockerfile')
for command in ['cmd/server','cmd/worker','cmd/migrate','cmd/healthcheck','npm run typecheck','/app/meow-healthcheck','--chown=65532:65532']:
    assert command in image,command
assert not re.search(r'^ARG .*SECRET|^ARG .*PASSWORD|^ARG .*TOKEN',image,re.M)
loader=read('internal/platform/envfile/secrets.go')
for guard in ['mutually exclusive','io.LimitReader','strings.TrimRight','os.Setenv']:
    assert guard in loader,guard
assert 'envfile.LoadServer()' in read('cmd/server/main.go')
assert 'envfile.Load("MND_WORKER_OPERATOR_TOKEN")' in read('cmd/worker/main.go')
runner=read('cmd/migrate/main.go')
for guard in ['pg_advisory_lock','mnd_schema_migrations','sha256.Sum256','-baseline-through','baseline filename','tx.Commit','transaction rolled back']:
    assert guard in runner or guard.lstrip('-') in runner,guard
files=sorted((root/'migrations').glob('[0-9][0-9][0-9]_*.sql'))
assert len(files)>=9
for i,file in enumerate(files,1):assert file.name.startswith(f'{i:03d}_')
android=read('kmp/androidApp/build.gradle.kts')
for guard in ['MND_ANDROID_KEYSTORE','MND_ANDROID_STORE_PASSWORD','MND_ANDROID_KEY_PASSWORD','signingConfigs','requireReleaseConfiguration','signingConfig = signingConfigs.getByName("release")']:
    assert guard in android,guard
assert 'org.gradle.java.home=' not in read('kmp/gradle.properties')
examples = root/'deploy/examples'
def values(name):
    return {key.strip():value.strip().strip('"') for line in (examples/name).read_text().splitlines() if '=' in line and not line.lstrip().startswith('#') for key,value in [line.split('=',1)]}
example = values('compose.env.example')
assert example['MND_CONFIG_MODE']=='placeholder' and example['MND_DOMAIN'].endswith('.invalid')
assert 'REPLACE_' in example['MND_DEPLOY_DIR']
for field in ['MND_ANDROID_STORE_PASSWORD','MND_ANDROID_KEY_PASSWORD']:
    assert values('android.env.example')[field]==''
assert values('ios.env.example')['MND_IOS_TEAM_ID']=='REPLACE_WITH_TEAM_ID'
assert values('ios.env.example')['MND_APPLE_SIGNIN_ENABLED']=='NO'
assert 'REPLACE_' in values('server-target.env.example')['DEPLOY_SSH_HOST']
script = read('scripts/delivery.py')
assert 'MND_CONFIG_MODE' in script and '== "placeholder"' in script
assert script.index('== "placeholder"') < script.index('shutil.which("docker")')
mobile=read('scripts/mobile.py')
for guard in [':androidApp:bundleRelease',':iosUi:embedAndSignAppleFrameworkForXcode','MndIOS.xcworkspace','MND_IOS_TEAM_ID','-exportArchive','devicectl','simctl','--device','--no-daemon','destination']:
    assert guard in mobile,guard
assert 'MND_IOS_SIGNING_STYLE' in mobile and 'PROVISIONING_PROFILE_SPECIFIER' in mobile
plistlib.loads((root/'kmp/iosApp/ExportOptions.example.plist').read_bytes())
assert plistlib.loads((root/'kmp/iosApp/ComposeApp/MndBase.entitlements').read_bytes())=={}
assert ':shared:embedAndSignAppleFrameworkForXcode' not in read('Makefile')
ci=read('.github/workflows/app-packages.yml')
assert 'workflow_dispatch:' in ci and not re.search(r'^  (push|pull_request):',ci,re.M)
for guard in ['android-release','ios-archive','macos-26','always()','scripts/ios_ci_signing.py cleanup','scripts/mobile.py desktop package']:
    assert guard in ci,guard
assert 'run: go run ./cmd/migrate' in read('.github/workflows/ci.yml')
assert '"include": ["src"]' in read('web-client/tsconfig.json')
json.loads(read('web-client/tsconfig.json'))
print(f'交付静态核对：{len(names)}服务／{len(jobs)}独立worker、{len(files)}迁移与账本、Secret文件加载、Android APK/AAB签名、iOS Archive/IPA/设备入口、Desktop及手动App CI；未构建／迁移／打包／安装／启动。')
