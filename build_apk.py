"""Reproducible APK build with official Android SDK archives; requires Java 17+, Python 3.
Dependencies are downloaded on first use into .build-cache, and a local debug key is generated.
The local APK produced this way is signed by that key, not by an app-store publisher.
"""
from pathlib import Path
import os, shutil, subprocess, urllib.request, zipfile, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
CACHE=ROOT/'.build-cache'; CACHE.mkdir(exist_ok=True)
urls={
 'platform.zip':'https://dl.google.com/android/repository/platform-35_r02.zip',
 'tools.zip':'https://dl.google.com/android/repository/build-tools_r35_linux.zip',
 'okhttp.jar':'https://repo.maven.apache.org/maven2/com/squareup/okhttp3/okhttp/3.14.9/okhttp-3.14.9.jar',
 'okio.jar':'https://repo.maven.apache.org/maven2/com/squareup/okio/okio/1.17.2/okio-1.17.2.jar'
}
def run(*args): subprocess.run([str(a) for a in args],check=True,cwd=ROOT)
def build():
 if not shutil.which('java'): raise SystemExit('Java 17+ required')
 for name,url in urls.items():
  p=CACHE/name
  if not p.exists():
   print('Downloading',name,flush=True)
   with urllib.request.urlopen(url,timeout=60) as response,p.open('wb') as out: shutil.copyfileobj(response,out)
 for name,sub in [('platform.zip','platforms'),('tools.zip','tools')]:
  destination=CACHE/sub
  if not destination.exists():
   with zipfile.ZipFile(CACHE/name) as z: z.extractall(destination)
 tools=CACHE/'tools/android-15'; android=CACHE/'platforms/android-35/android.jar'
 for exe in ['aapt2','zipalign']: (tools/exe).chmod(0o755)
 output=ROOT/'build-local';shutil.rmtree(output,ignore_errors=True);output.mkdir();(output/'classes').mkdir();(output/'dex').mkdir()
 manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml');manifest.getroot().set('package','com.yeezi.classlive');manifest.write(output/'AndroidManifest.xml',encoding='utf-8')
 run(tools/'aapt2','compile','--dir',ROOT/'app/src/main/res','-o',output/'res.zip')
 run(tools/'aapt2','link','-I',android,'--manifest',output/'AndroidManifest.xml','--min-sdk-version','26','--target-sdk-version','35','--version-code','1','--version-name','0.1.0','-o',output/'base.apk',output/'res.zip')
 javafiles=list((ROOT/'app/src/main/java').rglob('*.java'))
 run('java','-m','jdk.compiler/com.sun.tools.javac.Main','-source','8','-target','8','-encoding','UTF-8','-classpath',os.pathsep.join(map(str,[android,CACHE/'okhttp.jar',CACHE/'okio.jar'])),'-d',output/'classes',*javafiles)
 with zipfile.ZipFile(output/'classes.jar','w') as z:
  for p in (output/'classes').rglob('*.class'):z.write(p,p.relative_to(output/'classes'))
 run('java','-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--min-api','26','--lib',android,'--output',output/'dex',output/'classes.jar',CACHE/'okhttp.jar',CACHE/'okio.jar')
 shutil.copyfile(output/'base.apk',output/'unsigned.apk')
 with zipfile.ZipFile(output/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
  for p in (output/'dex').glob('*.dex'):z.write(p,p.name)
 run(tools/'zipalign','-f','4',output/'unsigned.apk',output/'aligned.apk')
 key=CACHE/'debug.jks'
 if not key.exists():
  run('keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=ClassLive local debug','-keyalg','RSA','-validity','10000')
 apk=ROOT.parent/'ClassLive-0.1.0.apk'
 signer=tools/'lib/apksigner.jar'
 run('java','-jar',signer,'sign','--ks',key,'--ks-pass','pass:android','--key-pass','pass:android','--out',apk,output/'aligned.apk')
 run('java','-jar',signer,'verify','--verbose',apk)
 run(tools/'aapt2','dump','badging',apk)
 print('APK:',apk)
if __name__=='__main__': build()
