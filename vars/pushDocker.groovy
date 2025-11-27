def call(String imageName, String tag, String gcpRegion, String gcpProject, String artifactRepo) {
    withCredentials([file(credentialsId: 'gcp-artifact-key', variable: 'GCP_KEY')]) {
        sh """
            # Authenticate with GCP
            gcloud auth activate-service-account --key-file=\$GCP_KEY
            gcloud auth configure-docker ${gcpRegion}-docker.pkg.dev

            # Tag and push the image
            docker tag ${imageName}:${tag} ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:${tag}
            docker tag ${imageName}:latest ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:latest
            docker push ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:${tag}
            docker push ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:latest

            # ---- Cleanup old builds (keep last 4) ----
            gcloud artifacts docker images list ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName} --format='get(tags,digest)' \
            | while read TAG DIGEST; do
                # Only consider build-* tags for deletion
                if [[ \$TAG == build-* ]]; then
                    echo "\$TAG \$DIGEST"
                fi
            done | sort -r | awk 'NR>4 {print \$2}' | while read DIGEST; do
                echo "Deleting old image with digest: \$DIGEST"
                gcloud artifacts docker images delete -q ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}@\$DIGEST
            done

            # Optional: prune local dangling images
            docker image prune -f
        """
    }
}
