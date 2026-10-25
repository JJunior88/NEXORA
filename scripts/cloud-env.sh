# Source from bash: source /workspace/NEXORA/scripts/cloud-env.sh
export JAVA_HOME=/workspace/toolchains/jdk-21.0.8+9
export ANDROID_HOME=/workspace/toolchains/android-sdk
export ANDROID_USER_HOME=/workspace/toolchains/android-user
export GRADLE_USER_HOME=/workspace/toolchains/gradle-home
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
# The cloud requires its HTTPS proxy and managed trust store for JVM downloads.
if [ -n "${HTTPS_PROXY:-}" ]; then
    NEXORA_JAVA_NETWORK_OPTIONS="$(python3 - <<'PY'
import os, urllib.parse
p = urllib.parse.urlparse(os.environ['HTTPS_PROXY'])
print(f'-Dhttps.proxyHost={p.hostname} -Dhttps.proxyPort={p.port or 80} -Dhttp.proxyHost={p.hostname} -Dhttp.proxyPort={p.port or 80} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts')
PY
)"
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} $NEXORA_JAVA_NETWORK_OPTIONS"
    unset NEXORA_JAVA_NETWORK_OPTIONS
fi
