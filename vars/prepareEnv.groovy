def call(String envCredsId, String targetDir) {
    withCredentials([file(credentialsId: envCredsId, variable: 'ENV_FILE')]) {
        sh "cp \$ENV_FILE ${targetDir}/.env"
    }
}
