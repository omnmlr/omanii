param(
    [string]$JavaHome = 'C:/Program Files/Android/Android Studio/jbr',
    [string]$GradleCache = 'C:/Users/USER/.gradle/caches/modules-2/files-2.1'
)
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$output = Join-Path $PSScriptRoot '.jvm-test'
New-Item -ItemType Directory -Force -Path $output | Out-Null

# Compile against the approved foundation sources, never the superseded proposal or a stub.
$shared = @(
    (Join-Path $repo 'android/app/src/main/java/com/omanii/app/model/MonotonicClock.kt'),
    (Join-Path $repo 'android/app/src/main/java/com/omanii/app/model/Availability.kt'),
    (Join-Path $repo 'android/app/src/main/java/com/omanii/app/model/Identity.kt'),
    (Join-Path $repo 'android/app/src/main/java/com/omanii/app/session/ProbeContextHooks.kt')
)
function Jar([string]$relative, [string]$filename) {
    $found = @(Get-ChildItem -LiteralPath (Join-Path $GradleCache $relative) -Recurse -File |
        Where-Object { $_.Name -eq $filename })
    if ($found.Count -ne 1) { throw "Missing/ambiguous cached jar: $filename" }
    return $found[0].FullName
}
$stdlib = Jar 'org.jetbrains.kotlin/kotlin-stdlib/2.3.21' 'kotlin-stdlib-2.3.21.jar'
$junit = Jar 'junit/junit/4.13.2' 'junit-4.13.2.jar'
$hamcrest = Jar 'org.hamcrest/hamcrest-core/1.3' 'hamcrest-core-1.3.jar'
$annotations = Jar 'org.jetbrains/annotations/13.0' 'annotations-13.0.jar'
$compilerJars = @(
    (Jar 'org.jetbrains.kotlin/kotlin-compiler-embeddable/2.3.21' 'kotlin-compiler-embeddable-2.3.21.jar'),
    $stdlib,
    (Jar 'org.jetbrains.kotlin/kotlin-script-runtime/2.3.21' 'kotlin-script-runtime-2.3.21.jar'),
    (Jar 'org.jetbrains.kotlin/kotlin-reflect/1.6.10' 'kotlin-reflect-1.6.10.jar'),
    (Jar 'org.jetbrains.kotlin/kotlin-daemon-embeddable/2.3.21' 'kotlin-daemon-embeddable-2.3.21.jar'),
    (Jar 'org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.0' 'kotlinx-coroutines-core-jvm-1.8.0.jar'),
    $annotations
)
$sources = @($shared) + @((Get-ChildItem -LiteralPath (Join-Path $repo 'android/app/src/main/java/com/omanii/app/probe') -Filter *.kt).FullName) +
    @((Get-ChildItem -LiteralPath (Join-Path $repo 'android/app/src/test/java/com/omanii/app/probe') -Filter *.kt).FullName)
$java = Join-Path $JavaHome 'bin/java.exe'
$classes = Join-Path $output 'wave1-classes'
$classpath = @($stdlib, $junit, $hamcrest, $annotations) -join ';'
& $java -cp ($compilerJars -join ';') org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -Werror -jvm-target 17 -classpath $classpath -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Isolated Kotlin compilation failed' }
& $java "-Domanii.repo=$repo" -cp "$classes;$classpath" org.junit.runner.JUnitCore com.omanii.app.probe.ConcurrentByteBudgetTest com.omanii.app.probe.ProbeClientTest
if ($LASTEXITCODE -ne 0) { throw 'Isolated task-owned Android tests failed' }
