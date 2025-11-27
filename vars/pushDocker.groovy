def call(String imageName, String tag, String gcpRegion, String gcpProject, String artifactRepo) {
    withCredentials([file(credentialsId: 'gcp-artifact-key', variable: 'GCP_KEY')]) {
        sh """
            gcloud auth activate-service-account --key-file=\$GCP_KEY
            gcloud auth configure-docker ${gcpRegion}-docker.pkg.dev

            docker tag ${imageName}:${tag} ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:${tag}
            docker tag ${imageName}:latest ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:latest
            docker push ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:${tag}
            docker push ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:latest

            # Clean old images (keep last 4 builds)
            IMAGES=\$(gcloud artifacts docker images list ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName} --format='get(tags)')
            TO_DELETE=\$(echo "\$IMAGES" | grep build- | sort -r | tail -n +5)
            for img in \$TO_DELETE; do
                gcloud artifacts docker images delete -q ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}@\${img}
            done

            docker image prune -f
        """
    }
}
