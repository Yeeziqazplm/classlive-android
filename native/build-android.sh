#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
root="$PWD"
work="$root/.build-cache/native"
revision=bcb639faa4c74a2300ba19e30360cb2355b1663a
: "${CLASSLIVE_ANDROID_NDK:?Set CLASSLIVE_ANDROID_NDK to the Android NDK r28c directory}"
mkdir -p "$work"
if [[ ! -d "$work/transcribe.cpp/.git" ]]; then
    git init -q "$work/transcribe.cpp"
fi
if ! git -C "$work/transcribe.cpp" cat-file -e "$revision^{commit}" 2>/dev/null; then
    git -C "$work/transcribe.cpp" fetch --depth 1 https://github.com/anomalyco/transcribe.cpp.git "$revision"
fi
git -C "$work/transcribe.cpp" checkout --detach "$revision"
cmake -S native -B "$work/arm64" \
    -DCMAKE_TOOLCHAIN_FILE="$CLASSLIVE_ANDROID_NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=26 -DANDROID_STL=c++_static \
    -DTRANSCRIBE_SOURCE="$work/transcribe.cpp" -DGGML_CPU_ARM_ARCH=armv8.2-a+dotprod
cmake --build "$work/arm64" --target classlive_asr --parallel "${CLASSLIVE_JOBS:-4}"
output=app/src/main/jniLibs/arm64-v8a
mkdir -p "$output"
"$CLASSLIVE_ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" \
    --strip-unneeded -o "$output/libclasslive_asr.so" "$work/arm64/libclasslive_asr.so"
