def call(String repoUrl, String branch='main', String targetDir, String githubPat) {
    sh "rm -rf ${targetDir}"
    sh "git clone --depth=1 https://${githubPat}@${repoUrl.replace('https://','')} ${targetDir}"
}
