"""Run credential-free core tests. build_apk.py should have been run first."""
from pathlib import Path
import subprocess, tempfile, urllib.request, os
root=Path(__file__).resolve().parent
java=root/'app/src/main/java/com/yeezi/classlive'
android=root/'.build-cache/platforms/android-35/android.jar'
if not android.exists():raise SystemExit('Run python3 build_apk.py first to obtain android.jar')
jar=root/'.build-cache/test-json.jar'
if not jar.exists():urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar',jar)
with tempfile.TemporaryDirectory() as d:
    deps=os.pathsep.join(map(str,[jar,android]))
    files=[java/'AudioProcessor.java',java/'Lecture.java',java/'LectureStore.java',root/'tests/CoreTests.java',root/'tests/CodecTests.java']
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-classpath',deps,'-d',d,*map(str,files)],check=True)
    for name in ['CoreTests','CodecTests']:
        subprocess.run(['java','-cp',d+os.pathsep+deps,'com.yeezi.classlive.'+name],check=True)
