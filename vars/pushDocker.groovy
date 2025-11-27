def call(String imageName, String tag, String gcpRegion, String gcpProject, String artifactRepo) {
    withCredentials([file(credentialsId: 'gcp-artifact-key', variable: 'GCP_KEY')]) {
        sh """
            echo "🔐 Authenticating with GCP..."
            gcloud auth activate-service-account --key-file=\$GCP_KEY
            gcloud auth configure-docker ${gcpRegion}-docker.pkg.dev

            echo "🏷 Tagging & pushing Docker image to Artifact Registry..."
            docker tag ${imageName}:${tag} ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:${tag}
            docker tag ${imageName}:latest ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:latest

            docker push ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:${tag}
            docker push ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}:latest

            echo "🧹 Cleaning up old build images (keeping latest 4)..."

            # List all images, filter build-* only, sort by newest CREATE_TIME, skip first 4, extract DIGEST
            DIGESTS_TO_DELETE=\$(gcloud artifacts docker images list ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName} \
                --format='get(tags,digest,createTime)' \
                | grep -E "^build-" \
                | sort -rk3 \
                | awk 'NR>4 {print \$2}' )

            if [[ -z "\$DIGESTS_TO_DELETE" ]]; then
                echo "✅ No old build images to delete."
            else
                echo "\$DIGESTS_TO_DELETE" | while read DIGEST; do
                    echo "🗑 Deleting image digest: \$DIGEST"
                    gcloud artifacts docker images delete -q ${gcpRegion}-docker.pkg.dev/${gcpProject}/${artifactRepo}/${imageName}@\$DIGEST --delete-tags
                done
            fi

            echo "♻ Pruning local unused images..."
            docker image prune -f

            echo "🚀 Push stage completed."
        """
    }
}
