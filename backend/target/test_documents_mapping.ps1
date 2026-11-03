$ErrorActionPreference = 'Stop'
$heritageProperties = @{}
Get-Content 'src/main/resources/application.properties' | ForEach-Object {
    if ($_ -match '^(spring\.datasource\.[^=]+)=(.*)$') { $heritageProperties[$matches[1]] = $matches[2].Trim() }
}
$heritagePreviousUrl = $env:AUXIMAD_TEST_DATABASE_URL
$heritagePreviousUser = $env:AUXIMAD_TEST_DATABASE_USER
$heritagePreviousPassword = $env:AUXIMAD_TEST_DATABASE_PASSWORD
try {
    $env:AUXIMAD_TEST_DATABASE_URL = $heritageProperties['spring.datasource.url']
    $env:AUXIMAD_TEST_DATABASE_USER = $heritageProperties['spring.datasource.username']
    $env:AUXIMAD_TEST_DATABASE_PASSWORD = $heritageProperties['spring.datasource.password']
    $env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
    $ErrorActionPreference = 'Continue'
    mvn -B -s target/mail-maven-settings.xml '-Dmaven.repo.local=target/mail-test-m2' '-Dtest=DocumentsManquantsMappingPostgresTest' test *> target/documents-mapping-before.log
    $heritageResult = $LASTEXITCODE
    Get-Content target/documents-mapping-before.log -Tail 35
} finally {
    $env:AUXIMAD_TEST_DATABASE_URL = $heritagePreviousUrl
    $env:AUXIMAD_TEST_DATABASE_USER = $heritagePreviousUser
    $env:AUXIMAD_TEST_DATABASE_PASSWORD = $heritagePreviousPassword
}
exit $heritageResult
