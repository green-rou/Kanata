#!/usr/bin/env python3
import glob, hashlib, json, os, re, subprocess, sys, time

SDK = os.environ.get("ANDROID_SDK_ROOT", "/usr/local/lib/android/sdk")
BUILD_TOOLS = os.path.join(SDK, "build-tools",
                           sorted(os.listdir(os.path.join(SDK, "build-tools")))[-1])
AAPT2      = os.path.join(BUILD_TOOLS, "aapt2")
APKSIGNER  = os.path.join(BUILD_TOOLS, "apksigner")

REPO_DIR  = sys.argv[1] if len(sys.argv) > 1 else "repo"
REPO_URL  = os.environ.get("REPO_URL", "")
REPO_NAME = os.environ.get("REPO_NAME", "Repo")
REPO_DESC = os.environ.get("REPO_DESC", "")


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def apk_info(apk_path):
    out = subprocess.run([AAPT2, "dump", "badging", apk_path],
                         capture_output=True, text=True).stdout

    def find(pattern):
        m = re.search(pattern, out)
        return m.group(1) if m else ""

    return {
        "packageName":      find(r"package: name='([^']+)'"),
        "versionCode":      int(find(r"versionCode='([^']+)'") or 0),
        "versionName":      find(r"versionName='([^']+)'"),
        "minSdkVersion":    int(find(r"sdkVersion:'([^']+)'") or 0),
        "targetSdkVersion": int(find(r"targetSdkVersion:'([^']+)'") or 0),
        "name":             find(r"application-label(?:-en)?:'([^']+)'"),
    }


def apk_sig(apk_path):
    out = subprocess.run([APKSIGNER, "verify", "--print-certs", "-v", apk_path],
                         capture_output=True, text=True).stdout
    m = re.search(r"Signer #1 certificate MD5 digest:\s*([0-9a-fA-F:]+)", out)
    return m.group(1).replace(":", "").lower() if m else ""


now_ms = int(time.time() * 1000)
apps, packages = {}, {}

for apk_path in sorted(glob.glob(os.path.join(REPO_DIR, "*.apk"))):
    info = apk_info(apk_path)
    pkg  = info["packageName"]
    if not pkg:
        print(f"WARNING: skipping {apk_path} — could not read package name")
        continue

    apk_name = os.path.basename(apk_path)
    print(f"Processing {apk_name} ({pkg} {info['versionName']})")

    if pkg not in apps:
        apps[pkg] = {
            "categories":  ["Multimedia"],
            "license":     "MIT",
            "name":        info["name"] or pkg,
            "summary":     "",
            "packageName": pkg,
            "lastUpdated": now_ms,
            "added":       now_ms,
        }

    packages.setdefault(pkg, []).append({
        "added":             now_ms,
        "apkName":           apk_name,
        "hash":              sha256_file(apk_path),
        "hashType":          "sha256",
        "minSdkVersion":     info["minSdkVersion"],
        "packageName":       pkg,
        "sig":               apk_sig(apk_path),
        "size":              os.path.getsize(apk_path),
        "targetSdkVersion":  info["targetSdkVersion"],
        "versionCode":       info["versionCode"],
        "versionName":       info["versionName"],
    })

index = {
    "repo": {
        "timestamp":   now_ms,
        "version":     21,
        "maxage":      14,
        "name":        REPO_NAME,
        "icon":        "icon.png",
        "address":     REPO_URL,
        "description": REPO_DESC,
    },
    "requests": {"install": [], "uninstall": []},
    "apps":     list(apps.values()),
    "packages": packages,
}

out_path = os.path.join(REPO_DIR, "index-v1.json")
with open(out_path, "w") as f:
    json.dump(index, f, indent=2)
print(f"Written {out_path}")
