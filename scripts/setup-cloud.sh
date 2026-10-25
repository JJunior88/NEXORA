#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/cloud-env.sh
mkdir -p /workspace/toolchains/downloads "$ANDROID_USER_HOME" "$GRADLE_USER_HOME/init.d"
python3 - <<'PY'
import hashlib, os, pathlib, shutil, subprocess, tarfile, urllib.parse, zipfile
root = pathlib.Path('/workspace/toolchains')
downloads = root / 'downloads'
def download(url, name, expected, algorithm='sha256'):
    path = downloads / name
    def valid():
        return path.exists() and hashlib.file_digest(path.open('rb'), algorithm).hexdigest() == expected
    if not valid():
        subprocess.run(['curl', '-fSL', '--retry', '2', url, '-o', str(path)], check=True)
    if not valid():
        raise RuntimeError('Checksum mismatch: ' + name)
    return path
if not pathlib.Path(os.environ['JAVA_HOME'], 'bin/javac').exists():
    archive = download('https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9/OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz',
                       'OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz',
                       'f2dc5418092c43003db8f9005c4a286e1c0104fea96ccdd49e8ebd037cac9219')
    with tarfile.open(archive) as tar:
        tar.extractall(root, filter='data')
sdk = pathlib.Path(os.environ['ANDROID_HOME'])
manager = sdk / 'cmdline-tools/16.0/bin/sdkmanager'
if not manager.exists():
    # Google's signed HTTPS repository metadata publishes this SHA-1 archive checksum.
    archive = download('https://dl.google.com/android/repository/commandlinetools-linux-12266719_latest.zip',
                       'android-tools16.zip', '47e61d3bb57b5e907a74f225a767a767a8b4d7a5', 'sha1')
    staging = root / 'sdk16-unpack'
    staging.mkdir(exist_ok=True)
    subprocess.run(['unzip', '-q', '-o', str(archive), '-d', str(staging)], check=True)
    manager.parent.parent.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(staging / 'cmdline-tools'), str(manager.parent.parent))
proxy_init = pathlib.Path(os.environ['GRADLE_USER_HOME']) / 'init.d/cloud-proxy.gradle'
proxy_init.write_text('''import java.net.URI
['http', 'https'].each { protocol ->
    def address = System.getenv(protocol.toUpperCase() + '_PROXY')
    if (address) {
        def proxy = new URI(address)
        System.setProperty(protocol + '.proxyHost', proxy.host)
        System.setProperty(protocol + '.proxyPort', String.valueOf(proxy.port > 0 ? proxy.port : 80))
    }
}
System.setProperty('javax.net.ssl.trustStore', '/etc/ssl/certs/java/cacerts')
''')
args = [str(manager)]
proxy = os.environ.get('HTTPS_PROXY')
if proxy:
    parsed = urllib.parse.urlparse(proxy)
    args += ['--proxy=http', '--proxy_host=' + parsed.hostname, '--proxy_port=' + str(parsed.port or 80)]
env = dict(os.environ, JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts')
subprocess.run(args + ['--licenses'], input='y\n' * 100, text=True, env=env, check=True)
subprocess.run(args + ['platforms;android-35', 'build-tools;35.0.0', 'platform-tools'], env=env, check=True)
if not (sdk / 'platforms/android-35/android.jar').exists():
    raise RuntimeError('Android SDK installation incomplete')
PY
./gradlew --no-daemon --max-workers=2 testDebugUnitTest lintDebug assembleDebug
