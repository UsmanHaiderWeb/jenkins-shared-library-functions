def call(String serviceName, String deployLogDir) {
    sh "docker-compose pull ${serviceName} >> ${deployLogDir}/deploy_\$(date +%F_%T).log 2>&1"
    sh "docker-compose up -d --no-deps --build ${serviceName}"
}
