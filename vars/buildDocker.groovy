def call(String imageName, String tag, String contextDir) {
    dir(contextDir) {
        sh """
            docker build -t ${imageName}:${tag} --no-cache .
            docker tag ${imageName}:${tag} ${imageName}:latest
        """
    }
}
