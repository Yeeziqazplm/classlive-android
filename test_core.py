"""Run credential-free core tests. build_apk.py should have been run first."""
from pathlib import Path
import subprocess, tempfile, urllib.request, os
root=Path(__file__).resolve().parent
java=root/'app/src/main/java/com/yeezi/classlive'
cache=Path(os.environ.get('CLASSLIVE_BUILD_CACHE',str(root/'.build-cache')))
android=cache/'platforms/android-35/android.jar'
if not android.exists():
    sdk=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if sdk:android=Path(sdk)/'platforms/android-35/android.jar'
if not android.exists():raise SystemExit('Run python3 build_apk.py first to obtain android.jar')
jar=cache/'test-json.jar'
cache.mkdir(parents=True,exist_ok=True)
if not jar.exists():urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar',jar)
with tempfile.TemporaryDirectory() as d:
    deps=os.pathsep.join(map(str,[jar,android]))
    files=[java/'AudioProcessor.java',java/'Lecture.java',java/'LectureStore.java',root/'tests/CoreTests.java',root/'tests/CodecTests.java']
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-classpath',deps,'-d',d,*map(str,files)],check=True)
    for name in ['CoreTests','CodecTests']:
        subprocess.run(['java','-cp',d+os.pathsep+deps,'com.yeezi.classlive.'+name],check=True)
