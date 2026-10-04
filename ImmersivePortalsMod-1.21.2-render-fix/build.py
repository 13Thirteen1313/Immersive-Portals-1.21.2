"""Reproducibly apply the narrow render fix to the preserved 1.21.2 release.

Usage: python build.py --jdk <Java 21 JDK directory> --mixin <sponge-mixin.jar>
                      --joml <joml.jar> [--fix-version 1|2|3]
                      --minecraft-runtime <1.21.2 client-intermediary.jar>
The original JAR is immutable. Versions 1/2 use the preserved named Minecraft
cache; version 3 uses the actual 1.21.2 intermediary runtime for exact hook types.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

HERE = Path(__file__).resolve().parent
VERSION = HERE.parents[1]
BASE = VERSION / 'immersive_portals_1.21.2_alpha_1.0.jar'
EXPECTED = 'b2aff6a952ee11c349db783707ff59fc947c1259e9733a1fc5d808b3c602c1f8'
MIXINS = [
    'client.render.MixinLevelRenderer_FramegraphClear',
    'client.render.MixinLevelRenderer_PortalTerrainClipping',
    'client.render.MixinRenderSystem_PortalClipping',
    'client.render.MixinLevelRenderer_PortalRecursion',
    'client.render.MixinStencilRenderer_PortalRecursion',
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jdk', required=True, type=Path)
    parser.add_argument('--mixin', required=True, type=Path)
    parser.add_argument('--joml', type=Path)
    parser.add_argument('--fix-version', type=int, choices=[1, 2, 3], default=3)
    parser.add_argument('--minecraft-runtime', type=Path)
    args = parser.parse_args()
    if args.fix_version >= 2 and not args.joml:
        parser.error('--joml is required for fix versions 2 and 3')
    if args.fix_version >= 3 and not args.minecraft_runtime:
        parser.error('--minecraft-runtime is required for fix version 3')
    mixins = MIXINS[:{1: 1, 2: 3, 3: 5}[args.fix_version]]
    entries = ['qouteall/imm_ptl/core/mixin/' + m.replace('.', '/') + '.class' for m in mixins]
    assert hashlib.sha256(BASE.read_bytes()).hexdigest() == EXPECTED, 'Unexpected base JAR'
    cache = VERSION / 'Source (Codex)/DimLib-1.21.2-port/.gradle/loom-cache/minecraftMaven'
    jars = list(cache.rglob('minecraft-merged-*.jar'))
    assert len(jars) == 1, f'Expected one preserved Minecraft 1.21.2 compile JAR: {jars}'
    classes = HERE / 'build/classes'
    classes.mkdir(parents=True, exist_ok=True)
    sources = [HERE / 'src' / entry.replace('.class', '.java') for entry in entries]
    if args.minecraft_runtime:
        with zipfile.ZipFile(args.minecraft_runtime) as runtime:
            assert json.loads(runtime.read('version.json'))['id'] == '1.21.2'
            assert 'net/minecraft/class_761.class' in runtime.namelist()
    classpath = [BASE, args.minecraft_runtime or jars[0], args.mixin]
    if args.joml:
        classpath.append(args.joml)
    subprocess.run([
        str(args.jdk / 'bin/javac.exe'), '--release', '21', '-proc:none',
        '-classpath', ';'.join(map(str, classpath)),
        '-d', str(classes), *map(str, sources),
    ], check=True)
    result = VERSION / f'immersive_portals_1.21.2_alpha_1.{args.fix_version}.jar'
    with zipfile.ZipFile(BASE) as original, zipfile.ZipFile(result, 'w') as output:
        for entry in original.infolist():
            data = original.read(entry.filename)
            if entry.filename == 'imm_ptl.mixins.json':
                config = json.loads(data.decode('utf-8-sig'))
                assert not set(mixins).intersection(config['client'])
                config['client'].extend(mixins)
                data = (json.dumps(config, indent=2) + '\n').encode()
            elif entry.filename == 'fabric.mod.json':
                metadata = json.loads(data.decode('utf-8-sig'))
                metadata['version'] = f'6.0.6+mc1.21.2.renderfix{args.fix_version}'
                data = (json.dumps(metadata, indent=2) + '\n').encode()
            output.writestr(entry, data)
        for entry in entries:
            info = zipfile.ZipInfo(entry, date_time=(2026, 10, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            output.writestr(info, (classes / entry).read_bytes())
    with zipfile.ZipFile(BASE) as original, zipfile.ZipFile(result) as patched:
        assert patched.testzip() is None
        assert len(patched.namelist()) == len(set(patched.namelist()))
        assert set(patched.namelist()) - set(original.namelist()) == set(entries)
        changed = [n for n in original.namelist() if original.read(n) != patched.read(n)]
        assert set(changed) == {'imm_ptl.mixins.json', 'fabric.mod.json'}, changed
        assert set(mixins).issubset(json.loads(patched.read('imm_ptl.mixins.json'))['client'])
        if args.fix_version >= 2:
            with zipfile.ZipFile(VERSION / 'immersive_portals_1.21.2_alpha_1.1.jar') as sky_fix:
                assert patched.read(entries[0]) == sky_fix.read(entries[0]), 'Sky fix changed'
        if args.fix_version >= 3:
            with zipfile.ZipFile(VERSION / 'immersive_portals_1.21.2_alpha_1.2.jar') as clipping_fix:
                for entry in entries[:3]:
                    assert patched.read(entry) == clipping_fix.read(entry), f'Previous fix changed: {entry}'
    print('Built:', result)
    print('SHA256:', hashlib.sha256(result.read_bytes()).hexdigest())
    print(f'Verified: {len(entries)} new mixins, registration + version metadata changed; all existing classes/resources preserved.')


if __name__ == '__main__':
    main()
